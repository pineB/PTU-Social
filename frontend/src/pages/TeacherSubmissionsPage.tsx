import { useRef, useState, type FormEvent } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControlLabel,
  MenuItem,
  Radio,
  RadioGroup,
  Stack,
  Tab,
  Tabs,
  TextField,
  Typography,
} from '@mui/material';
import CheckRoundedIcon from '@mui/icons-material/CheckRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import AttachFileRoundedIcon from '@mui/icons-material/AttachFileRounded';
import VisibilityRoundedIcon from '@mui/icons-material/VisibilityRounded';
import { downloadWorkspaceAttachment } from '../api/workspace';
import { calculateAward, useTaskWorkflow, type ReviewStatus, type StudentSubmission, type TeacherGrade } from '../data/task-workflow';

const reviewStatusLabels: Record<ReviewStatus, string> = {
  in_progress: 'В процессе',
  submitted: 'Сдано',
  accepted: 'Принято',
};

function parseTeacherGrade(value: string): TeacherGrade | '' {
  switch (value) {
    case '2': return 2;
    case '3': return 3;
    case '4': return 4;
    case '5': return 5;
    default: return '';
  }
}

function formatSubmittedAt(value: string): string {
  return new Intl.DateTimeFormat('ru-RU', {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function formatPendingCount(count: number): string {
  const mod100 = count % 100;
  const mod10 = count % 10;
  const singular = !(mod100 >= 11 && mod100 <= 14) && mod10 === 1;
  const word = mod100 >= 11 && mod100 <= 14
    ? 'работ'
    : singular
      ? 'работа'
      : mod10 >= 2 && mod10 <= 4
        ? 'работы'
        : 'работ';
  return `${count} ${word} ${singular ? 'ждёт' : 'ждут'} проверки`;
}

export default function TeacherSubmissionsPage() {
  const {
    assignments,
    submissions,
    teachingAssignments,
    acceptSubmission,
    returnSubmission,
    createAssignment,
    isLoading: isDataLoading,
    error: dataError,
  } = useTaskWorkflow();
  const [filter, setFilter] = useState<'all' | ReviewStatus>('submitted');
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [selectedTeachingAssignment, setSelectedTeachingAssignment] = useState('');
  const [taskTitle, setTaskTitle] = useState('');
  const [taskDescription, setTaskDescription] = useState('');
  const [taskFile, setTaskFile] = useState<File | null>(null);
  const [taskDueAt, setTaskDueAt] = useState('');
  const [taskMaxPoints, setTaskMaxPoints] = useState('100');
  const [allowLateSubmission, setAllowLateSubmission] = useState(false);
  const [isCreatingTask, setIsCreatingTask] = useState(false);
  const [createTaskError, setCreateTaskError] = useState('');
  const [reviewing, setReviewing] = useState<StudentSubmission | null>(null);
  const [returnTarget, setReturnTarget] = useState<StudentSubmission | null>(null);
  const [gradeTarget, setGradeTarget] = useState<StudentSubmission | null>(null);
  const [selectedGrade, setSelectedGrade] = useState<TeacherGrade | ''>('');
  const [returnComment, setReturnComment] = useState('');
  const [acceptComment, setAcceptComment] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [busySubmissionId, setBusySubmissionId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const actionLock = useRef(false);
  const createTaskLock = useRef(false);
  const visibleSubmissions = filter === 'all'
    ? submissions
    : submissions.filter((submission) => submission.status === filter);
  const pendingCount = submissions.filter((submission) => submission.status === 'submitted').length;

  async function handleCreateTask(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (createTaskLock.current || isCreatingTask) return;
    const assignment = teachingAssignments.find((item) =>
      `${item.groupId}:${item.disciplineId}` === selectedTeachingAssignment,
    );
    if (!assignment) {
      setCreateTaskError('Выберите дисциплину и группу.');
      return;
    }

    createTaskLock.current = true;
    setIsCreatingTask(true);
    setCreateTaskError('');
    try {
      await createAssignment({
        groupId: assignment.groupId,
        disciplineId: assignment.disciplineId,
        title: taskTitle.trim(),
        description: taskDescription.trim(),
        dueAt: new Date(taskDueAt).toISOString(),
        maxPoints: Number(taskMaxPoints),
        allowLateSubmission,
      }, taskFile ?? undefined);
      setCreateDialogOpen(false);
      setTaskTitle('');
      setTaskDescription('');
      setTaskFile(null);
      setTaskDueAt('');
      setTaskMaxPoints('100');
      setAllowLateSubmission(false);
    } catch (createError) {
      setCreateTaskError(createError instanceof Error ? createError.message : 'Не удалось создать задание.');
    } finally {
      createTaskLock.current = false;
      setIsCreatingTask(false);
    }
  }

  async function handleAccept(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!gradeTarget || selectedGrade === '' || actionLock.current || isLoading) return;
    const submission = gradeTarget;
    actionLock.current = true;
    setIsLoading(true);
    setBusySubmissionId(submission.id);
    setError('');
    try {
      await acceptSubmission(submission.id, selectedGrade, acceptComment.trim());
      setGradeTarget(null);
      setSelectedGrade('');
      setAcceptComment('');
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : 'Не удалось принять работу.');
    } finally {
      actionLock.current = false;
      setBusySubmissionId(null);
      setIsLoading(false);
    }
  }

  async function handleReturn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!returnTarget || actionLock.current || isLoading || !returnComment.trim()) return;

    actionLock.current = true;
    setIsLoading(true);
    setBusySubmissionId(returnTarget.id);
    setError('');
    try {
      await returnSubmission(returnTarget.id, returnComment.trim());
      setReturnTarget(null);
      setReturnComment('');
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : 'Не удалось вернуть работу.');
    } finally {
      actionLock.current = false;
      setBusySubmissionId(null);
      setIsLoading(false);
    }
  }

  return (
    <Box className="task-page">
      <Box className="task-page-heading">
        <Typography variant="h4" className="welcome-title">Сдачи студентов</Typography>
        <Typography color="text.secondary" mt={0.5}>{formatPendingCount(pendingCount)}</Typography>
        <Button
          variant="contained"
          sx={{ mt: 2, alignSelf: 'flex-start' }}
          disabled={isDataLoading || teachingAssignments.length === 0}
          onClick={() => {
            setSelectedTeachingAssignment(teachingAssignments[0]
              ? `${teachingAssignments[0].groupId}:${teachingAssignments[0].disciplineId}`
              : '');
            setCreateTaskError('');
            setCreateDialogOpen(true);
          }}
        >
          Создать задание
        </Button>
      </Box>

      <Card className="review-table-card">
        <CardContent className="review-table-content">
          {(dataError || error) && <Alert severity="error" sx={{ mb: 2 }}>{error || dataError}</Alert>}
          {isDataLoading && <CircularProgress size={28} aria-label="Загрузка сдач" />}
          {!isDataLoading && !dataError && teachingAssignments.length === 0 && (
            <Alert severity="info" sx={{ mb: 2 }}>Для создания задания преподавателю нужно назначить группу и дисциплину.</Alert>
          )}
          <Tabs value={filter} onChange={(_, value: 'all' | ReviewStatus) => setFilter(value)} variant="scrollable" scrollButtons="auto" aria-label="Фильтр сдач">
            <Tab value="submitted" label={`На проверке${pendingCount ? ` · ${pendingCount}` : ''}`} />
            <Tab value="all" label="Все работы" />
            <Tab value="in_progress" label="На доработке" />
            <Tab value="accepted" label="Принято" />
          </Tabs>
          <Stack spacing={1.5} mt={2}>
            {visibleSubmissions.map((submission) => {
              const isPending = submission.status === 'submitted';
              const busy = busySubmissionId === submission.id;
              const attachmentUrl = submission.attachmentUrl;
              const fileName = submission.fileName;
              return (
                <Card variant="outlined" className="submission-review-card" key={submission.id}>
                  <CardContent>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'flex-start' }} gap={1.5}>
                      <Box minWidth={0}>
                        <Typography fontWeight={750}>{submission.assignmentTitle}</Typography>
                        <Typography variant="body2" color="text.secondary">{submission.subject} · {submission.group}</Typography>
                      </Box>
                      <Chip size="small" label={reviewStatusLabels[submission.status]} className={`assignment-status assignment-status-${submission.status}`} />
                    </Stack>
                    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} gap={1.5} mt={1.5}>
                      <Box minWidth={0}>
                        <Typography fontWeight={650}>{submission.studentName}</Typography>
                        <Typography variant="caption" color="text.secondary">{formatSubmittedAt(submission.submittedAt)}</Typography>
                      </Box>
                      {fileName && attachmentUrl && (
                        <Chip
                          size="small"
                          icon={<AttachFileRoundedIcon />}
                          label={fileName}
                          onClick={() => void downloadWorkspaceAttachment(attachmentUrl, fileName)}
                          className="attached-file-chip submission-review-file"
                        />
                      )}
                      {submission.status === 'accepted' && (
                        <Typography variant="caption" className="awarded-points">
                          {submission.grade && `Оценка ${submission.grade} · `}+{submission.awardedPoints ?? 0} АР
                        </Typography>
                      )}
                    </Stack>
                    <Stack direction="row" flexWrap="wrap" gap={1} mt={1.5}>
                      <Button size="small" variant="outlined" startIcon={<VisibilityRoundedIcon />} onClick={() => setReviewing(submission)} disabled={isLoading}>
                        Посмотреть работу
                      </Button>
                      {isPending && (
                        <>
                          <Button size="small" color="inherit" startIcon={busy && isLoading ? <CircularProgress size={15} /> : <CloseRoundedIcon />} disabled={isLoading} onClick={() => { setReturnComment(''); setReturnTarget(submission); }}>
                            Вернуть на доработку
                          </Button>
                          <Button size="small" variant="contained" startIcon={<CheckRoundedIcon />} disabled={isLoading} onClick={() => { setSelectedGrade(''); setAcceptComment(''); setGradeTarget(submission); }}>
                            Принять
                          </Button>
                        </>
                      )}
                      {submission.status === 'in_progress' && submission.returnComment && (
                        <Typography variant="caption" color="text.secondary" alignSelf="center">Ожидает повторной сдачи</Typography>
                      )}
                    </Stack>
                  </CardContent>
                </Card>
              );
            })}
            {visibleSubmissions.length === 0 && <Typography color="text.secondary" textAlign="center" py={3}>Работ в этой категории пока нет.</Typography>}
          </Stack>
        </CardContent>
      </Card>

      <Dialog open={createDialogOpen} onClose={() => { if (!isCreatingTask) setCreateDialogOpen(false); }} fullWidth maxWidth="sm">
        <Box component="form" onSubmit={handleCreateTask}>
          <DialogTitle>Новое задание</DialogTitle>
          <DialogContent>
            <Stack spacing={2} mt={1}>
              <TextField
                select
                label="Дисциплина и группа"
                value={selectedTeachingAssignment}
                onChange={(event) => setSelectedTeachingAssignment(event.target.value)}
                required
                fullWidth
              >
                {teachingAssignments.map((item) => (
                  <MenuItem key={`${item.groupId}:${item.disciplineId}`} value={`${item.groupId}:${item.disciplineId}`}>
                    {item.disciplineName} · {item.groupName}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                label="Название"
                value={taskTitle}
                onChange={(event) => setTaskTitle(event.target.value)}
                inputProps={{ maxLength: 200 }}
                required
                fullWidth
              />
              <TextField
                label="Описание задания"
                value={taskDescription}
                onChange={(event) => setTaskDescription(event.target.value)}
                multiline
                minRows={3}
                required
                fullWidth
              />
              <Button component="label" variant="outlined" startIcon={<AttachFileRoundedIcon />} disabled={isCreatingTask}>
                {taskFile ? 'Заменить файл задания' : 'Прикрепить файл к заданию'}
                <input
                  type="file"
                  hidden
                  accept=".pdf,.doc,.docx,.zip,.txt,.png,.jpg,.jpeg"
                  onChange={(event) => {
                    const selectedFile = event.target.files?.[0] ?? null;
                    if (selectedFile && selectedFile.size > 20 * 1024 * 1024) {
                      setTaskFile(null);
                      setCreateTaskError('Файл не должен превышать 20 МБ.');
                      event.target.value = '';
                      return;
                    }
                    setCreateTaskError('');
                    setTaskFile(selectedFile);
                  }}
                />
              </Button>
              {taskFile && (
                <Stack direction="row" spacing={1} alignItems="center">
                  <Chip size="small" icon={<AttachFileRoundedIcon />} label={taskFile.name} />
                  <Button size="small" color="error" onClick={() => setTaskFile(null)}>Убрать файл</Button>
                </Stack>
              )}
              <TextField
                label="Срок сдачи"
                type="datetime-local"
                value={taskDueAt}
                onChange={(event) => setTaskDueAt(event.target.value)}
                InputLabelProps={{ shrink: true }}
                required
                fullWidth
              />
              <TextField
                label="Максимум АР"
                type="number"
                inputProps={{ min: 1, max: 100 }}
                value={taskMaxPoints}
                onChange={(event) => setTaskMaxPoints(event.target.value)}
                required
                fullWidth
              />
              <FormControlLabel
                control={<Checkbox checked={allowLateSubmission} onChange={(event) => setAllowLateSubmission(event.target.checked)} />}
                label="Разрешить сдачу после срока"
              />
              {createTaskError && <Alert severity="error">{createTaskError}</Alert>}
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCreateDialogOpen(false)} disabled={isCreatingTask}>Отмена</Button>
            <Button type="submit" variant="contained" disabled={isCreatingTask || !selectedTeachingAssignment}>
              {isCreatingTask ? <CircularProgress size={18} color="inherit" /> : 'Создать задание'}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>

      <Dialog open={reviewing !== null} onClose={() => setReviewing(null)} fullWidth maxWidth="sm">
        <DialogTitle>Работа студента</DialogTitle>
        <DialogContent>
          {reviewing && (
            <Stack spacing={1.5} mt={1}>
              <Typography fontWeight={650}>{reviewing.assignmentTitle}</Typography>
              <Typography variant="body2" color="text.secondary">{reviewing.studentName} · {reviewing.group}</Typography>
              <Typography variant="body2" className="submission-text">{reviewing.workText}</Typography>
              {reviewing.fileName && reviewing.attachmentUrl && (
                <Chip
                  size="small"
                  icon={<AttachFileRoundedIcon />}
                  label={reviewing.fileName}
                  onClick={() => reviewing.attachmentUrl && void downloadWorkspaceAttachment(reviewing.attachmentUrl, reviewing.fileName ?? 'work')}
                  className="attached-file-chip"
                />
              )}
              {reviewing.teacherComment && (
                <Alert severity={reviewing.status === 'accepted' ? 'success' : 'warning'}>
                  <strong>Комментарий преподавателя:</strong> {reviewing.teacherComment}
                </Alert>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions><Button onClick={() => setReviewing(null)}>Закрыть</Button></DialogActions>
      </Dialog>

      <Dialog open={gradeTarget !== null} onClose={() => { if (!isLoading) setGradeTarget(null); }} fullWidth maxWidth="sm">
        <Box component="form" onSubmit={handleAccept}>
          <DialogTitle>Проверка работы</DialogTitle>
          <DialogContent>
            <DialogContentText mb={2} className="grade-submission-summary">
              <strong>{gradeTarget?.studentName}</strong>
              <br />
              {gradeTarget?.assignmentTitle} · {gradeTarget?.group}
            </DialogContentText>
            <RadioGroup
              aria-label="Оценка"
              className="grade-radio-group"
              value={selectedGrade}
              onChange={(event) => setSelectedGrade(parseTeacherGrade(event.target.value))}
            >
              <FormControlLabel value="5" control={<Radio />} label="5 · Отлично" />
              <FormControlLabel value="4" control={<Radio />} label="4 · Хорошо" />
              <FormControlLabel value="3" control={<Radio />} label="3 · Удовлетворительно" />
              <FormControlLabel value="2" control={<Radio />} label="2 · Не зачтено" />
            </RadioGroup>
            <Stack spacing={1.5} mt={1}>
              <TextField
                label="Комментарий к оценке"
                value={acceptComment}
                onChange={(event) => setAcceptComment(event.target.value)}
                multiline
                minRows={2}
                fullWidth
              />
            </Stack>
            {gradeTarget && selectedGrade !== '' && (() => {
              const assignment = assignments.find((item) => item.id === gradeTarget.assignmentId);
              return assignment ? (
                <Typography variant="body2" color="text.secondary" mt={2}>
                  Будет начислено {calculateAward(assignment, gradeTarget, selectedGrade)} АР
                  {Date.parse(gradeTarget.submittedAt) > Date.parse(assignment.dueAt) && ' с учётом срока сдачи'}.
                </Typography>
              ) : null;
            })()}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setGradeTarget(null)} disabled={isLoading}>Отмена</Button>
            <Button type="submit" variant="contained" disabled={isLoading || selectedGrade === ''} startIcon={isLoading ? <CircularProgress size={16} color="inherit" /> : <CheckRoundedIcon />}>
              {isLoading ? 'Принимаем…' : 'Подтвердить'}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>

      <Dialog open={returnTarget !== null} onClose={() => { if (!isLoading) setReturnTarget(null); }} fullWidth maxWidth="sm">
        <Box component="form" onSubmit={handleReturn}>
          <DialogTitle>Вернуть на доработку</DialogTitle>
          <DialogContent>
            <DialogContentText mb={2}>Напишите, что нужно исправить. Студент увидит комментарий и сможет отправить работу повторно.</DialogContentText>
            <TextField
              label="Комментарий для студента"
              value={returnComment}
              onChange={(event) => setReturnComment(event.target.value)}
              multiline
              minRows={3}
              required
              fullWidth
              autoFocus
            />
            <Box mt={2}></Box>
            {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setReturnTarget(null)} disabled={isLoading}>Отмена</Button>
            <Button type="submit" variant="contained" disabled={isLoading || !returnComment.trim()} startIcon={isLoading ? <CircularProgress size={16} color="inherit" /> : undefined}>
              {isLoading ? 'Возвращаем…' : 'Вернуть работу'}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>
    </Box>
  );
}
