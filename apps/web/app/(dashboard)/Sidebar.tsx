"use client";

import {useSession} from "next-auth/react";
import Link from "next/link";
import {useLocale} from "@/components/LocaleProvider";
import { usePathname } from "next/navigation";
import {
  Hotel, LayoutDashboard, BedDouble, CalendarDays, LogIn, LogOut,
  FileText, ShoppingCart, ChefHat, Package, ShoppingBag, Wallet,
  CreditCard, Receipt, BarChart3, Home, Wrench, Users, Settings,
  ClipboardList, AlertTriangle, Bell, Utensils,
} from "lucide-react";
import { cn, MANAGER_ROLES, FINANCE_ROLES, RECEPTION_ROLES, POS_ROLES, STOCK_ROLES } from "@/lib/utils";

type NavItem = { label: string; href: string; icon: React.ElementType; roles: string[] };
type NavSection = { title: string; items: NavItem[] };

const navSections: NavSection[] = [
  {
    title: "Overview",
    items: [
      { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard, roles: [] },
    ],
  },
  {
    title: "Rooms & Guests",
    items: [
      { label: "Rooms", href: "/rooms", icon: BedDouble, roles: [...RECEPTION_ROLES] },
      { label: "Reservations", href: "/reservations", icon: CalendarDays, roles: [...RECEPTION_ROLES] },
      { label: "Check In", href: "/checkin", icon: LogIn, roles: [...RECEPTION_ROLES] },
      { label: "Check Out", href: "/checkout", icon: LogOut, roles: [...RECEPTION_ROLES] },
      { label: "Customers", href: "/customers", icon: Users, roles: ["OWNER","MANAGER","RECEPTIONIST","WAITER","SUPERVISOR","ACCOUNTANT"] },
      { label: "Guest Folios", href: "/folios", icon: FileText, roles: [...RECEPTION_ROLES, "CASHIER", "ACCOUNTANT"] },
    ],
  },
  {
    title: "Food & Beverage",
    items: [
      { label: "POS / Orders", href: "/pos", icon: ShoppingCart, roles: [...POS_ROLES] },
      { label: "Menu management", href: "/menu", icon: Utensils, roles: ["OWNER","MANAGER","ACCOUNTANT"] },
      { label: "Kitchen", href: "/kitchen", icon: ChefHat, roles: ["BARTENDER", "KITCHEN_STAFF", "SUPER_ADMIN", "OWNER", "MANAGER", "SUPERVISOR"] },
    ],
  },
  {
    title: "Inventory",
    items: [
      { label: "Stock", href: "/stock", icon: Package, roles: [...STOCK_ROLES] },
      { label: "Vendors", href: "/vendors", icon: Users, roles: [...STOCK_ROLES,"ACCOUNTANT"] },
      { label: "Purchasing", href: "/purchasing", icon: ShoppingBag, roles: [...STOCK_ROLES] },
    ],
  },
  {
    title: "Finance",
    items: [
      { label: "Cashier", href: "/cashier", icon: Wallet, roles: ["CASHIER", "SUPER_ADMIN", "OWNER", "MANAGER", "ACCOUNTANT", "SUPERVISOR"] },
      { label: "Credit Customers", href: "/credit", icon: CreditCard, roles: [...FINANCE_ROLES] },
      { label: "Expenses", href: "/expenses", icon: Receipt, roles: [...FINANCE_ROLES] },
      { label: "Reports", href: "/reports", icon: BarChart3, roles: [...FINANCE_ROLES, "SUPERVISOR"] },
    ],
  },
  {
    title: "Operations",
    items: [
      { label: "Housekeeping", href: "/housekeeping", icon: Home, roles: ["HOUSEKEEPER", "SUPER_ADMIN", "OWNER", "MANAGER", "SUPERVISOR"] },
      { label: "Maintenance", href: "/maintenance", icon: Wrench, roles: ["MAINTENANCE", "SUPER_ADMIN", "OWNER", "MANAGER", "SUPERVISOR"] },
    ],
  },
  {
    title: "Administration",
    items: [
      // Staff: Manager can manage staff below them, but Settings is admin-only
      { label: "Staff", href: "/staff", icon: Users, roles: ["SUPER_ADMIN", "OWNER", "MANAGER", "SUPERVISOR"] },
      { label: "Audit Trail", href: "/audit", icon: ClipboardList, roles: ["SUPER_ADMIN", "OWNER", "AUDITOR"] },
      { label: "Settings", href: "/settings", icon: Settings, roles: ["SUPER_ADMIN", "OWNER"] },
    ],
  },
];

