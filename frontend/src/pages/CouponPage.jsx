import { RefreshCw, Ticket } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client.js';
import { formatWon } from '../utils/format.js';

const couponStatusLabels = {
  AVAILABLE: '사용 가능',
  RESERVED: '결제 대기 예약',
  USED: '사용 완료',
  EXPIRED: '만료'
};

function formatCouponType(type) {
  return type === 'PRODUCT' ? '상품 쿠폰' : '전체 주문 쿠폰';
}

function formatCouponTarget(coupon) {
  return coupon.targetProductId ? `상품 #${coupon.targetProductId} 전용` : '전체 상품 적용';
}

function formatExpiresAt(value) {
  if (!value) {
    return '-';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
}

function CouponCard({ action, coupon, meta, statusLabel }) {
  return (
    <article className="couponCard">
      <div className="couponStub" aria-hidden="true">
        <Ticket size={24} />
      </div>
      <div className="couponBody">
        <div className="couponTitleLine">
          <span className="couponType">{formatCouponType(coupon.type)}</span>
          {statusLabel && <span className={`statusBadge status${coupon.status || ''}`}>{statusLabel}</span>}
        </div>
        <h2>{coupon.name}</h2>
        <strong>{formatWon(coupon.discountAmount)}</strong>
        <dl className="couponMeta">
          <div>
            <dt>적용</dt>
            <dd>{formatCouponTarget(coupon)}</dd>
          </div>
          <div>
            <dt>만료</dt>
            <dd>{formatExpiresAt(coupon.expiresAt)}</dd>
          </div>
        </dl>
        {meta && <p className="couponHint">{meta}</p>}
      </div>
      {action && <div className="couponAction">{action}</div>}
    </article>
  );
}

export default function CouponPage({ customerId, onApiEvent }) {
  const [issuableCoupons, setIssuableCoupons] = useState([]);
  const [myCoupons, setMyCoupons] = useState([]);
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');
  const [messageType, setMessageType] = useState('info');
  const [pendingCouponId, setPendingCouponId] = useState(null);

  async function loadCoupons() {
    setStatus('loading');
    setMessage('');
    setMessageType('info');

    try {
      const [issuablePayload, myPayload] = await Promise.all([
        api.getIssuableCoupons({ customerId, onApiEvent }),
        api.getMyCoupons({ customerId, onApiEvent })
      ]);
      setIssuableCoupons(issuablePayload || []);
      setMyCoupons(myPayload || []);
      setStatus('success');
    } catch (error) {
      setStatus('error');
      setMessage(error.message || '쿠폰을 불러오지 못했습니다.');
      setMessageType('error');
    }
  }

  useEffect(() => {
    loadCoupons();
  }, [customerId]);

  const availableCouponCount = useMemo(
    () => myCoupons.filter((coupon) => coupon.status === 'AVAILABLE').length,
    [myCoupons]
  );

  async function issueCoupon(couponId) {
    setPendingCouponId(couponId);
    setMessage('');
    setMessageType('info');

    try {
      await api.issueCoupon(couponId, { customerId, onApiEvent });
      await loadCoupons();
      setMessage('쿠폰을 발급했어요.');
      setMessageType('success');
    } catch (error) {
      setMessage(error.message || '쿠폰을 발급하지 못했습니다.');
      setMessageType('error');
    } finally {
      setPendingCouponId(null);
    }
  }

  return (
    <section className="pageStack">
      <div className="pageHeader">
        <p className="eyebrow">Coupons</p>
        <div>
          <h1>쿠폰</h1>
          <p>발급 가능한 쿠폰을 받고, 주문에 사용할 보유 쿠폰 상태를 확인합니다.</p>
        </div>
      </div>

      <div className="couponSummary">
        <div>
          <span>사용 가능 쿠폰</span>
          <strong>{availableCouponCount}장</strong>
        </div>
        <div>
          <span>발급 가능</span>
          <strong>{issuableCoupons.filter((coupon) => coupon.issuable).length}장</strong>
        </div>
        <button className="textButton" type="button" onClick={loadCoupons}>
          <RefreshCw aria-hidden="true" size={18} />
          새로고침
        </button>
      </div>

      {message && status !== 'error' && (
        <div className={messageType === 'error' ? 'inlineNotice error' : 'inlineNotice'}>
          <span>{message}</span>
        </div>
      )}

      {status === 'loading' && (
        <div className="couponGrid" aria-label="쿠폰 로딩">
          {Array.from({ length: 4 }).map((_, index) => (
            <div className="couponCard skeletonCouponCard" key={index}>
              <div className="couponStub skeleton" />
              <div className="couponBody">
                <div className="skeletonLine wide" />
                <div className="skeletonLine" />
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
          <button className="primaryButton" type="button" onClick={loadCoupons}>
            <RefreshCw aria-hidden="true" size={18} />
            다시 시도
          </button>
        </div>
      )}

      {status === 'success' && (
        <>
          <section className="couponSection" aria-labelledby="issuable-coupon-title">
            <div className="sectionTitle">
              <h2 id="issuable-coupon-title">발급 가능 쿠폰</h2>
              <span>{issuableCoupons.length}개</span>
            </div>

            {issuableCoupons.length === 0 ? (
              <div className="noticeBlock">
                <strong>발급 가능한 쿠폰이 없어요.</strong>
                <p>데모 데이터 초기화 후 다시 확인할 수 있습니다.</p>
              </div>
            ) : (
              <div className="couponGrid">
                {issuableCoupons.map((coupon) => {
                  const disabled = !coupon.issuable || pendingCouponId === coupon.couponId;

                  return (
                    <CouponCard
                      key={coupon.couponId}
                      coupon={coupon}
                      meta={coupon.issuable ? '아직 발급받지 않은 쿠폰입니다.' : '이미 보유 중인 쿠폰입니다.'}
                      action={(
                        <button
                          className="primaryButton"
                          disabled={disabled}
                          type="button"
                          onClick={() => issueCoupon(coupon.couponId)}
                        >
                          {coupon.issuable ? '발급 받기' : '발급 완료'}
                        </button>
                      )}
                    />
                  );
                })}
              </div>
            )}
          </section>

          <section className="couponSection" aria-labelledby="my-coupon-title">
            <div className="sectionTitle">
              <h2 id="my-coupon-title">내 보유 쿠폰</h2>
              <span>{myCoupons.length}장</span>
            </div>

            {myCoupons.length === 0 ? (
              <div className="noticeBlock">
                <strong>보유 쿠폰이 없어요.</strong>
                <p>발급 가능 쿠폰을 먼저 받아보세요.</p>
              </div>
            ) : (
              <div className="couponGrid">
                {myCoupons.map((coupon) => (
                  <CouponCard
                    key={coupon.issuedCouponId}
                    coupon={coupon}
                    meta={`보유 쿠폰 #${coupon.issuedCouponId}`}
                    statusLabel={couponStatusLabels[coupon.status] || coupon.status}
                  />
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </section>
  );
}
