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