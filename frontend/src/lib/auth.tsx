import {
  createContext,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import * as authApi from "../api/auth";
import { setAuthToken } from "./authToken";
import type { LoginRequestDto, RegisterRequestDto } from "../types/auth";

const USER_STORAGE_KEY = "skyair-auth-user";

export interface AuthUser {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (request: LoginRequestDto) => Promise<void>;
  register: (request: RegisterRequestDto) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function readStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(readStoredUser);

  const applyAuthResponse = (response: {
    token: string;
    userId: number;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
  }) => {
    const nextUser: AuthUser = {
      userId: response.userId,
      email: response.email,
      firstName: response.firstName,
      lastName: response.lastName,
      role: response.role,
    };
    setAuthToken(response.token);
    localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(nextUser));
    setUser(nextUser);
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      login: async (request) => {
        const response = await authApi.login(request);
        applyAuthResponse(response);
      },
      register: async (request) => {
        const response = await authApi.register(request);
        applyAuthResponse(response);
      },
      logout: () => {
        setAuthToken(null);
        localStorage.removeItem(USER_STORAGE_KEY);
        setUser(null);
      },
    }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
