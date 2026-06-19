import { apiClient } from './client';
import type {
  ApiResponse, PageResponse,
  Incident, RcaReport,
  ServiceHealth, Alert,
  Deployment, TimelineEvent,
  DashboardSummary
} from '../types';

// ─── Incidents ───────────────────────────────────────────
export const incidentApi = {
  list: (page = 0, size = 20) =>
    apiClient.get<ApiResponse<PageResponse<Incident>>>(`/api/v1/incidents?page=${page}&size=${size}`),

  getById: (id: string) =>
    apiClient.get<ApiResponse<Incident>>(`/api/v1/incidents/${id}`),

  create: (payload: { title: string; severity: string; affectedServices: string[]; description?: string }) =>
    apiClient.post<ApiResponse<Incident>>('/api/v1/incidents', payload),

  acknowledge: (id: string) =>
    apiClient.post<ApiResponse<Incident>>(`/api/v1/incidents/${id}/acknowledge`),

  resolve: (id: string) =>
    apiClient.post<ApiResponse<Incident>>(`/api/v1/incidents/${id}/resolve`),

  close: (id: string) =>
    apiClient.post<ApiResponse<Incident>>(`/api/v1/incidents/${id}/close`),

  getTimeline: (id: string) =>
    apiClient.get<ApiResponse<TimelineEvent[]>>(`/api/v1/incidents/${id}/timeline`),

  getDashboardSummary: () =>
    apiClient.get<ApiResponse<DashboardSummary>>('/api/v1/incidents/dashboard/summary'),
};

// ─── RCA ─────────────────────────────────────────────────
export const rcaApi = {
  getLatest: (incidentId: string) =>
    apiClient.get<ApiResponse<RcaReport>>(`/api/v1/rca/incidents/${incidentId}`),

  getAll: (incidentId: string) =>
    apiClient.get<ApiResponse<RcaReport[]>>(`/api/v1/rca/incidents/${incidentId}/all`),
};

// ─── Service Health ───────────────────────────────────────
export const healthApi = {
  getAll: () =>
    apiClient.get<ApiResponse<ServiceHealth[]>>('/api/v1/health'),

  getByService: (serviceName: string) =>
    apiClient.get<ApiResponse<ServiceHealth>>(`/api/v1/health/${serviceName}`),
};

// ─── Deployments ─────────────────────────────────────────
export const deploymentApi = {
  list: () =>
    apiClient.get<ApiResponse<Deployment[]>>('/api/v1/deployments'),
};

// ─── Alerts ──────────────────────────────────────────────
export const alertApi = {
  list: (status = 'FIRING') =>
    apiClient.get<ApiResponse<Alert[]>>(`/api/v1/alerts?status=${status}`),
};

// ─── Auth ─────────────────────────────────────────────────
export const authApi = {
  login: (email: string, password: string) =>
    apiClient.post<ApiResponse<{ token: string; userId: string; username: string; role: string }>>(
      '/api/v1/auth/login', { email, password }
    ),

  register: (payload: { username: string; email: string; password: string; role?: string }) =>
    apiClient.post('/api/v1/auth/register', payload),
};
