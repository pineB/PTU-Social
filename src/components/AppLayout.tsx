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
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { dashboards, type RoleId } from '../data/dashboards';
import { useColorMode } from '../theme/ColorModeProvider';

const drawerWidth = 256;

const navigation: { id: RoleId; label: string }[] = [
  { id: 'student', label: 'Студент' },
  { id: 'teacher', label: 'Преподаватель' },
  { id: 'academic-office', label: 'Учебная часть' },
];

function SidebarContent({ role, onNavigate }: { role: RoleId; onNavigate?: () => void }) {
  const location = useLocation();
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
          const active = location.pathname.startsWith(`/${item.id}`);
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
                {item.id === 'student' ? <SchoolRoundedIcon /> : item.id === 'teacher' ? <MenuBookRoundedIcon /> : <AccountBalanceRoundedIcon />}
              </ListItemIcon>
              <ListItemText
                primary={item.label}
                primaryTypographyProps={{ fontWeight: active ? 700 : 600, fontSize: 14 }}
              />
            </ListItemButton>
          );
        })}
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
  const pageTitle = profile.title;
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
