import { useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Avatar,
  Alert,
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
  CircularProgress,
  Chip,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import AutoStoriesRoundedIcon from '@mui/icons-material/AutoStoriesRounded';
import EmojiEventsRoundedIcon from '@mui/icons-material/EmojiEventsRounded';
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded';
import AssignmentTurnedInRoundedIcon from '@mui/icons-material/AssignmentTurnedInRounded';
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import {
  type CommunityAward,
  type CommunityMetric,
  type CommunityStudent,
} from '../data/community';
import { listCommunityAwards, listCommunityStudents } from '../api/community';

const metricLabels: Record<CommunityMetric, string> = {
  АР: 'Академический рейтинг',
  БОА: 'Общественная активность',
  БУА: 'Учебная активность',
};

const metricIcons = {
  АР: AutoStoriesRoundedIcon,
  БОА: GroupsRoundedIcon,
  БУА: AssignmentTurnedInRoundedIcon,
};

const metricValue = (student: CommunityStudent, metric: CommunityMetric) => {
  if (metric === 'АР') return student.academicRating;
  if (metric === 'БОА') return student.boaPoints;
  return student.buaPoints;
};

const initials = (name: string) => name.split(' ').slice(0, 2).map((part) => part[0]).join('');

const formatDate = (value: string) => new Intl.DateTimeFormat('ru-RU', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
}).format(new Date(value));

function PageHeading({ title, action }: { title: string; action?: ReactNode }) {
  return (
    <Box className="community-heading">
      <Typography variant="h4">{title}</Typography>
      {action}
    </Box>
  );
}

function StudentLink({ student, children }: { student: CommunityStudent; children: ReactNode }) {
  return (
    <Link className="community-student-link" to={`/student/people/${student.id}`}>
      {children}
    </Link>
  );
}

function MetricPicker({ value, onChange }: {
  value: CommunityMetric | 'Все';
  onChange: (metric: CommunityMetric | 'Все') => void;
}) {
  return (
    <ToggleButtonGroup
      exclusive
      value={value}
      onChange={(_, next: CommunityMetric | 'Все' | null) => { if (next) onChange(next); }}
      size="small"
      className="community-metric-picker"
      aria-label="Показатель рейтинга"
    >
      {(['Все', 'АР', 'БОА', 'БУА'] as const).map((metric) => (
        <ToggleButton key={metric} value={metric} aria-label={metric === 'Все' ? 'Все награды' : metricLabels[metric]}>{metric}</ToggleButton>
      ))}
    </ToggleButtonGroup>
  );
}

function FilterSelect({ label, value, options, onChange }: {
  label: string;
  value: string;
  options: string[];
  onChange: (next: string) => void;
}) {
  return (
    <FormControl size="small" className="community-filter">
      <InputLabel>{label}</InputLabel>
      <Select value={value} label={label} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => <MenuItem key={option} value={option}>{option}</MenuItem>)}
      </Select>
    </FormControl>
  );
}

