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
