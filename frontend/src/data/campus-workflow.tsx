import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react';

export interface CampusEvent {
  id: string;
  title: string;
  description: string;
  date: string;
  endsAt: string;
  location: string;
  category: string;
  capacity: number;
  format: 'individual' | 'group';
  groupMinPercent: number | null;
  activityType: 'БОА' | 'БУА';
  maxPoints: number;
  createdBy: string;
  groupRegistrations: EventGroupRegistration[];
  participants: EventParticipant[];
  reviews: EventReview[];
}

export interface EventReview {
  id: string;
  studentName: string;
  rating: number;
  comment: string;
  status: 'pending' | 'approved' | 'rejected';
  createdAt: string;
}

export interface EventParticipant {
  id: string;
  studentName: string;
  group: string;
  enrolledAt: string;
  participationConfirmed: boolean;
  groupRegistrationId?: string;
  awardedPoints?: number;
  awardedPercent?: number;
}

export interface EventGroupRegistration {
  id: string;
  group: string;
  groupSize: number;
  addedBy: string;
  requestedAt: string;
}

export interface GroupConfirmation {
  id: string;
  studentName: string;
  email: string;
  group: string;
  submittedAt: string;
  status: 'pending' | 'approved' | 'rejected';
}

export interface GroupStudent {
  id: string;
  studentName: string;
  email: string;
  group: string;
}

export interface ActivityAward {
  id: string;
  eventId: string;
  eventTitle: string;
  studentName: string;
  group: string;
  activityType: 'БОА' | 'БУА';
  activityPercent: number;
  points: number;
  awardedBy: string;
  createdAt: string;
}

export interface WalletTransaction {
  id: string;
  title: string;
  detail: string;
  amount: number;
  createdAt: string;
}

export interface InventoryItem {
  id: string;
  name: string;
  kind: 'case' | 'cosmetic';
  cosmeticType?: 'avatar' | 'avatar-frame' | 'profile-background' | 'badge';
  rarity?: 'Обычный' | 'Редкий' | 'Эпический';
  source: string;
  obtainedAt: string;
}

export interface CampusCaseReward {
  name: string;
  cosmeticType: NonNullable<InventoryItem['cosmeticType']>;
  rarity: NonNullable<InventoryItem['rarity']>;
  chance: number;
}

export interface CertificateRequest {
  id: string;
  studentName: string;
  group: string;
  type: string;
  comment: string;
  deliveryMethod: 'В учебной части' | 'Электронно';
  status: 'Подана' | 'В обработке' | 'Готова' | 'Выдана' | 'Отклонена' | 'Отменена';
  createdAt: string;
  electronicDocument?: {
    fileName: string;
    dataUrl: string;
  };
}

export interface ScheduleEntry {
  id: string;
  weekday: string;
  startTime: string;
  endTime: string;
  group: string;
  subject: string;
  teacher: string;
  room: string;
}

export interface PassReward {
  id: string;
  track: 'free' | 'premium';
  level: number;
  title: string;
  rewardKind: 'case' | 'cosmetic';
  claimed: boolean;
}

interface CampusWorkflowValue {
  isGroupLeader: boolean;
  students: GroupStudent[];
  teacherGroups: readonly string[];
  groupLeaders: Record<string, string>;
  events: CampusEvent[];
  groupConfirmations: GroupConfirmation[];
  activityAwards: ActivityAward[];
  academicRating: number;
  boaPoints: number;
  buaPoints: number;
  passExperience: number;
  premiumPassOwned: boolean;
  passRewards: PassReward[];
  walletBalance: number;
  transactions: WalletTransaction[];
  inventory: InventoryItem[];
  certificateRequests: CertificateRequest[];
  scheduleEntries: ScheduleEntry[];
  joinEvent: (eventId: string) => Promise<void>;
  addEventGroup: (eventId: string, group: string, groupSize: number, addedBy: string) => Promise<void>;
  decideGroupConfirmation: (id: string, approve: boolean, reviewer: { role: 'teacher' | 'group-leader'; group: string }) => Promise<void>;
  assignGroupLeader: (group: string, studentId: string) => Promise<void>;
  createEvent: (event: Omit<CampusEvent, 'id' | 'groupRegistrations' | 'participants' | 'reviews' | 'createdBy'>, createdBy: string) => Promise<void>;
  confirmGroupParticipation: (eventId: string, registrationId: string) => Promise<void>;
  awardEventPoints: (eventId: string, participantId: string, activityPercent: number) => Promise<void>;
  submitEventReview: (eventId: string, rating: number, comment: string) => Promise<void>;
  moderateEventReview: (eventId: string, reviewId: string, approve: boolean, moderator: string) => Promise<void>;
  awardAcademicRating: (points: number) => void;
  purchasePremiumPass: () => Promise<void>;
  claimPassReward: (rewardId: string) => Promise<void>;
  openCase: (caseId: string) => Promise<InventoryItem>;
  completeCaseOpening: (item: InventoryItem) => void;
  submitCertificateRequest: (type: string, comment: string, deliveryMethod: CertificateRequest['deliveryMethod']) => Promise<void>;
  updateCertificateStatus: (id: string, status: CertificateRequest['status']) => Promise<void>;
  uploadCertificateDocument: (id: string, document: NonNullable<CertificateRequest['electronicDocument']>) => Promise<void>;
  createScheduleEntry: (entry: Omit<ScheduleEntry, 'id'>) => Promise<void>;
}

