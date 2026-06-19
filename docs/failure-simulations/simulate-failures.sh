#!/bin/bash
# ============================================================
# IIMP Failure Simulation Scripts
# Each scenario:
#   1. Triggers the failure condition
#   2. Injects signals into Kafka topics
#   3. Monitors incident + RCA generation
# ============================================================

API_BASE="http://localhost:8080"
KAFKA_CMD="docker exec iimp-kafka kafka-console-producer --bootstrap-server localhost:29092"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${GREEN}[SIMULATE]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# ──────────────────────────────────────────────────────────
# SCENARIO 1: Database Connection Exhaustion
# ──────────────────────────────────────────────────────────
simulate_db_exhaustion() {
  log "=== Scenario 1: Database Connection Exhaustion ==="

  # Inject ERROR logs simulating DB connection failures
  for i in $(seq 1 10); do
    payload=$(cat <<EOF
{
  "eventId": "$(uuidgen)",
  "serviceName": "order-service",
  "level": "ERROR",
  "message": "Unable to acquire JDBC Connection: HikariPool-1 - Connection is not available, request timed out after 30000ms",
  "exceptionClass": "com.zaxxer.hikari.pool.HikariPool\$PoolInitializationException",
  "traceId": "abc$(printf '%028x' $i)",
  "loggedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
)
    echo "$payload" | docker exec -i iimp-kafka \
      kafka-console-producer --bootstrap-server localhost:29092 --topic logs-topic
    sleep 0.2
  done

  log "Injected 10 DB connection error logs → watching for auto-incident creation..."
  sleep 5
  check_incidents
}

# ──────────────────────────────────────────────────────────
# SCENARIO 2: Memory Leak / OOM
# ──────────────────────────────────────────────────────────
simulate_memory_leak() {
  log "=== Scenario 2: Memory Leak / OOM ==="

  for i in $(seq 1 5); do
    payload=$(cat <<EOF
{
  "eventId": "$(uuidgen)",
  "serviceName": "payment-service",
  "level": "FATAL",
  "message": "java.lang.OutOfMemoryError: Java heap space",
  "exceptionClass": "java.lang.OutOfMemoryError",
  "stackTrace": "java.lang.OutOfMemoryError: Java heap space\n\tat java.util.Arrays.copyOf\n\tat java.util.ArrayList.grow",
  "loggedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
)
    echo "$payload" | docker exec -i iimp-kafka \
      kafka-console-producer --bootstrap-server localhost:29092 --topic logs-topic
    sleep 0.5
  done

  log "Injected OOM errors → anomaly score = 1.0, CRITICAL alert expected"
  sleep 5
  check_incidents
}

# ──────────────────────────────────────────────────────────
# SCENARIO 3: Bad Deployment Rollout
# ──────────────────────────────────────────────────────────
simulate_bad_deployment() {
  log "=== Scenario 3: Bad Deployment Rollout ==="

  # Inject deployment event
  deploy_payload=$(cat <<EOF
{
  "eventId": "$(uuidgen)",
  "serviceName": "inventory-service",
  "version": "2.1.0-rc1",
  "environment": "production",
  "status": "FAILED",
  "deployedBy": "ci-pipeline",
  "commitSha": "deadbeef1234567890abcdef",
  "branch": "feature/bulk-import",
  "riskScore": 0.8,
  "configChanges": {"DB_POOL_SIZE": "200", "FEATURE_FLAG_BULK": "true"},
  "startedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
)
  echo "$deploy_payload" | docker exec -i iimp-kafka \
    kafka-console-producer --bootstrap-server localhost:29092 --topic deployment-topic

  log "Injected failed deployment → risk alert + incident expected"
  sleep 5
  check_incidents
}

# ──────────────────────────────────────────────────────────
# SCENARIO 4: Slow Downstream Dependency
# ──────────────────────────────────────────────────────────
simulate_slow_dependency() {
  log "=== Scenario 4: Slow Downstream Dependency ==="

  for i in $(seq 1 8); do
    trace_payload=$(cat <<EOF
{
  "eventId": "$(uuidgen)",
  "traceId": "trace$(printf '%028x' $i)",
  "spanId": "span$(printf '%015x' $i)",
  "serviceName": "order-service",
  "operationName": "POST /api/payments/charge",
  "status": "TIMEOUT",
  "durationMs": 35000,
  "httpMethod": "POST",
  "httpUrl": "http://payment-service/charge",
  "httpStatus": 504,
  "errorMessage": "Gateway Timeout: payment-service did not respond within 30s",
  "spanStartedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
)
    echo "$trace_payload" | docker exec -i iimp-kafka \
      kafka-console-producer --bootstrap-server localhost:29092 --topic traces-topic
    sleep 0.3
  done

  log "Injected 8 timeout traces → service health degradation expected"
  sleep 5
  check_incidents
}

# ──────────────────────────────────────────────────────────
# SCENARIO 5: Kafka Consumer Lag
# ──────────────────────────────────────────────────────────
simulate_kafka_consumer_lag() {
  log "=== Scenario 5: Kafka Consumer Lag ==="

  # Inject alert about consumer group lag
  alert_payload=$(cat <<EOF
{
  "eventId": "$(uuidgen)",
  "serviceName": "incident-management-service",
  "alertName": "KafkaConsumerLagHigh",
  "severity": "HIGH",
  "status": "FIRING",
  "source": "prometheus",
  "message": "Consumer group 'incident-management-service' lag is 15000 for topic 'alerts-topic'",
  "fingerprint": "kafka-lag-incident-mgmt-alerts",
  "labels": {"consumerGroup": "incident-management-service", "topic": "alerts-topic", "lag": "15000"},
  "firedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
)
  echo "$alert_payload" | docker exec -i iimp-kafka \
    kafka-console-producer --bootstrap-server localhost:29092 --topic alerts-topic

  log "Injected Kafka lag alert"
}

# ──────────────────────────────────────────────────────────
# Check generated incidents
# ──────────────────────────────────────────────────────────
check_incidents() {
  warn "Checking incidents API..."
  curl -s -H "Authorization: Bearer $(get_token)" \
    "$API_BASE/api/v1/incidents?page=0&size=5" | python3 -m json.tool 2>/dev/null || \
    curl -s -H "Authorization: Bearer $(get_token)" "$API_BASE/api/v1/incidents?page=0&size=5"
}

get_token() {
  curl -s -X POST "$API_BASE/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"email":"admin@iimp.io","password":"Admin@123"}' | \
    python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])" 2>/dev/null || \
    echo "test-token"
}

# ──────────────────────────────────────────────────────────
# MAIN
# ──────────────────────────────────────────────────────────
case "${1:-all}" in
  db)         simulate_db_exhaustion ;;
  oom)        simulate_memory_leak ;;
  deploy)     simulate_bad_deployment ;;
  slow)       simulate_slow_dependency ;;
  kafka)      simulate_kafka_consumer_lag ;;
  all)
    simulate_db_exhaustion
    sleep 10
    simulate_memory_leak
    sleep 10
    simulate_bad_deployment
    sleep 10
    simulate_slow_dependency
    sleep 10
    simulate_kafka_consumer_lag
    ;;
  *)
    echo "Usage: $0 [db|oom|deploy|slow|kafka|all]"
    exit 1
    ;;
esac

log "Simulation complete. Check Grafana at http://localhost:3001 and the UI at http://localhost:3000"
