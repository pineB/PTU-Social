import { useEffect, useLayoutEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  LinearProgress,
  MenuItem,
  Rating,
  Select,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import EventAvailableRoundedIcon from '@mui/icons-material/EventAvailableRounded';
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded';
import AccountBalanceWalletRoundedIcon from '@mui/icons-material/AccountBalanceWalletRounded';
import CardGiftcardRoundedIcon from '@mui/icons-material/CardGiftcardRounded';
import WorkspacePremiumRoundedIcon from '@mui/icons-material/WorkspacePremiumRounded';
import CheckRoundedIcon from '@mui/icons-material/CheckRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import CalendarMonthRoundedIcon from '@mui/icons-material/CalendarMonthRounded';
import ScheduleRoundedIcon from '@mui/icons-material/ScheduleRounded';
import PersonAddAltRoundedIcon from '@mui/icons-material/PersonAddAltRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import NotificationsActiveRoundedIcon from '@mui/icons-material/NotificationsActiveRounded';
import SaveRoundedIcon from '@mui/icons-material/SaveRounded';
import RateReviewRoundedIcon from '@mui/icons-material/RateReviewRounded';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { useCampusWorkflow, type CampusEvent, type InventoryItem, type PassReward, type ScheduleEntry } from '../data/campus-workflow';
import { listEventReviewModerationQueue, listOrganizerReviews, moderateCampusEventReview, type EventModerationReview, type OrganizerReview } from '../api/campus';
import AuthenticatedImage from '../components/AuthenticatedImage';
import { createCertificateRequest, downloadCertificateDocument, listCertificates, listMyCertificates, updateCertificateRequestStatus, uploadCertificateDocument, type CertificateRequestView, type CertificateStatus, type CertificateType } from '../api/certificates';
import { useAuth } from '../auth/AuthProvider';
import { getAllAcademicDisciplines, listManagedTeachers, type ManagedDiscipline, type ManagedTeacher } from '../api/administration';

const certificateTypeLabels: Record<CertificateType, string> = {
  EDUCATION: 'Справка об обучении',
  INCOME: 'Справка о доходах',
  MILITARY: 'Справка для военкомата',
  OTHER: 'Свой вариант',
};

const certificateStatusLabels: Record<CertificateStatus, string> = {
  PENDING: 'Новая',
  IN_PROGRESS: 'В работе',
  READY: 'Ожидает выдачи',
  APPROVED: 'Ожидает выдачи',
  ISSUED: 'Выдана',
  REJECTED: 'Отклонена',
};

function certificateTitle(request: CertificateRequestView): string {
  return request.type === 'OTHER' ? request.customTitle ?? 'Свой вариант' : certificateTypeLabels[request.type];
}

function PageHeader({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <Box className="campus-page-heading">
      <Typography variant="h4" className="welcome-title">{title}</Typography>
      {children}
    </Box>
  );
}

function SectionCard({ title, icon, children }: { title: string; icon: ReactNode; children: ReactNode }) {
  return (
    <Card className="campus-section-card">
      <CardContent>
        <Stack direction="row" spacing={1.2} alignItems="center" mb={2}>
          <Avatar className="campus-section-icon">{icon}</Avatar>
          <Typography variant="h6" fontSize={16} fontWeight={700}>{title}</Typography>
        </Stack>
        {children}
      </CardContent>
    </Card>
  );
}

function formatDate(value: string, withTime = true): string {
  return new Intl.DateTimeFormat('ru-RU', {
    day: 'numeric',
    month: 'long',
    ...(withTime ? { hour: '2-digit', minute: '2-digit' } : {}),
  }).format(new Date(value));
}

function formatNounCount(count: number, one: string, few: string, many: string): string {
  const mod100 = count % 100;
  const mod10 = count % 10;
  const noun = mod100 >= 11 && mod100 <= 14
    ? many
    : mod10 === 1
      ? one
      : mod10 >= 2 && mod10 <= 4
        ? few
        : many;
  return `${count} ${noun}`;
}

function canStudentReview(event: CampusEvent, studentId: string, studentName: string): boolean {
  if ((!studentId && !studentName) || Date.parse(event.endsAt) > Date.now() || event.reviews.some((review) => review.studentId === studentId || review.studentName === studentName)) return false;
  const participant = event.participants.find((item) => item.studentId === studentId || item.studentName === studentName);
  if (!participant) return false;
  if (event.format === 'individual') return true;
  const registration = event.groupRegistrations.find((item) => item.id === participant.groupRegistrationId);
  if (!registration || event.groupMinPercent === null) return false;
  const confirmed = event.participants.filter((item) => item.groupRegistrationId === registration.id && item.participationConfirmed).length;
  return confirmed >= Math.ceil(registration.groupSize * event.groupMinPercent / 100);
}

function EventCard({ eventId, reviewExpanded = false, showReview = true }: {
  eventId: string;
  reviewExpanded?: boolean;
  showReview?: boolean;
}) {
  const { events, joinEvent, confirmGroupParticipation, submitEventReview } = useCampusWorkflow();
  const { currentUser } = useAuth();
  const studentId = currentUser?.id ?? '';
  const studentName = currentUser?.fullName ?? '';
  const studentGroup = currentUser?.groupName ?? '';
  const studentGroupId = currentUser?.groupId ?? null;
  const event = events.find((item) => item.id === eventId);
  const selectedEventId = event?.id;
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [reviewComment, setReviewComment] = useState('');
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewSuccess, setReviewSuccess] = useState(false);
  const [isReviewOpen, setIsReviewOpen] = useState(false);
  const [organizerReviewsOpen, setOrganizerReviewsOpen] = useState(false);
  const [organizerReviewsLoading, setOrganizerReviewsLoading] = useState(false);
  const [organizerReviewsError, setOrganizerReviewsError] = useState('');
  const [organizerReviews, setOrganizerReviews] = useState<OrganizerReview[]>([]);
  const actionLock = useRef(false);
  if (!event) return null;
  const organizerId = event.creatorId;

  async function handleJoin(groupRegistrationId?: string) {
    if (actionLock.current || isLoading) return;
    actionLock.current = true;
    setIsLoading(true);
    setError('');
    try {
      if (!selectedEventId) throw new Error('Мероприятие не найдено.');
      if (groupRegistrationId) await confirmGroupParticipation(selectedEventId, groupRegistrationId);
      else await joinEvent(selectedEventId);
    } catch (joinError) {
      setError(joinError instanceof Error ? joinError.message : 'Не удалось записаться.');
    } finally {
      actionLock.current = false;
      setIsLoading(false);
    }
  }

  const currentStudentParticipant = event.participants.find((participant) => participant.studentId === studentId || participant.studentName === studentName);
  const currentStudentEnrolled = Boolean(currentStudentParticipant && (
    event.format === 'individual' || currentStudentParticipant.participationConfirmed
  ));
  const currentStudentRegistration = event.groupRegistrations.find((registration) => registration.id === currentStudentParticipant?.groupRegistrationId);
  const currentGroupParticipationCount = currentStudentRegistration
    ? event.participants.filter((participant) => participant.groupRegistrationId === currentStudentRegistration.id && participant.participationConfirmed).length
    : 0;
  const currentGroupParticipationRequired = currentStudentRegistration && event.groupMinPercent !== null
    ? Math.ceil(currentStudentRegistration.groupSize * event.groupMinPercent / 100)
    : 0;
  const currentStudentAttended = Boolean(
    currentStudentParticipant?.participationConfirmed &&
    (event.format === 'individual' || (currentStudentRegistration && currentGroupParticipationCount >= currentGroupParticipationRequired)),
  );
  const currentStudentReview = event.reviews.find((review) => review.studentId === studentId || review.studentName === studentName);
  const eventRating = event.rating;

  async function handleOpenOrganizerReviews() {
    if (!organizerId) {
      setOrganizerReviewsError('Не удалось определить организатора мероприятия.');
      setOrganizerReviewsOpen(true);
      return;
    }
    setOrganizerReviewsOpen(true);
    setOrganizerReviewsLoading(true);
    setOrganizerReviewsError('');
    try {
      setOrganizerReviews(await listOrganizerReviews(organizerId));
    } catch (requestError) {
      setOrganizerReviewsError(requestError instanceof Error ? requestError.message : 'Не удалось загрузить отзывы организатора.');
    } finally {
      setOrganizerReviewsLoading(false);
    }
  }

  async function handleSubmitReview(formEvent: FormEvent<HTMLFormElement>) {
    formEvent.preventDefault();
    if (actionLock.current || isLoading || !selectedEventId) return;
    actionLock.current = true;
    setIsLoading(true);
    setError('');
    setReviewSuccess(false);
    try {
      await submitEventReview(selectedEventId, reviewRating, reviewComment);
      setReviewComment('');
      setReviewSuccess(true);
    } catch (reviewError) {
      setError(reviewError instanceof Error ? reviewError.message : 'Не удалось отправить отзыв.');
    } finally {
      actionLock.current = false;
      setIsLoading(false);
    }
  }

  return (
    <Card
      className={`campus-event-card${event.format === 'group' ? ' campus-event-group' : ' campus-event-individual'}${Date.parse(event.endsAt) <= Date.now() ? ' campus-event-finished' : ''}${currentStudentEnrolled ? ' campus-event-enrolled' : ''}`}
      data-category={event.category}
    >
      <CardContent>
        <Box className="campus-event-heading">
          <Box>
            <Stack direction="row" gap={0.75} flexWrap="wrap">
              <Chip size="small" label={event.category} className="subtle-chip" />
              <Chip size="small" label={event.format === 'group' ? 'Для группы' : 'Индивидуально'} className={`event-format-chip event-format-${event.format}`} />
            </Stack>
            <Typography variant="h6" fontSize={19} fontWeight={750} mt={1.25}>{event.title}</Typography>
          </Box>
          {currentStudentEnrolled && <Chip size="small" color="success" label="Вы записаны" />}
        </Box>
        <Typography className="campus-event-description" color="text.secondary" variant="body2" lineHeight={1.65}>{event.description}</Typography>
        <Box className="campus-event-details">
          <Box className="campus-event-date">
            <Typography variant="overline">{formatDate(event.date).split(' в ')[0]}</Typography>
            <Typography variant="body2" fontWeight={700}>{formatDate(event.date).split(' в ')[1] ?? ''}</Typography>
          </Box>
          <Box className="campus-event-facts">
            <Typography variant="body2"><strong>Место</strong> · {event.location}</Typography>
            <Typography variant="body2"><strong>Награда</strong> · {event.activityType}, до {event.maxPoints} баллов</Typography>
            <Typography variant="body2">
              <strong>Мест</strong> · {event.capacity} участников · организатор:{' '}
              <Button
                size="small"
                variant="text"
                onClick={() => void handleOpenOrganizerReviews()}
                sx={{ minWidth: 0, p: 0, verticalAlign: 'baseline', textTransform: 'none', fontSize: 'inherit', fontWeight: 700 }}
              >
                {event.createdBy}
              </Button>
            </Typography>
            <Typography variant="body2" color={eventRating === null ? 'text.secondary' : 'warning.dark'}>
              <strong>Рейтинг</strong> · {eventRating === null
                ? 'появится после накопления достаточного числа одобренных отзывов'
                : `★ ${eventRating.toFixed(1)} из 5`}
            </Typography>
            {event.format === 'group' && <Typography variant="body2"><strong>Порог группы</strong> · {event.groupMinPercent}%</Typography>}
          </Box>
        </Box>
        {error && <Alert severity="error" sx={{ mt: 1.5 }}>{error}</Alert>}
        {event.format === 'individual' && !currentStudentEnrolled && Date.parse(event.date) > Date.now() && (
        <Button className="campus-event-action" variant="contained" fullWidth disabled={isLoading} onClick={() => void handleJoin()}>
          {isLoading ? <CircularProgress size={18} color="inherit" /> : 'Записаться'}
        </Button>
        )}
        {event.format === 'group' && Date.parse(event.date) > Date.now() && (
        <Box className="event-group-list">
          {event.groupRegistrations.map((registration) => {
            const joined = event.participants.filter((participant) => participant.groupRegistrationId === registration.id && participant.participationConfirmed).length;
            const needed = Math.ceil(registration.groupSize * (event.groupMinPercent ?? 0) / 100);
            const thresholdReached = joined >= needed;
            const memberEntry = event.participants.find((participant) => participant.groupRegistrationId === registration.id && participant.studentId === studentId);
            const memberJoined = memberEntry?.participationConfirmed === true;
            return (
              <Box className="event-group-registration" key={registration.id}>
                <Typography variant="body2" fontWeight={650}>{registration.group} · {joined} из {registration.groupSize} подтвердили участие</Typography>
                <Typography variant="caption" color="text.secondary">Порог: {formatNounCount(needed, 'участник', 'участника', 'участников')} · {thresholdReached ? 'группа допущена' : 'пока не набран'}</Typography>
                {memberEntry && !memberJoined && registration.group === studentGroup && (
                  <Box className="event-confirm-action">
                    <Typography variant="caption" color="text.secondary">
                      Вы включены в заявку группы. Подтвердите личное участие.
                    </Typography>
                    <Button variant="contained" disabled={isLoading} onClick={() => void handleJoin(registration.id)}>
                      {isLoading ? <CircularProgress size={18} color="inherit" /> : 'Я участвую'}
                    </Button>
                  </Box>
                )}
                {memberJoined && <Chip size="small" color="success" label="Участие подтверждено" />}
              </Box>
            );
          })}
          {!event.groupRegistrations.some((registration) => registration.group === studentGroup) && studentGroupId && (
            <Box className="event-group-registration event-group-self-signup">
              <Typography variant="body2" fontWeight={650}>{studentGroup || 'Ваша группа'} ещё не записана</Typography>
              <Typography variant="caption" color="text.secondary">
                {currentUser?.groupLeader
                  ? 'Староста может записать группу; каждый студент отдельно подтвердит участие.'
                  : 'Записать группу на мероприятие может её староста или преподаватель.'}
              </Typography>
              {currentUser?.groupLeader && (
                <Button variant="contained" disabled={isLoading} onClick={() => void handleJoin()}>
                  {isLoading ? <CircularProgress size={18} color="inherit" /> : 'Записаться от группы'}
                </Button>
              )}
            </Box>
          )}
          {!studentGroupId && (
            <Alert severity="info">Чтобы записаться на коллективное мероприятие, сначала подтвердите свою учебную группу.</Alert>
          )}
        </Box>
        )}
        {currentStudentEnrolled && Date.parse(event.endsAt) <= Date.now() && event.format === 'group' && !currentStudentAttended && (
          <Typography variant="caption" display="block" mt={1.5} color="error.main">Группа не набрала минимальное число участников · начисление баллов отменено</Typography>
        )}
        {currentStudentEnrolled && Date.parse(event.endsAt) > Date.now() && (
          <Typography variant="caption" display="block" mt={1.5} color="success.main">Вы записаны · создатель начислит баллы после завершения мероприятия</Typography>
        )}
        {!currentStudentEnrolled && Date.parse(event.endsAt) <= Date.now() && <Chip size="small" sx={{ mt: 1.5 }} label="Мероприятие завершено" />}
        {showReview && currentStudentAttended && Date.parse(event.endsAt) <= Date.now() && (
          <Box className="event-review event-review-finished">
            <Box>
              <Chip size="small" label="Мероприятие завершено" color="success" />
              <Typography fontWeight={750} mt={1}>Теперь можно оставить отзыв</Typography>
              <Typography variant="caption" color="text.secondary">Отзывы доступны только после окончания мероприятия</Typography>
            </Box>
            {currentStudentReview ? (
              <Stack spacing={0.5} mt={1}>
                <Chip size="small" label={currentStudentReview.status === 'pending' ? 'На модерации' : currentStudentReview.status === 'approved' ? 'Одобрен' : 'Отклонён'} className="subtle-chip" />
                <Typography variant="body2">{currentStudentReview.comment}</Typography>
              </Stack>
            ) : (reviewExpanded || isReviewOpen) ? (
              <Box component="form" onSubmit={handleSubmitReview} className="event-review-form">
                <Stack spacing={0.5}>
                  <Typography variant="body2" color="text.secondary">Оценка</Typography>
                  <Rating
                    size="large"
                    value={reviewRating}
                    onChange={(_, value) => setReviewRating(value ?? 5)}
                    aria-label="Оценка мероприятия"
                  />
                </Stack>
                <TextField label="Комментарий" value={reviewComment} onChange={(inputEvent) => setReviewComment(inputEvent.target.value)} inputProps={{ maxLength: 1000 }} helperText="От 20 символов · отзыв проверит учебная часть" multiline minRows={2} required fullWidth />
                <Button type="submit" variant="outlined" disabled={isLoading || reviewComment.trim().length < 20}>
                  {isLoading ? <CircularProgress size={18} /> : 'Отправить отзыв'}
                </Button>
              </Box>
            ) : (
              <Button variant="contained" className="event-review-open" onClick={() => setIsReviewOpen(true)}>
                Оставить отзыв
              </Button>
            )}
            {reviewSuccess && <Alert severity="success" sx={{ mt: 1 }}>Отзыв отправлен на модерацию.</Alert>}
          </Box>
        )}
      </CardContent>
      <Dialog open={organizerReviewsOpen} onClose={() => setOrganizerReviewsOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>Отзывы об организаторе · {event.createdBy}</DialogTitle>
        <DialogContent dividers>
          {organizerReviewsLoading && <Box display="grid" sx={{ placeItems: 'center', py: 3 }}><CircularProgress aria-label="Загрузка отзывов организатора" /></Box>}
          {organizerReviewsError && <Alert severity="error">{organizerReviewsError}</Alert>}
          {!organizerReviewsLoading && !organizerReviewsError && organizerReviews.length === 0 && (
            <Typography color="text.secondary">Одобренных отзывов об организаторе пока нет.</Typography>
          )}
          {!organizerReviewsLoading && organizerReviews.length > 0 && (
            <Stack divider={<Divider flexItem />} spacing={1.5}>
              {organizerReviews.map((review) => (
                <Box key={review.id} py={0.5}>
                  <Stack direction="row" justifyContent="space-between" gap={1} alignItems="flex-start">
                    <Box>
                      <Typography fontWeight={700}>{review.studentName}</Typography>
                      <Typography variant="caption" color="text.secondary">{review.eventTitle} · {formatDate(review.createdAt)}</Typography>
                    </Box>
                    <Chip size="small" color="warning" label={`★ ${review.rating}/5`} />
                  </Stack>
                  <Typography variant="body2" mt={1} sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{review.comment}</Typography>
                </Box>
              ))}
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOrganizerReviewsOpen(false)}>Закрыть</Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
}

export function EventsPage() {
  const { events, error, isLoading } = useCampusWorkflow();
  const { currentUser } = useAuth();
  const studentId = currentUser?.id ?? '';
  const studentName = currentUser?.fullName ?? '';
  const [selectedTab, setSelectedTab] = useState(0);
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30_000);
    return () => window.clearInterval(timer);
  }, []);
  const activeEvents = events.filter((event) => Date.parse(event.endsAt) > now);
  const reviewEvents = events.filter((event) => Date.parse(event.endsAt) <= now && (
    canStudentReview(event, studentId, studentName)
    || event.reviews.some((review) => review.studentId === studentId)
  ));
  return (
    <Box className="campus-page">
      <PageHeader title="Мероприятия">
        <Typography color="text.secondary">{formatNounCount(activeEvents.filter((event) => event.participants.some((participant) => participant.studentId === studentId || participant.studentName === studentName)).length, 'мероприятие', 'мероприятия', 'мероприятий')} в вашем списке</Typography>
      </PageHeader>
      {error && <Alert severity="error">{error}</Alert>}
      {!isLoading && !error && activeEvents.length === 0 && <Alert severity="info">Активных мероприятий пока нет.</Alert>}
      <Tabs value={selectedTab} onChange={(_, value: number) => setSelectedTab(value)} aria-label="Разделы мероприятий">
        <Tab label="Мероприятия" />
        <Tab label="Отзывы" />
      </Tabs>
      {selectedTab === 1 && reviewEvents.length === 0 && <Alert severity="info">Отзывов пока нет. После завершения посещённого мероприятия он появится здесь.</Alert>}
      <Box className="campus-event-list">
        {(selectedTab === 0 ? activeEvents : reviewEvents).map((event) => (
          <EventCard
            key={event.id}
            eventId={event.id}
            reviewExpanded={selectedTab === 1 && canStudentReview(event, studentId, studentName)}
            showReview={selectedTab === 1}
          />
        ))}
      </Box>
    </Box>
  );
}

