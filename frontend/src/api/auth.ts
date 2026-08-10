import { apiClient } from "../lib/apiClient";
import type {
  AuthResponseDto,
  LoginRequestDto,
  RegisterRequestDto,
} from "../types/auth";

export async function login(
  request: LoginRequestDto,
): Promise<AuthResponseDto> {
  const { data } = await apiClient.post<AuthResponseDto>(
    "/auth/login",
    request,
  );
  return data;
}

export async function register(
  request: RegisterRequestDto,
): Promise<AuthResponseDto> {
  const { data } = await apiClient.post<AuthResponseDto>(
    "/auth/register",
    request,
  );
  return data;
}

/**
 * Asks for a reset link. Always resolves, whether or not the address is
 * registered — the API answers identically on purpose, so the caller must not
 * report "no such account" either.
 */
export async function forgotPassword(email: string): Promise<void> {
  await apiClient.post("/auth/forgot-password", { email });
}

/** Redeems a reset token. Single use, and only until it expires. */
export async function resetPassword(
  token: string,
  newPassword: string,
): Promise<void> {
  await apiClient.post("/auth/reset-password", { token, newPassword });
}
