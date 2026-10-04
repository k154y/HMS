import { NextResponse } from "next/server";

export function referenceId(headers: Headers): string {
  const value = headers.get("X-Request-ID");
  return value && /^[A-Za-z0-9._-]{1,64}$/.test(value) ? value : crypto.randomUUID();
}

export function proxyError(status: number, requestId: string, message?: string, code?: string) {
  const defaults: Record<number, string> = {
    400: "Please check the information you entered and try again.",
    401: "Your session has expired or is no longer valid. Please sign in again.",
    403: "You don't have permission to perform this action.",
    404: "The requested record could not be found or is no longer available.",
    405: "This action is not supported.",
  };
  const codes: Record<number,string> = {400:"VALIDATION_FAILED",401:"UNAUTHORIZED",403:"ACCESS_DENIED",404:"RESOURCE_NOT_FOUND",405:"METHOD_NOT_ALLOWED"};
  if (status >= 500) console.error(`event=api_proxy_failure requestId=${requestId} status=${status}`);
  return NextResponse.json({status, code: code ?? (status >= 500 ? "INTERNAL_ERROR" : codes[status] ?? "INVALID_REQUEST"),
    message: status >= 500
      ? `We couldn't complete this action because of a system problem. Please contact the system administrator and provide reference ID: ${requestId}.`
      : message ?? defaults[status], requestId, fieldErrors: []},
    {status, headers: {"Cache-Control":"no-store", "X-Request-ID":requestId}});
}

export async function proxyResponse(response: Response, requestId: string) {
  const reference = referenceId(new Headers({"X-Request-ID":response.headers.get("X-Request-ID") ?? requestId}));
  if (response.status === 204) return new NextResponse(null, {status:204});
  if (response.status >= 500) return proxyError(response.status, reference);
  return new NextResponse(await response.text(), {status:response.status,
    headers:{"Content-Type":"application/json", "Cache-Control":"no-store", "X-Request-ID":reference}});
}