export function GroupConfirmationPage() {
  const {
    groupConfirmations,
    decideGroupConfirmation,
    isGroupLeader,
    error: workflowError,
  } = useCampusWorkflow();
  const { currentUser, profileError, profileLoading, session } = useAuth();
  const ownGroup = currentUser?.groupName ?? '';
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const actionLock = useRef(false);
  const ownGroupConfirmations = groupConfirmations.filter((item) => item.group === ownGroup);
  const pending = ownGroupConfirmations.filter((item) => item.status === 'pending');
  const processed = ownGroupConfirmations.filter((item) => item.status !== 'pending');

  if (!isGroupLeader) {
    if (session && !currentUser && (profileLoading || !profileError)) {
      return <Box className="campus-page" display="flex" justifyContent="center" p={4}><CircularProgress /></Box>;
    }
    return <Navigate to="/student" replace />;
  }

  async function decide(id: string, approve: boolean) {
    if (actionLock.current || busyId) return;
    actionLock.current = true;
    setBusyId(id);
    setError('');
    try {
      await decideGroupConfirmation(id, approve, { role: 'group-leader', group: ownGroup });
    } catch (decisionError) {
      setError(decisionError instanceof Error ? decisionError.message : 'Не удалось обработать заявку.');
    } finally {
      actionLock.current = false;
      setBusyId(null);
    }
  }

  return (
    <Box className="campus-page">
      <PageHeader title="Заявки студентов группы">
        <Typography color="text.secondary">
          Проверяйте и подтверждайте заявки студентов своей группы
        </Typography>
      </PageHeader>
      {workflowError && <Alert severity="error">{workflowError}</Alert>}
      {error && <Alert severity="error">{error}</Alert>}
      <Card className="group-leader-summary">
        <CardContent>
          <Box><Typography variant="overline">ВАША ГРУППА</Typography><Typography variant="h5" fontWeight={750}>{ownGroup || '—'}</Typography></Box>
          <Box><Typography variant="overline">ОЖИДАЮТ</Typography><Typography variant="h5" fontWeight={750}>{pending.length}</Typography></Box>
          <Box><Typography variant="overline">ОБРАБОТАНО</Typography><Typography variant="h5" fontWeight={750}>{processed.length}</Typography></Box>
        </CardContent>
      </Card>
      <SectionCard title="Требуют решения" icon={<GroupsRoundedIcon />}>
        {pending.length === 0 ? <Typography color="text.secondary">Новых заявок нет. Здесь появятся заявки студентов вашей группы.</Typography> : (
          <Stack spacing={1.5}>
            {pending.map((request) => (
              <Card className="group-leader-request" key={request.id} variant="outlined">
                <CardContent>
                  <Box className="group-leader-request-info">
                    <Avatar className="group-leader-student-avatar">{request.studentName.split(' ').map((part) => part[0]).slice(0, 2).join('')}</Avatar>
                    <Box>
                      <Typography fontWeight={750}>{request.studentName}</Typography>
                      <Typography variant="body2" color="text.secondary">{request.email}</Typography>
                      <Typography variant="caption" color="text.secondary">Подана {formatDate(request.submittedAt)}</Typography>
                    </Box>
                  </Box>
                  <Stack direction={{ xs: 'column-reverse', sm: 'row' }} spacing={1} className="group-leader-actions">
                    <Button variant="outlined" color="inherit" disabled={busyId !== null} onClick={() => void decide(request.id, false)}>Отклонить</Button>
                    <Button variant="contained" disabled={busyId !== null} startIcon={busyId === request.id ? <CircularProgress size={16} color="inherit" /> : <CheckRoundedIcon />} onClick={() => void decide(request.id, true)}>
                      Подтвердить студента
                    </Button>
                  </Stack>
                </CardContent>
              </Card>
            ))}
          </Stack>
        )}
      </SectionCard>
      {processed.length > 0 && (
        <SectionCard title="История решений" icon={<CheckRoundedIcon />}>
          <Stack spacing={1}>
            {processed.map((request) => (
              <Box className="group-leader-history-row" key={request.id}>
                <Box><Typography fontWeight={650}>{request.studentName}</Typography><Typography variant="caption" color="text.secondary">{request.email} · {formatDate(request.submittedAt)}</Typography></Box>
                <Chip size="small" label={request.status === 'approved' ? 'Подтверждён' : 'Отклонён'} color={request.status === 'approved' ? 'success' : 'default'} />
              </Box>
            ))}
          </Stack>
        </SectionCard>
      )}
    </Box>
  );
}

