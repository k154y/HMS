import { NextResponse } from "next/server";
import { loginDestination, safeDestination, isLoginPath } from "@/lib/navigation";
import {auth} from "@/lib/auth";

// Overwrite the client header. Layouts still perform authentication and authorization.
export const proxy = auth((request) => {
  if(isLoginPath(request.nextUrl.pathname))return NextResponse.next();
  if (!request.auth?.accessToken) return NextResponse.redirect(new URL(loginDestination(request.nextUrl.pathname==="/security"?request.nextUrl.searchParams?.get("next")??"/dashboard":request.nextUrl.pathname),request.url));
  const headers = new Headers(request.headers);
  headers.set("x-workspace-path", safeDestination(request.nextUrl.pathname));
  return NextResponse.next({ request: { headers } });
});
export const config = { matcher: [
  "/dashboard/:path*", "/commerce/:path*", "/platform/:path*", "/security/:path*",
  "/customers/:path*", "/checkin/:path*", "/checkout/:path*", "/credit/:path*",
  "/cashier/:path*", "/rooms/:path*", "/reports/:path*", "/housekeeping/:path*",
  "/purchasing/:path*", "/reservations/:path*", "/audit/:path*", "/pos/:path*",
  "/menu/:path*", "/staff/:path*", "/settings/:path*", "/expenses/:path*",
  "/kitchen/:path*", "/maintenance/:path*", "/folios/:path*", "/stock/:path*", "/vendors/:path*",
] };
