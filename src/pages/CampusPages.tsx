import { useRef, useState, type FormEvent, type ReactNode } from 'react';
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
  LinearProgress,
  MenuItem,
  Stack,
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
import PlaceRoundedIcon from '@mui/icons-material/PlaceRounded';
import PersonAddAltRoundedIcon from '@mui/icons-material/PersonAddAltRounded';
import { Link, useLocation } from 'react-router-dom';
import { campusGroups, caseOpeningPrice, useCampusWorkflow, type CampusEvent, type CertificateRequest, type InventoryItem, type PassReward, type ScheduleEntry } from '../data/campus-workflow';

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

function canStudentReview(event: CampusEvent): boolean {
  if (Date.parse(event.endsAt) > Date.now() || event.reviews.some((review) => review.studentName === 'Александра Ким')) return false;
  const participant = event.participants.find((item) => item.studentName === 'Александра Ким' && item.participationConfirmed);
  if (!participant) return false;
  if (event.format === 'individual') return true;
  const registration = event.groupRegistrations.find((item) => item.id === participant.groupRegistrationId);
  if (!registration || event.groupMinPercent === null) return false;
  const confirmed = event.participants.filter((item) => item.groupRegistrationId === registration.id && item.participationConfirmed).length;
  return confirmed >= Math.ceil(registration.groupSize * event.groupMinPercent / 100);
}

