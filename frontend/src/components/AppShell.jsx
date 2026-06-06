import {
  BarChart3,
  Package,
  ReceiptText,
  ShoppingCart,
  Ticket,
  UserRound
} from 'lucide-react';
import { NavLink } from 'react-router-dom';
import dropGoodsLogo from '../assets/brand/drop-goods-logo.png';
import StatusPanel from './StatusPanel.jsx';

const navigationItems = [
  { to: '/products', label: '상품', icon: Package },
  { to: '/cart', label: '장바구니', icon: ShoppingCart },
  { to: '/coupons', label: '쿠폰', icon: Ticket },
  { to: '/orders', label: '주문', icon: ReceiptText },
  { to: '/report', label: '리포트', icon: BarChart3 }
];

export default function AppShell({ apiEvents, children, customerId, onCustomerIdChange }) {
  return (
    <div className="app">
      <header className="topbar">
        <NavLink className="brand" to="/products" aria-label="Drop Goods 상품">
          <img className="brandLogo" src={dropGoodsLogo} alt="Drop Goods Limited IP Merch" />
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
      </header>

      <div className="appFrame">
        <main className="mainPane">{children}</main>
        <StatusPanel apiEvents={apiEvents} customerId={customerId} />
      </div>
    </div>
  );
}
