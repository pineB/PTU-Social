import apiClient from './client';
import type { TokenResponse } from './auth';

export interface FacultyOption {
  id: string;
  name: string;
}

export interface StudyGroupOption {
  id: string;
  name: string;
}

export interface RegistrationRequest {
  id: string;
  fullName: string;
  email: string;
  facultyId: string;
  groupId: string;
  status: string;
  createdAt: string;
  expiresAt: string;
}

export interface TeacherInviteInfo {
  fullName: string;
  email: string;
  expiresAt: string;
}

export async function listFaculties(): Promise<FacultyOption[]> {
  const response = await apiClient.get<FacultyOption[]>('/academic/faculties');
  return response.data;
}

export async function listStudyGroups(facultyId: string): Promise<StudyGroupOption[]> {
  const response = await apiClient.get<StudyGroupOption[]>(`/academic/faculties/${facultyId}/groups`);
  return response.data;
}

export async function createRegistrationRequest(input: {
  fullName: string;
  email: string;
  password: string;
  facultyId: string;
  groupId: string;
}): Promise<RegistrationRequest> {
  const response = await apiClient.post<RegistrationRequest>('/auth/registration-requests', input);
  return response.data;
}

export async function verifyRegistrationEmail(requestId: string, code: string): Promise<RegistrationRequest> {
  const response = await apiClient.post<RegistrationRequest>(`/auth/registration-requests/${requestId}/email-verify`, { code });
  return response.data;
}

export async function resendRegistrationCode(requestId: string): Promise<void> {
  await apiClient.post(`/auth/registration-requests/${requestId}/email-code`);
}

export async function getTeacherInvite(token: string): Promise<TeacherInviteInfo> {
  const response = await apiClient.get<TeacherInviteInfo>(`/auth/teacher-invites/${encodeURIComponent(token)}`);
  return response.data;
}

export async function acceptTeacherInvite(token: string, password: string): Promise<TokenResponse> {
  const response = await apiClient.post<TokenResponse>(`/auth/teacher-invites/${encodeURIComponent(token)}/accept`, { password });
  return response.data;
}
