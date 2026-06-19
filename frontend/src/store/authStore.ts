import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
  token: string | null;
  userId: string | null;
  username: string | null;
  role: string | null;
  isAuthenticated: boolean;
  login: (token: string, userId: string, username: string, role: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      userId: null,
      username: null,
      role: null,
      isAuthenticated: false,

      login: (token, userId, username, role) => {
        localStorage.setItem('iimp_token', token);
        set({ token, userId, username, role, isAuthenticated: true });
      },

      logout: () => {
        localStorage.removeItem('iimp_token');
        set({ token: null, userId: null, username: null, role: null, isAuthenticated: false });
      },
    }),
    { name: 'iimp-auth' }
  )
);