export function TeacherStudentsPage() {
  const { students, teacherGroups, groupLeaders, groupConfirmations, decideGroupConfirmation, assignGroupLeader, error: workflowError } = useCampusWorkflow();
  const [selectedGroup, setSelectedGroup] = useState<string>(teacherGroups[0] ?? '');
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const actionLock = useRef(false);
  const groupStudents = students.filter((student) => student.group === selectedGroup);
  const pendingRequests = groupConfirmations.filter((request) => request.group === selectedGroup && request.status === 'pending');
  const processedRequests = groupConfirmations.filter((request) => request.group === selectedGroup && request.status !== 'pending');
  const appointedLeaderId = groupLeaders[selectedGroup];

  async function handleApplication(id: string, approve: boolean) {
    if (actionLock.current) return;
    actionLock.current = true;
    setBusyAction(id);
    setError('');
    setSuccess('');
    try {
      await decideGroupConfirmation(id, approve, { role: 'teacher', group: selectedGroup });
      setSuccess(approve ? 'Студент принят в группу.' : 'Заявка отклонена.');
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Не удалось обработать заявку.');
    } finally {
      actionLock.current = false;
      setBusyAction(null);
    }
  }

  async function handleAppointLeader(studentId: string, studentName: string) {
    if (actionLock.current) return;
    actionLock.current = true;
    setBusyAction(`leader:${studentId}`);
    setError('');
    setSuccess('');
    try {
      await assignGroupLeader(selectedGroup, studentId);
      setSuccess(`${studentName} назначен старостой группы ${selectedGroup}.`);
    } catch (appointmentError) {
      setError(appointmentError instanceof Error ? appointmentError.message : 'Не удалось назначить старосту.');
    } finally {
      actionLock.current = false;
      setBusyAction(null);
    }
  }

  return (
    <Box className="campus-page">
      <PageHeader title="Мои студенты">
        <Typography color="text.secondary">Заявки на вступление, состав групп и назначение старост</Typography>
      </PageHeader>
      {teacherGroups.length === 0 && <Alert severity="info">За вами пока не закреплены учебные группы.</Alert>}
      {workflowError && <Alert severity="error">{workflowError}</Alert>}
      <Box className="teacher-student-toolbar">
        <TextField select label="Учебная группа" value={selectedGroup} onChange={(event) => setSelectedGroup(event.target.value)} size="small" disabled={!teacherGroups.length}>
          {teacherGroups.map((group) => <MenuItem key={group} value={group}>{group}</MenuItem>)}
        </TextField>
      </Box>
      {error && <Alert severity="error">{error}</Alert>}
      {success && <Alert severity="success">{success}</Alert>}
      <Card className="group-leader-summary teacher-student-summary">
        <CardContent>
          <Box><Typography variant="overline">УЧЕБНАЯ ГРУППА</Typography><Typography variant="h5" fontWeight={750}>{selectedGroup}</Typography></Box>
          <Box><Typography variant="overline">СТУДЕНТОВ</Typography><Typography variant="h5" fontWeight={750}>{groupStudents.length}</Typography></Box>
          <Box><Typography variant="overline">НОВЫХ ЗАЯВОК</Typography><Typography variant="h5" fontWeight={750}>{pendingRequests.length}</Typography></Box>
        </CardContent>
      </Card>
      <SectionCard title="Заявки на вступление" icon={<PersonAddAltRoundedIcon />}>
        {pendingRequests.length === 0 ? <Typography color="text.secondary">Нет заявок, ожидающих решения.</Typography> : (
          <Stack spacing={1.5}>
            {pendingRequests.map((request) => (
              <Card className="group-leader-request" key={request.id} variant="outlined">
                <CardContent>
                  <Box className="group-leader-request-info">
                    <Avatar className="group-leader-student-avatar">{request.studentName.split(' ').map((part) => part[0]).slice(0, 2).join('')}</Avatar>
                    <Box>
                      <Typography fontWeight={750}>{request.studentName}</Typography>
                      <Typography variant="body2" color="text.secondary">{request.email}</Typography>
                      <Typography variant="caption" color="text.secondary">Подана {formatDate(request.submittedAt)}</Typography>
                    </Box>
                  </Box>
                  <Stack direction={{ xs: 'column-reverse', sm: 'row' }} spacing={1} className="group-leader-actions">
                    <Button variant="outlined" color="inherit" disabled={busyAction !== null} onClick={() => void handleApplication(request.id, false)}>
                      {busyAction === request.id ? <CircularProgress size={16} /> : 'Отклонить'}
                    </Button>
                    <Button variant="contained" disabled={busyAction !== null} startIcon={busyAction === request.id ? <CircularProgress size={16} color="inherit" /> : <CheckRoundedIcon />} onClick={() => void handleApplication(request.id, true)}>
                      Принять студента
                    </Button>
                  </Stack>
                </CardContent>
              </Card>
            ))}
          </Stack>
        )}
        {processedRequests.length > 0 && (
          <Box className="teacher-student-application-history">
            <Typography variant="subtitle2" fontWeight={700} mb={1}>Рассмотренные заявки</Typography>
            {processedRequests.map((request) => (
              <Box className="group-leader-history-row" key={request.id}>
                <Box><Typography fontWeight={650}>{request.studentName}</Typography><Typography variant="caption" color="text.secondary">{request.email}</Typography></Box>
                <Chip size="small" label={request.status === 'approved' ? 'Принят' : 'Отклонён'} color={request.status === 'approved' ? 'success' : 'default'} />
              </Box>
            ))}
          </Box>
        )}
      </SectionCard>
      <SectionCard title={`Состав группы · ${groupStudents.length}`} icon={<GroupsRoundedIcon />}>
        {groupStudents.length === 0 ? <Typography color="text.secondary">В этой группе пока нет студентов.</Typography> : (
          <Stack spacing={1}>
            {groupStudents.map((student) => {
              const isLeader = student.id === appointedLeaderId;
              return (
                <Box className="teacher-student-row" key={student.id}>
                  <Box className="group-leader-request-info">
                    <Avatar className="group-leader-student-avatar">{student.studentName.split(' ').map((part) => part[0]).slice(0, 2).join('')}</Avatar>
                    <Box>
                      <Typography fontWeight={700}>{student.studentName}</Typography>
                      <Typography variant="body2" color="text.secondary">{student.email}</Typography>
                    </Box>
                  </Box>
                  {isLeader ? <Chip size="small" color="primary" label="Староста" /> : (
                    <Button size="small" variant="outlined" disabled={busyAction !== null} onClick={() => void handleAppointLeader(student.id, student.studentName)}>
                      {busyAction === `leader:${student.id}` ? <CircularProgress size={16} /> : 'Назначить старостой'}
                    </Button>
                  )}
                </Box>
              );
            })}
          </Stack>
        )}
      </SectionCard>
    </Box>
  );
}

export function EventManagementPage() {
  const { events, createEvent, addEventGroup, awardEventPoints, teacherGroups, campusGroups, activityAwards, error: workflowError } = useCampusWorkflow();
  const { currentUser } = useAuth();
  const location = useLocation();
  const isTeacherPage = location.pathname.startsWith('/teacher/');
  const creator = currentUser?.fullName ?? '';
  const ownedEvents = events.filter((event) => event.creatorId === currentUser?.id);
  const groupEvents = events.filter((event) => event.format === 'group' && Date.parse(event.date) > Date.now());
  const creatorAwards = activityAwards.filter((award) => award.awardedById === currentUser?.id);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('Обучение');
  const [locationText, setLocationText] = useState('');
  const [startsAt, setStartsAt] = useState('');
  const [endsAt, setEndsAt] = useState('');
  const [capacity, setCapacity] = useState('30');
  const [format, setFormat] = useState<CampusEvent['format']>('individual');
  const [groupMinPercent, setGroupMinPercent] = useState('50');
  const [activityType, setActivityType] = useState<CampusEvent['activityType']>('БОА');
  const [maxPoints, setMaxPoints] = useState('50');
  const [selectedGroups, setSelectedGroups] = useState<Record<string, string>>({});
  const [activityPercentages, setActivityPercentages] = useState<Record<string, string>>({});
  const [isCreating, setIsCreating] = useState(false);
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const actionLock = useRef(false);

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (actionLock.current) return;
    actionLock.current = true;
    setIsCreating(true);
    setError('');
    setSuccess('');
    try {
      await createEvent({
        title,
        description,
        category,
        location: locationText,
        date: startsAt,
        endsAt,
        capacity: Number(capacity),
        format,
        groupMinPercent: format === 'group' ? Number(groupMinPercent) : null,
        activityType,
        maxPoints: Number(maxPoints),
      });
      setTitle('');
      setDescription('');
      setLocationText('');
      setSuccess('Мероприятие создано.');
    } catch (createError) {
      setError(createError instanceof Error ? createError.message : 'Не удалось создать мероприятие.');
    } finally {
      actionLock.current = false;
      setIsCreating(false);
    }
  }

  async function handleAddEventGroup(event: CampusEvent) {
    if (actionLock.current) return;
    actionLock.current = true;
    const key = `group:${event.id}`;
    setBusyAction(key);
    setError('');
    setSuccess('');
    try {
      const group = campusGroups.find((item) => item.name === selectedGroups[event.id]);
      if (!group) throw new Error('Выберите группу для записи.');
      await addEventGroup(event.id, group.name, group.size, creator);
      setSelectedGroups((current) => ({ ...current, [event.id]: '' }));
      setSuccess(`${group.name} записана на мероприятие.`);
    } catch (confirmError) {
      setError(confirmError instanceof Error ? confirmError.message : 'Не удалось записать группу.');
    } finally {
      actionLock.current = false;
      setBusyAction(null);
    }
  }

  async function handleAward(event: CampusEvent, participantId: string) {
    if (actionLock.current) return;
    const enteredPercent = activityPercentages[`${event.id}:${participantId}`];
    if (enteredPercent === undefined || enteredPercent.trim() === '') {
      setError('Укажите активность участника в процентах от 0 до 100.');
      return;
    }
    actionLock.current = true;
    const key = `${event.id}:${participantId}`;
    setBusyAction(key);
    setError('');
    try {
      const percent = Number(enteredPercent);
      const points = Math.round(event.maxPoints * percent / 100);
      await awardEventPoints(event.id, participantId, percent);
      setSuccess(`Начислено ${points} ${event.activityType} (${percent}% активности).`);
    } catch (awardError) {
      setError(awardError instanceof Error ? awardError.message : 'Не удалось начислить баллы.');
    } finally {
      actionLock.current = false;
      setBusyAction(null);
    }
  }

  return (
    <Box className="campus-page">
      <PageHeader title="Мероприятия и баллы">
        <Typography color="text.secondary">БОА/БУА начисляются создателем после завершения мероприятия участникам, подтвердившим участие</Typography>
      </PageHeader>
      {error && <Alert severity="error">{error}</Alert>}
      {workflowError && <Alert severity="error">{workflowError}</Alert>}
      {success && <Alert severity="success">{success}</Alert>}
      <SectionCard title="Создать мероприятие" icon={<EventAvailableRoundedIcon />}>
        <Box component="form" onSubmit={handleCreate} className="campus-form">
          <TextField label="Название" value={title} onChange={(event) => setTitle(event.target.value)} required fullWidth />
          <TextField label="Описание" value={description} onChange={(event) => setDescription(event.target.value)} required multiline minRows={2} fullWidth />
          <Box className="event-form-grid">
            <TextField select label="Категория" value={category} onChange={(event) => setCategory(event.target.value)} fullWidth>
              {['Обучение', 'Культура', 'Волонтёрство', 'Спорт', 'Другое'].map((value) => <MenuItem key={value} value={value}>{value}</MenuItem>)}
            </TextField>
            <TextField label="Место проведения" value={locationText} onChange={(event) => setLocationText(event.target.value)} required fullWidth />
            <TextField label="Начало" type="datetime-local" value={startsAt} onChange={(event) => setStartsAt(event.target.value)} required fullWidth InputLabelProps={{ shrink: true }} />
            <TextField label="Окончание" type="datetime-local" value={endsAt} onChange={(event) => setEndsAt(event.target.value)} required fullWidth InputLabelProps={{ shrink: true }} />
            <TextField label="Вместимость" type="number" value={capacity} onChange={(event) => setCapacity(event.target.value)} inputProps={{ min: 1, step: 1 }} required fullWidth />
            <TextField select label="Формат участия" value={format} onChange={(event) => setFormat(event.target.value as CampusEvent['format'])} fullWidth>
              <MenuItem value="individual">Индивидуальный</MenuItem>
              <MenuItem value="group">Коллективный</MenuItem>
            </TextField>
            {format === 'group' && <TextField label="Минимальный порог участия группы, %" type="number" value={groupMinPercent} onChange={(event) => setGroupMinPercent(event.target.value)} inputProps={{ min: 1, max: 100, step: 1 }} required fullWidth />}
            <TextField select label="Тип активности" value={activityType} onChange={(event) => setActivityType(event.target.value as CampusEvent['activityType'])} fullWidth>
              <MenuItem value="БОА">БОА · общественная активность</MenuItem>
              <MenuItem value="БУА">БУА · учебная активность</MenuItem>
            </TextField>
            <TextField label="Баллы при 100% активности" type="number" value={maxPoints} onChange={(event) => setMaxPoints(event.target.value)} inputProps={{ min: 0, step: 1 }} helperText="Начисление участнику рассчитывается по его проценту активности" required fullWidth />
          </Box>
          <Button type="submit" variant="contained" disabled={isCreating} startIcon={isCreating ? <CircularProgress size={18} color="inherit" /> : <EventAvailableRoundedIcon />}>
            {isCreating ? 'Создаём…' : 'Создать мероприятие'}
          </Button>
        </Box>
      </SectionCard>
      {isTeacherPage && groupEvents.length > 0 && (
        <SectionCard title="Записать свою группу" icon={<GroupsRoundedIcon />}>
          <Stack spacing={1.5}>
            {groupEvents.map((event) => (
              <Box className="managed-event" key={event.id}>
                <Typography fontWeight={700}>{event.title}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {formatDate(event.date)} · порог {event.groupMinPercent}% · группы: {event.groupRegistrations.map((registration) => registration.group).join(', ') || 'пока не записаны'}
                </Typography>
                <Stack direction={{ xs: 'column', sm: 'row' }} gap={1} mt={1.5}>
                  <TextField
                    select
                    size="small"
                    label="Группа"
                    value={selectedGroups[event.id] ?? ''}
                    onChange={(inputEvent) => setSelectedGroups((current) => ({ ...current, [event.id]: inputEvent.target.value }))}
                    sx={{ minWidth: { sm: 220 } }}
                  >
                    {campusGroups.filter((group) => teacherGroups.some((teacherGroup) => teacherGroup === group.name) && !event.groupRegistrations.some((registration) => registration.group === group.name)).map((group) => (
                      <MenuItem key={group.name} value={group.name}>{group.name} · {group.size} студентов</MenuItem>
                    ))}
                  </TextField>
                  <Button variant="outlined" disabled={busyAction !== null || !selectedGroups[event.id]} onClick={() => void handleAddEventGroup(event)}>
                    {busyAction === `group:${event.id}` ? <CircularProgress size={16} /> : 'Записать группу'}
                  </Button>
                </Stack>
              </Box>
            ))}
          </Stack>
        </SectionCard>
      )}
      <SectionCard title="Мои мероприятия" icon={<GroupsRoundedIcon />}>
        {ownedEvents.length === 0 ? <Typography color="text.secondary">Мероприятий пока нет.</Typography> : (
          <Stack spacing={2}>
            {ownedEvents.map((event) => (
              <Box className="managed-event" key={event.id}>
                <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={1}>
                  <Box>
                    <Typography fontWeight={700}>{event.title}</Typography>
                    <Typography variant="caption" color="text.secondary">{formatDate(event.date)} · {event.format === 'group' ? `коллективное, порог ${event.groupMinPercent}%` : 'индивидуальное'} · {event.activityType}, до {event.maxPoints} баллов</Typography>
                  </Box>
                  <Chip size="small" label={Date.parse(event.endsAt) <= Date.now() ? 'Завершено' : 'Регистрация'} className="subtle-chip" />
                </Stack>
                {event.format === 'group' && event.groupRegistrations.length > 0 && (
                  <Stack spacing={0.75} mt={1.5}>
                    {event.groupRegistrations.map((registration) => {
                      const optedIn = event.participants.filter((participant) => participant.groupRegistrationId === registration.id && participant.participationConfirmed).length;
                      const required = Math.ceil(registration.groupSize * (event.groupMinPercent ?? 0) / 100);
                      return <Typography key={registration.id} variant="caption" color="text.secondary">
                        {registration.group}: участие подтвердили {optedIn}/{registration.groupSize} · порог {required} {optedIn >= required ? '· допущена' : Date.parse(event.endsAt) <= Date.now() ? '· не прошла, начисление отменено' : ''}
                      </Typography>;
                    })}
                  </Stack>
                )}
                {Date.parse(event.endsAt) <= Date.now() && (
                  <Stack spacing={1.2} mt={1.5}>
                    {event.participants.length === 0 && <Typography variant="body2" color="text.secondary">На мероприятие не записались участники.</Typography>}
                    {event.participants.map((participant) => {
                      const key = `${event.id}:${participant.id}`;
                      return (
                        <Box className="event-participant" key={participant.id}>
                          <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} justifyContent="space-between" gap={1}>
                            <Box>
                              <Typography fontWeight={650}>{participant.studentName} · {participant.group}</Typography>
                              <Typography variant="caption" color="text.secondary">Записан: {formatDate(participant.enrolledAt)}</Typography>
                            </Box>
                            {participant.participationConfirmed ? (
                              event.format === 'group' && !event.groupRegistrations.some((registration) => registration.id === participant.groupRegistrationId && event.participants.filter((item) => item.groupRegistrationId === registration.id && item.participationConfirmed).length >= Math.ceil(registration.groupSize * (event.groupMinPercent ?? 0) / 100)) ? (
                                <Chip size="small" label="Группа не прошла порог" />
                              ) :
                              participant.awardedPoints === undefined ? (
                                <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} gap={1}>
                                  <Chip size="small" color="success" label="Участие подтверждено" />
                                  <TextField
                                    size="small"
                                    type="number"
                                    label="Активность, %"
                                    value={activityPercentages[key] ?? ''}
                                    onChange={(inputEvent) => setActivityPercentages((current) => ({ ...current, [key]: inputEvent.target.value }))}
                                    inputProps={{ min: 0, max: 100, step: 1 }}
                                    helperText={activityPercentages[key] !== undefined && activityPercentages[key] !== ''
                                      ? `Начисление: ${Math.round(event.maxPoints * Number(activityPercentages[key]) / 100)} ${event.activityType} из ${event.maxPoints}`
                                      : `Введите 0–100% · максимум ${event.maxPoints} ${event.activityType}`}
                                    sx={{ width: { xs: '100%', sm: 190 } }}
                                  />
                                  <Button size="small" variant="contained" disabled={busyAction !== null || activityPercentages[key] === undefined || activityPercentages[key].trim() === '' || !Number.isInteger(Number(activityPercentages[key])) || Number(activityPercentages[key]) < 0 || Number(activityPercentages[key]) > 100} onClick={() => void handleAward(event, participant.id)}>
                                    {busyAction === key ? <CircularProgress size={16} color="inherit" /> : 'Начислить'}
                                  </Button>
                                </Stack>
                              ) : <Chip size="small" color="success" label={`Начислено ${participant.awardedPercent}% · ${participant.awardedPoints} ${event.activityType}`} />
                            ) : <Chip size="small" label="Нет подтверждения участия" />}
                          </Stack>
                        </Box>
                      );
                    })}
                  </Stack>
                )}
              </Box>
            ))}
          </Stack>
        )}
      </SectionCard>
      {creatorAwards.length > 0 && (
        <SectionCard title="Последние начисления" icon={<WorkspacePremiumRoundedIcon />}>
          <Stack divider={<Divider flexItem />} spacing={1.5}>
            {creatorAwards.map((award) => (
              <Stack key={award.id} direction="row" justifyContent="space-between" gap={2}>
                <Box><Typography fontWeight={650}>{award.studentName} · {award.group}</Typography><Typography variant="caption" color="text.secondary">{award.eventTitle} · {formatDate(award.createdAt)}</Typography></Box>
                <Typography fontWeight={700} color="success.main">{award.activityPercent}% · +{award.points} {award.activityType}</Typography>
              </Stack>
            ))}
          </Stack>
        </SectionCard>
      )}
    </Box>
  );
}

