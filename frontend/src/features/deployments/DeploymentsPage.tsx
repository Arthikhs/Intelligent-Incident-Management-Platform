import React from 'react';
import {
  Box, Typography, Paper, Chip, CircularProgress,
  Table, TableBody, TableCell, TableHead, TableRow, LinearProgress
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { deploymentApi } from '../../api';
import type { Deployment } from '../../types';
import dayjs from 'dayjs';

const STATUS_COLOR: Record<string, string> = {
  SUCCESS: '#4caf50', FAILED: '#f44336', IN_PROGRESS: '#2196f3', ROLLED_BACK: '#ff9800',
};

const RiskBar: React.FC<{ value: number }> = ({ value }) => (
  <Box display="flex" alignItems="center" gap={1} minWidth={100}>
    <LinearProgress
      variant="determinate"
      value={value * 100}
      sx={{
        flex: 1, height: 6, borderRadius: 3,
        '& .MuiLinearProgress-bar': {
          bgcolor: value > 0.7 ? '#f44336' : value > 0.4 ? '#ff9800' : '#4caf50',
        },
      }}
    />
    <Typography variant="caption" color="grey.400">{(value * 100).toFixed(0)}%</Typography>
  </Box>
);

export const DeploymentsPage: React.FC = () => {
  const { data, isLoading } = useQuery<Deployment[]>({
    queryKey: ['deployments'],
    queryFn: () => deploymentApi.list().then(r => r.data.data),
    refetchInterval: 30_000,
  });

  if (isLoading) return <Box display="flex" justifyContent="center" mt={8}><CircularProgress /></Box>;

  const deployments = data ?? [];

  return (
    <Box>
      <Typography variant="h5" color="white" fontWeight={700} mb={3}>🚀 Deployments</Typography>

      <Paper sx={{ bgcolor: '#1e1e3a', overflow: 'auto' }}>
        <Table>
          <TableHead>
            <TableRow sx={{ '& th': { color: '#90caf9', fontWeight: 700, borderColor: '#333' } }}>
              <TableCell>Service</TableCell>
              <TableCell>Version</TableCell>
              <TableCell>Environment</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Risk Score</TableCell>
              <TableCell>Deployed By</TableCell>
              <TableCell>Commit</TableCell>
              <TableCell>Started</TableCell>
              <TableCell>Completed</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {deployments.map((dep) => (
              <TableRow key={dep.id} sx={{ '& td': { color: 'white', borderColor: '#2a2a4a' } }}>
                <TableCell>
                  <Typography variant="body2" fontWeight={600}>{dep.serviceName}</Typography>
                </TableCell>
                <TableCell>
                  <Chip label={dep.version} size="small" sx={{ bgcolor: '#2a2a5a', color: 'white', fontSize: 11 }} />
                </TableCell>
                <TableCell>
                  <Chip label={dep.environment} size="small"
                    sx={{ bgcolor: dep.environment === 'production' ? '#4a1a1a' : '#1a3a1a', color: 'white', fontSize: 11 }} />
                </TableCell>
                <TableCell>
                  <Chip
                    label={dep.status}
                    size="small"
                    sx={{ bgcolor: (STATUS_COLOR[dep.status] ?? '#9e9e9e') + '33', color: STATUS_COLOR[dep.status] ?? '#9e9e9e', fontWeight: 700 }}
                  />
                </TableCell>
                <TableCell><RiskBar value={dep.riskScore ?? 0} /></TableCell>
                <TableCell sx={{ fontSize: 12 }}>{dep.deployedBy ?? '—'}</TableCell>
                <TableCell sx={{ fontSize: 11, fontFamily: 'monospace' }}>
                  {dep.commitSha ? dep.commitSha.substring(0, 8) : '—'}
                </TableCell>
                <TableCell sx={{ fontSize: 12 }}>{dayjs(dep.startedAt).format('MM/DD HH:mm')}</TableCell>
                <TableCell sx={{ fontSize: 12 }}>{dep.completedAt ? dayjs(dep.completedAt).format('MM/DD HH:mm') : '—'}</TableCell>
              </TableRow>
            ))}
            {deployments.length === 0 && (
              <TableRow>
                <TableCell colSpan={9} align="center" sx={{ color: 'grey.500', py: 4 }}>
                  No deployments recorded yet
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </Box>
  );
};
