/** Mirrors com.airlinebookingsystem.dto.auth.RegisterRequest. */
export interface RegisterRequestDto {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  phoneNumber: string | null;
}

/** Mirrors com.airlinebookingsystem.dto.auth.LoginRequest. */
export interface LoginRequestDto {
  email: string;
  password: string;
}

/** Mirrors com.airlinebookingsystem.dto.auth.AuthResponse. */
export interface AuthResponseDto {
  token: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  userId: number;
}
