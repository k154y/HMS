export const ROLE_LABELS: Record<string, string> = { OWNER: "Owner", MANAGER: "Manager", RECEPTIONIST: "Receptionist", CASHIER: "Cashier", WAITER: "Waiter", HOUSEKEEPING: "Housekeeping" };
export const MANAGER_ROLES = ["OWNER", "MANAGER"];
export function formatCurrency(value: number | string, currency = "RWF") { return new Intl.NumberFormat("en", { style: "currency", currency }).format(Number(value || 0)); }
export function getDaysUntilTrialExpiry(date?: string | Date | null) { return date ? Math.ceil((new Date(date).getTime() - Date.now()) / 86400000) : 0; }
export function generateNumber(prefix: string) { return `${prefix}-${Date.now().toString(36).toUpperCase()}`; }
export function formatDate(value: string | Date) { return new Intl.DateTimeFormat("en-GB", { dateStyle: "medium" }).format(new Date(value)); }
export const BOOKING_STATUS_COLORS: Record<string,string> = { CONFIRMED:"bg-blue-100 text-blue-700", CHECKED_IN:"bg-emerald-100 text-emerald-700", CHECKED_OUT:"bg-gray-100 text-gray-700", CANCELLED:"bg-red-100 text-red-700", NO_SHOW:"bg-amber-100 text-amber-700" };
export const RECEPTION_ROLES = ["OWNER","MANAGER","SUPERVISOR","RECEPTIONIST"];
export const POS_ROLES = ["OWNER","MANAGER","SUPERVISOR","WAITER","RECEPTIONIST"];
export const STOCK_ROLES = ["OWNER","MANAGER","SUPERVISOR","STOREKEEPER"];
export const FINANCE_ROLES = ["OWNER","MANAGER","SUPERVISOR","ACCOUNTANT","CASHIER"];
export const ORDER_STATUS_COLORS: Record<string,string> = { OPEN:"bg-blue-100 text-blue-700", IN_PROGRESS:"bg-amber-100 text-amber-700", READY:"bg-emerald-100 text-emerald-700", SERVED:"bg-gray-100 text-gray-700", VOIDED:"bg-red-100 text-red-700" };
export const ROOM_STATUS_COLORS: Record<string,string> = { AVAILABLE:"bg-emerald-100 text-emerald-700", RESERVED:"bg-blue-100 text-blue-700", OCCUPIED:"bg-amber-100 text-amber-700", MAINTENANCE:"bg-red-100 text-red-700" };
export function cn(...values: Array<string | false | null | undefined>) { return values.filter(Boolean).join(" "); }
