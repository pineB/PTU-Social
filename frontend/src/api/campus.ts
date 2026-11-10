import apiClient from './client';

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
  creatorId?: string;
  rating: number | null;
  groupRegistrations: EventGroupRegistration[];
  participants: EventParticipant[];
  reviews: EventReview[];
}

export interface EventReview {
  id: string;
  studentId?: string;
  studentName: string;
  rating: number;
  comment: string;
  status: 'pending' | 'approved' | 'rejected';
  createdAt: string;
}

export interface OrganizerReview {
  id: string;
  eventTitle: string;
  studentName: string;
  rating: number;
  comment: string;
  createdAt: string;
}

export interface EventModerationReview {
  id: string;
  eventId: string;
  eventTitle: string;
  studentName: string;
  group: string;
  rating: number;
  comment: string;
  createdAt: string;
  flaggedWords: string[];
}

export interface EventParticipant {
  id: string;
  studentId?: string;
  studentName: string;
  group: string;
  enrolledAt: string;
  participationConfirmed: boolean;
  groupRegistrationId?: string;
  awardedPoints?: number;
  awardedPercent?: number;
  awardedAt?: string;
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
  awardedById?: string;
  createdAt: string;
}

export interface WalletTransaction {
  id: string;
  title: string;
  detail: string;
  amount: number;
  createdAt: string;
}

export interface WalletSnapshot {
  balance: number;
  transactions: WalletTransaction[];
}

export interface InventoryItem {
  id: string;
  name: string;
  kind: 'case' | 'cosmetic';
  cosmeticType?: 'avatar' | 'avatar-frame' | 'profile-background' | 'badge';
  rarity?: 'Обычный' | 'Редкий' | 'Эпический';
  source: string;
  obtainedAt: string;
  caseId: string;
  imageUrl: string;
}

export interface CampusCaseReward {
  name: string;
  cosmeticType: NonNullable<InventoryItem['cosmeticType']>;
  rarity: NonNullable<InventoryItem['rarity']>;
  chance: number;
  imageUrl: string;
}

export interface CampusCase {
  id: string;
  name: string;
  description: string;
  price: number;
  imageUrl: string;
  rewards: CampusCaseReward[];
}

export interface ProfileCosmetics {
  avatarItemId: string | null;
  frameItemId: string | null;
  backgroundItemId: string | null;
}

export interface CampusGroup {
  id: string;
  name: string;
  size: number;
  leaderId?: string | null;
}

export interface ScheduleEntry {
  id: string;
  weekday: string;
  startTime: string;
  endTime: string;
  groupId: string;
  group: string;
  subject: string;
  disciplineId: string | null;
  teacherId: string | null;
  teacher: string;
  room: string;
}

export interface ScheduleChange {
  id: string;
  type: 'ADDED' | 'CHANGED' | 'CANCELLED';
  message: string;
  groupName: string;
  createdAt: string;
}

export interface PassReward {
  id: string;
  track: 'free' | 'premium';
  level: number;
  title: string;
  rewardKind: 'case' | 'cosmetic';
  claimed: boolean;
  imageUrl: string | null;
}

export interface BattlePassSnapshot {
  experience: number;
  premiumPassOwned: boolean;
  premiumPrice: number;
  seasonName: string;
  startsAt: string;
  endsAt: string;
  rewards: PassReward[];
}

export interface CampusCatalog {
  groups: CampusGroup[];
  caseOpeningPrice: number;
  caseRewards: CampusCaseReward[];
  cases: CampusCase[];
}

export interface CreateCampusEventRequest {
  title: string;
  description: string;
  date: string;
  endsAt: string;
  location: string;
  category: string;
  capacity: number;
  format: CampusEvent['format'];
  groupMinPercent: number | null;
  activityType: CampusEvent['activityType'];
  maxPoints: number;
}

export async function listCampusEvents(): Promise<CampusEvent[]> {
  const response = await apiClient.get<CampusEvent[]>('/campus/events');
  return response.data.map(normalizeCampusEvent);
}

export async function listOrganizerReviews(organizerId: string): Promise<OrganizerReview[]> {
  const response = await apiClient.get<OrganizerReview[]>(`/campus/organizers/${organizerId}/reviews`);
  return response.data;
}

export async function createCampusEvent(event: CreateCampusEventRequest): Promise<CampusEvent> {
  const response = await apiClient.post<CampusEvent>('/campus/events', {
    ...event,
    date: new Date(event.date).toISOString(),
    endsAt: new Date(event.endsAt).toISOString(),
  });
  return normalizeCampusEvent(response.data);
}

function normalizeCampusEvent(event: CampusEvent): CampusEvent {
  const format = event.format.toLowerCase();
  if (format !== 'group' && format !== 'individual') {
    throw new Error('Не удалось определить формат мероприятия.');
  }
  return {
    ...event,
    format,
  };
}

export async function joinCampusEvent(eventId: string): Promise<void> {
  await apiClient.post(`/campus/events/${eventId}/join`);
}

export async function addCampusEventGroup(eventId: string, groupId: string): Promise<void> {
  await apiClient.post(`/campus/events/${eventId}/groups`, { groupId });
}

