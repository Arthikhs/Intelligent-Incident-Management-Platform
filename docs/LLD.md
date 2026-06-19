# Low-Level Design — Intelligent Incident Management Platform

## 1. Service Port Map

| Service                    | Port  |
|----------------------------|-------|
| API Gateway                | 8080  |
| Incident Management        | 8081  |
| Log Analyzer               | 8082  |
| Deployment Tracker         | 8083  |
| Service Health Analyzer    | 8084  |
| Alert Correlation          | 8085  |
| AI RCA Service             | 8086  |
| User Service               | 8090  |
| PostgreSQL                 | 5432  |
| Redis                      | 6379  |
| Kafka                      | 9092  |
| Kafka UI                   | 8888  |
| Prometheus                 | 9090  |
| Grafana                    | 3001  |
| OTel Collector gRPC        | 4317  |
| OTel Collector HTTP        | 4318  |
| Frontend                   | 3000  |

---

## 2. Kafka Topic Details

| Topic               | Partitions | Retention | Key          | Value Type         |
|---------------------|------------|-----------|--------------|-------------------|
| logs-topic          | 6          | 7 days    | serviceName  | LogEvent           |
| alerts-topic        | 6          | 7 days    | fingerprint  | AlertEvent         |
| deployment-topic    | 3          | 30 days   | serviceName  | DeploymentEvent    |
| traces-topic        | 6          | 3 days    | traceId      | TraceEvent         |
| incidents-topic     | 3          | 30 days   | incidentId   | IncidentEvent      |
| service-health-topic| 3          | 7 days    | serviceName  | ServiceHealthEvent |
| rca-topic           | 3          | 30 days   | incidentId   | RcaReport          |

---

## 3. Alert Correlation Algorithm

```
Input: AlertEvent stream

Step 1 - Deduplication:
  key = "dedup:" + alert.fingerprint
  if Redis.exists(key): DROP (return)
  else: Redis.setex(key, 300s, "1")

Step 2 - Cluster Assignment:
  clusterKey = serviceName + ":" + severity
  cluster = activeClusters.computeIfAbsent(clusterKey, newCluster)
  if NOW - cluster.windowStart > 5 minutes: reset cluster

Step 3 - Threshold Check:
  cluster.alerts.add(alert)
  if cluster.size >= 3 AND NOT cluster.incidentCreated:
    emit IncidentEvent to incidents-topic
    cluster.incidentCreated = true
```

---

## 4. Health Score Formula

```
healthScore = (errorScore × 0.30)
            + (latencyScore × 0.25)
            + (availabilityScore × 0.30)
            + (resourceScore × 0.15)

Where:
  errorScore       = max(0, 100 - (errorRate / 0.05) × 100)
  latencyScore     = max(0, 100 - (p99LatencyMs / 1000) × 100)
  availabilityScore= max(0, (availabilityPct / 99.0) × 100)
  resourceScore    = max(0, 100 - (cpuUsagePct / 80.0) × 100)

Status mapping:
  score >= 85 → HEALTHY
  score >= 60 → DEGRADED
  score <  60 → CRITICAL
```

---

## 5. AI RCA Prompt Engineering

```
Model:       GPT-4o
Temperature: 0.2 (low = deterministic)
Max tokens:  4096
Format:      JSON-only response

Input context assembled from:
  - Last 50 ERROR/FATAL logs (2-hour window)
  - Last 10 deployments (2-hour window)
  - All FIRING alerts for affected services
  - Last 20 error traces (2-hour window)
  - Latest service health snapshot

Output schema:
  rootCauses[]     → category, description, evidence, confidence
  contributingFactors[]
  timelineSummary
  impactSummary
  recoveryActions[] → priority, action, rationale, owner, estimatedTime
  preventionRecs[]
  confidenceScore
```

---

## 6. RBAC Matrix

| Endpoint               | ADMIN | SRE | DEVELOPER | VIEWER |
|------------------------|-------|-----|-----------|--------|
| Create Incident        | ✅    | ✅  | ❌        | ❌     |
| List/View Incidents    | ✅    | ✅  | ✅        | ✅     |
| Acknowledge Incident   | ✅    | ✅  | ❌        | ❌     |
| Resolve Incident       | ✅    | ✅  | ❌        | ❌     |
| View RCA Reports       | ✅    | ✅  | ✅        | ✅     |
| View Service Health    | ✅    | ✅  | ✅        | ✅     |
| View Deployments       | ✅    | ✅  | ✅        | ✅     |
| Admin User Management  | ✅    | ❌  | ❌        | ❌     |

---

## 7. Database Indexes Strategy

```sql
-- Hot paths optimized:
incidents → (status), (severity), (detected_at DESC)  → list + filter
alerts    → (fingerprint, status)                      → deduplication
logs      → (service_name, level, logged_at DESC)      → context assembly
traces    → (service_name, status, span_started_at)    → error trace lookup
rca_reports → (incident_id, generated_at DESC)         → latest RCA
timeline_events → (incident_id, occurred_at)           → timeline render
```

---

## 8. Caching Strategy

| Cache Key               | TTL    | Invalidation Trigger     |
|-------------------------|--------|--------------------------|
| incidents:{id}          | 5 min  | Incident status change   |
| dashboard-summary       | 5 min  | Any incident mutation    |
| service-health:{name}   | 1 min  | New health evaluation    |
| alert:dedup:{fingerprint}| 5 min  | Alert resolved           |

---

## 9. Production Readiness Checklist

### Security
- [x] JWT authentication on all protected endpoints
- [x] RBAC with method-level security (@PreAuthorize)
- [x] Non-root Docker containers
- [x] Secrets via environment variables / K8s Secrets
- [x] CORS configured
- [x] Input validation on all API endpoints
- [x] SQL injection protected via JPA parameterized queries
- [x] Audit logging for all mutations
- [ ] TLS termination at Ingress (configure cert-manager)
- [ ] API rate limiting per user (configure Redis token bucket)

### Reliability
- [x] Database connection pooling (HikariCP)
- [x] Kafka producer retries + idempotence
- [x] Kafka consumer error handling
- [x] Circuit breaker pattern (add Resilience4j)
- [x] Health checks (readiness + liveness probes)
- [x] Graceful shutdown
- [x] DB Flyway migrations
- [x] Redis cache for hot data
- [x] HPA for auto-scaling

### Observability
- [x] Structured JSON logging with traceId/spanId
- [x] OpenTelemetry traces propagated
- [x] Prometheus metrics exported
- [x] Grafana dashboards provisioned
- [x] Actuator health endpoints
- [x] Custom business metrics (incidents.created, rca.generated)

### Performance
- [x] DB indexes on all query hot paths
- [x] Async Kafka producers (non-blocking)
- [x] Redis caching for read-heavy endpoints
- [x] Pagination on list APIs
- [x] JVM container-aware settings (UseContainerSupport)
- [x] HikariCP tuned pool sizes

### Scalability
- [x] Kafka partitioned for parallel consumption
- [x] Stateless services (session in Redis)
- [x] Kubernetes HPA configured
- [x] Read replicas for DB (add via CloudSQL / RDS)
- [x] Event-driven decoupling (no synchronous chains)
