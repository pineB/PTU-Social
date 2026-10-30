import { useState, type ReactNode } from 'react';
import {
  AppBar,
  Avatar,
  Badge,
  Box,
  Button,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Tooltip,
  Typography,
} from '@mui/material';
import MenuRoundedIcon from '@mui/icons-material/MenuRounded';
import NotificationsNoneRoundedIcon from '@mui/icons-material/NotificationsNoneRounded';
import SpaceDashboardRoundedIcon from '@mui/icons-material/SpaceDashboardRounded';
import DarkModeRoundedIcon from '@mui/icons-material/DarkModeRounded';
import LightModeRoundedIcon from '@mui/icons-material/LightModeRounded';
import SchoolRoundedIcon from '@mui/icons-material/SchoolRounded';
import MenuBookRoundedIcon from '@mui/icons-material/MenuBookRounded';
import AccountBalanceRoundedIcon from '@mui/icons-material/AccountBalanceRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import AssignmentRoundedIcon from '@mui/icons-material/AssignmentRounded';
import FactCheckRoundedIcon from '@mui/icons-material/FactCheckRounded';
import EventAvailableRoundedIcon from '@mui/icons-material/EventAvailableRounded';
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded';
import AccountBalanceWalletRoundedIcon from '@mui/icons-material/AccountBalanceWalletRounded';
import SportsEsportsRoundedIcon from '@mui/icons-material/SportsEsportsRounded';
import WorkspacePremiumRoundedIcon from '@mui/icons-material/WorkspacePremiumRounded';
import CalendarMonthRoundedIcon from '@mui/icons-material/CalendarMonthRounded';
import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';
import EmojiEventsRoundedIcon from '@mui/icons-material/EmojiEventsRounded';
import LeaderboardRoundedIcon from '@mui/icons-material/LeaderboardRounded';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { dashboards, type RoleId } from '../data/dashboards';
import { useCampusWorkflow } from '../data/campus-workflow';
import { useColorMode } from '../theme/ColorModeProvider';

const drawerWidth = 272;

const navigation: { id: RoleId; label: string }[] = [
  { id: 'student', label: 'Студент' },
  { id: 'teacher', label: 'Преподаватель' },
  { id: 'organizer', label: 'Организатор' },
  { id: 'academic-office', label: 'Учебная часть' },
];

