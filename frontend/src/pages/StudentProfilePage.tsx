import {
  Avatar,
  Box,
  Card,
  CardContent,
  Chip,
  Stack,
  Typography,
} from '@mui/material';
import AutoStoriesRoundedIcon from '@mui/icons-material/AutoStoriesRounded';
import EmojiEventsRoundedIcon from '@mui/icons-material/EmojiEventsRounded';
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded';
import AssignmentTurnedInRoundedIcon from '@mui/icons-material/AssignmentTurnedInRounded';
import type { SvgIconComponent } from '@mui/icons-material';
import { useCampusWorkflow } from '../data/campus-workflow';
import { useAuth } from '../auth/AuthProvider';

interface ProfileMetric {
  label: string;
  value: string;
  description: string;
  icon: SvgIconComponent;
}

export default function StudentProfilePage() {
  const { currentUser, profileLoading, profileError } = useAuth();
  const { academicRating, boaPoints, buaPoints, activityAwards } = useCampusWorkflow();
  const profileName = currentUser?.fullName ?? '';
  const initials = profileName.split(' ').slice(0, 2).map((part) => part[0]).join('');
  const profileMetrics: ProfileMetric[] = [
    { label: 'Академический рейтинг', value: academicRating === null ? '—' : String(academicRating), description: 'за текущий семестр', icon: AutoStoriesRoundedIcon },
    { label: 'БОА', value: boaPoints === null ? '—' : String(boaPoints), description: 'общественная активность', icon: GroupsRoundedIcon },
    { label: 'БУА', value: buaPoints === null ? '—' : String(buaPoints), description: 'учебная активность', icon: AssignmentTurnedInRoundedIcon },
  ];
  const recentAwards = activityAwards.map((award) => ({
    title: award.eventTitle,
    detail: `Участие подтверждено · ${award.group}`,
    date: new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'long' }).format(new Date(award.createdAt)),
    points: `+${award.points} ${award.activityType}`,
    type: award.activityType === 'БОА' ? 'social' : 'academic',
  }));
  const profileHistory = recentAwards;

  return (
    <Box className="student-profile-page">
      <Card className="profile-hero-card">
        <CardContent className="profile-hero-content">
          <Avatar className="profile-hero-avatar">{initials}</Avatar>
          <Box className="profile-hero-copy">
            <Typography variant="h4">{profileLoading ? 'Загрузка профиля…' : profileName}</Typography>
            <Typography color="text.secondary" mt={0.5}>{currentUser?.groupName ?? 'Студент'}</Typography>
          </Box>
          <Chip label="Студент" className="profile-role-chip" />
        </CardContent>
      </Card>
      {profileError && <Typography color="error">{profileError}</Typography>}

      <Box className="profile-metrics-grid">
        {profileMetrics.map((metric) => {
          const Icon = metric.icon;
          return (
            <Card className="profile-metric-card" key={metric.label}>
              <CardContent className="profile-metric-content">
                <Box className="profile-metric-header">
                  <Typography color="text.secondary" fontSize={13} fontWeight={600}>{metric.label}</Typography>
                  <Avatar className="profile-metric-icon"><Icon fontSize="small" /></Avatar>
                </Box>
                <Typography variant="h5" mt={1.6}>{metric.value}</Typography>
                <Typography variant="caption" color="text.secondary">{metric.description}</Typography>
              </CardContent>
            </Card>
          );
        })}
      </Box>

      <Card className="profile-history-card">
        <CardContent className="profile-history-content">
          <Stack direction="row" spacing={1.2} alignItems="center" mb={1}>
            <EmojiEventsRoundedIcon color="primary" />
            <Typography variant="h6">История активности</Typography>
          </Stack>
          <Box className="profile-history-list">
            {profileHistory.map((item) => (
              <Box className="profile-history-row" key={`${item.title}-${item.date}`}>
                <Avatar className={`history-icon history-icon-${item.type}`}>
                  {item.type === 'academic' ? <AutoStoriesRoundedIcon /> : <GroupsRoundedIcon />}
                </Avatar>
                <Box className="history-copy">
                  <Typography fontSize={13.5} fontWeight={650}>{item.title}</Typography>
                  <Typography fontSize={12} color="text.secondary" mt={0.35}>{item.detail}</Typography>
                </Box>
                <Typography className="history-date">{item.date}</Typography>
                <Chip size="small" label={item.points} className={`history-points history-points-${item.type}`} />
              </Box>
            ))}
            {profileHistory.length === 0 && <Typography color="text.secondary">История активности пока пуста.</Typography>}
          </Box>
        </CardContent>
      </Card>
    </Box>
  );
}
