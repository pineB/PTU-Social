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
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded';
import PersonAddAltRoundedIcon from '@mui/icons-material/PersonAddAltRounded';
import { createAcademicOfficeUser, listAcademicOfficeUsers, type ManagedAcademicOfficeUser } from '../api/administration';

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
  const [loading, setLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  async function loadUsers() {
    setLoading(true);
    setError('');
    try {
      setUsers(await listAcademicOfficeUsers());
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
    </Box>
  );
}
