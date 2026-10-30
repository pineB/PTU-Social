import { useMemo, useState, type FormEvent, type ReactNode } from 'react';
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
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { useColorMode } from '../theme/ColorModeProvider';

const demoVerificationCode = '246810';
const demoInvitationToken = 'demo-invite-2026';
const registrationEmailKey = 'ptu-social-registration-email';

const groupsByFaculty: Record<string, string[]> = {
  'Информационных технологий': ['ИС-21', 'ИС-22', 'ИС-23', 'ИС-24'],
  Дизайна: ['ДИ-21', 'ДИ-22', 'ДИ-23', 'ДИ-24'],
  Экономики: ['ЭК-21', 'ЭК-22', 'ЭК-23', 'ЭК-24'],
};

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

export function StudentRegistrationPage() {
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [faculty, setFaculty] = useState('');
  const [group, setGroup] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const groups = useMemo(() => groupsByFaculty[faculty] ?? [], [faculty]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!faculty || !group) {
      setError('Выберите факультет и учебную группу.');
      return;
    }

    setError('');
    setLoading(true);
    await new Promise<void>((resolve) => window.setTimeout(resolve, 450));
    window.sessionStorage.setItem(registrationEmailKey, email.trim());
    setLoading(false);
    navigate('/verify-email');
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
                  value={faculty}
                  onChange={(event) => {
                    setFaculty(event.target.value);
                    setGroup('');
                  }}
                >
                  {Object.keys(groupsByFaculty).map((name) => <MenuItem key={name} value={name}>{name}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl required fullWidth disabled={!faculty}>
                <InputLabel id="group-label">Группа</InputLabel>
                <Select
                  labelId="group-label"
                  label="Группа"
                  value={group}
                  onChange={(event) => setGroup(event.target.value)}
                >
                  {groups.map((name) => <MenuItem key={name} value={name}>{name}</MenuItem>)}
                </Select>
              </FormControl>
            </Box>
            {error && <Alert severity="error">{error}</Alert>}
            <Alert severity="info" icon={false} className="demo-notice">
              Демонстрационный режим: заявка и подтверждение почты пока работают без сервера.
            </Alert>
            <SubmitButton loading={loading}>Продолжить</SubmitButton>
            <Typography variant="body2" color="text.secondary" textAlign="center">
              Преподаватель?{' '}
              <MuiLink component={Link} to={`/teacher/activate?token=${demoInvitationToken}`} underline="hover">
                Активировать приглашение
              </MuiLink>
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

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (code !== demoVerificationCode) {
      setError('Код не подошёл. Проверьте его и попробуйте ещё раз.');
      return;
    }

    setError('');
    setLoading(true);
    await new Promise<void>((resolve) => window.setTimeout(resolve, 400));
    window.sessionStorage.removeItem(registrationEmailKey);
    setLoading(false);
    navigate('/student', { state: { registrationSubmitted: true } });
  }

  if (!email) return <Navigate to="/register" replace />;

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
            <Alert severity="info" icon={false} className="demo-notice">
              Демо-код: <strong>{demoVerificationCode}</strong> · в рабочей версии код придёт на почту.
            </Alert>
            <SubmitButton loading={loading}>Подтвердить почту</SubmitButton>
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
  const token = searchParams.get('token');
  const invitationIsValid = token === demoInvitationToken;
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (password !== confirmPassword) {
      setError('Пароли не совпадают.');
      return;
    }

    setError('');
    setLoading(true);
    await new Promise<void>((resolve) => window.setTimeout(resolve, 450));
    setLoading(false);
    navigate('/teacher', { state: { invitationActivated: true } });
  }

  return (
    <AccountPageShell>
      <AccountCard
        eyebrow="УЧЁТНАЯ ЗАПИСЬ ПРЕПОДАВАТЕЛЯ"
        title={invitationIsValid ? 'Задайте пароль' : 'Приглашение не найдено'}
        description={invitationIsValid ? 'Установите пароль, чтобы активировать учётную запись.' : 'Откройте ссылку из приглашения учебной части.'}
      >
        {!invitationIsValid ? (
          <Stack spacing={2}>
            <Alert severity="warning">Ссылка приглашения отсутствует или недействительна.</Alert>
            <Button
              component={Link}
              to={`/teacher/activate?token=${demoInvitationToken}`}
              variant="contained"
              endIcon={<ArrowForwardRoundedIcon />}
              fullWidth
            >
              Открыть демонстрационное приглашение
            </Button>
            <Button component={Link} to="/student" startIcon={<ArrowBackRoundedIcon />} color="inherit">
              На главную
            </Button>
          </Stack>
        ) : (
          <form onSubmit={handleSubmit}>
            <Stack spacing={2}>
              <Alert severity="info" icon={false} className="demo-notice">
                Демонстрационное приглашение · Кафедра информатики
              </Alert>
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
  return activated ? (
    <Alert severity="success" className="registration-result">
      Приглашение активировано. Создание учётной записи будет доступно после подключения сервера.
    </Alert>
  ) : null;
}
