import { formatContentType, formatWon } from '../utils/format.js';

const productArtworkProfiles = {
  1: { kind: 'artbook', tone: 'artMoon', label: '달빛 상점', contentType: 'WEBTOON' },
  2: { kind: 'poster', tone: 'artStar', label: '검은별 기록관', contentType: 'WEBTOON' },
  3: { kind: 'acrylic', tone: 'artLibrary', label: '푸른 사서의 방', contentType: 'WEB_NOVEL' },
  4: { kind: 'ost', tone: 'artOrchestra', label: '라스트 오케스트라', contentType: 'MUSIC' },
  5: { kind: 'sticker', tone: 'artDawn', label: '새벽의 문장', contentType: 'WEB_NOVEL' },
  10001: { kind: 'artbook', tone: 'artMoon', label: '달빛 상점', contentType: 'WEBTOON' },
  10002: { kind: 'poster', tone: 'artStar', label: '검은별 기록관', contentType: 'WEBTOON' },
  10003: { kind: 'acrylic', tone: 'artLibrary', label: '푸른 사서의 방', contentType: 'WEB_NOVEL' },
  10004: { kind: 'ost', tone: 'artOrchestra', label: '라스트 오케스트라', contentType: 'MUSIC' },
  10005: { kind: 'sticker', tone: 'artDawn', label: '새벽의 문장', contentType: 'WEB_NOVEL' }
};

const fallbackProfiles = [
  { kind: 'artbook', tone: 'artMoon', label: 'Drop Goods', contentType: 'WEBTOON' },
  { kind: 'poster', tone: 'artStar', label: 'Limited Poster', contentType: 'MOVIE' },
  { kind: 'acrylic', tone: 'artLibrary', label: 'Limited Stand', contentType: 'WEB_NOVEL' },
  { kind: 'ost', tone: 'artOrchestra', label: 'Original Soundtrack', contentType: 'MUSIC' },
  { kind: 'sticker', tone: 'artDawn', label: 'Sticker Pack', contentType: 'DRAMA' }
];

function resolveArtworkProfile(product = {}) {
  const productId = Number(product.productId || 0);

  if (productArtworkProfiles[productId]) {
    return productArtworkProfiles[productId];
  }

  const searchableText = [
    product.name,
    product.productName,
    product.contentTitle,
    product.category
  ].filter(Boolean).join(' ');

  if (/포스터|poster/i.test(searchableText)) {
    return fallbackProfiles[1];
  }

  if (/아크릴|스탠드|stand/i.test(searchableText)) {
    return fallbackProfiles[2];
  }

  if (/ost|앨범|음악|sound/i.test(searchableText)) {
    return fallbackProfiles[3];
  }

  if (/스티커|sticker/i.test(searchableText)) {
    return fallbackProfiles[4];
  }

  return fallbackProfiles[productId % fallbackProfiles.length];
}

function ArtbookArtwork() {
  return (
    <>
      <circle cx="178" cy="72" r="36" fill="#fff9ea" opacity="0.9" />
      <path d="M0 252 C48 226 88 230 126 256 C156 276 194 272 240 238 V360 H0 Z" fill="#151a23" opacity="0.42" />
      <rect x="58" y="104" width="112" height="168" rx="8" fill="#f9f9f9" />
      <rect x="70" y="118" width="88" height="140" rx="5" fill="#233047" />
      <path d="M82 204 C104 174 130 172 148 204" fill="none" stroke="#d8b66a" strokeWidth="8" strokeLinecap="round" />
      <rect x="88" y="134" width="52" height="6" rx="3" fill="#fff9ea" opacity="0.85" />
      <rect x="88" y="150" width="70" height="4" rx="2" fill="#9d9d9d" opacity="0.48" />
      <rect x="48" y="118" width="22" height="170" rx="5" fill="#d0d0d0" />
    </>
  );
}

function PosterArtwork() {
  return (
    <>
      <rect x="0" y="0" width="240" height="360" fill="#202020" />
      <g opacity="0.9">
        {Array.from({ length: 16 }).map((_, index) => (
          <circle
            key={index}
            cx={28 + ((index * 41) % 188)}
            cy={32 + ((index * 53) % 250)}
            r={index % 3 === 0 ? 2.8 : 1.8}
            fill="#fff9ea"
          />
        ))}
      </g>
      <rect x="50" y="86" width="104" height="182" rx="8" fill="#f9f9f9" />
      <rect x="70" y="68" width="104" height="182" rx="8" fill="#dfe9f8" />
      <path d="M88 214 L122 142 L158 214 Z" fill="#222222" />
      <path d="M108 112 L116 130 L136 132 L120 144 L124 164 L108 154 L90 164 L96 144 L80 132 L100 130 Z" fill="#1f8ce6" />
      <rect x="88" y="230" width="66" height="7" rx="3.5" fill="#5d5d5d" opacity="0.58" />
    </>
  );
}