export default function Sidebar({ role }: { role: string }) {
  const pathname = usePathname(); const {t}=useLocale();

  const {data:session}=useSession();
  const permissions=(session?.user as {permissions?:string[]})?.permissions;
  const byPage:Record<string,string[]>={"/rooms":["ROOM_VIEW"],"/reservations":["RESERVATION_VIEW"],"/checkin":["CHECKIN_PERFORM"],"/checkout":["CHECKOUT_PERFORM"],"/customers":["CUSTOMER_VIEW"],"/folios":["FOLIO_VIEW"],"/pos":["ORDER_CREATE"],"/menu":["PRODUCT_MANAGE"],"/kitchen":["KITCHEN_VIEW","BAR_VIEW"],"/stock":["PRODUCT_VIEW"],"/vendors":["VENDOR_VIEW"],"/purchasing":["PURCHASE_VIEW"],"/cashier":["PAYMENT_VIEW"],"/credit":["CREDIT_VIEW"],"/expenses":["FINANCIAL_REPORT_VIEW"],"/reports":["REPORT_VIEW","FINANCIAL_REPORT_VIEW"],"/housekeeping":["HOUSEKEEPING_VIEW"],"/maintenance":["MAINTENANCE_VIEW"],"/staff":["USER_VIEW"],"/audit":["AUDIT_VIEW"],"/settings":["HOTEL_SETTINGS_MANAGE"]};
  const canSee=(item:NavItem)=>item.href==="/dashboard"||(permissions?byPage[item.href]?.some(p=>permissions.includes(p)):item.roles.includes(role));
  return (
    <aside className="w-60 bg-slate-900 flex flex-col h-full shrink-0">
      {/* Logo */}
      <div className="flex items-center gap-2.5 px-5 py-5 border-b border-slate-700/50">
        <div className="w-8 h-8 bg-blue-500 rounded-lg flex items-center justify-center shrink-0">
          <Hotel className="w-4 h-4 text-white" />
        </div>
        <div className="min-w-0">
          <div className="text-white font-semibold text-sm leading-tight truncate">HotelPro</div>
          <div className="text-slate-400 text-xs">{t("Management System")}</div>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto py-4 px-3">
        {navSections.map((section) => {
          const visibleItems = section.items.filter(canSee);
          if (!visibleItems.length) return null;
          return (
            <div key={t(section.title)} className="mb-5">
              <div className="text-slate-500 text-xs font-semibold uppercase tracking-wider px-2 mb-1.5">
                {t(section.title)}
              </div>
              {visibleItems.map((item) => {
                const Icon = item.icon;
                const isActive = item.href === "/dashboard"
                  ? pathname === "/dashboard"
                  : pathname.startsWith(item.href);
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      "flex items-center gap-2.5 px-2.5 py-2 rounded-lg text-sm font-medium transition-all mb-0.5",
                      isActive
                        ? "bg-blue-600 text-white"
                        : "text-slate-400 hover:text-white hover:bg-slate-800"
                    )}
                  >
                    <Icon className="w-4 h-4 shrink-0" />
                    {t(item.label)}
                  </Link>
                );
              })}
            </div>
          );
        })}
      </nav>

      {/* Role badge */}
      <div className="px-4 py-3 border-t border-slate-700/50">
        <div className="text-slate-500 text-xs font-medium">{t(role)}</div>
      </div>
    </aside>
  );
}