const initialEvents: CampusEvent[] = [
  { id: 'welcome-fest', title: 'Осенний фестиваль', description: 'Музыка, мастер-классы и знакомство с проектами студенческих объединений.', date: '2026-11-06T16:00:00+03:00', endsAt: '2026-11-06T20:00:00+03:00', location: 'Главный корпус · атриум', category: 'Культура', capacity: 120, format: 'individual', groupMinPercent: null, activityType: 'БОА', maxPoints: 100, createdBy: 'Комитет студенческих инициатив', groupRegistrations: [], participants: [], reviews: [] },
  { id: 'design-lab', title: 'Дизайн-лаборатория: интерфейс за вечер', description: 'Командная практика по созданию прототипа приложения и его разбор с наставником.', date: '2026-11-12T17:30:00+03:00', endsAt: '2026-11-12T20:00:00+03:00', location: 'Корпус Б · аудитория 412', category: 'Обучение', capacity: 32, format: 'individual', groupMinPercent: null, activityType: 'БУА', maxPoints: 40, createdBy: 'Мария Иванова', groupRegistrations: [], participants: [{ id: 'participant-alex-design', studentName: 'Александра Ким', group: 'ИС-24', enrolledAt: '2026-10-29T13:00:00+03:00', participationConfirmed: true }], reviews: [] },
  { id: 'volunteer-day', title: 'День волонтёра', description: 'Помощь городскому приюту и сбор необходимых вещей.', date: '2026-11-21T10:00:00+03:00', endsAt: '2026-11-21T15:00:00+03:00', location: 'Сбор у главного входа', category: 'Волонтёрство', capacity: 45, format: 'group', groupMinPercent: 50, activityType: 'БОА', maxPoints: 60, createdBy: 'Комитет студенческих инициатив', groupRegistrations: [], participants: [], reviews: [] },
  { id: 'completed-open-day', title: 'День открытых дверей', description: 'Встреча абитуриентов со студентами и помощь на площадках колледжа.', date: '2026-10-25T10:00:00+03:00', endsAt: '2026-10-25T15:00:00+03:00', location: 'Главный корпус', category: 'Волонтёрство', capacity: 25, format: 'individual', groupMinPercent: null, activityType: 'БОА', maxPoints: 50, createdBy: 'Мария Иванова', groupRegistrations: [], participants: [{ id: 'participant-alex-open-day', studentName: 'Александра Ким', group: 'ИС-24', enrolledAt: '2026-10-20T13:00:00+03:00', participationConfirmed: true }, { id: 'participant-mikhail-open-day', studentName: 'Михаил Орлов', group: 'ИС-24', enrolledAt: '2026-10-20T13:30:00+03:00', participationConfirmed: true }], reviews: [] },
];

const initialConfirmations: GroupConfirmation[] = [
  { id: 'confirm-1', studentName: 'Артём Смирнов', email: 'a.smirnov@example.edu', group: 'ИС-24', submittedAt: '2026-10-29T13:20:00+03:00', status: 'pending' },
  { id: 'confirm-2', studentName: 'Полина Крылова', email: 'p.krylova@example.edu', group: 'ИС-24', submittedAt: '2026-10-30T09:10:00+03:00', status: 'pending' },
  { id: 'confirm-3', studentName: 'Никита Волков', email: 'n.volkov@example.edu', group: 'ИС-24', submittedAt: '2026-10-30T11:45:00+03:00', status: 'pending' },
];

export const teacherGroups = ['ИС-24', 'ИС-23', 'ДИ-23'] as const;

const initialGroupStudents: GroupStudent[] = [
  { id: 'student-alexandra', studentName: 'Александра Ким', email: 'a.kim@example.edu', group: 'ИС-24' },
  { id: 'student-mikhail', studentName: 'Михаил Орлов', email: 'm.orlov@example.edu', group: 'ИС-24' },
  { id: 'student-elena', studentName: 'Елена Смирнова', email: 'e.smirnova@example.edu', group: 'ИС-23' },
  { id: 'student-ivan', studentName: 'Иван Беляев', email: 'i.belyaev@example.edu', group: 'ИС-23' },
  { id: 'student-sofia', studentName: 'София Волкова', email: 's.volkova@example.edu', group: 'ДИ-23' },
];

