import { useNavigate } from 'react-router';
import { api } from '../api.js';
import { PageHeader } from '../components/Common.jsx';
import SubscriptionForm from '../components/SubscriptionForm.jsx';

export default function SubscriptionNewPage() {
  const navigate = useNavigate();

  const onSubmit = async (form) => {
    const created = await api('/api/subscriptions', { method: 'POST', body: form });
    navigate(`/subscriptions/${created.id}`, { replace: true });
  };

  return (
    <div className="page">
      <PageHeader title="구독 추가" back="/subscriptions" />
      <SubscriptionForm submitText="저장하기" onSubmit={onSubmit} />
    </div>
  );
}
