import { useRef, useState, type FormEvent } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Collapse,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AssignmentRoundedIcon from '@mui/icons-material/AssignmentRounded';
import AttachFileRoundedIcon from '@mui/icons-material/AttachFileRounded';
import CloudUploadRoundedIcon from '@mui/icons-material/CloudUploadRounded';
import { useTaskWorkflow, type Assignment, type AssignmentStatus } from '../data/task-workflow';

const statusLabels: Record<AssignmentStatus, string> = {
  in_progress: 'В процессе',
  submitted: 'Сдано',
  overdue: 'Просрочено',
  accepted: 'Принято',
};

function getStatusClass(status: AssignmentStatus): string {
  return `assignment-status assignment-status-${status}`;
}

function formatDeadline(value: string): string {
  return new Intl.DateTimeFormat('ru-RU', {
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function formatAssignmentCount(count: number): string {
  const mod100 = count % 100;
  const mod10 = count % 10;
  const word = mod100 >= 11 && mod100 <= 14
    ? 'заданий'
    : mod10 === 1
      ? 'задание'
      : mod10 >= 2 && mod10 <= 4
        ? 'задания'
        : 'заданий';
  return `${count} ${word}`;
}

function AssignmentSubmissionForm({ assignment, onClose }: { assignment: Assignment; onClose: () => void }) {
  const { submitAssignment } = useTaskWorkflow();
  const [workText, setWorkText] = useState(assignment.workText ?? '');
  const [file, setFile] = useState<File | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const submissionLock = useRef(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submissionLock.current || isLoading) return;
    if (!workText.trim() && !file && !assignment.fileName) {
      setError('Добавьте комментарий к работе или прикрепите файл.');
      return;
    }

    submissionLock.current = true;
    setIsLoading(true);
    setError('');
    try {
      await submitAssignment(assignment.id, workText.trim(), file?.name ?? assignment.fileName);
      onClose();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Не удалось отправить работу.');
    } finally {
      submissionLock.current = false;
      setIsLoading(false);
    }
  }

  return (
    <form className="assignment-submit-form" onSubmit={handleSubmit}>
      <Stack spacing={2}>
        {assignment.status === 'in_progress' && (assignment.teacherComment ?? assignment.returnComment) && (
          <Alert severity="warning">
            <strong>Комментарий преподавателя:</strong> {assignment.teacherComment ?? assignment.returnComment}
            {assignment.feedbackFileName && (
              <Box><Chip size="small" icon={<AttachFileRoundedIcon />} label={assignment.feedbackFileName} className="attached-file-chip" /></Box>
            )}
          </Alert>
        )}
        {assignment.status === 'overdue' && (
          <Alert severity="warning">Срок сдачи прошёл. Отправка будет отмечена как просроченная.</Alert>
        )}
        <TextField
          label="Комментарий к работе"
          value={workText}
          onChange={(event) => setWorkText(event.target.value)}
          multiline
          minRows={3}
          fullWidth
        />
        <Button
          component="label"
          variant="outlined"
          startIcon={<AttachFileRoundedIcon />}
          disabled={isLoading}
          className="assignment-file-button"
        >
          {file?.name ?? assignment.fileName ?? 'Прикрепить файл'}
          <input
            type="file"
            hidden
            onChange={(event) => setFile(event.target.files?.[0] ?? null)}
            accept=".pdf,.doc,.docx,.zip,.txt,.png,.jpg,.jpeg"
          />
        </Button>
        <Typography variant="caption" color="text.secondary">
          Максимальная награда — {assignment.maxPoints} АР
        </Typography>
        {error && <Alert severity="error">{error}</Alert>}
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.2}>
          <Button type="submit" variant="contained" disabled={isLoading} startIcon={isLoading ? <CircularProgress size={18} color="inherit" /> : <CloudUploadRoundedIcon />} fullWidth>
            {isLoading ? 'Отправляем…' : assignment.status === 'in_progress' ? 'Сдать работу' : 'Отправить повторно'}
          </Button>
          <Button type="button" color="inherit" disabled={isLoading} onClick={onClose}>Отмена</Button>
        </Stack>
      </Stack>
    </form>
  );
}

function AssignmentCard({ assignment }: { assignment: Assignment }) {
  const [showForm, setShowForm] = useState(false);
  const canSubmit = assignment.status === 'in_progress' || (assignment.status === 'overdue' && assignment.allowLateSubmission);
  const hasSubmittedWork = Boolean(assignment.workText || assignment.fileName);
  const feedback = assignment.teacherComment ?? assignment.returnComment;

  return (
    <Card className="assignment-card">
      <CardContent className="assignment-card-content">
        <Box className="assignment-heading-row">
          <Box className="assignment-title-group">
            <Box className="assignment-icon"><AssignmentRoundedIcon /></Box>
            <Box>
              <Typography fontWeight={700} fontSize={17}>{assignment.title}</Typography>
              <Typography color="text.secondary" fontSize={12} mt={0.4}>{assignment.subject} · {assignment.teacher}</Typography>
            </Box>
          </Box>
          <Chip className={getStatusClass(assignment.status)} label={statusLabels[assignment.status]} />
        </Box>

        <Box className="assignment-prompt">
          <Typography variant="overline" color="text.secondary">Задание</Typography>
          <Typography variant="body1" lineHeight={1.7}>{assignment.description}</Typography>
        </Box>

        <Box className="assignment-meta">
          <Typography variant="caption" color="text.secondary">Срок: {formatDeadline(assignment.dueAt)}</Typography>
          <Typography variant="caption" color="text.secondary">До {assignment.maxPoints} АР</Typography>
        </Box>

        <Box className="assignment-actions">
          {canSubmit && <Button size="small" variant="contained" onClick={() => setShowForm((value) => !value)}>{showForm ? 'Скрыть форму' : assignment.status === 'overdue' ? 'Сдать с опозданием' : assignment.returnComment ? 'Исправить работу' : 'Сдать работу'}</Button>}
          {assignment.status === 'submitted' && <Typography variant="caption" color="text.secondary">Отправлено на проверку</Typography>}
          {assignment.status === 'accepted' && <Typography variant="caption" className="awarded-points">{assignment.grade && `Оценка ${assignment.grade} · `}+{assignment.awardedPoints ?? assignment.maxPoints} АР</Typography>}
        </Box>

        {feedback && assignment.status === 'accepted' && (
          <Alert severity="success" className="assignment-feedback">
            <strong>Комментарий преподавателя:</strong> {feedback}
            {assignment.feedbackFileName && (
              <Box><Chip size="small" icon={<AttachFileRoundedIcon />} label={assignment.feedbackFileName} className="attached-file-chip" /></Box>
            )}
          </Alert>
        )}

        {hasSubmittedWork && (
          <Box className="submitted-work-preview">
            <Typography variant="overline" color="text.secondary">Моя работа</Typography>
            {assignment.workText && <Typography variant="body2" mt={0.5}>{assignment.workText}</Typography>}
            {assignment.fileName && <Chip size="small" icon={<AttachFileRoundedIcon />} label={assignment.fileName} className="attached-file-chip" />}
          </Box>
        )}

        {canSubmit && (
          <Collapse in={showForm} unmountOnExit>
            <Box className="assignment-details">
              <AssignmentSubmissionForm assignment={assignment} onClose={() => setShowForm(false)} />
            </Box>
          </Collapse>
        )}
      </CardContent>
    </Card>
  );
}

export default function StudentTasksPage() {
  const { assignments } = useTaskWorkflow();
  const [filter, setFilter] = useState<'all' | AssignmentStatus>('all');
  const visibleAssignments = filter === 'all' ? assignments : assignments.filter((item) => item.status === filter);
  const filters: { value: 'all' | AssignmentStatus; label: string }[] = [
    { value: 'all', label: 'Все' },
    { value: 'in_progress', label: 'В процессе' },
    { value: 'submitted', label: 'Сдано' },
    { value: 'overdue', label: 'Просрочено' },
    { value: 'accepted', label: 'Принято' },
  ];

  return (
    <Box className="task-page">
      <Box className="task-page-heading">
        <Typography variant="h4" className="welcome-title">Мои задания</Typography>
        <Typography color="text.secondary" mt={0.5}>{formatAssignmentCount(assignments.length)} · текущий семестр</Typography>
      </Box>
      <Stack direction="row" spacing={1} className="task-filters" useFlexGap flexWrap="wrap">
        {filters.map((item) => (
          <Button
            key={item.value}
            size="small"
            variant={filter === item.value ? 'contained' : 'outlined'}
            color={filter === item.value ? 'primary' : 'inherit'}
            onClick={() => setFilter(item.value)}
          >
            {item.label}{item.value !== 'all' && ` · ${assignments.filter((assignment) => assignment.status === item.value).length}`}
          </Button>
        ))}
      </Stack>
      <Box className="assignment-list">
        {visibleAssignments.map((assignment) => <AssignmentCard key={assignment.id} assignment={assignment} />)}
        {visibleAssignments.length === 0 && <Alert severity="info">В этой категории пока нет заданий.</Alert>}
      </Box>
    </Box>
  );
}