export function AwardsFeedPage() {
  const [students, setStudents] = useState<CommunityStudent[]>([]);
  const [awards, setAwards] = useState<CommunityAward[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [metric, setMetric] = useState<'Все' | CommunityMetric>('Все');
  const [group, setGroup] = useState('Все группы');
  useEffect(() => {
    let active = true;
    setLoading(true);
    void Promise.all([listCommunityStudents(), listCommunityAwards()]).then(([studentItems, awardItems]) => {
      if (!active) return;
      setStudents(studentItems);
      setAwards(awardItems);
    }).catch((loadError: unknown) => {
      if (active) setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить ленту наград.');
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, []);
  const visibleAwards = useMemo(() => awards
    .filter((award) => metric === 'Все' || award.metric === metric)
    .filter((award) => group === 'Все группы' || award.group === group)
    .sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()),
  [awards, group, metric]);
  const groups = ['Все группы', ...new Set(students.map((student) => student.group))];

  return (
    <Box className="community-page">
      <PageHeading title="Лента наград" />
      <Box className="community-filters" aria-label="Фильтры ленты наград">
        <MetricPicker value={metric} onChange={setMetric} />
        <FilterSelect label="Группа" value={group} options={groups} onChange={setGroup} />
      </Box>
      {error && <Alert severity="error">{error}</Alert>}
      {loading ? <CircularProgress /> : visibleAwards.length > 0 ? (
        <Box className="awards-feed">
          {visibleAwards.map((award) => {
            const student = students.find((item) => item.id === award.studentId);
            const MetricIcon = metricIcons[award.metric];
            const content = (
              <CardContent className="award-feed-content">
                <Avatar className="award-student-avatar">{initials(award.studentName)}</Avatar>
                <Box className="award-feed-copy">
                  <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap">
                    <Typography fontWeight={750}>{award.studentName}</Typography>
                    <Typography variant="caption" color="text.secondary">{award.group}</Typography>
                  </Stack>
                  <Typography variant="body2" mt={0.45}>{award.title}</Typography>
                  <Typography variant="caption" color="text.secondary">{award.detail}</Typography>
                </Box>
                <Box className={`award-points award-points-${award.metric.toLowerCase()}`}>
                  <MetricIcon fontSize="small" />
                  <Typography fontWeight={800}>+{award.points} {award.metric}</Typography>
                </Box>
                <Typography className="award-date">{formatDate(award.createdAt)}</Typography>
              </CardContent>
            );
            return (
              <Card className="award-feed-card" key={award.id}>
                {student
                  ? <CardActionArea component={Link} to={`/student/people/${student.id}`} className="award-feed-action">{content}</CardActionArea>
                  : content}
              </Card>
            );
          })}
        </Box>
      ) : (
        <Card className="community-empty-state"><CardContent><Typography color="text.secondary">Наград по выбранным фильтрам пока нет.</Typography></CardContent></Card>
      )}
    </Box>
  );
}

export function LeaderboardPage() {
  const [students, setStudents] = useState<CommunityStudent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [metric, setMetric] = useState<CommunityMetric>('АР');
  const [group, setGroup] = useState('Все группы');
  useEffect(() => {
    let active = true;
    void listCommunityStudents().then((items) => {
      if (active) setStudents(items);
    }).catch((loadError: unknown) => {
      if (active) setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить рейтинг.');
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, []);
  const groups = ['Все группы', ...new Set(students.map((student) => student.group))];
  const leaders = useMemo(() => students
    .filter((student) => group === 'Все группы' || student.group === group)
    .sort((left, right) => metricValue(right, metric) - metricValue(left, metric)), [group, metric, students]);

  return (
    <Box className="community-page">
      <PageHeading title="Таблица лидеров" />
      <Card className="leaderboard-card">
        <CardContent>
          <Box className="leaderboard-toolbar">
            <MetricPicker value={metric} onChange={(next) => { if (next !== 'Все') setMetric(next); }} />
            <FilterSelect label="Группа" value={group} options={groups} onChange={setGroup} />
          </Box>
          {error && <Alert severity="error">{error}</Alert>}
          <Box className="leaderboard-list" role="list" aria-label={`Рейтинг по показателю ${metric}`}>
            {loading ? <CircularProgress /> : leaders.map((student, index) => {
              const MetricIcon = metricIcons[metric];
              return (
                <StudentLink student={student} key={student.id}>
                  <Box className={`leaderboard-row${index < 3 ? ` leaderboard-row-top leaderboard-row-place-${index + 1}` : ''}`} role="listitem">
                    <Typography className="leaderboard-place">{String(index + 1).padStart(2, '0')}</Typography>
                    <Avatar className="leaderboard-avatar">{initials(student.name)}</Avatar>
                    <Box className="leaderboard-student">
                      <Typography fontWeight={750}>{student.name}</Typography>
                      <Typography variant="caption" color="text.secondary">{student.group}</Typography>
                    </Box>
                    <Box className="leaderboard-score"><MetricIcon fontSize="small" /><Typography fontWeight={800}>{metricValue(student, metric)} <span>{metric}</span></Typography></Box>
                  </Box>
                </StudentLink>
              );
            })}
            {leaders.length === 0 && <Typography className="community-empty-text" color="text.secondary">В этой группе нет участников рейтинга.</Typography>}
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}

export function PublicStudentProfilePage() {
  const { studentId } = useParams<{ studentId: string }>();
  const [students, setStudents] = useState<CommunityStudent[]>([]);
  const [awardsData, setAwardsData] = useState<CommunityAward[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => {
    let active = true;
    void Promise.all([listCommunityStudents(), listCommunityAwards()]).then(([studentItems, awardItems]) => {
      if (!active) return;
      setStudents(studentItems);
      setAwardsData(awardItems);
    }).catch((loadError: unknown) => {
      if (active) setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить профиль.');
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, []);
  const student = students.find((item) => item.id === studentId);
  const awards = awardsData.filter((award) => award.studentId === studentId)
    .sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime())
    .slice(0, 5);

  if (loading) return <Box className="community-page"><CircularProgress /></Box>;
  if (!student) {
    return (
      <Box className="community-page">
        {error && <Alert severity="error">{error}</Alert>}
        <Card className="community-empty-state">
          <CardContent>
            <Typography variant="h5" mb={1}>Профиль не найден</Typography>
            <Button component={Link} to="/student/leaderboard" startIcon={<ArrowBackRoundedIcon />}>К таблице лидеров</Button>
          </CardContent>
        </Card>
      </Box>
    );
  }

  const studentRank = (metric: CommunityMetric) => students
    .filter((candidate) => candidate.group === student.group)
    .sort((left, right) => metricValue(right, metric) - metricValue(left, metric))
    .findIndex((candidate) => candidate.id === student.id) + 1;
  const metrics: CommunityMetric[] = ['АР', 'БОА', 'БУА'];

  return (
    <Box className="community-page">
      <PageHeading title="Профиль студента" action={<Button component={Link} to="/student/leaderboard" variant="outlined" startIcon={<ArrowBackRoundedIcon />}>К рейтингу</Button>} />
      <Card className="public-profile-hero">
        <CardContent>
          <Avatar className="public-profile-avatar">{initials(student.name)}</Avatar>
          <Box className="public-profile-copy">
            <Typography variant="h4">{student.name}</Typography>
            <Typography color="text.secondary">{student.group}</Typography>
          </Box>
          <Chip label={`В сообществе с ${new Intl.DateTimeFormat('ru-RU', { month: 'long', year: 'numeric' }).format(new Date(student.joinedAt))}`} className="profile-role-chip" />
        </CardContent>
      </Card>
      <Box className="public-profile-metrics">
        {metrics.map((metric) => {
          const MetricIcon = metricIcons[metric];
          return (
            <Card className="public-profile-metric" key={metric}>
              <CardContent>
                <Avatar className="profile-metric-icon"><MetricIcon fontSize="small" /></Avatar>
                <Typography variant="h5">{metricValue(student, metric)}</Typography>
                <Typography variant="body2" color="text.secondary">{metricLabels[metric]}</Typography>
                <Chip size="small" label={`${studentRank(metric)} место в группе`} className="subtle-chip" />
              </CardContent>
            </Card>
          );
        })}
      </Box>
      <Card className="public-profile-awards">
        <CardContent>
          <Stack direction="row" spacing={1} alignItems="center" mb={1.5}>
            <EmojiEventsRoundedIcon color="primary" />
            <Typography variant="h6">Последние награды</Typography>
          </Stack>
          {awards.length ? awards.map((award) => (
            <Box className="public-award-row" key={award.id}>
              <Box><Typography fontWeight={700}>{award.title}</Typography><Typography variant="caption" color="text.secondary">{award.detail}</Typography></Box>
              <Typography className="award-date">{formatDate(award.createdAt)}</Typography>
            </Box>
          )) : <Typography color="text.secondary">Наград пока нет.</Typography>}
        </CardContent>
      </Card>
    </Box>
  );
}
