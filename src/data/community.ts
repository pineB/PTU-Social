export type CommunityMetric = 'АР' | 'БОА' | 'БУА';

export interface CommunityStudent {
  id: string;
  name: string;
  group: string;
  course: number;
  academicRating: number;
  boaPoints: number;
  buaPoints: number;
  joinedAt: string;
  interests: string[];
}

export interface CommunityAward {
  id: string;
  studentId?: string;
  studentName: string;
  group: string;
  title: string;
  detail: string;
  metric: CommunityMetric;
  points: number;
  createdAt: string;
}

export const communityStudents: CommunityStudent[] = [
  { id: 'student-alexandra', name: 'Александра Ким', group: 'ИС-24', course: 2, academicRating: 689, boaPoints: 285, buaPoints: 340, joinedAt: '2024-09-01', interests: ['Веб-разработка', 'Дизайн', 'Волонтёрство'] },
  { id: 'student-mikhail', name: 'Михаил Орлов', group: 'ИС-24', course: 2, academicRating: 624, boaPoints: 410, buaPoints: 280, joinedAt: '2024-09-01', interests: ['Робототехника', 'Спорт', 'Волонтёрство'] },
  { id: 'student-elena', name: 'Елена Смирнова', group: 'ИС-23', course: 3, academicRating: 720, boaPoints: 180, buaPoints: 405, joinedAt: '2023-09-01', interests: ['Алгоритмы', 'Наука', 'Чтение'] },
  { id: 'student-ivan', name: 'Иван Беляев', group: 'ИС-23', course: 3, academicRating: 580, boaPoints: 230, buaPoints: 390, joinedAt: '2023-09-01', interests: ['Программирование', 'Киберспорт', 'Музыка'] },
  { id: 'student-sofia', name: 'София Волкова', group: 'ДИ-23', course: 3, academicRating: 645, boaPoints: 360, buaPoints: 240, joinedAt: '2023-09-01', interests: ['Иллюстрация', 'Фотография', 'Культура'] },
  { id: 'student-artem', name: 'Артём Смирнов', group: 'ИС-24', course: 2, academicRating: 552, boaPoints: 190, buaPoints: 315, joinedAt: '2024-09-01', interests: ['Разработка игр', 'Футбол'] },
  { id: 'student-polina', name: 'Полина Крылова', group: 'ДИ-23', course: 3, academicRating: 610, boaPoints: 320, buaPoints: 298, joinedAt: '2023-09-01', interests: ['Типографика', 'Искусство', 'Кино'] },
  { id: 'student-nikita', name: 'Никита Волков', group: 'КС-22', course: 4, academicRating: 498, boaPoints: 275, buaPoints: 360, joinedAt: '2022-09-01', interests: ['Сети', 'Настольные игры', 'Музыка'] },
];

export const initialCommunityAwards: CommunityAward[] = [
  { id: 'award-open-day-alex', studentId: 'student-alexandra', studentName: 'Александра Ким', group: 'ИС-24', title: 'День открытых дверей', detail: 'Помощь на площадке колледжа', metric: 'БОА', points: 50, createdAt: '2026-10-25T15:10:00+03:00' },
  { id: 'award-design-mikhail', studentId: 'student-mikhail', studentName: 'Михаил Орлов', group: 'ИС-24', title: 'Дизайн-лаборатория', detail: 'Командная работа и защита прототипа', metric: 'БУА', points: 32, createdAt: '2026-10-24T18:30:00+03:00' },
  { id: 'award-volunteer-sofia', studentId: 'student-sofia', studentName: 'София Волкова', group: 'ДИ-23', title: 'Волонтёрский день колледжа', detail: 'Организация сбора вещей', metric: 'БОА', points: 45, createdAt: '2026-10-23T16:00:00+03:00' },
  { id: 'award-database-elena', studentId: 'student-elena', studentName: 'Елена Смирнова', group: 'ИС-23', title: 'Практическая работа № 5', detail: 'Базы данных · оценка 5', metric: 'АР', points: 90, createdAt: '2026-10-22T13:15:00+03:00' },
  { id: 'award-project-ivan', studentId: 'student-ivan', studentName: 'Иван Беляев', group: 'ИС-23', title: 'Защита учебного проекта', detail: 'Веб-разработка · оценка 4', metric: 'АР', points: 75, createdAt: '2026-10-21T11:40:00+03:00' },
  { id: 'award-workshop-polina', studentId: 'student-polina', studentName: 'Полина Крылова', group: 'ДИ-23', title: 'Дизайн-лаборатория', detail: 'Участие в мастер-классе', metric: 'БОА', points: 30, createdAt: '2026-10-20T17:20:00+03:00' },
  { id: 'award-network-nikita', studentId: 'student-nikita', studentName: 'Никита Волков', group: 'КС-22', title: 'Лабораторная работа по сетям', detail: 'Компьютерные сети · оценка 5', metric: 'БУА', points: 85, createdAt: '2026-10-19T14:00:00+03:00' },
];
