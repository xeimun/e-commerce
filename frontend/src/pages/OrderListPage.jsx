import { Clock3, ReceiptText, RefreshCw } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client.js';
import { formatDateTime, formatWon } from '../utils/format.js';

const orderStatusLabels = {
  PAYMENT_PENDING: '결제 대기',
  COMPLETED: '주문 완료',
  CANCELED: '주문 취소'
};

function OrderStatusBadge({ status }) {
  return (
    <span className={`statusBadge orderStatus status${status || ''}`}>
      {orderStatusLabels[status] || status || '-'}
    </span>
  );
}

function OrderCard({ order }) {
  return (
    <Link className="orderCard" to={`/orders/${order.orderId}`}>
      <div className="orderCardIcon" aria-hidden="true">
        <ReceiptText size={22} />
      </div>
      <div className="orderCardBody">
        <div className="orderCardTitle">
          <div>
            <span>주문 #{order.orderId}</span>
            <strong>{formatWon(order.finalPaymentAmount)}</strong>
          </div>
          <OrderStatusBadge status={order.status} />
        </div>
        <div className="orderCardMeta">
          <Clock3 aria-hidden="true" size={16} />
          <span>결제 만료 {formatDateTime(order.expiresAt)}</span>
        </div>
      </div>
    </Link>
  );
}

export default function OrderListPage({ customerId, onApiEvent }) {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');

  async function loadOrders() {
    setStatus('loading');
    setMessage('');

    try {
      const payload = await api.getOrders({ customerId, onApiEvent });
      setOrders(payload || []);
      setStatus('success');
    } catch (error) {
      setStatus('error');
      setMessage(error.message || '주문을 불러오지 못했습니다.');
    }
  }

  useEffect(() => {
    loadOrders();
  }, [customerId]);

  const counts = useMemo(() => {
    return orders.reduce(
      (acc, order) => ({
        ...acc,
        [order.status]: (acc[order.status] || 0) + 1
      }),
      {}
    );
  }, [orders]);

  return (
    <section className="pageStack">
      <div className="pageHeader">
        <p className="eyebrow">Orders</p>
        <div>
          <h1>주문</h1>
          <p>생성한 결제 대기 주문과 완료/취소 상태를 확인합니다.</p>
        </div>
      </div>

      <div className="orderSummaryGrid">
        <div>
          <span>전체 주문</span>
          <strong>{orders.length}건</strong>
        </div>
        <div>
          <span>결제 대기</span>
          <strong>{counts.PAYMENT_PENDING || 0}건</strong>
        </div>
        <div>
          <span>완료</span>
          <strong>{counts.COMPLETED || 0}건</strong>
        </div>
        <button className="textButton" type="button" onClick={loadOrders}>
          <RefreshCw aria-hidden="true" size={18} />
          새로고침
        </button>
      </div>

      {status === 'loading' && (
        <div className="orderList" aria-label="주문 로딩">
          {Array.from({ length: 3 }).map((_, index) => (
            <div className="orderCard skeletonOrderCard" key={index}>
              <div className="orderCardIcon skeleton" />
              <div className="orderCardBody">
                <div className="skeletonLine wide" />
                <div className="skeletonLine" />
              </div>
            </div>
          ))}
        </div>
      )}

      {status === 'error' && (
        <div className="noticeBlock">
          <strong>연결이 불안정해요</strong>
          <p>{message}</p>
          <button className="primaryButton" type="button" onClick={loadOrders}>
            <RefreshCw aria-hidden="true" size={18} />
            다시 시도
          </button>
        </div>
      )}

      {status === 'success' && orders.length === 0 && (
        <div className="noticeBlock">
          <strong>아직 주문이 없어요.</strong>
          <p>장바구니에서 상품을 선택해 첫 주문을 만들어 보세요.</p>
          <button className="primaryButton" type="button" onClick={() => navigate('/products')}>
            상품 보기
          </button>
        </div>
      )}

      {status === 'success' && orders.length > 0 && (
        <div className="orderList">
          {orders.map((order) => (
            <OrderCard key={order.orderId} order={order} />
          ))}
        </div>
      )}
    </section>
  );
}

export { OrderStatusBadge, orderStatusLabels };
