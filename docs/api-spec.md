# API 명세

이 문서는 첫 구현에 필요한 API 계약 초안을 정리한다.
요청/응답 형식은 구현 과정에서 변경될 수 있으며, 변경 시 이 문서를 함께 갱신한다.

## 1. 기본 규칙

### API 버전

- 모든 API 경로는 `/api/v1`로 시작한다.
- API 버전은 요청/응답 형식이 크게 변경될 때 기존 클라이언트를 보호하기 위해 사용한다.

### 고객 식별

- 첫 버전에서는 인증/인가를 구현하지 않는다.
- 고객 식별은 `X-Customer-Id` 헤더로 처리한다.
- 이후 인증/인가 도입 시 `X-Customer-Id` 대신 인증된 사용자 정보를 사용한다.

```http
X-Customer-Id: 1
```

### 성공 응답

- 성공 응답은 공통 래퍼 없이 각 API의 응답 객체를 바로 반환한다.

### 공통 에러 응답

```json
{
  "code": "OUT_OF_STOCK",
  "message": "상품 재고가 부족합니다.",
  "details": [
    {
      "targetType": "PRODUCT",
      "targetId": 1,
      "reason": "OUT_OF_STOCK",
      "currentStock": 0
    }
  ]
}
```

- `code`: 대표 에러 코드
- `message`: 사용자 또는 클라이언트가 이해할 수 있는 메시지
- `details`: 여러 상품/쿠폰 검증 실패가 있을 때의 상세 정보

## 2. API 목록

### 상품

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/v1/products` | 상품 목록 조회 |
| GET | `/api/v1/products/{productId}` | 상품 상세 조회 |
| POST | `/api/v1/products` | 상품 생성 |
| PUT | `/api/v1/products/{productId}` | 상품 기본 정보 수정 |
| PATCH | `/api/v1/products/{productId}/status` | 상품 판매 상태 변경 |
| PATCH | `/api/v1/products/{productId}/stock` | 상품 재고 수량 변경 |
| PUT | `/api/v1/products/{productId}/instant-discount` | 상품 즉시 할인 설정 |
| DELETE | `/api/v1/products/{productId}/instant-discount` | 상품 즉시 할인 비활성화 |

### 장바구니

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/v1/cart` | 장바구니 조회 |
| POST | `/api/v1/cart/items` | 장바구니 상품 추가 |
| PATCH | `/api/v1/cart/items/{cartItemId}` | 장바구니 상품 수량 변경 |
| DELETE | `/api/v1/cart/items/{cartItemId}` | 장바구니 상품 삭제 |

### 주문

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/v1/orders` | 장바구니 선택 상품 주문 생성 |
| GET | `/api/v1/orders` | 주문 목록 조회 |
| GET | `/api/v1/orders/{orderId}` | 주문 상세 조회 |

### 결제

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/v1/orders/{orderId}/payment/success` | 가짜 결제 성공 처리 |
| POST | `/api/v1/orders/{orderId}/payment/cancel` | 가짜 결제 취소 처리 |

