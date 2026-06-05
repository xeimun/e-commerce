import {
  ArrowLeft,
  CheckCircle2,
  Clock3,
  CreditCard,
  Package,
  RefreshCw,
  XCircle
} from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import { formatDateTime, formatWon } from '../utils/format.js';
import { OrderStatusBadge } from './OrderListPage.jsx';

const cancelReasonLabels = {
  PAYMENT_CANCELED: '결제 취소',
  PAYMENT_EXPIRED: '결제 만료'
};

function OrderItemRow({ item }) {
  return (
    <article className="orderItemRow">
      <div className="thumb orderItemThumb">
        <span>{(item.productName || '상품').slice(0, 2)}</span>
      </div>
      <div className="orderItemBody">
        <h2>{item.productName}</h2>
        <p>상품 #{item.productId} · 수량 {item.quantity}개</p>
        <div className="checkoutPriceMeta">
          <span>상품 금액 {formatWon(item.originalAmount)}</span>
          {Number(item.instantDiscountAmount || 0) > 0 && (
            <span>즉시 할인 -{formatWon(item.instantDiscountAmount)}</span>
          )}
          {Number(item.productCouponDiscountAmount || 0) > 0 && (
            <span>상품 쿠폰 -{formatWon(item.productCouponDiscountAmount)}</span>
          )}
        </div>
      </div>
      <div className="orderItemPrice">
        <span>단가 {formatWon(item.productPrice)}</span>
        <strong>{formatWon(item.finalAmount)}</strong>
      </div>
    </article>
  );
}

