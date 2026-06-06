import sunnySideUpImage from '../assets/products/sunny-side-up-lp.jpg';
import savoiaS21Image from '../assets/products/savoia-s21-figure.jpg';
import cloudWorkshopImage from '../assets/products/cloud-workshop-original-drawing.jpg';
import myokoPlushImage from '../assets/products/myoko-plush.jpg';
import cyberpunkKeyringImage from '../assets/products/cyberpunk-2088-keyring.jpg';
import { formatWon } from '../utils/format.js';

const productImages = {
  1: { src: sunnySideUpImage },
  2: { src: savoiaS21Image },
  3: { src: cloudWorkshopImage },
  4: { src: myokoPlushImage },
  5: { src: cyberpunkKeyringImage },
  10001: { src: sunnySideUpImage },
  10002: { src: savoiaS21Image },
  10003: { src: cloudWorkshopImage },
  10004: { src: myokoPlushImage },
  10005: { src: cyberpunkKeyringImage }
};

function resolveProductImage(product = {}) {
  const productId = Number(product.productId || product.id || 0);

  if (productImages[productId]) {
    return productImages[productId];
  }

  const searchableText = [
    product.name,
    product.productName,
    product.contentTitle,
    product.category
  ].filter(Boolean).join(' ');

  if (/sunny|side up|soundtrack|lp|사운드트랙/i.test(searchableText)) {
    return productImages[10001];
  }

  if (/savoia|s-21|수상비행기|피규어/i.test(searchableText)) {
    return productImages[10002];
  }

  if (/구름을 만드는 정비소|하레|드로잉|원화/i.test(searchableText)) {
    return productImages[10003];
  }

  if (/묘코|myoko|마스코트|봉제/i.test(searchableText)) {
    return productImages[10004];
  }

  if (/사이버펑크|cyberpunk|2088|키링|keyring/i.test(searchableText)) {
    return productImages[10005];
  }

  return null;
}

export default function ProductArtwork({ className = '', compact = false, product = {}, showBadges = true }) {
  const image = resolveProductImage(product);
  const hasDiscount = Number(product.instantDiscountAmount || 0) > 0;

  return (
    <div
      aria-hidden="true"
      className={`productArtwork ${className} ${compact ? 'compactArtwork' : ''}`.trim()}
    >
      {image ? (
        <img
          className="artworkImage"
          src={image.src}
          alt=""
          loading="lazy"
          draggable="false"
        />
      ) : (
        <div className="artworkFallback">
          <span>DG</span>
        </div>
      )}
      {showBadges && !compact && hasDiscount && (
        <span className="saleFlag">
          {formatWon(product.instantDiscountAmount)} 할인
        </span>
      )}
    </div>
  );
}