export function WalletPage() {
  const { walletBalance, transactions, error } = useCampusWorkflow();
  return (
    <Box className="campus-page">
      <PageHeader title="Кошелёк">
      </PageHeader>
      {error && <Alert severity="error">{error}</Alert>}
      <Card className="wallet-balance-card">
        <CardContent>
          <AccountBalanceWalletRoundedIcon />
          <Typography variant="body2" color="text.secondary" mt={1}>Доступный баланс</Typography>
          <Typography variant="h3" fontWeight={750}>{walletBalance?.toLocaleString('ru-RU') ?? '—'}</Typography>
          <Typography variant="body2" color="text.secondary">жетонов кампуса</Typography>
        </CardContent>
      </Card>
      <SectionCard title="История операций" icon={<AccountBalanceWalletRoundedIcon />}>
        {walletBalance === null && <Alert severity="info" sx={{ mb: 2 }}>Баланс пока недоступен.</Alert>}
        {walletBalance !== null && transactions.length === 0 && <Typography color="text.secondary">Операций пока нет.</Typography>}
        <Stack divider={<Divider flexItem />} spacing={0}>
          {transactions.map((transaction) => (
            <Stack key={transaction.id} direction="row" alignItems="center" spacing={1.5} className="wallet-transaction">
              <Avatar className={transaction.amount < 0 ? 'transaction-icon transaction-out' : 'transaction-icon transaction-in'}>
                {transaction.amount < 0 ? <CloseRoundedIcon fontSize="small" /> : <CheckRoundedIcon fontSize="small" />}
              </Avatar>
              <Box sx={{ flex: 1, minWidth: 0 }}>
                <Typography fontWeight={650}>{transaction.title}</Typography>
                <Typography variant="caption" color="text.secondary">{transaction.detail} · {formatDate(transaction.createdAt)}</Typography>
              </Box>
              <Typography fontWeight={700} color={transaction.amount < 0 ? 'text.primary' : 'primary.main'}>{transaction.amount > 0 ? '+' : ''}{transaction.amount}</Typography>
            </Stack>
          ))}
        </Stack>
      </SectionCard>
    </Box>
  );
}

function InventoryCard({ item, onOpen, isOpening, canAfford, showRewards, onToggleRewards }: {
  item: InventoryItem;
  onOpen: (item: InventoryItem) => void;
  isOpening: boolean;
  canAfford: boolean;
  showRewards: boolean;
  onToggleRewards: () => void;
}) {
  const { campusCases } = useCampusWorkflow();
  const campusCase = campusCases.find((entry) => entry.id === item.caseId);
  const caseRewards = campusCase?.rewards ?? [];
  const rarityClass = (rarity: string) => rarity === 'Эпический' ? 'epic' : rarity === 'Редкий' ? 'rare' : 'common';
  return (
    <Card className="inventory-item inventory-case-card">
      <CardContent>
        <Avatar className={`inventory-item-icon inventory-case-image${item.kind === 'case' ? ' inventory-case-icon' : ''}`}>
          <AuthenticatedImage src={campusCase?.imageUrl} alt={item.name} className="inventory-item-image" fallback={<CardGiftcardRoundedIcon />} />
        </Avatar>
        <Box className="inventory-case-summary">
          <Box className="inventory-case-heading">
            <Box sx={{ minWidth: 0 }}>
              <Typography variant="overline">КЕЙС</Typography>
              <Typography className="inventory-case-title" fontWeight={750}>{item.name}</Typography>
            </Box>
            <Chip size="small" label={formatNounCount(caseRewards.length, 'награда', 'награды', 'наград')} className="subtle-chip" />
          </Box>
          <Typography variant="caption" color="text.secondary" className="inventory-case-description">{campusCase?.description || item.source}</Typography>
          <Stack gap={1} className="inventory-case-actions">
            <Button fullWidth variant="contained" disabled={isOpening || !canAfford} onClick={() => onOpen(item)}>
              {isOpening ? <CircularProgress size={18} color="inherit" /> : campusCase ? `Открыть · ${campusCase.price} жетонов` : 'Открытие недоступно'}
            </Button>
            <Button fullWidth variant="outlined" onClick={onToggleRewards}>
              {showRewards ? 'Скрыть награды' : 'Шансы и награды'}
            </Button>
          </Stack>
          {!canAfford && campusCase && <Typography variant="caption" color="error">Недостаточно жетонов</Typography>}
        </Box>
        {showRewards && (
          <Box className="case-reward-list">
            <Box className="case-reward-heading">
              <Box>
                <Typography variant="subtitle2" fontWeight={750}>Награды кейса</Typography>
                <Typography variant="caption" color="text.secondary">Шанс выпадения каждой награды</Typography>
              </Box>
              <Typography variant="caption" color="text.secondary">Шанс</Typography>
            </Box>
            {caseRewards.length === 0 ? <Typography variant="body2" color="text.secondary">В кейсе пока нет наград.</Typography> : (
              <Box className="case-reward-grid">
                {caseRewards.map((reward, index) => (
                  <Box className={`case-reward-card rarity-border-${rarityClass(reward.rarity)}`} key={`${reward.name}-${index}`}>
                    <Avatar className="case-reward-image">
                      <AuthenticatedImage src={reward.imageUrl} alt={reward.name} className="inventory-item-image" fallback={<WorkspacePremiumRoundedIcon />} />
                    </Avatar>
                    <Box className="case-reward-copy">
                      <Typography variant="body2" fontWeight={700} className="case-reward-name">{reward.name}</Typography>
                      <Typography variant="caption" color="text.secondary">
                        {reward.cosmeticType === 'avatar' ? 'Аватар' : reward.cosmeticType === 'avatar-frame' ? 'Рамка профиля' : reward.cosmeticType === 'profile-background' ? 'Фон профиля' : 'Значок'} · {reward.rarity}
                      </Typography>
                      <Box className={`case-reward-chance-track case-reward-chance-${rarityClass(reward.rarity)}`}>
                        <Box className="case-reward-chance-fill" style={{ width: `${Math.min(100, Math.max(0, reward.chance))}%` }} />
                      </Box>
                    </Box>
                    <Chip size="small" label={`${reward.chance}%`} className={`rarity-chip rarity-${rarityClass(reward.rarity)}`} />
                  </Box>
                ))}
              </Box>
            )}
          </Box>
        )}
      </CardContent>
    </Card>
  );
}

