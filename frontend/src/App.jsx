import { useCallback, useEffect, useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import AppShell from './components/AppShell.jsx';
import CartPage from './pages/CartPage.jsx';
import PlaceholderPage from './pages/PlaceholderPage.jsx';
import ProductDetailPage from './pages/ProductDetailPage.jsx';
import ProductListPage from './pages/ProductListPage.jsx';

const CUSTOMER_ID_STORAGE_KEY = 'ecommerce-demo-customer-id';

export default function App() {
  const [customerId, setCustomerId] = useState(() => {
    return window.localStorage.getItem(CUSTOMER_ID_STORAGE_KEY) || '1';
  });
  const [apiEvents, setApiEvents] = useState([]);

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

  return (
    <AppShell
      apiEvents={apiEvents}
      customerId={customerId}
      onCustomerIdChange={setCustomerId}
    >
      <Routes>
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
        <Route path="/coupons" element={<PlaceholderPage title="쿠폰" />} />
        <Route path="/checkout" element={<PlaceholderPage title="체크아웃" />} />
        <Route path="/orders" element={<PlaceholderPage title="주문" />} />
        <Route path="/orders/:orderId" element={<PlaceholderPage title="주문 상세" />} />
        <Route path="/report" element={<PlaceholderPage title="성능 리포트" />} />
        <Route path="*" element={<Navigate to="/products" replace />} />
      </Routes>
    </AppShell>
  );
}
