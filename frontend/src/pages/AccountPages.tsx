import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import axios from 'axios';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  FormControl,
  IconButton,
  InputAdornment,
  InputLabel,
  Link as MuiLink,
  MenuItem,
  Select,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import DarkModeRoundedIcon from '@mui/icons-material/DarkModeRounded';
import LightModeRoundedIcon from '@mui/icons-material/LightModeRounded';
import MailOutlineRoundedIcon from '@mui/icons-material/MailOutlineRounded';
import SpaceDashboardRoundedIcon from '@mui/icons-material/SpaceDashboardRounded';
import VisibilityOffRoundedIcon from '@mui/icons-material/VisibilityOffRounded';
import VisibilityRoundedIcon from '@mui/icons-material/VisibilityRounded';
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useColorMode } from '../theme/ColorModeProvider';
import { useAuth } from '../auth/AuthProvider';
import type { UserRole } from '../api/auth';
import { acceptTeacherInvite, createRegistrationRequest, getTeacherInvite, listFaculties, listStudyGroups, resendRegistrationCode, verifyRegistrationEmail, type FacultyOption, type StudyGroupOption, type TeacherInviteInfo } from '../api/account';

interface ApiErrorResponse {
  message?: string;
}

const roleHomePaths: Record<UserRole, string> = {
  STUDENT: '/student',
  TEACHER: '/teacher',
  ORGANIZER: '/organizer',
  ACADEMIC_OFFICE: '/academic-office',
  ADMIN: '/admin',
};

const registrationRequestIdKey = 'ptu-social-registration-request-id';
const registrationEmailKey = 'ptu-social-registration-email';

function AccountPageShell({ children }: { children: ReactNode }) {
  const { mode, toggleMode } = useColorMode();
  const ModeIcon = mode === 'dark' ? LightModeRoundedIcon : DarkModeRoundedIcon;
  const modeLabel = mode === 'dark' ? 'Включить светлую тему' : 'Включить тёмную тему';

  return (
    <Box className="account-page">
      <Box className="account-topbar">
        <Link to="/student" className="account-brand" aria-label="ПТУ-Сошиал — на главную">
          <Box className="brand-mark"><SpaceDashboardRoundedIcon /></Box>
          <Typography className="brand-name">ПТУ-Сошиал</Typography>
        </Link>
        <Tooltip title={modeLabel}>
          <IconButton aria-label={modeLabel} onClick={toggleMode}>
            <ModeIcon />
          </IconButton>
        </Tooltip>
      </Box>
      <Box className="account-content">{children}</Box>
    </Box>
  );
}

function AccountCard({
  eyebrow,
  title,
  description,
  children,
}: {
  eyebrow: string;
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <Card className="account-card">
      <CardContent className="account-card-content">
        <Typography className="account-eyebrow">{eyebrow}</Typography>
        <Typography variant="h4" className="account-title">{title}</Typography>
        {description && <Typography color="text.secondary" mt={1}>{description}</Typography>}
        <Box className="account-form-content">{children}</Box>
      </CardContent>
    </Card>
  );
}

function SubmitButton({ loading, children }: { loading: boolean; children: string }) {
  return (
    <Button
      type="submit"
      variant="contained"
      fullWidth
      disabled={loading}
      endIcon={loading ? undefined : <ArrowForwardRoundedIcon />}
      className="account-submit"
    >
      {loading ? <CircularProgress size={20} color="inherit" /> : children}
    </Button>
  );
}

export function LoginPage() {
  const { session, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (loading) return;
    setLoading(true);
    setError('');
    try {
      const role = await login(email, password);
      navigate(roleHomePaths[role], { replace: true });
    } catch (loginError) {
      const message = axios.isAxiosError<ApiErrorResponse>(loginError)
        ? loginError.response?.data.message
        : undefined;
      setError(message || (loginError instanceof Error ? loginError.message : 'Не удалось войти.'));
    } finally {
      setLoading(false);
    }
  }

  if (session) return <Navigate to={roleHomePaths[session.role]} replace />;

  return (
    <AccountPageShell>
      <AccountCard
        eyebrow="ПТУ-СОШИАЛ"
        title="Вход в аккаунт"
        description="Введите данные учётной записи колледжа."
      >
        <form onSubmit={handleSubmit}>
          <Stack spacing={2}>
            {Boolean((location.state as { registrationSubmitted?: boolean } | null)?.registrationSubmitted) && (
              <Alert severity="success">Почта подтверждена. Заявка отправлена на рассмотрение учебной части.</Alert>
            )}
            <TextField
              label="Электронная почта"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              fullWidth
              InputProps={{ startAdornment: <InputAdornment position="start"><MailOutlineRoundedIcon fontSize="small" /></InputAdornment> }}
            />
            <TextField
              label="Пароль"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
              fullWidth
            />
            {error && <Alert severity="error">{error}</Alert>}
            <SubmitButton loading={loading}>Войти</SubmitButton>
            <Typography variant="body2" color="text.secondary" textAlign="center">
              Новый студент?{' '}
              <MuiLink component={Link} to="/register" underline="hover">Подать заявку</MuiLink>
            </Typography>
          </Stack>
        </form>
      </AccountCard>
    </AccountPageShell>
  );
}

