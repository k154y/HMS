import {proxyError, proxyResponse, referenceId} from "@/lib/api-proxy-errors";
import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/lib/auth";

const base =
  process.env.HMS_API_URL ??
  "http://localhost:8081/api/v1";

const hotelResources = new Set([
  "customers",
  "products",
  "vendors",
  "memberships",
  "roles",
  "permissions",
  "branches",
  "exchange-rates",
  "currencies",
  "payment-accounts",
  "expense-categories",
  "audit-events",
]);

const branchResources = new Set([
  "reservations",
  "rooms",
  "room-types",
  "folios",
  "orders",
  "payments",
  "payment-approvals",
  "purchase-orders",
  "cashier-shifts",
  "maintenance",
  "housekeeping",
  "credit",
  "reports",
  "inventory",
  "dashboard",
  "expenses",
  "non-resident-bills",
]);

const aliases: Record<string, string> = {
  bookings: "reservations",
  suppliers: "vendors",
  users: "memberships",
  "menu-items": "products",
  audit: "audit-events",
  stock: "inventory",
  "credit-customers": "credit",
};

async function forward(
  req: NextRequest,
  {
    params,
  }: {
    params: Promise<{
      path: string[];
    }>;
  },
) {
  const { path } = await params;
  const requestId = referenceId(req.headers);

  if (path.length===2 && path[0]==="account" && ["forgot-password","reset-password","password"].includes(path[1])) {
    if(req.method!=="POST") return proxyError(405,requestId);
    const origin=req.headers.get("origin");
    let originHost="";
    try {originHost=origin?new URL(origin).host:"";} catch {return proxyError(403,requestId);}
    if(!originHost || originHost!==req.headers.get("host")) return proxyError(403,requestId);
    const current=path[1]==="password"?await auth():null;
    if(path[1]==="password"&&!current?.accessToken)return proxyError(401,requestId);
    try {
      const response=await fetch(`${base}/auth/${path[1]}`,{method:"POST",cache:"no-store",headers:{"Content-Type":"application/json","X-Request-ID":requestId,...(current?.accessToken?{Authorization:`Bearer ${current.accessToken}`}:{})},body:await req.text(),signal:AbortSignal.timeout(15000)});
      return proxyResponse(response,requestId);
    } catch {return proxyError(502,requestId);}

  }

  const session =
    await auth();

  if (!session?.accessToken) {
    return proxyError(401, requestId);
  }

  const user =
    session.user as {
      hotelId?: string;
      branchId?: string;
    };

  if (
    !user.hotelId ||
    !user.branchId
  ) {
    return proxyError(400, requestId, "Hotel and branch context are required");
  }

  if (
    path.some(
      (segment) =>
        !/^[a-zA-Z0-9_-]+$/.test(
          segment,
        ),
    )
  ) {
    return proxyError(400, requestId, "Invalid API path");
  }

  const resource =
    aliases[path[0]] ??
    path[0];

  if (
    !hotelResources.has(
      resource,
    ) &&
    !branchResources.has(
      resource,
    ) &&
    resource !== "settings"
  ) {
    return proxyError(404, requestId);
  }

  const scope =
    `${base}/hotels/${encodeURIComponent(
      user.hotelId,
    )}`;

  const suffix =
    path
      .slice(1)
      .map(encodeURIComponent)
      .join("/");

  const resourcePath =
    resource === "settings"
      ? ""
      : `${
          hotelResources.has(
            resource,
          )
            ? ""
            : `/branches/${encodeURIComponent(
                user.branchId,
              )}`
        }/${resource}`;

  const url =
    new URL(
      `${scope}${resourcePath}${
        suffix
          ? `/${suffix}`
          : ""
      }`,
    );

  req.nextUrl.searchParams.forEach(
    (value, key) =>
      url.searchParams.set(
        key,
        value,
      ),
  );

  try {
    const response =
      await fetch(url, {
        method: req.method,
        cache: "no-store",
        headers: {
          "Content-Type":
            "application/json",
          "X-Request-ID": requestId,
          Authorization:
            `Bearer ${session.accessToken}`,
        },
        body: [
          "GET",
          "HEAD",
        ].includes(req.method)
          ? undefined
          : await req.text(),
        signal:
          AbortSignal.timeout(
            30000,
          ),
      });

    return proxyResponse(response,requestId);
  } catch {
    return proxyError(502, requestId);
  }
}

export const GET = forward;
export const POST = forward;
export const PATCH = forward;
export const PUT = forward;
export const DELETE = forward;