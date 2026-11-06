import apiClient from './client';

export interface CatalogItem {
  id: string;
  name: string;
}

export interface ManagedGroup {
  id: string;
  name: string;
  facultyId: string;
  facultyName: string;
}

export interface ManagedAssignment {
  id: string;
  groupId: string;
  groupName: string;
  disciplineId: string;
  disciplineName: string;
}

export interface ManagedTeacher {
  id: string;
  fullName: string;
  email: string;
  active: boolean;
  assignments: ManagedAssignment[];
}

export interface ManagedAcademicOfficeUser {
  id: string;
  fullName: string;
  email: string;
  active: boolean;
}

export interface CreateAcademicOfficeUserRequest {
  fullName: string;
  email: string;
  password: string;
}

export interface CreateTeacherInviteRequest {
  fullName: string;
  email: string;
  departmentId: string;
  disciplineIds: string[];
  groupIds: string[];
}

export interface TeacherInviteCreatedResponse {
  id: string;
  email: string;
  expiresAt: string;
}

export async function listAcademicOfficeUsers(): Promise<ManagedAcademicOfficeUser[]> {
  const response = await apiClient.get<ManagedAcademicOfficeUser[]>('/admin/academic-office-users');
  return response.data;
}

export async function createAcademicOfficeUser(request: CreateAcademicOfficeUserRequest): Promise<ManagedAcademicOfficeUser> {
  const response = await apiClient.post<ManagedAcademicOfficeUser>('/admin/academic-office-users', request);
  return response.data;
}

export async function getAcademicFaculties(): Promise<CatalogItem[]> {
  const response = await apiClient.get<CatalogItem[]>('/academic/faculties');
  return response.data;
}

export async function getAcademicDepartments(): Promise<CatalogItem[]> {
  const response = await apiClient.get<CatalogItem[]>('/academic/management/departments');
  return response.data;
}

export async function getAcademicDisciplines(departmentId: string): Promise<CatalogItem[]> {
  const response = await apiClient.get<CatalogItem[]>(`/academic/management/departments/${departmentId}/disciplines`);
  return response.data;
}

export async function listManagedGroups(): Promise<ManagedGroup[]> {
  const response = await apiClient.get<ManagedGroup[]>('/academic/management/groups');
  return response.data;
}

export async function createManagedGroup(name: string, facultyId: string): Promise<ManagedGroup> {
  const response = await apiClient.post<ManagedGroup>('/academic/management/groups', { name, facultyId });
  return response.data;
}

export async function renameManagedGroup(groupId: string, name: string): Promise<ManagedGroup> {
  const response = await apiClient.patch<ManagedGroup>(`/academic/management/groups/${groupId}`, { name });
  return response.data;
}

export async function createTeacherInvite(request: CreateTeacherInviteRequest): Promise<TeacherInviteCreatedResponse> {
  const response = await apiClient.post<TeacherInviteCreatedResponse>('/academic/teacher-invites', request);
  return response.data;
}

export async function listManagedTeachers(): Promise<ManagedTeacher[]> {
  const response = await apiClient.get<ManagedTeacher[]>('/academic/management/teachers');
  return response.data;
}

export async function setManagedTeacherActive(teacherId: string, active: boolean): Promise<ManagedTeacher> {
  const response = await apiClient.patch<ManagedTeacher>(`/academic/management/teachers/${teacherId}/active`, { active });
  return response.data;
}

export async function createTeacherAssignment(teacherId: string, groupId: string, disciplineId: string): Promise<ManagedAssignment> {
  const response = await apiClient.post<ManagedAssignment>(`/academic/management/teachers/${teacherId}/assignments`, { groupId, disciplineId });
  return response.data;
}

export async function archiveTeacherAssignment(teacherId: string, assignmentId: string): Promise<void> {
  await apiClient.delete(`/academic/management/teachers/${teacherId}/assignments/${assignmentId}`);
}
