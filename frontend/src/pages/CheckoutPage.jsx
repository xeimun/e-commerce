import {
  ArrowLeft,
  CheckCircle2,
  CreditCard,
  RefreshCw,
  TicketPercent,
  X
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client.js';
import { formatWon, getCartItemUnitPrice } from '../utils/format.js';

const CHECKOUT_CART_ITEM_IDS_KEY = 'checkout-cart-item-ids';

function readCheckoutCartItemIds() {
  try {
    const rawValue = window.sessionStorage.getItem(CHECKOUT_CART_ITEM_IDS_KEY);
    const parsedValue = rawValue ? JSON.parse(rawValue) : [];

    if (!Array.isArray(parsedValue)) {
      return [];
    }

    return parsedValue.map(Number).filter(Number.isFinite);
  } catch {
    return [];
  }
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

function findCouponById(coupons, couponId) {
  return coupons.find((coupon) => coupon.issuedCouponId === Number(couponId));
}

function getProductCouponDiscount(item, coupon) {
  if (!coupon) {
    return 0;
  }

  return Math.min(Number(coupon.discountAmount || 0), getCartItemUnitPrice(item));
}

function CheckoutLineItem({ item, onRemove, productCoupon, productCoupons, onProductCouponChange }) {
  const unitPrice = getCartItemUnitPrice(item);
  const originalAmount = Number(item.price || 0) * Number(item.quantity || 0);
  const instantDiscountAmount = Number(item.instantDiscountAmount || 0) * Number(item.quantity || 0);
  const lineAmount = unitPrice * Number(item.quantity || 0);
  const productCouponDiscount = getProductCouponDiscount(item, productCoupon);

  return (
    <article className="checkoutLineItem">
      <div className="thumb checkoutThumb">
        <span>{item.productName.slice(0, 2)}</span>
      </div>
      <div className="checkoutItemBody">
        <div>
          <h2>{item.productName}</h2>
          <p>수량 {item.quantity}개 · 재고 {item.stockQuantity}개</p>
        </div>
        <div className="checkoutPriceMeta">
          <span>상품 금액 {formatWon(originalAmount)}</span>
          {instantDiscountAmount > 0 && <span>즉시 할인 -{formatWon(instantDiscountAmount)}</span>}
          {productCouponDiscount > 0 && <span>상품 쿠폰 -{formatWon(productCouponDiscount)}</span>}
        </div>
      </div>
      <div className="checkoutItemAction">
        <label className="fieldLabel" htmlFor={`product-coupon-${item.cartItemId}`}>
          상품 쿠폰
        </label>
        <select
          id={`product-coupon-${item.cartItemId}`}
          className="selectControl"
          value={productCoupon?.issuedCouponId || ''}
          onChange={(event) => onProductCouponChange(item.cartItemId, event.target.value)}
        >
          <option value="">사용 안 함</option>
          {productCoupons.map((coupon) => (
            <option key={coupon.issuedCouponId} value={coupon.issuedCouponId}>
              {coupon.name} · {formatWon(coupon.discountAmount)}
            </option>
          ))}
        </select>
        <strong>{formatWon(lineAmount - productCouponDiscount)}</strong>
      </div>
      <button
        className="iconButton"
        type="button"
        aria-label={`${item.productName} 제외`}
        onClick={() => onRemove(item.cartItemId)}
      >
        <X aria-hidden="true" size={18} />
      </button>
    </article>
  );
}

export default function CheckoutPage({ customerId, onApiEvent }) {
  const navigate = useNavigate();
  const [cart, setCart] = useState({ items: [] });
  const [selectedIds, setSelectedIds] = useState(() => readCheckoutCartItemIds());
  const [myCoupons, setMyCoupons] = useState([]);
  const [selectedOrderCouponId, setSelectedOrderCouponId] = useState('');
  const [productCouponByCartItemId, setProductCouponByCartItemId] = useState({});
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');
  const [mutationStatus, setMutationStatus] = useState('idle');
  const [createdOrder, setCreatedOrder] = useState(null);

  async function loadCheckout() {
    setStatus('loading');
    setMessage('');
    setMutationStatus('idle');
    setCreatedOrder(null);

    try {
      const [cartPayload, couponPayload] = await Promise.all([
        api.getCart({ customerId, onApiEvent }),
        api.getMyCoupons({ customerId, onApiEvent })
      ]);
      const cartItems = cartPayload?.items || [];
      const checkoutIds = readCheckoutCartItemIds();
      const validIds = checkoutIds.filter((cartItemId) => {
        const item = cartItems.find((cartItem) => cartItem.cartItemId === cartItemId);
        return item?.selectable;
      });

      setCart({ ...cartPayload, items: cartItems });
      setMyCoupons(couponPayload || []);
      setSelectedIds(validIds);
      setProductCouponByCartItemId((current) => {
        const next = {};
        validIds.forEach((cartItemId) => {
          if (current[cartItemId]) {
            next[cartItemId] = current[cartItemId];
          }
        });
        return next;
      });
      setStatus('success');
    } catch (error) {
      setStatus('error');
      setMessage(error.message || '체크아웃 정보를 불러오지 못했습니다.');
    }
  }

  useEffect(() => {
    setSelectedIds(readCheckoutCartItemIds());
    loadCheckout();
  }, [customerId]);

  const selectedItems = useMemo(
    () => selectedIds
      .map((cartItemId) => cart.items.find((item) => item.cartItemId === cartItemId))
      .filter(Boolean),
    [cart.items, selectedIds]
  );

  const availableCoupons = useMemo(
    () => myCoupons.filter((coupon) => coupon.status === 'AVAILABLE'),
    [myCoupons]
  );
  const orderCoupons = availableCoupons.filter((coupon) => coupon.type === 'ORDER');
  const productCoupons = availableCoupons.filter((coupon) => coupon.type === 'PRODUCT');
  const selectedOrderCoupon = findCouponById(orderCoupons, selectedOrderCouponId);

  const totals = useMemo(() => {
    const totalProductAmount = selectedItems.reduce(
      (sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0),
      0
    );
    const totalInstantDiscountAmount = selectedItems.reduce(
      (sum, item) => sum + Number(item.instantDiscountAmount || 0) * Number(item.quantity || 0),
      0
    );
    const afterInstantDiscountAmount = selectedItems.reduce(
      (sum, item) => sum + getCartItemUnitPrice(item) * Number(item.quantity || 0),
      0
    );
    const totalProductCouponDiscountAmount = selectedItems.reduce((sum, item) => {
      const coupon = findCouponById(productCoupons, productCouponByCartItemId[item.cartItemId]);
      return sum + getProductCouponDiscount(item, coupon);
    }, 0);
    const orderCouponBaseAmount = Math.max(afterInstantDiscountAmount - totalProductCouponDiscountAmount, 0);
    const orderCouponDiscountAmount = selectedOrderCoupon
      ? Math.min(Number(selectedOrderCoupon.discountAmount || 0), orderCouponBaseAmount)
      : 0;
    const totalCouponDiscountAmount = totalProductCouponDiscountAmount + orderCouponDiscountAmount;
    const finalPaymentAmount = Math.max(afterInstantDiscountAmount - totalCouponDiscountAmount, 0);

    return {
      finalPaymentAmount,
      orderCouponDiscountAmount,
      totalCouponDiscountAmount,
      totalInstantDiscountAmount,
      totalProductAmount,
      totalProductCouponDiscountAmount
    };
  }, [productCouponByCartItemId, productCoupons, selectedItems, selectedOrderCoupon]);

  function removeSelectedItem(cartItemId) {
    setSelectedIds((current) => {
      const next = current.filter((id) => id !== cartItemId);
      window.sessionStorage.setItem(CHECKOUT_CART_ITEM_IDS_KEY, JSON.stringify(next));
      return next;
    });
    setProductCouponByCartItemId((current) => {
      const next = { ...current };
      delete next[cartItemId];
      return next;
    });
  }

  function changeProductCoupon(cartItemId, couponId) {
    setProductCouponByCartItemId((current) => {
      const next = { ...current };

      if (!couponId) {
        delete next[cartItemId];
        return next;
      }

      next[cartItemId] = Number(couponId);
      return next;
    });
  }

  function getProductCouponsForItem(item) {
    return productCoupons.filter((coupon) => coupon.targetProductId === item.productId);
  }

  async function createOrder() {
    setMutationStatus('pending');
    setMessage('');
    setCreatedOrder(null);

    const productCouponRequests = selectedItems
      .map((item) => {
        const couponId = productCouponByCartItemId[item.cartItemId];

        if (!couponId) {
          return null;
        }

        return {
          cartItemId: item.cartItemId,
          couponId: Number(couponId)
        };
      })
      .filter(Boolean);

    try {
      const payload = await api.createOrder({
        cartItemIds: selectedIds,
        orderCouponId: selectedOrderCouponId ? Number(selectedOrderCouponId) : null,
        productCoupons: productCouponRequests
      }, { customerId, onApiEvent });

      window.sessionStorage.removeItem(CHECKOUT_CART_ITEM_IDS_KEY);
      setCreatedOrder(payload);
      setMutationStatus('success');
    } catch (error) {
      setMutationStatus('error');
      setMessage(error.message || '주문을 생성하지 못했습니다.');
    }
  }

  if (status === 'loading') {
    return (
      <section className="pageStack">
        <div className="pageHeader">
          <p className="eyebrow">Checkout</p>
          <div>
            <h1>체크아웃</h1>
            <p>주문 상품과 적용 쿠폰을 확인합니다.</p>
          </div>
        </div>
        <div className="checkoutLayout">
          <div className="checkoutList">
            {Array.from({ length: 3 }).map((_, index) => (
              <div className="checkoutLineItem skeletonCheckoutLine" key={index}>
                <div className="thumb skeleton" />
                <div className="checkoutItemBody">
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
        <Link className="textButton" to="/cart">
          <ArrowLeft aria-hidden="true" size={18} />
          장바구니
        </Link>
        <div className="noticeBlock">
          <strong>연결이 불안정해요</strong>
          <p>{message}</p>
          <button className="primaryButton" type="button" onClick={loadCheckout}>
            <RefreshCw aria-hidden="true" size={18} />
            다시 시도
          </button>
        </div>
      </section>
    );
  }

  if (createdOrder) {
    return (
      <section className="pageStack">
        <div className="checkoutComplete">
          <CheckCircle2 aria-hidden="true" size={32} />
          <p className="eyebrow">Payment Pending</p>
          <h1>주문이 생성됐어요.</h1>
          <dl className="checkoutCompleteFacts">
            <div>
              <dt>주문 번호</dt>
              <dd>#{createdOrder.orderId}</dd>
            </div>
            <div>
              <dt>결제 금액</dt>
              <dd>{formatWon(createdOrder.finalPaymentAmount)}</dd>
            </div>
            <div>
              <dt>결제 만료</dt>
              <dd>{formatExpiresAt(createdOrder.expiresAt)}</dd>
            </div>
          </dl>
          <div className="checkoutCompleteActions">
            <button
              className="primaryButton"
              type="button"
              onClick={() => navigate(`/orders/${createdOrder.orderId}`)}
            >
              주문 상세
            </button>
            <button className="textButton" type="button" onClick={() => navigate('/products')}>
              상품 더 보기
            </button>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="pageStack">
      <Link className="textButton" to="/cart">
        <ArrowLeft aria-hidden="true" size={18} />
        장바구니
      </Link>

      <div className="pageHeader">
        <p className="eyebrow">Checkout</p>
        <div>
          <h1>체크아웃</h1>
          <p>주문 상품과 적용 쿠폰을 확인합니다.</p>
        </div>
      </div>

      {selectedItems.length === 0 ? (
        <div className="noticeBlock">
          <strong>주문할 상품이 없어요.</strong>
          <p>장바구니에서 주문 가능한 상품을 선택해 주세요.</p>
          <button className="primaryButton" type="button" onClick={() => navigate('/cart')}>
            장바구니 보기
          </button>
        </div>
      ) : (
        <div className="checkoutLayout">
          <div className="checkoutMain">
            <section className="checkoutSection" aria-labelledby="checkout-items-title">
              <div className="sectionTitle">
                <h2 id="checkout-items-title">주문 상품</h2>
                <span>{selectedItems.length}개</span>
              </div>
              <div className="checkoutList">
                {selectedItems.map((item) => {
                  const selectedProductCoupon = findCouponById(
                    productCoupons,
                    productCouponByCartItemId[item.cartItemId]
                  );

                  return (
                    <CheckoutLineItem
                      key={item.cartItemId}
                      item={item}
                      productCoupon={selectedProductCoupon}
                      productCoupons={getProductCouponsForItem(item)}
                      onProductCouponChange={changeProductCoupon}
                      onRemove={removeSelectedItem}
                    />
                  );
                })}
              </div>
            </section>

            <section className="checkoutSection" aria-labelledby="checkout-coupons-title">
              <div className="sectionTitle">
                <h2 id="checkout-coupons-title">전체 주문 쿠폰</h2>
                <span>{orderCoupons.length}장</span>
              </div>
              <div className="orderCouponBox">
                <TicketPercent aria-hidden="true" size={22} />
                <div>
                  <label className="fieldLabel" htmlFor="order-coupon">
                    적용 쿠폰
                  </label>
                  <select
                    id="order-coupon"
                    className="selectControl"
                    value={selectedOrderCouponId}
                    onChange={(event) => setSelectedOrderCouponId(event.target.value)}
                  >
                    <option value="">사용 안 함</option>
                    {orderCoupons.map((coupon) => (
                      <option key={coupon.issuedCouponId} value={coupon.issuedCouponId}>
                        {coupon.name} · {formatWon(coupon.discountAmount)}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </section>
          </div>

          <aside className="checkoutSummary" aria-label="주문 금액">
            <div className="panelHeader">
              <CreditCard aria-hidden="true" size={18} />
              <h2>결제 금액</h2>
            </div>
            <dl className="summaryRows">
              <div>
                <dt>상품 금액</dt>
                <dd>{formatWon(totals.totalProductAmount)}</dd>
              </div>
              <div>
                <dt>즉시 할인</dt>
                <dd>-{formatWon(totals.totalInstantDiscountAmount)}</dd>
              </div>
              <div>
                <dt>상품 쿠폰</dt>
                <dd>-{formatWon(totals.totalProductCouponDiscountAmount)}</dd>
              </div>
              <div>
                <dt>전체 주문 쿠폰</dt>
                <dd>-{formatWon(totals.orderCouponDiscountAmount)}</dd>
              </div>
              <div className="summaryTotal">
                <dt>최종 결제 금액</dt>
                <dd>{formatWon(totals.finalPaymentAmount)}</dd>
              </div>
            </dl>

            {message && <div className="inlineNotice error">{message}</div>}

            <button
              className="primaryButton checkoutSubmit"
              disabled={mutationStatus === 'pending'}
              type="button"
              onClick={createOrder}
            >
              {mutationStatus === 'pending' ? '주문 생성 중' : '결제 대기 주문 생성'}
            </button>
          </aside>
        </div>
      )}
    </section>
  );
}
