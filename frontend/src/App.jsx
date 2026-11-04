import { Navigate, Route, Routes } from 'react-router';
import Layout from './components/Layout.jsx';
import { GuestOnly, RequireAuth } from './components/Common.jsx';
import LoginPage from './pages/LoginPage.jsx';
import DashboardPage from './pages/DashboardPage.jsx';
import SubscriptionListPage from './pages/SubscriptionListPage.jsx';
import SubscriptionNewPage from './pages/SubscriptionNewPage.jsx';
import SubscriptionDetailPage from './pages/SubscriptionDetailPage.jsx';
import StatsPage from './pages/StatsPage.jsx';
import SettingsPage from './pages/SettingsPage.jsx';
import NotificationsPage from './pages/NotificationsPage.jsx';
import PrivacyPage from './pages/PrivacyPage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
      {/* 이메일 가입·비밀번호 찾기 화면은 없앴다. 예전 주소로 들어오면 로그인 화면으로 보낸다. */}
      <Route path="/signup" element={<Navigate to="/login" replace />} />
      <Route path="/forgot-password" element={<Navigate to="/login" replace />} />
      <Route path="/reset-password" element={<Navigate to="/login" replace />} />
      <Route path="/privacy" element={<PrivacyPage />} />
      <Route element={<RequireAuth><Layout /></RequireAuth>}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/subscriptions" element={<SubscriptionListPage />} />
        <Route path="/subscriptions/new" element={<SubscriptionNewPage />} />
        <Route path="/subscriptions/:id" element={<SubscriptionDetailPage />} />
        <Route path="/stats" element={<StatsPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
