# 🛡️ Intelligent Incident Management Platform (IIMP)

Production-grade AIOps platform with AI-powered Root Cause Analysis, event-driven architecture, and full observability stack.

---

## Quick Start

### Prerequisites
- Docker Desktop 24+
- Docker Compose v2
- OpenAI API Key (for RCA generation)

### 1. Clone & Configure
```bash
git clone <repo>
cd intelligent-incident-management-platform

# Set your OpenAI key
export OPENAI_API_KEY=sk-your-key-here
```

### 2. Start All Services
```bash
docker compose up -d
```

### 3. Wait for Health
```bash
docker compose ps
# All services should show "healthy"
```

### 4. Access the Platform

| Service                  | URL                                             |
|--------------------------|-------------------------------------------------|
| Frontend UI              | http://localhost:3000                           |
| API Gateway              | http://localhost:8080                           |
| Swagger (IMS)            | http://localhost:8081/swagger-ui.html           |
| Swagger (RCA)            | http://localhost:8086/swagger-ui.html           |
| Swagger (Anomaly)        | http://localhost:8088/swagger-ui.html           |
| Swagger (Blast Radius)   | http://localhost:8087/swagger-ui.html           |
| Swagger (Notifications)  | http://localhost:8094/swagger-ui.html           |
| Swagger (SLO)            | http://localhost:8096/swagger-ui.html           |
| Swagger (Postmortem)     | http://localhost:8097/swagger-ui.html           |
| Swagger (Knowledge Base) | http://localhost:8095/swagger-ui.html           |
| Swagger (AI Copilot)     | http://localhost:8098/swagger-ui.html           |
| Grafana                  | http://localhost:3001 (admin/admin)             |
| Kafka UI                 | http://localhost:8888                           |
| Prometheus               | http://localhost:9090                           |

### 5. Create Admin User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@iimp.io",
    "password": "Admin@123456",
    "role": "ADMIN"
  }'
```

### 6. Run Failure Simulations
```bash
# Simulate all failure scenarios
bash docs/failure-simulations/simulate-failures.sh all

# Or individually
bash docs/failure-simulations/simulate-failures.sh db      # DB exhaustion
bash docs/failure-simulations/simulate-failures.sh oom     # Memory leak
bash docs/failure-simulations/simulate-failures.sh deploy  # Bad deployment
bash docs/failure-simulations/simulate-failures.sh slow    # Slow dependency
bash docs/failure-simulations/simulate-failures.sh kafka   # Kafka lag
```

---

## Architecture

```
React Frontend (Port 3000)
         │
    API Gateway (8080) ← JWT validation, routing
         │
    ┌────┼────────────────────────────────────┐
    │    │                                    │
  IMS   RCA    Health   Deployments   Users  │
 (8081)(8086)  (8084)     (8083)     (8090) │
    │    │                                    │
    └────┴─── PostgreSQL + Redis + Kafka ─────┘
                         │
              Log Analyzer + Alert Correlation
                         │
              Prometheus + Grafana (Observability)
```

---

## Kafka Event Flow

```
Microservices → logs-topic      → Log Analyzer → alerts-topic
                alerts-topic    → Alert Correlation → incidents-topic
                deployment-topic→ Deployment Tracker → alerts-topic
                traces-topic    → Service Health Analyzer
                incidents-topic → AI RCA Service → rca-topic
                service-health-topic → Dashboard
