import React from 'react';
import { Grid, Paper, Typography, Chip, Box, CircularProgress } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import ReactECharts from 'echarts-for-react';
import { incidentApi } from '../../api';

export const DashboardPage: React.FC = () => {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: () => incidentApi.getDashboardSummary().then(r => r.data.data),
    refetchInterval: 30_000,
  });

  if (isLoading) return <Box display="flex" justifyContent="center" mt={8}><CircularProgress /></Box>;
  if (!data) return null;

  const severityChartOption = {
    tooltip: { trigger: 'item' },
    series: [{
      name: 'By Severity',
      type: 'pie',
      radius: ['40%', '70%'],
      data: Object.entries(data.incidentsBySeverity).map(([name, value]) => ({ name, value })),
      color: ['#f44336', '#ff9800', '#2196f3', '#4caf50'],
    }],
  };

  const statusChartOption = {
    tooltip: { trigger: 'item' },
    series: [{
      name: 'By Status',
      type: 'pie',
      radius: '60%',
      data: Object.entries(data.incidentsByStatus).map(([name, value]) => ({ name, value })),
    }],
  };

  const serviceBarOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: Object.keys(data.incidentsByService) },
    series: [{
      type: 'bar',
      data: Object.values(data.incidentsByService),
      itemStyle: { color: '#2196f3' },
    }],
  };

  return (
    <Box>
      <Typography variant="h5" color="white" mb={3} fontWeight={700}>
        📊 Incident Dashboard
      </Typography>

      {/* KPI Cards */}
      <Grid container spacing={2} mb={3}>
        {[
          { label: 'Total Incidents',  value: data.totalIncidents,    color: '#2196f3' },
          { label: 'Active Incidents', value: data.activeIncidents,   color: '#ff9800' },
          { label: 'Critical',         value: data.criticalIncidents, color: '#f44336' },
          { label: 'High',             value: data.highIncidents,     color: '#ff5722' },
          { label: 'Avg MTTR (hrs)',   value: data.avgMttrHours.toFixed(1), color: '#4caf50' },
        ].map(({ label, value, color }) => (
          <Grid item xs={12} sm={6} md={2.4} key={label}>
            <Paper sx={{ p: 2.5, bgcolor: '#1e1e3a', border: `1px solid ${color}33`, textAlign: 'center' }}>
              <Typography variant="h4" fontWeight={800} sx={{ color }}>{value}</Typography>
              <Typography variant="caption" color="grey.400">{label}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      {/* Charts */}
      <Grid container spacing={2}>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2, bgcolor: '#1e1e3a' }}>
            <Typography variant="subtitle1" color="white" mb={1}>Incidents by Severity</Typography>
            <ReactECharts option={severityChartOption} style={{ height: 250 }} />
          </Paper>
        </Grid>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2, bgcolor: '#1e1e3a' }}>
            <Typography variant="subtitle1" color="white" mb={1}>Incidents by Status</Typography>
            <ReactECharts option={statusChartOption} style={{ height: 250 }} />
          </Paper>
        </Grid>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2, bgcolor: '#1e1e3a' }}>
            <Typography variant="subtitle1" color="white" mb={1}>Incidents by Service</Typography>
            <ReactECharts option={serviceBarOption} style={{ height: 250 }} />
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
};
