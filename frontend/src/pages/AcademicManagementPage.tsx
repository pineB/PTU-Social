import { useEffect, useState, type FormEvent } from 'react';
import axios from 'axios';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Checkbox,
  Chip,
  CircularProgress,
  Divider,
  FormControl,
  IconButton,
  InputLabel,
  ListItemText,
  MenuItem,
  Select,
  Stack,
  Tab,
  Tabs,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import ArchiveRoundedIcon from '@mui/icons-material/ArchiveRounded';
import GroupAddRoundedIcon from '@mui/icons-material/GroupAddRounded';
import PersonAddAltRoundedIcon from '@mui/icons-material/PersonAddAltRounded';
import SaveRoundedIcon from '@mui/icons-material/SaveRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import { archiveTeacherAssignment, createAcademicDepartment, createAcademicDiscipline, createAcademicFaculty, createManagedGroup, createTeacherAssignment, createTeacherInvite, deleteAcademicDepartment, deleteAcademicDiscipline, deleteAcademicFaculty, deleteManagedGroup, getAcademicDepartments, getAcademicDisciplines, getAcademicFaculties, listManagedGroups, listManagedTeachers, renameAcademicDepartment, renameManagedGroup, setManagedTeacherActive, type CatalogItem, type ManagedGroup, type ManagedTeacher } from '../api/administration';

interface ApiErrorResponse {
  message?: string;
}

function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    return error.response?.data.message ?? error.message;
  }
  return error instanceof Error ? error.message : 'Не удалось выполнить запрос.';
}

