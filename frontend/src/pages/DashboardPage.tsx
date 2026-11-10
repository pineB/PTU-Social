import { useMemo, useState } from 'react';
import {
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  InputAdornment,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded';
import AutoStoriesRoundedIcon from '@mui/icons-material/AutoStoriesRounded';
import AssignmentTurnedInRoundedIcon from '@mui/icons-material/AssignmentTurnedInRounded';
import EventAvailableRoundedIcon from '@mui/icons-material/EventAvailableRounded';
import { dashboards, type DashboardData } from '../data/dashboards';
import { useLocation } from 'react-router-dom';
import { ActivationResultNotice, RegistrationResultNotice } from './AccountPages';
import { Link } from 'react-router-dom';
import { useCampusWorkflow } from '../data/campus-workflow';
import { useTaskWorkflow } from '../data/task-workflow';

export default function DashboardPage({ data, focusOverride }: { data: DashboardData; focusOverride?: { label: string; title: string; description?: string; footnote?: string; progress?: number; action?: { href: string; label: string } } }) {
  const [search, setSearch] = useState('');
  const focus = focusOverride ?? {
    label: 'Обзор',
    title: 'Пока нет обновлений',
    description: 'Здесь появятся ближайшие задачи и важные события.',
  };
  const filteredRows = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('ru');
    if (!query) return data.rows;
    return data.rows.filter((row) =>
      `${row.subject} ${row.detail} ${row.status}`.toLocaleLowerCase('ru').includes(query),
    );
  }, [data.rows, search]);

  return (
    <Box className="dashboard">
      <Box className="welcome-row">
        <Box className="welcome-copy">
          <Typography variant="h4" className="welcome-title">{data.heading}</Typography>
        </Box>
        <Stack direction="row" gap={1} flexWrap="wrap">
          {data.role === 'academic-office' && <Button component={Link} to="/academic-office/schedule" variant="outlined">Расписание</Button>}
          {data.role === 'teacher' && <Button component={Link} to="/teacher/students" variant="outlined" startIcon={<GroupsRoundedIcon />}>Мои студенты</Button>}
          <Button component={Link} to={data.role === 'student' ? '/student/tasks' : data.role === 'teacher' ? '/teacher/submissions' : data.role === 'organizer' ? '/organizer/events' : '/academic-office/certificates'} variant="contained" endIcon={<ArrowForwardRoundedIcon />} className="primary-action">
            {data.role === 'student' ? 'Все задания' : data.role === 'teacher' ? 'Проверить работы' : data.role === 'organizer' ? 'Мои мероприятия' : 'Обработать справки'}
          </Button>
        </Stack>
      </Box>

      <Box className="metrics-grid">
        {data.metrics.map((metric) => {
          const Icon = metric.icon;
          return (
            <Card key={metric.label} className="metric-card">
              <CardContent className="metric-card-content">
                <Box className="metric-topline">
                  <Typography color="text.secondary" fontSize={13} fontWeight={600}>{metric.label}</Typography>
                  <Avatar className="metric-icon" sx={{ color: metric.color, bgcolor: metric.background }}>
                    <Icon fontSize="small" />
                  </Avatar>
                </Box>
                <Typography variant="h5" className="metric-value">{metric.value}</Typography>
                <Typography variant="caption" color="text.secondary">{metric.detail}</Typography>
              </CardContent>
            </Card>
          );
        })}
      </Box>

      <Box className="content-grid">
        <Card className="schedule-card" id="dashboard-table">
          <CardContent className="schedule-card-content">
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} justifyContent="space-between" mb={2.5}>
              <Box>
                <Typography variant="h6">{data.tableTitle}</Typography>
              </Box>
              <TextField
                size="small"
                placeholder="Поиск..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                className="table-search"
                inputProps={{ 'aria-label': 'Поиск по таблице' }}
                InputProps={{
                  startAdornment: <InputAdornment position="start"><SearchRoundedIcon fontSize="small" /></InputAdornment>,
                }}
              />
            </Stack>
            <TableContainer>
              <Table aria-label={data.tableTitle}>
                <TableHead>
                  <TableRow>
                    <TableCell>{data.subjectHeading}</TableCell>
                    <TableCell>{data.detailHeading}</TableCell>
                    <TableCell align="right">
                      {data.role === 'student' ? 'Срок / статус' : data.role === 'teacher' ? 'Время' : 'Статус'}
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredRows.map((row) => (
                    <TableRow key={`${row.subject}-${row.status}`} hover>
                      <TableCell>
                        <Typography fontSize={13.5} fontWeight={650}>{row.subject}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography fontSize={12.5} color="text.secondary">{row.detail}</Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Chip size="small" label={row.status} className={`status-chip status-${row.tone}`} />
                      </TableCell>
                    </TableRow>
                  ))}
                  {filteredRows.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={3} align="center">
                        <Typography color="text.secondary" py={2}>
                          {data.rows.length ? 'Ничего не найдено' : 'В этом разделе пока нет записей.'}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>

        <Card className="focus-card">
          <CardContent className="focus-card-content">
            <Typography className="focus-label">{focus.label}</Typography>
            <Typography variant="h5" className="focus-title">{focus.title}</Typography>
            {'description' in focus && (
              <Typography color="text.secondary" variant="body2" lineHeight={1.7} mt={0.8}>
                {focus.description}
              </Typography>
            )}
            {'progress' in focus && (
              <Box className="progress-block">
                <Box className="progress-track"><Box className="progress-fill" style={{ width: `${focus.progress}%` }} /></Box>
              </Box>
            )}
            {'footnote' in focus && <Typography className="focus-footnote">{focus.footnote}</Typography>}
            {'action' in focus && focus.action && (
              <Button component={Link} to={focus.action.href} variant="outlined" sx={{ alignSelf: 'flex-start', mt: 1.5 }}>
                {focus.action.label}
              </Button>
            )}
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
}

