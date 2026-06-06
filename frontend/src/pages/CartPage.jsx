import { Minus, Plus, RefreshCw, Trash2 } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client.js';
import ProductArtwork from '../components/ProductArtwork.jsx';
import { formatWon, getCartItemUnitPrice } from '../utils/format.js';

export default function CartPage({ customerId, onApiEvent }) {
  const navigate = useNavigate();
  const [cart, setCart] = useState({ items: [] });
  const [selectedIds, setSelectedIds] = useState([]);
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');
  const [pendingItemId, setPendingItemId] = useState(null);

  async function loadCart() {
    setStatus('loading');
    setMessage('');

    try {
      const payload = await api.getCart({ customerId, onApiEvent });
      const items = payload?.items || [];
      setCart({ ...payload, items });
      setSelectedIds(items.filter((item) => item.selectable).map((item) => item.cartItemId));
      setStatus('success');
    } catch (error) {
      setStatus('error');
      setMessage(error.message || '장바구니를 불러오지 못했습니다.');
    }
  }

  useEffect(() => {
    loadCart();
  }, [customerId]);

  const selectableIds = useMemo(
    () => cart.items.filter((item) => item.selectable).map((item) => item.cartItemId),
    [cart.items]
  );
  const allSelected = selectableIds.length > 0 && selectableIds.every((id) => selectedIds.includes(id));
  const selectedItems = cart.items.filter((item) => selectedIds.includes(item.cartItemId));
  const selectedTotal = selectedItems.reduce(
    (sum, item) => sum + getCartItemUnitPrice(item) * Number(item.quantity || 0),
    0
  );

  function toggleItem(cartItemId) {
    setSelectedIds((current) => (
      current.includes(cartItemId)
        ? current.filter((id) => id !== cartItemId)
        : [...current, cartItemId]
    ));
  }

  function toggleAll() {
    setSelectedIds(allSelected ? [] : selectableIds);
  }

  async function updateQuantity(item, quantity) {
    const nextQuantity = Math.max(1, Math.min(quantity, item.stockQuantity));
    setPendingItemId(item.cartItemId);
    setMessage('');

    try {
      await api.updateCartItemQuantity(item.cartItemId, nextQuantity, { customerId, onApiEvent });
      await loadCart();
    } catch (error) {
      setMessage(error.message || '수량을 변경하지 못했습니다.');
    } finally {
      setPendingItemId(null);
    }
  }

  async function deleteItem(cartItemId) {
    setPendingItemId(cartItemId);
    setMessage('');

    try {
      await api.deleteCartItem(cartItemId, { customerId, onApiEvent });
      await loadCart();
    } catch (error) {
      setMessage(error.message || '상품을 삭제하지 못했습니다.');
    } finally {
      setPendingItemId(null);
    }
  }

  function moveToCheckout() {
    window.sessionStorage.setItem('checkout-cart-item-ids', JSON.stringify(selectedIds));
    navigate('/checkout');
  }

  return (
    <section className="pageStack">
      <div className="pageHeader">
        <p className="eyebrow">Cart</p>
        <div>
          <h1>장바구니</h1>
          <p>선택한 굿즈의 수량과 주문 가능 상태를 확인합니다.</p>
        </div>
      </div>

      {status === 'loading' && (
        <div className="cartList">
          {Array.from({ length: 3 }).map((_, index) => (
            <div className="cartRow skeletonCartRow" key={index}>
              <div className="thumb skeleton" />
              <div className="cartText">
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
          <button className="primaryButton" type="button" onClick={loadCart}>
            <RefreshCw aria-hidden="true" size={18} />
            다시 시도
          </button>
        </div>
      )}

      {status === 'success' && cart.items.length === 0 && (
        <div className="noticeBlock">
          <strong>장바구니가 비어 있어요.</strong>
          <button className="primaryButton" type="button" onClick={() => navigate('/products')}>
            상품 보기
          </button>
        </div>
      )}

      {status === 'success' && cart.items.length > 0 && (
        <>
          <div className="cartToolbar">
            <label className="checkboxLine">
              <input checked={allSelected} type="checkbox" onChange={toggleAll} />
              <span>주문 가능 상품 전체 선택</span>
            </label>
            <button className="textButton" type="button" onClick={loadCart}>
              <RefreshCw aria-hidden="true" size={18} />
              새로고침
            </button>
          </div>

          <div className="cartList">
            {cart.items.map((item) => {
              const disabled = pendingItemId === item.cartItemId;
              const unitPrice = getCartItemUnitPrice(item);
              const hasDiscount = Number(item.instantDiscountAmount || 0) > 0;

              return (
                <article className="cartRow" key={item.cartItemId}>
                  <input
                    aria-label={`${item.productName} 선택`}
                    checked={selectedIds.includes(item.cartItemId)}
                    disabled={!item.selectable}
                    type="checkbox"
                    onChange={() => toggleItem(item.cartItemId)}
                  />
                  <ProductArtwork product={item} className="thumb" compact />
                  <div className="cartText">
                    <h2>{item.productName}</h2>
                    <div className="cartPriceLine">
                      <strong>{formatWon(unitPrice)}</strong>
                      {hasDiscount && (
                        <>
                          <del>{formatWon(item.price)}</del>
                          <span>{formatWon(item.instantDiscountAmount)} 즉시 할인</span>
                        </>
                      )}
                    </div>
                    <p>재고 {item.stockQuantity}개</p>
                    {!item.selectable && (
                      <span className="dangerText">{item.notSelectableReason || '주문할 수 없습니다.'}</span>
                    )}
                  </div>
                  <div className="cartActions">
                    <div className="stepper compact" aria-label={`${item.productName} 수량`}>
                      <button
                        aria-label="수량 줄이기"
                        disabled={disabled || item.quantity <= 1}
                        type="button"
                        onClick={() => updateQuantity(item, item.quantity - 1)}
                      >
                        <Minus aria-hidden="true" size={16} />
                      </button>
                      <span>{item.quantity}</span>
                      <button
                        aria-label="수량 늘리기"
                        disabled={disabled || item.quantity >= item.stockQuantity}
                        type="button"
                        onClick={() => updateQuantity(item, item.quantity + 1)}
                      >
                        <Plus aria-hidden="true" size={16} />
                      </button>
                    </div>
                    <button
                      aria-label={`${item.productName} 삭제`}
                      className="iconButton"
                      disabled={disabled}
                      type="button"
                      onClick={() => deleteItem(item.cartItemId)}
                    >
                      <Trash2 aria-hidden="true" size={18} />
                    </button>
                  </div>
                </article>
              );
            })}
          </div>

          {message && <div className="inlineNotice error">{message}</div>}

          <div className="summaryBand">
            <div>
              <span>{selectedItems.length}개 상품</span>
              <strong>{formatWon(selectedTotal)}</strong>
            </div>
            <button
              className="primaryButton"
              disabled={selectedIds.length === 0}
              type="button"
              onClick={moveToCheckout}
            >
              선택 주문
            </button>
          </div>
        </>
      )}
    </section>
  );
}
