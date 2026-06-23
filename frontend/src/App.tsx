import React, { useState } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import { Box, TextField, Button, Typography, Paper } from '@mui/material';
import { AppLayout } from './components/layout/AppLayout';
import { DashboardPage } from './features/dashboard/DashboardPage';
import { IncidentsPage } from './features/incidents/IncidentsPage';
import { RcaPage } from './features/rca/RcaPage';
import { HealthPage } from './features/health/HealthPage';
import { DeploymentsPage } from './features/deployments/DeploymentsPage';
import { AlertsPage } from './features/alerts/AlertsPage';
import { useAuthStore } from './store/authStore';
import { authApi } from './api';

const darkTheme = createTheme({
  palette: {
    mode: 'dark',
    primary:   { main: '#90caf9' },
    secondary: { main: '#ce93d8' },
    background: { default: '#0f0f1a', paper: '#1e1e3a' },
  },
  typography: { fontFamily: '"Inter", "Roboto", sans-serif' },
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 10_000, retry: 2 },
    mutations: { retry: 1 },
  },
});

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuthStore();
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuthStore();
  const navigate = useNavigate();

  const handleLogin = async () => {
    try {
      const res = await authApi.login(email, password);
      const { token, userId, username, role } = res.data.data;
      login(token, userId, username, role);
      navigate('/');
    } catch {
      setError('Invalid credentials');
    }
  };

  return (
    <Box minHeight="100vh" display="flex" alignItems="center" justifyContent="center" bgcolor="#0f0f1a">
      <Paper sx={{ p: 4, bgcolor: '#1e1e3a', minWidth: 360 }}>
        <Typography variant="h5" color="white" fontWeight={700} mb={3} textAlign="center">
          🛡️ IIMP Login
        </Typography>
        <Box display="flex" flexDirection="column" gap={2}>
          <TextField label="Email" type="email" value={email} onChange={e => setEmail(e.target.value)}
            InputLabelProps={{ sx: { color: 'grey.400' } }} InputProps={{ sx: { color: 'white' } }} fullWidth />
          <TextField label="Password" type="password" value={password} onChange={e => setPassword(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleLogin()}
            InputLabelProps={{ sx: { color: 'grey.400' } }} InputProps={{ sx: { color: 'white' } }} fullWidth />
          {error && <Typography color="error" variant="caption">{error}</Typography>}
          <Button variant="contained" color="primary" onClick={handleLogin} fullWidth>Sign In</Button>
        </Box>
      </Paper>
    </Box>
  );
};

export const App: React.FC = () => (
  <QueryClientProvider client={queryClient}>
    <ThemeProvider theme={darkTheme}>
      <CssBaseline />
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
            <Route index element={<DashboardPage />} />
            <Route path="incidents" element={<IncidentsPage />} />
            <Route path="rca" element={<RcaPage />} />
            <Route path="health" element={<HealthPage />} />
            <Route path="deployments" element={<DeploymentsPage />} />
            <Route path="alerts" element={<AlertsPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
    {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
  </QueryClientProvider>
);

export default App;
