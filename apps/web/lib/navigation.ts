const workspaceRoots = new Set([
  "dashboard", "commerce", "platform", "security", "customers", "checkin", "checkout",
  "credit", "cashier", "rooms", "reports", "housekeeping", "purchasing", "reservations",
  "audit", "pos", "menu", "staff", "settings", "expenses", "kitchen", "maintenance",
  "folios", "stock", "vendors",
]);

/** Deliberately accepts canonical pathnames only: no encoded paths, queries or fragments. */
export function safeDestination(value: unknown): string {
  if (typeof value !== "string" || value.length > 1024 || !/^\/[A-Za-z0-9_-]+(?:\/[A-Za-z0-9_-]+)*$/.test(value)) return "/dashboard";
  return workspaceRoots.has(value.split("/")[1]) && !value.endsWith("/login") ? value : "/dashboard";
}
export function loginDestination(path: unknown, expired = false): string {
  return `${loginRoute(applicationForPath(safeDestination(path)))}?next=${encodeURIComponent(safeDestination(path))}${expired ? "&reason=expired" : ""}`;
}
export function authRedirect(url: string, baseUrl: string): string {
  if (url === "/" || url === baseUrl || url === `${baseUrl}/`) return baseUrl;
  if (isLoginPath(url)) return baseUrl + url;
  if (url.startsWith("/")) return baseUrl + safeDestination(url);
  try {
    const target = new URL(url);
    if (target.origin === new URL(baseUrl).origin && !target.search && !target.hash) return baseUrl + (isLoginPath(target.pathname)?target.pathname:safeDestination(target.pathname));
  } catch { /* Invalid destinations use the default workspace. */ }
  return baseUrl + "/dashboard";
}

export type Application = "HMS" | "COMMERCE" | "PLATFORM_ADMIN";
export const applicationHome:Record<Application,string>={HMS:"/dashboard",COMMERCE:"/commerce",PLATFORM_ADMIN:"/platform/organizations"};
export function loginRoute(app:Application):string{return app==="COMMERCE"?"/commerce/login":app==="PLATFORM_ADMIN"?"/admin/login":"/hms/login";}
export function applicationForPath(path:string):Application{return path==="/commerce"||path.startsWith("/commerce/")?"COMMERCE":path==="/admin/login"||path==="/platform"||path.startsWith("/platform/")?"PLATFORM_ADMIN":"HMS";}
export function applicationDestination(app:Application,value:unknown):string{
 const path=safeDestination(value);
 return path!=="/security"&&applicationForPath(path)===app?path:applicationHome[app];
}
export function isLoginPath(path:string):boolean{return ["/hms/login","/commerce/login","/admin/login"].includes(path);}
export function applicationFromQuery(value:unknown):Application{return value==="COMMERCE"||value==="PLATFORM_ADMIN"?value:"HMS";}
