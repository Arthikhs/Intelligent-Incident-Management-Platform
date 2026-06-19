import React from 'react';
import {
  AppBar, Toolbar, Typography, Drawer, List,
  ListItemButton, ListItemIcon, ListItemText,
  Box, IconButton, Chip
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  Warning as IncidentIcon,
  Psychology as RcaIcon,
  MonitorHeart as HealthIcon,
  RocketLaunch as DeployIcon,
  Notifications as AlertIcon,
  Logout as LogoutIcon,
} from '@mui/icons-material';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

const DRAWER_WIDTH = 240;

const NAV_ITEMS = [
  { label: 'Dashboard',   icon: <DashboardIcon />, path: '/' },
  { label: 'Incidents',   icon: <IncidentIcon />,  path: '/incidents' },
  { label: 'RCA Reports', icon: <RcaIcon />,       path: '/rca' },
  { label: 'Health',      icon: <HealthIcon />,    path: '/health' },
  { label: 'Deployments', icon: <DeployIcon />,    path: '/deployments' },
  { label: 'Alerts',      icon: <AlertIcon />,     path: '/alerts' },
];

const ROLE_COLORS: Record<string, 'error' | 'warning' | 'info' | 'default'> = {
  ADMIN: 'error', SRE: 'warning', DEVELOPER: 'info', VIEWER: 'default',
};

export const AppLayout: React.FC = () => {
  const navigate  = useNavigate();
  const location  = useLocation();
  const { username, role, logout } = useAuthStore();

  return (
    <Box sx={{ display: 'flex' }}>
      <AppBar position="fixed" sx={{ zIndex: (t) => t.zIndex.drawer + 1, bgcolor: '#1a1a2e' }}>
        <Toolbar sx={{ justifyContent: 'space-between' }}>
          <Typography variant="h6" fontWeight={700} letterSpacing={1}>
            🛡️ IIMP — Incident Management
          </Typography>
          <Box display="flex" alignItems="center" gap={2}>
            <Typography variant="body2">{username}</Typography>
            <Chip label={role} color={ROLE_COLORS[role ?? ''] ?? 'default'} size="small" />
            <IconButton color="inherit" onClick={logout} title="Logout">
              <LogoutIcon />
            </IconButton>
          </Box>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          '& .MuiDrawer-paper': {
            width: DRAWER_WIDTH,
            bgcolor: '#16213e',
            color: '#fff',
            mt: '64px',
          },
        }}
      >
        <List>
          {NAV_ITEMS.map(({ label, icon, path }) => (
            <ListItemButton
              key={path}
              selected={location.pathname === path}
              onClick={() => navigate(path)}
              sx={{ '&.Mui-selected': { bgcolor: 'rgba(255,255,255,0.1)' } }}
            >
              <ListItemIcon sx={{ color: '#90caf9', minWidth: 36 }}>{icon}</ListItemIcon>
              <ListItemText primary={label} primaryTypographyProps={{ fontSize: 14 }} />
            </ListItemButton>
          ))}
        </List>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: 3, mt: '64px', ml: `${DRAWER_WIDTH}px`, minHeight: '100vh', bgcolor: '#0f0f1a' }}>
        <Outlet />
      </Box>
    </Box>
  );
};
