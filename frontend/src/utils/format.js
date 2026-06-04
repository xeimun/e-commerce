const contentTypeLabels = {
  WEBTOON: '웹툰',
  WEB_NOVEL: '웹소설',
  MOVIE: '영화',
  DRAMA: '드라마',
  MUSIC: '음악'
};

export function formatWon(value) {
  return `${Number(value || 0).toLocaleString('ko-KR')}원`;
}

export function formatContentType(value) {
  return contentTypeLabels[value] || value || '콘텐츠';
}

export function getDiscountedPrice(product) {
  return Math.max(Number(product.price || 0) - Number(product.instantDiscountAmount || 0), 0);
}

export function getCartItemUnitPrice(item) {
  if (item.discountedPrice !== undefined && item.discountedPrice !== null) {
    return Number(item.discountedPrice);
  }

  return getDiscountedPrice(item);
}

export function getProductTone(productId) {
  const tones = ['toneInk', 'tonePaper', 'toneBlue', 'toneGreen', 'toneCream'];
  return tones[Number(productId || 0) % tones.length];
}
