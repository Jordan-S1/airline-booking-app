const STORAGE_KEY = "skyair-auth-token";

let currentToken: string | null = localStorage.getItem(STORAGE_KEY);

export function getAuthToken(): string | null {
  return currentToken;
}

export function setAuthToken(token: string | null): void {
  currentToken = token;
  if (token) {
    localStorage.setItem(STORAGE_KEY, token);
  } else {
    localStorage.removeItem(STORAGE_KEY);
  }
}
