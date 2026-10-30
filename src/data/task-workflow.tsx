import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';

export type AssignmentStatus = 'in_progress' | 'submitted' | 'overdue' | 'accepted';
export type ReviewStatus = 'in_progress' | 'submitted' | 'accepted';
export type TeacherGrade = 2 | 3 | 4 | 5;

const gradeMultipliers: Record<TeacherGrade, number> = {
  2: 0,
  3: 0.5,
  4: 0.75,
  5: 1,
};

export interface Assignment {
  id: string;
  title: string;
  subject: string;
  teacher: string;
  dueAt: string;
  maxPoints: number;
  description: string;
  allowLateSubmission: boolean;
  status: AssignmentStatus;
  submittedAt?: string;
  workText?: string;
  fileName?: string;
  returnComment?: string;
  teacherComment?: string;
  feedbackFileName?: string;
  grade?: TeacherGrade;
  awardedPoints?: number;
}

export interface StudentSubmission {
  id: string;
  assignmentId: string;
  assignmentTitle: string;
  subject: string;
  studentName: string;
  group: string;
  submittedAt: string;
  workText: string;
  fileName?: string;
  status: ReviewStatus;
  returnComment?: string;
  teacherComment?: string;
  feedbackFileName?: string;
  grade?: TeacherGrade;
  awardedPoints?: number;
}

interface TaskWorkflowContextValue {
  assignments: Assignment[];
  submissions: StudentSubmission[];
  submitAssignment: (assignmentId: string, workText: string, fileName?: string) => Promise<void>;
  acceptSubmission: (submissionId: string, grade: TeacherGrade, comment: string, feedbackFileName?: string) => Promise<void>;
  returnSubmission: (submissionId: string, comment: string, feedbackFileName?: string) => Promise<void>;
}

const initialAssignments: Assignment[] = [
  {
    id: 'database-practical-4',
    title: 'Практическая работа № 4',
    subject: 'Базы данных',
    teacher: 'Петров И. С.',
    dueAt: '2026-10-31T23:59:00+03:00',
    maxPoints: 100,
    description: 'Создайте схему базы данных для выбранной предметной области и подготовьте SQL-запросы.',
    allowLateSubmission: true,
    status: 'in_progress',
  },
  {
    id: 'react-components',
    title: 'Компонентный подход в React',
    subject: 'Веб-разработка',
    teacher: 'Иванова М. А.',
    dueAt: '2026-10-30T18:00:00+03:00',
    maxPoints: 100,
    description: 'Разработайте набор переиспользуемых компонентов с типизированными пропсами.',
    allowLateSubmission: true,
    status: 'submitted',
    submittedAt: '2026-10-30T09:15:00+03:00',
    workText: 'Выполнила практическую работу, ссылка на проект приложена.',
    fileName: 'react-components.zip',
  },
  {
    id: 'project-prototype',
    title: 'Прототипирование проекта',
    subject: 'Проектный практикум',
    teacher: 'Смирнова Е. В.',
    dueAt: '2026-10-25T23:59:00+03:00',
    maxPoints: 80,
    description: 'Подготовьте интерактивный прототип интерфейса для учебного проекта.',
    allowLateSubmission: true,
    status: 'overdue',
  },
  {
    id: 'english-topic-6',
    title: 'Тема 6 · подготовка к занятию',
    subject: 'Английский язык',
    teacher: 'Кузнецова О. П.',
    dueAt: '2026-10-28T23:59:00+03:00',
    maxPoints: 60,
    description: 'Выполните задания к шестой теме и отправьте ответы на проверку.',
    allowLateSubmission: false,
    status: 'accepted',
    submittedAt: '2026-10-27T16:30:00+03:00',
    workText: 'Все упражнения выполнены. Ответы добавлены в файл.',
    fileName: 'english-topic-6.pdf',
    grade: 5,
    awardedPoints: 60,
  },
];

const initialSubmissions: StudentSubmission[] = [
  {
    id: 'submission-alex-react',
    assignmentId: 'react-components',
    assignmentTitle: 'Компонентный подход в React',
    subject: 'Веб-разработка',
    studentName: 'Александра Ким',
    group: 'ИС-24',
    submittedAt: '2026-10-30T09:15:00+03:00',
    workText: 'Выполнила практическую работу, ссылка на проект приложена.',
    fileName: 'react-components.zip',
    status: 'submitted',
  },
  {
    id: 'submission-mikhail-db',
    assignmentId: 'database-practical-4',
    assignmentTitle: 'Практическая работа № 4',
    subject: 'Базы данных',
    studentName: 'Михаил Орлов',
    group: 'ИС-24',
    submittedAt: '2026-10-29T14:10:00+03:00',
    workText: 'Схема базы данных и запросы находятся в приложенном файле.',
    fileName: 'database-practical-4.sql',
    status: 'submitted',
  },
  {
    id: 'submission-polina-ux',
    assignmentId: 'react-components',
    assignmentTitle: 'Компонентный подход в React',
    subject: 'Веб-разработка',
    studentName: 'Полина Соколова',
    group: 'ИС-23',
    submittedAt: '2026-10-29T12:05:00+03:00',
    workText: 'Прошу проверить исправленную работу.',
    fileName: 'components-v2.zip',
    status: 'in_progress',
    returnComment: 'Добавьте обработку состояния загрузки и повторно отправьте работу.',
    teacherComment: 'Добавьте обработку состояния загрузки и повторно отправьте работу.',
    feedbackFileName: 'замечания-к-проекту.pdf',
  },
];