export function BattlePassPage() {
  const { academicRating, passExperience, premiumPassOwned, premiumPassPrice, passSeasonName, passStartsAt, passEndsAt, passRewards, claimPassReward, purchasePremiumPass, walletBalance, error: workflowError } = useCampusWorkflow();
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [error, setError] = useState('');
  const actionLock = useRef(false);
  const battlePassLevelsRef = useRef<HTMLDivElement>(null);
  const currentLevelRef = useRef<HTMLDivElement>(null);
  const currentLevel = passExperience === null ? null : Math.min(50, Math.floor(passExperience / 20));
  const progressInLevel = passExperience === null ? null : currentLevel === 50 ? 20 : passExperience % 20;
  const needed = progressInLevel === null || currentLevel === null || currentLevel >= 50 ? 0 : 20 - progressInLevel;
  const progressPercent = progressInLevel === null ? 0 : currentLevel === 50 ? 100 : (progressInLevel / 20) * 100;

  useLayoutEffect(() => {
    const scrollContainer = battlePassLevelsRef.current;
    const currentLevelElement = currentLevelRef.current;
    if (!scrollContainer || !currentLevelElement) return;
    const levelLeft = currentLevelElement.getBoundingClientRect().left
      - scrollContainer.getBoundingClientRect().left
      + scrollContainer.scrollLeft;
    scrollContainer.scrollTo({
      left: levelLeft - (scrollContainer.clientWidth - currentLevelElement.clientWidth) / 2,
      behavior: 'auto',
    });
  }, [currentLevel]);

  async function handlePurchase() {
    if (actionLock.current) return;
    actionLock.current = true;
    setBusyAction('purchase');
    setError('');
    try {
      await purchasePremiumPass();
    } catch (purchaseError) {
      setError(purchaseError instanceof Error ? purchaseError.message : 'Не удалось купить премиальный пропуск.');
    } finally {
      actionLock.current = false;
      setBusyAction(null);
    }
  }

  async function handleClaim(reward: PassReward) {
    if (actionLock.current) return;
    actionLock.current = true;
    setBusyAction(reward.id);
    setError('');
    try {
      await claimPassReward(reward.id);
    } catch (claimError) {
      setError(claimError instanceof Error ? claimError.message : 'Не удалось получить награду.');
    } finally {
      actionLock.current = false;
      setBusyAction(null);
    }
  }

  return (
    <Box className="campus-page">
      <PageHeader title="ПТУ-пасс">
      </PageHeader>
      <Card className="battle-pass-hero">
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} gap={2}>
            <Box>
              <Typography variant="overline">{passSeasonName ?? 'ПТУ-ПАСС'}</Typography>
              <Typography variant="h3" className="battle-pass-level-title" fontWeight={750}>{currentLevel === null ? 'Уровень —' : `Уровень ${currentLevel} / 50`}</Typography>
              <Typography>{currentLevel === null ? 'Ваш прогресс появится здесь.' : currentLevel >= 50 ? 'Максимальный уровень сезона' : `Ещё ${needed} АР до следующего уровня`}</Typography>
              {passStartsAt && passEndsAt && <Typography variant="caption" color="text.secondary">
                Сезон: {new Intl.DateTimeFormat('ru-RU', { dateStyle: 'medium' }).format(new Date(passStartsAt))}
                {' — '}
                {new Intl.DateTimeFormat('ru-RU', { dateStyle: 'medium' }).format(new Date(passEndsAt))}
              </Typography>}
            </Box>
            <Box className="battle-pass-progress">
              <Stack direction="row" justifyContent="space-between"><Typography variant="caption">Академический рейтинг</Typography><Typography variant="caption">{academicRating === null ? '—' : `${academicRating} АР`} · {progressInLevel ?? '—'} / 20</Typography></Stack>
              <LinearProgress variant="determinate" value={progressPercent} />
            </Box>
          </Stack>
        </CardContent>
      </Card>
      {error && <Alert severity="error">{error}</Alert>}
      {workflowError && <Alert severity="error">{workflowError}</Alert>}
      <SectionCard title="Уровни сезона" icon={<CardGiftcardRoundedIcon />}>
        <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} justifyContent="space-between" gap={1.5} mb={2}>
          {premiumPassOwned === true && <Chip size="small" color="success" label="Премиальный пропуск активен" />}
          {premiumPassOwned === false && premiumPassPrice !== null && (
            <Button variant="contained" disabled={busyAction !== null || walletBalance === null || walletBalance < premiumPassPrice} onClick={() => void handlePurchase()}>
              {busyAction === 'purchase' ? <CircularProgress size={18} color="inherit" /> : `Купить премиум · ${premiumPassPrice} жетонов`}
            </Button>
          )}
        </Stack>
        {passRewards.length === 0 ? (
          <Alert severity="info">В сезоне пока нет наград.</Alert>
        ) : (
        <Box className="battle-pass-layout" aria-label="Все 50 уровней ПТУ-пасса">
          <Box className="battle-pass-legend" aria-hidden="true">
            <Box className="battle-pass-legend-spacer" />
            <Box className="battle-pass-legend-lane battle-pass-legend-free">
              <Typography variant="caption" fontWeight={750}>Бесплатно</Typography>
            </Box>
            <Box className="battle-pass-legend-lane battle-pass-legend-premium">
              <Typography variant="caption" fontWeight={750}>Премиум</Typography>
            </Box>
          </Box>
          <Box className="battle-pass-levels" ref={battlePassLevelsRef}>
            {Array.from({ length: 50 }, (_, index) => {
              const level = index + 1;
              const freeReward = passRewards.find((reward) => reward.track === 'free' && reward.level === level);
              const premiumReward = passRewards.find((reward) => reward.track === 'premium' && reward.level === level);
              return (
                <Box
                  className={`battle-pass-level${level <= (currentLevel ?? 0) ? ' battle-pass-level-current' : ''}`}
                  key={level}
                  ref={level === Math.max(currentLevel ?? 1, 1) ? currentLevelRef : undefined}
                >
                  <Typography variant="caption" fontWeight={750}>Ур. {level}</Typography>
                  {([
                    { reward: freeReward, track: 'free' as const },
                    { reward: premiumReward, track: 'premium' as const },
                  ]).map(({ reward, track }) => (
                    <Box className={`battle-pass-reward battle-pass-reward-${track}${reward ? '' : ' battle-pass-reward-empty'}`} key={track}>
                      {reward && (
                        <>
                          <Box className="battle-pass-reward-main">
                            <Avatar className={`battle-pass-reward-icon battle-pass-reward-icon-${track}`}>
                              <AuthenticatedImage
                                src={reward.imageUrl ?? undefined}
                                alt={reward.title}
                                className="battle-pass-reward-image"
                                fallback={reward.rewardKind === 'case' ? <CardGiftcardRoundedIcon /> : <WorkspacePremiumRoundedIcon />}
                              />
                            </Avatar>
                            <Typography className="battle-pass-reward-title" variant="caption" title={reward.title}>{reward.title}</Typography>
                          </Box>
                          <Box className="battle-pass-reward-action">
                            {reward.claimed ? <Chip size="small" label="Получено" color="success" /> :
                              reward.level <= (currentLevel ?? 0) && (track === 'free' || premiumPassOwned === true) ? (
                                <Button
                                  size="small"
                                  variant="outlined"
                                  className="battle-pass-claim-button"
                                  disabled={busyAction !== null}
                                  onClick={() => void handleClaim(reward)}
                                >
                                  {busyAction === reward.id ? <CircularProgress size={15} /> : 'Забрать'}
                                </Button>
                              ) : (
                                <Typography className="battle-pass-reward-lock" variant="caption" color="text.secondary">
                                  {reward.level > (currentLevel ?? 0) ? 'Закрыто' : 'Нужен премиум'}
                                </Typography>
                              )}
                          </Box>
                        </>
                      )}
                    </Box>
                  ))}
                </Box>
              );
            })}
          </Box>
        </Box>
        )}
      </SectionCard>
      <Button component={Link} to="/student/inventory" variant="outlined" startIcon={<WorkspacePremiumRoundedIcon />}>Открыть инвентарь</Button>
    </Box>
  );
}

