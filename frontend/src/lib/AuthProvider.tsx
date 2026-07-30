import { useMemo, useState, type ReactNode } from "react";
import * as authApi from "../api/auth";
import { setAuthToken } from "./authToken";
import {
  AuthContext,
  USER_STORAGE_KEY,
  type AuthContextValue,
  type AuthUser,
} from "./auth";
import type { AuthResponseDto } from "../types/auth";

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

  const applyAuthResponse = (response: AuthResponseDto) => {
    const nextUser: AuthUser = {
      userId: response.userId,
      email: response.email,
      firstName: response.firstName,
      lastName: response.lastName,
      role: response.role,
      preferredCurrency: response.preferredCurrency ?? "EUR",
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
      updateStoredUser: (patch) => {
        setUser((prev) => {
          if (!prev) return prev;
          const next = { ...prev, ...patch };
          localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(next));
          return next;
        });
      },
    }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