function SidebarContent({ role, onNavigate }: { role: RoleId; onNavigate?: () => void }) {
  const location = useLocation();
  const { isGroupLeader } = useCampusWorkflow();
  const isActive = (path: string) => location.pathname === path;

  return (
    <Box className="sidebar-content">
      <Box className="brand">
        <Box className="brand-mark"><AutoAwesomeRoundedIcon /></Box>
        <Typography className="brand-name">ПТУ-Сошиал</Typography>
      </Box>

      <Box className="workspace-switcher">
        {navigation.map((item) => {
          const active = role === item.id;
          return (
            <Button
              key={item.id}
              component={NavLink}
              to={`/${item.id}`}
              onClick={onNavigate}
              className={`workspace-link${active ? ' workspace-link-active' : ''}`}
              aria-label={item.label}
              aria-current={active ? 'page' : undefined}
            >
              <Box className="workspace-icon">
                {item.id === 'student' ? <SchoolRoundedIcon /> : item.id === 'teacher' ? <MenuBookRoundedIcon /> : item.id === 'organizer' ? <EventAvailableRoundedIcon /> : <AccountBalanceRoundedIcon />}
              </Box>
              <Typography>{item.label}</Typography>
            </Button>
          );
        })}
      </Box>

      <List disablePadding className="sidebar-page-links">
        <ListItemButton component={NavLink} to={`/${role}`} onClick={onNavigate} className={`role-link${isActive(`/${role}`) ? ' role-link-active' : ''}`} aria-current={isActive(`/${role}`) ? 'page' : undefined}>
          <ListItemIcon className="role-icon"><SpaceDashboardRoundedIcon /></ListItemIcon>
          <ListItemText primary="Обзор" />
        </ListItemButton>
        {role === 'student' && (
          <ListItemButton
            component={NavLink}
            to="/student/tasks"
            onClick={onNavigate}
            className={`role-link${isActive('/student/tasks') ? ' role-link-active' : ''}`}
            aria-label="Задания"
            aria-current={isActive('/student/tasks') ? 'page' : undefined}
          >
            <ListItemIcon className="role-icon"><AssignmentRoundedIcon /></ListItemIcon>
            <ListItemText primary="Задания" primaryTypographyProps={{ fontWeight: location.pathname === '/student/tasks' ? 700 : 600, fontSize: 14 }} />
          </ListItemButton>
        )}
        {role === 'student' && (
          <ListItemButton
            component={NavLink}
            to="/student/profile"
            onClick={onNavigate}
            className={`role-link${isActive('/student/profile') ? ' role-link-active' : ''}`}
            aria-label="Профиль"
            aria-current={isActive('/student/profile') ? 'page' : undefined}
          >
            <ListItemIcon className="role-icon"><PersonRoundedIcon /></ListItemIcon>
            <ListItemText primary="Профиль" primaryTypographyProps={{ fontWeight: location.pathname === '/student/profile' ? 700 : 600, fontSize: 14 }} />
          </ListItemButton>
        )}
        {role === 'teacher' && (
          <ListItemButton
            component={NavLink}
            to="/teacher/students"
            onClick={onNavigate}
            className={`role-link${isActive('/teacher/students') ? ' role-link-active' : ''}`}
            aria-label="Мои студенты"
            aria-current={isActive('/teacher/students') ? 'page' : undefined}
          >
            <ListItemIcon className="role-icon"><GroupsRoundedIcon /></ListItemIcon>
            <ListItemText primary="Мои студенты" primaryTypographyProps={{ fontWeight: location.pathname === '/teacher/students' ? 700 : 600, fontSize: 14 }} />
          </ListItemButton>
        )}
        {role === 'teacher' && (
          <ListItemButton
            component={NavLink}
            to="/teacher/submissions"
            onClick={onNavigate}
            className={`role-link${isActive('/teacher/submissions') ? ' role-link-active' : ''}`}
            aria-label="Сдачи студентов"
            aria-current={isActive('/teacher/submissions') ? 'page' : undefined}
          >
            <ListItemIcon className="role-icon"><FactCheckRoundedIcon /></ListItemIcon>
            <ListItemText primary="Сдачи студентов" primaryTypographyProps={{ fontWeight: location.pathname === '/teacher/submissions' ? 700 : 600, fontSize: 14 }} />
          </ListItemButton>
        )}
        {(role === 'teacher' || role === 'organizer') && (
          <ListItemButton
            component={NavLink}
            to={role === 'teacher' ? '/teacher/events' : '/organizer/events'}
            onClick={onNavigate}
            className={`role-link${location.pathname.endsWith('/events') ? ' role-link-active' : ''}`}
            aria-label="Мероприятия и начисления"
            aria-current={location.pathname.endsWith('/events') ? 'page' : undefined}
          >
            <ListItemIcon className="role-icon"><EventAvailableRoundedIcon /></ListItemIcon>
            <ListItemText primary="Мероприятия" primaryTypographyProps={{ fontWeight: location.pathname.endsWith('/events') ? 700 : 600, fontSize: 14 }} />
          </ListItemButton>
        )}
        {role === 'academic-office' && (
          <>
            <ListItemButton component={NavLink} to="/academic-office/certificates" onClick={onNavigate} className={`role-link${isActive('/academic-office/certificates') ? ' role-link-active' : ''}`} aria-label="Обработка заявок на справки" aria-current={isActive('/academic-office/certificates') ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><FactCheckRoundedIcon /></ListItemIcon>
              <ListItemText primary="Заявки на справки" primaryTypographyProps={{ fontWeight: location.pathname === '/academic-office/certificates' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
            <ListItemButton component={NavLink} to="/academic-office/schedule" onClick={onNavigate} className={`role-link${isActive('/academic-office/schedule') ? ' role-link-active' : ''}`} aria-label="Расписание занятий" aria-current={isActive('/academic-office/schedule') ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><CalendarMonthRoundedIcon /></ListItemIcon>
              <ListItemText primary="Расписание" primaryTypographyProps={{ fontWeight: location.pathname === '/academic-office/schedule' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
          </>
        )}
        {role === 'student' && (
          <>
            <ListItemButton component={NavLink} to="/student/events" onClick={onNavigate} className={`role-link${isActive('/student/events') ? ' role-link-active' : ''}`} aria-label="Мероприятия" aria-current={isActive('/student/events') ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><EventAvailableRoundedIcon /></ListItemIcon>
              <ListItemText primary="Мероприятия" primaryTypographyProps={{ fontWeight: location.pathname === '/student/events' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
            {isGroupLeader && <ListItemButton component={NavLink} to="/student/group-confirmation" onClick={onNavigate} className={`role-link${isActive('/student/group-confirmation') ? ' role-link-active' : ''}`} aria-label="Заявки студентов группы" aria-current={isActive('/student/group-confirmation') ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><GroupsRoundedIcon /></ListItemIcon>
              <ListItemText primary="Заявки группы" primaryTypographyProps={{ fontWeight: location.pathname === '/student/group-confirmation' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>}
            <ListItemButton component={NavLink} to="/student/wallet" onClick={onNavigate} className={`role-link${isActive('/student/wallet') ? ' role-link-active' : ''}`} aria-label="Кошелёк" aria-current={isActive('/student/wallet') ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><AccountBalanceWalletRoundedIcon /></ListItemIcon>
              <ListItemText primary="Кошелёк" primaryTypographyProps={{ fontWeight: location.pathname === '/student/wallet' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
            <ListItemButton component={NavLink} to="/student/battle-pass" onClick={onNavigate} className={`role-link${isActive('/student/battle-pass') ? ' role-link-active' : ''}`} aria-label="Батл-пасс" aria-current={isActive('/student/battle-pass') ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><SportsEsportsRoundedIcon /></ListItemIcon>
              <ListItemText primary="Батл-пасс" primaryTypographyProps={{ fontWeight: location.pathname === '/student/battle-pass' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
            <ListItemButton component={NavLink} to="/student/inventory" onClick={onNavigate} className={`role-link${isActive('/student/inventory') ? ' role-link-active' : ''}`} aria-label="Инвентарь" aria-current={isActive('/student/inventory') ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><WorkspacePremiumRoundedIcon /></ListItemIcon>
              <ListItemText primary="Инвентарь" primaryTypographyProps={{ fontWeight: location.pathname === '/student/inventory' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
            <ListItemButton component={NavLink} to="/student/certificates" onClick={onNavigate} className={`role-link${isActive('/student/certificates') ? ' role-link-active' : ''}`} aria-label="Справки" aria-current={isActive('/student/certificates') ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><WorkspacePremiumRoundedIcon /></ListItemIcon>
              <ListItemText primary="Справки" primaryTypographyProps={{ fontWeight: location.pathname === '/student/certificates' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
          </>
        )}
        {[
          { path: '/student/awards', label: 'Лента наград', icon: <EmojiEventsRoundedIcon /> },
          { path: '/student/leaderboard', label: 'Таблица лидеров', icon: <LeaderboardRoundedIcon /> },
        ].map((item) => (
          <ListItemButton
            key={item.path}
            component={NavLink}
            to={item.path}
            onClick={onNavigate}
            className={`role-link${isActive(item.path) ? ' role-link-active' : ''}`}
            aria-current={isActive(item.path) ? 'page' : undefined}
          >
            <ListItemIcon className="role-icon">{item.icon}</ListItemIcon>
            <ListItemText primary={item.label} primaryTypographyProps={{ fontWeight: isActive(item.path) ? 700 : 600, fontSize: 14 }} />
          </ListItemButton>
        ))}
      </List>
    </Box>
  );
}

export default function AppLayout({ children }: { children?: ReactNode }) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();
  const role = navigation.find(({ id }) => location.pathname.startsWith(`/${id}`))?.id ?? 'student';
  const profile = dashboards[role];
  const pageTitles: Record<string, string> = {
    '/student/profile': 'Профиль',
    '/student/awards': 'Лента наград',
    '/student/leaderboard': 'Таблица лидеров',
    '/student/tasks': 'Задания',
    '/student/events': 'Мероприятия',
    '/student/group-confirmation': 'Заявки студентов группы',
    '/student/wallet': 'Кошелёк',
    '/student/battle-pass': 'Батл-пасс',
    '/student/inventory': 'Инвентарь',
    '/student/certificates': 'Справки',
    '/teacher/submissions': 'Сдачи студентов',
    '/teacher/students': 'Мои студенты',
    '/teacher/events': 'Мероприятия и баллы',
    '/organizer/events': 'Мероприятия и баллы',
    '/academic-office/certificates': 'Заявления на справки',
    '/academic-office/schedule': 'Расписание занятий',
  };
  const pageTitle = pageTitles[location.pathname]
    ?? (location.pathname.startsWith('/student/people/') ? 'Профиль студента' : profile.title);
  const profileInitials = profile.profileName
    .split(' ')
    .slice(0, 2)
    .map((part) => part[0])
    .join('');
  const closeMobile = () => setMobileOpen(false);
  const { mode, toggleMode } = useColorMode();
  const { walletBalance } = useCampusWorkflow();
  const ModeIcon = mode === 'dark' ? LightModeRoundedIcon : DarkModeRoundedIcon;
  const modeLabel = mode === 'dark' ? 'Включить светлую тему' : 'Включить тёмную тему';

  return (
    <Box className="app-shell">
      <Box component="nav" aria-label="Основная навигация">
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={closeMobile}
          ModalProps={{ keepMounted: true }}
          sx={{ display: { xs: 'block', md: 'none' }, '& .MuiDrawer-paper': { width: drawerWidth } }}
        >
          <SidebarContent role={role} onNavigate={closeMobile} />
        </Drawer>
        <Drawer
          variant="permanent"
          open
          sx={{ display: { xs: 'none', md: 'block' }, '& .MuiDrawer-paper': { width: drawerWidth } }}
        >
          <SidebarContent role={role} />
        </Drawer>
      </Box>

      <Box className="main-column" sx={{ ml: { md: `${drawerWidth}px` } }}>
        <AppBar position="sticky" color="inherit" elevation={0} className="topbar">
          <Toolbar className="topbar-inner">
            <IconButton
              aria-label="Открыть меню"
              onClick={() => setMobileOpen(true)}
              sx={{ display: { xs: 'inline-flex', md: 'none' }, mr: 1 }}
            >
              <MenuRoundedIcon />
            </IconButton>
            <Typography className="topbar-page-title" fontSize={14} fontWeight={650} color="text.secondary" sx={{ flexGrow: 1 }}>
              {pageTitle}
            </Typography>
            {role === 'student' && (
              <Button
                component={NavLink}
                to="/student/wallet"
                className="topbar-balance"
                startIcon={<AccountBalanceWalletRoundedIcon />}
                aria-label={`Баланс: ${walletBalance.toLocaleString('ru-RU')} жетонов. Открыть кошелёк`}
              >
                <span>{walletBalance.toLocaleString('ru-RU')}</span>
                <span className="topbar-balance-unit">жетонов</span>
              </Button>
            )}
            <Tooltip title={modeLabel}>
              <IconButton
                aria-label={modeLabel}
                onClick={toggleMode}
                className="theme-toggle"
              >
                <ModeIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Уведомления">
              <IconButton aria-label="Уведомления" className="notification-button">
                <Badge color="error" variant="dot" overlap="circular">
                  <NotificationsNoneRoundedIcon />
                </Badge>
              </IconButton>
            </Tooltip>
            {role === 'student' ? (
              <Button component={NavLink} to="/student/profile" className="topbar-user" aria-label={`Профиль: ${profile.profileName}`}>
                <Avatar className="topbar-avatar">{profileInitials}</Avatar>
                <Box className="topbar-user-copy">
                  <Typography variant="body2" fontWeight={700} noWrap>{profile.profileName}</Typography>
                  <Typography variant="caption" color="text.secondary" noWrap>{profile.profileCaption}</Typography>
                </Box>
              </Button>
            ) : (
              <Box className="topbar-user topbar-user-static" aria-label={profile.profileName}>
                <Avatar className="topbar-avatar">{profileInitials}</Avatar>
                <Box className="topbar-user-copy">
                  <Typography variant="body2" fontWeight={700} noWrap>{profile.profileName}</Typography>
                  <Typography variant="caption" color="text.secondary" noWrap>{profile.profileCaption}</Typography>
                </Box>
              </Box>
            )}
          </Toolbar>
        </AppBar>
        <Box component="main" className="page-content">
          {children ?? <Outlet />}
        </Box>
      </Box>
    </Box>
  );
}