### 쿠폰

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/v1/coupons/issuable` | 발급 가능 쿠폰 조회 |
| POST | `/api/v1/coupons/{couponId}/issue` | 쿠폰 발급 |
| GET | `/api/v1/customers/me/coupons` | 내 보유 쿠폰 조회 |

## 3. 상품 API

### 상품 목록 조회

```http
GET /api/v1/products
```

응답 예시:

```json
[
  {
    "productId": 1,
    "name": "상품 A",
    "price": 10000,
    "status": "ON_SALE",
    "contentTitle": "달빛 상점",
    "contentType": "WEBTOON",
    "category": "ARTBOOK",
    "description": "달빛 상점 한정판 아트북",
    "stockQuantity": 10,
    "instantDiscountAmount": 1000
  }
]
```

### 상품 상세 조회

```http
GET /api/v1/products/1
```

응답 예시:

```json
{
  "productId": 1,
  "name": "상품 A",
  "price": 10000,
  "status": "ON_SALE",
  "contentTitle": "달빛 상점",
  "contentType": "WEBTOON",
  "category": "ARTBOOK",
  "description": "달빛 상점 한정판 아트북",
  "stockQuantity": 10,
  "instantDiscountAmount": 1000
}
```

### 상품 생성

초기 버전에서는 별도 관리자 인증을 구현하지 않는다.
운영자용 API라는 전제를 두고 상품과 초기 재고를 함께 생성한다.

```http
POST /api/v1/products
```

요청 예시:

```json
{
  "name": "달빛 상점 한정판 아트북",
  "price": 35000,
  "status": "ON_SALE",
  "contentTitle": "달빛 상점",
  "contentType": "WEBTOON",
  "category": "ARTBOOK",
  "description": "웹툰 달빛 상점의 시즌 1 한정판 아트북",
  "stockQuantity": 100
}
```

응답 예시:

```json
{
  "productId": 1,
  "name": "달빛 상점 한정판 아트북",
  "price": 35000,
  "status": "ON_SALE",
  "contentTitle": "달빛 상점",
  "contentType": "WEBTOON",
  "category": "ARTBOOK",
  "description": "웹툰 달빛 상점의 시즌 1 한정판 아트북",
  "stockQuantity": 100,
  "instantDiscountAmount": 0
}
```

### 상품 기본 정보 수정

```http
PUT /api/v1/products/1
```

요청 예시:

```json
{
  "name": "달빛 상점 한정판 아트북",
  "price": 36000,
  "contentTitle": "달빛 상점",
  "contentType": "WEBTOON",
  "category": "ARTBOOK",
  "description": "웹툰 달빛 상점의 시즌 1 개정판 한정 아트북"
}
```

응답 형식은 상품 상세 조회와 동일하다.

### 상품 판매 상태 변경

```http
PATCH /api/v1/products/1/status
```

요청 예시:

```json
{
  "status": "STOPPED"
}
```

응답 형식은 상품 상세 조회와 동일하다.

### 상품 재고 수량 변경

```http
PATCH /api/v1/products/1/stock
```

요청 예시:

```json
{
  "stockQuantity": 50
}
```

응답 형식은 상품 상세 조회와 동일하다.

### 상품 즉시 할인 설정

운영자용 API라는 전제로 상품에 자동 적용되는 즉시 할인 정책을 생성하거나 갱신한다.
즉시 할인은 활성 상태이고 현재 시간이 적용 기간에 포함될 때 상품 조회 응답과 주문 금액에 반영된다.

```http
PUT /api/v1/products/1/instant-discount
```

요청 예시:

```json
{
  "name": "드롭 오픈 할인",
  "discountAmount": 3000,
  "startsAt": "2026-06-04T10:00:00",
  "endsAt": "2026-06-30T23:59:59"
}
```

응답 형식은 상품 상세 조회와 동일하다.

### 상품 즉시 할인 비활성화

```http
DELETE /api/v1/products/1/instant-discount
```

응답 형식은 상품 상세 조회와 동일하다.

## 4. 장바구니 API

### 장바구니 조회

```http
GET /api/v1/cart
X-Customer-Id: 1
```

응답 예시:

```json
{
  "cartId": 1,
  "items": [
    {
      "cartItemId": 1,
      "productId": 1,
      "productName": "상품 A",
      "price": 10000,
      "quantity": 2,
      "stockQuantity": 10,
      "productStatus": "ON_SALE",
      "selectable": true,
      "notSelectableReason": null
    }
  ]
}
```

### 장바구니 상품 추가

```http
POST /api/v1/cart/items
X-Customer-Id: 1
```

요청 예시:

```json
{
  "productId": 1,
  "quantity": 2
}
```

응답 예시:

```json
{
  "cartItemId": 1,
  "productId": 1,
  "quantity": 2
}
```

- 같은 상품이 이미 장바구니에 있으면 기존 수량에 요청 수량을 더한다.
- 추가 후 최종 수량이 현재 재고보다 크면 `OUT_OF_STOCK`으로 실패한다.

### 장바구니 상품 수량 변경

```http
PATCH /api/v1/cart/items/1
X-Customer-Id: 1
```

요청 예시:

```json
{
  "quantity": 3
}
```

응답 예시:

```json
{
  "cartItemId": 1,
  "productId": 1,
  "quantity": 3
}
```

- 요청 고객의 장바구니에 속하지 않은 상품 ID이면 `CART_ITEM_NOT_FOUND`로 실패한다.
- 변경 후 수량이 현재 재고보다 크면 `OUT_OF_STOCK`으로 실패한다.

### 장바구니 상품 삭제

```http
DELETE /api/v1/cart/items/1
X-Customer-Id: 1
```

응답:

```http
204 No Content
```

- 요청 고객의 장바구니에 속하지 않은 상품 ID이면 `CART_ITEM_NOT_FOUND`로 실패한다.

## 5. 주문 API

### 주문 생성

장바구니에서 선택한 상품으로 결제 대기 주문을 생성한다.
주문 생성 성공 시 재고가 즉시 차감되고, 고객 보유 쿠폰은 예약된다.

```http
POST /api/v1/orders
X-Customer-Id: 1
```

요청 예시:

```json
{
  "cartItemIds": [1, 2],
  "orderCouponId": 10,
  "productCoupons": [
    {
      "cartItemId": 1,
      "couponId": 20
    }
  ]
}
```

- `cartItemIds`: 주문할 장바구니 상품 ID 목록
- `orderCouponId`: 주문 전체에 적용할 고객 보유 쿠폰 ID. 선택 값이다.
- `productCoupons`: 특정 상품에 적용할 고객 보유 쿠폰 목록. 선택 값이다.
- 고객 보유 쿠폰이 유효하면 주문 생성 시 `RESERVED` 상태로 예약되고 할인 금액에 반영된다.
- 상품 즉시 할인은 현재 활성 정책이 있으면 상품 1개당 할인 금액을 주문 수량만큼 합산해 반영한다.
- 특정 상품 쿠폰은 상품 즉시 할인 적용 후 남은 상품 1개 금액까지만 반영한다.

응답 예시:

```json
{
  "orderId": 1,
  "status": "PAYMENT_PENDING",
  "expiresAt": "2026-06-02T10:30:00",
  "totalProductAmount": 50000,
  "totalInstantDiscountAmount": 3000,
  "totalCouponDiscountAmount": 4000,
  "finalPaymentAmount": 43000
}
```

주문 생성 실패 응답 예시:

```json
{
  "code": "ORDER_VALIDATION_FAILED",
  "message": "주문할 수 없는 상품 또는 쿠폰이 포함되어 있습니다.",
  "details": [
    {
      "targetType": "PRODUCT",
      "targetId": 1,
      "reason": "OUT_OF_STOCK",
      "currentStock": 0
    },
    {
      "targetType": "COUPON",
      "targetId": 20,
      "reason": "COUPON_EXPIRED"
    }
  ]
}
```

### 주문 목록 조회

```http
GET /api/v1/orders
X-Customer-Id: 1
```

응답 예시:

```json
[
  {
    "orderId": 1,
    "status": "PAYMENT_PENDING",
    "finalPaymentAmount": 43000,
    "expiresAt": "2026-06-02T10:30:00"
  }
]
```

### 주문 상세 조회

```http
GET /api/v1/orders/1
X-Customer-Id: 1
```

응답 예시:

```json
{
  "orderId": 1,
  "status": "PAYMENT_PENDING",
  "expiresAt": "2026-06-02T10:30:00",
  "cancelReason": null,
  "totalProductAmount": 50000,
  "totalInstantDiscountAmount": 3000,
  "totalCouponDiscountAmount": 4000,
  "finalPaymentAmount": 43000,
  "items": [
    {
      "orderItemId": 1,
      "productId": 1,
      "productName": "상품 A",
      "productPrice": 10000,
      "quantity": 2,
      "originalAmount": 20000,
      "instantDiscountAmount": 1000,
      "productCouponDiscountAmount": 1000,
      "finalAmount": 18000
    }
  ]
}
```

## 6. 결제 API

### 가짜 결제 성공 처리

```http
POST /api/v1/orders/1/payment/success
X-Customer-Id: 1
```

응답 예시:

```json
{
  "orderId": 1,
  "status": "COMPLETED",
  "paymentStatus": "PAID",
  "finalPaymentAmount": 43000,
  "paidAt": "2026-06-02T10:25:00"
}
```

- 결제 성공 시 주문 상태는 `COMPLETED`가 된다.
- 결제 성공 시 예약된 고객 보유 쿠폰은 `USED` 상태가 된다.
- 결제 성공 시 주문 생성에 사용된 장바구니 상품은 장바구니에서 제거된다.
- 주문 생성 후 같은 상품을 다시 담은 새 장바구니 상품은 제거하지 않는다.
- 주문 생성 후 같은 장바구니 상품의 수량이 증가했다면 주문 수량만큼만 차감하고 남은 수량은 유지한다.
- 결제 성공 요청은 `PAYMENT_PENDING` 주문에만 허용된다.

만료된 주문에 결제 성공을 요청한 경우:

```json
{
  "code": "ORDER_PAYMENT_EXPIRED",
  "message": "결제 대기 시간이 만료되었습니다.",
  "details": [
    {
      "targetType": "ORDER",
      "targetId": 1,
      "reason": "ORDER_PAYMENT_EXPIRED"
    }
  ]
}
```

### 가짜 결제 취소 처리

```http
POST /api/v1/orders/1/payment/cancel
X-Customer-Id: 1
```

응답 예시:

```json
{
  "orderId": 1,
  "status": "CANCELED",
  "paymentStatus": "CANCELED",
  "cancelReason": "PAYMENT_CANCELED"
}
```

- 결제 취소 시 주문 상태는 `CANCELED`가 된다.
- 결제 취소 시 주문 생성으로 예약 차감했던 재고를 복구한다.
- 결제 취소 시 예약된 고객 보유 쿠폰은 `AVAILABLE` 상태로 되돌린다.
- 결제 취소 요청은 `PAYMENT_PENDING` 주문에만 허용된다.

## 7. 쿠폰 API

### 발급 가능 쿠폰 조회

```http
GET /api/v1/coupons/issuable
X-Customer-Id: 1
```

응답 예시:

```json
[
  {
    "couponId": 1,
    "name": "전체 상품 3000원 할인",
    "type": "ORDER",
    "discountAmount": 3000,
    "targetProductId": null,
    "expiresAt": "2026-06-30T23:59:59",
    "issuable": true
  }
]
```

### 쿠폰 발급

```http
POST /api/v1/coupons/1/issue
X-Customer-Id: 1
```

응답 예시:

```json
{
  "issuedCouponId": 1,
  "couponId": 1,
  "status": "AVAILABLE",
  "issuedAt": "2026-06-02T10:00:00"
}
```

### 내 보유 쿠폰 조회

```http
GET /api/v1/customers/me/coupons
X-Customer-Id: 1
```

응답 예시:

```json
[
  {
    "issuedCouponId": 1,
    "couponId": 1,
    "name": "전체 상품 3000원 할인",
    "type": "ORDER",
    "discountAmount": 3000,
    "targetProductId": null,
    "status": "AVAILABLE",
    "expiresAt": "2026-06-30T23:59:59"
  }
]
```

## 8. 주요 에러 코드

| Code | Description |
| --- | --- |
| `PRODUCT_NOT_FOUND` | 상품 없음 |
| `PRODUCT_NOT_ON_SALE` | 판매 중이 아닌 상품 |
| `OUT_OF_STOCK` | 재고 부족 |
| `CART_ITEM_NOT_FOUND` | 장바구니 상품 없음 |
| `CART_ITEM_NOT_SELECTABLE` | 주문 선택 불가 장바구니 상품 |
| `COUPON_NOT_OWNED` | 고객이 보유하지 않은 쿠폰 |
| `COUPON_NOT_AVAILABLE` | 사용할 수 없는 쿠폰 |
| `COUPON_ALREADY_USED` | 이미 사용된 쿠폰 |
| `COUPON_RESERVED` | 다른 결제 대기 주문에 예약된 쿠폰 |
| `COUPON_EXPIRED` | 만료된 쿠폰 |
| `COUPON_TARGET_MISMATCH` | 쿠폰 적용 대상 불일치 |
| `ORDER_NOT_FOUND` | 주문 없음 |
| `ORDER_NOT_PAYMENT_PENDING` | 결제 대기 상태가 아닌 주문 |
| `ORDER_PAYMENT_EXPIRED` | 결제 대기 시간 만료 |