export function StudentDashboard() {
  const location = useLocation();
  const {
    academicRating, boaPoints, buaPoints, passExperience, passSeasonName,
    passStartsAt, passEndsAt, passRewards, isLoading,
  } = useCampusWorkflow();
  const { assignments } = useTaskWorkflow();
  const routeState = location.state as { registrationSubmitted?: boolean } | null;
  const passLevel = passExperience === null ? null : Math.min(50, Math.floor(passExperience / 20));
  const passProgress = passExperience === null || passLevel === null
    ? null
    : passLevel >= 50 ? 20 : passExperience % 20;
  const seasonDates = passStartsAt && passEndsAt
    ? `${new Intl.DateTimeFormat('ru-RU', { dateStyle: 'medium' }).format(new Date(passStartsAt))} — ${new Intl.DateTimeFormat('ru-RU', { dateStyle: 'medium' }).format(new Date(passEndsAt))}`
    : null;
  const data = {
    ...dashboards.student,
    metrics: [
      { label: 'Академический рейтинг', value: academicRating === null ? '—' : String(academicRating), detail: 'за текущий семестр', icon: AutoStoriesRoundedIcon, color: '#3569b8', background: '#e7f0fc' },
      { label: 'Учебная активность', value: buaPoints === null ? '—' : String(buaPoints), detail: 'БУА', icon: AssignmentTurnedInRoundedIcon, color: '#3569b8', background: '#e7f0fc' },
      { label: 'Общественная активность', value: boaPoints === null ? '—' : String(boaPoints), detail: 'БОА', icon: EventAvailableRoundedIcon, color: '#b8782b', background: '#fbf0df' },
      { label: 'ПТУ-пасс', value: passLevel === null ? '—' : `${passLevel} / 50`, detail: passSeasonName ?? 'Уровень сезона', icon: GroupsRoundedIcon, color: '#3569b8', background: '#e7f0fc' },
    ],
    rows: assignments.map((assignment) => ({
      subject: assignment.title,
      detail: `${assignment.subject} · ${assignment.teacher}`,
      status: assignment.status,
      tone: assignment.status === 'accepted' ? 'success' as const : assignment.status === 'overdue' ? 'warning' as const : 'neutral' as const,
    })),
  };
  const focus = {
    label: 'ПТУ-пасс',
    title: passLevel === null
      ? isLoading ? 'Загружаем данные сезона…' : 'Сезон пока недоступен'
      : `${passSeasonName ?? 'ПТУ-пасс'} · уровень ${passLevel} / 50`,
    description: passExperience === null
      ? 'Информация о сезоне и наградах появится здесь.'
      : passLevel !== null && passLevel >= 50
        ? `Максимальный уровень достигнут · ${passExperience} АР`
        : `${passExperience} АР · ещё ${20 - (passProgress ?? 0)} АР до следующего уровня`,
    ...(passProgress === null ? {} : { progress: (passProgress / 20) * 100 }),
    ...(passExperience === null ? {} : {
      footnote: seasonDates ? `Сезон: ${seasonDates} · наград: ${passRewards.length}` : `Наград сезона: ${passRewards.length}`,
    }),
    action: { href: '/student/battle-pass', label: 'Открыть ПТУ-пасс' },
  };
  return (
    <>
      <RegistrationResultNotice submitted={routeState?.registrationSubmitted === true} />
      <DashboardPage data={data} focusOverride={focus} />
    </>
  );
}

export function TeacherDashboard() {
  const location = useLocation();
  const { assignments, submissions } = useTaskWorkflow();
  const routeState = location.state as { invitationActivated?: boolean } | null;
  const data = {
    ...dashboards.teacher,
    metrics: [
      { label: 'Заданий', value: String(assignments.length), detail: 'назначены вам', icon: AssignmentTurnedInRoundedIcon, color: '#3569b8', background: '#e7f0fc' },
      { label: 'Работ на проверку', value: String(submissions.filter((item) => item.status === 'submitted').length), detail: 'ожидают проверки', icon: AutoStoriesRoundedIcon, color: '#3569b8', background: '#e7f0fc' },
    ],
    rows: assignments.map((assignment) => ({
      subject: assignment.title,
      detail: assignment.subject,
      status: new Date(assignment.dueAt).toLocaleDateString('ru-RU'),
      tone: 'neutral' as const,
    })),
  };
  return (
    <>
      <ActivationResultNotice activated={routeState?.invitationActivated === true} />
      <DashboardPage data={data} />
    </>
  );
}

export function OrganizerDashboard() {
  return <DashboardPage data={dashboards.organizer} />;
}

export function AcademicOfficeDashboard() {
  return <DashboardPage data={dashboards['academic-office']} />;
}
