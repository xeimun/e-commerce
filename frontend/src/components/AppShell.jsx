import {
  BarChart3,
  Package,
  ReceiptText,
  RotateCcw,
  ShoppingCart,
  Ticket,
  UserRound
} from 'lucide-react';
import { NavLink } from 'react-router-dom';
import StatusPanel from './StatusPanel.jsx';

const navigationItems = [
  { to: '/products', label: '상품', icon: Package },
  { to: '/cart', label: '장바구니', icon: ShoppingCart },
  { to: '/coupons', label: '쿠폰', icon: Ticket },
  { to: '/orders', label: '주문', icon: ReceiptText },
  { to: '/report', label: '리포트', icon: BarChart3 }
];

export default function AppShell({
  apiEvents,
  children,
  customerId,
  demoResetMessage,
  demoResetStatus,
  onCustomerIdChange,
  onDemoReset
}) {
  const isResetting = demoResetStatus === 'pending';

  return (
    <div className="app">
      <header className="topbar">
        <NavLink className="brand" to="/products" aria-label="Drop Goods 상품">
          <span className="brandMark">DG</span>
          <span>
            <strong>Drop Goods</strong>
            <small>Limited IP Merch</small>
          </span>
        </NavLink>

        <nav className="navTabs" aria-label="주요 화면">
          {navigationItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink key={item.to} className="navTab" to={item.to}>
                <Icon aria-hidden="true" size={18} />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="topActions">
          <button
            aria-label={isResetting ? '데모 초기화 진행 중' : '데모 초기화'}
            className="outlineButton demoResetButton"
            disabled={isResetting}
            type="button"
            onClick={onDemoReset}
          >
            <RotateCcw aria-hidden="true" className={isResetting ? 'spinIcon' : ''} size={18} />
            <span>{isResetting ? '초기화 중' : '데모 초기화'}</span>
          </button>

          <label className="customerControl">
            <UserRound aria-hidden="true" size={18} />
            <span>고객</span>
            <input
              inputMode="numeric"
              min="1"
              value={customerId}
              onChange={(event) => onCustomerIdChange(event.target.value.replace(/\D/g, '') || '1')}
            />
          </label>
        </div>
      </header>

      {demoResetMessage && (
        <div className={demoResetStatus === 'error' ? 'globalNotice error' : 'globalNotice success'}>
          <span>{demoResetMessage}</span>
        </div>
      )}

      <div className="appFrame">
        <main className="mainPane">{children}</main>
        <StatusPanel apiEvents={apiEvents} customerId={customerId} />
      </div>
    </div>
  );
}
