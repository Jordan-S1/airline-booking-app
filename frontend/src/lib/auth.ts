import { createContext, useContext } from "react";
import type { LoginRequestDto, RegisterRequestDto } from "../types/auth";

export const USER_STORAGE_KEY = "skyair-auth-user";

export interface AuthUser {
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  preferredCurrency: string;
}

export interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  login: (request: LoginRequestDto) => Promise<void>;
  register: (request: RegisterRequestDto) => Promise<void>;
  logout: () => void;
  /** Patch the cached user after a profile update, keeping local state in sync. */
  updateStoredUser: (patch: Partial<AuthUser>) => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}

/**
 * Whether the signed-in user holds the ADMIN role.
 *
 * This decides what the UI offers, not what it permits — every admin endpoint
 * is guarded server-side with `@PreAuthorize`, and a token that isn't an
 * admin's is rejected there regardless of what the browser chooses to render.
 * Treat this as a way to avoid showing people doors they cannot open.
 */
export function isAdmin(user: AuthUser | null): boolean {
  return user?.role?.toUpperCase() === "ADMIN";
}
