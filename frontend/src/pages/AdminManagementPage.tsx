import { useEffect, useState, type FormEvent } from 'react';
import axios from 'axios';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded';
import CardGiftcardRoundedIcon from '@mui/icons-material/CardGiftcardRounded';
import PersonAddAltRoundedIcon from '@mui/icons-material/PersonAddAltRounded';
import AccountBalanceWalletRoundedIcon from '@mui/icons-material/AccountBalanceWalletRounded';
import { createAcademicOfficeUser, grantAdminTestCurrency, grantAdminTestItem, listAcademicOfficeUsers, listAdminTestStudents, type AdminTestStudent, type ManagedAcademicOfficeUser } from '../api/administration';
import { listAdminCases, type AdminCase } from '../api/admin-cases';

interface ApiErrorResponse {
  message?: string;
}

function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data.message ?? error.message;
  }
  return error instanceof Error ? error.message : 'Не удалось выполнить запрос.';
}

export default function AdminManagementPage() {
  const [users, setUsers] = useState<ManagedAcademicOfficeUser[]>([]);
  const [students, setStudents] = useState<AdminTestStudent[]>([]);
  const [cases, setCases] = useState<AdminCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [granting, setGranting] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [studentId, setStudentId] = useState('');
  const [currencyAmount, setCurrencyAmount] = useState('');
  const [rewardId, setRewardId] = useState('');

  async function loadUsers() {
    setLoading(true);
    setError('');
    try {
      const [academicOfficeUsers, studentUsers, adminCases] = await Promise.all([
        listAcademicOfficeUsers(),
        listAdminTestStudents(),
        listAdminCases(),
      ]);
      setUsers(academicOfficeUsers);
      setStudents(studentUsers);
      setCases(adminCases);
      if (!studentId && studentUsers.length) setStudentId(studentUsers[0].id);
    } catch (loadError: unknown) {
      setError(getErrorMessage(loadError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadUsers();
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isSaving) return;
    setIsSaving(true);
    setError('');
    setSuccess('');
    try {
      const created = await createAcademicOfficeUser({ fullName: fullName.trim(), email: email.trim(), password });
      setUsers((current) => [...current, created].sort((a, b) => a.fullName.localeCompare(b.fullName, 'ru')));
      setFullName('');
      setEmail('');
      setPassword('');
      setSuccess(`Учётная запись ${created.fullName} создана.`);
    } catch (createError: unknown) {
      setError(getErrorMessage(createError));
    } finally {
      setIsSaving(false);
    }
  }

  async function handleCurrencyGrant(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (granting || !studentId) return;
    setGranting(true);
    setError('');
    setSuccess('');
    try {
      await grantAdminTestCurrency(studentId, Number(currencyAmount));
      const student = students.find((item) => item.id === studentId);
      setSuccess(`${Number(currencyAmount).toLocaleString('ru-RU')} жетонов выдано студенту ${student?.fullName ?? ''}.`);
    } catch (grantError: unknown) {
      setError(getErrorMessage(grantError));
    } finally {
      setGranting(false);
    }
  }

  async function handleItemGrant(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (granting || !studentId || !rewardId) return;
    setGranting(true);
    setError('');
    setSuccess('');
    try {
      await grantAdminTestItem(studentId, rewardId);
      const reward = cases.flatMap((item) => item.rewards).find((item) => item.id === rewardId);
      const student = students.find((item) => item.id === studentId);
      setSuccess(`Предмет «${reward?.name ?? ''}» выдан студенту ${student?.fullName ?? ''}.`);
    } catch (grantError: unknown) {
      setError(getErrorMessage(grantError));
    } finally {
      setGranting(false);
    }
  }

  return (
    <Box className="management-page">
      <Stack spacing={0.75} mb={3}>
        <Typography variant="h4" fontWeight={800}>Управление учебной частью</Typography>
        <Typography color="text.secondary">Создавайте учётные записи сотрудников и проверяйте их статус.</Typography>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}

      <Card>
        <CardContent>
          <Stack direction="row" spacing={1.25} alignItems="center" mb={2}>
            <PersonAddAltRoundedIcon color="primary" />
            <Typography variant="h6" fontWeight={750}>Новая учётная запись</Typography>
          </Stack>
          <Box component="form" onSubmit={(event) => void handleSubmit(event)}>
            <Box display="grid" gridTemplateColumns={{ xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' }} gap={2}>
              <TextField label="ФИО сотрудника" value={fullName} onChange={(event) => setFullName(event.target.value)} required fullWidth autoComplete="name" />
              <TextField label="Рабочая электронная почта" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required fullWidth autoComplete="email" />
              <TextField
                label="Временный пароль"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
                fullWidth
                autoComplete="new-password"
                inputProps={{ minLength: 12, maxLength: 72 }}
                helperText="Не менее 12 символов. Передайте пароль сотруднику защищённым способом."
              />
              <Box display="flex" alignItems="center">
                <Button type="submit" variant="contained" disabled={isSaving} startIcon={isSaving ? <CircularProgress size={18} color="inherit" /> : <PersonAddAltRoundedIcon />}>
                  Создать учётную запись
                </Button>
              </Box>
            </Box>
          </Box>
        </CardContent>
      </Card>

      <Card sx={{ mt: 2.5 }}>
        <CardContent>
          <Stack direction="row" spacing={1.25} alignItems="center" mb={2}>
            <AdminPanelSettingsRoundedIcon color="primary" />
            <Typography variant="h6" fontWeight={750}>Сотрудники учебной части</Typography>
            <Chip size="small" label={users.length} />
          </Stack>
          {loading ? <CircularProgress size={24} aria-label="Загрузка сотрудников" /> : users.length === 0 ? (
            <Typography color="text.secondary">Учётных записей пока нет.</Typography>
          ) : (
            <Stack divider={<Divider flexItem />} spacing={0}>
              {users.map((user) => (
                <Stack key={user.id} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'center' }} justifyContent="space-between" py={1.5}>
                  <Box minWidth={0}>
                    <Typography fontWeight={700}>{user.fullName}</Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ overflowWrap: 'anywhere' }}>{user.email}</Typography>
                  </Box>
                  <Chip size="small" color={user.active ? 'success' : 'default'} label={user.active ? 'Активна' : 'Отключена'} />
                </Stack>
              ))}
            </Stack>
          )}
        </CardContent>
      </Card>

      <Card sx={{ mt: 2.5 }}>
        <CardContent>
          <Stack direction="row" spacing={1.25} alignItems="center" mb={1}>
            <CardGiftcardRoundedIcon color="primary" />
            <Typography variant="h6" fontWeight={750}>Тестовая выдача</Typography>
          </Stack>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Временный инструмент для проверки студенческих аккаунтов. Выданные жетоны и предметы попадут в настоящий баланс и инвентарь выбранного студента.
          </Alert>
          {loading ? <CircularProgress size={24} aria-label="Загрузка списка студентов и наград" /> : (
            <Stack spacing={2.5}>
              <FormControl fullWidth required disabled={!students.length || granting}>
                <InputLabel id="test-grant-student-label">Студент</InputLabel>
                <Select
                  labelId="test-grant-student-label"
                  label="Студент"
                  value={studentId}
                  onChange={(event) => setStudentId(event.target.value)}
                >
                  {students.map((student) => (
                    <MenuItem key={student.id} value={student.id}>
                      {student.fullName} · {student.email}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              {!students.length && <Typography color="text.secondary">Нет доступных студенческих аккаунтов для тестирования.</Typography>}
              <Box component="form" onSubmit={(event) => void handleCurrencyGrant(event)}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} alignItems={{ sm: 'flex-start' }}>
                  <TextField
                    label="Количество жетонов"
                    type="number"
                    value={currencyAmount}
                    onChange={(event) => setCurrencyAmount(event.target.value)}
                    inputProps={{ min: 0.01, step: 0.01, max: 99999999999999999 }}
                    required
                    fullWidth
                    disabled={granting || !students.length}
                  />
                  <Button
                    type="submit"
                    variant="contained"
                    disabled={granting || !studentId || !currencyAmount || Number(currencyAmount) <= 0}
                    startIcon={granting ? <CircularProgress size={18} color="inherit" /> : <AccountBalanceWalletRoundedIcon />}
                    sx={{ minWidth: { sm: 190 }, minHeight: 56 }}
                  >
                    Выдать жетоны
                  </Button>
                </Stack>
              </Box>
              <Divider />
              <Box component="form" onSubmit={(event) => void handleItemGrant(event)}>
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} alignItems={{ md: 'flex-start' }}>
                  <FormControl fullWidth required disabled={!students.length || granting || !cases.some((item) => item.active && item.rewards.length)}>
                    <InputLabel id="test-grant-item-label">Предмет из каталога наград</InputLabel>
                    <Select
                      labelId="test-grant-item-label"
                      label="Предмет из каталога наград"
                      value={rewardId}
                      onChange={(event) => setRewardId(event.target.value)}
                    >
                      {cases.filter((item) => item.active).flatMap((adminCase) => adminCase.rewards.map((reward) => (
                        <MenuItem key={reward.id} value={reward.id}>
                          {reward.name} · {reward.rarity} · {adminCase.name}
                        </MenuItem>
                      )))}
                    </Select>
                  </FormControl>
                  <Button
                    type="submit"
                    variant="outlined"
                    disabled={granting || !studentId || !rewardId}
                    startIcon={granting ? <CircularProgress size={18} /> : <CardGiftcardRoundedIcon />}
                    sx={{ minWidth: { md: 190 }, minHeight: 56 }}
                  >
                    Выдать предмет
                  </Button>
                </Stack>
                {!cases.some((item) => item.active && item.rewards.length) && (
                  <Typography variant="body2" color="text.secondary" mt={1}>
                    Сначала добавьте предметы в активный кейс в разделе «Кейсы и награды».
                  </Typography>
                )}
              </Box>
            </Stack>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
