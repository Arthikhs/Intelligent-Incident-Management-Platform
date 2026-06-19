-- V1__init_schema.sql
-- Intelligent Incident Management Platform — Initial Schema

-- ============================================================
-- EXTENSIONS
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username      VARCHAR(100) NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'VIEWER',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);

-- ============================================================
-- INCIDENTS
-- ============================================================
CREATE TABLE incidents (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title               VARCHAR(500)  NOT NULL,
    description         TEXT,
    severity            VARCHAR(20)   NOT NULL CHECK (severity IN ('CRITICAL','HIGH','MEDIUM','LOW')),
    status              VARCHAR(30)   NOT NULL CHECK (status IN ('OPEN','INVESTIGATING','MITIGATED','RESOLVED','CLOSED')),
    affected_services   TEXT[]        NOT NULL DEFAULT '{}',
    source_alert_ids    UUID[]        DEFAULT '{}',
    deployment_id       UUID,
    assigned_to         UUID          REFERENCES users(id),
    created_by          UUID          REFERENCES users(id),
    detected_at         TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    acknowledged_at     TIMESTAMPTZ,
    resolved_at         TIMESTAMPTZ,
    closed_at           TIMESTAMPTZ,
    mttr_seconds        BIGINT,
    alert_count         INT           NOT NULL DEFAULT 0,
    error_count         INT           NOT NULL DEFAULT 0,
    impacted_users      BIGINT        NOT NULL DEFAULT 0,
    tags                TEXT[]        DEFAULT '{}',
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incidents_status     ON incidents(status);
CREATE INDEX idx_incidents_severity   ON incidents(severity);
CREATE INDEX idx_incidents_detected   ON incidents(detected_at DESC);
CREATE INDEX idx_incidents_services   ON incidents USING GIN(affected_services);

-- ============================================================
-- ALERTS
-- ============================================================
CREATE TABLE alerts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    incident_id     UUID REFERENCES incidents(id) ON DELETE SET NULL,
    service_name    VARCHAR(200) NOT NULL,
    alert_name      VARCHAR(500) NOT NULL,
    severity        VARCHAR(20)  NOT NULL CHECK (severity IN ('CRITICAL','HIGH','MEDIUM','LOW','INFO')),
    status          VARCHAR(20)  NOT NULL DEFAULT 'FIRING' CHECK (status IN ('FIRING','RESOLVED','SILENCED')),
    source          VARCHAR(100),
    message         TEXT,
    labels          JSONB        DEFAULT '{}',
    annotations     JSONB        DEFAULT '{}',
    fingerprint     VARCHAR(64)  NOT NULL,
    fired_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alerts_incident    ON alerts(incident_id);
CREATE INDEX idx_alerts_service     ON alerts(service_name);
CREATE INDEX idx_alerts_fingerprint ON alerts(fingerprint);
CREATE INDEX idx_alerts_status      ON alerts(status);
CREATE INDEX idx_alerts_fired_at    ON alerts(fired_at DESC);
CREATE INDEX idx_alerts_labels      ON alerts USING GIN(labels);

-- ============================================================
-- DEPLOYMENTS
-- ============================================================
CREATE TABLE deployments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name    VARCHAR(200) NOT NULL,
    version         VARCHAR(100) NOT NULL,
    environment     VARCHAR(50)  NOT NULL DEFAULT 'production',
    status          VARCHAR(30)  NOT NULL DEFAULT 'IN_PROGRESS'
                    CHECK (status IN ('IN_PROGRESS','SUCCESS','FAILED','ROLLED_BACK')),
    deployed_by     VARCHAR(200),
    commit_sha      VARCHAR(64),
    branch          VARCHAR(200),
    pipeline_url    VARCHAR(1000),
    config_changes  JSONB        DEFAULT '{}',
    risk_score      DECIMAL(5,2) DEFAULT 0.0,
    incident_id     UUID REFERENCES incidents(id) ON DELETE SET NULL,
    started_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    rolled_back_at  TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_deployments_service   ON deployments(service_name);
CREATE INDEX idx_deployments_status    ON deployments(status);
CREATE INDEX idx_deployments_started   ON deployments(started_at DESC);
CREATE INDEX idx_deployments_incident  ON deployments(incident_id);

-- ============================================================
-- SERVICE HEALTH
-- ============================================================
CREATE TABLE service_health (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name        VARCHAR(200) NOT NULL,
    health_score        DECIMAL(5,2) NOT NULL DEFAULT 100.0,
    status              VARCHAR(20)  NOT NULL DEFAULT 'HEALTHY'
                        CHECK (status IN ('HEALTHY','DEGRADED','CRITICAL','UNKNOWN')),
    error_rate          DECIMAL(8,4) DEFAULT 0.0,
    p50_latency_ms      DECIMAL(10,2),
    p95_latency_ms      DECIMAL(10,2),
    p99_latency_ms      DECIMAL(10,2),
    request_rate_rps    DECIMAL(10,2),
    availability_pct    DECIMAL(5,2)  DEFAULT 100.0,
    cpu_usage_pct       DECIMAL(5,2),
    memory_usage_pct    DECIMAL(5,2),
    active_instances    INT           DEFAULT 1,
    evaluated_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sh_service     ON service_health(service_name);
CREATE INDEX idx_sh_evaluated   ON service_health(evaluated_at DESC);
CREATE INDEX idx_sh_status      ON service_health(status);

-- ============================================================
-- TRACES
-- ============================================================
CREATE TABLE traces (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trace_id        VARCHAR(64)  NOT NULL,
    span_id         VARCHAR(64)  NOT NULL,
    parent_span_id  VARCHAR(64),
    service_name    VARCHAR(200) NOT NULL,
    operation_name  VARCHAR(500) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OK' CHECK (status IN ('OK','ERROR','TIMEOUT')),
    duration_ms     BIGINT       NOT NULL DEFAULT 0,
    http_method     VARCHAR(10),
    http_url        VARCHAR(2000),
    http_status     INT,
    error_message   TEXT,
    attributes      JSONB        DEFAULT '{}',
    span_started_at TIMESTAMPTZ  NOT NULL,
    span_ended_at   TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_traces_trace_id  ON traces(trace_id);
CREATE INDEX idx_traces_service   ON traces(service_name);
CREATE INDEX idx_traces_status    ON traces(status);
CREATE INDEX idx_traces_started   ON traces(span_started_at DESC);

-- ============================================================
-- LOGS
-- ============================================================
CREATE TABLE logs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name    VARCHAR(200) NOT NULL,
    level           VARCHAR(10)  NOT NULL CHECK (level IN ('TRACE','DEBUG','INFO','WARN','ERROR','FATAL')),
    message         TEXT         NOT NULL,
    logger_name     VARCHAR(500),
    thread_name     VARCHAR(200),
    trace_id        VARCHAR(64),
    span_id         VARCHAR(64),
    exception_class VARCHAR(500),
    stack_trace     TEXT,
    fields          JSONB        DEFAULT '{}',
    anomaly_score   DECIMAL(5,4) DEFAULT 0.0,
    is_anomaly      BOOLEAN      NOT NULL DEFAULT FALSE,
    logged_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_logs_service    ON logs(service_name);
CREATE INDEX idx_logs_level      ON logs(level);
CREATE INDEX idx_logs_trace      ON logs(trace_id);
CREATE INDEX idx_logs_logged_at  ON logs(logged_at DESC);
CREATE INDEX idx_logs_anomaly    ON logs(is_anomaly) WHERE is_anomaly = TRUE;
CREATE INDEX idx_logs_message    ON logs USING GIN(to_tsvector('english', message));

-- ============================================================
-- RCA REPORTS
-- ============================================================
CREATE TABLE rca_reports (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    incident_id          UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    generated_by         VARCHAR(100) NOT NULL DEFAULT 'gpt-4o',
    root_causes          JSONB        NOT NULL DEFAULT '[]',
    contributing_factors JSONB        NOT NULL DEFAULT '[]',
    timeline_summary     TEXT,
    impact_summary       TEXT,
    recovery_actions     JSONB        NOT NULL DEFAULT '[]',
    prevention_recs      JSONB        NOT NULL DEFAULT '[]',
    confidence_score     DECIMAL(5,2) DEFAULT 0.0,
    prompt_tokens        INT          DEFAULT 0,
    completion_tokens    INT          DEFAULT 0,
    generation_ms        BIGINT       DEFAULT 0,
    status               VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED'
                         CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','FAILED')),
    error_message        TEXT,
    generated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rca_incident   ON rca_reports(incident_id);
CREATE INDEX idx_rca_generated  ON rca_reports(generated_at DESC);

-- ============================================================
-- TIMELINE EVENTS
-- ============================================================
CREATE TABLE timeline_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    incident_id     UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    event_type      VARCHAR(50)  NOT NULL
                    CHECK (event_type IN ('LOG','ALERT','DEPLOYMENT','TRACE','INCIDENT_STATUS','METRIC','RCA')),
    service_name    VARCHAR(200),
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    severity        VARCHAR(20),
    source_id       UUID,
    metadata        JSONB        DEFAULT '{}',
    occurred_at     TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_timeline_incident  ON timeline_events(incident_id);
CREATE INDEX idx_timeline_occurred  ON timeline_events(occurred_at);
CREATE INDEX idx_timeline_type      ON timeline_events(event_type);
CREATE INDEX idx_timeline_service   ON timeline_events(service_name);

-- ============================================================
-- AUDIT LOG
-- ============================================================
CREATE TABLE audit_logs (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID REFERENCES users(id),
    username     VARCHAR(100),
    action       VARCHAR(100) NOT NULL,
    resource     VARCHAR(100) NOT NULL,
    resource_id  VARCHAR(100),
    old_value    JSONB,
    new_value    JSONB,
    ip_address   INET,
    user_agent   TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user     ON audit_logs(user_id);
CREATE INDEX idx_audit_resource ON audit_logs(resource, resource_id);
CREATE INDEX idx_audit_created  ON audit_logs(created_at DESC);
