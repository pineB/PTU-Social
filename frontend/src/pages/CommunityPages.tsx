import { useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  Avatar,
  Box,
  Button,
  Card,
  CardActionArea,
  CardContent,
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
import { useCampusWorkflow } from '../data/campus-workflow';
import {
  communityStudents,
  initialCommunityAwards,
  type CommunityAward,
  type CommunityMetric,
  type CommunityStudent,
} from '../data/community';

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

function studentsWithLiveScores(scores: { academicRating: number; boaPoints: number; buaPoints: number }) {
  return communityStudents.map((student) => student.id === 'student-alexandra'
    ? { ...student, ...scores }
    : student);
}

export function AwardsFeedPage() {
  const { activityAwards } = useCampusWorkflow();
  const [metric, setMetric] = useState<'Все' | CommunityMetric>('Все');
  const [group, setGroup] = useState('Все группы');
  const awards = useMemo(() => {
    const liveAwards: CommunityAward[] = activityAwards.map((award, index) => {
      const student = communityStudents.find((item) => item.name === award.studentName);
      return {
        id: award.id || `live-award-${index}`,
        studentId: student?.id,
        studentName: award.studentName,
        group: award.group,
        title: award.eventTitle,
        detail: `Активность ${award.activityPercent}% · ${award.awardedBy}`,
        metric: award.activityType,
        points: award.points,
        createdAt: award.createdAt,
      };
    });
    return [...liveAwards, ...initialCommunityAwards]
      .filter((award) => metric === 'Все' || award.metric === metric)
      .filter((award) => group === 'Все группы' || award.group === group)
      .sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime());
  }, [activityAwards, group, metric]);
  const groups = ['Все группы', ...new Set(communityStudents.map((student) => student.group))];

  return (
    <Box className="community-page">
      <PageHeading title="Лента наград" />
      <Box className="community-filters" aria-label="Фильтры ленты наград">
        <MetricPicker value={metric} onChange={setMetric} />
        <FilterSelect label="Группа" value={group} options={groups} onChange={setGroup} />
      </Box>
      {awards.length > 0 ? (
        <Box className="awards-feed">
          {awards.map((award) => {
            const student = communityStudents.find((item) => item.id === award.studentId);
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
  const { academicRating, boaPoints, buaPoints } = useCampusWorkflow();
  const [metric, setMetric] = useState<CommunityMetric>('АР');
  const [group, setGroup] = useState('Все группы');
  const [course, setCourse] = useState('Все курсы');
  const students = useMemo(() => studentsWithLiveScores({ academicRating, boaPoints, buaPoints }), [academicRating, boaPoints, buaPoints]);
  const groups = ['Все группы', ...new Set(students.map((student) => student.group))];
  const courses = ['Все курсы', ...new Set(students.map((student) => `${student.course} курс`))];
  const leaders = useMemo(() => students
    .filter((student) => group === 'Все группы' || student.group === group)
    .filter((student) => course === 'Все курсы' || `${student.course} курс` === course)
    .sort((left, right) => metricValue(right, metric) - metricValue(left, metric)), [course, group, metric, students]);

  return (
    <Box className="community-page">
      <PageHeading title="Таблица лидеров" />
      <Card className="leaderboard-card">
        <CardContent>
          <Box className="leaderboard-toolbar">
            <MetricPicker value={metric} onChange={(next) => { if (next !== 'Все') setMetric(next); }} />
            <FilterSelect label="Группа" value={group} options={groups} onChange={setGroup} />
            <FilterSelect label="Курс" value={course} options={courses} onChange={setCourse} />
          </Box>
          <Box className="leaderboard-list" role="list" aria-label={`Рейтинг по показателю ${metric}`}>
            {leaders.map((student, index) => {
              const MetricIcon = metricIcons[metric];
              return (
                <StudentLink student={student} key={student.id}>
                  <Box className={`leaderboard-row${index < 3 ? ` leaderboard-row-top leaderboard-row-place-${index + 1}` : ''}`} role="listitem">
                    <Typography className="leaderboard-place">{String(index + 1).padStart(2, '0')}</Typography>
                    <Avatar className="leaderboard-avatar">{initials(student.name)}</Avatar>
                    <Box className="leaderboard-student">
                      <Typography fontWeight={750}>{student.name}</Typography>
                      <Typography variant="caption" color="text.secondary">{student.group} · {student.course} курс</Typography>
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
  const { academicRating, boaPoints, buaPoints, activityAwards } = useCampusWorkflow();
  const students = studentsWithLiveScores({ academicRating, boaPoints, buaPoints });
  const student = students.find((item) => item.id === studentId);
  const awards = [
    ...activityAwards.filter((award) => award.studentName === student?.name).map((award) => ({
      id: award.id,
      title: award.eventTitle,
      detail: `+${award.points} ${award.activityType} · активность ${award.activityPercent}%`,
      createdAt: award.createdAt,
    })),
    ...initialCommunityAwards.filter((award) => award.studentId === studentId).map((award) => ({
      id: award.id,
      title: award.title,
      detail: `+${award.points} ${award.metric} · ${award.detail}`,
      createdAt: award.createdAt,
    })),
  ].sort((left, right) => new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime()).slice(0, 5);

  if (!student) {
    return (
      <Box className="community-page">
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
            <Typography color="text.secondary">{student.group} · {student.course} курс</Typography>
          </Box>
          <Chip label={`В группе с ${new Intl.DateTimeFormat('ru-RU', { month: 'long', year: 'numeric' }).format(new Date(student.joinedAt))}`} className="profile-role-chip" />
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
      <Card className="public-profile-interests">
        <CardContent>
          <Typography variant="h6">Интересы</Typography>
          <Stack direction="row" gap={1} flexWrap="wrap" mt={1.5}>
            {student.interests.map((interest) => <Chip key={interest} label={interest} className="subtle-chip" />)}
          </Stack>
        </CardContent>
      </Card>
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
