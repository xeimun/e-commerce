import {
  Activity,
  ChevronDown,
  ChevronUp,
  ClipboardList,
  Package,
  RefreshCw,
  ShoppingCart,
  Ticket,
  TriangleAlert,
  UserRound
} from 'lucide-react';
import { useEffect, useState } from 'react';
import { api } from '../api/client.js';

const emptySnapshot = {
  products: [],
  cartItems: [],
  coupons: []
};

function asArray(value) {
  return Array.isArray(value) ? value : [];
}

function formatShortTime() {
  return new Date().toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
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

function StatusSummaryCard({ icon: Icon, label, value }) {
  return (
    <div className="statusSummaryCard">
      <Icon aria-hidden="true" size={16} />
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
    </div>
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
        const [products, cart, coupons] = await Promise.all([
          api.getProducts(),
          api.getCart({ customerId }),
          api.getMyCoupons({ customerId })
        ]);

        if (ignore) {
          return;
        }

        setSnapshot({
          products: asArray(products),
          cartItems: asArray(cart?.items),
          coupons: asArray(coupons)
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

  const products = asArray(snapshot.products);
  const cartItems = asArray(snapshot.cartItems);
  const coupons = asArray(snapshot.coupons);
  const availableCouponCount = coupons.filter((coupon) => coupon.status === 'AVAILABLE').length;
  const stockAttentionCount = products.filter((product) => {
    const stockQuantity = Number(product.stockQuantity || 0);
    return product.status !== 'ON_SALE' || stockQuantity <= 10;
  }).length;
  const isSnapshotLoading = snapshotStatus === 'loading';
  const isRefreshing = snapshotStatus === 'loading' || snapshotStatus === 'refreshing';

  return (
    <aside className={isOpen ? 'statusPanel' : 'statusPanel isCollapsed'} aria-label="상태 요약 패널">
      <div className="panelHeader">
        <div className="panelTitle">
          <ClipboardList aria-hidden="true" size={18} />
          <h2>상태 요약</h2>
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
          <div className="statusSummary" aria-label="상태 요약">
            <StatusSummaryCard icon={UserRound} label="고객 ID (X-Customer-Id)" value={customerId} />
            <StatusSummaryCard
              icon={Package}
              label="상품 재고"
              value={isSnapshotLoading ? '확인 중' : `${products.length}종`}
            />
            <StatusSummaryCard
              icon={ShoppingCart}
              label="장바구니"
              value={isSnapshotLoading ? '확인 중' : `${cartItems.length}개`}
            />
            <StatusSummaryCard
              icon={Ticket}
              label="보유 쿠폰"
              value={isSnapshotLoading ? '확인 중' : `${availableCouponCount}장`}
            />
            <StatusSummaryCard
              icon={TriangleAlert}
              label="재고 주의"
              value={isSnapshotLoading ? '확인 중' : `${stockAttentionCount}종`}
            />
          </div>

          {lastSyncedAt && <p className="statusSyncedAt">동기화 {lastSyncedAt}</p>}

          {snapshotStatus === 'error' && (
            <div className="inlineNotice error">
              <span>{snapshotMessage}</span>
            </div>
          )}

          <StatusSection count={apiEvents.length ? `${apiEvents.length}건` : '대기'} icon={Activity} id="api-events" title="호출 API">
            <div className="apiTimeline">
              {apiEvents.length === 0 ? (
                <EmptyPanelRow text="호출 기록이 아직 없어요." />
              ) : (
                apiEvents.slice(0, 8).map((event) => (
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
