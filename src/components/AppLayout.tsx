import { useState, type ReactNode } from 'react';
import {
  AppBar,
  Avatar,
  Badge,
  Box,
  Divider,
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
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { dashboards, type RoleId } from '../data/dashboards';
import { useCampusWorkflow } from '../data/campus-workflow';
import { useColorMode } from '../theme/ColorModeProvider';

const drawerWidth = 256;

const navigation: { id: RoleId; label: string }[] = [
  { id: 'student', label: 'Студент' },
  { id: 'teacher', label: 'Преподаватель' },
  { id: 'organizer', label: 'Организатор' },
  { id: 'academic-office', label: 'Учебная часть' },
];

function SidebarContent({ role, onNavigate }: { role: RoleId; onNavigate?: () => void }) {
  const location = useLocation();
  const { isGroupLeader } = useCampusWorkflow();
  const profile = dashboards[role];
  const initials = profile.profileName
    .split(' ')
    .slice(0, 2)
    .map((part) => part[0])
    .join('');

  return (
    <Box className="sidebar-content">
      <Box className="brand">
        <Box className="brand-mark"><SpaceDashboardRoundedIcon /></Box>
        <Typography className="brand-name">ПТУ-Сошиал</Typography>
      </Box>

      <List disablePadding>
        {navigation.map((item) => {
          const active = location.pathname === `/${item.id}`;
          return (
            <ListItemButton
              key={item.id}
              component={NavLink}
              to={`/${item.id}`}
              onClick={onNavigate}
              className={`role-link${active ? ' role-link-active' : ''}`}
              aria-label={item.label}
              aria-current={active ? 'page' : undefined}
            >
              <ListItemIcon className="role-icon">
                {item.id === 'student' ? <SchoolRoundedIcon /> : item.id === 'teacher' ? <MenuBookRoundedIcon /> : item.id === 'organizer' ? <EventAvailableRoundedIcon /> : <AccountBalanceRoundedIcon />}
              </ListItemIcon>
              <ListItemText
                primary={item.label}
                primaryTypographyProps={{ fontWeight: active ? 700 : 600, fontSize: 14 }}
              />
            </ListItemButton>
          );
        })}
        {role === 'student' && (
          <ListItemButton
            component={NavLink}
            to="/student/tasks"
            onClick={onNavigate}
            className={`role-link${location.pathname === '/student/tasks' ? ' role-link-active' : ''}`}
            aria-label="Задания"
            aria-current={location.pathname === '/student/tasks' ? 'page' : undefined}
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
            className={`role-link${location.pathname === '/student/profile' ? ' role-link-active' : ''}`}
            aria-label="Профиль"
            aria-current={location.pathname === '/student/profile' ? 'page' : undefined}
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
            className={`role-link${location.pathname === '/teacher/students' ? ' role-link-active' : ''}`}
            aria-label="Мои студенты"
            aria-current={location.pathname === '/teacher/students' ? 'page' : undefined}
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
            className={`role-link${location.pathname === '/teacher/submissions' ? ' role-link-active' : ''}`}
            aria-label="Сдачи студентов"
            aria-current={location.pathname === '/teacher/submissions' ? 'page' : undefined}
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
            <ListItemButton component={NavLink} to="/academic-office/certificates" onClick={onNavigate} className={`role-link${location.pathname === '/academic-office/certificates' ? ' role-link-active' : ''}`} aria-label="Обработка заявок на справки" aria-current={location.pathname === '/academic-office/certificates' ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><FactCheckRoundedIcon /></ListItemIcon>
              <ListItemText primary="Заявки на справки" primaryTypographyProps={{ fontWeight: location.pathname === '/academic-office/certificates' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
            <ListItemButton component={NavLink} to="/academic-office/schedule" onClick={onNavigate} className={`role-link${location.pathname === '/academic-office/schedule' ? ' role-link-active' : ''}`} aria-label="Расписание занятий" aria-current={location.pathname === '/academic-office/schedule' ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><CalendarMonthRoundedIcon /></ListItemIcon>
              <ListItemText primary="Расписание" primaryTypographyProps={{ fontWeight: location.pathname === '/academic-office/schedule' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
          </>
        )}
        {role === 'student' && (
          <>
            <ListItemButton component={NavLink} to="/student/events" onClick={onNavigate} className={`role-link${location.pathname === '/student/events' ? ' role-link-active' : ''}`} aria-label="Мероприятия" aria-current={location.pathname === '/student/events' ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><EventAvailableRoundedIcon /></ListItemIcon>
              <ListItemText primary="Мероприятия" primaryTypographyProps={{ fontWeight: location.pathname === '/student/events' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
            {isGroupLeader && <ListItemButton component={NavLink} to="/student/group-confirmation" onClick={onNavigate} className={`role-link${location.pathname === '/student/group-confirmation' ? ' role-link-active' : ''}`} aria-label="Заявки студентов группы" aria-current={location.pathname === '/student/group-confirmation' ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><GroupsRoundedIcon /></ListItemIcon>
              <ListItemText primary="Заявки группы" primaryTypographyProps={{ fontWeight: location.pathname === '/student/group-confirmation' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>}
            <ListItemButton component={NavLink} to="/student/wallet" onClick={onNavigate} className={`role-link${location.pathname === '/student/wallet' ? ' role-link-active' : ''}`} aria-label="Кошелёк" aria-current={location.pathname === '/student/wallet' ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><AccountBalanceWalletRoundedIcon /></ListItemIcon>
              <ListItemText primary="Кошелёк" primaryTypographyProps={{ fontWeight: location.pathname === '/student/wallet' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
            <ListItemButton component={NavLink} to="/student/battle-pass" onClick={onNavigate} className={`role-link${location.pathname === '/student/battle-pass' ? ' role-link-active' : ''}`} aria-label="Батл-пасс" aria-current={location.pathname === '/student/battle-pass' ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><SportsEsportsRoundedIcon /></ListItemIcon>
              <ListItemText primary="Батл-пасс" primaryTypographyProps={{ fontWeight: location.pathname === '/student/battle-pass' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
            <ListItemButton component={NavLink} to="/student/inventory" onClick={onNavigate} className={`role-link${location.pathname === '/student/inventory' ? ' role-link-active' : ''}`} aria-label="Инвентарь" aria-current={location.pathname === '/student/inventory' ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><WorkspacePremiumRoundedIcon /></ListItemIcon>
              <ListItemText primary="Инвентарь" primaryTypographyProps={{ fontWeight: location.pathname === '/student/inventory' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
            <ListItemButton component={NavLink} to="/student/certificates" onClick={onNavigate} className={`role-link${location.pathname === '/student/certificates' ? ' role-link-active' : ''}`} aria-label="Справки" aria-current={location.pathname === '/student/certificates' ? 'page' : undefined}>
              <ListItemIcon className="role-icon"><WorkspacePremiumRoundedIcon /></ListItemIcon>
              <ListItemText primary="Справки" primaryTypographyProps={{ fontWeight: location.pathname === '/student/certificates' ? 700 : 600, fontSize: 14 }} />
            </ListItemButton>
          </>
        )}
      </List>

      <Box className="sidebar-bottom">
        <Divider />
        <Box className="sidebar-profile">
          <Avatar className="profile-avatar">{initials}</Avatar>
          <Box sx={{ minWidth: 0 }}>
            <Typography fontSize={13} fontWeight={700} noWrap>{profile.profileName}</Typography>
            <Typography variant="caption" color="text.secondary" noWrap>{profile.profileCaption}</Typography>
          </Box>
        </Box>
      </Box>
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
  const pageTitle = pageTitles[location.pathname] ?? profile.title;
  const profileInitials = profile.profileName
    .split(' ')
    .slice(0, 2)
    .map((part) => part[0])
    .join('');
  const closeMobile = () => setMobileOpen(false);
  const { mode, toggleMode } = useColorMode();
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
            <Typography fontSize={14} fontWeight={650} color="text.secondary" sx={{ flexGrow: 1 }}>
              {pageTitle}
            </Typography>
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
            <Avatar className="topbar-avatar">{profileInitials}</Avatar>
          </Toolbar>
        </AppBar>
        <Box component="main" className="page-content">
          {children ?? <Outlet />}
        </Box>
      </Box>
    </Box>
  );
}
