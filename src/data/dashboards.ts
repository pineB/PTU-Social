import type { SvgIconComponent } from '@mui/icons-material';
import AutoStoriesRoundedIcon from '@mui/icons-material/AutoStoriesRounded';
import AssignmentTurnedInRoundedIcon from '@mui/icons-material/AssignmentTurnedInRounded';
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded';
import ScheduleRoundedIcon from '@mui/icons-material/ScheduleRounded';
import SchoolRoundedIcon from '@mui/icons-material/SchoolRounded';
import FactCheckRoundedIcon from '@mui/icons-material/FactCheckRounded';
import EventAvailableRoundedIcon from '@mui/icons-material/EventAvailableRounded';
import PendingActionsRoundedIcon from '@mui/icons-material/PendingActionsRounded';
import { PeopleAltRounded } from '@mui/icons-material';

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
    metrics: [
      { label: 'Академический рейтинг', value: '689', detail: 'за текущий семестр', icon: AutoStoriesRoundedIcon, color: '#5b5ce2', background: '#efefff' },
      { label: 'Учебная активность', value: '340', detail: 'БУА · за семестр', icon: AssignmentTurnedInRoundedIcon, color: '#159b79', background: '#e8f7f2' },
      { label: 'Общественная активность', value: '285', detail: 'БОА · за семестр', icon: EventAvailableRoundedIcon, color: '#dc9640', background: '#fff5e7' },
      { label: 'Батл-пасс', value: '34 ур.', detail: 'сезон 2026/27', icon: GroupsRoundedIcon, color: '#d45f7e', background: '#fff0f3' },
    ],
    profileName: 'Александра Ким',
    profileCaption: 'ИС-24 · студент',
    tableTitle: 'Задания и сроки',
    subjectHeading: 'Задание',
    detailHeading: 'Дисциплина · преподаватель',
    rows: [
      { subject: 'Практическая работа № 4', detail: 'Базы данных · Петров И. С.', status: 'В процессе', tone: 'neutral' },
      { subject: 'Компонентный подход в React', detail: 'Веб-разработка · Иванова М. А.', status: 'Сдано', tone: 'success' },
      { subject: 'Прототипирование проекта', detail: 'Проектный практикум · Смирнова Е. В.', status: 'Просрочено', tone: 'warning' },
      { subject: 'Тема 6 · подготовка к занятию', detail: 'Английский язык · Кузнецова О. П.', status: 'Принято', tone: 'neutral' },
    ],
  },
  teacher: {
    role: 'teacher',
    title: 'Кабинет преподавателя',
    heading: 'План занятий',
    metrics: [
      { label: 'Занятий сегодня', value: '4', detail: 'Первое — в 09:00', icon: ScheduleRoundedIcon, color: '#5b5ce2', background: '#efefff' },
      { label: 'Работ на проверку', value: '12', detail: '5 новых отправок', icon: FactCheckRoundedIcon, color: '#159b79', background: '#e8f7f2' },
      { label: 'Учебных групп', value: '3', detail: 'закреплены за вами', icon: GroupsRoundedIcon, color: '#dc9640', background: '#fff5e7' },
      { label: 'Мои дисциплины', value: '3', detail: 'назначены на семестр', icon: AutoStoriesRoundedIcon, color: '#d45f7e', background: '#fff0f3' },
    ],
    profileName: 'Мария Иванова',
    profileCaption: 'Кафедра информатики · преподаватель',
    tableTitle: 'Занятия и группы',
    subjectHeading: 'Занятие',
    detailHeading: 'Закреплённая группа · аудитория',
    rows: [
      { subject: 'Веб-разработка', detail: 'ИС-24 · ауд. 308', status: '09:00 – 10:30', tone: 'neutral' },
      { subject: 'Основы UX-дизайна', detail: 'ДИ-23 · ауд. 412', status: '10:50 – 12:20', tone: 'neutral' },
      { subject: 'Веб-разработка', detail: 'ИС-23 · ауд. 308', status: '13:10 – 14:40', tone: 'neutral' },
      { subject: 'Консультация', detail: 'ИС-24 · ауд. 308', status: '15:00 – 15:45', tone: 'neutral' },
    ],
  },
  organizer: {
    role: 'organizer',
    title: 'Кабинет организатора',
    heading: 'Мероприятия',
    metrics: [
      { label: 'Мои мероприятия', value: '3', detail: 'в текущем сезоне', icon: EventAvailableRoundedIcon, color: '#5b5ce2', background: '#efefff' },
      { label: 'Ожидают подтверждения', value: '2', detail: 'после завершения', icon: PendingActionsRoundedIcon, color: '#dc9640', background: '#fff5e7' },
      { label: 'Отзывы на проверке', value: '4', detail: 'по завершённым событиям', icon: PendingActionsRoundedIcon, color: '#159b79', background: '#e8f7f2' },
      { label: 'Участники', value: '87', detail: 'зарегистрированы', icon: GroupsRoundedIcon, color: '#d45f7e', background: '#fff0f3' },
    ],
    profileName: 'Комитет студенческих инициатив',
    profileCaption: 'Организатор мероприятий',
    tableTitle: 'Мероприятия и участие',
    subjectHeading: 'Мероприятие',
    detailHeading: 'Формат · баллы',
    rows: [
      { subject: 'День открытых дверей', detail: 'Индивидуально · БОА', status: 'Подтвердить участие', tone: 'warning' },
      { subject: 'Осенний фестиваль', detail: 'Индивидуально · БОА', status: 'Регистрация', tone: 'neutral' },
      { subject: 'День волонтёра', detail: 'Коллективно · БОА', status: 'Регистрация', tone: 'neutral' },
    ],
  },
  'academic-office': {
    role: 'academic-office',
    title: 'Учебная часть',
    heading: 'Заявления на справки',
    metrics: [
      { label: 'Учебных групп', value: '48', detail: 'на текущем семестре', icon: SchoolRoundedIcon, color: '#5b5ce2', background: '#efefff' },
      { label: 'Обращения', value: '18', detail: 'ожидают обработки', icon: PendingActionsRoundedIcon, color: '#dc9640', background: '#fff5e7' },
      { label: 'Преподавателей', value: '76', detail: 'в учебном заведении', icon: PeopleAltRounded, color: '#159b79', background: '#e8f7f2' },
      { label: 'Дисциплин', value: '132', detail: 'в учебной структуре', icon: EventAvailableRoundedIcon, color: '#d45f7e', background: '#fff0f3' },
    ],
    profileName: 'Елена Викторовна',
    profileCaption: 'Сотрудник · учебная часть',
    tableTitle: 'Заявления на справки',
    subjectHeading: 'Вопрос',
    detailHeading: 'Студент · группа',
    rows: [
      { subject: 'Справка об обучении', detail: 'Александра Ким · ИС-24', status: 'Подана', tone: 'warning' },
      { subject: 'Справка для общежития', detail: 'Михаил Орлов · КС-22', status: 'В обработке', tone: 'success' },
      { subject: 'Архивная справка', detail: 'Полина Соколова · ИС-23', status: 'Готова', tone: 'neutral' },
      { subject: 'Справка об обучении', detail: 'Артём Смирнов · ИС-24', status: 'Выдана', tone: 'neutral' },
    ],
  },
};
