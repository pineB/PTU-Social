import { useEffect, useState } from 'react';
import { Alert, Snackbar } from '@mui/material';
import { Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import {
  AcademicOfficeDashboard,
  OrganizerDashboard,
  StudentDashboard,
  TeacherDashboard,
} from './pages/DashboardPage';
import {
  StudentRegistrationPage,
  TeacherActivationPage,
  VerifyEmailPage,
  LoginPage,
} from './pages/AccountPages';
import StudentProfilePage from './pages/StudentProfilePage';
import { AwardsFeedPage, LeaderboardPage, PublicStudentProfilePage } from './pages/CommunityPages';
import StudentTasksPage from './pages/StudentTasksPage';
import TeacherSubmissionsPage from './pages/TeacherSubmissionsPage';
import { TaskWorkflowProvider } from './data/task-workflow';
import { CampusWorkflowProvider } from './data/campus-workflow';
import {
  BattlePassPage,
  CertificateRequestPage,
  CertificateManagementPage,
  EventsPage,
  EventManagementPage,
  GroupConfirmationPage,
  InventoryPage,
  ScheduleManagementPage,
  StudentSchedulePage,
  TeacherStudentsPage,
  WalletPage,
} from './pages/CampusPages';
import type { HttpErrorNotice } from './types/events';
import { useAuth } from './auth/AuthProvider';
import type { UserRole } from './api/auth';
import AdminManagementPage from './pages/AdminManagementPage';
import AcademicManagementPage from './pages/AcademicManagementPage';

const roleHomePaths: Record<UserRole, string> = {
  STUDENT: '/student',
  TEACHER: '/teacher',
  ORGANIZER: '/organizer',
  ACADEMIC_OFFICE: '/academic-office',
  ADMIN: '/admin',
};

function RequireAuthentication() {
  const { session } = useAuth();
  const location = useLocation();
  return session ? <Outlet /> : <Navigate to="/login" replace state={{ from: location.pathname }} />;
}

function RequireRole({ role }: { role: UserRole }) {
  const { session } = useAuth();
  return session?.role === role
    ? <Outlet />
    : <Navigate to={session ? roleHomePaths[session.role] : '/login'} replace />;
}

function HomeRedirect() {
  const { session } = useAuth();
  return <Navigate to={session ? roleHomePaths[session.role] : '/login'} replace />;
}

export default function App() {
  const [notice, setNotice] = useState<HttpErrorNotice | null>(null);

  useEffect(() => {
    const handleHttpError = (event: WindowEventMap['app:http-error']) => setNotice(event.detail);
    window.addEventListener('app:http-error', handleHttpError);
    return () => window.removeEventListener('app:http-error', handleHttpError);
  }, []);

  return (
    <CampusWorkflowProvider>
      <TaskWorkflowProvider>
        <Routes>
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<StudentRegistrationPage />} />
          <Route path="verify-email" element={<VerifyEmailPage />} />
          <Route path="teacher/activate" element={<TeacherActivationPage />} />
          <Route element={<RequireAuthentication />}>
            <Route element={<AppLayout />}>
              <Route index element={<HomeRedirect />} />
              <Route element={<RequireRole role="STUDENT" />}>
                <Route path="student" element={<StudentDashboard />} />
                <Route path="student/profile" element={<StudentProfilePage />} />
                <Route path="student/awards" element={<AwardsFeedPage />} />
                <Route path="student/leaderboard" element={<LeaderboardPage />} />
                <Route path="student/people/:studentId" element={<PublicStudentProfilePage />} />
                <Route path="student/tasks" element={<StudentTasksPage />} />
                <Route path="student/events" element={<EventsPage />} />
                <Route path="student/group-confirmation" element={<GroupConfirmationPage />} />
                <Route path="student/wallet" element={<WalletPage />} />
                <Route path="student/battle-pass" element={<BattlePassPage />} />
                <Route path="student/inventory" element={<InventoryPage />} />
                <Route path="student/certificates" element={<CertificateRequestPage />} />
                <Route path="student/schedule" element={<StudentSchedulePage />} />
              </Route>
              <Route element={<RequireRole role="TEACHER" />}>
                <Route path="teacher" element={<TeacherDashboard />} />
                <Route path="teacher/submissions" element={<TeacherSubmissionsPage />} />
                <Route path="teacher/students" element={<TeacherStudentsPage />} />
                <Route path="teacher/events" element={<EventManagementPage />} />
              </Route>
              <Route element={<RequireRole role="ORGANIZER" />}>
                <Route path="organizer" element={<OrganizerDashboard />} />
                <Route path="organizer/events" element={<EventManagementPage />} />
              </Route>
              <Route element={<RequireRole role="ACADEMIC_OFFICE" />}>
                <Route path="academic-office" element={<AcademicOfficeDashboard />} />
                <Route path="academic-office/management" element={<AcademicManagementPage />} />
                <Route path="academic-office/certificates" element={<CertificateManagementPage />} />
                <Route path="academic-office/schedule" element={<ScheduleManagementPage />} />
              </Route>
              <Route element={<RequireRole role="ADMIN" />}>
                <Route path="admin" element={<AdminManagementPage />} />
              </Route>
              <Route path="*" element={<HomeRedirect />} />
            </Route>
          </Route>
          <Route path="*" element={<HomeRedirect />} />
        </Routes>
        <Snackbar
          open={notice !== null}
          autoHideDuration={6000}
          onClose={() => setNotice(null)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
        >
          {notice ? (
            <Alert onClose={() => setNotice(null)} severity={notice.status === 401 ? 'warning' : 'error'} variant="filled" sx={{ width: '100%' }}>
              {notice.message}
            </Alert>
          ) : undefined}
        </Snackbar>
      </TaskWorkflowProvider>
    </CampusWorkflowProvider>
  );
}
