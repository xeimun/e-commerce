import { useCallback, useEffect, useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { api } from './api/client.js';
import AppShell from './components/AppShell.jsx';
import CartPage from './pages/CartPage.jsx';
import CheckoutPage from './pages/CheckoutPage.jsx';
import CouponPage from './pages/CouponPage.jsx';
import OrderDetailPage from './pages/OrderDetailPage.jsx';
import OrderListPage from './pages/OrderListPage.jsx';
import PlaceholderPage from './pages/PlaceholderPage.jsx';
import ProductDetailPage from './pages/ProductDetailPage.jsx';
import ProductListPage from './pages/ProductListPage.jsx';

const CUSTOMER_ID_STORAGE_KEY = 'ecommerce-demo-customer-id';

export default function App() {
  const [customerId, setCustomerId] = useState(() => {
    return window.localStorage.getItem(CUSTOMER_ID_STORAGE_KEY) || '1';
  });
  const [apiEvents, setApiEvents] = useState([]);
  const [demoResetStatus, setDemoResetStatus] = useState('idle');
  const [demoResetMessage, setDemoResetMessage] = useState('');
  const [routeRefreshKey, setRouteRefreshKey] = useState(0);

  useEffect(() => {
    window.localStorage.setItem(CUSTOMER_ID_STORAGE_KEY, customerId || '1');
  }, [customerId]);

  const recordApiEvent = useCallback((event) => {
    setApiEvents((current) => [
      {
        id: crypto.randomUUID(),
        at: new Date().toLocaleTimeString('ko-KR', {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        }),
        ...event
      },
      ...current
    ].slice(0, 8));
  }, []);

  const resetDemoData = useCallback(async () => {
    setDemoResetStatus('pending');
    setDemoResetMessage('');

    try {
      await api.resetDemoData({ onApiEvent: recordApiEvent });
      window.sessionStorage.removeItem('checkout-cart-item-ids');
      setCustomerId('1');
      setRouteRefreshKey((current) => current + 1);
      setDemoResetStatus('success');
      setDemoResetMessage('데모 상태를 초기화했어요.');
    } catch (error) {
      setDemoResetStatus('error');
      setDemoResetMessage(error.message || '데모 상태를 초기화하지 못했습니다.');
    }
  }, [recordApiEvent]);

  return (
    <AppShell
      apiEvents={apiEvents}
      customerId={customerId}
      demoResetMessage={demoResetMessage}
      demoResetStatus={demoResetStatus}
      onDemoReset={resetDemoData}
      onCustomerIdChange={setCustomerId}
    >
      <Routes key={routeRefreshKey}>
        <Route path="/" element={<Navigate to="/products" replace />} />
        <Route
          path="/products"
          element={<ProductListPage onApiEvent={recordApiEvent} />}
        />
        <Route
          path="/products/:productId"
          element={<ProductDetailPage customerId={customerId} onApiEvent={recordApiEvent} />}
        />
        <Route
          path="/cart"
          element={<CartPage customerId={customerId} onApiEvent={recordApiEvent} />}
        />
        <Route
          path="/coupons"
          element={<CouponPage customerId={customerId} onApiEvent={recordApiEvent} />}
        />
        <Route
          path="/checkout"
          element={<CheckoutPage customerId={customerId} onApiEvent={recordApiEvent} />}
        />
        <Route
          path="/orders"
          element={<OrderListPage customerId={customerId} onApiEvent={recordApiEvent} />}
        />
        <Route
          path="/orders/:orderId"
          element={<OrderDetailPage customerId={customerId} onApiEvent={recordApiEvent} />}
        />
        <Route path="/report" element={<PlaceholderPage title="성능 리포트" />} />
        <Route path="*" element={<Navigate to="/products" replace />} />
      </Routes>
    </AppShell>
  );
}