export function StudentRegistrationPage() {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [faculties, setFaculties] = useState<FacultyOption[]>([]);
  const [facultyId, setFacultyId] = useState('');
  const [groups, setGroups] = useState<StudyGroupOption[]>([]);
  const [groupId, setGroupId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    void listFaculties().then((items) => {
      if (active) setFaculties(items);
    }).catch((loadError: unknown) => {
      if (active) setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить список факультетов.');
    });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!facultyId) {
      setGroups([]);
      setGroupId('');
      return;
    }
    let active = true;
    void listStudyGroups(facultyId).then((items) => {
      if (active) setGroups(items);
    }).catch((loadError: unknown) => {
      if (active) setError(loadError instanceof Error ? loadError.message : 'Не удалось загрузить учебные группы.');
    });
    return () => { active = false; };
  }, [facultyId]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!facultyId || !groupId) {
      setError('Выберите факультет и учебную группу.');
      return;
    }

    setError('');
    setLoading(true);
    try {
      const request = await createRegistrationRequest({ fullName, email, password, facultyId, groupId });
      window.sessionStorage.setItem(registrationRequestIdKey, request.id);
      window.sessionStorage.setItem(registrationEmailKey, request.email);
      navigate('/verify-email');
    } catch (requestError) {
      const message = axios.isAxiosError<ApiErrorResponse>(requestError) ? requestError.response?.data.message : undefined;
      setError(message || (requestError instanceof Error ? requestError.message : 'Не удалось отправить заявку.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AccountPageShell>
      <AccountCard
        eyebrow="СТУДЕНТ · РЕГИСТРАЦИЯ"
        title="Создать аккаунт"
        description="После подтверждения почты заявка поступит на проверку группы."
      >
        <form onSubmit={handleSubmit}>
          <Stack spacing={2}>
            <TextField
              label="ФИО"
              autoComplete="name"
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
              required
              fullWidth
            />
            <TextField
              label="Электронная почта"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              fullWidth
              InputProps={{ startAdornment: <InputAdornment position="start"><MailOutlineRoundedIcon fontSize="small" /></InputAdornment> }}
            />
            <TextField
              label="Пароль"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              inputProps={{ minLength: 8 }}
              helperText="Не менее 8 символов"
              required
              fullWidth
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      aria-label={showPassword ? 'Скрыть пароль' : 'Показать пароль'}
                      onClick={() => setShowPassword((visible) => !visible)}
                      edge="end"
                    >
                      {showPassword ? <VisibilityOffRoundedIcon /> : <VisibilityRoundedIcon />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            <Box className="account-select-grid">
              <FormControl required fullWidth>
                <InputLabel id="faculty-label">Факультет</InputLabel>
                <Select
                  labelId="faculty-label"
                  label="Факультет"
                  value={facultyId}
                  onChange={(event) => {
                    setFacultyId(event.target.value);
                    setGroupId('');
                  }}
                >
                  {faculties.map((facultyOption) => <MenuItem key={facultyOption.id} value={facultyOption.id}>{facultyOption.name}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl required fullWidth disabled={!facultyId}>
                <InputLabel id="group-label">Группа</InputLabel>
                <Select
                  labelId="group-label"
                  label="Группа"
                  value={groupId}
                  onChange={(event) => setGroupId(event.target.value)}
                >
                  {groups.map((groupOption) => <MenuItem key={groupOption.id} value={groupOption.id}>{groupOption.name}</MenuItem>)}
                </Select>
              </FormControl>
            </Box>
            {error && <Alert severity="error">{error}</Alert>}
            <SubmitButton loading={loading}>Продолжить</SubmitButton>
            <Typography variant="body2" color="text.secondary" textAlign="center">
              Приглашение преподавателю приходит на электронную почту.
            </Typography>
          </Stack>
        </form>
      </AccountCard>
    </AccountPageShell>
  );
}

export function VerifyEmailPage() {
  const navigate = useNavigate();
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [email] = useState(() => window.sessionStorage.getItem(registrationEmailKey) ?? '');
  const [requestId] = useState(() => window.sessionStorage.getItem(registrationRequestIdKey) ?? '');
  const [resending, setResending] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      await verifyRegistrationEmail(requestId, code);
      window.sessionStorage.removeItem(registrationEmailKey);
      window.sessionStorage.removeItem(registrationRequestIdKey);
      navigate('/login', { state: { registrationSubmitted: true }, replace: true });
    } catch (verificationError) {
      const message = axios.isAxiosError<ApiErrorResponse>(verificationError) ? verificationError.response?.data.message : undefined;
      setError(message || (verificationError instanceof Error ? verificationError.message : 'Не удалось подтвердить почту.'));
    } finally {
      setLoading(false);
    }
  }

  if (!email || !requestId) return <Navigate to="/register" replace />;

  async function handleResendCode() {
    if (resending) return;
    setError('');
    setResending(true);
    try {
      await resendRegistrationCode(requestId);
    } catch (resendError) {
      const message = axios.isAxiosError<ApiErrorResponse>(resendError) ? resendError.response?.data.message : undefined;
      setError(message || (resendError instanceof Error ? resendError.message : 'Не удалось отправить код повторно.'));
    } finally {
      setResending(false);
    }
  }

  return (
    <AccountPageShell>
      <AccountCard
        eyebrow="ПОДТВЕРЖДЕНИЕ ПОЧТЫ"
        title="Введите код"
        description={`Код отправлен на ${email}`}
      >
        <form onSubmit={handleSubmit}>
          <Stack spacing={2}>
            <TextField
              label="Код из письма"
              value={code}
              onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
              inputProps={{ inputMode: 'numeric', autoComplete: 'one-time-code', maxLength: 6, pattern: '[0-9]{6}' }}
              placeholder="000000"
              required
              fullWidth
            />
            {error && <Alert severity="error">{error}</Alert>}
            <SubmitButton loading={loading}>Подтвердить почту</SubmitButton>
            <Button disabled={resending || loading} onClick={() => void handleResendCode()}>
              {resending ? <CircularProgress size={18} /> : 'Отправить код повторно'}
            </Button>
            <Button component={Link} to="/register" startIcon={<ArrowBackRoundedIcon />} color="inherit">
              Вернуться к регистрации
            </Button>
          </Stack>
        </form>
      </AccountCard>
    </AccountPageShell>
  );
}

export function TeacherActivationPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { authenticate } = useAuth();
  const token = searchParams.get('token');
  const [invite, setInvite] = useState<TeacherInviteInfo | null>(null);
  const [inviteLoading, setInviteLoading] = useState(Boolean(token));
  const [inviteError, setInviteError] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!token) return;
    let active = true;
    void getTeacherInvite(token).then((inviteInfo) => {
      if (active) setInvite(inviteInfo);
    }).catch((loadError: unknown) => {
      if (active) setInviteError(loadError instanceof Error ? loadError.message : 'Приглашение недействительно или истекло.');
    }).finally(() => {
      if (active) setInviteLoading(false);
    });
    return () => { active = false; };
  }, [token]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (password !== confirmPassword) {
      setError('Пароли не совпадают.');
      return;
    }

    setError('');
    setLoading(true);
    try {
      if (!token) throw new Error('В ссылке приглашения отсутствует токен.');
      const tokenResponse = await acceptTeacherInvite(token, password);
      authenticate(tokenResponse, invite?.email ?? '');
      navigate('/teacher', { replace: true });
    } catch (activationError) {
      const message = axios.isAxiosError<ApiErrorResponse>(activationError) ? activationError.response?.data.message : undefined;
      setError(message || (activationError instanceof Error ? activationError.message : 'Не удалось активировать аккаунт.'));
    } finally {
      setLoading(false);
    }
  }

  const invitationIsValid = invite !== null;

  return (
    <AccountPageShell>
      <AccountCard
        eyebrow="УЧЁТНАЯ ЗАПИСЬ ПРЕПОДАВАТЕЛЯ"
        title={inviteLoading ? 'Проверяем приглашение' : invitationIsValid ? 'Задайте пароль' : 'Приглашение не найдено'}
        description={invitationIsValid ? `${invite.fullName} · ${invite.email}` : 'Откройте действующую ссылку из письма учебной части.'}
      >
        {inviteLoading ? <CircularProgress /> : !invitationIsValid ? (
          <Stack spacing={2}>
            <Alert severity="warning">{inviteError || 'Ссылка приглашения отсутствует или недействительна.'}</Alert>
            <Button component={Link} to="/login" startIcon={<ArrowBackRoundedIcon />} color="inherit">Ко входу</Button>
          </Stack>
        ) : (
          <form onSubmit={handleSubmit}>
            <Stack spacing={2}>
              <TextField
                label="Новый пароль"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                inputProps={{ minLength: 8 }}
                helperText="Не менее 8 символов"
                required
                fullWidth
                InputProps={{
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        aria-label={showPassword ? 'Скрыть пароль' : 'Показать пароль'}
                        onClick={() => setShowPassword((visible) => !visible)}
                        edge="end"
                      >
                        {showPassword ? <VisibilityOffRoundedIcon /> : <VisibilityRoundedIcon />}
                      </IconButton>
                    </InputAdornment>
                  ),
                }}
              />
              <TextField
                label="Повторите пароль"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                required
                fullWidth
              />
              {error && <Alert severity="error">{error}</Alert>}
              <SubmitButton loading={loading}>Активировать аккаунт</SubmitButton>
            </Stack>
          </form>
        )}
      </AccountCard>
    </AccountPageShell>
  );
}

export function RegistrationResultNotice({ submitted }: { submitted: boolean }) {
  return submitted ? (
    <Alert severity="success" className="registration-result">
      Почта подтверждена. Заявка отправлена на рассмотрение преподавателю вашей группы.
    </Alert>
  ) : null;
}

export function ActivationResultNotice({ activated }: { activated: boolean }) {
  return activated ? <Alert severity="success" className="registration-result">Учётная запись преподавателя активирована.</Alert> : null;
}
