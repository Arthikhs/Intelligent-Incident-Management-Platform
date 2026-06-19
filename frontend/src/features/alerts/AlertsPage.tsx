import React, { useState } from 'react';
import {
  Box, Typography, Paper, Chip, CircularProgress,
  Table, TableBody, TableCell, TableHead, TableRow,
  ToggleButtonGroup, ToggleButton
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { alertApi } from '../../api';
import type { Alert } from '../../types';
import dayjs from 'dayjs';

const SEVERITY_COLOR: Record<string, 'error' | 'warning' | 'info' | 'success' | 'default'> = {
  CRITICAL: 'error', HIGH: 'warning', MEDIUM: 'info', LOW: 'success', INFO: 'default',
};

const STATUS_STYLE: Record<string, { bg: string; color: string }> = {
  FIRING:   { bg: '#f4433622', color: '#f44336' },
  RESOLVED: { bg: '#4caf5022', color: '#4caf50' },
  SILENCED: { bg: '#9e9e9e22', color: '#9e9e9e' },
};

export const AlertsPage: React.FC = () => {
  const [statusFilter, setStatusFilter] = useState<string>('FIRING');

  const { data, isLoading } = useQuery<Alert[]>({
    queryKey: ['alerts', statusFilter],
    queryFn: () => alertApi.list(statusFilter).then(r => r.data.data),
    refetchInterval: 15_000,
  });

  const alerts = data ?? [];

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" color="white" fontWeight={700}>🔔 Alerts</Typography>
        <ToggleButtonGroup
          value={statusFilter}
          exclusive
          onChange={(_, v) => v && setStatusFilter(v)}
          size="small"
          sx={{ '& .MuiToggleButton-root': { color: 'grey.400', borderColor: '#444' },
                '& .Mui-selected': { color: 'white', bgcolor: '#2a2a5a !important' } }}
        >
          <ToggleButton value="FIRING">Firing</ToggleButton>
          <ToggleButton value="RESOLVED">Resolved</ToggleButton>
          <ToggleButton value="ALL">All</ToggleButton>
        </ToggleButtonGroup>
      </Box>

      {isLoading ? (
        <Box display="flex" justifyContent="center" mt={8}><CircularProgress /></Box>
      ) : (
        <Paper sx={{ bgcolor: '#1e1e3a', overflow: 'auto' }}>
          <Table>
            <TableHead>
              <TableRow sx={{ '& th': { color: '#90caf9', fontWeight: 700, borderColor: '#333' } }}>
                <TableCell>Service</TableCell>
                <TableCell>Alert</TableCell>
                <TableCell>Severity</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Message</TableCell>
                <TableCell>Fired At</TableCell>
                <TableCell>Resolved At</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {alerts.map((alert) => (
                <TableRow key={alert.id} sx={{ '& td': { color: 'white', borderColor: '#2a2a4a' } }}>
                  <TableCell>
                    <Typography variant="body2" fontWeight={600}>{alert.serviceName}</Typography>
                  </TableCell>
                  <TableCell sx={{ fontSize: 12, fontFamily: 'monospace' }}>{alert.alertName}</TableCell>
                  <TableCell>
                    <Chip label={alert.severity} color={SEVERITY_COLOR[alert.severity] ?? 'default'} size="small" />
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={alert.status}
                      size="small"
                      sx={{
                        bgcolor: STATUS_STYLE[alert.status]?.bg ?? '#9e9e9e22',
                        color:   STATUS_STYLE[alert.status]?.color ?? '#9e9e9e',
                        fontWeight: 700,
                      }}
                    />
                  </TableCell>
                  <TableCell sx={{ fontSize: 11, maxWidth: 300 }}>
                    <Typography variant="caption" color="grey.400" noWrap title={alert.message}>
                      {alert.message}
                    </Typography>
                  </TableCell>
                  <TableCell sx={{ fontSize: 12 }}>{dayjs(alert.firedAt).format('MM/DD HH:mm')}</TableCell>
                  <TableCell sx={{ fontSize: 12 }}>{alert.resolvedAt ? dayjs(alert.resolvedAt).format('MM/DD HH:mm') : '—'}</TableCell>
                </TableRow>
              ))}
              {alerts.length === 0 && (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ color: 'grey.500', py: 4 }}>
                    No {statusFilter.toLowerCase()} alerts
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </Paper>
      )}
    </Box>
  );
};
