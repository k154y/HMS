import { NextRequest } from "next/server";
import { auth } from "@/lib/auth";
import { proxyError, proxyResponse, referenceId } from "@/lib/api-proxy-errors";

// Commerce requests are deliberately separate from the hotel's context-bound proxy.
const base = process.env.HMS_API_URL ?? "http://localhost:8081/api/v1";
const id = /^[a-zA-Z0-9_-]+$/;

async function forward(request: NextRequest, { params }: { params: Promise<{ path: string[] }> }) {
  const requestId = referenceId(request.headers);
  const { path } = await params;
  if (!path.length || path[0] !== "businesses" || path.some(segment => !id.test(segment))) {
    return proxyError(404, requestId);
  }
  // The backend authorizes business ownership, entitlements and each mutation.
  const session = await auth();
  if (!session?.accessToken) return proxyError(401, requestId);
  if (!["GET", "POST"].includes(request.method)) return proxyError(405, requestId);
  if (request.method !== "GET") {
    const origin = request.headers.get("origin");
    const host = request.headers.get("host");
    let sameOrigin = false;
    try { sameOrigin = !!origin && new URL(origin).host === host; } catch { sameOrigin = false; }
    if (!sameOrigin) return proxyError(403, requestId);
    const type = request.headers.get("content-type") ?? "";
    if (!type.toLowerCase().startsWith("application/json")) return proxyError(415, requestId);
  }
  const url = new URL(`${base}/commerce/${path.map(encodeURIComponent).join("/")}`);
  request.nextUrl.searchParams.forEach((value, key) => url.searchParams.append(key, value));
  try {
    const response = await fetch(url, {
      method: request.method,
      headers: { Authorization: `Bearer ${session.accessToken}`, "X-Request-ID": requestId,
        ...(request.method !== "GET" ? {"Content-Type": "application/json"} : {}) },
      body: request.method === "GET" ? undefined : await request.text(),
      cache: "no-store", signal: AbortSignal.timeout(30000),
    });
    return proxyResponse(response, requestId);
  } catch { return proxyError(502, requestId); }
}
export const GET = forward;
export const POST = forward;
