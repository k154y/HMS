export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
};

export type ApiFieldError = {
  field: string;
  message: string;
};

type ApiErrorPayload = {
  requestId?: string;
  code?: string;
  message?: string;
  error?: string;
  fieldErrors?: ApiFieldError[];
};

export class HmsApiError extends Error {
  readonly status: number;
  readonly requestId?: string;
  readonly safeMessage: string;
  readonly code?: string;
  readonly fieldErrors: ApiFieldError[];

  constructor(
    status: number,
    message: string,
    code?: string,
    fieldErrors: ApiFieldError[] = [],
    requestId?: string,
  ) {
    super([message, ...fieldErrors.map(item => `${item.field}: ${item.message}`)].join(" "));
    this.safeMessage = message;
    this.requestId = requestId;
    this.name = "HmsApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

export function isHmsApiError(error: unknown): error is HmsApiError {
  return error instanceof HmsApiError;
}

function validFieldErrors(value: unknown): ApiFieldError[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .filter(
      (item): item is ApiFieldError =>
        typeof item === "object" &&
        item !== null &&
        typeof (item as ApiFieldError).field === "string" &&
        typeof (item as ApiFieldError).message === "string",
    )
    .map((item) => ({
      field: item.field,
      message: item.message,
    }));
}

export async function api<T>(
  path: string,
  method = "GET",
  body?: unknown,
): Promise<T> {
  const requestId = crypto.randomUUID();
  let response: Response;
  try { response = await fetch(`/api/${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      "X-Request-ID": requestId,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    cache: "no-store",
  });
  } catch { throw new HmsApiError(0, "The system could not be reached. Check your connection and try again.", "NETWORK_ERROR", [], requestId); }

  if (response.status === 204) {
    return undefined as T;
  }

  let data: unknown;

  try {
    data = await response.json();
  } catch {
    data = {};
  }

  if (!response.ok) {
    const payload =
      typeof data === "object" && data !== null
        ? (data as ApiErrorPayload)
        : {};

    const reference = payload.requestId ?? response.headers.get("X-Request-ID") ?? requestId;
    const statusMessage: Record<number,string> = {
      401: "Your session has expired or is no longer valid. Please sign in again.",
      403: "You don't have permission to perform this action.",
      404: "The requested record could not be found or is no longer available.",
    };
    const message = response.status >= 500
      ? `We couldn't complete this action because of a system problem. Please contact the system administrator and provide reference ID: ${reference}.`
      : statusMessage[response.status] ?? payload.message ?? payload.error ?? "Please check the information you entered and try again.";
    if (response.status === 401 && typeof window !== "undefined") window.location.replace("/login?reason=expired");
    throw new HmsApiError(response.status, message, payload.code,
      response.status >= 500 ? [] : validFieldErrors(payload.fieldErrors), reference);
  }

  return data as T;
}

export async function all<T>(path: string): Promise<T[]> {
  const separator = path.includes("?") ? "&" : "?";

  const data = await api<T[] | Page<T>>(
    `${path}${separator}size=100`,
  );

  if (Array.isArray(data)) {
    return data;
  }

  const result = [...data.content];

  for (let page = 1; page < data.totalPages; page++) {
    const next = await api<Page<T>>(
      `${path}${separator}size=100&page=${page}`,
    );

    result.push(...next.content);
  }

  return result;
}

export const number = (value: unknown) =>
  Number(value ?? 0);