```

---

## API Reference

### Authentication
```bash
POST /api/v1/auth/login
POST /api/v1/auth/register
```

### Incidents
```bash
GET    /api/v1/incidents
POST   /api/v1/incidents
GET    /api/v1/incidents/{id}
POST   /api/v1/incidents/{id}/acknowledge
POST   /api/v1/incidents/{id}/resolve
POST   /api/v1/incidents/{id}/close
GET    /api/v1/incidents/{id}/timeline
GET    /api/v1/incidents/dashboard/summary
```

### AI RCA
```bash
GET /api/v1/rca/incidents/{incidentId}
GET /api/v1/rca/incidents/{incidentId}/all
```

### Service Health
```bash
GET /api/v1/health
GET /api/v1/health/{serviceName}
```

### Anomaly Detection
```bash
GET  /api/v1/anomalies/{serviceName}
POST /api/v1/anomalies/detect/error-rate
POST /api/v1/anomalies/detect/latency
```

### Blast Radius
```bash
POST /api/v1/blast-radius/analyze
GET  /api/v1/blast-radius/incidents/{incidentId}
```

### Notifications
```bash
GET  /api/v1/notifications/incidents/{incidentId}
POST /api/v1/notifications/send
```

### SLO / Error Budget
```bash
GET  /api/v1/slos
GET  /api/v1/slos/{serviceName}
GET  /api/v1/slos/breached
POST /api/v1/slos
POST /api/v1/slos/{serviceName}/downtime
```

### Postmortems
```bash
GET  /api/v1/postmortems
GET  /api/v1/postmortems/incidents/{incidentId}
POST /api/v1/postmortems/generate
POST /api/v1/postmortems/{id}/publish
```

### Knowledge Base
```bash
GET  /api/v1/kb/search?q={query}
GET  /api/v1/kb/services/{serviceName}
GET  /api/v1/kb/category/{category}
GET  /api/v1/kb/{id}
POST /api/v1/kb
POST /api/v1/kb/runbook/generate
```

### AI SRE Copilot
```bash
POST /api/v1/copilot/chat
GET  /api/v1/copilot/history/{sessionId}
```

---

## Technology Stack

| Layer        | Technology                                    |
|--------------|-----------------------------------------------|
| Backend      | Java 21, Spring Boot 3, Spring WebFlux        |
| AI           | Spring AI, OpenAI GPT-4o                      |
| Messaging    | Apache Kafka                                  |
| Database     | PostgreSQL 16 + Flyway migrations             |
| Cache        | Redis 7                                       |
| Security     | Spring Security, JWT (JJWT)                   |
| Observability| OpenTelemetry, Prometheus, Grafana            |
| Frontend     | React 18, TypeScript, MUI v5, ECharts         |
| State Mgmt   | TanStack Query, Zustand                       |
| Container    | Docker, Kubernetes, Helm                      |
| Testing      | JUnit 5, Testcontainers, MockMvc              |

---

## Folder Structure

```
.
├── backend/
│   ├── common-lib/                 # Shared events, DTOs, security
│   ├── incident-management-service/# Core incident CRUD + lifecycle
│   ├── ai-rca-service/             # GPT-4o powered RCA
│   ├── log-analyzer-service/       # Log ingestion + anomaly detection
│   ├── alert-correlation-service/  # Alert dedup + clustering
│   ├── deployment-tracker-service/ # Deployment tracking + risk scoring
│   ├── service-health-analyzer/    # Health score computation
│   ├── user-service/               # JWT auth + RBAC
│   ├── api-gateway/                # Spring Cloud Gateway
│   ├── anomaly-detection-service/  # Z-score + EWMA anomaly detection
│   ├── blast-radius-analyzer/      # Incident propagation impact analysis
│   ├── notification-service/       # Email/webhook alert delivery
│   ├── slo-service/                # SLO tracking + error budget management
│   ├── postmortem-service/         # AI-generated blameless postmortems
│   ├── knowledge-base-service/     # Runbooks, RCA articles + AI search
│   ├── ai-copilot-service/         # GPT-4o SRE chat copilot
│   ├── order-service/              # Domain service (signal generator)
│   ├── payment-service/            # Domain service (signal generator)
│   └── inventory-service/          # Domain service (signal generator)
├── frontend/
│   └── src/
│       ├── features/               # Feature-based modules
│       │   ├── dashboard/
│       │   ├── incidents/
│       │   ├── rca/
│       │   ├── health/
│       │   ├── deployments/
│       │   └── alerts/
│       ├── api/                    # Axios API client + endpoints
│       ├── components/             # Shared UI components
│       ├── store/                  # Zustand stores
│       ├── hooks/                  # Custom React hooks
│       └── types/                  # TypeScript interfaces
├── infrastructure/
│   ├── prometheus/                 # Prometheus config
│   ├── grafana/                    # Dashboards + provisioning
│   ├── otel/                       # OpenTelemetry collector config
│   ├── kubernetes/                 # K8s manifests
│   └── helm/                       # Helm charts
├── docs/
│   ├── HLD.md
│   ├── LLD.md
│   └── failure-simulations/
└── docker-compose.yml
```

---

## Grafana Dashboards

1. **Incident Overview** — Active incidents, severity trends, MTTR analysis
2. **Service Health** — Health score gauges, error rates, latency P99
3. **RCA Analytics** — Generation duration, success rate, confidence scores

Import dashboards from `infrastructure/grafana/dashboards/`.
