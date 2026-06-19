import React, { useState } from 'react';
import {
  Box, Typography, Paper, Chip, Button, CircularProgress,
  Table, TableBody, TableCell, TableHead, TableRow,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField, Select, MenuItem, FormControl, InputLabel
} from '@mui/material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { incidentApi } from '../../api';
import type { Incident } from '../../types';
import dayjs from 'dayjs';

const SEVERITY_COLOR: Record<string, 'error' | 'warning' | 'info' | 'success'> = {
  CRITICAL: 'error', HIGH: 'warning', MEDIUM: 'info', LOW: 'success',
};

const STATUS_COLOR: Record<string, string> = {
  OPEN: '#f44336', INVESTIGATING: '#ff9800', MITIGATED: '#2196f3',
  RESOLVED: '#4caf50', CLOSED: '#9e9e9e',
};

export const IncidentsPage: React.FC = () => {
  const qc = useQueryClient();
  const [createOpen, setCreateOpen] = useState(false);
  const [form, setForm] = useState({ title: '', severity: 'HIGH', affectedServices: '', description: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['incidents'],
    queryFn: () => incidentApi.list().then(r => r.data.data),
    refetchInterval: 15_000,
  });

  const createMutation = useMutation({
    mutationFn: () => incidentApi.create({
      title: form.title,
      severity: form.severity,
      affectedServices: form.affectedServices.split(',').map(s => s.trim()),
      description: form.description,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['incidents'] }); setCreateOpen(false); },
  });

  const ackMutation  = useMutation({ mutationFn: (id: string) => incidentApi.acknowledge(id), onSuccess: () => qc.invalidateQueries({ queryKey: ['incidents'] }) });
  const resMutation  = useMutation({ mutationFn: (id: string) => incidentApi.resolve(id),    onSuccess: () => qc.invalidateQueries({ queryKey: ['incidents'] }) });

  if (isLoading) return <Box display="flex" justifyContent="center" mt={8}><CircularProgress /></Box>;

  const incidents: Incident[] = data?.content ?? [];

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" color="white" fontWeight={700}>🚨 Incidents</Typography>
        <Button variant="contained" color="error" onClick={() => setCreateOpen(true)}>
          + New Incident
        </Button>
      </Box>

      <Paper sx={{ bgcolor: '#1e1e3a', overflow: 'auto' }}>
        <Table>
          <TableHead>
            <TableRow sx={{ '& th': { color: '#90caf9', fontWeight: 700, borderColor: '#333' } }}>
              <TableCell>Title</TableCell>
              <TableCell>Severity</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Services</TableCell>
              <TableCell>Detected</TableCell>
              <TableCell>MTTR</TableCell>
              <TableCell>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {incidents.map((inc) => (
              <TableRow key={inc.id} sx={{ '& td': { color: 'white', borderColor: '#2a2a4a' } }}>
                <TableCell>
                  <Typography variant="body2" fontWeight={600}>{inc.title}</Typography>
                </TableCell>
                <TableCell>
                  <Chip label={inc.severity} color={SEVERITY_COLOR[inc.severity]} size="small" />
                </TableCell>
                <TableCell>
                  <Chip label={inc.status} size="small" sx={{ bgcolor: STATUS_COLOR[inc.status] + '33', color: STATUS_COLOR[inc.status] }} />
                </TableCell>
                <TableCell>
                  <Box display="flex" gap={0.5} flexWrap="wrap">
                    {inc.affectedServices.map(s => <Chip key={s} label={s} size="small" sx={{ bgcolor: '#2a2a5a', color: 'white', fontSize: 10 }} />)}
                  </Box>
                </TableCell>
                <TableCell sx={{ fontSize: 12 }}>{dayjs(inc.detectedAt).format('MM/DD HH:mm')}</TableCell>
                <TableCell sx={{ fontSize: 12 }}>{inc.mttrSeconds ? `${Math.round(inc.mttrSeconds / 60)}m` : '—'}</TableCell>
                <TableCell>
                  <Box display="flex" gap={1}>
                    {inc.status === 'OPEN' && (
                      <Button size="small" variant="outlined" color="warning" onClick={() => ackMutation.mutate(inc.id)}>ACK</Button>
                    )}
                    {['OPEN','INVESTIGATING','MITIGATED'].includes(inc.status) && (
                      <Button size="small" variant="outlined" color="success" onClick={() => resMutation.mutate(inc.id)}>RESOLVE</Button>
                    )}
                  </Box>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Paper>

      {/* Create Incident Dialog */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} PaperProps={{ sx: { bgcolor: '#1e1e3a', color: 'white' } }}>
        <DialogTitle>Create Incident</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important', minWidth: 400 }}>
          <TextField label="Title" value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
            InputLabelProps={{ sx: { color: 'grey.400' } }} InputProps={{ sx: { color: 'white' } }} fullWidth />
          <FormControl fullWidth>
            <InputLabel sx={{ color: 'grey.400' }}>Severity</InputLabel>
            <Select value={form.severity} onChange={e => setForm(f => ({ ...f, severity: e.target.value }))} sx={{ color: 'white' }}>
              {['CRITICAL','HIGH','MEDIUM','LOW'].map(s => <MenuItem key={s} value={s}>{s}</MenuItem>)}
            </Select>
          </FormControl>
          <TextField label="Affected Services (comma separated)" value={form.affectedServices}
            onChange={e => setForm(f => ({ ...f, affectedServices: e.target.value }))}
            InputLabelProps={{ sx: { color: 'grey.400' } }} InputProps={{ sx: { color: 'white' } }} fullWidth />
          <TextField label="Description" value={form.description} multiline rows={3}
            onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
            InputLabelProps={{ sx: { color: 'grey.400' } }} InputProps={{ sx: { color: 'white' } }} fullWidth />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)} color="inherit">Cancel</Button>
          <Button onClick={() => createMutation.mutate()} variant="contained" color="error" disabled={!form.title}>
            Create
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
