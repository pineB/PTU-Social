import { useCallback, useEffect, useState, type FormEvent } from 'react';
import axios from 'axios';
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import ImageOutlinedIcon from '@mui/icons-material/ImageOutlined';
import AuthenticatedImage from '../components/AuthenticatedImage';
import {
  createAdminCase,
  createAdminCaseReward,
  deactivateAdminCase,
  deleteAdminCaseReward,
  listAdminCases,
  updateAdminCase,
  updateAdminCaseReward,
  type AdminCase,
  type AdminCaseReward,
  type CosmeticRarity,
  type CosmeticType,
  type SaveAdminCase,
  type SaveAdminCaseReward,
} from '../api/admin-cases';

interface ApiErrorResponse {
  message?: string;
}

interface CaseForm {
  name: string;
  description: string;
  price: string;
  image: File | null;
}

interface RewardForm {
  name: string;
  cosmeticType: CosmeticType;
  rarity: CosmeticRarity;
  weight: string;
  image: File | null;
}

const emptyCase: CaseForm = { name: '', description: '', price: '100', image: null };
const emptyReward: RewardForm = { name: '', cosmeticType: 'avatar', rarity: 'Обычный', weight: '1', image: null };
const rarityOptions: CosmeticRarity[] = ['Обычный', 'Редкий', 'Эпический'];

function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data.message ?? 'Не удалось сохранить изменения. Проверьте данные и повторите попытку.';
  }
  return error instanceof Error ? error.message : 'Не удалось выполнить операцию.';
}

function caseRequest(form: CaseForm): SaveAdminCase {
  return { name: form.name.trim(), description: form.description.trim(), price: Number(form.price), image: form.image };
}

function rewardRequest(form: RewardForm): SaveAdminCaseReward {
  return {
    name: form.name.trim(),
    cosmeticType: form.cosmeticType,
    rarity: form.rarity,
    weight: Number(form.weight),
    image: form.image,
  };
}