export async function confirmCampusEventGroup(eventId: string, registrationId: string): Promise<void> {
  await apiClient.post(`/campus/events/${eventId}/groups/${registrationId}/confirm`);
}

export async function awardCampusEventPoints(eventId: string, participantId: string, activityPercent: number): Promise<void> {
  await apiClient.post(`/campus/events/${eventId}/participants/${participantId}/award`, { activityPercent });
}

export async function submitCampusEventReview(eventId: string, rating: number, comment: string): Promise<void> {
  await apiClient.post(`/campus/events/${eventId}/reviews`, { rating, comment });
}

export async function moderateCampusEventReview(eventId: string, reviewId: string, approved: boolean): Promise<void> {
  await apiClient.patch(`/campus/events/${eventId}/reviews/${reviewId}`, { approved });
}

export async function listEventReviewModerationQueue(): Promise<EventModerationReview[]> {
  const response = await apiClient.get<EventModerationReview[]>('/campus/reviews/moderation');
  return response.data;
}

export async function getCampusGroups(): Promise<CampusGroup[]> {
  const response = await apiClient.get<CampusGroup[]>('/campus/groups');
  return response.data;
}

export async function listCampusStudents(groupId: string): Promise<GroupStudent[]> {
  const response = await apiClient.get<GroupStudent[]>('/campus/students', { params: { groupId } });
  return response.data;
}

export async function listGroupConfirmations(groupId: string): Promise<GroupConfirmation[]> {
  const response = await apiClient.get<GroupConfirmation[]>('/campus/group-confirmations', { params: { groupId } });
  return response.data;
}

export async function updateGroupConfirmation(id: string, approve: boolean): Promise<void> {
  await apiClient.patch(`/campus/group-confirmations/${id}`, { approve });
}

export async function getMyGroupConfirmation(): Promise<GroupConfirmation | null> {
  const response = await apiClient.get<GroupConfirmation | null>('/campus/group-confirmations/me');
  return response.status === 204 ? null : response.data;
}

export async function assignCampusGroupLeader(groupId: string, studentId: string): Promise<void> {
  await apiClient.put(`/campus/groups/${groupId}/leader`, { studentId });
}

export async function requestCampusGroupMembership(): Promise<GroupConfirmation> {
  const response = await apiClient.post<GroupConfirmation>('/campus/group-confirmations/me');
  return response.data;
}

export async function getCampusCatalog(): Promise<CampusCatalog> {
  const response = await apiClient.get<CampusCatalog>('/campus/catalog');
  return response.data;
}

export async function getProfileCosmetics(): Promise<ProfileCosmetics> {
  const response = await apiClient.get<ProfileCosmetics>('/campus/profile/cosmetics');
  return response.data;
}

export async function saveProfileCosmetics(cosmetics: ProfileCosmetics): Promise<ProfileCosmetics> {
  const response = await apiClient.put<ProfileCosmetics>('/campus/profile/cosmetics', cosmetics);
  return response.data;
}

export async function getBattlePass(): Promise<BattlePassSnapshot> {
  const response = await apiClient.get<BattlePassSnapshot>('/campus/pass');
  return response.data;
}

export async function purchasePremiumPass(): Promise<void> {
  await apiClient.post('/campus/pass/premium');
}

export async function claimBattlePassReward(rewardId: string): Promise<void> {
  await apiClient.post(`/campus/pass/rewards/${rewardId}/claim`);
}

export async function openCampusCase(caseId: string): Promise<InventoryItem> {
  const response = await apiClient.post<InventoryItem>(`/campus/cases/${caseId}/open`);
  return response.data;
}

export async function getCampusInventory(): Promise<InventoryItem[]> {
  const response = await apiClient.get<InventoryItem[]>('/campus/inventory');
  return response.data;
}

export async function getCampusSchedule(): Promise<ScheduleEntry[]> {
  const response = await apiClient.get<ScheduleEntry[]>('/campus/schedule');
  return response.data;
}

export async function createCampusScheduleEntry(entry: Omit<ScheduleEntry, 'id'>): Promise<ScheduleEntry> {
  const response = await apiClient.post<ScheduleEntry>('/campus/schedule', entry);
  return response.data;
}

export async function updateCampusScheduleEntry(id: string, entry: Omit<ScheduleEntry, 'id'>): Promise<ScheduleEntry> {
  const response = await apiClient.put<ScheduleEntry>(`/campus/schedule/${id}`, entry);
  return response.data;
}

export async function deleteCampusScheduleEntry(id: string): Promise<void> {
  await apiClient.delete(`/campus/schedule/${id}`);
}

export async function updateCampusScheduleTimeSlot(request: {
  previousStartTime: string;
  previousEndTime: string;
  startTime: string;
  endTime: string;
}): Promise<void> {
  await apiClient.patch('/campus/schedule/time-slot', request);
}

export async function getCampusScheduleChanges(): Promise<ScheduleChange[]> {
  const response = await apiClient.get<ScheduleChange[]>('/campus/schedule/changes');
  return response.data;
}

export async function getWallet(): Promise<WalletSnapshot> {
  const response = await apiClient.get<WalletSnapshot>('/wallet/me');
  return response.data;
}
