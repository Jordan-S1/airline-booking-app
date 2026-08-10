import { apiClient } from "../lib/apiClient";
import type { UserResponseDto, UserUpdateRequestDto } from "../types/user";

export async function getUser(id: number): Promise<UserResponseDto> {
  const { data } = await apiClient.get<UserResponseDto>(`/users/${id}`);
  return data;
}

export async function updateUser(
  id: number,
  request: UserUpdateRequestDto,
): Promise<UserResponseDto> {
  const { data } = await apiClient.put<UserResponseDto>(
    `/users/${id}`,
    request,
  );
  return data;
}

/**
 * Changes the signed-in user's own password.
 *
 * <p>A wrong current password comes back as 400, not 401 — the api client
 * treats 401 as an ended session and would sign the user out mid-form.
 */
export async function changePassword(
  id: number,
  request: { currentPassword: string; newPassword: string },
): Promise<void> {
  await apiClient.patch(`/users/${id}/password`, request);
}
