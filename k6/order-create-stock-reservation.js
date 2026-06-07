import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PRODUCT_ID = Number(__ENV.PRODUCT_ID || 10001);
const ORDERS = Number(__ENV.ORDERS || 500);
const VUS = Number(__ENV.VUS || 100);
const STOCK = Number(__ENV.STOCK || 1000);
const CUSTOMER_BASE = Number(__ENV.CUSTOMER_BASE || Date.now());
const MAX_DURATION = __ENV.MAX_DURATION || '2m';

export const options = {
  scenarios: {
    order_create: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: ORDERS,
      maxDuration: MAX_DURATION,
    },
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

const orderCreateDuration = new Trend('order_create_duration', true);
const orderCreateStartedAt = new Trend('order_create_started_at');
const orderCreateFinishedAt = new Trend('order_create_finished_at');
const orderCreateSuccess = new Counter('order_create_success');
const orderCreateFailure = new Counter('order_create_failure');
const unexpectedResponse = new Counter('order_create_unexpected_response');

function jsonHeaders(customerId = null) {
  const headers = {
    'Content-Type': 'application/json',
  };
  if (customerId !== null) {
    headers['X-Customer-Id'] = String(customerId);
  }

  return { headers };
}

function parseJson(response) {
  if (!response.body) {
    return null;
  }

  return JSON.parse(response.body);
}

function prepareCartItem(customerId) {
  const addResponse = http.post(
    `${BASE_URL}/api/v1/cart/items`,
    JSON.stringify({ productId: PRODUCT_ID, quantity: 1 }),
    {
      ...jsonHeaders(customerId),
      tags: { name: 'SetupAddCartItem' },
    }
  );
  if (addResponse.status !== 201) {
    throw new Error(`Failed to add cart item: customerId=${customerId}, status=${addResponse.status}`);
  }

  const cartResponse = http.get(`${BASE_URL}/api/v1/cart`, {
    ...jsonHeaders(customerId),
    tags: { name: 'SetupGetCart' },
  });
  if (cartResponse.status !== 200) {
    throw new Error(`Failed to load cart: customerId=${customerId}, status=${cartResponse.status}`);
  }

  const cart = parseJson(cartResponse);
  const item = cart.items.find((cartItem) => cartItem.productId === PRODUCT_ID);
  if (!item || !item.cartItemId) {
    throw new Error(`Cart item for order was not found: customerId=${customerId}`);
  }

  return {
    customerId,
    cartItemId: item.cartItemId,
  };
}

export function setup() {
  const stockResponse = http.patch(
    `${BASE_URL}/api/v1/products/${PRODUCT_ID}/stock`,
    JSON.stringify({ stockQuantity: STOCK }),
    {
      ...jsonHeaders(),
      tags: { name: 'SetupUpdateStock' },
    }
  );
  if (stockResponse.status !== 200) {
    throw new Error(`Failed to reset product stock: productId=${PRODUCT_ID}, status=${stockResponse.status}`);
  }

  const cartItems = [];
  for (let index = 0; index < ORDERS; index += 1) {
    const customerId = CUSTOMER_BASE + index;
    cartItems.push(prepareCartItem(customerId));
  }

  return {
    productId: PRODUCT_ID,
    stock: STOCK,
    orders: ORDERS,
    vus: VUS,
    customerBase: CUSTOMER_BASE,
    cartItems,
  };
}

export default function createOrder(data) {
  const index = exec.scenario.iterationInTest;
  const target = data.cartItems[index];
  const startedAt = Date.now();
  const response = http.post(
    `${BASE_URL}/api/v1/orders`,
    JSON.stringify({
      cartItemIds: [target.cartItemId],
      orderCouponId: null,
      productCoupons: [],
    }),
    {
      ...jsonHeaders(target.customerId),
      tags: { name: 'CreateOrder' },
    }
  );
  const finishedAt = Date.now();

  orderCreateDuration.add(response.timings.duration);
  orderCreateStartedAt.add(startedAt);
  orderCreateFinishedAt.add(finishedAt);

  if (response.status === 201) {
    orderCreateSuccess.add(1);
  } else if (response.status === 400) {
    orderCreateFailure.add(1);
  } else {
    unexpectedResponse.add(1);
  }

  check(response, {
    'order-create status is 201 or 400': (res) => res.status === 201 || res.status === 400,
  });
}
