import { useMemo, useState } from 'react';
import {
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  InputAdornment,
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
import ArrowForwardRoundedIcon from '@mui/icons-material/ArrowForwardRounded';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import { dashboards, type DashboardData } from '../data/dashboards';

const focusCards = {
  student: {
    label: 'БАТЛ-ПАСС · СЕЗОН 2026/27',
    title: 'Уровень 34',
    description: 'Ещё 11 АР до следующего уровня',
    footnote: '689 из 700 АР',
    progress: 45,
  },
  teacher: {
    label: 'РАСПИСАНИЕ НА СЕГОДНЯ',
    title: '4 занятия',
    description: 'Первое занятие · 09:00',
    footnote: '12 работ на проверку',
  },
  'academic-office': {
    label: 'СПРАВКИ',
    title: '18 заявлений',
  },
} as const;

export default function DashboardPage({ data }: { data: DashboardData }) {
  const [search, setSearch] = useState('');
  const focus = focusCards[data.role];
  const filteredRows = useMemo(() => {
    const query = search.trim().toLocaleLowerCase('ru');
    if (!query) return data.rows;
    return data.rows.filter((row) =>
      `${row.subject} ${row.detail} ${row.status}`.toLocaleLowerCase('ru').includes(query),
    );
  }, [data.rows, search]);

  return (
    <Box className="dashboard">
      <Box className="welcome-row">
        <Box className="welcome-copy">
          <Typography variant="h4" className="welcome-title">{data.heading}</Typography>
        </Box>
        <Button href="#dashboard-table" variant="contained" endIcon={<ArrowForwardRoundedIcon />} className="primary-action">
          {data.role === 'student' ? 'Все задания' : data.role === 'teacher' ? 'Расписание' : 'Заявления'}
        </Button>
      </Box>

      <Box className="metrics-grid">
        {data.metrics.map((metric) => {
          const Icon = metric.icon;
          return (
            <Card key={metric.label} className="metric-card">
              <CardContent className="metric-card-content">
                <Box className="metric-topline">
                  <Typography color="text.secondary" fontSize={13} fontWeight={600}>{metric.label}</Typography>
                  <Avatar className="metric-icon" sx={{ color: metric.color, bgcolor: metric.background }}>
                    <Icon fontSize="small" />
                  </Avatar>
                </Box>
                <Typography variant="h5" className="metric-value">{metric.value}</Typography>
                <Typography variant="caption" color="text.secondary">{metric.detail}</Typography>
              </CardContent>
            </Card>
          );
        })}
      </Box>

      <Box className="content-grid">
        <Card className="schedule-card" id="dashboard-table">
          <CardContent className="schedule-card-content">
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ sm: 'center' }} justifyContent="space-between" mb={2.5}>
              <Box>
                <Typography variant="h6">{data.tableTitle}</Typography>
              </Box>
              <TextField
                size="small"
                placeholder="Поиск..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                className="table-search"
                inputProps={{ 'aria-label': 'Поиск по таблице' }}
                InputProps={{
                  startAdornment: <InputAdornment position="start"><SearchRoundedIcon fontSize="small" /></InputAdornment>,
                }}
              />
            </Stack>
            <TableContainer>
              <Table aria-label={data.tableTitle}>
                <TableHead>
                  <TableRow>
                    <TableCell>{data.subjectHeading}</TableCell>
                    <TableCell>{data.detailHeading}</TableCell>
                    <TableCell align="right">
                      {data.role === 'student' ? 'Срок / статус' : data.role === 'teacher' ? 'Время' : 'Статус'}
                    </TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {filteredRows.map((row) => (
                    <TableRow key={`${row.subject}-${row.status}`} hover>
                      <TableCell>
                        <Typography fontSize={13.5} fontWeight={650}>{row.subject}</Typography>
                      </TableCell>
                      <TableCell>
                        <Typography fontSize={12.5} color="text.secondary">{row.detail}</Typography>
                      </TableCell>
                      <TableCell align="right">
                        <Chip size="small" label={row.status} className={`status-chip status-${row.tone}`} />
                      </TableCell>
                    </TableRow>
                  ))}
                  {filteredRows.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={3} align="center">
                        <Typography color="text.secondary" py={2}>Ничего не найдено</Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </CardContent>
        </Card>

        <Card className="focus-card">
          <CardContent className="focus-card-content">
            <Box className="focus-card-orbit" aria-hidden="true" />
            <Typography className="focus-label">{focus.label}</Typography>
            <Typography variant="h5" className="focus-title">{focus.title}</Typography>
            {'description' in focus && (
              <Typography color="text.secondary" variant="body2" lineHeight={1.7} mt={0.8}>
                {focus.description}
              </Typography>
            )}
            {'progress' in focus && (
              <Box className="progress-block">
                <Box className="progress-track"><Box className="progress-fill" style={{ width: `${focus.progress}%` }} /></Box>
              </Box>
            )}
            {'footnote' in focus && <Typography className="focus-footnote">{focus.footnote}</Typography>}
            <Box className="focus-decoration" aria-hidden="true">
              <Box /><Box /><Box /><Box /><Box /><Box />
            </Box>
          </CardContent>
        </Card>
      </Box>
    </Box>
  );
}

export function StudentDashboard() {
  return <DashboardPage data={dashboards.student} />;
}

export function TeacherDashboard() {
  return <DashboardPage data={dashboards.teacher} />;
}

export function AcademicOfficeDashboard() {
  return <DashboardPage data={dashboards['academic-office']} />;
}
