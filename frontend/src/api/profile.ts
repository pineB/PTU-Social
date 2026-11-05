import apiClient from './client';
import type { UserRole } from './auth';

export interface TeachingGroup {
  id: string;
  name: string;
}

export interface CurrentUser {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  groupId: string | null;
  groupName: string | null;
  groupLeader: boolean;
  academicRating: number | null;
  boaPoints: number | null;
  buaPoints: number | null;
  teachingGroups: TeachingGroup[];
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiClient.get<CurrentUser>('/me');
  return response.data;
}