const initialTransactions: WalletTransaction[] = [
  { id: 'wallet-1', title: 'Конвертация БОА и БУА', detail: 'Начисление за октябрь', amount: 1195, createdAt: '2026-10-28T15:40:00+03:00' },
  { id: 'wallet-2', title: 'Вознаграждение за отзыв', detail: 'Дизайн-лаборатория · отзыв одобрен', amount: 5, createdAt: '2026-10-27T12:15:00+03:00' },
];

const initialInventory: InventoryItem[] = [
  { id: 'case-1', name: 'Кейс кампуса', kind: 'case', source: 'ПТУ-пасс · уровень 30', obtainedAt: '2026-10-24T18:05:00+03:00' },
  { id: 'item-1', name: 'Аватар «Неоновый пиксель»', kind: 'cosmetic', rarity: 'Редкий', source: 'Кейс кампуса', obtainedAt: '2026-10-24T18:05:00+03:00' },
  { id: 'item-2', name: 'Фон профиля «Ночной город»', kind: 'cosmetic', rarity: 'Обычный', source: 'ПТУ-пасс · уровень 3', obtainedAt: '2026-10-18T12:00:00+03:00' },
];

const initialPassRewards: PassReward[] = [
  { id: 'free-5', track: 'free', level: 5, title: 'Кейс кампуса', rewardKind: 'case', claimed: false },
  { id: 'free-10', track: 'free', level: 10, title: 'Аватар «Пиксель»', rewardKind: 'cosmetic', claimed: false },
  { id: 'free-20', track: 'free', level: 20, title: 'Кейс кампуса', rewardKind: 'case', claimed: false },
  { id: 'free-30', track: 'free', level: 30, title: 'Кейс кампуса', rewardKind: 'case', claimed: true },
  { id: 'free-40', track: 'free', level: 40, title: 'Рамка профиля', rewardKind: 'cosmetic', claimed: false },
  { id: 'free-50', track: 'free', level: 50, title: 'Кейс кампуса', rewardKind: 'case', claimed: false },
  { id: 'premium-5', track: 'premium', level: 5, title: 'Эпический аватар', rewardKind: 'cosmetic', claimed: false },
  { id: 'premium-10', track: 'premium', level: 10, title: 'Кейс кампуса', rewardKind: 'case', claimed: false },
  { id: 'premium-20', track: 'premium', level: 20, title: 'Уникальный фон профиля', rewardKind: 'cosmetic', claimed: false },
  { id: 'premium-30', track: 'premium', level: 30, title: 'Кейс кампуса', rewardKind: 'case', claimed: false },
  { id: 'premium-40', track: 'premium', level: 40, title: 'Коллекционная рамка', rewardKind: 'cosmetic', claimed: false },
  { id: 'premium-50', track: 'premium', level: 50, title: 'Редкий кейс кампуса', rewardKind: 'case', claimed: false },
];

export const caseOpeningPrice = 200;
export const campusCaseRewards: CampusCaseReward[] = [
  { name: 'Аватар «Первая пара»', cosmeticType: 'avatar', rarity: 'Обычный', chance: 25 },
  { name: 'Аватар «Неоновый пиксель»', cosmeticType: 'avatar', rarity: 'Редкий', chance: 25 },
  { name: 'Рамка профиля «Лазурь»', cosmeticType: 'avatar-frame', rarity: 'Редкий', chance: 20 },
  { name: 'Фон профиля «Ночной город»', cosmeticType: 'profile-background', rarity: 'Эпический', chance: 15 },
  { name: 'Фон профиля «Пиксельный кампус»', cosmeticType: 'profile-background', rarity: 'Редкий', chance: 10 },
  { name: 'Рамка профиля «Золотая эмблема»', cosmeticType: 'avatar-frame', rarity: 'Эпический', chance: 5 },
];

export function chooseCampusCaseReward(): CampusCaseReward {
  let roll = Math.random() * campusCaseRewards.reduce((total, reward) => total + reward.chance, 0);
  for (const reward of campusCaseRewards) {
    if (roll < reward.chance) return reward;
    roll -= reward.chance;
  }
  throw new Error('В кейсе не настроены награды.');
}

export const campusGroups = [
  { name: 'ИС-24', size: 24 },
  { name: 'ИС-23', size: 22 },
  { name: 'ДИ-23', size: 21 },
  { name: 'КС-22', size: 20 },
] as const;

function mockRequest(): Promise<void> {
  return new Promise((resolve) => window.setTimeout(resolve, 350));
}

function formatNow(): string {
  return new Date().toISOString();
}

