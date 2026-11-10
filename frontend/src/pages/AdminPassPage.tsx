import { useEffect, useState, type FormEvent } from 'react';
import axios from 'axios';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
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
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import SaveRoundedIcon from '@mui/icons-material/SaveRounded';
import {
  createAdminPassSeason,
  createAdminPassReward,
  deleteAdminPassReward,
  getAdminPassConfiguration,
  saveAdminPassSeason,
  updateAdminPassReward,
  type AdminPassConfiguration,
  type AdminPassReward,
  type PassCosmeticType,
  type PassRewardKind,
  type PassRewardRarity,
  type PassRewardTrack,
  type SavePassRewardRequest,
} from '../api/admin-pass';

interface ApiErrorResponse {
  message?: string;
}

interface RewardForm {
  track: PassRewardTrack;
  level: number;
  title: string;
  rewardKind: PassRewardKind;
  cosmeticType: PassCosmeticType | '';
  caseId: string;
  rarity: PassRewardRarity;
}

const emptyReward: RewardForm = {
  track: 'free',
  level: 1,
  title: '',
  rewardKind: 'case',
  cosmeticType: '',
  caseId: '',
  rarity: 'Обычный',
};

function errorText(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data.message ?? 'Не удалось сохранить изменения. Попробуйте ещё раз.';
  }
  return error instanceof Error ? error.message : 'Не удалось выполнить операцию.';
}

