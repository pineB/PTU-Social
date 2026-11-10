import { useEffect, useState } from 'react';
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Typography,
} from '@mui/material';
import AutoStoriesRoundedIcon from '@mui/icons-material/AutoStoriesRounded';
import EmojiEventsRoundedIcon from '@mui/icons-material/EmojiEventsRounded';
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded';
import AssignmentTurnedInRoundedIcon from '@mui/icons-material/AssignmentTurnedInRounded';
import type { SvgIconComponent } from '@mui/icons-material';
import { getProfileCosmetics, saveProfileCosmetics, type ProfileCosmetics } from '../api/campus';
import AuthenticatedImage from '../components/AuthenticatedImage';
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
  const { academicRating, boaPoints, buaPoints, activityAwards, inventory } = useCampusWorkflow();
  const [cosmetics, setCosmetics] = useState<ProfileCosmetics>({
    avatarItemId: null,
    frameItemId: null,
    backgroundItemId: null,
  });
  const [cosmeticsLoading, setCosmeticsLoading] = useState(true);
  const [cosmeticsSaving, setCosmeticsSaving] = useState(false);
  const [cosmeticsError, setCosmeticsError] = useState('');
  const [cosmeticsSaved, setCosmeticsSaved] = useState(false);

  useEffect(() => {
    let active = true;
    getProfileCosmetics().then((value) => {
      if (active) setCosmetics(value);
    }).catch((error: unknown) => {
      if (active) setCosmeticsError(error instanceof Error ? error.message : 'Не удалось загрузить оформление профиля.');
    }).finally(() => {
      if (active) setCosmeticsLoading(false);
    });
    return () => { active = false; };
  }, []);

  async function handleSaveCosmetics() {
    if (cosmeticsSaving) return;
    setCosmeticsSaving(true);
    setCosmeticsError('');
    setCosmeticsSaved(false);
    try {
      setCosmetics(await saveProfileCosmetics(cosmetics));
      setCosmeticsSaved(true);
    } catch (error) {
      setCosmeticsError(error instanceof Error ? error.message : 'Не удалось сохранить оформление профиля.');
    } finally {
      setCosmeticsSaving(false);
    }
  }

  const profileName = currentUser?.fullName ?? '';
  const initials = profileName.split(' ').slice(0, 2).map((part) => part[0]).join('');
  const avatarItem = inventory.find((item) => item.id === cosmetics.avatarItemId);
  const frameItem = inventory.find((item) => item.id === cosmetics.frameItemId);
  const backgroundItem = inventory.find((item) => item.id === cosmetics.backgroundItemId);
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
          {backgroundItem && <AuthenticatedImage src={backgroundItem.imageUrl} alt="" className="profile-hero-background" />}
          <Box className="profile-avatar-composition">
            <Avatar className="profile-hero-avatar">
              {avatarItem?.imageUrl
                ? <AuthenticatedImage src={avatarItem.imageUrl} alt="Аватар профиля" className="profile-cosmetic-avatar" />
                : initials}
            </Avatar>
            {frameItem && <AuthenticatedImage src={frameItem.imageUrl} alt="Рамка профиля" className="profile-avatar-frame-overlay" />}
          </Box>
          <Box className="profile-hero-copy">
            <Typography variant="h4">{profileLoading ? 'Загрузка профиля…' : profileName}</Typography>
            <Typography color="text.secondary" mt={0.5}>{currentUser?.groupName ?? 'Студент'}</Typography>
          </Box>
          <Chip label="Студент" className="profile-role-chip" />
        </CardContent>
      </Card>
      {profileError && <Typography color="error">{profileError}</Typography>}
      <Card>
        <CardContent>
          <Typography variant="h6" fontWeight={700}>Оформление профиля</Typography>
          <Typography color="text.secondary" mb={2}>Выбирайте только предметы, которые уже есть в вашем инвентаре.</Typography>
          {cosmeticsError && <Alert severity="error" sx={{ mb: 2 }}>{cosmeticsError}</Alert>}
          {cosmeticsSaved && <Alert severity="success" sx={{ mb: 2 }}>Оформление профиля сохранено.</Alert>}
          {cosmeticsLoading ? <CircularProgress size={24} /> : (
            <Stack spacing={2} maxWidth={520}>
              <FormControl fullWidth>
                <InputLabel id="profile-avatar-label">Аватар</InputLabel>
                <Select
                  labelId="profile-avatar-label"
                  label="Аватар"
                  value={cosmetics.avatarItemId ?? ''}
                  onChange={(event) => setCosmetics({ ...cosmetics, avatarItemId: event.target.value || null })}
                >
                  <MenuItem value="">Стандартный</MenuItem>
                  {inventory.filter((item) => item.kind === 'cosmetic' && item.cosmeticType === 'avatar').map((item) => (
                    <MenuItem value={item.id} key={item.id}>{item.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl fullWidth>
                <InputLabel id="profile-frame-label">Рамка профиля</InputLabel>
                <Select
                  labelId="profile-frame-label"
                  label="Рамка профиля"
                  value={cosmetics.frameItemId ?? ''}
                  onChange={(event) => setCosmetics({ ...cosmetics, frameItemId: event.target.value || null })}
                >
                  <MenuItem value="">Без рамки</MenuItem>
                  {inventory.filter((item) => item.kind === 'cosmetic' && item.cosmeticType === 'avatar-frame').map((item) => (
                    <MenuItem value={item.id} key={item.id}>{item.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl fullWidth>
                <InputLabel id="profile-background-label">Фон профиля</InputLabel>
                <Select
                  labelId="profile-background-label"
                  label="Фон профиля"
                  value={cosmetics.backgroundItemId ?? ''}
                  onChange={(event) => setCosmetics({ ...cosmetics, backgroundItemId: event.target.value || null })}
                >
                  <MenuItem value="">Стандартный</MenuItem>
                  {inventory.filter((item) => item.kind === 'cosmetic' && item.cosmeticType === 'profile-background').map((item) => (
                    <MenuItem value={item.id} key={item.id}>{item.name}</MenuItem>
                  ))}
                </Select>
              </FormControl>
              <Button variant="contained" disabled={cosmeticsSaving} onClick={() => void handleSaveCosmetics()}>
                {cosmeticsSaving ? <CircularProgress size={18} color="inherit" /> : 'Сохранить оформление'}
              </Button>
            </Stack>
          )}
        </CardContent>
      </Card>

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
