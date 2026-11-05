import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  createWorkspaceTask,
  getStudentTasks,
  getTeacherSubmissions,
  getTeacherTasks,
  getTeachingAssignments,
  reviewWorkspaceSubmission,
  submitWorkspaceTask,
  type SubmissionStatus,
  type CreateWorkspaceTaskRequest,
  type TaskStatus,
  type TeacherGrade,
  type WorkspaceSubmission,
  type WorkspaceTask,
  type TeachingAssignmentOption,
} from '../api/workspace';
import { useAuth } from '../auth/AuthProvider';

export type AssignmentStatus = TaskStatus;
export type ReviewStatus = SubmissionStatus;
export type { TeacherGrade };

export type Assignment = Omit<WorkspaceTask, 'submittedAt' | 'workText' | 'fileName' | 'teacherComment' | 'grade' | 'awardedPoints'> & {
  submittedAt?: string;
  workText?: string;
  fileName?: string;
  teacherComment?: string;
  grade?: TeacherGrade;
  awardedPoints?: number;
  returnComment?: string;
  feedbackFileName?: string;
};

export type StudentSubmission = Omit<WorkspaceSubmission, 'workText' | 'fileName' | 'teacherComment' | 'grade' | 'awardedPoints'> & {
  workText: string;
  fileName?: string;
  teacherComment?: string;
  grade?: TeacherGrade;
  awardedPoints?: number;
  returnComment?: string;
  feedbackFileName?: string;
};

interface TaskWorkflowContextValue {
  assignments: Assignment[];
  submissions: StudentSubmission[];
  teachingAssignments: TeachingAssignmentOption[];
  isLoading: boolean;
  error: string;
  submitAssignment: (assignmentId: string, workText: string, file?: File) => Promise<void>;
  createAssignment: (request: CreateWorkspaceTaskRequest) => Promise<void>;
  acceptSubmission: (submissionId: string, grade: TeacherGrade, comment: string) => Promise<void>;
  returnSubmission: (submissionId: string, comment: string) => Promise<void>;
}

const TaskWorkflowContext = createContext<TaskWorkflowContextValue | null>(null);

function mapTask(task: WorkspaceTask): Assignment {
  return {
    ...task,
    submittedAt: task.submittedAt ?? undefined,
    workText: task.workText ?? undefined,
    fileName: task.fileName ?? undefined,
    teacherComment: task.teacherComment ?? undefined,
    returnComment: task.status === 'in_progress' ? task.teacherComment ?? undefined : undefined,
    grade: task.grade ?? undefined,
    awardedPoints: task.awardedPoints ?? undefined,
  };
}

function mapSubmission(submission: WorkspaceSubmission): StudentSubmission {
  return {
    ...submission,
    workText: submission.workText ?? '',
    fileName: submission.fileName ?? undefined,
    teacherComment: submission.teacherComment ?? undefined,
    returnComment: submission.status === 'in_progress' ? submission.teacherComment ?? undefined : undefined,
    grade: submission.grade ?? undefined,
    awardedPoints: submission.awardedPoints ?? undefined,
  };
}

const gradeMultipliers: Record<TeacherGrade, number> = {
  2: 0,
  3: 0.5,
  4: 0.75,
  5: 1,
};

export function calculateAward(assignment: Assignment, submission: StudentSubmission, grade: TeacherGrade): number {
  const lateDays = Math.max(
    0,
    Math.ceil((Date.parse(submission.submittedAt) - Date.parse(assignment.dueAt)) / 86_400_000),
  );
  const lateMultiplier = Math.max(0.5, 1 - 0.01 * lateDays);
  const basePoints = Math.floor(assignment.maxPoints * gradeMultipliers[grade]);
  return Math.floor(basePoints * lateMultiplier);
}

export function TaskWorkflowProvider({ children }: { children: ReactNode }) {
  const { session, refreshProfile } = useAuth();
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [submissions, setSubmissions] = useState<StudentSubmission[]>([]);
  const [teachingAssignments, setTeachingAssignments] = useState<TeachingAssignmentOption[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setAssignments([]);
    setSubmissions([]);
    setTeachingAssignments([]);
    setError('');
    if (!session) {
      setIsLoading(false);
      return () => { active = false; };
    }

    setIsLoading(true);
    const request = session.role === 'STUDENT'
      ? getStudentTasks().then((items) => {
          if (active) setAssignments(items.map(mapTask));
        })
      : session.role === 'TEACHER'
        ? Promise.all([getTeacherTasks(), getTeacherSubmissions(), getTeachingAssignments()]).then(([teacherTasks, items, options]) => {
            if (active) {
              setAssignments(teacherTasks.map(mapTask));
              setSubmissions(items.map(mapSubmission));
              setTeachingAssignments(options);
            }
          })
        : Promise.resolve();
    void request.catch((requestError: unknown) => {
      if (active) setError(requestError instanceof Error ? requestError.message : 'Не удалось загрузить задания.');
    }).finally(() => {
      if (active) setIsLoading(false);
    });
    return () => { active = false; };
  }, [session]);

  const submitAssignment = useCallback(async (assignmentId: string, workText: string, file?: File) => {
    const updatedTask = await submitWorkspaceTask(assignmentId, workText, file);
    setAssignments((current) => current.map((item) => item.id === assignmentId ? mapTask(updatedTask) : item));
  }, []);

  const createAssignment = useCallback(async (request: CreateWorkspaceTaskRequest) => {
    const task = await createWorkspaceTask(request);
    setAssignments((current) => [...current, mapTask(task)].sort(
      (left, right) => Date.parse(left.dueAt) - Date.parse(right.dueAt),
    ));
  }, []);

  const acceptSubmission = useCallback(async (submissionId: string, grade: TeacherGrade, comment: string) => {
    const updatedSubmission = await reviewWorkspaceSubmission(submissionId, 'ACCEPTED', grade, comment);
    await refreshProfile();
    setSubmissions((current) => current.map((item) =>
      item.id === submissionId ? mapSubmission(updatedSubmission) : item,
    ));
  }, [refreshProfile]);

  const returnSubmission = useCallback(async (submissionId: string, comment: string) => {
    const updatedSubmission = await reviewWorkspaceSubmission(submissionId, 'RETURNED', undefined, comment);
    setSubmissions((current) => current.map((item) =>
      item.id === submissionId ? mapSubmission(updatedSubmission) : item,
    ));
  }, []);

  const value = useMemo(() => ({
    assignments,
    submissions,
    teachingAssignments,
    isLoading,
    error,
    submitAssignment,
    createAssignment,
    acceptSubmission,
    returnSubmission,
  }), [assignments, submissions, teachingAssignments, isLoading, error, submitAssignment, createAssignment, acceptSubmission, returnSubmission]);

  return <TaskWorkflowContext.Provider value={value}>{children}</TaskWorkflowContext.Provider>;
}

export function useTaskWorkflow(): TaskWorkflowContextValue {
  const context = useContext(TaskWorkflowContext);
  if (context === null) {
    throw new Error('useTaskWorkflow must be used within TaskWorkflowProvider');
  }
  return context;
}
