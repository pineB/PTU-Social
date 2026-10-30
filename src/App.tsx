import { useEffect, useState } from 'react';
import { Alert, Snackbar } from '@mui/material';
import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/AppLayout';
import {
  AcademicOfficeDashboard,
  StudentDashboard,
  TeacherDashboard,
} from './pages/DashboardPage';
import {
  StudentRegistrationPage,
  TeacherActivationPage,
  VerifyEmailPage,
} from './pages/AccountPages';
import StudentProfilePage from './pages/StudentProfilePage';
import StudentTasksPage from './pages/StudentTasksPage';
import TeacherSubmissionsPage from './pages/TeacherSubmissionsPage';
import { TaskWorkflowProvider } from './data/task-workflow';
import type { HttpErrorNotice } from './types/events';

export default function App() {
  const [notice, setNotice] = useState<HttpErrorNotice | null>(null);

  useEffect(() => {
    const handleHttpError = (event: WindowEventMap['app:http-error']) => setNotice(event.detail);
    window.addEventListener('app:http-error', handleHttpError);
    return () => window.removeEventListener('app:http-error', handleHttpError);
  }, []);

  return (
    <TaskWorkflowProvider>
      <Routes>
        <Route path="register" element={<StudentRegistrationPage />} />
        <Route path="verify-email" element={<VerifyEmailPage />} />
        <Route path="teacher/activate" element={<TeacherActivationPage />} />
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to="/student" replace />} />
          <Route path="student" element={<StudentDashboard />} />
          <Route path="student/profile" element={<StudentProfilePage />} />
          <Route path="student/tasks" element={<StudentTasksPage />} />
          <Route path="teacher" element={<TeacherDashboard />} />
          <Route path="teacher/submissions" element={<TeacherSubmissionsPage />} />
          <Route path="academic-office" element={<AcademicOfficeDashboard />} />
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
  );
}
