import {
  Activity,
  Boxes,
  ChevronDown,
  ChevronUp,
  Clock3,
  Package,
  ReceiptText,
  RefreshCw,
  ShoppingCart,
  Ticket,
  UserRound
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';
import { formatDateTime, formatWon, getCartItemUnitPrice, getDiscountedPrice } from '../utils/format.js';

const couponStatusLabels = {
  AVAILABLE: '사용 가능',
  RESERVED: '예약',
  USED: '사용 완료',
  EXPIRED: '만료'
};

const orderStatusLabels = {
  PAYMENT_PENDING: '결제 대기',
  COMPLETED: '완료',
  CANCELED: '취소'
};

const productStatusLabels = {
  ON_SALE: '판매 중',
  STOPPED: '판매 중지'
};

const emptySnapshot = {
  products: [],
  cartItems: [],
  coupons: [],
  orders: []
};

function formatShortTime() {
  return new Date().toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

function getTimeValue(value) {
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? Number.MAX_SAFE_INTEGER : time;
}

function EmptyPanelRow({ text }) {
  return <p className="mutedText statusEmpty">{text}</p>;
}

function StatusSection({ children, count, icon: Icon, id, title }) {
  return (
    <section className="statusSection" aria-labelledby={`status-${id}`}>
      <div className="statusSectionHeader">
        <span>
          <Icon aria-hidden="true" size={16} />
          <h3 id={`status-${id}`}>{title}</h3>
        </span>
        <small>{count}</small>
      </div>
      {children}
    </section>
  );
}

export default function StatusPanel({ apiEvents, customerId }) {
  const [isOpen, setIsOpen] = useState(true);
  const [snapshot, setSnapshot] = useState(emptySnapshot);
  const [snapshotStatus, setSnapshotStatus] = useState('idle');
  const [snapshotMessage, setSnapshotMessage] = useState('');
  const [lastSyncedAt, setLastSyncedAt] = useState('');
  const [refreshKey, setRefreshKey] = useState(0);
  const latestApiEventId = apiEvents[0]?.id || '';

  useEffect(() => {
    let ignore = false;

    async function loadSnapshot() {
      setSnapshotStatus((current) => (current === 'idle' ? 'loading' : 'refreshing'));
      setSnapshotMessage('');

      try {
        const [products, cart, coupons, orders] = await Promise.all([
          api.getProducts(),
          api.getCart({ customerId }),
          api.getMyCoupons({ customerId }),
          api.getOrders({ customerId })
        ]);

        if (ignore) {
          return;
        }

        setSnapshot({
          products: products || [],
          cartItems: cart?.items || [],
          coupons: coupons || [],
          orders: orders || []
        });
        setLastSyncedAt(formatShortTime());
        setSnapshotStatus('success');
      } catch (error) {
        if (ignore) {
          return;
        }

        setSnapshotMessage(error.message || '상태를 불러오지 못했습니다.');
        setSnapshotStatus('error');
      }
    }

    loadSnapshot();

    return () => {
      ignore = true;
    };
  }, [customerId, latestApiEventId, refreshKey]);

  const pendingOrders = useMemo(
    () => snapshot.orders.filter((order) => order.status === 'PAYMENT_PENDING'),
    [snapshot.orders]
  );
  const nearestPendingOrder = useMemo(() => {
    return [...pendingOrders].sort((left, right) => getTimeValue(left.expiresAt) - getTimeValue(right.expiresAt))[0];
  }, [pendingOrders]);
  const availableCouponCount = snapshot.coupons.filter((coupon) => coupon.status === 'AVAILABLE').length;
  const reservedCouponCount = snapshot.coupons.filter((coupon) => coupon.status === 'RESERVED').length;
  const selectableCartCount = snapshot.cartItems.filter((item) => item.selectable).length;
  const isSnapshotLoading = snapshotStatus === 'loading';
  const isRefreshing = snapshotStatus === 'loading' || snapshotStatus === 'refreshing';

  return (
    <aside className={isOpen ? 'statusPanel' : 'statusPanel isCollapsed'} aria-label="상태 관찰 패널">
      <div className="panelHeader">
        <div className="panelTitle">
          <Activity aria-hidden="true" size={18} />
          <h2>상태 관찰</h2>
        </div>
        <div className="panelActions">
          <button
            aria-label="상태 새로고침"
            className="iconButton"
            disabled={isRefreshing}
            type="button"
            onClick={() => setRefreshKey((current) => current + 1)}
          >
            <RefreshCw aria-hidden="true" className={isRefreshing ? 'spinIcon' : ''} size={18} />
          </button>
          <button
            aria-label={isOpen ? '상태 패널 접기' : '상태 패널 펼치기'}
            className="iconButton"
            type="button"
            onClick={() => setIsOpen((current) => !current)}
          >
            {isOpen ? <ChevronUp aria-hidden="true" size={18} /> : <ChevronDown aria-hidden="true" size={18} />}
          </button>
        </div>
      </div>

      {isOpen && (
        <div className="statusPanelBody">
          <dl className="stateList statusSummary">
            <div>
              <dt>
                <UserRound aria-hidden="true" size={16} />
                고객 ID
              </dt>
              <dd>{customerId}</dd>
            </div>
            <div>
              <dt>
                <Package aria-hidden="true" size={16} />
                상품 재고
              </dt>
              <dd>{snapshot.products.length}종</dd>
            </div>
            <div>
              <dt>
                <ShoppingCart aria-hidden="true" size={16} />
                장바구니
              </dt>
              <dd>{snapshot.cartItems.length}개</dd>
            </div>
            <div>
              <dt>
                <Ticket aria-hidden="true" size={16} />
                쿠폰
              </dt>
              <dd>{availableCouponCount}장</dd>
            </div>
            <div>
              <dt>
                <ReceiptText aria-hidden="true" size={16} />
                결제 대기
              </dt>
              <dd>{pendingOrders.length}건</dd>
            </div>
            <div>
              <dt>
                <Clock3 aria-hidden="true" size={16} />
                최근 API
              </dt>
              <dd>{apiEvents.length ? `${apiEvents.length}건` : '대기'}</dd>
            </div>
          </dl>

          {lastSyncedAt && <p className="statusSyncedAt">동기화 {lastSyncedAt}</p>}

          {snapshotStatus === 'error' && (
            <div className="inlineNotice error">
              <span>{snapshotMessage}</span>
            </div>
          )}

          {nearestPendingOrder && (
            <div className="pendingExpiry">
              <Clock3 aria-hidden="true" size={16} />
              <div>
                <span>가장 가까운 결제 만료</span>
                <strong>주문 #{nearestPendingOrder.orderId} · {formatDateTime(nearestPendingOrder.expiresAt)}</strong>
              </div>
            </div>
          )}

          {isSnapshotLoading ? (
            <div className="statusSkeleton" aria-label="상태 스냅샷 로딩">
              {Array.from({ length: 5 }).map((_, index) => (
                <div className="skeletonLine wide" key={index} />
              ))}
            </div>
          ) : (
            <>
              <StatusSection count={`${snapshot.products.length}종`} icon={Boxes} id="products" title="상품 재고">
                {snapshot.products.length === 0 ? (
                  <EmptyPanelRow text="상품 상태가 아직 없어요." />
                ) : (
                  <div className="statusMiniList">
                    {snapshot.products.slice(0, 5).map((product) => {
                      const stockQuantity = Number(product.stockQuantity || 0);
                      const hasStock = product.status === 'ON_SALE' && stockQuantity > 0;

                      return (
                        <div className="statusMiniRow" key={product.productId}>
                          <div>
                            <strong>{product.name}</strong>
                            <p>{formatWon(getDiscountedPrice(product))} · {productStatusLabels[product.status] || product.status}</p>
                          </div>
                          <span className={hasStock ? 'stockBadge' : 'stockBadge danger'}>{stockQuantity}개</span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </StatusSection>

              <StatusSection count={`${selectableCartCount}/${snapshot.cartItems.length}개 가능`} icon={ShoppingCart} id="cart" title="장바구니">
                {snapshot.cartItems.length === 0 ? (
                  <EmptyPanelRow text="담긴 상품이 없어요." />
                ) : (
                  <div className="statusMiniList">
                    {snapshot.cartItems.slice(0, 4).map((item) => {
                      const lineAmount = getCartItemUnitPrice(item) * Number(item.quantity || 0);

                      return (
                        <div className="statusMiniRow" key={item.cartItemId}>
                          <div>
                            <strong>{item.productName}</strong>
                            <p>{item.quantity}개 · {formatWon(lineAmount)}</p>
                          </div>
                          <span className={item.selectable ? 'statusBadge statusAVAILABLE' : 'statusBadge statusCANCELED'}>
                            {item.selectable ? '가능' : '불가'}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </StatusSection>

              <StatusSection count={`사용 가능 ${availableCouponCount}장 · 예약 ${reservedCouponCount}장`} icon={Ticket} id="coupons" title="보유 쿠폰">
                {snapshot.coupons.length === 0 ? (
                  <EmptyPanelRow text="보유 쿠폰이 없어요." />
                ) : (
                  <div className="statusMiniList">
                    {snapshot.coupons.slice(0, 4).map((coupon) => (
                      <div className="statusMiniRow" key={coupon.issuedCouponId}>
                        <div>
                          <strong>{coupon.name}</strong>
                          <p>{formatWon(coupon.discountAmount)} · 만료 {formatDateTime(coupon.expiresAt)}</p>
                        </div>
                        <span className={`statusBadge status${coupon.status || ''}`}>
                          {couponStatusLabels[coupon.status] || coupon.status}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </StatusSection>

              <StatusSection count={`${snapshot.orders.length}건`} icon={ReceiptText} id="orders" title="최근 주문">
                {snapshot.orders.length === 0 ? (
                  <EmptyPanelRow text="최근 주문이 없어요." />
                ) : (
                  <div className="statusMiniList">
                    {snapshot.orders.slice(0, 4).map((order) => (
                      <div className="statusMiniRow" key={order.orderId}>
                        <div>
                          <strong>주문 #{order.orderId}</strong>
                          <p>{formatWon(order.finalPaymentAmount)} · 만료 {formatDateTime(order.expiresAt)}</p>
                        </div>
                        <span className={`statusBadge orderStatus status${order.status || ''}`}>
                          {orderStatusLabels[order.status] || order.status}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </StatusSection>
            </>
          )}

          <StatusSection count={apiEvents.length ? `${apiEvents.length}건` : '대기'} icon={Activity} id="api-events" title="최근 API">
            <div className="apiTimeline">
              {apiEvents.length === 0 ? (
                <EmptyPanelRow text="호출 기록이 아직 없어요." />
              ) : (
                apiEvents.map((event) => (
                  <div className="apiEvent" key={event.id}>
                    <span className={event.ok ? 'okDot' : 'failDot'} />
                    <div>
                      <strong>{event.method} {event.path}</strong>
                      <p>{event.status} · {event.durationMs}ms · {event.at}</p>
                    </div>
                  </div>
                ))
              )}
            </div>
          </StatusSection>
        </div>
      )}
    </aside>
  );
}
