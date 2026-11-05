import { useEffect, useState } from 'react';
import { Alert, Snackbar } from '@mui/material';
import { Navigate, Route, Routes } from 'react-router-dom';
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
  TeacherStudentsPage,
  WalletPage,
} from './pages/CampusPages';
import type { HttpErrorNotice } from './types/events';

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
          <Route path="register" element={<StudentRegistrationPage />} />
          <Route path="verify-email" element={<VerifyEmailPage />} />
          <Route path="teacher/activate" element={<TeacherActivationPage />} />
          <Route element={<AppLayout />}>
            <Route index element={<Navigate to="/student" replace />} />
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
            <Route path="teacher" element={<TeacherDashboard />} />
            <Route path="teacher/submissions" element={<TeacherSubmissionsPage />} />
            <Route path="teacher/students" element={<TeacherStudentsPage />} />
            <Route path="teacher/events" element={<EventManagementPage />} />
            <Route path="organizer" element={<OrganizerDashboard />} />
            <Route path="organizer/events" element={<EventManagementPage />} />
            <Route path="academic-office" element={<AcademicOfficeDashboard />} />
            <Route path="academic-office/certificates" element={<CertificateManagementPage />} />
            <Route path="academic-office/schedule" element={<ScheduleManagementPage />} />
            <Route path="*" element={<Navigate to="/student" replace />} />
          </Route>
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