function EventCard({ eventId, reviewExpanded = false }: { eventId: string; reviewExpanded?: boolean }) {
  const { events, joinEvent, addEventGroup, confirmGroupParticipation, isGroupLeader, submitEventReview } = useCampusWorkflow();
  const event = events.find((item) => item.id === eventId);
  const selectedEventId = event?.id;
  const [isLoading, setIsLoading] = useState(false);
  const [selectedGroupId, setSelectedGroupId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [reviewComment, setReviewComment] = useState('');
  const [reviewRating, setReviewRating] = useState('5');
  const [reviewSuccess, setReviewSuccess] = useState(false);
  const [isReviewOpen, setIsReviewOpen] = useState(false);
  const actionLock = useRef(false);
  if (!event) return null;

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

  async function handleAddGroup(group: string, groupSize: number) {
    if (actionLock.current || isLoading || !selectedEventId) return;
    actionLock.current = true;
    setIsLoading(true);
    setError('');
    try {
      await addEventGroup(selectedEventId, group, groupSize, 'Александра Ким · староста');
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Не удалось добавить группу.');
    } finally {
      actionLock.current = false;
      setIsLoading(false);
    }
  }

  const currentStudentParticipant = event.participants.find((participant) => participant.studentName === 'Александра Ким');
  const currentStudentEnrolled = currentStudentParticipant !== undefined;
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
  const currentStudentReview = event.reviews.find((review) => review.studentName === 'Александра Ким');

  async function handleSubmitReview(formEvent: FormEvent<HTMLFormElement>) {
    formEvent.preventDefault();
    if (actionLock.current || isLoading || !selectedEventId) return;
    actionLock.current = true;
    setIsLoading(true);
    setError('');
    setReviewSuccess(false);
    try {
      await submitEventReview(selectedEventId, Number(reviewRating), reviewComment);
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
            <Typography variant="body2"><strong>Мест</strong> · {event.capacity} участников · организатор: {event.createdBy}</Typography>
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
          {isGroupLeader && !event.groupRegistrations.some((registration) => registration.group === 'ИС-24') && (
            <Button className="campus-event-action" variant="outlined" disabled={isLoading} onClick={() => void handleAddGroup('ИС-24', 24)}>
              {isLoading ? <CircularProgress size={18} /> : 'Записать группу ИС-24'}
            </Button>
          )}
          {event.groupRegistrations.map((registration) => {
            const joined = event.participants.filter((participant) => participant.groupRegistrationId === registration.id && participant.participationConfirmed).length;
            const needed = Math.ceil(registration.groupSize * (event.groupMinPercent ?? 0) / 100);
            const thresholdReached = joined >= needed;
            const memberJoined = event.participants.some((participant) => participant.groupRegistrationId === registration.id && participant.studentName === 'Александра Ким');
            return (
              <Box className="event-group-registration" key={registration.id}>
                <Typography variant="body2" fontWeight={650}>{registration.group} · {joined} из {registration.groupSize} подтвердили участие</Typography>
                <Typography variant="caption" color="text.secondary">Порог: {formatNounCount(needed, 'участник', 'участника', 'участников')} · {thresholdReached ? 'группа допущена' : 'пока не набран'}</Typography>
                {!memberJoined && registration.group === 'ИС-24' && (
                  <Box className="event-confirm-action">
                    <Typography variant="caption" color="text.secondary">Ваш ответ влияет на допуск всей группы к мероприятию</Typography>
                    <Button variant="contained" disabled={isLoading} onClick={() => { setSelectedGroupId(registration.id); void handleJoin(registration.id); }}>
                      {isLoading && selectedGroupId === registration.id ? <CircularProgress size={18} color="inherit" /> : 'Я участвую'}
                    </Button>
                  </Box>
                )}
                {memberJoined && <Chip size="small" color="success" label="Участие подтверждено" />}
              </Box>
            );
          })}
        </Box>
        )}
        {currentStudentEnrolled && Date.parse(event.endsAt) <= Date.now() && event.format === 'group' && !currentStudentAttended && (
          <Typography variant="caption" display="block" mt={1.5} color="error.main">Группа не набрала минимальное число участников · начисление баллов отменено</Typography>
        )}
        {currentStudentEnrolled && Date.parse(event.endsAt) > Date.now() && (
          <Typography variant="caption" display="block" mt={1.5} color="success.main">Вы записаны · создатель начислит баллы после завершения мероприятия</Typography>
        )}
        {!currentStudentEnrolled && Date.parse(event.endsAt) <= Date.now() && <Chip size="small" sx={{ mt: 1.5 }} label="Мероприятие завершено" />}
        {currentStudentAttended && Date.parse(event.endsAt) <= Date.now() && (
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
                <TextField select size="small" label="Оценка" value={reviewRating} onChange={(inputEvent) => setReviewRating(inputEvent.target.value)} fullWidth>
                  {[5, 4, 3, 2, 1].map((rating) => <MenuItem key={rating} value={rating}>{rating}</MenuItem>)}
                </TextField>
                <TextField label="Комментарий" value={reviewComment} onChange={(inputEvent) => setReviewComment(inputEvent.target.value)} inputProps={{ maxLength: 1000 }} helperText="От 20 символов · отзыв проверит создатель мероприятия" multiline minRows={2} required fullWidth />
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
    </Card>
  );
}

export function EventsPage() {
  const { events } = useCampusWorkflow();
  const [reviewMode, setReviewMode] = useState(false);
  const pendingReviewEvents = events.filter(canStudentReview);
  const visibleEvents = reviewMode ? pendingReviewEvents : events;
  return (
    <Box className="campus-page">
      <PageHeader title="Мероприятия">
        <Typography color="text.secondary">{formatNounCount(events.filter((event) => event.participants.some((participant) => participant.studentName === 'Александра Ким')).length, 'мероприятие', 'мероприятия', 'мероприятий')} в вашем списке</Typography>
      </PageHeader>
      {pendingReviewEvents.length > 0 && (
        <Button
          variant={reviewMode ? 'contained' : 'outlined'}
          className="pending-reviews-button"
          onClick={() => setReviewMode((current) => !current)}
        >
          {formatNounCount(pendingReviewEvents.length, 'мероприятие ожидает', 'мероприятия ожидают', 'мероприятий ожидают')} вашего отзыва
          <Typography component="span" variant="caption" ml={1}>{reviewMode ? 'Показать все' : ''}</Typography>
        </Button>
      )}
      {reviewMode && pendingReviewEvents.length === 0 && <Alert severity="info">Мероприятий, ожидающих отзыва, нет.</Alert>}
      <Box className="campus-event-list">
        {visibleEvents.map((event) => (
          <EventCard key={event.id} eventId={event.id} reviewExpanded={reviewMode && canStudentReview(event)} />
        ))}
        {visibleEvents.length === 0 && reviewMode && <Typography color="text.secondary">Нет мероприятий для отображения.</Typography>}
      </Box>
    </Box>
  );
}

export function GroupConfirmationPage() {
  const { groupConfirmations, decideGroupConfirmation, isGroupLeader } = useCampusWorkflow();
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const actionLock = useRef(false);
  const ownGroupConfirmations = groupConfirmations.filter((item) => item.group === 'ИС-24');
  const pending = ownGroupConfirmations.filter((item) => item.status === 'pending');
  const processed = ownGroupConfirmations.filter((item) => item.status !== 'pending');

  async function decide(id: string, approve: boolean) {
    if (actionLock.current || busyId) return;
    actionLock.current = true;
    setBusyId(id);
    setError('');
    try {
      await decideGroupConfirmation(id, approve, { role: 'group-leader', group: 'ИС-24' });
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
        <Typography color="text.secondary">Проверяйте и подтверждайте заявки на вступление в группу ИС-24</Typography>
      </PageHeader>
      {!isGroupLeader && <Alert severity="info">Подтверждение заявок доступно только студенту, назначенному старостой преподавателем.</Alert>}
      {isGroupLeader && error && <Alert severity="error">{error}</Alert>}
      {isGroupLeader && (
        <>
          <Card className="group-leader-summary">
            <CardContent>
              <Box><Typography variant="overline">ВАША ГРУППА</Typography><Typography variant="h5" fontWeight={750}>ИС-24</Typography></Box>
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
        </>
      )}
    </Box>
  );
}

export function TeacherStudentsPage() {
  const { students, teacherGroups, groupLeaders, groupConfirmations, decideGroupConfirmation, assignGroupLeader } = useCampusWorkflow();
  const [selectedGroup, setSelectedGroup] = useState<string>(teacherGroups[0]);
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
      <Box className="teacher-student-toolbar">
        <TextField select label="Учебная группа" value={selectedGroup} onChange={(event) => setSelectedGroup(event.target.value)} size="small">
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
  const { events, createEvent, addEventGroup, awardEventPoints, moderateEventReview, teacherGroups, activityAwards } = useCampusWorkflow();
  const location = useLocation();
  const isTeacherPage = location.pathname.startsWith('/teacher/');
  const creator = isTeacherPage ? 'Мария Иванова' : 'Комитет студенческих инициатив';
  const ownedEvents = events.filter((event) => event.createdBy === creator);
  const groupEvents = events.filter((event) => event.format === 'group' && Date.parse(event.date) > Date.now());
  const creatorAwards = activityAwards.filter((award) => award.awardedBy === creator);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('Обучение');
  const [locationText, setLocationText] = useState('');
  const [startsAt, setStartsAt] = useState('2026-11-25T10:00');
  const [endsAt, setEndsAt] = useState('2026-11-25T15:00');
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
        date: new Date(startsAt).toISOString(),
        endsAt: new Date(endsAt).toISOString(),
        capacity: Number(capacity),
        format,
        groupMinPercent: format === 'group' ? Number(groupMinPercent) : null,
        activityType,
        maxPoints: Number(maxPoints),
      }, creator);
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

  async function handleReviewModeration(event: CampusEvent, reviewId: string, approve: boolean) {
    if (actionLock.current) return;
    actionLock.current = true;
    const key = `review:${event.id}:${reviewId}`;
    setBusyAction(key);
    setError('');
    setSuccess('');
    try {
      await moderateEventReview(event.id, reviewId, approve, creator);
      setSuccess(approve ? 'Отзыв одобрен, студенту начислено 5 единиц валюты.' : 'Отзыв отклонён без начисления валюты.');
    } catch (moderationError) {
      setError(moderationError instanceof Error ? moderationError.message : 'Не удалось обработать отзыв.');
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
            <TextField label="Баллы при 100% активности" type="number" value={maxPoints} onChange={(event) => setMaxPoints(event.target.value)} inputProps={{ min: 0, max: 100, step: 1 }} helperText="Максимум за мероприятие; начисление участнику рассчитывается по указанному проценту активности" required fullWidth />
          </Box>
          <Button type="submit" variant="contained" disabled={isCreating} startIcon={isCreating ? <CircularProgress size={18} color="inherit" /> : <EventAvailableRoundedIcon />}>
            {isCreating ? 'Создаём…' : 'Создать мероприятие'}
          </Button>
        </Box>
      </SectionCard>
      {creator === 'Мария Иванова' && groupEvents.length > 0 && (
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
                    {event.reviews.filter((review) => review.status === 'pending').map((review) => {
                      const reviewActionKey = `review:${event.id}:${review.id}`;
                      return (
                        <Box className="event-review-moderation" key={review.id}>
                          <Typography fontWeight={650}>{review.studentName} · оценка {review.rating}/5</Typography>
                          <Typography variant="body2" color="text.secondary">{review.comment}</Typography>
                          <Stack direction="row" flexWrap="wrap" gap={1} mt={1}>
                            <Button size="small" variant="contained" disabled={busyAction !== null} onClick={() => void handleReviewModeration(event, review.id, true)}>
                              {busyAction === reviewActionKey ? <CircularProgress size={16} color="inherit" /> : 'Одобрить · +5'}
                            </Button>
                            <Button size="small" variant="outlined" color="inherit" disabled={busyAction !== null} onClick={() => void handleReviewModeration(event, review.id, false)}>Отклонить</Button>
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
  const { walletBalance, transactions } = useCampusWorkflow();
  return (
    <Box className="campus-page">
      <PageHeader title="Кошелёк">
      </PageHeader>
      <Card className="wallet-balance-card">
        <CardContent>
          <AccountBalanceWalletRoundedIcon />
          <Typography variant="body2" mt={1} sx={{ opacity: 0.8 }}>Доступный баланс</Typography>
          <Typography variant="h3" fontWeight={750}>{walletBalance.toLocaleString('ru-RU')}</Typography>
          <Typography variant="body2" sx={{ opacity: 0.8 }}>жетонов кампуса</Typography>
        </CardContent>
      </Card>
      <SectionCard title="История операций" icon={<AccountBalanceWalletRoundedIcon />}>
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
              <Typography fontWeight={700} color={transaction.amount < 0 ? 'text.primary' : 'success.main'}>{transaction.amount > 0 ? '+' : ''}{transaction.amount}</Typography>
            </Stack>
          ))}
        </Stack>
      </SectionCard>
    </Box>
  );
}

const premiumPassPrice = 500;

function InventoryCard({ item, onOpen, isOpening, canAfford }: { item: InventoryItem; onOpen?: (item: InventoryItem) => void; isOpening?: boolean; canAfford: boolean }) {
  return (
    <Card className="inventory-item">
      <CardContent>
        <Avatar className={`inventory-item-icon${item.kind === 'case' ? ' inventory-case-icon' : ''}`}><CardGiftcardRoundedIcon /></Avatar>
        {item.kind === 'case' ? (
          <Chip size="small" label="Не открыт" className="rarity-chip subtle-chip" />
        ) : item.rarity ? (
          <Chip size="small" label={item.rarity} className={`rarity-chip rarity-${item.rarity === 'Эпический' ? 'epic' : item.rarity === 'Редкий' ? 'rare' : 'common'}`} />
        ) : null}
        <Typography fontWeight={700} mt={1}>{item.name}</Typography>
        <Typography variant="caption" color="text.secondary">{item.source}</Typography>
        {item.kind === 'case' && onOpen && (
          <>
            <Button fullWidth variant="contained" sx={{ mt: 1.5 }} disabled={isOpening || !canAfford} onClick={() => onOpen(item)}>
              {isOpening ? <CircularProgress size={18} color="inherit" /> : `Открыть · ${caseOpeningPrice} жетонов`}
            </Button>
            {!canAfford && <Typography variant="caption" color="error">Недостаточно жетонов</Typography>}
          </>
        )}
      </CardContent>
    </Card>
  );
}

export function BattlePassPage() {
  const { walletBalance, academicRating, passExperience, premiumPassOwned, passRewards, purchasePremiumPass, claimPassReward } = useCampusWorkflow();
  const [busyAction, setBusyAction] = useState<string | null>(null);
  const [error, setError] = useState('');
  const actionLock = useRef(false);
  const currentLevel = Math.min(50, Math.floor(passExperience / 20));
  const progressInLevel = currentLevel >= 50 ? 20 : passExperience % 20;
  const needed = currentLevel >= 50 ? 0 : 20 - progressInLevel;
  const progressPercent = currentLevel >= 50 ? 100 : (progressInLevel / 20) * 100;

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
      <PageHeader title="Батл-пасс">
      </PageHeader>
      <Card className="battle-pass-hero">
        <CardContent>
          <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} gap={2}>
            <Box>
              <Typography variant="overline">СЕЗОН 2026/27</Typography>
              <Typography variant="h3" fontWeight={750}>Уровень {currentLevel} / 50</Typography>
              <Typography>{currentLevel >= 50 ? 'Максимальный уровень сезона' : `Ещё ${needed} АР до следующего уровня`}</Typography>
            </Box>
            <Box className="battle-pass-progress">
              <Stack direction="row" justifyContent="space-between"><Typography variant="caption">Академический рейтинг</Typography><Typography variant="caption">{academicRating} АР · {progressInLevel} / 20</Typography></Stack>
              <LinearProgress variant="determinate" value={progressPercent} />
            </Box>
          </Stack>
        </CardContent>
      </Card>
      {error && <Alert severity="error">{error}</Alert>}
      <SectionCard title="Уровни сезона" icon={<CardGiftcardRoundedIcon />}>
        <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} justifyContent="space-between" gap={1.5} mb={2}>
          {premiumPassOwned ? <Chip size="small" color="success" label="Премиальный пропуск активен" /> : (
            <Button variant="contained" disabled={busyAction !== null || walletBalance < premiumPassPrice} onClick={() => void handlePurchase()}>
              {busyAction === 'purchase' ? <CircularProgress size={18} color="inherit" /> : `Купить премиум · ${premiumPassPrice} жетонов`}
            </Button>
          )}
        </Stack>
        <Box className="battle-pass-layout" aria-label="Все 50 уровней батл-пасса">
          <Box className="battle-pass-legend" aria-hidden="true">
            <Box className="battle-pass-legend-spacer" />
            <Box className="battle-pass-legend-lane battle-pass-legend-free">
              <Typography variant="caption" fontWeight={750}>Бесплатно</Typography>
            </Box>
            <Box className="battle-pass-legend-lane battle-pass-legend-premium">
              <Typography variant="caption" fontWeight={750}>Премиум</Typography>
            </Box>
          </Box>
          <Box className="battle-pass-levels">
            {Array.from({ length: 50 }, (_, index) => {
              const level = index + 1;
              const freeReward = passRewards.find((reward) => reward.track === 'free' && reward.level === level);
              const premiumReward = passRewards.find((reward) => reward.track === 'premium' && reward.level === level);
              return (
                <Box className={`battle-pass-level${level <= currentLevel ? ' battle-pass-level-current' : ''}`} key={level}>
                  <Typography variant="caption" fontWeight={750}>Ур. {level}</Typography>
                  {([
                    { reward: freeReward, track: 'free' as const },
                    { reward: premiumReward, track: 'premium' as const },
                  ]).map(({ reward, track }) => (
                    <Box className={`battle-pass-reward battle-pass-reward-${track}${reward ? '' : ' battle-pass-reward-empty'}`} key={track}>
                      {reward && (
                        <>
                          <Typography className="battle-pass-reward-title" variant="caption" title={reward.title}>{reward.title}</Typography>
                          {reward.claimed ? <Chip size="small" label="Получено" color="success" /> :
                            reward.level <= currentLevel && (track === 'free' || premiumPassOwned) ? (
                              <Button size="small" disabled={busyAction !== null} onClick={() => void handleClaim(reward)}>
                                {busyAction === reward.id ? <CircularProgress size={16} /> : 'Забрать'}
                              </Button>
                            ) : <Typography variant="caption" color="text.secondary">{reward.level > currentLevel ? 'Закрыто' : 'Нужен пропуск'}</Typography>}
                        </>
                      )}
                    </Box>
                  ))}
                </Box>
              );
            })}
          </Box>
        </Box>
      </SectionCard>
      <Button component={Link} to="/student/inventory" variant="outlined" startIcon={<WorkspacePremiumRoundedIcon />}>Открыть инвентарь</Button>
    </Box>
  );
}

export function InventoryPage() {
  const { inventory, openCase, walletBalance } = useCampusWorkflow();
  const [openingId, setOpeningId] = useState<string | null>(null);
  const [openedItem, setOpenedItem] = useState<InventoryItem | null>(null);
  const [rouletteItems, setRouletteItems] = useState<Array<{ name: string; rarity: InventoryItem['rarity']; isPrize: boolean }>>([]);
  const [rouletteOffset, setRouletteOffset] = useState(0);
  const [rouletteStopped, setRouletteStopped] = useState(false);
  const [error, setError] = useState('');
  const [isRevealing, setIsRevealing] = useState(false);
  const rouletteViewportRef = useRef<HTMLDivElement>(null);
  const actionLock = useRef(false);

  async function handleOpenCase(item: InventoryItem) {
    if (actionLock.current) return;
    actionLock.current = true;
    setOpeningId(item.id);
    setOpenedItem(null);
    setRouletteItems([]);
    setRouletteStopped(false);
    setError('');
    setIsRevealing(true);
    try {
      const reward = await openCase(item.id);
      const reelPool = [
        { name: 'Значок «Первая пара»', rarity: 'Обычный' as const },
        { name: 'Аватар «Неоновый пиксель»', rarity: 'Редкий' as const },
        { name: 'Фон профиля «Ночной город»', rarity: 'Эпический' as const },
      ];
      const reel = Array.from({ length: 20 }, (_, index) => {
        const prize = reelPool[(index * 7 + 1) % reelPool.length];
        return { ...prize, isPrize: false };
      });
      setRouletteItems([...reel, { name: reward.name, rarity: reward.rarity, isPrize: true }]);
      await new Promise<void>((resolve) => window.requestAnimationFrame(() => window.requestAnimationFrame(() => resolve())));
      const viewportWidth = rouletteViewportRef.current?.clientWidth ?? 540;
      const prizeIndex = reel.length;
      const prizeCenter = 12 + prizeIndex * 152 + 70;
      setRouletteOffset(viewportWidth / 2 - prizeCenter);
      if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        await new Promise<void>((resolve) => window.setTimeout(resolve, 5400));
      }
      setRouletteStopped(true);
      setOpenedItem(reward);
    } catch (openError) {
      setError(openError instanceof Error ? openError.message : 'Не удалось открыть кейс.');
    } finally {
      setIsRevealing(false);
      setOpeningId(null);
      actionLock.current = false;
    }
  }

  return (
    <Box className="campus-page">
      <PageHeader title="Инвентарь">
        <Typography color="text.secondary">{formatNounCount(inventory.length, 'предмет', 'предмета', 'предметов')} · {walletBalance.toLocaleString('ru-RU')} жетонов</Typography>
      </PageHeader>
      {error && <Alert severity="error">{error}</Alert>}
      {openedItem && (
        <Card className="case-result-card">
          <Avatar className="case-result-icon"><CardGiftcardRoundedIcon /></Avatar>
          <Box>
            <Typography variant="overline">КЕЙС ОТКРЫТ</Typography>
            <Typography variant="h6" fontWeight={750}>{openedItem.name}</Typography>
            <Typography color="text.secondary">{openedItem.rarity} · Предмет уже добавлен в инвентарь</Typography>
          </Box>
        </Card>
      )}
      {isRevealing && (
        <Box className="case-reveal" role="status" aria-live="polite">
          <Box className="case-reveal-glow" />
          <Typography variant="h6">Кейс открывается…</Typography>
          <Box className={`case-roulette${rouletteItems.length ? ' case-roulette-ready' : ''}${rouletteStopped ? ' case-roulette-stopped' : ''}`} ref={rouletteViewportRef}>
            <Box className="case-roulette-marker" />
            <Box className="case-roulette-track" style={{ transform: `translateX(${rouletteOffset}px)` }}>
              {rouletteItems.map((entry, index) => (
                <Box className={`case-roulette-item${entry.isPrize ? ' case-roulette-prize' : ''}`} key={`${index}:${entry.name}`}>
                  {entry.isPrize && !rouletteStopped ? (
                    <>
                      <Avatar className="case-roulette-icon case-roulette-mystery">?</Avatar>
                      <Typography variant="caption" fontWeight={650}>Награда</Typography>
                      <Typography variant="caption" color="text.secondary">Сюрприз</Typography>
                    </>
                  ) : (
                    <>
                      <Avatar className={`case-roulette-icon rarity-${entry.rarity?.toLowerCase() ?? 'common'}`}><CardGiftcardRoundedIcon /></Avatar>
                      <Typography variant="caption" fontWeight={650}>{entry.name}</Typography>
                      <Typography variant="caption" color="text.secondary">{entry.rarity}</Typography>
                    </>
                  )}
                </Box>
              ))}
            </Box>
          </Box>
          {!rouletteItems.length && <LinearProgress />}
        </Box>
      )}
      {inventory.length === 0 ? (
        <SectionCard title="Пока пусто" icon={<CardGiftcardRoundedIcon />}>
          <Typography color="text.secondary">Кейсы и косметические награды появятся здесь после получения уровней батл-пасса.</Typography>
        </SectionCard>
      ) : <Box className="inventory-grid">{inventory.map((item) => <InventoryCard key={item.id} item={item} onOpen={handleOpenCase} isOpening={openingId === item.id} canAfford={walletBalance >= caseOpeningPrice} />)}</Box>}
    </Box>
  );
}

const requestStatusActions: Partial<Record<CertificateRequest['status'], Array<{ status: CertificateRequest['status']; label: string; color: 'primary' | 'success' | 'error' }>>> = {
  Подана: [
    { status: 'В обработке', label: 'Взять в работу', color: 'primary' },
    { status: 'Отклонена', label: 'Отклонить', color: 'error' },
  ],
  'В обработке': [
    { status: 'Готова', label: 'Отметить готовой', color: 'success' },
    { status: 'Отклонена', label: 'Отклонить', color: 'error' },
  ],
  Готова: [{ status: 'Выдана', label: 'Отметить выданной', color: 'success' }],
};

export function CertificateManagementPage() {
  const { certificateRequests, updateCertificateStatus } = useCampusWorkflow();
  const [busyId, setBusyId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const actionLock = useRef(false);
  const pendingRequests = certificateRequests.filter((request) => ['Подана', 'В обработке'].includes(request.status));
  const completedRequests = certificateRequests.filter((request) => !['Подана', 'В обработке'].includes(request.status));

  async function handleStatusUpdate(request: CertificateRequest, status: CertificateRequest['status']) {
    if (actionLock.current) return;
    actionLock.current = true;
    setBusyId(request.id);
    setError('');
    try {
      await updateCertificateStatus(request.id, status);
    } catch (updateError) {
      setError(updateError instanceof Error ? updateError.message : 'Не удалось изменить статус заявки.');
    } finally {
      actionLock.current = false;
      setBusyId(null);
    }
  }

  function renderRequest(request: CertificateRequest) {
    return (
      <Card className="office-request-card" key={request.id} variant="outlined">
        <CardContent>
          <Box className="office-request-heading">
            <Box>
              <Typography variant="h6" fontSize={16} fontWeight={750}>{request.type}</Typography>
              <Typography variant="body2" color="text.secondary">
                {request.studentName} · {request.group} · {request.deliveryMethod}
              </Typography>
            </Box>
            <Chip size="small" label={request.status} className={`certificate-status certificate-status-${request.status}`} />
          </Box>
          {request.comment && <Typography className="office-request-comment">{request.comment}</Typography>}
          <Typography variant="caption" color="text.secondary">Подана {formatDate(request.createdAt)}</Typography>
          {(requestStatusActions[request.status] ?? []).length > 0 && (
            <Stack direction={{ xs: 'column', sm: 'row' }} gap={1} className="office-request-actions">
              {(requestStatusActions[request.status] ?? []).map((action) => (
                <Button
                  key={action.status}
                  variant={action.color === 'error' ? 'outlined' : 'contained'}
                  color={action.color}
                  disabled={busyId !== null}
                  onClick={() => void handleStatusUpdate(request, action.status)}
                >
                  {busyId === request.id ? <CircularProgress size={18} color="inherit" /> : action.label}
                </Button>
              ))}
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
        <Card><CardContent><Typography variant="caption" color="text.secondary">Новые</Typography><Typography variant="h4" fontWeight={750}>{certificateRequests.filter((item) => item.status === 'Подана').length}</Typography></CardContent></Card>
        <Card><CardContent><Typography variant="caption" color="text.secondary">В работе</Typography><Typography variant="h4" fontWeight={750}>{certificateRequests.filter((item) => item.status === 'В обработке').length}</Typography></CardContent></Card>
        <Card><CardContent><Typography variant="caption" color="text.secondary">Готовы к выдаче</Typography><Typography variant="h4" fontWeight={750}>{certificateRequests.filter((item) => item.status === 'Готова').length}</Typography></CardContent></Card>
      </Box>
      {error && <Alert severity="error">{error}</Alert>}
      <SectionCard title="Требуют обработки" icon={<WorkspacePremiumRoundedIcon />}>
        {pendingRequests.length ? <Stack spacing={1.5}>{pendingRequests.map(renderRequest)}</Stack> : <Typography color="text.secondary">Активных заявок нет.</Typography>}
      </SectionCard>
      <SectionCard title="История заявок" icon={<CheckRoundedIcon />}>
        {completedRequests.length ? <Stack spacing={1.5}>{completedRequests.map(renderRequest)}</Stack> : <Typography color="text.secondary">Завершённые заявки появятся здесь.</Typography>}
      </SectionCard>
    </Box>
  );
}

const weekdays = ['Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота'];

export function ScheduleManagementPage() {
  const { scheduleEntries, createScheduleEntry } = useCampusWorkflow();
  const [weekday, setWeekday] = useState('Понедельник');
  const [startTime, setStartTime] = useState('09:00');
  const [endTime, setEndTime] = useState('10:30');
  const [group, setGroup] = useState('ИС-24');
  const [subject, setSubject] = useState('');
  const [teacher, setTeacher] = useState('');
  const [room, setRoom] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const actionLock = useRef(false);

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (actionLock.current) return;
    actionLock.current = true;
    setIsLoading(true);
    setError('');
    setSuccess('');
    try {
      await createScheduleEntry({ weekday, startTime, endTime, group, subject, teacher, room });
      setSuccess('Занятие добавлено в расписание.');
      setSubject('');
      setTeacher('');
      setRoom('');
    } catch (createError) {
      setError(createError instanceof Error ? createError.message : 'Не удалось добавить занятие.');
    } finally {
      actionLock.current = false;
      setIsLoading(false);
    }
  }

  return (
    <Box className="campus-page">
      <PageHeader title="Расписание занятий">
      </PageHeader>
      <SectionCard title="Добавить занятие" icon={<CalendarMonthRoundedIcon />}>
        <Box component="form" onSubmit={handleCreate} className="schedule-entry-form">
          <TextField select label="День недели" value={weekday} onChange={(event) => setWeekday(event.target.value)} required>
            {weekdays.map((day) => <MenuItem key={day} value={day}>{day}</MenuItem>)}
          </TextField>
          <TextField select label="Группа" value={group} onChange={(event) => setGroup(event.target.value)} required>
            {campusGroups.map((campusGroup) => <MenuItem key={campusGroup.name} value={campusGroup.name}>{campusGroup.name}</MenuItem>)}
          </TextField>
          <TextField label="Дисциплина" value={subject} onChange={(event) => setSubject(event.target.value)} required />
          <TextField label="Преподаватель" value={teacher} onChange={(event) => setTeacher(event.target.value)} required />
          <TextField label="Начало" type="time" value={startTime} onChange={(event) => setStartTime(event.target.value)} required InputLabelProps={{ shrink: true }} />
          <TextField label="Окончание" type="time" value={endTime} onChange={(event) => setEndTime(event.target.value)} required InputLabelProps={{ shrink: true }} />
          <TextField label="Аудитория" value={room} onChange={(event) => setRoom(event.target.value)} required />
          {error && <Alert severity="error" className="schedule-form-message">{error}</Alert>}
          {success && <Alert severity="success" className="schedule-form-message">{success}</Alert>}
          <Button type="submit" variant="contained" disabled={isLoading} startIcon={isLoading ? <CircularProgress size={18} color="inherit" /> : <AddRoundedIcon />}>
            {isLoading ? 'Добавляем…' : 'Добавить в расписание'}
          </Button>
        </Box>
      </SectionCard>
      <SectionCard title="Недельное расписание" icon={<ScheduleRoundedIcon />}>
        <Stack spacing={2} className="weekly-schedule">
          {weekdays.map((day) => {
            const dayEntries = scheduleEntries.filter((entry) => entry.weekday === day).sort((a, b) => a.startTime.localeCompare(b.startTime));
            if (!dayEntries.length) return null;
            return (
              <Box className="schedule-day" key={day}>
                <Typography variant="subtitle1" fontWeight={750}>{day}</Typography>
                <Stack spacing={1}>
                  {dayEntries.map((entry: ScheduleEntry) => (
                    <Box className="schedule-entry" key={entry.id}>
                      <Box className="schedule-entry-time"><Typography fontWeight={750}>{entry.startTime}</Typography><Typography variant="caption">до {entry.endTime}</Typography></Box>
                      <Box className="schedule-entry-info"><Typography fontWeight={700}>{entry.subject}</Typography><Typography variant="body2" color="text.secondary">{entry.group} · {entry.teacher}</Typography></Box>
                      <Chip size="small" icon={<PlaceRoundedIcon />} label={entry.room} className="subtle-chip" />
                    </Box>
                  ))}
                </Stack>
              </Box>
            );
          })}
        </Stack>
      </SectionCard>
    </Box>
  );
}

export function CertificateRequestPage() {
  const { certificateRequests, submitCertificateRequest } = useCampusWorkflow();
  const [type, setType] = useState('Справка об обучении');
  const [comment, setComment] = useState('');
  const [deliveryMethod, setDeliveryMethod] = useState<'В учебной части' | 'Электронно'>('В учебной части');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const actionLock = useRef(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (actionLock.current || isLoading) return;
    actionLock.current = true;
    setIsLoading(true);
    setError('');
    setSuccess('');
    try {
      await submitCertificateRequest(type, comment, deliveryMethod);
      setComment('');
      setSuccess('Заявка отправлена в учебную часть.');
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Не удалось подать заявку.');
    } finally {
      actionLock.current = false;
      setIsLoading(false);
    }
  }

  return (
    <Box className="campus-page">
      <PageHeader title="Справки">
        <Typography color="text.secondary">Заявка и статус обработки</Typography>
      </PageHeader>
      <SectionCard title="Подать заявку" icon={<WorkspacePremiumRoundedIcon />}>
        <Box component="form" onSubmit={handleSubmit} className="campus-form">
          <TextField select label="Вид справки" value={type} onChange={(event) => setType(event.target.value)} fullWidth>
            <MenuItem value="Справка об обучении">Справка об обучении</MenuItem>
            <MenuItem value="Справка для общежития">Справка для общежития</MenuItem>
            <MenuItem value="Справка о доходах">Справка о доходах</MenuItem>
            <MenuItem value="Архивная справка">Архивная справка</MenuItem>
          </TextField>
          <TextField label="Комментарий (необязательно)" value={comment} onChange={(event) => setComment(event.target.value)} multiline minRows={2} fullWidth />
          <TextField select label="Способ получения" value={deliveryMethod} onChange={(event) => setDeliveryMethod(event.target.value as typeof deliveryMethod)} fullWidth>
            <MenuItem value="В учебной части">В учебной части</MenuItem>
            <MenuItem value="Электронно">Электронно</MenuItem>
          </TextField>
          {error && <Alert severity="error">{error}</Alert>}
          {success && <Alert severity="success">{success}</Alert>}
          <Button type="submit" variant="contained" disabled={isLoading} startIcon={isLoading ? <CircularProgress size={18} color="inherit" /> : undefined}>
            {isLoading ? 'Отправляем…' : 'Подать заявку'}
          </Button>
        </Box>
      </SectionCard>
      <SectionCard title="Мои заявки" icon={<EventAvailableRoundedIcon />}>
        <Stack divider={<Divider flexItem />} spacing={1.5}>
          {certificateRequests.map((request) => (
            <Stack key={request.id} direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={1}>
              <Box><Typography fontWeight={650}>{request.type}</Typography><Typography variant="caption" color="text.secondary">{request.comment ? `${request.comment} · ` : ''}{request.deliveryMethod} · {formatDate(request.createdAt, false)}</Typography></Box>
              <Chip size="small" label={request.status} className={`certificate-status certificate-status-${request.status}`} />
            </Stack>
          ))}
        </Stack>
      </SectionCard>
    </Box>
  );
}
