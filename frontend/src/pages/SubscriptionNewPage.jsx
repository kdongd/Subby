import { useNavigate } from 'react-router';
import { api } from '../api.js';
import SubscriptionForm from '../components/SubscriptionForm.jsx';

export default function SubscriptionNewPage() {
  const navigate = useNavigate();

  const onSubmit = async (form) => {
    const created = await api('/api/subscriptions', { method: 'POST', body: form });
    navigate(`/subscriptions/${created.id}`, { replace: true });
  };

  return (
    <div className="stack narrow">
      <h1>구독 등록</h1>
      <SubscriptionForm submitText="등록" onSubmit={onSubmit} onCancel={() => navigate(-1)} />
    </div>
  );
}
