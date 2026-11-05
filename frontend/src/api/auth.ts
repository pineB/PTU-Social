import apiClient from './client';

export type UserRole = 'STUDENT' | 'TEACHER' | 'ORGANIZER' | 'ACADEMIC_OFFICE';

export interface TokenResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresInSeconds: number;
  userId: string;
  role: UserRole;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export async function login(request: LoginRequest): Promise<TokenResponse> {
  const response = await apiClient.post<TokenResponse>('/auth/login', request);
  return response.data;
}
