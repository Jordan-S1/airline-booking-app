/** Mirrors com.airlinebookingsystem.dto.user.UserResponse. */
export interface UserResponseDto {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string | null;
  address: string | null;
  city: string | null;
  country: string | null;
  postalCode: string | null;
  preferredCurrency: string;
  role: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Mirrors com.airlinebookingsystem.dto.user.UserUpdateRequest. */
export interface UserUpdateRequestDto {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string | null;
  address?: string | null;
  city?: string | null;
  country?: string | null;
  postalCode?: string | null;
  preferredCurrency?: string;
}