function dateTimeInput(value: string): string {
  const date = new Date(value);
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

function toRewardRequest(form: RewardForm): SavePassRewardRequest {
  return {
    ...form,
    title: form.title.trim(),
    cosmeticType: form.rewardKind === 'cosmetic' && form.cosmeticType ? form.cosmeticType : null,
    caseId: form.rewardKind === 'case' && form.caseId ? form.caseId : null,
  };
}

function rewardForm(reward: AdminPassReward): RewardForm {
  return {
    track: reward.track,
    level: reward.level,
    title: reward.title,
    rewardKind: reward.rewardKind,
    cosmeticType: reward.cosmeticType ?? '',
    caseId: reward.caseId ?? '',
    rarity: reward.rarity,
  };
}

export default function AdminPassPage() {
  const [configuration, setConfiguration] = useState<AdminPassConfiguration | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [seasonName, setSeasonName] = useState('');
  const [startsAt, setStartsAt] = useState('');
  const [endsAt, setEndsAt] = useState('');
  const [premiumPrice, setPremiumPrice] = useState('250');
  const [newReward, setNewReward] = useState<RewardForm>(emptyReward);
  const [editing, setEditing] = useState<AdminPassReward | null>(null);
  const [editForm, setEditForm] = useState<RewardForm>(emptyReward);

  useEffect(() => {
    let active = true;
    void getAdminPassConfiguration().then((items) => {
      if (!active) return;
      setConfiguration(items);
      setSeasonName(items.seasonName);
      setStartsAt(dateTimeInput(items.startsAt));
      setEndsAt(dateTimeInput(items.endsAt));
      setPremiumPrice(String(items.premiumPrice));
    }).catch((loadError: unknown) => {
      if (active) setError(errorText(loadError));
    }).finally(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, []);

  function applyConfiguration(next: AdminPassConfiguration) {
    setConfiguration(next);
    setSeasonName(next.seasonName);
    setStartsAt(dateTimeInput(next.startsAt));
    setEndsAt(dateTimeInput(next.endsAt));
    setPremiumPrice(String(next.premiumPrice));
  }

  async function saveSeason(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const updated = await saveAdminPassSeason({
        seasonName: seasonName.trim(),
        startsAt: new Date(startsAt).toISOString(),
        endsAt: new Date(endsAt).toISOString(),
        premiumPrice: Number(premiumPrice),
      });
      applyConfiguration(updated);
      setSuccess('Настройки сезона сохранены.');
    } catch (saveError: unknown) {
      setError(errorText(saveError));
    } finally {
      setBusy(false);
    }
  }

  async function createSeason() {
    if (busy || !window.confirm('Создать новый активный сезон и скопировать в него награды текущего сезона?')) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const created = await createAdminPassSeason({
        seasonName: seasonName.trim(),
        startsAt: new Date(startsAt).toISOString(),
        endsAt: new Date(endsAt).toISOString(),
        premiumPrice: Number(premiumPrice),
        copyRewards: true,
      });
      applyConfiguration(created);
      setSuccess('Новый сезон создан и активирован. Награды перенесены в его дорожки.');
    } catch (createError: unknown) {
      setError(errorText(createError));
    } finally {
      setBusy(false);
    }
  }

  async function addReward(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const created = await createAdminPassReward(toRewardRequest(newReward));
      setConfiguration((current) => current
        ? { ...current, rewards: [...current.rewards, created].sort((a, b) => a.level - b.level || a.track.localeCompare(b.track)) }
        : current);
      setNewReward(emptyReward);
      setSuccess('Награда добавлена в дорожку.');
    } catch (saveError: unknown) {
      setError(errorText(saveError));
    } finally {
      setBusy(false);
    }
  }

  async function saveReward(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editing || busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      const updated = await updateAdminPassReward(editing.id, toRewardRequest(editForm));
      setConfiguration((current) => current
        ? { ...current, rewards: current.rewards.map((reward) => reward.id === updated.id ? updated : reward)
          .sort((a, b) => a.level - b.level || a.track.localeCompare(b.track)) }
        : current);
      setEditing(null);
      setSuccess('Награда обновлена.');
    } catch (saveError: unknown) {
      setError(errorText(saveError));
    } finally {
      setBusy(false);
    }
  }

  async function removeReward(reward: AdminPassReward) {
    if (busy || !window.confirm(`Удалить награду «${reward.title}»?`)) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      await deleteAdminPassReward(reward.id);
      setConfiguration((current) => current
        ? { ...current, rewards: current.rewards.filter((item) => item.id !== reward.id) }
        : current);
      setSuccess('Награда удалена.');
    } catch (deleteError: unknown) {
      setError(errorText(deleteError));
    } finally {
      setBusy(false);
    }
  }

  function rewardEditor(form: RewardForm, change: (next: RewardForm) => void) {
    return (
      <Stack spacing={2} mt={1}>
        <Box display="grid" gridTemplateColumns={{ xs: '1fr', sm: '1fr 1fr' }} gap={2}>
          <FormControl fullWidth required>
            <InputLabel>Дорожка</InputLabel>
            <Select value={form.track} label="Дорожка" onChange={(event) => change({ ...form, track: event.target.value as PassRewardTrack })}>
              <MenuItem value="free">Бесплатная</MenuItem>
              <MenuItem value="premium">Премиальная</MenuItem>
            </Select>
          </FormControl>
          {form.rewardKind === 'case' && (
            <FormControl fullWidth required>
              <InputLabel>Кейс</InputLabel>
              <Select
                value={form.caseId}
                label="Кейс"
                onChange={(event) => change({ ...form, caseId: event.target.value })}
              >
                {(configuration?.cases ?? []).map((item) => (
                  <MenuItem value={item.id} key={item.id}>{item.name}</MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
          <TextField label="Уровень" type="number" inputProps={{ min: 1, max: 50 }} value={form.level} onChange={(event) => change({ ...form, level: Number(event.target.value) })} required fullWidth />
          <TextField label="Название награды" value={form.title} onChange={(event) => change({ ...form, title: event.target.value })} inputProps={{ maxLength: 120 }} required fullWidth />
          <FormControl fullWidth required>
            <InputLabel>Награда</InputLabel>
            <Select value={form.rewardKind} label="Награда" onChange={(event) => change({ ...form, rewardKind: event.target.value as PassRewardKind })}>
              <MenuItem value="case">Кейс</MenuItem>
              <MenuItem value="cosmetic">Предмет оформления</MenuItem>
            </Select>
          </FormControl>
          {form.rewardKind === 'cosmetic' && (
            <FormControl fullWidth required>
              <InputLabel>Тип предмета</InputLabel>
              <Select value={form.cosmeticType} label="Тип предмета" onChange={(event) => change({ ...form, cosmeticType: event.target.value as PassCosmeticType })}>
                <MenuItem value="avatar">Аватар</MenuItem>
                <MenuItem value="avatar-frame">Рамка аватара</MenuItem>
                <MenuItem value="profile-background">Фон профиля</MenuItem>
                <MenuItem value="badge">Значок</MenuItem>
              </Select>
            </FormControl>
          )}
          <FormControl fullWidth required>
            <InputLabel>Редкость</InputLabel>
            <Select value={form.rarity} label="Редкость" onChange={(event) => change({ ...form, rarity: event.target.value as PassRewardRarity })}>
              <MenuItem value="Обычный">Обычная</MenuItem>
              <MenuItem value="Редкий">Редкая</MenuItem>
              <MenuItem value="Эпический">Эпическая</MenuItem>
            </Select>
          </FormControl>
        </Box>
      </Stack>
    );
  }

  if (loading) return <Box display="grid" minHeight={240} sx={{ placeItems: 'center' }}><CircularProgress aria-label="Загрузка ПТУ-пасса" /></Box>;

  return (
    <Box className="management-page">
      <Stack spacing={0.75} mb={3}>
        <Typography variant="h4" fontWeight={800}>Управление ПТУ-пассом</Typography>
        <Typography color="text.secondary">Настраивайте сезон и распределяйте награды по уровням обеих дорожек.</Typography>
      </Stack>
      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}

      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" fontWeight={750} mb={2}>Сезон и премиальная дорожка</Typography>
          <Box component="form" onSubmit={(event) => void saveSeason(event)}>
            <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' }} gap={2}>
              <TextField label="Название сезона" value={seasonName} onChange={(event) => setSeasonName(event.target.value)} inputProps={{ maxLength: 120 }} required fullWidth />
              <TextField label="Стоимость премиальной дорожки, жетоны" type="number" inputProps={{ min: 1, step: 1 }} value={premiumPrice} onChange={(event) => setPremiumPrice(event.target.value)} required fullWidth />
              <TextField label="Начало сезона" type="datetime-local" value={startsAt} onChange={(event) => setStartsAt(event.target.value)} InputLabelProps={{ shrink: true }} required fullWidth />
              <TextField label="Окончание сезона" type="datetime-local" value={endsAt} onChange={(event) => setEndsAt(event.target.value)} InputLabelProps={{ shrink: true }} required fullWidth />
            </Box>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} mt={2}>
              <Button type="submit" variant="contained" disabled={busy} startIcon={busy ? <CircularProgress size={18} color="inherit" /> : <SaveRoundedIcon />}>Сохранить настройки</Button>
              <Button type="button" variant="outlined" disabled={busy} onClick={() => void createSeason()} startIcon={busy ? <CircularProgress size={18} /> : <AddRoundedIcon />}>Создать сезон с копией наград</Button>
            </Stack>
          </Box>
        </CardContent>
      </Card>

      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" fontWeight={750} mb={2}>Сезоны ПТУ-пасса</Typography>
          <Stack spacing={1}>
            {configuration?.seasons.map((season) => (
              <Box key={season.id} display="flex" flexWrap="wrap" alignItems="center" justifyContent="space-between" gap={1} p={1.5} border={1} borderColor="divider" borderRadius={2}>
                <Box>
                  <Typography fontWeight={700}>{season.seasonName}{season.active ? ' · Активный' : ''}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {new Date(season.startsAt).toLocaleDateString()} — {new Date(season.endsAt).toLocaleDateString()}
                    {' · '}{season.premiumPrice} жетонов
                  </Typography>
                </Box>
              </Box>
            ))}
          </Stack>
        </CardContent>
      </Card>

      <Card sx={{ mb: 2 }}>
        <CardContent>
          <Typography variant="h6" fontWeight={750} mb={2}>Добавить награду</Typography>
          <Box component="form" onSubmit={(event) => void addReward(event)}>
            {rewardEditor(newReward, setNewReward)}
            <Button type="submit" variant="outlined" disabled={busy} startIcon={busy ? <CircularProgress size={18} /> : <AddRoundedIcon />} sx={{ mt: 2 }}>Добавить награду</Button>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Typography variant="h6" fontWeight={750} mb={1}>Награды по уровням</Typography>
          {!configuration?.rewards.length ? <Typography color="text.secondary">Добавьте награды для уровней ПТУ-пасса.</Typography> : (
            <TableContainer>
              <Table size="small" aria-label="Награды ПТУ-пасса">
                <TableHead><TableRow><TableCell>Уровень</TableCell><TableCell>Дорожка</TableCell><TableCell>Награда</TableCell><TableCell>Редкость</TableCell><TableCell align="right">Действия</TableCell></TableRow></TableHead>
                <TableBody>
                  {configuration.rewards.map((reward) => (
                    <TableRow key={reward.id}>
                      <TableCell>{reward.level}</TableCell>
                      <TableCell>{reward.track === 'free' ? 'Бесплатная' : 'Премиальная'}</TableCell>
                      <TableCell>{reward.title}</TableCell>
                      <TableCell>{reward.rarity}</TableCell>
                      <TableCell align="right">
                        <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                          <Button size="small" aria-label={`Изменить награду ${reward.title}`} disabled={busy} onClick={() => { setEditing(reward); setEditForm(rewardForm(reward)); }} startIcon={<EditRoundedIcon />}>Изменить</Button>
                          <Button size="small" color="error" aria-label={`Удалить награду ${reward.title}`} disabled={busy} onClick={() => void removeReward(reward)}><DeleteOutlineRoundedIcon /></Button>
                        </Stack>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>

      <Dialog open={editing !== null} onClose={() => { if (!busy) setEditing(null); }} fullWidth maxWidth="sm">
        <Box component="form" onSubmit={(event) => void saveReward(event)}>
          <DialogTitle>Изменить награду</DialogTitle>
          <DialogContent>{rewardEditor(editForm, setEditForm)}</DialogContent>
          <DialogActions>
            <Button onClick={() => setEditing(null)} disabled={busy}>Отмена</Button>
            <Button type="submit" variant="contained" disabled={busy} startIcon={busy ? <CircularProgress size={18} color="inherit" /> : <SaveRoundedIcon />}>Сохранить</Button>
          </DialogActions>
        </Box>
      </Dialog>
    </Box>
  );
}
