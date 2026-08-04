import axios, { type AxiosError } from "axios";
import { getAuthToken, setAuthToken } from "./authToken";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1",
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Called when a request is rejected because the stored token is no longer
 * good. AuthProvider registers itself here so React state clears with the
 * token — this module cannot reach into the provider directly.
 */
let onSessionExpired: (() => void) | null = null;

export function setSessionExpiredHandler(handler: (() => void) | null): void {
  onSessionExpired = handler;
}

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const status = error.response?.status;
    // A 401 from /auth/login means those credentials were wrong, which the
    // sign-in form reports itself. Only a 401 on a request that actually
    // carried a token means the session has ended — because the token
    // expired, or because the server's signing key changed under it.
    const isSignInAttempt = (error.config?.url ?? "").startsWith("/auth/");

    if (status === 401 && !isSignInAttempt && getAuthToken()) {
      setAuthToken(null);
      onSessionExpired?.();
    }

    return Promise.reject(error);
  },
);