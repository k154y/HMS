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

  if (path.length===2 && path[0]==="account" && ["forgot-password","reset-password","password"].includes(path[1])) {
    if(req.method!=="POST") return NextResponse.json({message:"Method not allowed"},{status:405});
    const origin=req.headers.get("origin");
    if(!origin || new URL(origin).host!==req.headers.get("host")) return NextResponse.json({message:"Invalid request origin"},{status:403});
    const current=path[1]==="password"?await auth():null;
    if(path[1]==="password"&&!current?.accessToken)return NextResponse.json({message:"Sign in required"},{status:401});
    try {
      const response=await fetch(`${base}/auth/${path[1]}`,{method:"POST",cache:"no-store",headers:{"Content-Type":"application/json",...(current?.accessToken?{Authorization:`Bearer ${current.accessToken}`}:{})},body:await req.text(),signal:AbortSignal.timeout(15000)});
      if(response.status===204)return new NextResponse(null,{status:204});
      if(!response.ok)return NextResponse.json({message:response.status===429?"Please wait before trying again.":path[1]==="reset-password"?"The reset link is invalid, expired, or the password does not meet the requirements.":path[1]==="password"?"Unable to change password. Check your current password and try again.":"Unable to send instructions. Please retry later."},{status:response.status,headers:{"Cache-Control":"no-store"}});
      return new NextResponse(await response.text(),{status:response.status,headers:{"Content-Type":"application/json","Cache-Control":"no-store"}});
    } catch {return NextResponse.json({message:"The hotel API is unavailable. Please retry."},{status:502});}
  }

  const session =
    await auth();

  if (!session?.accessToken) {
    return NextResponse.json(
      {
        error: "Sign in required",
      },
      {
        status: 401,
      },
    );
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
    return NextResponse.json(
      {
        error:
          "Hotel and branch context are required",
      },
      {
        status: 400,
      },
    );
  }

  if (
    path.some(
      (segment) =>
        !/^[a-zA-Z0-9_-]+$/.test(
          segment,
        ),
    )
  ) {
    return NextResponse.json(
      {
        error: "Invalid API path",
      },
      {
        status: 400,
      },
    );
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
    return NextResponse.json(
      {
        error:
          "Unknown API resource",
      },
      {
        status: 404,
      },
    );
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

    if (
      response.status === 204
    ) {
      return new NextResponse(
        null,
        {
          status: 204,
        },
      );
    }

    return new NextResponse(
      await response.text(),
      {
        status:
          response.status,
        headers: {
          "Content-Type":
            response.headers.get(
              "content-type",
            ) ??
            "application/json",
          "Cache-Control":
            "no-store",
        },
      },
    );
  } catch {
    return NextResponse.json(
      {
        error:
          "The hotel API is unavailable. Please retry.",
      },
      {
        status: 502,
      },
    );
  }
}

export const GET = forward;
export const POST = forward;
export const PATCH = forward;
export const PUT = forward;
export const DELETE = forward;