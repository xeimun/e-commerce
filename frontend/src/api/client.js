const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export class ApiRequestError extends Error {
  constructor(message, { status, payload }) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.payload = payload;
  }
}

async function parsePayload(response) {
  if (response.status === 204) {
    return null;
  }

  const text = await response.text();
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

async function request(path, { method = 'GET', body, customerId, onApiEvent } = {}) {
  const startedAt = performance.now();
  const headers = {
    Accept: 'application/json'
  };

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }

  if (customerId) {
    headers['X-Customer-Id'] = String(customerId);
  }

  try {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body)
    });
    const payload = await parsePayload(response);
    const durationMs = Math.round(performance.now() - startedAt);

    onApiEvent?.({
      method,
      path,
      status: response.status,
      ok: response.ok,
      durationMs
    });

    if (!response.ok) {
      const message = payload?.message || `API 요청에 실패했습니다. (${response.status})`;
      throw new ApiRequestError(message, {
        status: response.status,
        payload
      });
    }

    return payload;
  } catch (error) {
    if (error instanceof ApiRequestError) {
      throw error;
    }

    const durationMs = Math.round(performance.now() - startedAt);
    onApiEvent?.({
      method,
      path,
      status: 'NETWORK',
      ok: false,
      durationMs
    });
    throw error;
  }
}

export const api = {
  getProducts(options) {
    return request('/api/v1/products', options);
  },
  getProduct(productId, options) {
    return request(`/api/v1/products/${productId}`, options);
  },
  getCart(options) {
    return request('/api/v1/cart', options);
  },
  addCartItem(productId, quantity, options) {
    return request('/api/v1/cart/items', {
      ...options,
      method: 'POST',
      body: {
        productId,
        quantity
      }
    });
  },
  updateCartItemQuantity(cartItemId, quantity, options) {
    return request(`/api/v1/cart/items/${cartItemId}`, {
      ...options,
      method: 'PATCH',
      body: {
        quantity
      }
    });
  },
  deleteCartItem(cartItemId, options) {
    return request(`/api/v1/cart/items/${cartItemId}`, {
      ...options,
      method: 'DELETE'
    });
  },
  getIssuableCoupons(options) {
    return request('/api/v1/coupons/issuable', options);
  },
  issueCoupon(couponId, options) {
    return request(`/api/v1/coupons/${couponId}/issue`, {
      ...options,
      method: 'POST'
    });
  },
  getMyCoupons(options) {
    return request('/api/v1/customers/me/coupons', options);
  },
  createOrder(order, options) {
    return request('/api/v1/orders', {
      ...options,
      method: 'POST',
      body: order
    });
  }
};
