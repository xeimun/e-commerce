import { ShoppingBag } from 'lucide-react';
import { Link } from 'react-router-dom';
import { formatWon, getDiscountedPrice } from '../utils/format.js';
import ProductArtwork from './ProductArtwork.jsx';

export default function ProductCard({ product }) {
  const hasDiscount = Number(product.instantDiscountAmount || 0) > 0;
  const discountedPrice = getDiscountedPrice(product);
  const soldOut = Number(product.stockQuantity || 0) <= 0;

  return (
    <Link className="bookCard" to={`/products/${product.productId}`}>
      <ProductArtwork product={product} className="cover" />

      <div className="bookMeta">
        <h2>{product.name}</h2>
        <p>{product.category || '한정판 굿즈'}</p>
        <div className="priceLine">
          <span className={hasDiscount ? 'discountPrice' : undefined}>
            {formatWon(discountedPrice)}
          </span>
          {hasDiscount && <del>{formatWon(product.price)}</del>}
        </div>
        <div className="stockLine">
          <ShoppingBag aria-hidden="true" size={16} />
          <span className={soldOut ? 'dangerText' : undefined}>
            {soldOut ? '품절' : `${product.stockQuantity}개 남음`}
          </span>
        </div>
      </div>
    </Link>
  );
}
