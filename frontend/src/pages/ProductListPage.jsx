import { RefreshCw } from 'lucide-react';
import { useEffect, useState } from 'react';
import { api } from '../api/client.js';
import ProductCard from '../components/ProductCard.jsx';

export default function ProductListPage({ onApiEvent }) {
  const [products, setProducts] = useState([]);
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');

  async function loadProducts() {
    setStatus('loading');
    setMessage('');

    try {
      const payload = await api.getProducts({ onApiEvent });
      setProducts(payload || []);
      setStatus('success');
    } catch (error) {
      setStatus('error');
      setMessage(error.message || '상품을 불러오지 못했습니다.');
    }
  }

  useEffect(() => {
    loadProducts();
  }, []);

  return (
    <section className="pageStack">
      <div className="pageHeader">
        <p className="eyebrow">Drop Goods</p>
        <div>
          <h1>콘텐츠 IP 한정판 굿즈</h1>
          <p>모든 콘텐츠 IP의 한정판 드롭 상품을 한곳에서 만나보세요.</p>
        </div>
      </div>

      {status === 'loading' && (
        <div className="productGrid" aria-label="상품 로딩">
          {Array.from({ length: 8 }).map((_, index) => (
            <div className="bookCard skeletonCard" key={index}>
              <div className="cover skeleton" />
              <div className="skeletonLine wide" />
              <div className="skeletonLine" />
            </div>
          ))}
        </div>
      )}

      {status === 'error' && (
        <div className="noticeBlock">
          <strong>연결이 불안정해요</strong>
          <p>{message}</p>
          <button className="primaryButton" type="button" onClick={loadProducts}>
            <RefreshCw aria-hidden="true" size={18} />
            다시 시도
          </button>
        </div>
      )}

      {status === 'success' && products.length === 0 && (
        <div className="noticeBlock">
          <strong>아직 등록된 상품이 없어요.</strong>
          <button className="primaryButton" type="button" onClick={loadProducts}>
            <RefreshCw aria-hidden="true" size={18} />
            새로고침
          </button>
        </div>
      )}

      {status === 'success' && products.length > 0 && (
        <div className="productGrid">
          {products.map((product) => (
            <ProductCard key={product.productId} product={product} />
          ))}
        </div>
      )}
    </section>
  );
}