export default function AdminCasesPage() {
  const [cases, setCases] = useState<AdminCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [caseDialogOpen, setCaseDialogOpen] = useState(false);
  const [rewardDialog, setRewardDialog] = useState<{ caseItem: AdminCase; reward?: AdminCaseReward } | null>(null);
  const [caseForm, setCaseForm] = useState<CaseForm>(emptyCase);
  const [rewardForm, setRewardForm] = useState<RewardForm>(emptyReward);
  const [editingCase, setEditingCase] = useState<AdminCase | null>(null);

  const refresh = useCallback(async () => {
    setError('');
    setLoading(true);
    try {
      setCases(await listAdminCases());
    } catch (loadError) {
      setError(getErrorMessage(loadError));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  function openCaseForm(item?: AdminCase) {
    setEditingCase(item ?? null);
    setCaseForm(item
      ? { name: item.name, description: item.description ?? '', price: String(item.price), image: null }
      : emptyCase);
    setCaseDialogOpen(true);
  }

  function openRewardForm(caseItem: AdminCase, reward?: AdminCaseReward) {
    setRewardDialog({ caseItem, reward });
    setRewardForm(reward
      ? { name: reward.name, cosmeticType: reward.cosmeticType, rarity: reward.rarity, weight: String(reward.weight), image: null }
      : emptyReward);
  }

  async function submitCase(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      const request = caseRequest(caseForm);
      if (editingCase) await updateAdminCase(editingCase.id, request);
      else await createAdminCase(request);
      setCaseDialogOpen(false);
      await refresh();
    } catch (saveError) {
      setError(getErrorMessage(saveError));
    } finally {
      setBusy(false);
    }
  }

  async function submitReward(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy || !rewardDialog) return;
    setBusy(true);
    setError('');
    try {
      const request = rewardRequest(rewardForm);
      if (rewardDialog.reward) {
        await updateAdminCaseReward(rewardDialog.caseItem.id, rewardDialog.reward.id, request);
      } else {
        await createAdminCaseReward(rewardDialog.caseItem.id, request);
      }
      setRewardDialog(null);
      await refresh();
    } catch (saveError) {
      setError(getErrorMessage(saveError));
    } finally {
      setBusy(false);
    }
  }

  async function removeCase(item: AdminCase) {
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      await deactivateAdminCase(item.id);
      await refresh();
    } catch (removeError) {
      setError(getErrorMessage(removeError));
    } finally {
      setBusy(false);
    }
  }

  async function removeReward(caseItem: AdminCase, reward: AdminCaseReward) {
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      await deleteAdminCaseReward(caseItem.id, reward.id);
      await refresh();
    } catch (removeError) {
      setError(getErrorMessage(removeError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Box className="campus-page">
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} gap={2}>
        <Box>
          <Typography variant="h4" fontWeight={750}>Кейсы и награды</Typography>
          <Typography color="text.secondary">Создавайте предметы, задавайте вероятность выпадения и загружайте изображения.</Typography>
        </Box>
        <Button startIcon={<AddRoundedIcon />} variant="contained" onClick={() => openCaseForm()}>Создать кейс</Button>
      </Stack>
      {error && <Alert severity="error">{error}</Alert>}
      {loading ? <CircularProgress /> : cases.length === 0 ? (
        <Alert severity="info">Кейсов пока нет. Создайте первый кейс и добавьте в него награды.</Alert>
      ) : (
        <Stack spacing={2}>
          {cases.map((item) => (
            <Card key={item.id} variant="outlined">
              <CardContent>
                <Stack direction={{ xs: 'column', md: 'row' }} gap={2} alignItems={{ md: 'flex-start' }}>
                  <Avatar variant="rounded" sx={{ width: 76, height: 76 }}>
                    <AuthenticatedImage
                      src={item.imageUrl}
                      alt={item.name}
                      className="admin-catalog-image"
                      fallback={<ImageOutlinedIcon />}
                    />
                  </Avatar>
                  <Box flex={1} minWidth={0}>
                    <Stack direction="row" gap={1} alignItems="center" flexWrap="wrap">
                      <Typography variant="h6" fontWeight={700}>{item.name}</Typography>
                      <Chip size="small" label={item.active ? 'Активен' : 'Скрыт'} color={item.active ? 'success' : 'default'} />
                      <Chip size="small" label={`${item.price.toLocaleString('ru-RU')} жетонов`} />
                    </Stack>
                    <Typography color="text.secondary" mt={0.5}>{item.description || 'Описание не добавлено'}</Typography>
                  </Box>
                  <Stack direction="row" flexWrap="wrap" gap={1}>
                    <Button startIcon={<AddRoundedIcon />} onClick={() => openRewardForm(item)}>Добавить награду</Button>
                    <Button startIcon={<EditRoundedIcon />} onClick={() => openCaseForm(item)}>Изменить</Button>
                    {item.active && <Button color="error" startIcon={<DeleteOutlineRoundedIcon />} disabled={busy} onClick={() => void removeCase(item)}>Скрыть</Button>}
                  </Stack>
                </Stack>
                <Stack spacing={1.2} mt={2}>
                  {item.rewards.length === 0 && <Typography color="text.secondary">Добавьте первую награду в кейс.</Typography>}
                  {item.rewards.map((reward) => (
                    <Box className="admin-case-reward" key={reward.id}>
                      <Avatar variant="rounded" sx={{ width: 48, height: 48 }}>
                        <AuthenticatedImage
                          src={reward.imageUrl}
                          alt={reward.name}
                          className="admin-catalog-image"
                          fallback={<ImageOutlinedIcon />}
                        />
                      </Avatar>
                      <Box flex={1} minWidth={0}>
                        <Typography fontWeight={650}>{reward.name}</Typography>
                        <Typography variant="caption" color="text.secondary">
                          {rewardTypeLabel(reward.cosmeticType)} · {reward.rarity} · вес {reward.weight}
                        </Typography>
                      </Box>
                      <Chip size="small" label={`${reward.chance}%`} />
                      <Button size="small" onClick={() => openRewardForm(item, reward)}>Изменить</Button>
                      <Button size="small" color="error" disabled={busy} onClick={() => void removeReward(item, reward)}>Удалить</Button>
                    </Box>
                  ))}
                </Stack>
              </CardContent>
            </Card>
          ))}
        </Stack>
      )}

      <Dialog open={caseDialogOpen} onClose={() => !busy && setCaseDialogOpen(false)} fullWidth maxWidth="sm">
        <Box component="form" onSubmit={(event: FormEvent<HTMLFormElement>) => void submitCase(event)}>
          <DialogTitle>{editingCase ? 'Изменить кейс' : 'Создать кейс'}</DialogTitle>
          <DialogContent>
            <Stack spacing={2} mt={1}>
              <TextField label="Название кейса" value={caseForm.name} onChange={(event) => setCaseForm({ ...caseForm, name: event.target.value })} required inputProps={{ maxLength: 120 }} />
              <TextField label="Описание" value={caseForm.description} onChange={(event) => setCaseForm({ ...caseForm, description: event.target.value })} multiline minRows={2} inputProps={{ maxLength: 2000 }} />
              <TextField label="Стоимость открытия, жетоны" type="number" value={caseForm.price} onChange={(event) => setCaseForm({ ...caseForm, price: event.target.value })} required inputProps={{ min: 0.01, step: 0.01 }} />
              <Button component="label" variant="outlined" startIcon={<ImageOutlinedIcon />}>
                {caseForm.image?.name ?? 'Загрузить изображение кейса'}
                <input hidden type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => setCaseForm({ ...caseForm, image: event.target.files?.[0] ?? null })} />
              </Button>
              <Typography variant="caption" color="text.secondary">PNG, JPG или WebP, до 5 МБ.</Typography>
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setCaseDialogOpen(false)} disabled={busy}>Отмена</Button>
            <Button type="submit" variant="contained" disabled={busy}>{busy ? <CircularProgress size={18} /> : 'Сохранить'}</Button>
          </DialogActions>
        </Box>
      </Dialog>

      <Dialog open={rewardDialog !== null} onClose={() => !busy && setRewardDialog(null)} fullWidth maxWidth="sm">
        <Box component="form" onSubmit={(event: FormEvent<HTMLFormElement>) => void submitReward(event)}>
          <DialogTitle>{rewardDialog?.reward ? 'Изменить награду' : 'Добавить награду'}</DialogTitle>
          <DialogContent>
            <Stack spacing={2} mt={1}>
              <TextField label="Название награды" value={rewardForm.name} onChange={(event) => setRewardForm({ ...rewardForm, name: event.target.value })} required inputProps={{ maxLength: 120 }} />
              <TextField select label="Тип награды" value={rewardForm.cosmeticType} onChange={(event) => setRewardForm({ ...rewardForm, cosmeticType: event.target.value as CosmeticType })}>
                <MenuItem value="avatar">Аватар</MenuItem>
                <MenuItem value="avatar-frame">Рамка профиля</MenuItem>
                <MenuItem value="profile-background">Фон профиля</MenuItem>
                <MenuItem value="badge">Значок</MenuItem>
              </TextField>
              <TextField select label="Редкость" value={rewardForm.rarity} onChange={(event) => setRewardForm({ ...rewardForm, rarity: event.target.value as CosmeticRarity })}>
                {rarityOptions.map((rarity) => <MenuItem key={rarity} value={rarity}>{rarity}</MenuItem>)}
              </TextField>
              <TextField label="Вес выпадения" type="number" value={rewardForm.weight} onChange={(event) => setRewardForm({ ...rewardForm, weight: event.target.value })} required inputProps={{ min: 1, max: 1000000, step: 1 }} helperText="Вероятность рассчитывается как вес награды от общего веса кейса." />
              <Button component="label" variant="outlined" startIcon={<ImageOutlinedIcon />}>
                {rewardForm.image?.name ?? 'Загрузить изображение награды'}
                <input hidden type="file" accept="image/png,image/jpeg,image/webp" onChange={(event) => setRewardForm({ ...rewardForm, image: event.target.files?.[0] ?? null })} />
              </Button>
              <Typography variant="caption" color="text.secondary">PNG, JPG или WebP, до 5 МБ.</Typography>
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setRewardDialog(null)} disabled={busy}>Отмена</Button>
            <Button type="submit" variant="contained" disabled={busy}>{busy ? <CircularProgress size={18} /> : 'Сохранить'}</Button>
          </DialogActions>
        </Box>
      </Dialog>
    </Box>
  );
}

function rewardTypeLabel(type: CosmeticType): string {
  switch (type) {
    case 'avatar': return 'Аватар';
    case 'avatar-frame': return 'Рамка профиля';
    case 'profile-background': return 'Фон профиля';
    case 'badge': return 'Значок';
  }
}