export default function OrderDetailPage({ customerId, onApiEvent }) {
  const { orderId } = useParams();
  const [order, setOrder] = useState(null);
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');
  const [paymentAction, setPaymentAction] = useState('idle');
  const [paymentMessage, setPaymentMessage] = useState('');
  const [paymentMessageType, setPaymentMessageType] = useState('success');

  async function loadOrder({ showLoading = true } = {}) {
    if (showLoading) {
      setStatus('loading');
    }
    setMessage('');

    try {
      const payload = await api.getOrder(orderId, { customerId, onApiEvent });
      setOrder(payload);
      setStatus('success');
    } catch (error) {
      setStatus('error');
      setMessage(error.message || '주문 상세를 불러오지 못했습니다.');
    }
  }

  async function processPayment(action) {
    setPaymentAction(action);
    setPaymentMessage('');

    try {
      if (action === 'success') {
        await api.completePayment(orderId, { customerId, onApiEvent });
      } else {
        await api.cancelPayment(orderId, { customerId, onApiEvent });
      }

      await loadOrder({ showLoading: false });
      setPaymentMessageType('success');
      setPaymentMessage(action === 'success' ? '결제 성공 처리됐어요.' : '결제가 취소됐어요.');
    } catch (error) {
      await loadOrder({ showLoading: false });
      setPaymentMessageType('error');
      setPaymentMessage(error.message || '결제 상태를 변경하지 못했습니다.');
    } finally {
      setPaymentAction('idle');
    }
  }

  useEffect(() => {
    loadOrder();
  }, [customerId, orderId]);

  if (status === 'loading') {
    return (
      <section className="pageStack">
        <Link className="textButton" to="/orders">
          <ArrowLeft aria-hidden="true" size={18} />
          주문 목록
        </Link>
        <div className="orderDetailLayout">
          <div className="checkoutList">
            {Array.from({ length: 3 }).map((_, index) => (
              <div className="orderItemRow skeletonOrderCard" key={index}>
                <div className="thumb skeleton" />
                <div className="orderItemBody">
                  <div className="skeletonLine wide" />
                  <div className="skeletonLine" />
                </div>
              </div>
            ))}
          </div>
          <div className="checkoutSummary skeletonCheckoutSummary">
            <div className="skeletonLine wide" />
            <div className="skeletonLine" />
            <div className="skeletonLine wide" />
          </div>
        </div>
      </section>
    );
  }

  if (status === 'error') {
    return (
      <section className="pageStack">
        <Link className="textButton" to="/orders">
          <ArrowLeft aria-hidden="true" size={18} />
          주문 목록
        </Link>
        <div className="noticeBlock">
          <strong>주문을 찾지 못했어요.</strong>
          <p>{message}</p>
          <button className="primaryButton" type="button" onClick={loadOrder}>
            <RefreshCw aria-hidden="true" size={18} />
            다시 시도
          </button>
        </div>
      </section>
    );
  }

  const isPaymentPending = order.status === 'PAYMENT_PENDING';
  const isMutatingPayment = paymentAction !== 'idle';

  return (
    <section className="pageStack">
      <Link className="textButton" to="/orders">
        <ArrowLeft aria-hidden="true" size={18} />
        주문 목록
      </Link>

      <div className="pageHeader">
        <p className="eyebrow">Order Detail</p>
        <div>
          <h1>주문 #{order.orderId}</h1>
          <p>주문 당시 금액과 상품별 할인 내역을 확인합니다.</p>
        </div>
      </div>

      <div className="orderDetailLayout">
        <div className="checkoutMain">
          <section className="checkoutSection" aria-labelledby="order-state-title">
            <div className="sectionTitle">
              <h2 id="order-state-title">주문 상태</h2>
              <OrderStatusBadge status={order.status} />
            </div>
            <dl className="orderFacts">
              <div>
                <dt>
                  <Clock3 aria-hidden="true" size={16} />
                  결제 만료
                </dt>
                <dd>{formatDateTime(order.expiresAt)}</dd>
              </div>
              <div>
                <dt>취소 사유</dt>
                <dd>{cancelReasonLabels[order.cancelReason] || order.cancelReason || '-'}</dd>
              </div>
              <div>
                <dt>상품 수</dt>
                <dd>{order.items?.length || 0}개</dd>
              </div>
            </dl>
          </section>

          <section className="checkoutSection" aria-labelledby="order-items-title">
            <div className="sectionTitle">
              <h2 id="order-items-title">주문 상품</h2>
              <span>{order.items?.length || 0}개</span>
            </div>
            <div className="checkoutList">
              {(order.items || []).map((item) => (
                <OrderItemRow key={item.orderItemId} item={item} />
              ))}
            </div>
          </section>
        </div>

        <aside className="checkoutSummary" aria-label="주문 금액">
          <div className="panelHeader">
            <CreditCard aria-hidden="true" size={18} />
            <h2>주문 금액</h2>
          </div>
          <dl className="summaryRows">
            <div>
              <dt>상품 금액</dt>
              <dd>{formatWon(order.totalProductAmount)}</dd>
            </div>
            <div>
              <dt>즉시 할인</dt>
              <dd>-{formatWon(order.totalInstantDiscountAmount)}</dd>
            </div>
            <div>
              <dt>쿠폰 할인</dt>
              <dd>-{formatWon(order.totalCouponDiscountAmount)}</dd>
            </div>
            <div className="summaryTotal">
              <dt>최종 결제 금액</dt>
              <dd>{formatWon(order.finalPaymentAmount)}</dd>
            </div>
          </dl>

          {isPaymentPending && (
            <div className="paymentActionBox" aria-label="결제 처리">
              <button
                className="primaryButton"
                disabled={isMutatingPayment}
                type="button"
                onClick={() => processPayment('success')}
              >
                <CheckCircle2 aria-hidden="true" size={18} />
                {paymentAction === 'success' ? '처리 중' : '결제 성공'}
              </button>
              <button
                className="outlineButton"
                disabled={isMutatingPayment}
                type="button"
                onClick={() => processPayment('cancel')}
              >
                <XCircle aria-hidden="true" size={18} />
                {paymentAction === 'cancel' ? '처리 중' : '결제 취소'}
              </button>
            </div>
          )}

          {paymentMessage && (
            <div className={`inlineNotice ${paymentMessageType}`}>
              {paymentMessage}
            </div>
          )}

          <div className="orderSummaryNote">
            <Package aria-hidden="true" size={18} />
            <span>주문 생성 시점의 금액 정보입니다.</span>
          </div>
        </aside>
      </div>
    </section>
  );
}
