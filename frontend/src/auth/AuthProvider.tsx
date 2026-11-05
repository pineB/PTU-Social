import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { login as loginRequest, type TokenResponse, type UserRole } from '../api/auth';
import { getCurrentUser, type CurrentUser } from '../api/profile';

const sessionStorageKey = 'ptu-social-auth-session';

export interface AuthSession extends TokenResponse {
  email: string;
  expiresAt: number;
}

interface AuthContextValue {
  session: AuthSession | null;
  currentUser: CurrentUser | null;
  profileLoading: boolean;
  profileError: string;
  login: (email: string, password: string) => Promise<UserRole>;
  authenticate: (token: TokenResponse, email: string) => UserRole;
  refreshProfile: () => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function isAuthSession(value: unknown): value is AuthSession {
  if (typeof value !== 'object' || value === null) return false;
  const session = value as Partial<AuthSession>;
  return typeof session.accessToken === 'string'
    && session.tokenType === 'Bearer'
    && typeof session.expiresInSeconds === 'number'
    && typeof session.userId === 'string'
    && ['STUDENT', 'TEACHER', 'ORGANIZER', 'ACADEMIC_OFFICE'].includes(session.role ?? '')
    && typeof session.email === 'string'
    && typeof session.expiresAt === 'number'
    && session.expiresAt > Date.now();
}

function readStoredSession(): AuthSession | null {
  const stored = window.sessionStorage.getItem(sessionStorageKey);
  if (!stored) return null;

  try {
    const parsed: unknown = JSON.parse(stored);
    if (isAuthSession(parsed)) return parsed;
  } catch {
    window.sessionStorage.removeItem(sessionStorageKey);
    return null;
  }

  window.sessionStorage.removeItem(sessionStorageKey);
  return null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(readStoredSession);
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState('');
  const profileRequestId = useRef(0);

  const logout = useCallback(() => {
    profileRequestId.current += 1;
    window.sessionStorage.removeItem(sessionStorageKey);
    setSession(null);
    setCurrentUser(null);
    setProfileError('');
  }, []);

  const authenticate = useCallback((token: TokenResponse, email: string) => {
    const nextSession: AuthSession = {
      ...token,
      email: email.trim(),
      expiresAt: Date.now() + token.expiresInSeconds * 1000,
    };
    window.sessionStorage.setItem(sessionStorageKey, JSON.stringify(nextSession));
    setSession(nextSession);
    return token.role;
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const token = await loginRequest({ email: email.trim(), password });
    return authenticate(token, email);
  }, [authenticate]);

  const refreshProfile = useCallback(async () => {
    const requestId = ++profileRequestId.current;
    if (!session) {
      setCurrentUser(null);
      setProfileLoading(false);
      return;
    }

    setProfileLoading(true);
    setProfileError('');
    try {
      const user = await getCurrentUser();
      if (profileRequestId.current === requestId) setCurrentUser(user);
    } catch (error: unknown) {
      if (profileRequestId.current === requestId) {
        setProfileError(error instanceof Error ? error.message : 'Не удалось загрузить профиль.');
      }
    } finally {
      if (profileRequestId.current === requestId) setProfileLoading(false);
    }
  }, [session]);

  useEffect(() => {
    if (session) {
      void refreshProfile();
    } else {
      setCurrentUser(null);
      setProfileLoading(false);
    }
  }, [refreshProfile, session]);

  useEffect(() => {
    const handleExpired = () => logout();
    window.addEventListener('app:auth-expired', handleExpired);
    return () => window.removeEventListener('app:auth-expired', handleExpired);
  }, [logout]);

  useEffect(() => {
    if (!session) return;
    const timeout = window.setTimeout(logout, Math.max(0, session.expiresAt - Date.now()));
    return () => window.clearTimeout(timeout);
  }, [logout, session]);

  const value = useMemo(() => ({
    session,
    currentUser,
    profileLoading,
    profileError,
    login,
    authenticate,
    refreshProfile,
    logout,
  }), [session, currentUser, profileLoading, profileError, login, authenticate, refreshProfile, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