const TaskWorkflowContext = createContext<TaskWorkflowContextValue | null>(null);

function mockRequest(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, 400));
}

export function calculateAward(assignment: Assignment, submission: StudentSubmission, grade: TeacherGrade): number {
  const lateDays = Math.max(
    0,
    Math.ceil((Date.parse(submission.submittedAt) - Date.parse(assignment.dueAt)) / 86_400_000),
  );
  const lateMultiplier = Math.max(0.5, 1 - 0.01 * lateDays);
  return Math.round(assignment.maxPoints * gradeMultipliers[grade] * lateMultiplier);
}

export function TaskWorkflowProvider({ children }: { children: ReactNode }) {
  const [assignments, setAssignments] = useState(initialAssignments);
  const [submissions, setSubmissions] = useState(initialSubmissions);

  const submitAssignment = useCallback(async (assignmentId: string, workText: string, fileName?: string) => {
    await mockRequest();
    const assignment = assignments.find((item) => item.id === assignmentId);
    if (!assignment) throw new Error('Задание не найдено.');

    const submittedAt = new Date().toISOString();
    const submissionId = `submission-${assignmentId}-${Date.now()}`;
    setAssignments((current) => current.map((item) =>
      item.id === assignmentId
        ? { ...item, status: 'submitted', submittedAt, workText, fileName, returnComment: undefined, teacherComment: undefined, feedbackFileName: undefined }
        : item,
    ));
    setSubmissions((current) => [
      {
        id: submissionId,
        assignmentId,
        assignmentTitle: assignment.title,
        subject: assignment.subject,
        studentName: 'Александра Ким',
        group: 'ИС-24',
        submittedAt,
        workText,
        fileName,
        status: 'submitted',
      },
      ...current.filter((item) => !(item.assignmentId === assignmentId && item.studentName === 'Александра Ким')),
    ]);
  }, [assignments]);

  const acceptSubmission = useCallback(async (submissionId: string, grade: TeacherGrade, comment: string, feedbackFileName?: string) => {
    await mockRequest();
    const submission = submissions.find((item) => item.id === submissionId);
    if (!submission || submission.status !== 'submitted') {
      throw new Error('Работа уже обработана или не найдена.');
    }

    const assignment = assignments.find((item) => item.id === submission.assignmentId);
    if (!assignment) throw new Error('Задание для этой работы не найдено.');
    const awardedPoints = calculateAward(assignment, submission, grade);
    setSubmissions((current) => current.map((item) =>
      item.id === submissionId ? { ...item, status: 'accepted', grade, awardedPoints, teacherComment: comment, feedbackFileName } : item,
    ));
    if (submission.studentName === 'Александра Ким') {
      setAssignments((current) => current.map((item) =>
        item.id === submission.assignmentId ? { ...item, status: 'accepted', grade, awardedPoints, teacherComment: comment, feedbackFileName } : item,
      ));
    }
  }, [assignments, submissions]);

  const returnSubmission = useCallback(async (submissionId: string, comment: string, feedbackFileName?: string) => {
    await mockRequest();
    const submission = submissions.find((item) => item.id === submissionId);
    if (!submission || submission.status !== 'submitted') {
      throw new Error('Работа уже обработана или не найдена.');
    }

    setSubmissions((current) => current.map((item) =>
      item.id === submissionId
        ? { ...item, status: 'in_progress', returnComment: comment, teacherComment: comment, feedbackFileName }
        : item,
    ));
    if (submission.studentName === 'Александра Ким') {
      setAssignments((current) => current.map((item) =>
        item.id === submission.assignmentId
          ? { ...item, status: 'in_progress', returnComment: comment, teacherComment: comment, feedbackFileName }
          : item,
      ));
    }
  }, [submissions]);

  return (
    <TaskWorkflowContext.Provider value={{ assignments, submissions, submitAssignment, acceptSubmission, returnSubmission }}>
      {children}
    </TaskWorkflowContext.Provider>
  );
}

export function useTaskWorkflow(): TaskWorkflowContextValue {
  const context = useContext(TaskWorkflowContext);
  if (context === null) {
    throw new Error('useTaskWorkflow must be used within TaskWorkflowProvider');
  }
  return context;
}
