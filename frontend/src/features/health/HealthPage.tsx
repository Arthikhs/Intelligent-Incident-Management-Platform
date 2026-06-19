import React from 'react';
import { Box, Typography, Paper, Grid, LinearProgress, CircularProgress, Chip } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import ReactECharts from 'echarts-for-react';
import { healthApi } from '../../api';
import type { ServiceHealth } from '../../types';

const STATUS_COLOR: Record<string, string> = {
  HEALTHY: '#4caf50', DEGRADED: '#ff9800', CRITICAL: '#f44336', UNKNOWN: '#9e9e9e',
};

const HealthGauge: React.FC<{ score: number; service: string; status: string }> = ({ score, service, status }) => {
  const option = {
    series: [{
      type: 'gauge',
      min: 0, max: 100,
      axisLine: {
        lineStyle: {
          width: 12,
          color: [[0.6, '#f44336'], [0.85, '#ff9800'], [1, '#4caf50']],
        },
      },
      pointer: { itemStyle: { color: 'auto' } },
      detail: { valueAnimation: true, formatter: '{value}', color: 'white', fontSize: 18, fontWeight: 700 },
      data: [{ value: Math.round(score), name: service }],
      title: { color: '#90caf9', fontSize: 11 },
    }],
  };
  return <ReactECharts option={option} style={{ height: 180 }} />;
};

export const HealthPage: React.FC = () => {
  const { data: services, isLoading } = useQuery<ServiceHealth[]>({
    queryKey: ['service-health'],
    queryFn: () => healthApi.getAll().then(r => r.data.data),
    refetchInterval: 15_000,
  });

  if (isLoading) return <Box display="flex" justifyContent="center" mt={8}><CircularProgress /></Box>;

  return (
    <Box>
      <Typography variant="h5" color="white" fontWeight={700} mb={3}>💓 Service Health</Typography>
      <Grid container spacing={2}>
        {(services ?? []).map((svc) => (
          <Grid item xs={12} sm={6} md={4} lg={3} key={svc.id}>
            <Paper sx={{ p: 2, bgcolor: '#1e1e3a', border: `1px solid ${STATUS_COLOR[svc.status]}44` }}>
              <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                <Typography variant="subtitle2" color="white" fontWeight={700}>{svc.serviceName}</Typography>
                <Chip label={svc.status} size="small" sx={{ bgcolor: STATUS_COLOR[svc.status] + '33', color: STATUS_COLOR[svc.status], fontWeight: 700 }} />
              </Box>

              <HealthGauge score={svc.healthScore} service={svc.serviceName} status={svc.status} />

              <Box mt={1} display="flex" flexDirection="column" gap={0.5}>
                {[
                  { label: 'Error Rate',   value: `${(svc.errorRate * 100).toFixed(2)}%` },
                  { label: 'P99 Latency',  value: `${svc.p99LatencyMs?.toFixed(0)}ms` },
                  { label: 'Availability', value: `${svc.availabilityPct?.toFixed(2)}%` },
                  { label: 'CPU',          value: `${svc.cpuUsagePct?.toFixed(1)}%` },
                  { label: 'Memory',       value: `${svc.memoryUsagePct?.toFixed(1)}%` },
                  { label: 'Instances',    value: `${svc.activeInstances}` },
                ].map(({ label, value }) => (
                  <Box key={label} display="flex" justifyContent="space-between">
                    <Typography variant="caption" color="grey.500">{label}</Typography>
                    <Typography variant="caption" color="grey.300">{value}</Typography>
                  </Box>
                ))}
              </Box>
            </Paper>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
};
