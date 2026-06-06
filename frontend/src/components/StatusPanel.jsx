import {
  Activity,
  Boxes,
  ChevronDown,
  ChevronUp,
  Package,
  RefreshCw,
  ShoppingCart,
  Ticket,
  UserRound
} from 'lucide-react';
import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import { formatDateTime, formatWon, getCartItemUnitPrice, getDiscountedPrice } from '../utils/format.js';

const couponStatusLabels = {
  AVAILABLE: '사용 가능',
  RESERVED: '예약',
  USED: '사용 완료',
  EXPIRED: '만료'
};

const productStatusLabels = {
  ON_SALE: '판매 중',
  STOPPED: '판매 중지'
};

const emptySnapshot = {
  products: [],
  cartItems: [],
  coupons: []
};

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

function StatusSection({ children, className = '', count, icon: Icon, id, title }) {
  return (
    <section className={`statusSection ${className}`} aria-labelledby={`status-${id}`}>
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

function StatusSignal({ icon: Icon, label, meta, value }) {
  return (
    <div className="statusSignal">
      <Icon aria-hidden="true" size={16} />
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
        {meta && <small>{meta}</small>}
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
  const latestApiEvent = apiEvents[0];
  const latestApiEventId = latestApiEvent?.id || '';

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
          products: products || [],
          cartItems: cart?.items || [],
          coupons: coupons || []
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

  const availableCouponCount = snapshot.coupons.filter((coupon) => coupon.status === 'AVAILABLE').length;
  const reservedCouponCount = snapshot.coupons.filter((coupon) => coupon.status === 'RESERVED').length;
  const selectableCartCount = snapshot.cartItems.filter((item) => item.selectable).length;
  const lowStockCount = snapshot.products.filter((product) => {
    const stockQuantity = Number(product.stockQuantity || 0);
    return product.status !== 'ON_SALE' || stockQuantity <= 10;
  }).length;
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
          <div className="statusOverview" aria-label="상태 요약">
            <StatusSignal icon={UserRound} label="고객 ID" value={customerId} meta="X-Customer-Id" />
            <StatusSignal
              icon={Package}
              label="상품 재고"
              value={`${snapshot.products.length}종`}
              meta={lowStockCount > 0 ? `주의 ${lowStockCount}종` : '전체 정상'}
            />
            <StatusSignal
              icon={ShoppingCart}
              label="장바구니"
              value={`${snapshot.cartItems.length}개`}
              meta={`${selectableCartCount}개 주문 가능`}
            />
            <StatusSignal
              icon={Ticket}
              label="보유 쿠폰"
              value={`${availableCouponCount}장`}
              meta={reservedCouponCount > 0 ? `예약 ${reservedCouponCount}장` : '예약 없음'}
            />
            <StatusSignal
              icon={Activity}
              label="최근 API"
              value={latestApiEvent ? `${latestApiEvent.status}` : '대기'}
              meta={latestApiEvent ? `${latestApiEvent.method} ${latestApiEvent.path}` : '호출 없음'}
            />
          </div>

          {lastSyncedAt && <p className="statusSyncedAt">동기화 {lastSyncedAt}</p>}

          {snapshotStatus === 'error' && (
            <div className="inlineNotice error">
              <span>{snapshotMessage}</span>
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
                      const hasDiscount = Number(product.instantDiscountAmount || 0) > 0;

                      return (
                        <div className="statusMiniRow" key={product.productId}>
                          <div>
                            <strong>{product.name}</strong>
                            <p>
                              {formatWon(getDiscountedPrice(product))}
                              {hasDiscount && ` · 즉시 할인 ${formatWon(product.instantDiscountAmount)}`}
                              {!hasDiscount && ` · ${productStatusLabels[product.status] || product.status}`}
                            </p>
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

              <StatusSection count={`사용 가능 ${availableCouponCount}장`} icon={Ticket} id="coupons" title="보유 쿠폰">
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
            </>
          )}

          <StatusSection
            className="apiStatusSection"
            count={apiEvents.length ? `${apiEvents.length}건` : '대기'}
            icon={Activity}
            id="api-events"
            title="호출 API"
          >
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
