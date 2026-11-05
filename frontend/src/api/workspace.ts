import apiClient from './client';

export type TaskStatus = 'in_progress' | 'submitted' | 'overdue' | 'accepted';
export type SubmissionStatus = 'in_progress' | 'submitted' | 'accepted';
export type TeacherGrade = 2 | 3 | 4 | 5;

export interface WorkspaceTask {
  id: string;
  title: string;
  subject: string;
  teacher: string;
  dueAt: string;
  maxPoints: number;
  description: string;
  allowLateSubmission: boolean;
  status: TaskStatus;
  submittedAt: string | null;
  workText: string | null;
  fileName: string | null;
  teacherComment: string | null;
  grade: TeacherGrade | null;
  awardedPoints: number | null;
  attachmentUrl: string | null;
}

export interface WorkspaceSubmission {
  id: string;
  assignmentId: string;
  assignmentTitle: string;
  subject: string;
  studentName: string;
  group: string;
  submittedAt: string;
  workText: string | null;
  fileName: string | null;
  status: SubmissionStatus;
  teacherComment: string | null;
  grade: TeacherGrade | null;
  awardedPoints: number | null;
  attachmentUrl: string | null;
}

export interface TeachingAssignmentOption {
  groupId: string;
  groupName: string;
  disciplineId: string;
  disciplineName: string;
}

export interface CreateWorkspaceTaskRequest {
  groupId: string;
  disciplineId: string;
  title: string;
  description: string;
  dueAt: string;
  maxPoints: number;
  allowLateSubmission: boolean;
}

export async function getStudentTasks(): Promise<WorkspaceTask[]> {
  const response = await apiClient.get<WorkspaceTask[]>('/workspace/student/tasks');
  return response.data;
}

export async function getTeacherSubmissions(): Promise<WorkspaceSubmission[]> {
  const response = await apiClient.get<WorkspaceSubmission[]>('/workspace/teacher/submissions');
  return response.data;
}

export async function getTeacherTasks(): Promise<WorkspaceTask[]> {
  const response = await apiClient.get<WorkspaceTask[]>('/workspace/teacher/tasks');
  return response.data;
}

export async function getTeachingAssignments(): Promise<TeachingAssignmentOption[]> {
  const response = await apiClient.get<TeachingAssignmentOption[]>('/workspace/teacher/assignments');
  return response.data;
}

export async function createWorkspaceTask(request: CreateWorkspaceTaskRequest): Promise<WorkspaceTask> {
  const response = await apiClient.post<WorkspaceTask>('/workspace/teacher/tasks', request);
  return response.data;
}

export async function submitWorkspaceTask(taskId: string, content: string, file?: File): Promise<WorkspaceTask> {
  const formData = new FormData();
  formData.append('content', content);
  if (file) formData.append('file', file);
  const response = await apiClient.post<WorkspaceTask>(`/workspace/student/tasks/${taskId}/submission`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
}

export async function reviewWorkspaceSubmission(
  submissionId: string,
  status: 'ACCEPTED' | 'RETURNED',
  grade: TeacherGrade | undefined,
  feedback: string,
): Promise<WorkspaceSubmission> {
  const response = await apiClient.patch<WorkspaceSubmission>(`/workspace/teacher/submissions/${submissionId}`, {
    status,
    grade,
    feedback,
  });
  return response.data;
}

export async function downloadWorkspaceAttachment(url: string, fileName: string): Promise<void> {
  const response = await apiClient.get<Blob>(url, { responseType: 'blob' });
  const objectUrl = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = fileName;
  link.click();
  URL.revokeObjectURL(objectUrl);
}
