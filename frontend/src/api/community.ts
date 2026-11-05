import apiClient from './client';

export type CommunityMetric = 'АР' | 'БОА' | 'БУА';

export interface CommunityStudent {
  id: string;
  name: string;
  group: string;
  academicRating: number;
  boaPoints: number;
  buaPoints: number;
  joinedAt: string;
}

export interface CommunityAward {
  id: string;
  studentId: string;
  studentName: string;
  group: string;
  title: string;
  detail: string;
  metric: CommunityMetric;
  points: number;
  createdAt: string;
}

export async function listCommunityStudents(): Promise<CommunityStudent[]> {
  const response = await apiClient.get<CommunityStudent[]>('/community/students');
  return response.data;
}

export async function listCommunityAwards(): Promise<CommunityAward[]> {
  const response = await apiClient.get<CommunityAward[]>('/community/awards');
  return response.data;
}