export default function AcademicManagementPage() {
  const [faculties, setFaculties] = useState<CatalogItem[]>([]);
  const [departments, setDepartments] = useState<CatalogItem[]>([]);
  const [disciplines, setDisciplines] = useState<CatalogItem[]>([]);
  const [groups, setGroups] = useState<ManagedGroup[]>([]);
  const [teachers, setTeachers] = useState<ManagedTeacher[]>([]);
  const [loading, setLoading] = useState(true);
  const [disciplinesLoading, setDisciplinesLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [managementTab, setManagementTab] = useState<'structure' | 'teachers'>('structure');
  const [structureTab, setStructureTab] = useState<'groups' | 'faculties' | 'departments' | 'disciplines'>('groups');

  const [groupName, setGroupName] = useState('');
  const [facultyId, setFacultyId] = useState('');
  const [newFacultyName, setNewFacultyName] = useState('');
  const [newDepartmentName, setNewDepartmentName] = useState('');
  const [newDisciplineName, setNewDisciplineName] = useState('');
  const [renamedGroups, setRenamedGroups] = useState<Record<string, string>>({});
  const [renamedDepartments, setRenamedDepartments] = useState<Record<string, string>>({});

  const [teacherName, setTeacherName] = useState('');
  const [teacherEmail, setTeacherEmail] = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [inviteGroupIds, setInviteGroupIds] = useState<string[]>([]);
  const [inviteDisciplineIds, setInviteDisciplineIds] = useState<string[]>([]);

  const [assignmentTeacherId, setAssignmentTeacherId] = useState('');
  const [assignmentGroupId, setAssignmentGroupId] = useState('');
  const [assignmentDisciplineId, setAssignmentDisciplineId] = useState('');

  async function loadData() {
    setLoading(true);
    setError('');
    try {
      const [facultyItems, departmentItems, groupItems, teacherItems] = await Promise.all([
        getAcademicFaculties(),
        getAcademicDepartments(),
        listManagedGroups(),
        listManagedTeachers(),
      ]);
      setFaculties(facultyItems);
      setDepartments(departmentItems);
      setRenamedDepartments(Object.fromEntries(departmentItems.map((department) => [department.id, department.name])));
      setGroups(groupItems);
      setTeachers(teacherItems);
      setRenamedGroups(Object.fromEntries(groupItems.map((group) => [group.id, group.name])));
      if (!facultyId && facultyItems.length) setFacultyId(facultyItems[0].id);
      if (!departmentId && departmentItems.length) setDepartmentId(departmentItems[0].id);
    } catch (loadError: unknown) {
      setError(getErrorMessage(loadError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  useEffect(() => {
    let active = true;
    setInviteDisciplineIds([]);
    setAssignmentDisciplineId('');
    if (!departmentId) {
      setDisciplines([]);
      return () => { active = false; };
    }
    setDisciplinesLoading(true);
    setError('');
    void getAcademicDisciplines(departmentId)
      .then((items) => {
        if (active) setDisciplines(items);
      })
      .catch((loadError: unknown) => {
        if (active) setError(getErrorMessage(loadError));
      })
      .finally(() => {
        if (active) setDisciplinesLoading(false);
      });
    return () => { active = false; };
  }, [departmentId]);

  async function refreshTeachersAndGroups() {
    const [groupItems, teacherItems] = await Promise.all([listManagedGroups(), listManagedTeachers()]);
    setGroups(groupItems);
    setTeachers(teacherItems);
    setRenamedGroups(Object.fromEntries(groupItems.map((group) => [group.id, group.name])));
  }

  async function runAction(action: () => Promise<void>, successMessage: string) {
    if (busy) return;
    setBusy(true);
    setError('');
    setSuccess('');
    try {
      await action();
      setSuccess(successMessage);
    } catch (actionError: unknown) {
      setError(getErrorMessage(actionError));
    } finally {
      setBusy(false);
    }
  }

  async function handleCreateGroup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(async () => {
      const created = await createManagedGroup(groupName.trim(), facultyId);
      setGroups((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name, 'ru')));
      setRenamedGroups((current) => ({ ...current, [created.id]: created.name }));
      setGroupName('');
    }, 'Учебная группа создана.');
  }

  async function handleCreateFaculty(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(async () => {
      const created = await createAcademicFaculty(newFacultyName.trim());
      setFaculties((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name, 'ru')));
      setFacultyId(created.id);
      setNewFacultyName('');
    }, 'Факультет создан.');
  }

  async function handleCreateDepartment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction(async () => {
      const created = await createAcademicDepartment(newDepartmentName.trim());
      setDepartments((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name, 'ru')));
      setRenamedDepartments((current) => ({ ...current, [created.id]: created.name }));
      setDepartmentId(created.id);
      setNewDepartmentName('');
    }, 'Кафедра создана.');
  }

  async function handleDeleteFaculty(faculty: CatalogItem) {
    if (!window.confirm(`Удалить факультет «${faculty.name}»? Удалить факультет с учебными группами нельзя.`)) return;
    await runAction(async () => {
      await deleteAcademicFaculty(faculty.id);
      setFaculties((current) => current.filter((item) => item.id !== faculty.id));
      setFacultyId((current) => current === faculty.id ? '' : current);
    }, 'Факультет удалён.');
  }

  async function handleDeleteGroup(group: ManagedGroup) {
    if (!window.confirm(`Удалить учебную группу «${group.name}»?`)) return;
    await runAction(async () => {
      await deleteManagedGroup(group.id);
      setGroups((current) => current.filter((item) => item.id !== group.id));
      setRenamedGroups((current) => {
        const next = { ...current };
        delete next[group.id];
        return next;
      });
      setInviteGroupIds((current) => current.filter((id) => id !== group.id));
      setAssignmentGroupId((current) => current === group.id ? '' : current);
    }, `Группа ${group.name} удалена.`);
  }

  async function handleRenameGroup(groupId: string) {
    const name = renamedGroups[groupId]?.trim();
    if (!name) {
      setError('Укажите название группы.');
      return;
    }
    await runAction(async () => {
      const updated = await renameManagedGroup(groupId, name);
      setGroups((current) => current.map((group) => group.id === updated.id ? updated : group));
      setRenamedGroups((current) => ({ ...current, [updated.id]: updated.name }));
    }, 'Название группы сохранено.');
  }

  async function handleRenameDepartment(departmentId: string) {
    const name = renamedDepartments[departmentId]?.trim();
    if (!name) {
      setError('Укажите название кафедры.');
      return;
    }
    await runAction(async () => {
      const updated = await renameAcademicDepartment(departmentId, name);
      setDepartments((current) => current.map((department) => department.id === updated.id ? updated : department)
        .sort((a, b) => a.name.localeCompare(b.name, 'ru')));
      setRenamedDepartments((current) => ({ ...current, [updated.id]: updated.name }));
    }, 'Название кафедры сохранено.');
  }

  async function handleDeleteDepartment(department: CatalogItem) {
    if (!window.confirm(`Удалить кафедру «${department.name}»? Удаление доступно, только если к кафедре не привязаны дисциплины или приглашения преподавателей.`)) return;
    await runAction(async () => {
      await deleteAcademicDepartment(department.id);
      const remainingDepartments = departments.filter((item) => item.id !== department.id);
      setDepartments(remainingDepartments);
      setRenamedDepartments((current) => {
        const next = { ...current };
        delete next[department.id];
        return next;
      });
      setDepartmentId((current) => current === department.id ? remainingDepartments[0]?.id ?? '' : current);
    }, 'Кафедра удалена.');
  }

  async function handleCreateDiscipline(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!departmentId) {
      setError('Сначала выберите кафедру.');
      return;
    }
    await runAction(async () => {
      const created = await createAcademicDiscipline(departmentId, newDisciplineName.trim());
      if (created.departmentId === departmentId) {
        setDisciplines((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name, 'ru')));
      }
      setNewDisciplineName('');
    }, 'Дисциплина создана.');
  }

  async function handleDeleteDiscipline(discipline: CatalogItem) {
    if (!window.confirm(`Удалить дисциплину «${discipline.name}»? Если она используется в назначениях, заданиях или расписании, удалить её не получится.`)) return;
    await runAction(async () => {
      await deleteAcademicDiscipline(discipline.id);
      setDisciplines((current) => current.filter((item) => item.id !== discipline.id));
      setInviteDisciplineIds((current) => current.filter((id) => id !== discipline.id));
      setAssignmentDisciplineId((current) => current === discipline.id ? '' : current);
    }, 'Дисциплина удалена.');
  }

  async function handleInviteTeacher(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!inviteGroupIds.length || !inviteDisciplineIds.length) {
      setError('Для приглашения выберите хотя бы одну группу и дисциплину.');
      return;
    }
    await runAction(async () => {
      await createTeacherInvite({
        fullName: teacherName.trim(),
        email: teacherEmail.trim(),
        departmentId,
        disciplineIds: inviteDisciplineIds,
        groupIds: inviteGroupIds,
      });
      setTeacherName('');
      setTeacherEmail('');
      setInviteGroupIds([]);
      setInviteDisciplineIds([]);
    }, 'Приглашение преподавателю отправлено.');
  }

  async function handleCreateAssignment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!assignmentTeacherId || !assignmentGroupId || !assignmentDisciplineId) {
      setError('Выберите преподавателя, группу и дисциплину.');
      return;
    }
    await runAction(async () => {
      await createTeacherAssignment(assignmentTeacherId, assignmentGroupId, assignmentDisciplineId);
      await refreshTeachersAndGroups();
    }, 'Группа и дисциплина закреплены за преподавателем.');
  }

  async function toggleTeacher(teacher: ManagedTeacher) {
    await runAction(async () => {
      const updated = await setManagedTeacherActive(teacher.id, !teacher.active);
      setTeachers((current) => current.map((item) => item.id === teacher.id ? updated : item));
    }, teacher.active ? 'Доступ преподавателя отключён.' : 'Доступ преподавателя восстановлен.');
  }

  async function removeAssignment(teacherId: string, assignmentId: string) {
    await runAction(async () => {
      await archiveTeacherAssignment(teacherId, assignmentId);
      setTeachers((current) => current.map((teacher) => teacher.id === teacherId
        ? { ...teacher, assignments: teacher.assignments.filter((assignment) => assignment.id !== assignmentId) }
        : teacher));
    }, 'Назначение группы архивировано.');
  }

  if (loading) {
    return <Box display="grid" minHeight={240} sx={{ placeItems: 'center' }}><CircularProgress aria-label="Загрузка учебной структуры" /></Box>;
  }

  return (
    <Box className="management-page">
      <Stack spacing={0.75} mb={3}>
        <Typography variant="h4" fontWeight={800}>Группы и преподаватели</Typography>
        <Typography color="text.secondary">Создавайте учебные группы, приглашайте преподавателей и управляйте их назначениями.</Typography>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}

      <Tabs value={managementTab} onChange={(_, value: 'structure' | 'teachers') => setManagementTab(value)} aria-label="Разделы управления">
        <Tab value="structure" label="Учебная структура" />
        <Tab value="teachers" label="Преподаватели" />
      </Tabs>
      <Box display="grid" gridTemplateColumns="minmax(0, 1fr)" gap={2} mt={2}>
        <Card sx={{ display: managementTab === 'structure' ? undefined : 'none' }}>
          <CardContent>
            <Tabs
              value={structureTab}
              onChange={(_, value: 'groups' | 'faculties' | 'departments' | 'disciplines') => setStructureTab(value)}
              variant="scrollable"
              scrollButtons="auto"
              aria-label="Управление учебной структурой"
              sx={{ mb: 2 }}
            >
              <Tab value="groups" label={`Группы${groups.length ? ` · ${groups.length}` : ''}`} />
              <Tab value="faculties" label={`Факультеты${faculties.length ? ` · ${faculties.length}` : ''}`} />
              <Tab value="departments" label={`Кафедры${departments.length ? ` · ${departments.length}` : ''}`} />
              <Tab value="disciplines" label={`Дисциплины${disciplines.length ? ` · ${disciplines.length}` : ''}`} />
            </Tabs>
            <Box sx={{ display: structureTab === 'groups' ? undefined : 'none', mt: 1 }}>
            <Stack direction="row" spacing={1.25} alignItems="center" mb={2}>
              <GroupAddRoundedIcon color="primary" />
              <Typography variant="h6" fontWeight={750}>Создание учебной группы</Typography>
            </Stack>
            <Box
              component="form"
              onSubmit={(event) => void handleCreateGroup(event)}
              display="grid"
              gridTemplateColumns={{ xs: '1fr', sm: 'minmax(0, 1fr) minmax(0, 1fr) auto' }}
              gap={1.5}
              alignItems="start"
            >
                <TextField label="Название группы" value={groupName} onChange={(event) => setGroupName(event.target.value)} required fullWidth />
                <FormControl fullWidth required>
                  <InputLabel id="group-faculty-label">Факультет</InputLabel>
                  <Select labelId="group-faculty-label" label="Факультет" value={facultyId} onChange={(event) => setFacultyId(event.target.value)} disabled={!faculties.length}>
                    {faculties.map((faculty) => <MenuItem key={faculty.id} value={faculty.id}>{faculty.name}</MenuItem>)}
                  </Select>
                </FormControl>
                <Button type="submit" variant="contained" disabled={busy || !faculties.length} startIcon={busy ? <CircularProgress size={18} color="inherit" /> : <AddRoundedIcon />}>Создать группу</Button>
            </Box>
            </Box>

            <Box sx={{ display: structureTab === 'faculties' ? undefined : 'none', mt: 1 }}>
            <Typography variant="subtitle1" fontWeight={750} mb={1}>Факультеты</Typography>
            <Box component="form" onSubmit={(event) => void handleCreateFaculty(event)} display="grid" gridTemplateColumns={{ xs: '1fr', sm: 'minmax(0, 1fr) auto' }} gap={1} mb={2}>
              <TextField label="Название факультета" value={newFacultyName} onChange={(event) => setNewFacultyName(event.target.value)} required fullWidth />
              <Button type="submit" variant="outlined" disabled={busy} startIcon={busy ? <CircularProgress size={18} /> : <AddRoundedIcon />}>Добавить</Button>
            </Box>
            {faculties.map((faculty) => (
              <Stack key={faculty.id} direction="row" spacing={1} alignItems="center" justifyContent="space-between" py={0.5}>
                <Typography variant="body2">{faculty.name}</Typography>
                <Button aria-label={`Удалить факультет ${faculty.name}`} color="error" size="small" disabled={busy} onClick={() => void handleDeleteFaculty(faculty)} startIcon={<DeleteOutlineRoundedIcon />}>Удалить</Button>
              </Stack>
            ))}
            {!faculties.length && <Typography color="text.secondary">Факультетов пока нет. Создайте первый, чтобы добавлять учебные группы.</Typography>}
            </Box>

            <Box sx={{ display: structureTab === 'departments' ? undefined : 'none', mt: 1 }}>
            <Typography variant="subtitle1" fontWeight={750} mb={1}>Кафедры</Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Удалить можно только кафедру без дисциплин и приглашений преподавателей.
            </Typography>
            <Box component="form" onSubmit={(event) => void handleCreateDepartment(event)} display="grid" gridTemplateColumns={{ xs: '1fr', sm: 'minmax(0, 1fr) auto' }} gap={1} mb={2}>
              <TextField label="Название кафедры" value={newDepartmentName} onChange={(event) => setNewDepartmentName(event.target.value)} inputProps={{ maxLength: 255 }} required fullWidth />
              <Button type="submit" variant="outlined" disabled={busy} startIcon={busy ? <CircularProgress size={18} /> : <AddRoundedIcon />}>Создать кафедру</Button>
            </Box>
            {departments.length ? (
              <Stack spacing={1.25}>
                {departments.map((department) => (
                  <Box key={department.id} display="grid" gridTemplateColumns={{ xs: '1fr', sm: 'minmax(0, 1fr) auto' }} gap={1} alignItems="center">
                    <TextField
                      size="small"
                      label="Название кафедры"
                      value={renamedDepartments[department.id] ?? department.name}
                      onChange={(event) => setRenamedDepartments((current) => ({ ...current, [department.id]: event.target.value }))}
                      fullWidth
                    />
                    <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                      <Tooltip title="Сохранить название">
                        <span>
                          <IconButton
                            aria-label={`Сохранить название кафедры ${department.name}`}
                            color="primary"
                            disabled={busy || !renamedDepartments[department.id]?.trim() || renamedDepartments[department.id] === department.name}
                            onClick={() => void handleRenameDepartment(department.id)}
                          >
                            <SaveRoundedIcon />
                          </IconButton>
                        </span>
                      </Tooltip>
                      <Tooltip title="Удалить кафедру">
                        <span>
                          <IconButton
                            aria-label={`Удалить кафедру ${department.name}`}
                            color="error"
                            disabled={busy}
                            onClick={() => void handleDeleteDepartment(department)}
                          >
                            <DeleteOutlineRoundedIcon />
                          </IconButton>
                        </span>
                      </Tooltip>
                    </Stack>
                  </Box>
                ))}
              </Stack>
            ) : <Typography color="text.secondary">Кафедры не найдены.</Typography>}
            </Box>

            <Box sx={{ display: structureTab === 'disciplines' ? undefined : 'none', mt: 1 }}>
            <Typography variant="h6" fontWeight={750} mb={0.75}>Дисциплины</Typography>
            <Typography color="text.secondary" variant="body2" mb={2}>Добавляйте дисциплины на кафедру. Используемые в учебном процессе дисциплины нельзя удалить.</Typography>
            <FormControl fullWidth sx={{ mb: 2 }} disabled={!departments.length}>
              <InputLabel id="discipline-filter-department-label">Кафедра</InputLabel>
              <Select
                labelId="discipline-filter-department-label"
                label="Кафедра"
                value={departmentId}
                onChange={(event) => setDepartmentId(event.target.value)}
              >
                {departments.map((department) => <MenuItem key={department.id} value={department.id}>{department.name}</MenuItem>)}
              </Select>
            </FormControl>
            <Typography variant="subtitle1" fontWeight={750} mb={1}>Новая дисциплина</Typography>
            <Box component="form" onSubmit={(event) => void handleCreateDiscipline(event)}>
              <Stack spacing={1.25}>
                <TextField
                  label="Название дисциплины"
                  value={newDisciplineName}
                  onChange={(event) => setNewDisciplineName(event.target.value)}
                  inputProps={{ maxLength: 255 }}
                  required
                  fullWidth
                />
                <Button type="submit" variant="outlined" disabled={busy || !departments.length} startIcon={busy ? <CircularProgress size={18} /> : <AddRoundedIcon />}>
                  Добавить дисциплину
                </Button>
              </Stack>
            </Box>
            <Divider sx={{ my: 2.5 }} />
            {disciplinesLoading ? <CircularProgress size={24} aria-label="Загрузка дисциплин" /> : disciplines.length ? (
              <Stack divider={<Divider flexItem />} spacing={0}>
                {disciplines.map((discipline) => (
                  <Stack key={discipline.id} direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'center' }} justifyContent="space-between" py={1}>
                    <Typography>{discipline.name}</Typography>
                    <Button
                      aria-label={`Удалить дисциплину ${discipline.name}`}
                      color="error"
                      size="small"
                      disabled={busy}
                      onClick={() => void handleDeleteDiscipline(discipline)}
                      startIcon={<DeleteOutlineRoundedIcon />}
                    >
                      Удалить
                    </Button>
                  </Stack>
                ))}
              </Stack>
            ) : <Typography color="text.secondary">{departmentId ? 'На этой кафедре пока нет дисциплин.' : 'Сначала создайте кафедру.'}</Typography>}
            </Box>

            <Box sx={{ display: structureTab === 'groups' ? undefined : 'none', mt: 3 }}>
            <Typography variant="subtitle1" fontWeight={750} mb={1}>Группы</Typography>
            {groups.length ? (
              <Stack spacing={1.5}>
                {groups.map((group) => (
                  <Box
                    key={group.id}
                    display="grid"
                    gridTemplateColumns={{ xs: '1fr', sm: 'minmax(0, 1fr) auto' }}
                    gap={{ xs: 0.75, sm: 1.5 }}
                    alignItems="center"
                    sx={{ p: 1.5, border: 1, borderColor: 'divider', borderRadius: 2, bgcolor: 'background.paper' }}
                  >
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={{ xs: 0.25, sm: 1 }} alignItems={{ sm: 'center' }} minWidth={0}>
                      <TextField
                        size="small"
                        label="Название группы"
                        value={renamedGroups[group.id] ?? group.name}
                        onChange={(event) => setRenamedGroups((current) => ({ ...current, [group.id]: event.target.value }))}
                        fullWidth
                      />
                      <Typography variant="caption" color="text.secondary" sx={{ px: 0.25 }}>
                        {group.facultyName}
                      </Typography>
                    </Stack>
                    <Stack direction="row" spacing={0.5} justifyContent="flex-end">
                      <Tooltip title="Сохранить название">
                        <span>
                          <IconButton
                            aria-label={`Сохранить название группы ${group.name}`}
                            color="primary"
                            disabled={busy || !renamedGroups[group.id]?.trim() || renamedGroups[group.id] === group.name}
                            onClick={() => void handleRenameGroup(group.id)}
                          >
                            <SaveRoundedIcon />
                          </IconButton>
                        </span>
                      </Tooltip>
                      <Tooltip title="Удалить группу">
                        <span>
                          <IconButton
                            aria-label={`Удалить группу ${group.name}`}
                            color="error"
                            disabled={busy}
                            onClick={() => void handleDeleteGroup(group)}
                          >
                            <DeleteOutlineRoundedIcon />
                          </IconButton>
                        </span>
                      </Tooltip>
                    </Stack>
                  </Box>
                ))}
              </Stack>
            ) : <Typography color="text.secondary">Групп пока нет.</Typography>}
            </Box>
          </CardContent>
        </Card>

        <Card sx={{ display: managementTab === 'teachers' ? undefined : 'none' }}>
          <CardContent>
            <Stack direction="row" spacing={1.25} alignItems="center" mb={2}>
              <PersonAddAltRoundedIcon color="primary" />
              <Typography variant="h6" fontWeight={750}>Пригласить преподавателя</Typography>
            </Stack>
            <Box component="form" onSubmit={(event) => void handleInviteTeacher(event)}>
              <Stack spacing={1.5}>
                <TextField label="ФИО преподавателя" value={teacherName} onChange={(event) => setTeacherName(event.target.value)} required fullWidth />
                <TextField label="Электронная почта" type="email" value={teacherEmail} onChange={(event) => setTeacherEmail(event.target.value)} required fullWidth />
                <FormControl fullWidth required>
                  <InputLabel id="invite-department-label">Кафедра</InputLabel>
                  <Select labelId="invite-department-label" label="Кафедра" value={departmentId} onChange={(event) => {
                    setDepartmentId(event.target.value);
                  }}>
                    {departments.map((department) => <MenuItem key={department.id} value={department.id}>{department.name}</MenuItem>)}
                  </Select>
                </FormControl>
                <FormControl fullWidth required>
                  <InputLabel id="invite-disciplines-label">Дисциплины</InputLabel>
                  <Select<string[]>
                    labelId="invite-disciplines-label"
                    label="Дисциплины"
                    multiple
                    value={inviteDisciplineIds}
                    onChange={(event) => setInviteDisciplineIds(typeof event.target.value === 'string' ? event.target.value.split(',') : event.target.value)}
                    disabled={disciplinesLoading || !disciplines.length}
                    renderValue={(selected) => disciplines.filter((discipline) => selected.includes(discipline.id)).map((discipline) => discipline.name).join(', ')}
                  >
                    {disciplines.map((discipline) => (
                      <MenuItem key={discipline.id} value={discipline.id}>
                        <Checkbox checked={inviteDisciplineIds.includes(discipline.id)} />
                        <ListItemText primary={discipline.name} />
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <FormControl fullWidth required>
                  <InputLabel id="invite-groups-label">Группы</InputLabel>
                  <Select<string[]>
                    labelId="invite-groups-label"
                    label="Группы"
                    multiple
                    value={inviteGroupIds}
                    onChange={(event) => setInviteGroupIds(typeof event.target.value === 'string' ? event.target.value.split(',') : event.target.value)}
                    disabled={!groups.length}
                    renderValue={(selected) => groups.filter((group) => selected.includes(group.id)).map((group) => group.name).join(', ')}
                  >
                    {groups.map((group) => (
                      <MenuItem key={group.id} value={group.id}>
                        <Checkbox checked={inviteGroupIds.includes(group.id)} />
                        <ListItemText primary={group.name} secondary={group.facultyName} />
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <Button type="submit" variant="contained" disabled={busy || !departments.length || !groups.length || !disciplines.length} startIcon={busy ? <CircularProgress size={18} color="inherit" /> : <PersonAddAltRoundedIcon />}>
                  Отправить приглашение
                </Button>
                <Typography variant="caption" color="text.secondary">Преподаватель получит ссылку по электронной почте и самостоятельно задаст пароль.</Typography>
              </Stack>
            </Box>
          </CardContent>
        </Card>
      </Box>

      <Card sx={{ mt: 2, display: managementTab === 'teachers' ? undefined : 'none' }}>
        <CardContent>
          <Typography variant="h6" fontWeight={750} mb={2}>Преподаватели и назначения</Typography>
          <Box component="form" onSubmit={(event) => void handleCreateAssignment(event)} display="grid" gridTemplateColumns={{ xs: '1fr', md: 'repeat(3, minmax(0, 1fr)) auto' }} gap={1.5} mb={2.5}>
            <FormControl fullWidth required>
              <InputLabel id="assignment-teacher-label">Преподаватель</InputLabel>
              <Select labelId="assignment-teacher-label" label="Преподаватель" value={assignmentTeacherId} onChange={(event) => setAssignmentTeacherId(event.target.value)}>
                {teachers.filter((teacher) => teacher.active).map((teacher) => <MenuItem key={teacher.id} value={teacher.id}>{teacher.fullName}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth required>
              <InputLabel id="assignment-group-label">Группа</InputLabel>
              <Select labelId="assignment-group-label" label="Группа" value={assignmentGroupId} onChange={(event) => setAssignmentGroupId(event.target.value)}>
                {groups.map((group) => <MenuItem key={group.id} value={group.id}>{group.name}</MenuItem>)}
              </Select>
            </FormControl>
            <FormControl fullWidth required>
              <InputLabel id="assignment-discipline-label">Дисциплина</InputLabel>
              <Select labelId="assignment-discipline-label" label="Дисциплина" value={assignmentDisciplineId} onChange={(event) => setAssignmentDisciplineId(event.target.value)} disabled={!disciplines.length}>
                {disciplines.map((discipline) => <MenuItem key={discipline.id} value={discipline.id}>{discipline.name}</MenuItem>)}
              </Select>
            </FormControl>
            <Button type="submit" variant="outlined" disabled={busy || !teachers.some((teacher) => teacher.active) || !groups.length || !disciplines.length} startIcon={busy ? <CircularProgress size={18} /> : <AddRoundedIcon />}>Закрепить</Button>
          </Box>

          {teachers.length ? (
            <Stack divider={<Divider flexItem />} spacing={0}>
              {teachers.map((teacher) => (
                <Box key={teacher.id} py={2}>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'center' }} justifyContent="space-between">
                    <Box minWidth={0}>
                      <Typography fontWeight={750}>{teacher.fullName}</Typography>
                      <Typography variant="body2" color="text.secondary" sx={{ overflowWrap: 'anywhere' }}>{teacher.email}</Typography>
                    </Box>
                    <Button color={teacher.active ? 'warning' : 'success'} disabled={busy} onClick={() => void toggleTeacher(teacher)}>
                      {teacher.active ? 'Отключить доступ' : 'Восстановить доступ'}
                    </Button>
                  </Stack>
                  <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap" mt={1.25}>
                    {teacher.assignments.map((assignment) => (
                      <Chip
                        key={assignment.id}
                        label={`${assignment.groupName} · ${assignment.disciplineName}`}
                        variant="outlined"
                        onDelete={busy ? undefined : () => void removeAssignment(teacher.id, assignment.id)}
                        deleteIcon={<ArchiveRoundedIcon />}
                      />
                    ))}
                    {!teacher.assignments.length && <Typography variant="body2" color="text.secondary">Нет активных назначений.</Typography>}
                  </Stack>
                </Box>
              ))}
            </Stack>
          ) : <Typography color="text.secondary">Преподавателей пока нет. Создайте приглашение выше.</Typography>}
        </CardContent>
      </Card>
    </Box>
  );
}
