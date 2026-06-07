import { ArrowLeft, Minus, Plus, ShoppingCart } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client.js';
import ProductArtwork from '../components/ProductArtwork.jsx';
import {
  formatWon,
  getDiscountedPrice
} from '../utils/format.js';

export default function ProductDetailPage({ customerId, onApiEvent }) {
  const { productId } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');
  const [mutationStatus, setMutationStatus] = useState('idle');

  async function loadProduct() {
    setStatus('loading');
    setMessage('');

    try {
      const payload = await api.getProduct(productId, { onApiEvent });
      setProduct(payload);
      setStatus('success');
      setQuantity(1);
    } catch (error) {
      setStatus('error');
      setMessage(error.message || '상품을 불러오지 못했습니다.');
    }
  }

  useEffect(() => {
    loadProduct();
  }, [productId]);

  const maxQuantity = Math.max(Number(product?.stockQuantity || 0), 0);
  const finalPrice = useMemo(() => getDiscountedPrice(product || {}), [product]);
  const canBuy = product?.status === 'ON_SALE' && maxQuantity > 0;

  async function addToCart() {
    if (!canBuy) {
      return;
    }

    setMutationStatus('pending');
    setMessage('');

    try {
      await api.addCartItem(product.productId, quantity, {
        customerId,
        onApiEvent
      });
      setMutationStatus('success');
      setMessage('장바구니에 담았어요.');
    } catch (error) {
      setMutationStatus('error');
      setMessage(error.message || '장바구니에 담지 못했습니다.');
    }
  }

  if (status === 'loading') {
    return (
      <section className="pageStack">
        <div className="detailSkeleton">
          <div className="detailCover skeleton" />
          <div className="detailInfo">
            <div className="skeletonLine wide" />
            <div className="skeletonLine wide" />
            <div className="skeletonLine" />
          </div>
        </div>
      </section>
    );
  }

  if (status === 'error') {
    return (
      <section className="pageStack">
        <Link className="textButton" to="/products">
          <ArrowLeft aria-hidden="true" size={18} />
          상품 목록
        </Link>
        <div className="noticeBlock">
          <strong>상품을 찾지 못했어요.</strong>
          <p>{message}</p>
          <button className="primaryButton" type="button" onClick={loadProduct}>
            다시 시도
          </button>
        </div>
      </section>
    );
  }

  return (
    <section className="pageStack">
      <Link className="textButton" to="/products">
        <ArrowLeft aria-hidden="true" size={18} />
        상품 목록
      </Link>

      <div className="productDetail">
        <ProductArtwork product={product} className="detailCover" showBadges={false} />

        <div className="detailInfo">
          <div className="detailMetaBlock">
            <span className="detailCategory">{product.category || '한정판 굿즈'}</span>
            <div className="detailMetaRow">
              <span>IP 라이선스</span>
              <strong>{product.contentTitle || '-'}</strong>
            </div>
          </div>

          <h1>{product.name}</h1>
          <p className="detailDescription">{product.description}</p>

          <div className="detailPrice">
            <strong>{formatWon(finalPrice)}</strong>
            {Number(product.instantDiscountAmount || 0) > 0 && (
              <span>
                <del>{formatWon(product.price)}</del>
                <em>{formatWon(product.instantDiscountAmount)} 즉시 할인</em>
              </span>
            )}
          </div>

          <dl className="detailRows">
            <div>
              <dt>재고</dt>
              <dd>{product.stockQuantity}개</dd>
            </div>
            <div>
              <dt>수량</dt>
              <dd>
                <div className="stepper detailStepper" aria-label="수량">
                  <button
                    aria-label="수량 줄이기"
                    disabled={quantity <= 1}
                    type="button"
                    onClick={() => setQuantity((current) => Math.max(current - 1, 1))}
                  >
                    <Minus aria-hidden="true" size={18} />
                  </button>
                  <span>{quantity}</span>
                  <button
                    aria-label="수량 늘리기"
                    disabled={quantity >= maxQuantity}
                    type="button"
                    onClick={() => setQuantity((current) => Math.min(current + 1, maxQuantity))}
                  >
                    <Plus aria-hidden="true" size={18} />
                  </button>
                </div>
              </dd>
            </div>
          </dl>

          <button
            className="primaryButton detailCartButton"
            disabled={!canBuy || mutationStatus === 'pending'}
            type="button"
            onClick={addToCart}
          >
            <ShoppingCart aria-hidden="true" size={18} />
            {canBuy ? '장바구니 담기' : '구매 불가'}
          </button>

          {message && (
            <div className={mutationStatus === 'error' ? 'inlineNotice error' : 'inlineNotice'}>
              <span>{message}</span>
              {mutationStatus === 'success' && (
                <button type="button" onClick={() => navigate('/cart')}>
                  장바구니
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