function AcrylicArtwork() {
  return (
    <>
      <rect x="0" y="0" width="240" height="360" fill="#eaf5ff" />
      <rect x="38" y="236" width="164" height="20" rx="10" fill="#1f8ce6" opacity="0.22" />
      <rect x="70" y="86" width="100" height="168" rx="50" fill="#ffffff" opacity="0.7" />
      <path d="M120 86 C154 92 174 122 174 158 C174 218 146 246 120 246 C94 246 66 218 66 158 C66 122 86 92 120 86 Z" fill="#ffffff" stroke="#1f8ce6" strokeWidth="6" />
      <circle cx="98" cy="152" r="8" fill="#222222" />
      <circle cx="142" cy="152" r="8" fill="#222222" />
      <path d="M96 188 C112 202 132 202 148 188" fill="none" stroke="#03aa5a" strokeWidth="7" strokeLinecap="round" />
      <path d="M76 116 C100 80 146 82 166 118 C138 104 106 104 76 116 Z" fill="#27496d" />
      <rect x="92" y="256" width="56" height="28" rx="14" fill="#ffffff" opacity="0.8" />
    </>
  );
}

function OstArtwork() {
  return (
    <>
      <rect x="0" y="0" width="240" height="360" fill="#f4f1eb" />
      <rect x="52" y="82" width="136" height="176" rx="12" fill="#222222" />
      <rect x="66" y="98" width="108" height="144" rx="8" fill="#3d3d3d" />
      <circle cx="120" cy="170" r="46" fill="#f9f9f9" />
      <circle cx="120" cy="170" r="15" fill="#d0d0d0" />
      <path d="M90 132 C118 116 144 118 162 142" fill="none" stroke="#1f8ce6" strokeWidth="5" strokeLinecap="round" />
      <g stroke="#fff9ea" strokeWidth="4" strokeLinecap="round" opacity="0.82">
        <path d="M82 270 H178" />
        <path d="M88 288 H156" />
        <path d="M100 306 H170" />
      </g>
    </>
  );
}

function StickerArtwork() {
  return (
    <>
      <rect x="0" y="0" width="240" height="360" fill="#fff9ea" />
      <rect x="48" y="76" width="124" height="190" rx="16" fill="#ffffff" />
      <rect x="68" y="98" width="84" height="26" rx="13" fill="#1f8ce6" opacity="0.18" />
      <circle cx="88" cy="166" r="30" fill="#f4361e" opacity="0.82" />
      <path d="M74 166 H106 M88 150 V182" stroke="#ffffff" strokeWidth="7" strokeLinecap="round" />
      <rect x="118" y="148" width="62" height="62" rx="18" fill="#03aa5a" opacity="0.86" />
      <path d="M130 180 L144 194 L168 160" fill="none" stroke="#ffffff" strokeWidth="7" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M70 236 C92 214 126 214 148 236 C126 258 92 258 70 236 Z" fill="#222222" opacity="0.82" />
      <rect x="36" y="266" width="168" height="20" rx="10" fill="#d0d0d0" opacity="0.45" />
    </>
  );
}

function ArtworkSvg({ kind }) {
  const artworkByKind = {
    acrylic: <AcrylicArtwork />,
    artbook: <ArtbookArtwork />,
    ost: <OstArtwork />,
    poster: <PosterArtwork />,
    sticker: <StickerArtwork />
  };

  return (
    <svg className="artworkImage" viewBox="0 0 240 360" aria-hidden="true" focusable="false">
      {artworkByKind[kind] || artworkByKind.artbook}
    </svg>
  );
}

export default function ProductArtwork({ className = '', compact = false, product = {}, showBadges = true }) {
  const profile = resolveArtworkProfile(product);
  const title = product.contentTitle || product.productName || product.name || profile.label;
  const contentType = product.contentType || profile.contentType;
  const hasDiscount = Number(product.instantDiscountAmount || 0) > 0;

  return (
    <div
      aria-hidden="true"
      className={`productArtwork ${profile.tone} ${className} ${compact ? 'compactArtwork' : ''}`.trim()}
    >
      <ArtworkSvg kind={profile.kind} />
      {showBadges && !compact && hasDiscount && (
        <span className="saleFlag">
          {formatWon(product.instantDiscountAmount)} 할인
        </span>
      )}
      {showBadges && !compact && <span className="coverType">{formatContentType(contentType)}</span>}
      {showBadges && !compact && <strong className="artworkTitle">{title}</strong>}
    </div>
  );
}