function meetsGroupThreshold(event: CampusEvent, registrationId: string | undefined): boolean {
  if (event.format !== 'group' || event.groupMinPercent === null || !registrationId) return event.format === 'individual';
  const registration = event.groupRegistrations.find((item) => item.id === registrationId);
  if (!registration) return false;
  const participants = event.participants.filter((item) => item.groupRegistrationId === registrationId && item.participationConfirmed).length;
  return participants >= Math.ceil(registration.groupSize * event.groupMinPercent / 100);
}

export function CampusWorkflowProvider({ children }: { children: ReactNode }) {
  const [events, setEvents] = useState(initialEvents);
  const [groupConfirmations, setGroupConfirmations] = useState(initialConfirmations);
  const [students, setStudents] = useState(initialGroupStudents);
  const [groupLeaders, setGroupLeaders] = useState<Record<string, string>>({ 'ИС-24': 'student-alexandra' });
  const isGroupLeader = groupLeaders['ИС-24'] === 'student-alexandra';
  const [activityAwards, setActivityAwards] = useState<ActivityAward[]>([]);
  const [academicRating, setAcademicRating] = useState(689);
  const [boaPoints, setBoaPoints] = useState(285);
  const [buaPoints, setBuaPoints] = useState(340);
  const [premiumPassOwned, setPremiumPassOwned] = useState(false);
  const [passRewards, setPassRewards] = useState(initialPassRewards);
  const [walletBalance, setWalletBalance] = useState(1200);
  const [transactions, setTransactions] = useState(initialTransactions);
  const [inventory, setInventory] = useState(initialInventory);
  const caseOpeningLock = useRef(false);
  const [certificateRequests, setCertificateRequests] = useState<CertificateRequest[]>([
    { id: 'certificate-1', studentName: 'Александра Ким', group: 'ИС-24', type: 'Справка об обучении', comment: 'Для предоставления по месту требования', deliveryMethod: 'Электронно', status: 'В обработке', createdAt: '2026-10-28T10:00:00+03:00' },
    { id: 'certificate-2', studentName: 'Михаил Орлов', group: 'КС-22', type: 'Справка для общежития', comment: '', deliveryMethod: 'В учебной части', status: 'Подана', createdAt: '2026-10-30T09:30:00+03:00' },
  ]);
  const [scheduleEntries, setScheduleEntries] = useState<ScheduleEntry[]>([
    { id: 'schedule-1', weekday: 'Понедельник', startTime: '09:00', endTime: '10:30', group: 'ИС-24', subject: 'Веб-разработка', teacher: 'Мария Иванова', room: 'Б-412' },
    { id: 'schedule-2', weekday: 'Понедельник', startTime: '10:45', endTime: '12:15', group: 'ИС-24', subject: 'Базы данных', teacher: 'Дмитрий Петров', room: 'Б-308' },
    { id: 'schedule-3', weekday: 'Вторник', startTime: '09:00', endTime: '10:30', group: 'ИС-23', subject: 'Алгоритмы и структуры данных', teacher: 'Елена Смирнова', room: 'А-205' },
    { id: 'schedule-4', weekday: 'Среда', startTime: '13:00', endTime: '14:30', group: 'ИС-24', subject: 'Проектная практика', teacher: 'Мария Иванова', room: 'Б-412' },
  ]);

  const joinEvent = useCallback(async (eventId: string) => {
    await mockRequest();
    const event = events.find((item) => item.id === eventId);
    if (!event) throw new Error('Мероприятие не найдено.');
    if (event.format !== 'individual') throw new Error('Для группового мероприятия дождитесь записи группы преподавателем или старостой.');
    if (Date.parse(event.date) <= Date.now()) throw new Error('Регистрация на это мероприятие закрыта.');
    if (event.participants.length >= event.capacity) throw new Error('На мероприятии больше нет свободных мест.');
    const existingParticipation = event.participants.some((participant) => participant.studentName === 'Александра Ким');
    if (existingParticipation) throw new Error('Вы уже записаны на это мероприятие.');
    const participant: EventParticipant = {
      id: `participant-${Date.now()}`,
      studentName: 'Александра Ким',
      group: 'ИС-24',
      enrolledAt: formatNow(),
      participationConfirmed: true,
    };
    setEvents((current) => current.map((item) => item.id === eventId
      ? { ...item, participants: [...item.participants, participant] }
      : item));
  }, [events]);

  const addEventGroup = useCallback(async (eventId: string, group: string, groupSize: number, addedBy: string) => {
    await mockRequest();
    const isTeacher = addedBy === 'Мария Иванова';
    const isOwnGroupLeader = isGroupLeader && group === 'ИС-24' && addedBy === 'Александра Ким · староста';
    if (!isTeacher && !isOwnGroupLeader) throw new Error('Группу может записать её преподаватель или староста этой группы.');
    const event = events.find((item) => item.id === eventId);
    if (!event || event.format !== 'group') throw new Error('Коллективное мероприятие не найдено.');
    if (Date.parse(event.date) <= Date.now()) throw new Error('Регистрация на мероприятие закрыта.');
    if (!group.trim() || !Number.isInteger(groupSize) || groupSize <= 0) throw new Error('Проверьте группу и численность студентов.');
    if (event.groupRegistrations.some((registration) => registration.group === group)) {
      throw new Error('Заявка этой группы уже добавлена.');
    }
    const knownGroup = campusGroups.find((campusGroup) => campusGroup.name === group);
    if (!knownGroup || knownGroup.size !== groupSize) throw new Error('Выберите существующую группу с корректной численностью.');
    if (isTeacher && !teacherGroups.some((teacherGroup) => teacherGroup === group)) throw new Error('Преподаватель не закреплён за этой группой.');
    if (event.groupRegistrations.reduce((total, registration) => total + registration.groupSize, 0) + groupSize > event.capacity) {
      throw new Error('Вместимости мероприятия недостаточно для этой группы.');
    }
    const registration: EventGroupRegistration = {
      id: `event-group-${Date.now()}`,
      group,
      groupSize,
      addedBy,
      requestedAt: formatNow(),
    };
    setEvents((current) => current.map((item) => item.id === eventId
      ? { ...item, groupRegistrations: [...item.groupRegistrations, registration] }
      : item));
  }, [events, isGroupLeader]);

  const decideGroupConfirmation = useCallback(async (
    id: string,
    approve: boolean,
    reviewer: { role: 'teacher' | 'group-leader'; group: string },
  ) => {
    await mockRequest();
    const request = groupConfirmations.find((item) => item.id === id);
    if (!request || request.status !== 'pending') throw new Error('Заявка уже обработана или не найдена.');
    if (request.group !== reviewer.group) throw new Error('Заявка относится к другой группе.');
    if (reviewer.role === 'teacher' && !teacherGroups.some((group) => group === reviewer.group)) {
      throw new Error('Эта группа не закреплена за преподавателем.');
    }
    if (reviewer.role === 'group-leader' && (!isGroupLeader || reviewer.group !== 'ИС-24')) {
      throw new Error('Обрабатывать заявки может только староста своей группы.');
    }
    setGroupConfirmations((current) => current.map((item) => item.id === id ? { ...item, status: approve ? 'approved' : 'rejected' } : item));
    if (approve) {
      setStudents((current) => current.some((student) => student.id === `student-${request.id}`)
        ? current
        : [...current, { id: `student-${request.id}`, studentName: request.studentName, email: request.email, group: request.group }]);
    }
  }, [groupConfirmations, isGroupLeader]);

  const assignGroupLeader = useCallback(async (group: string, studentId: string) => {
    await mockRequest();
    if (!teacherGroups.some((teacherGroup) => teacherGroup === group)) throw new Error('Эта группа не закреплена за преподавателем.');
    if (!students.some((student) => student.id === studentId && student.group === group)) {
      throw new Error('Старостой можно назначить только студента этой группы.');
    }
    setGroupLeaders((current) => ({ ...current, [group]: studentId }));
  }, [students]);

  const createEvent = useCallback(async (event: Omit<CampusEvent, 'id' | 'groupRegistrations' | 'participants' | 'reviews' | 'createdBy'>, createdBy: string) => {
    await mockRequest();
    if (!event.title.trim() || !event.description.trim() || !event.location.trim() || !event.category.trim()) {
      throw new Error('Заполните все обязательные поля мероприятия.');
    }
    if (Number.isNaN(Date.parse(event.date)) || Number.isNaN(Date.parse(event.endsAt)) || Date.parse(event.endsAt) <= Date.now() || Date.parse(event.endsAt) <= Date.parse(event.date)) {
      throw new Error('Укажите корректные даты будущего мероприятия.');
    }
    if (!Number.isInteger(event.maxPoints) || event.maxPoints < 0 || event.maxPoints > 100) {
      throw new Error('Максимальная награда должна быть целым числом от 0 до 100.');
    }
    if (!Number.isInteger(event.capacity) || event.capacity <= 0) throw new Error('Вместимость должна быть положительным целым числом.');
    if (event.format === 'group' && (!Number.isInteger(event.groupMinPercent) || (event.groupMinPercent ?? 0) < 1 || (event.groupMinPercent ?? 0) > 100)) {
      throw new Error('Для коллективного мероприятия укажите порог от 1 до 100%.');
    }
    const newEvent: CampusEvent = {
      ...event,
      id: `event-${Date.now()}`,
      createdBy,
      groupRegistrations: [],
      participants: [],
      reviews: [],
    };
    setEvents((current) => [newEvent, ...current]);
  }, []);

  const confirmGroupParticipation = useCallback(async (eventId: string, registrationId: string) => {
    await mockRequest();
    const event = events.find((item) => item.id === eventId);
    const registration = event?.groupRegistrations.find((item) => item.id === registrationId);
    if (!event || !registration) throw new Error('Групповая запись или мероприятие не найдены.');
    if (event.format !== 'group') throw new Error('Подтверждение записи доступно только для группового мероприятия.');
    if (Date.parse(event.date) <= Date.now()) throw new Error('Подтвердить запись можно до начала мероприятия.');
    if (registration.group !== 'ИС-24') throw new Error('Вы можете подтвердить участие только от своей группы.');
    if (event.participants.some((item) => item.studentName === 'Александра Ким')) throw new Error('Вы уже подтвердили участие в этом мероприятии.');
    const chosen = event.participants.filter((item) => item.groupRegistrationId === registrationId).length;
    if (chosen >= registration.groupSize) throw new Error('Все студенческие места группы уже заняты.');
    const participant: EventParticipant = {
      id: `participant-${Date.now()}`,
      studentName: 'Александра Ким',
      group: registration.group,
      enrolledAt: formatNow(),
      participationConfirmed: true,
      groupRegistrationId: registrationId,
    };
    setEvents((current) => current.map((item) => item.id === eventId
      ? { ...item, participants: [...item.participants, participant] }
      : item));
  }, [events]);

  const awardEventPoints = useCallback(async (eventId: string, participantId: string, activityPercent: number) => {
    await mockRequest();
    const event = events.find((item) => item.id === eventId);
    const participant = event?.participants.find((item) => item.id === participantId);
    if (!event || !participant) throw new Error('Участник или мероприятие не найдены.');
    if (Date.parse(event.endsAt) > Date.now()) throw new Error('Баллы можно начислить после окончания мероприятия.');
    if (!meetsGroupThreshold(event, participant.groupRegistrationId)) {
      throw new Error('Групповая заявка отменена: не достигнут порог студентов, подтвердивших участие.');
    }
    if (!participant.participationConfirmed) throw new Error('Баллы начисляются только студентам, записавшимся на мероприятие.');
    if (participant.awardedPoints !== undefined) throw new Error('Баллы этому участнику уже начислены.');
    if (!Number.isInteger(activityPercent) || activityPercent < 0 || activityPercent > 100) {
      throw new Error('Укажите активность целым числом от 0 до 100%.');
    }
    const points = Math.round(event.maxPoints * activityPercent / 100);
    const awardedAt = formatNow();
    const award: ActivityAward = {
      id: `award-${eventId}-${participantId}`,
      eventId,
      eventTitle: event.title,
      studentName: participant.studentName,
      group: participant.group,
      activityType: event.activityType,
      activityPercent,
      points,
      awardedBy: event.createdBy,
      createdAt: awardedAt,
    };
    setEvents((current) => current.map((item) => item.id === eventId
      ? { ...item, participants: item.participants.map((person) => person.id === participantId ? { ...person, awardedPoints: points, awardedPercent: activityPercent } : person) }
      : item));
    setActivityAwards((current) => [award, ...current]);
    if (participant.studentName === 'Александра Ким') {
      if (event.activityType === 'БОА') setBoaPoints((current) => current + points);
      else setBuaPoints((current) => current + points);
    }
  }, [events]);

  const submitEventReview = useCallback(async (eventId: string, rating: number, comment: string) => {
    await mockRequest();
    const event = events.find((item) => item.id === eventId);
    if (!event || Date.parse(event.endsAt) > Date.now()) throw new Error('Оставить отзыв можно после завершения мероприятия.');
    const participant = event.participants.find((item) => item.studentName === 'Александра Ким' && item.participationConfirmed === true);
    if (!participant) throw new Error('Отзыв доступен только студенту с подтверждённым участием.');
    if (!meetsGroupThreshold(event, participant.groupRegistrationId)) {
      throw new Error('Групповая заявка не прошла порог участия, поэтому оставить отзыв нельзя.');
    }
    if (event.reviews.some((review) => review.studentName === participant.studentName)) {
      throw new Error('Отзыв на это мероприятие уже отправлен.');
    }
    if (!Number.isInteger(rating) || rating < 1 || rating > 5 || comment.trim().length < 20) {
      throw new Error('Укажите оценку от 1 до 5 и содержательный комментарий не короче 20 символов.');
    }
    const review: EventReview = {
      id: `review-${eventId}-${participant.id}`,
      studentName: participant.studentName,
      rating,
      comment: comment.trim(),
      status: 'pending',
      createdAt: formatNow(),
    };
    setEvents((current) => current.map((item) => item.id === eventId ? { ...item, reviews: [...item.reviews, review] } : item));
  }, [events]);

  const moderateEventReview = useCallback(async (eventId: string, reviewId: string, approve: boolean, moderator: string) => {
    await mockRequest();
    const event = events.find((item) => item.id === eventId);
    const review = event?.reviews.find((item) => item.id === reviewId);
    if (!event || !review || review.status !== 'pending') throw new Error('Отзыв уже обработан или не найден.');
    if (event.createdBy !== moderator) throw new Error('Обрабатывать отзывы может создатель мероприятия.');
    if (approve) {
      const currentDate = new Date();
      const monthlyReviewRewards = transactions.filter((transaction) => {
        const transactionDate = new Date(transaction.createdAt);
        return transaction.title === 'Вознаграждение за отзыв'
          && transactionDate.getFullYear() === currentDate.getFullYear()
          && transactionDate.getMonth() === currentDate.getMonth();
      }).length;
      if (monthlyReviewRewards >= 5) throw new Error('Достигнут месячный лимит оплачиваемых отзывов.');
      const rewardedAt = formatNow();
      setWalletBalance((current) => current + 5);
      setTransactions((current) => [
        { id: `wallet-review-${review.id}`, title: 'Вознаграждение за отзыв', detail: `${event.title} · отзыв одобрен`, amount: 5, createdAt: rewardedAt },
        ...current,
      ]);
    }
    setEvents((current) => current.map((item) => item.id === eventId
      ? { ...item, reviews: item.reviews.map((entry) => entry.id === reviewId ? { ...entry, status: approve ? 'approved' : 'rejected' } : entry) }
      : item));
  }, [events, transactions]);

  const awardAcademicRating = useCallback((points: number) => {
    if (!Number.isInteger(points) || points < 0) throw new Error('АР должны быть неотрицательным целым числом.');
    setAcademicRating((current) => current + points);
  }, []);

  const purchasePremiumPass = useCallback(async () => {
    await mockRequest();
    const price = 500;
    if (premiumPassOwned) throw new Error('Премиальный пропуск уже куплен для этого сезона.');
    if (walletBalance < price) throw new Error('Недостаточно валюты для покупки премиального пропуска.');
    const purchasedAt = formatNow();
    setWalletBalance((current) => current - price);
    setTransactions((current) => [
      { id: `wallet-premium-${Date.now()}`, title: 'Премиальный пропуск', detail: 'Сезон 2026/27', amount: -price, createdAt: purchasedAt },
      ...current,
    ]);
    setPremiumPassOwned(true);
  }, [premiumPassOwned, walletBalance]);

  const claimPassReward = useCallback(async (rewardId: string) => {
    await mockRequest();
    const reward = passRewards.find((item) => item.id === rewardId);
    if (!reward) throw new Error('Награда не найдена.');
    if (reward.claimed) throw new Error('Эта награда уже получена.');
    if (reward.level > Math.floor(academicRating / 20)) throw new Error('Сначала достигните нужного уровня.');
    if (reward.track === 'premium' && !premiumPassOwned) throw new Error('Для этой награды нужен премиальный пропуск.');
    const obtainedAt = formatNow();
    setPassRewards((current) => current.map((item) => item.id === rewardId ? { ...item, claimed: true } : item));
    setInventory((current) => [{
      id: `item-${rewardId}`,
      name: reward.title,
      kind: reward.rewardKind,
      ...(reward.rewardKind === 'cosmetic' ? { rarity: 'Редкий' as const } : {}),
      source: `ПТУ-пасс · уровень ${reward.level}`,
      obtainedAt,
    }, ...current]);
  }, [academicRating, passRewards, premiumPassOwned]);

  const openCase = useCallback(async (caseId: string) => {
    if (caseOpeningLock.current) throw new Error('Дождитесь завершения текущего открытия кейса.');
    caseOpeningLock.current = true;
    try {
      await mockRequest();
      const caseItem = inventory.find((item) => item.id === caseId && item.kind === 'case');
      if (!caseItem) throw new Error('Кейс отсутствует в инвентаре или уже открыт.');
      if (walletBalance < caseOpeningPrice) throw new Error(`Для открытия кейса нужно ${caseOpeningPrice} жетонов.`);
      const reward = chooseCampusCaseReward();
      const obtainedAt = formatNow();
      const item: InventoryItem = {
        ...reward,
        kind: 'cosmetic',
        source: 'Кейс кампуса',
        id: `item-${Date.now()}`,
        obtainedAt,
      };
      setWalletBalance((current) => current - caseOpeningPrice);
      setTransactions((current) => [
        { id: `wallet-case-${item.id}`, title: 'Открытие кейса', detail: caseItem.name, amount: -caseOpeningPrice, createdAt: obtainedAt },
        ...current,
      ]);
      setInventory((current) => current.filter((inventoryItem) => inventoryItem.id !== caseId));
      return item;
    } finally {
      caseOpeningLock.current = false;
    }
  }, [inventory, walletBalance]);

  const completeCaseOpening = useCallback((item: InventoryItem) => {
    setInventory((current) => current.some((inventoryItem) => inventoryItem.id === item.id)
      ? current
      : [item, ...current]);
  }, []);

  const submitCertificateRequest = useCallback(async (type: string, comment: string, deliveryMethod: CertificateRequest['deliveryMethod']) => {
    await mockRequest();
    if (!type || !deliveryMethod) throw new Error('Выберите вид справки и способ получения.');
    setCertificateRequests((current) => [
      { id: `certificate-${Date.now()}`, studentName: 'Александра Ким', group: 'ИС-24', type, comment: comment.trim(), deliveryMethod, status: 'Подана', createdAt: formatNow() },
      ...current,
    ]);
  }, []);

  const updateCertificateStatus = useCallback(async (id: string, status: CertificateRequest['status']) => {
    await mockRequest();
    const request = certificateRequests.find((item) => item.id === id);
    if (!request) throw new Error('Заявка на справку не найдена.');
    const allowedTransitions: Record<CertificateRequest['status'], CertificateRequest['status'][]> = {
      Подана: ['В обработке', 'Отклонена', 'Отменена'],
      'В обработке': ['Готова', 'Отклонена', 'Отменена'],
      Готова: ['Выдана', 'Отменена'],
      Выдана: [],
      Отклонена: [],
      Отменена: [],
    };
    if (!allowedTransitions[request.status].includes(status)) {
      throw new Error(`Нельзя изменить статус «${request.status}» на «${status}».`);
    }
    setCertificateRequests((current) => current.map((item) => item.id === id ? { ...item, status } : item));
  }, [certificateRequests]);

  const uploadCertificateDocument = useCallback(async (id: string, document: NonNullable<CertificateRequest['electronicDocument']>) => {
    await mockRequest();
    const request = certificateRequests.find((item) => item.id === id);
    if (!request) throw new Error('Заявка на справку не найдена.');
    if (request.deliveryMethod !== 'Электронно') throw new Error('Загрузить электронный файл можно только для электронной справки.');
    if (request.status !== 'В обработке') throw new Error('Переведите заявку в обработку перед загрузкой справки.');
    if (!document.fileName.trim() || !document.dataUrl.startsWith('data:application/pdf;base64,')) {
      throw new Error('Загрузите файл справки в формате PDF.');
    }
    setCertificateRequests((current) => current.map((item) => item.id === id
      ? { ...item, electronicDocument: document, status: 'Готова' }
      : item));
  }, [certificateRequests]);

  const createScheduleEntry = useCallback(async (entry: Omit<ScheduleEntry, 'id'>) => {
    await mockRequest();
    if (!entry.weekday || !entry.group.trim() || !entry.subject.trim() || !entry.teacher.trim() || !entry.room.trim()) {
      throw new Error('Заполните день, группу, дисциплину, преподавателя и аудиторию.');
    }
    if (!/^\d{2}:\d{2}$/.test(entry.startTime) || !/^\d{2}:\d{2}$/.test(entry.endTime) || entry.startTime >= entry.endTime) {
      throw new Error('Укажите корректное время начала и окончания пары.');
    }
    const collision = scheduleEntries.find((item) =>
      item.weekday === entry.weekday &&
      item.group === entry.group &&
      entry.startTime < item.endTime &&
      entry.endTime > item.startTime,
    );
    if (collision) throw new Error(`У группы ${entry.group} уже есть занятие «${collision.subject}» в это время.`);
    setScheduleEntries((current) => [
      ...current,
      { ...entry, id: `schedule-${Date.now()}` },
    ]);
  }, [scheduleEntries]);

  const value: CampusWorkflowValue = {
    isGroupLeader,
    students,
    teacherGroups,
    groupLeaders,
    events,
    groupConfirmations,
    activityAwards,
    academicRating,
    boaPoints,
    buaPoints,
    passExperience: academicRating,
    premiumPassOwned,
    passRewards,
    walletBalance,
    transactions,
    inventory,
    certificateRequests,
    scheduleEntries,
    joinEvent,
    addEventGroup,
    decideGroupConfirmation,
    assignGroupLeader,
    createEvent,
    confirmGroupParticipation,
    awardEventPoints,
    submitEventReview,
    moderateEventReview,
    awardAcademicRating,
    purchasePremiumPass,
    claimPassReward,
    openCase,
    completeCaseOpening,
    submitCertificateRequest,
    updateCertificateStatus,
    uploadCertificateDocument,
    createScheduleEntry,
  };

  return <CampusWorkflowContext.Provider value={value}>{children}</CampusWorkflowContext.Provider>;
}

const CampusWorkflowContext = createContext<CampusWorkflowValue | null>(null);

export function useCampusWorkflow(): CampusWorkflowValue {
  const context = useContext(CampusWorkflowContext);
  if (!context) throw new Error('useCampusWorkflow must be used within CampusWorkflowProvider');
  return context;
}
