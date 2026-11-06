import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import {
  addCampusEventGroup as addCampusEventGroupRequest,
  assignCampusGroupLeader,
  awardCampusEventPoints,
  claimBattlePassReward,
  confirmCampusEventGroup,
  createCampusEvent,
  createCampusScheduleEntry,
  deleteCampusScheduleEntry,
  getBattlePass,
  getCampusCatalog,
  getCampusGroups,
  getCampusInventory,
  getCampusSchedule,
  getCampusScheduleChanges,
  getMyGroupConfirmation,
  getWallet,
  joinCampusEvent,
  listCampusEvents,
  listCampusStudents,
  listGroupConfirmations,
  moderateCampusEventReview,
  openCampusCase,
  purchasePremiumPass as purchasePremiumPassRequest,
  requestCampusGroupMembership,
  submitCampusEventReview,
  updateCampusScheduleEntry,
  updateCampusScheduleTimeSlot as updateCampusScheduleTimeSlotRequest,
  updateGroupConfirmation,
  type ActivityAward,
  type CampusCaseReward,
  type CampusEvent,
  type CampusGroup,
  type EventGroupRegistration,
  type EventParticipant,
  type EventReview,
  type GroupConfirmation,
  type GroupStudent,
  type InventoryItem,
  type PassReward,
  type ScheduleEntry,
  type ScheduleChange,
  type WalletTransaction,
} from '../api/campus';
import { useAuth } from '../auth/AuthProvider';
import type { CreateCampusEventRequest } from '../api/campus';

export type {
  ActivityAward,
  CampusCaseReward,
  CampusEvent,
  EventGroupRegistration,
  EventParticipant,
  EventReview,
  GroupConfirmation,
  GroupStudent,
  InventoryItem,
  PassReward,
  ScheduleChange,
  ScheduleEntry,
  WalletTransaction,
};

interface CampusWorkflowValue {
  isGroupLeader: boolean;
  students: GroupStudent[];
  teacherGroups: readonly string[];
  campusGroups: CampusGroup[];
  groupLeaders: Record<string, string>;
  events: CampusEvent[];
  groupConfirmations: GroupConfirmation[];
  myGroupConfirmation: GroupConfirmation | null;
  activityAwards: ActivityAward[];
  academicRating: number | null;
  boaPoints: number | null;
  buaPoints: number | null;
  passExperience: number | null;
  premiumPassOwned: boolean | null;
  premiumPassPrice: number | null;
  passRewards: PassReward[];
  walletBalance: number | null;
  transactions: WalletTransaction[];
  inventory: InventoryItem[];
  caseOpeningPrice: number | null;
  campusCaseRewards: CampusCaseReward[];
  scheduleEntries: ScheduleEntry[];
  scheduleChanges: ScheduleChange[];
  isLoading: boolean;
  error: string;
  refresh: () => Promise<void>;
  joinEvent: (eventId: string) => Promise<void>;
  addEventGroup: (eventId: string, group: string, groupSize: number, addedBy: string) => Promise<void>;
  decideGroupConfirmation: (id: string, approve: boolean, reviewer: { role: 'teacher' | 'group-leader'; group: string }) => Promise<void>;
  requestGroupConfirmation: () => Promise<void>;
  assignGroupLeader: (group: string, studentId: string) => Promise<void>;
  createEvent: (event: CreateCampusEventRequest) => Promise<void>;
  confirmGroupParticipation: (eventId: string, registrationId: string) => Promise<void>;
  awardEventPoints: (eventId: string, participantId: string, activityPercent: number) => Promise<void>;
  submitEventReview: (eventId: string, rating: number, comment: string) => Promise<void>;
  moderateEventReview: (eventId: string, reviewId: string, approve: boolean, moderator: string) => Promise<void>;
  purchasePremiumPass: () => Promise<void>;
  claimPassReward: (rewardId: string) => Promise<void>;
  openCase: (caseId: string) => Promise<InventoryItem>;
  saveScheduleEntry: (entry: Omit<ScheduleEntry, 'id'>, id?: string) => Promise<void>;
  deleteScheduleEntry: (id: string) => Promise<void>;
  updateScheduleTimeSlot: (request: {
    previousStartTime: string;
    previousEndTime: string;
    startTime: string;
    endTime: string;
  }) => Promise<void>;
}

const CampusWorkflowContext = createContext<CampusWorkflowValue | null>(null);

