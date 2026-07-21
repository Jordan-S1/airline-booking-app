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
