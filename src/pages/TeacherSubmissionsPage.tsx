import { useRef, useState, type FormEvent } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControlLabel,
  IconButton,
  Radio,
  RadioGroup,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import CheckRoundedIcon from '@mui/icons-material/CheckRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import AttachFileRoundedIcon from '@mui/icons-material/AttachFileRounded';
import VisibilityRoundedIcon from '@mui/icons-material/VisibilityRounded';
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

function FeedbackAttachmentInput({ file, onChange, disabled }: { file: File | null; onChange: (file: File | null) => void; disabled: boolean }) {
  return (
    <Button component="label" variant="outlined" startIcon={<AttachFileRoundedIcon />} disabled={disabled} className="feedback-file-button">
      {file?.name ?? 'Прикрепить файл к комментарию'}
      <input
        hidden
        type="file"
        accept=".pdf,.doc,.docx,.zip,.txt,.png,.jpg,.jpeg"
        onChange={(event) => onChange(event.target.files?.[0] ?? null)}
      />
    </Button>
  );
}

export default function TeacherSubmissionsPage() {
  const { assignments, submissions, acceptSubmission, returnSubmission } = useTaskWorkflow();
  const [filter, setFilter] = useState<'all' | ReviewStatus>('submitted');
  const [reviewing, setReviewing] = useState<StudentSubmission | null>(null);
  const [returnTarget, setReturnTarget] = useState<StudentSubmission | null>(null);
  const [gradeTarget, setGradeTarget] = useState<StudentSubmission | null>(null);
  const [selectedGrade, setSelectedGrade] = useState<TeacherGrade | ''>('');
  const [returnComment, setReturnComment] = useState('');
  const [acceptComment, setAcceptComment] = useState('');
  const [returnFile, setReturnFile] = useState<File | null>(null);
  const [acceptFile, setAcceptFile] = useState<File | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [busySubmissionId, setBusySubmissionId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const actionLock = useRef(false);
  const visibleSubmissions = filter === 'all'
    ? submissions
    : submissions.filter((submission) => submission.status === filter);
  const pendingCount = submissions.filter((submission) => submission.status === 'submitted').length;

  async function handleAccept(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!gradeTarget || selectedGrade === '' || actionLock.current || isLoading) return;
    const submission = gradeTarget;
    actionLock.current = true;
    setIsLoading(true);
    setBusySubmissionId(submission.id);
    setError('');
    try {
      await acceptSubmission(submission.id, selectedGrade, acceptComment.trim(), acceptFile?.name);
      setGradeTarget(null);
      setSelectedGrade('');
      setAcceptComment('');
      setAcceptFile(null);
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
      await returnSubmission(returnTarget.id, returnComment.trim(), returnFile?.name);
      setReturnTarget(null);
      setReturnComment('');
      setReturnFile(null);
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
      </Box>

      <Card className="review-table-card">
        <CardContent className="review-table-content">
          <Stack direction="row" spacing={1} className="task-filters">
            {([
              ['submitted', 'На проверке'],
              ['all', 'Все работы'],
              ['in_progress', 'В процессе'],
              ['accepted', 'Принято'],
            ] as const).map(([value, label]) => (
              <Button key={value} size="small" variant={filter === value ? 'contained' : 'outlined'} color={filter === value ? 'primary' : 'inherit'} onClick={() => setFilter(value)}>
                {label}
              </Button>
            ))}
          </Stack>

          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

          <TableContainer>
            <Table aria-label="Сдачи студентов">
              <TableHead>
                <TableRow>
                  <TableCell>Студент</TableCell>
                  <TableCell>Задание</TableCell>
                  <TableCell>Файл</TableCell>
                  <TableCell>Дата сдачи</TableCell>
                  <TableCell>Статус</TableCell>
                  <TableCell align="right">Действия</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {visibleSubmissions.map((submission) => {
                  const isPending = submission.status === 'submitted';
                  const busy = busySubmissionId === submission.id;
                  return (
                    <TableRow hover key={submission.id}>
                      <TableCell>
                        <Typography fontSize={13} fontWeight={650}>{submission.studentName}</Typography>
                        <Typography variant="caption" color="text.secondary">{submission.group}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography fontSize={13} fontWeight={600}>{submission.assignmentTitle}</Typography>
                        <Typography variant="caption" color="text.secondary">{submission.subject}</Typography>
                      </TableCell>
                      <TableCell className="submission-file-cell">
                        {submission.fileName
                          ? <Chip size="small" icon={<AttachFileRoundedIcon />} label={submission.fileName} className="attached-file-chip" />
                          : <Typography variant="caption" color="text.secondary">Без файла</Typography>}
                      </TableCell>
                      <TableCell>{formatSubmittedAt(submission.submittedAt)}</TableCell>
                      <TableCell className="submission-status-cell">
                        <Stack direction="row" alignItems="center" spacing={1} className="submission-status-content">
                          <Chip size="small" label={reviewStatusLabels[submission.status]} className={`assignment-status assignment-status-${submission.status}`} />
                          {submission.status === 'accepted' && (
                            <Typography variant="caption" className="awarded-points">
                              {submission.grade && `Оценка ${submission.grade} · `}+{submission.awardedPoints ?? 0} АР
                            </Typography>
                          )}
                        </Stack>
                      </TableCell>
                      <TableCell align="right">
                        <Stack direction="row" justifyContent="flex-end" spacing={0.5}>
                          <Tooltip title="Посмотреть работу">
                            <span>
                              <IconButton aria-label={`Посмотреть работу: ${submission.studentName}`} onClick={() => setReviewing(submission)} disabled={isLoading}>
                                <VisibilityRoundedIcon fontSize="small" />
                              </IconButton>
                            </span>
                          </Tooltip>
                          {isPending && (
                            <>
                              <Button size="small" color="inherit" startIcon={busy && isLoading ? <CircularProgress size={15} /> : <CloseRoundedIcon />} disabled={isLoading} onClick={() => { setReturnComment(''); setReturnFile(null); setReturnTarget(submission); }}>
                                Вернуть
                              </Button>
                              <Button size="small" variant="contained" startIcon={<CheckRoundedIcon />} disabled={isLoading} onClick={() => { setSelectedGrade(''); setAcceptComment(''); setAcceptFile(null); setGradeTarget(submission); }}>
                                Принять
                              </Button>
                            </>
                          )}
                          {submission.status === 'in_progress' && submission.returnComment && (
                            <Typography variant="caption" color="text.secondary">На доработке</Typography>
                          )}
                        </Stack>
                      </TableCell>
                    </TableRow>
                  );
                })}
                {visibleSubmissions.length === 0 && (
                  <TableRow><TableCell colSpan={6} align="center"><Typography color="text.secondary" py={3}>Работ в этой категории нет.</Typography></TableCell></TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={reviewing !== null} onClose={() => setReviewing(null)} fullWidth maxWidth="sm">
        <DialogTitle>Работа студента</DialogTitle>
        <DialogContent>
          {reviewing && (
            <Stack spacing={1.5} mt={1}>
              <Typography fontWeight={650}>{reviewing.assignmentTitle}</Typography>
              <Typography variant="body2" color="text.secondary">{reviewing.studentName} · {reviewing.group}</Typography>
              <Typography variant="body2" className="submission-text">{reviewing.workText}</Typography>
              {reviewing.fileName && <Chip size="small" label={reviewing.fileName} className="attached-file-chip" />}
              {reviewing.teacherComment && (
                <Alert severity={reviewing.status === 'accepted' ? 'success' : 'warning'}>
                  <strong>Комментарий преподавателя:</strong> {reviewing.teacherComment}
                  {reviewing.feedbackFileName && <Box><Chip size="small" icon={<AttachFileRoundedIcon />} label={reviewing.feedbackFileName} className="attached-file-chip" /></Box>}
                </Alert>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions><Button onClick={() => setReviewing(null)}>Закрыть</Button></DialogActions>
      </Dialog>

      <Dialog open={gradeTarget !== null} onClose={() => { if (!isLoading) setGradeTarget(null); }} fullWidth maxWidth="xs">
        <Box component="form" onSubmit={handleAccept}>
          <DialogTitle>Принять работу</DialogTitle>
          <DialogContent>
            <DialogContentText mb={2}>
              {gradeTarget?.studentName} · {gradeTarget?.assignmentTitle}
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
              <FeedbackAttachmentInput file={acceptFile} onChange={setAcceptFile} disabled={isLoading} />
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
            <Box mt={2}><FeedbackAttachmentInput file={returnFile} onChange={setReturnFile} disabled={isLoading} /></Box>
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