export function InventoryPage() {
  const { inventory, openCase, walletBalance, error: workflowError, campusCases } = useCampusWorkflow();
  const [openingId, setOpeningId] = useState<string | null>(null);
  const [selectedCaseId, setSelectedCaseId] = useState<string | null>(null);
  const [openedItem, setOpenedItem] = useState<InventoryItem | null>(null);
  const [inventoryTab, setInventoryTab] = useState<'cases' | 'items'>('cases');
  const [reel, setReel] = useState<Array<{ label: string; rarity: string; winner: boolean }>>([]);
  const [reelOffset, setReelOffset] = useState(0);
  const [reelPhase, setReelPhase] = useState<'idle' | 'rolling' | 'suspense' | 'settling' | 'stopped'>('idle');
  const [suspenseIndex, setSuspenseIndex] = useState<number | null>(null);
  const [revealComplete, setRevealComplete] = useState(false);
  const [error, setError] = useState('');
  const actionLock = useRef(false);
  const reelWindow = useRef<HTMLDivElement>(null);
  const reelTrack = useRef<HTMLDivElement>(null);
  const winnerIndex = 18;

  useEffect(() => {
    if (!openedItem || reel.length === 0) return;
    let suspenseTimer = 0;
    let revealTimer = 0;
    let completionTimer = 0;
    const frame = requestAnimationFrame(() => {
      const visibleWidth = reelWindow.current?.clientWidth ?? 320;
      const trackWidth = reelTrack.current?.scrollWidth ?? 0;
      const maxOffset = Math.max(0, trackWidth - visibleWidth);
      const finalOffset = Math.min(maxOffset, Math.max(0, winnerIndex * 156 + 12 + 74 - visibleWidth / 2));
      const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

      if (reduceMotion) {
        setReelPhase('stopped');
        setReelOffset(finalOffset);
        revealTimer = window.setTimeout(() => {
          setReelPhase('stopped');
          setRevealComplete(true);
        }, 80);
        return;
      }

      const settleRange = 2 * 156;
      const minStopOffset = Math.max(0, finalOffset - settleRange);
      const maxStopOffset = Math.min(maxOffset, finalOffset + settleRange);
      const edgeRoll = Math.random();
      const randomStopOffset = edgeRoll < 0.05
        ? minStopOffset
        : edgeRoll > 0.95
          ? maxStopOffset
          : minStopOffset + Math.random() * (maxStopOffset - minStopOffset);
      const stopIndex = Math.min(
        reel.length - 1,
        Math.max(0, Math.round((randomStopOffset + visibleWidth / 2 - 12 - 74) / 156)),
      );
      setReelPhase('rolling');
      setSuspenseIndex(null);
      setReelOffset(randomStopOffset);

      suspenseTimer = window.setTimeout(() => {
        setReelPhase('suspense');
        setSuspenseIndex(stopIndex);
        revealTimer = window.setTimeout(() => {
          setReelPhase('settling');
          setSuspenseIndex(null);
          setReelOffset(finalOffset);
          completionTimer = window.setTimeout(() => {
            setReelPhase('stopped');
            setRevealComplete(true);
          }, 700);
        }, 650);
      }, 4000);
    });
    return () => {
      cancelAnimationFrame(frame);
      window.clearTimeout(suspenseTimer);
      window.clearTimeout(revealTimer);
      window.clearTimeout(completionTimer);
    };
  }, [openedItem, reel.length]);

  async function handleOpenCase(item: InventoryItem) {
    if (actionLock.current) return;
    actionLock.current = true;
    setOpeningId(item.id);
    setOpenedItem(null);
    setReel([]);
    setReelOffset(0);
    setReelPhase('idle');
    setSuspenseIndex(null);
    setRevealComplete(false);
    setError('');
    try {
      const reward = await openCase(item.id);
      const rewards = campusCases.find((entry) => entry.id === item.caseId)?.rewards ?? [];
      const filler = Array.from({ length: winnerIndex }, () => {
        const candidate = rewards[Math.floor(Math.random() * rewards.length)];
        return {
          label: candidate?.name ?? 'Награда',
          rarity: candidate?.rarity ?? 'Обычный',
          winner: false,
        };
      });
      setReel([
        ...filler.map(({ label, rarity, winner }) => ({ label, rarity, winner })),
        { label: reward.name, rarity: reward.rarity ?? 'Обычный', winner: true },
        ...Array.from({ length: 5 }, (_, index) => ({
          label: rewards[index % Math.max(rewards.length, 1)]?.name ?? 'Награда',
          rarity: rewards[index % Math.max(rewards.length, 1)]?.rarity ?? 'Обычный',
          winner: false,
        })),
      ]);
      setReelOffset(0);
      setOpenedItem(reward);
    } catch (openError) {
      setError(openError instanceof Error ? openError.message : 'Не удалось открыть кейс.');
    } finally {
      setOpeningId(null);
      actionLock.current = false;
    }
  }

  return (
    <Box className="campus-page">
      <PageHeader title="Инвентарь">
        <Typography color="text.secondary">{formatNounCount(inventory.filter((item) => item.kind === 'case').length, 'кейс', 'кейса', 'кейсов')} · {formatNounCount(inventory.filter((item) => item.kind === 'cosmetic').length, 'предмет', 'предмета', 'предметов')} · {walletBalance?.toLocaleString('ru-RU') ?? '—'} жетонов</Typography>
      </PageHeader>
      <Tabs value={inventoryTab} onChange={(_, value: 'cases' | 'items') => setInventoryTab(value)} aria-label="Раздел инвентаря">
        <Tab value="cases" label={`Кейсы (${inventory.filter((item) => item.kind === 'case').length})`} />
        <Tab value="items" label={`Предметы (${inventory.filter((item) => item.kind === 'cosmetic').length})`} />
      </Tabs>
      {error && <Alert severity="error">{error}</Alert>}
      {workflowError && <Alert severity="error">{workflowError}</Alert>}
      {inventoryTab === 'cases' && walletBalance === null && <Alert severity="info">Баланс пока недоступен.</Alert>}
      {inventoryTab === 'cases' && inventory.filter((item) => item.kind === 'case').length === 0 ? (
        <SectionCard title="Пока пусто" icon={<CardGiftcardRoundedIcon />}>
          <Typography color="text.secondary">Кейсы не загружены.</Typography>
        </SectionCard>
      ) : inventoryTab === 'cases' ? <Box className="inventory-grid inventory-case-grid">{inventory.filter((item) => item.kind === 'case').map((item) => {
        const caseInfo = campusCases.find((entry) => entry.id === item.caseId);
        const canAfford = walletBalance !== null && caseInfo !== undefined && walletBalance >= caseInfo.price;
        return (
        <InventoryCard
          key={item.id}
          item={item}
          onOpen={handleOpenCase}
          isOpening={openingId === item.id}
          canAfford={canAfford}
          showRewards={selectedCaseId === item.id}
          onToggleRewards={() => setSelectedCaseId((current) => current === item.id ? null : item.id)}
        />
      );})}</Box> : inventory.filter((item) => item.kind === 'cosmetic').length === 0 ? (
        <SectionCard title="Пока пусто" icon={<WorkspacePremiumRoundedIcon />}>
          <Typography color="text.secondary">Полученные награды появятся здесь. Их можно выбрать в настройках профиля.</Typography>
        </SectionCard>
      ) : <Box className="inventory-grid">{inventory.filter((item) => item.kind === 'cosmetic').map((item) => (
        <Card className="inventory-item" key={item.id}>
          <CardContent>
            <Avatar className="inventory-item-icon">
              <AuthenticatedImage src={item.imageUrl} alt={item.name} className="inventory-item-image" fallback={<WorkspacePremiumRoundedIcon />} />
            </Avatar>
            <Chip size="small" label={item.rarity ?? 'Награда'} className={`rarity-chip rarity-${item.rarity === 'Эпический' ? 'epic' : item.rarity === 'Редкий' ? 'rare' : 'common'}`} />
            <Typography fontWeight={700} mt={1}>{item.name}</Typography>
            <Typography variant="caption" color="text.secondary">{item.cosmeticType === 'avatar' ? 'Аватар' : item.cosmeticType === 'avatar-frame' ? 'Рамка профиля' : item.cosmeticType === 'profile-background' ? 'Фон профиля' : 'Значок'}</Typography>
            <Typography variant="caption" display="block" color="text.secondary">{item.source}</Typography>
          </CardContent>
        </Card>
      ))}</Box>}
      <Dialog open={openedItem !== null} onClose={() => revealComplete && setOpenedItem(null)} maxWidth="md" fullWidth>
        <DialogTitle>Открытие кейса</DialogTitle>
        <DialogContent>
          <Box className="case-reel-window" ref={reelWindow}>
            <Box ref={reelTrack} className={`case-reel-track case-reel-${reelPhase}`} style={{ transform: `translateX(-${reelOffset}px)` }}>
              {reel.map((entry, index) => (
                <Box className={`case-reel-item${entry.winner && revealComplete ? ' case-reel-winner' : ''}${reelPhase === 'suspense' && suspenseIndex === index ? ' case-reel-nearby' : ''}`} key={`${index}-${entry.label}`}>
                  <Avatar className="case-reel-icon"><CardGiftcardRoundedIcon /></Avatar>
                  <Typography variant="caption" fontWeight={650}>{entry.label}</Typography>
                  <Typography variant="caption" color="text.secondary">{entry.rarity}</Typography>
                </Box>
              ))}
            </Box>
            <Box className="case-reel-marker" />
          </Box>
          {!revealComplete && (
            <Typography className="case-reel-status" role="status" variant="body2" color="text.secondary">
              {reelPhase === 'suspense' ? 'Прокрутка остановилась…' : reelPhase === 'settling' ? 'Определяем выпавшую награду…' : 'Кейс открыт, выбираем награду…'}
            </Typography>
          )}
          {revealComplete && openedItem && (
            <Box className="case-reveal-result">
              <Avatar className="case-result-icon">
                <AuthenticatedImage src={openedItem.imageUrl} alt={openedItem.name} className="inventory-item-image" fallback={<WorkspacePremiumRoundedIcon />} />
              </Avatar>
              <Box>
                <Typography variant="overline">НАГРАДА ДОБАВЛЕНА В ИНВЕНТАРЬ</Typography>
                <Typography variant="h6" fontWeight={750}>{openedItem.name}</Typography>
                <Typography color="text.secondary">{openedItem.rarity} · {openedItem.source}</Typography>
              </Box>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button disabled={!revealComplete} onClick={() => setOpenedItem(null)}>Готово</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export function CertificateManagementPage() {
  const [certificateRequests, setCertificateRequests] = useState<CertificateRequestView[]>([]);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [rejectionTarget, setRejectionTarget] = useState<CertificateRequestView | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [selectedSection, setSelectedSection] = useState(0);
  const actionLock = useRef(false);
  const requestSections = [
    certificateRequests.filter((request) => request.status === 'PENDING'),
    certificateRequests.filter((request) => request.status === 'IN_PROGRESS'),
    certificateRequests.filter((request) => request.status === 'READY' || request.status === 'APPROVED'),
    certificateRequests.filter((request) => request.status === 'ISSUED' || request.status === 'REJECTED'),
  ];
  const sectionTitles = ['Новые', 'В работе', 'Ожидают выдачи', 'История'];

  useEffect(() => {
    let active = true;
    setLoading(true);
    void listCertificates().then((items) => {
      if (active) setCertificateRequests(items);
    }).catch((loadError: unknown) => {
      if (active) setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить заявления.');
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, []);

  async function handleStatusUpdate(
    request: CertificateRequestView,
    status: Exclude<CertificateStatus, 'PENDING' | 'APPROVED'>,
    reason?: string,
  ) {
    if (actionLock.current) return;
    actionLock.current = true;
    setBusyId(request.id);
    setError('');
    try {
      const updated = await updateCertificateRequestStatus(request.id, status, reason);
      setCertificateRequests((current) => current.map((item) => item.id === request.id ? updated : item));
      setRejectionTarget(null);
      setRejectionReason('');
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : 'Не удалось изменить статус заявки.');
    } finally {
      actionLock.current = false;
      setBusyId(null);
    }
  }

  async function handleDocumentUpload(request: CertificateRequestView, file: File | null) {
    if (!file || actionLock.current) return;
    if (!file.name.toLowerCase().endsWith('.pdf') || (file.type && file.type.toLowerCase() !== 'application/pdf') || file.size > 20 * 1024 * 1024) {
      setError('Загрузите файл PDF размером не более 20 МБ.');
      return;
    }
    actionLock.current = true;
    setBusyId(request.id);
    setError('');
    try {
      const updated = await uploadCertificateDocument(request.id, file);
      setCertificateRequests((current) => current.map((item) => item.id === request.id ? updated : item));
    } catch (uploadError) {
      setError(uploadError instanceof Error ? uploadError.message : 'Не удалось загрузить файл справки.');
    } finally {
      actionLock.current = false;
      setBusyId(null);
    }
  }

  async function handleDocumentDownload(request: CertificateRequestView) {
    if (!request.documentAvailable || !request.documentName) return;
    setError('');
    try {
      await downloadCertificateDocument(request.id, request.documentName);
    } catch (downloadError) {
      setError(downloadError instanceof Error ? downloadError.message : 'Не удалось скачать файл справки.');
    }
  }

  function renderRequest(request: CertificateRequestView) {
    return (
      <Card className="office-request-card" key={request.id} variant="outlined">
        <CardContent>
          <Box className="office-request-heading">
            <Box>
              <Typography variant="h6" fontSize={16} fontWeight={750}>{certificateTitle(request)}</Typography>
              <Typography variant="body2" color="text.secondary">{request.studentName}</Typography>
            </Box>
            <Chip size="small" label={certificateStatusLabels[request.status]} className={`certificate-status certificate-status-${request.status}`} />
          </Box>
          <Typography variant="caption" color="text.secondary">Подана {formatDate(request.createdAt)}</Typography>
          <Typography variant="body2" mt={1}>{request.description || 'Дополнительных пояснений нет.'}</Typography>
          <Typography variant="caption" color="text.secondary">
            Получение: {request.deliveryMethod === 'DIGITAL' ? 'электронный файл' : 'в учебной части'}
          </Typography>
          {request.rejectionReason && <Typography className="office-request-comment">{request.rejectionReason}</Typography>}
          {request.documentAvailable && request.documentName && (
            <Button
              size="small"
              onClick={() => void handleDocumentDownload(request)}
            >
              Скачать {request.documentName}
            </Button>
          )}
          {request.status !== 'REJECTED' && request.status !== 'ISSUED' && (
            <Stack direction={{ xs: 'column', sm: 'row' }} gap={1} className="office-request-actions">
              <Button component="label" variant="outlined" disabled={busyId !== null}>
                {busyId === request.id ? <CircularProgress size={18} /> : request.documentAvailable ? 'Заменить PDF справки' : 'Загрузить PDF справки'}
                <input
                  hidden
                  type="file"
                  accept="application/pdf,.pdf"
                  onChange={(event) => {
                    const input = event.currentTarget;
                    const file = input.files?.[0] ?? null;
                    input.value = '';
                    void handleDocumentUpload(request, file);
                  }}
                />
              </Button>
              {request.status === 'PENDING' && (
                <>
                  <Button variant="contained" color="primary" disabled={busyId !== null} onClick={() => void handleStatusUpdate(request, 'IN_PROGRESS')}>
                    {busyId === request.id ? <CircularProgress size={18} color="inherit" /> : 'Взять в работу'}
                  </Button>
                  <Button variant="outlined" color="error" disabled={busyId !== null} onClick={() => { setRejectionTarget(request); setRejectionReason(''); }}>
                    Отклонить
                  </Button>
                </>
              )}
              {request.status === 'IN_PROGRESS' && (
                <Button variant="contained" color="primary" disabled={busyId !== null || !request.documentAvailable} onClick={() => void handleStatusUpdate(request, 'READY')}>
                  Отметить «Ожидает выдачи»
                </Button>
              )}
              {(request.status === 'READY' || request.status === 'APPROVED') && (
                <Button variant="contained" color="success" disabled={busyId !== null} onClick={() => void handleStatusUpdate(request, 'ISSUED')}>
                  Отметить «Выдана»
                </Button>
              )}
            </Stack>
          )}
        </CardContent>
      </Card>
    );
  }

  return (
    <Box className="campus-page">
      <PageHeader title="Заявления на справки">
      </PageHeader>
      <Box className="office-metrics">
        {sectionTitles.map((title, index) => <Card key={title}><CardContent><Typography variant="caption" color="text.secondary">{title}</Typography><Typography variant="h4" fontWeight={750}>{requestSections[index].length}</Typography></CardContent></Card>)}
      </Box>
      {error && <Alert severity="error">{error}</Alert>}
      <SectionCard title="Заявления" icon={<WorkspacePremiumRoundedIcon />}>
        <Tabs value={selectedSection} onChange={(_, value: number) => setSelectedSection(value)} variant="scrollable" scrollButtons="auto" aria-label="Разделы заявлений">
          {sectionTitles.map((title, index) => <Tab key={title} label={`${title} · ${requestSections[index].length}`} />)}
        </Tabs>
        {loading ? <CircularProgress size={24} /> : requestSections[selectedSection].length
          ? <Stack spacing={1.5} mt={2}>{requestSections[selectedSection].map(renderRequest)}</Stack>
          : <Typography color="text.secondary" mt={2}>В этом разделе пока нет заявлений.</Typography>}
      </SectionCard>
      <Dialog open={rejectionTarget !== null} onClose={() => { if (!busyId) setRejectionTarget(null); }} fullWidth maxWidth="sm">
        <DialogTitle>Отклонить заявление</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            required
            multiline
            minRows={2}
            label="Причина отклонения"
            value={rejectionReason}
            onChange={(event) => setRejectionReason(event.target.value)}
            inputProps={{ maxLength: 1000 }}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button disabled={busyId !== null} onClick={() => setRejectionTarget(null)}>Отмена</Button>
          <Button
            color="error"
            variant="contained"
            disabled={busyId !== null || !rejectionReason.trim()}
            onClick={() => {
              if (rejectionTarget) void handleStatusUpdate(rejectionTarget, 'REJECTED', rejectionReason.trim());
            }}
          >
            {busyId ? <CircularProgress size={18} color="inherit" /> : 'Подтвердить отказ'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

const weekdays = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота', 'Воскресенье'];
const weekdayAbbreviations: Record<string, string> = {
  Понедельник: 'Пн',
  Вторник: 'Вт',
  Среда: 'Ср',
  Четверг: 'Чт',
  Пятница: 'Пт',
  Суббота: 'Сб',
  Воскресенье: 'Вс',
};

interface ScheduleDraft {
  id?: string;
  weekday: string;
  startTime: string;
  endTime: string;
  groupId: string;
  disciplineId: string;
  teacherId: string;
  room: string;
}

const defaultPeriods = [
  ['09:00', '10:30'],
  ['10:45', '12:15'],
  ['12:30', '14:00'],
  ['14:15', '15:45'],
  ['16:00', '17:30'],
];

function ScheduleWeek({ editable }: { editable: boolean }) {
  const { scheduleEntries, campusGroups, saveScheduleEntry, deleteScheduleEntry, updateScheduleTimeSlot, isLoading } = useCampusWorkflow();
  const [teachers, setTeachers] = useState<ManagedTeacher[]>([]);
  const [disciplines, setDisciplines] = useState<ManagedDiscipline[]>([]);
  const [draft, setDraft] = useState<ScheduleDraft | null>(null);
  const [periodTimes, setPeriodTimes] = useState<Record<string, { startTime: string; endTime: string }>>({});
  const [periodEditorOpen, setPeriodEditorOpen] = useState(false);
  const [scheduleMode, setScheduleMode] = useState<'all' | 'group'>('all');
  const [selectedGroupId, setSelectedGroupId] = useState('');
  const [selectedMobileDay, setSelectedMobileDay] = useState(weekdays[0]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    if (!editable) return;
    let active = true;
    void Promise.all([listManagedTeachers(), getAllAcademicDisciplines()])
      .then(([teacherItems, disciplineItems]) => {
        if (active) {
          setTeachers(teacherItems.filter((teacher) => teacher.active));
          setDisciplines(disciplineItems);
        }
      })
      .catch((loadError: unknown) => { if (active) setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить список преподавателей.'); });
    return () => { active = false; };
  }, [editable]);

  useEffect(() => {
    if (!selectedGroupId && campusGroups.length) setSelectedGroupId(campusGroups[0].id);
  }, [campusGroups, selectedGroupId]);

  const activeEntries = scheduleMode === 'group' && editable
    ? scheduleEntries.filter((entry) => entry.groupId === selectedGroupId)
    : scheduleEntries;
  const periods = Array.from(new Map([
    ...defaultPeriods,
    ...activeEntries.map((entry) => [entry.startTime, entry.endTime]),
  ].map(([start, end]) => [`${start}-${end}`, { startTime: start, endTime: end }])).values())
    .sort((a, b) => a.startTime.localeCompare(b.startTime));
  const selectedGroup = campusGroups.find((group) => group.id === selectedGroupId);

  function openDraft(weekday: string, startTime = defaultPeriods[0][0], endTime = defaultPeriods[0][1], groupId = '') {
    setDraft({
      weekday,
      startTime,
      endTime,
      groupId: groupId || (scheduleMode === 'group' ? selectedGroupId : campusGroups[0]?.id) || '',
      disciplineId: disciplines[0]?.id ?? '',
      teacherId: teachers[0]?.id ?? '',
      room: '',
    });
  }

  async function submitDraft(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!draft || busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const group = campusGroups.find((item) => item.id === draft.groupId);
      const teacher = teachers.find((item) => item.id === draft.teacherId);
      const discipline = disciplines.find((item) => item.id === draft.disciplineId);
      if (!group || !teacher || !discipline) throw new Error('Выберите группу, дисциплину и действующего преподавателя из списка.');
      await saveScheduleEntry({
        weekday: draft.weekday,
        startTime: draft.startTime,
        endTime: draft.endTime,
        groupId: group.id,
        group: group.name,
        subject: discipline.name,
        disciplineId: discipline.id,
        teacherId: teacher.id,
        teacher: teacher.fullName,
        room: draft.room.trim(),
      }, draft.id);
      setDraft(null);
      setSuccess(draft.id ? 'Изменение расписания сохранено. Студенты группы получат уведомление.' : 'Занятие добавлено. Студенты группы получат уведомление.');
    } catch (saveError: unknown) {
      setError(saveError instanceof Error ? saveError.message : 'Не удалось сохранить занятие.');
    } finally {
      setBusy(false);
    }
  }

  async function removeEntry(entry: ScheduleEntry) {
    if (busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      await deleteScheduleEntry(entry.id);
      setSuccess('Занятие отменено. Студенты группы получат уведомление.');
    } catch (deleteError: unknown) {
      setError(deleteError instanceof Error ? deleteError.message : 'Не удалось отменить занятие.');
    } finally {
      setBusy(false);
    }
  }

  async function savePeriodTime(oldStart: string, oldEnd: string) {
    const updated = periodTimes[`${oldStart}-${oldEnd}`];
    if (!updated || updated.startTime >= updated.endTime) {
      setError('Время окончания должно быть позже времени начала.');
      return;
    }
    const entries = scheduleEntries.filter((entry) => entry.startTime === oldStart && entry.endTime === oldEnd);
    if (!entries.length || busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      await updateScheduleTimeSlot({
        previousStartTime: oldStart,
        previousEndTime: oldEnd,
        startTime: updated.startTime,
        endTime: updated.endTime,
      });
      setSuccess('Время пары обновлено для всех дней недели; студентам отправлены уведомления.');
    } catch (updateError: unknown) {
      setError(updateError instanceof Error ? updateError.message : 'Не удалось изменить время пары.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}
      {editable && (
        <Box className="schedule-mode-toolbar">
          <Tabs
            value={scheduleMode}
            onChange={(_, value: 'all' | 'group') => setScheduleMode(value)}
            aria-label="Режим просмотра расписания"
            className="schedule-mode-tabs"
          >
            <Tab value="all" label="Общее расписание" />
            <Tab value="group" label="По группам" />
          </Tabs>
          {scheduleMode === 'group' && (
            <FormControl size="small" className="schedule-group-select" disabled={!campusGroups.length}>
              <InputLabel id="schedule-group-label">Учебная группа</InputLabel>
              <Select
                labelId="schedule-group-label"
                label="Учебная группа"
                value={selectedGroupId}
                onChange={(event) => setSelectedGroupId(event.target.value)}
              >
                {campusGroups.map((group) => <MenuItem key={group.id} value={group.id}>{group.name}</MenuItem>)}
              </Select>
            </FormControl>
          )}
        </Box>
      )}
      <Box className={`schedule-toolbar${editable && scheduleMode === 'all' ? ' schedule-toolbar-compact' : ''}`}>
          <Box className="schedule-summary">
            <Typography variant="body2" fontWeight={700}>
              {formatNounCount(activeEntries.length, 'занятие', 'занятия', 'занятий')}
              {(!editable || scheduleMode === 'group') && ' за неделю'}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {scheduleMode === 'group' && editable
                ? selectedGroup?.name ?? 'Выберите группу'
                : formatNounCount(new Set(activeEntries.map((entry) => entry.groupId)).size, 'группа', 'группы', 'групп')}
            </Typography>
          </Box>
          {editable && scheduleMode === 'all' && (
            <Button
              variant="outlined"
              size="small"
              startIcon={<ScheduleRoundedIcon />}
              onClick={() => setPeriodEditorOpen((open) => !open)}
              aria-expanded={periodEditorOpen}
            >
              {periodEditorOpen ? 'Скрыть интервалы' : 'Время пар'}
            </Button>
          )}
        </Box>
        {editable && scheduleMode === 'all' && periodEditorOpen && (
          <Box className="schedule-period-editor">
            <Box className="schedule-period-editor-heading">
              <Typography variant="subtitle1" fontWeight={750}>Время занятий</Typography>
              <Typography variant="body2" color="text.secondary">
                Новое время применяется ко всем занятиям с таким интервалом во все дни недели.
              </Typography>
            </Box>
            {periods.filter((period) => activeEntries.some((entry) => entry.startTime === period.startTime
              && entry.endTime === period.endTime)).length === 0
              ? <Typography variant="body2" color="text.secondary">Сначала добавьте занятие, чтобы настроить время пары.</Typography>
              : <Box className="schedule-period-list">
                {periods.filter((period) => activeEntries.some((entry) => entry.startTime === period.startTime
                  && entry.endTime === period.endTime)).map((period) => {
                  const periodKey = `${period.startTime}-${period.endTime}`;
                  const times = periodTimes[periodKey] ?? period;
                  return (
                    <Box className="schedule-period-row" key={periodKey}>
                      <Typography variant="body2" fontWeight={700}>{period.startTime}–{period.endTime}</Typography>
                      <Stack direction="row" spacing={1} className="schedule-period-fields">
                        <TextField
                          label="Начало"
                          type="time"
                          size="small"
                          value={times.startTime}
                          onChange={(event) => setPeriodTimes((current) => ({ ...current, [periodKey]: { ...times, startTime: event.target.value } }))}
                          InputLabelProps={{ shrink: true }}
                        />
                        <TextField
                          label="Окончание"
                          type="time"
                          size="small"
                          value={times.endTime}
                          onChange={(event) => setPeriodTimes((current) => ({ ...current, [periodKey]: { ...times, endTime: event.target.value } }))}
                          InputLabelProps={{ shrink: true }}
                        />
                      </Stack>
                      <Button
                        size="small"
                        disabled={busy || (times.startTime === period.startTime && times.endTime === period.endTime)}
                        onClick={() => void savePeriodTime(period.startTime, period.endTime)}
                      >
                        Применить
                      </Button>
                    </Box>
                  );
                })}
              </Box>}
          </Box>
        )}
        <Box className="schedule-mobile-day-tabs" aria-label="Выберите день">
          {weekdays.map((day) => (
            <Button
              key={day}
              size="small"
              variant={selectedMobileDay === day ? 'contained' : 'outlined'}
              onClick={() => setSelectedMobileDay(day)}
            >
              {weekdayAbbreviations[day]}
            </Button>
          ))}
        </Box>
        <Box className={`schedule-calendar${editable && scheduleMode === 'all' ? ' schedule-calendar-compact' : ''}`}>
          <Box className="schedule-calendar-head">
            <Box className="schedule-calendar-corner">ВРЕМЯ</Box>
            {weekdays.map((day) => (
              <Box key={day} className={`schedule-calendar-day-heading${day === selectedMobileDay ? ' schedule-calendar-mobile-selected' : ''}`}>
                <Typography variant="caption" fontWeight={750} className="schedule-calendar-day-label">{day}</Typography>
                {editable && (
                  <Button size="small" onClick={() => openDraft(day)} aria-label={`Добавить занятие: ${day}`}>
                    <AddRoundedIcon fontSize="small" />
                  </Button>
                )}
              </Box>
            ))}
          </Box>
          {periods.map((period) => (
            <Box className="schedule-calendar-row" key={`${period.startTime}-${period.endTime}`}>
              <Box className="schedule-calendar-time">
                <Typography variant="caption" fontWeight={750}>{period.startTime}</Typography>
                <Typography variant="caption" color="text.secondary">{period.endTime}</Typography>
              </Box>
              {weekdays.map((day) => {
                const entries = activeEntries
                  .filter((entry) => entry.weekday === day && entry.startTime === period.startTime && entry.endTime === period.endTime)
                  .sort((a, b) => a.group.localeCompare(b.group) || a.subject.localeCompare(b.subject));
                return (
                  <Box
                    className={`schedule-calendar-cell${day === selectedMobileDay ? ' schedule-calendar-mobile-selected' : ''}${entries.length ? ' schedule-calendar-cell-filled' : ''}`}
                    key={day}
                  >
                    {entries.map((entry) => {
                      const canEdit = editable;
                      const concise = editable && scheduleMode === 'all';
                      return (
                        <Box className={`schedule-calendar-event${concise ? ' schedule-calendar-event-compact' : ''}`} key={entry.id}>
                          <Box className="schedule-calendar-event-copy">
                            <Typography variant="caption" fontWeight={750}>{entry.subject}</Typography>
                            {concise ? (
                              <Typography variant="caption">{entry.group} · ауд. {entry.room}</Typography>
                            ) : (
                              <>
                                <Typography variant="caption">{entry.group} · ауд. {entry.room}</Typography>
                                <Typography variant="caption" color="text.secondary">{entry.teacher}</Typography>
                              </>
                            )}
                          </Box>
                          {canEdit && (
                            <Box className="schedule-calendar-event-actions">
                              <Button
                                size="small"
                                aria-label={`Изменить ${entry.subject} ${entry.group}`}
                                onClick={() => setDraft({ ...entry, disciplineId: entry.disciplineId ?? '', teacherId: entry.teacherId ?? '' })}
                              ><EditRoundedIcon fontSize="small" /></Button>
                              <Button
                                size="small"
                                color="error"
                                aria-label={`Отменить ${entry.subject} ${entry.group}`}
                                disabled={busy}
                                onClick={() => void removeEntry(entry)}
                              ><DeleteOutlineRoundedIcon fontSize="small" /></Button>
                            </Box>
                          )}
                        </Box>
                      );
                    })}
                  </Box>
                );
              })}
            </Box>
          ))}
        </Box>
        {activeEntries.length === 0 && !isLoading && (
          <Alert severity="info" className="schedule-empty-state">
            {editable && scheduleMode === 'group'
              ? 'Для этой группы занятий пока нет. Добавьте пару из нужного дня и времени.'
              : editable ? 'Расписание пока не заполнено. Добавьте первое занятие, чтобы начать.' : 'Расписание пока не заполнено.'}
          </Alert>
        )}
      <Dialog open={draft !== null} onClose={() => !busy && setDraft(null)} fullWidth maxWidth="sm">
        <Box component="form" onSubmit={(event) => void submitDraft(event)}>
          <DialogTitle>{draft?.id ? 'Изменить занятие' : 'Добавить занятие'}</DialogTitle>
          <DialogContent>
            {draft && <Stack spacing={1.5} mt={1}>
              <TextField select label="День недели" value={draft.weekday} onChange={(event) => setDraft({ ...draft, weekday: event.target.value })}>
                {weekdays.map((day) => <MenuItem key={day} value={day}>{day}</MenuItem>)}
              </TextField>
              <Stack direction="row" spacing={1}>
                <TextField label="Начало" type="time" value={draft.startTime} onChange={(event) => setDraft({ ...draft, startTime: event.target.value })} InputLabelProps={{ shrink: true }} fullWidth />
                <TextField label="Окончание" type="time" value={draft.endTime} onChange={(event) => setDraft({ ...draft, endTime: event.target.value })} InputLabelProps={{ shrink: true }} fullWidth />
              </Stack>
              <TextField select label="Группа" value={draft.groupId} onChange={(event) => setDraft({ ...draft, groupId: event.target.value })} required>
                {campusGroups.map((group) => <MenuItem key={group.id} value={group.id}>{group.name}</MenuItem>)}
              </TextField>
              <TextField select label="Преподаватель" value={draft.teacherId} onChange={(event) => setDraft({ ...draft, teacherId: event.target.value })} required>
                {teachers.map((teacher) => <MenuItem key={teacher.id} value={teacher.id}>{teacher.fullName}</MenuItem>)}
              </TextField>
              <FormControl required disabled={!disciplines.length}>
                <InputLabel id="schedule-discipline-label">Дисциплина</InputLabel>
                <Select labelId="schedule-discipline-label" label="Дисциплина" value={draft.disciplineId} onChange={(event) => setDraft({ ...draft, disciplineId: event.target.value })}>
                  {disciplines.map((discipline) => <MenuItem key={discipline.id} value={discipline.id}>{discipline.departmentName} · {discipline.name}</MenuItem>)}
                </Select>
              </FormControl>
              <TextField label="Аудитория" value={draft.room} onChange={(event) => setDraft({ ...draft, room: event.target.value })} required />
            </Stack>}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDraft(null)} disabled={busy}>Отмена</Button>
            <Button type="submit" variant="contained" disabled={busy || !draft || !teachers.length || !campusGroups.length || !disciplines.length} startIcon={busy ? <CircularProgress size={18} color="inherit" /> : <SaveRoundedIcon />}>Сохранить</Button>
          </DialogActions>
        </Box>
      </Dialog>
    </>
  );
}

export function ScheduleManagementPage() {
  const { error: workflowError } = useCampusWorkflow();
  return (
    <Box className="campus-page">
      <PageHeader title="Расписание занятий" />
      {workflowError && <Alert severity="error">{workflowError}</Alert>}
      <SectionCard title="Календарь занятий на неделю" icon={<CalendarMonthRoundedIcon />}>
        <ScheduleWeek editable />
      </SectionCard>
    </Box>
  );
}

export function TeacherSchedulePage() {
  const { error: workflowError } = useCampusWorkflow();
  return (
    <Box className="campus-page">
      <PageHeader title="Моё расписание" />
      {workflowError && <Alert severity="error">{workflowError}</Alert>}
      <SectionCard title="Занятия моих групп" icon={<ScheduleRoundedIcon />}>
        <ScheduleWeek editable={false} />
      </SectionCard>
    </Box>
  );
}

export function StudentSchedulePage() {
  const { scheduleChanges, error: workflowError } = useCampusWorkflow();
  return (
    <Box className="campus-page">
      <PageHeader title="Моё расписание" />
      {workflowError && <Alert severity="error">{workflowError}</Alert>}
      {scheduleChanges.length > 0 && (
        <SectionCard title="Изменения расписания" icon={<NotificationsActiveRoundedIcon />}>
          <Stack spacing={1}>
            {scheduleChanges.map((change) => (
              <Alert key={change.id} severity={change.type === 'CANCELLED' ? 'warning' : 'info'}>
                <Typography fontWeight={750}>{change.groupName} · {formatDate(change.createdAt)}</Typography>
                <Typography variant="body2">{change.message}</Typography>
              </Alert>
            ))}
          </Stack>
        </SectionCard>
      )}
      <SectionCard title="Расписание на неделю" icon={<ScheduleRoundedIcon />}>
        <ScheduleWeek editable={false} />
      </SectionCard>
    </Box>
  );
}

export function CertificateRequestPage() {
  const [certificateRequests, setCertificateRequests] = useState<CertificateRequestView[]>([]);
  const [type, setType] = useState<CertificateType>('EDUCATION');
  const [customTitle, setCustomTitle] = useState('');
  const [description, setDescription] = useState('');
  const [deliveryMethod, setDeliveryMethod] = useState<'PICKUP' | 'DIGITAL'>('PICKUP');
  const [loading, setLoading] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const actionLock = useRef(false);

  useEffect(() => {
    let active = true;
    void listMyCertificates().then((items) => {
      if (active) setCertificateRequests(items);
    }).catch((loadError: unknown) => {
      if (active) setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить ваши заявления.');
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (actionLock.current || isLoading) return;
    actionLock.current = true;
    setIsLoading(true);
    setError('');
    setSuccess('');
    try {
      const request = await createCertificateRequest({
        type,
        customTitle: type === 'OTHER' ? customTitle.trim() : null,
        description: description.trim(),
        deliveryMethod,
      });
      setCertificateRequests((current) => [request, ...current]);
      setCustomTitle('');
      setDescription('');
      setSuccess('Заявка отправлена в учебную часть.');
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Не удалось подать заявку.');
    } finally {
      actionLock.current = false;
      setIsLoading(false);
    }
  }

  async function handleDownload(request: CertificateRequestView) {
    if (!request.documentAvailable || !request.documentName) return;
    setError('');
    try {
      await downloadCertificateDocument(request.id, request.documentName);
    } catch (downloadError) {
      setError(downloadError instanceof Error ? downloadError.message : 'Не удалось скачать справку.');
    }
  }

  return (
    <Box className="campus-page">
      <PageHeader title="Справки">
        <Typography color="text.secondary">Заявка и статус обработки</Typography>
      </PageHeader>
      <SectionCard title="Подать заявку" icon={<WorkspacePremiumRoundedIcon />}>
        <Box component="form" onSubmit={handleSubmit} className="campus-form">
          <TextField select label="Вид справки" value={type} onChange={(event) => {
            const value = event.target.value;
            setType(value === 'INCOME' || value === 'MILITARY' || value === 'OTHER' ? value : 'EDUCATION');
          }} fullWidth>
            <MenuItem value="EDUCATION">Справка об обучении</MenuItem>
            <MenuItem value="INCOME">Справка о доходах</MenuItem>
            <MenuItem value="MILITARY">Справка для военкомата</MenuItem>
            <MenuItem value="OTHER">Свой вариант</MenuItem>
          </TextField>
          {type === 'OTHER' && <TextField label="Название справки" value={customTitle} onChange={(event) => setCustomTitle(event.target.value)} inputProps={{ maxLength: 120 }} required fullWidth />}
          <TextField
            select
            label="Способ получения"
            value={deliveryMethod}
            onChange={(event) => setDeliveryMethod(event.target.value === 'DIGITAL' ? 'DIGITAL' : 'PICKUP')}
            fullWidth
          >
            <MenuItem value="PICKUP">Заберу в учебной части</MenuItem>
            <MenuItem value="DIGITAL">Получить электронный файл</MenuItem>
          </TextField>
          <TextField
            label="Описание и дополнительные сведения"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            inputProps={{ maxLength: 2000 }}
            multiline
            minRows={3}
            fullWidth
          />
          <Typography variant="body2" color="text.secondary">Электронную справку можно скачать после подготовки; статус заявления будет виден в списке ниже.</Typography>
          {error && <Alert severity="error">{error}</Alert>}
          {success && <Alert severity="success">{success}</Alert>}
          <Button type="submit" variant="contained" disabled={isLoading} startIcon={isLoading ? <CircularProgress size={18} color="inherit" /> : undefined}>
            {isLoading ? 'Отправляем…' : 'Подать заявку'}
          </Button>
        </Box>
      </SectionCard>
      <SectionCard title="Мои заявки" icon={<EventAvailableRoundedIcon />}>
        <Stack divider={<Divider flexItem />} spacing={1.5}>
          {loading && <CircularProgress size={24} />}
          {!loading && certificateRequests.length === 0 && !error && <Typography color="text.secondary">Заявлений пока нет.</Typography>}
          {certificateRequests.map((request) => (
            <Stack key={request.id} direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={1}>
              <Box sx={{ minWidth: 0 }}>
                <Typography fontWeight={650} sx={{ overflowWrap: 'anywhere' }}>{certificateTitle(request)}</Typography>
                <Typography variant="caption" color="text.secondary" sx={{ overflowWrap: 'anywhere' }}>{formatDate(request.createdAt, false)} · {request.deliveryMethod === 'DIGITAL' ? 'электронный файл' : 'получение в учебной части'}</Typography>
                {request.description && <Typography variant="body2">{request.description}</Typography>}
                {request.rejectionReason && <Typography variant="body2" color="error">{request.rejectionReason}</Typography>}
              </Box>
              <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                <Chip size="small" label={certificateStatusLabels[request.status]} className={`certificate-status certificate-status-${request.status}`} />
                {(request.status === 'READY' || request.status === 'ISSUED' || request.status === 'APPROVED') && request.documentAvailable && request.documentName && (
                  <Button size="small" onClick={() => void handleDownload(request)}>Скачать PDF</Button>
                )}
              </Stack>
            </Stack>
          ))}
        </Stack>
      </SectionCard>
    </Box>
  );
}

export function AcademicReviewModerationPage() {
  const [reviews, setReviews] = useState<EventModerationReview[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const actionLock = useRef(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    void listEventReviewModerationQueue()
      .then((items) => {
        if (active) setReviews(items);
      })
      .catch((loadError: unknown) => {
        if (active) setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить отзывы.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  async function handleDecision(review: EventModerationReview, approved: boolean) {
    if (actionLock.current) return;
    actionLock.current = true;
    setBusyId(review.id);
    setError('');
    setSuccess('');
    try {
      await moderateCampusEventReview(review.eventId, review.id, approved);
      setReviews((current) => current.filter((item) => item.id !== review.id));
      setSuccess(approved ? 'Отзыв одобрен.' : 'Отзыв отклонён.');
    } catch (decisionError) {
      setError(decisionError instanceof Error ? decisionError.message : 'Не удалось обработать отзыв.');
    } finally {
      actionLock.current = false;
      setBusyId(null);
    }
  }

  return (
    <Box className="campus-page">
      <PageHeader title="Отзывы на модерации">
        <Typography color="text.secondary">Проверка отзывов о мероприятиях: нецензурная лексика, спам и оскорбления</Typography>
      </PageHeader>
      {error && <Alert severity="error">{error}</Alert>}
      {success && <Alert severity="success">{success}</Alert>}
      <SectionCard title={`Ожидают проверки · ${reviews.length}`} icon={<RateReviewRoundedIcon />}>
        {loading ? <CircularProgress size={24} /> : reviews.length === 0 ? (
          <Typography color="text.secondary">Все отзывы рассмотрены.</Typography>
        ) : (
          <Stack spacing={1.5}>
            {reviews.map((review) => (
              <Box className="event-review-moderation" key={review.id}>
                <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={1}>
                  <Box>
                    <Typography fontWeight={700}>{review.studentName}{review.group ? ` · ${review.group}` : ''}</Typography>
                    <Typography variant="caption" color="text.secondary">{review.eventTitle} · {formatDate(review.createdAt)}</Typography>
                  </Box>
                  <Stack direction="row" alignItems="center" gap={1}>
                    <Rating value={review.rating} readOnly size="small" aria-label={`Оценка ${review.rating} из 5`} />
                    <Chip size="small" label="Ожидает проверки" />
                  </Stack>
                </Stack>
                <Typography variant="body2" mt={1} sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{review.comment}</Typography>
                {review.flaggedWords.length > 0 && (
                  <Alert severity="warning" sx={{ mt: 1 }}>
                    Найдены недопустимые слова: {review.flaggedWords.join(', ')}
                  </Alert>
                )}
                <Stack direction="row" flexWrap="wrap" gap={1} mt={1}>
                  <Button size="small" variant="contained" color="success" disabled={busyId !== null} onClick={() => void handleDecision(review, true)}>
                    {busyId === review.id ? <CircularProgress size={16} color="inherit" /> : 'Одобрить'}
                  </Button>
                  <Button size="small" variant="outlined" color="error" disabled={busyId !== null} onClick={() => void handleDecision(review, false)}>
                    Отклонить
                  </Button>
                </Stack>
              </Box>
            ))}
          </Stack>
        )}
      </SectionCard>
    </Box>
  );
}