export function CampusWorkflowProvider({ children }: { children: ReactNode }) {
  const { currentUser, session, refreshProfile } = useAuth();
  const [events, setEvents] = useState<CampusEvent[]>([]);
  const [students, setStudents] = useState<GroupStudent[]>([]);
  const [campusGroups, setCampusGroups] = useState<CampusGroup[]>([]);
  const [groupConfirmations, setGroupConfirmations] = useState<GroupConfirmation[]>([]);
  const [myGroupConfirmation, setMyGroupConfirmation] = useState<GroupConfirmation | null>(null);
  const [passExperience, setPassExperience] = useState<number | null>(null);
  const [premiumPassOwned, setPremiumPassOwned] = useState<boolean | null>(null);
  const [premiumPassPrice, setPremiumPassPrice] = useState<number | null>(null);
  const [passRewards, setPassRewards] = useState<PassReward[]>([]);
  const [walletBalance, setWalletBalance] = useState<number | null>(null);
  const [transactions, setTransactions] = useState<WalletTransaction[]>([]);
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [caseOpeningPrice, setCaseOpeningPrice] = useState<number | null>(null);
  const [campusCaseRewards, setCampusCaseRewards] = useState<CampusCaseReward[]>([]);
  const [scheduleEntries, setScheduleEntries] = useState<ScheduleEntry[]>([]);
  const [scheduleChanges, setScheduleChanges] = useState<ScheduleChange[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const refresh = useCallback(async () => {
    if (!session || !currentUser || session.role === 'ADMIN') {
      setEvents([]);
      setStudents([]);
      setCampusGroups([]);
      setGroupConfirmations([]);
      setMyGroupConfirmation(null);
      setPassExperience(null);
      setPremiumPassOwned(null);
      setPremiumPassPrice(null);
      setPassRewards([]);
      setWalletBalance(null);
      setTransactions([]);
      setInventory([]);
      setCaseOpeningPrice(null);
      setCampusCaseRewards([]);
      setScheduleEntries([]);
      setScheduleChanges([]);
      setError('');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError('');
    const errors: string[] = [];
    const load = async <T,>(request: () => Promise<T>, apply: (data: T) => void): Promise<void> => {
      try {
        apply(await request());
      } catch (requestError) {
        errors.push(requestError instanceof Error ? requestError.message : 'Не удалось загрузить данные кампуса.');
      }
    };

    try {
      const groupsRequest = getCampusGroups();
      await Promise.all([
        load(listCampusEvents, setEvents),
        load(getCampusSchedule, setScheduleEntries),
        load(async () => {
          const catalog = await getCampusCatalog();
          setCampusCaseRewards(catalog.caseRewards);
          setCaseOpeningPrice(catalog.caseOpeningPrice);
        }, () => undefined),
        ...(session.role === 'STUDENT' ? [
          load(getCampusScheduleChanges, setScheduleChanges),
          load(getMyGroupConfirmation, setMyGroupConfirmation),
          load(async () => {
            const wallet = await getWallet();
            setWalletBalance(wallet.balance);
            setTransactions(wallet.transactions);
          }, () => undefined),
          load(async () => {
            const pass = await getBattlePass();
            setPassExperience(pass.experience);
            setPremiumPassOwned(pass.premiumPassOwned);
            setPremiumPassPrice(pass.premiumPrice);
            setPassRewards(pass.rewards);
          }, () => undefined),
          load(getCampusInventory, setInventory),
        ] : []),
        load(async () => {
          const groups = await groupsRequest;
          setCampusGroups(groups);
          const groupIds = session.role === 'TEACHER'
            ? groups.map((group) => group.id)
            : currentUser.groupLeader && currentUser.groupId ? [currentUser.groupId] : [];
          const data = await Promise.all(groupIds.map(async (groupId) => {
            const [groupStudents, confirmations] = await Promise.all([
              listCampusStudents(groupId),
              listGroupConfirmations(groupId),
            ]);
            return { groupStudents, confirmations };
          }));
          setStudents(data.flatMap((item) => item.groupStudents));
          setGroupConfirmations(data.flatMap((item) => item.confirmations));
        }, () => undefined),
      ]);
    } finally {
      setError([...new Set(errors)].join(' '));
      setIsLoading(false);
    }
  }, [currentUser, session]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const runAndRefresh = useCallback(async (request: () => Promise<unknown>, reloadProfile = false) => {
    await request();
    if (reloadProfile) await refreshProfile();
    await refresh();
  }, [refresh, refreshProfile]);

  const activityAwards = useMemo<ActivityAward[]>(() => events.flatMap((event) => (
    event.participants.flatMap((participant) => participant.awardedPercent === undefined || participant.awardedPoints === undefined
      ? []
      : [{
          id: `${event.id}:${participant.id}`,
          eventId: event.id,
          eventTitle: event.title,
          studentName: participant.studentName,
          group: participant.group,
          activityType: event.activityType,
          activityPercent: participant.awardedPercent,
          points: participant.awardedPoints,
          awardedBy: event.createdBy,
          awardedById: event.creatorId,
          createdAt: participant.awardedAt ?? participant.enrolledAt,
        }])
  )), [events]);
  const groupLeaders = useMemo(() => Object.fromEntries([
    ...campusGroups.flatMap((group) => group.leaderId ? [[group.name, group.leaderId] as const] : []),
    ...(currentUser?.groupLeader && currentUser.groupName ? [[currentUser.groupName, currentUser.id] as const] : []),
  ]), [campusGroups, currentUser]);
  const isGroupLeader = currentUser?.groupLeader ?? false;
  const teacherGroups = useMemo(() => currentUser?.teachingGroups.map((group) => group.name) ?? [], [currentUser]);

  const value = useMemo<CampusWorkflowValue>(() => ({
    isGroupLeader,
    students,
    teacherGroups,
    campusGroups,
    groupLeaders,
    events,
    groupConfirmations,
    myGroupConfirmation,
    activityAwards,
    academicRating: currentUser?.academicRating ?? null,
    boaPoints: currentUser?.boaPoints ?? null,
    buaPoints: currentUser?.buaPoints ?? null,
    passExperience,
    premiumPassOwned,
    premiumPassPrice,
    passRewards,
    walletBalance,
    transactions,
    inventory,
    caseOpeningPrice,
    campusCaseRewards,
    scheduleEntries,
    scheduleChanges,
    isLoading,
    error,
    refresh,
    joinEvent: (eventId) => runAndRefresh(() => joinCampusEvent(eventId)),
    addEventGroup: (eventId, groupName) => {
      const group = campusGroups.find((item) => item.name === groupName);
      if (!group) return Promise.reject(new Error('Выбранная группа не найдена в каталоге.'));
      return runAndRefresh(() => addCampusEventGroupRequest(eventId, group.id));
    },
    decideGroupConfirmation: (id, approve, reviewer) => {
      if (!campusGroups.some((item) => item.name === reviewer.group)) return Promise.reject(new Error('Учебная группа не найдена.'));
      return runAndRefresh(() => updateGroupConfirmation(id, approve));
    },
    requestGroupConfirmation: () => runAndRefresh(requestCampusGroupMembership),
    assignGroupLeader: (groupName, studentId) => {
      const group = campusGroups.find((item) => item.name === groupName);
      if (!group) return Promise.reject(new Error('Учебная группа не найдена.'));
      return runAndRefresh(() => assignCampusGroupLeader(group.id, studentId));
    },
    createEvent: (event) => runAndRefresh(() => createCampusEvent(event)),
    confirmGroupParticipation: (eventId, registrationId) => runAndRefresh(() => confirmCampusEventGroup(eventId, registrationId)),
    awardEventPoints: (eventId, participantId, activityPercent) => runAndRefresh(() => awardCampusEventPoints(eventId, participantId, activityPercent), true),
    submitEventReview: (eventId, rating, comment) => runAndRefresh(() => submitCampusEventReview(eventId, rating, comment)),
    moderateEventReview: (eventId, reviewId, approve) => runAndRefresh(() => moderateCampusEventReview(eventId, reviewId, approve)),
    purchasePremiumPass: () => runAndRefresh(purchasePremiumPassRequest),
    claimPassReward: (rewardId) => runAndRefresh(() => claimBattlePassReward(rewardId)),
    openCase: async (caseId) => {
      const reward = await openCampusCase(caseId);
      await refresh();
      return reward;
    },
    saveScheduleEntry: (entry, id) => runAndRefresh(() => id
      ? updateCampusScheduleEntry(id, entry)
      : createCampusScheduleEntry(entry)),
    deleteScheduleEntry: (id) => runAndRefresh(() => deleteCampusScheduleEntry(id)),
    updateScheduleTimeSlot: (request) => runAndRefresh(() => updateCampusScheduleTimeSlotRequest(request)),
  }), [
    isGroupLeader, students, teacherGroups, campusGroups, groupLeaders, events, groupConfirmations, myGroupConfirmation, activityAwards,
    currentUser, passExperience, premiumPassOwned, premiumPassPrice, passRewards, walletBalance, transactions,
    inventory, caseOpeningPrice, campusCaseRewards, scheduleEntries, scheduleChanges, isLoading, error, refresh, runAndRefresh,
  ]);

  return <CampusWorkflowContext.Provider value={value}>{children}</CampusWorkflowContext.Provider>;
}

export function useCampusWorkflow(): CampusWorkflowValue {
  const context = useContext(CampusWorkflowContext);
  if (!context) throw new Error('useCampusWorkflow must be used within CampusWorkflowProvider');
  return context;
}
