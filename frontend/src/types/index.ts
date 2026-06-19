export interface Incident {
  id: string;
  title: string;
  description?: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'OPEN' | 'INVESTIGATING' | 'MITIGATED' | 'RESOLVED' | 'CLOSED';
  affectedServices: string[];
  alertCount: number;
  errorCount: number;
  impactedUsers: number;
  mttrSeconds?: number;
  detectedAt: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface RcaReport {
  id: string;
  incidentId: string;
  generatedBy: string;
  rootCauses: RootCause[];
  contributingFactors: string[];
  timelineSummary: string;
  impactSummary: string;
  recoveryActions: RecoveryAction[];
  preventionRecs: string[];
  confidenceScore: number;
  generationMs: number;
  status: string;
  generatedAt: string;
}

export interface RootCause {
  category: 'DEPLOYMENT' | 'CODE_BUG' | 'INFRA' | 'DEPENDENCY' | 'CONFIG';
  description: string;
  evidence: string;
  confidence: number;
}

export interface RecoveryAction {
  priority: number;
  action: string;
  rationale: string;
  owner: string;
  estimatedTime: string;
}

export interface ServiceHealth {
  id: string;
  serviceName: string;
  healthScore: number;
  status: 'HEALTHY' | 'DEGRADED' | 'CRITICAL' | 'UNKNOWN';
  errorRate: number;
  p99LatencyMs: number;
  availabilityPct: number;
  cpuUsagePct: number;
  memoryUsagePct: number;
  requestRateRps: number;
  activeInstances: number;
  evaluatedAt: string;
}

export interface Alert {
  id: string;
  incidentId?: string;
  serviceName: string;
  alertName: string;
  severity: string;
  status: string;
  message: string;
  firedAt: string;
  resolvedAt?: string;
}

export interface Deployment {
  id: string;
  serviceName: string;
  version: string;
  environment: string;
  status: string;
  deployedBy: string;
  commitSha: string;
  riskScore: number;
  startedAt: string;
  completedAt?: string;
}

export interface TimelineEvent {
  id: string;
  incidentId: string;
  eventType: string;
  serviceName?: string;
  title: string;
  description?: string;
  severity?: string;
  occurredAt: string;
}

export interface DashboardSummary {
  totalIncidents: number;
  activeIncidents: number;
  criticalIncidents: number;
  highIncidents: number;
  avgMttrHours: number;
  incidentsByService: Record<string, number>;
  incidentsByStatus: Record<string, number>;
  incidentsBySeverity: Record<string, number>;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  errorCode?: string;
  errors?: string[];
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
