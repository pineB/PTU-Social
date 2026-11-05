import type { SvgIconComponent } from '@mui/icons-material';

export type RoleId = 'student' | 'teacher' | 'organizer' | 'academic-office';

export interface DashboardRow {
  subject: string;
  detail: string;
  status: string;
  tone: 'success' | 'warning' | 'neutral';
}

export interface Metric {
  label: string;
  value: string;
  detail: string;
  icon: SvgIconComponent;
  color: string;
  background: string;
}

export interface DashboardData {
  role: RoleId;
  title: string;
  heading: string;
  profileName: string;
  profileCaption: string;
  metrics: Metric[];
  rows: DashboardRow[];
  tableTitle: string;
  subjectHeading: string;
  detailHeading: string;
}

export const dashboards: Record<RoleId, DashboardData> = {
  student: {
    role: 'student',
    title: 'Кабинет студента',
    heading: 'Моя учёба',
    profileName: '',
    profileCaption: 'Студент',
    metrics: [],
    rows: [],
    tableTitle: 'Задания и сроки',
    subjectHeading: 'Задание',
    detailHeading: 'Дисциплина · преподаватель',
  },
  teacher: {
    role: 'teacher',
    title: 'Кабинет преподавателя',
    heading: 'План занятий',
    profileName: '',
    profileCaption: 'Преподаватель',
    metrics: [],
    rows: [],
    tableTitle: 'Занятия и группы',
    subjectHeading: 'Занятие',
    detailHeading: 'Группа · аудитория',
  },
  organizer: {
    role: 'organizer',
    title: 'Кабинет организатора',
    heading: 'Мероприятия',
    profileName: '',
    profileCaption: 'Организатор',
    metrics: [],
    rows: [],
    tableTitle: 'Мероприятия и участие',
    subjectHeading: 'Мероприятие',
    detailHeading: 'Формат · баллы',
  },
  'academic-office': {
    role: 'academic-office',
    title: 'Учебная часть',
    heading: 'Заявления на справки',
    profileName: '',
    profileCaption: 'Учебная часть',
    metrics: [],
    rows: [],
    tableTitle: 'Заявления на справки',
    subjectHeading: 'Вопрос',
    detailHeading: 'Студент · группа',
  },
};
