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
  code?: string;
  message?: string;
  error?: string;
  fieldErrors?: ApiFieldError[];
};

export class HmsApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly fieldErrors: ApiFieldError[];

  constructor(
    status: number,
    message: string,
    code?: string,
    fieldErrors: ApiFieldError[] = [],
  ) {
    super(message);
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
  const response = await fetch(`/api/${path}`, {
    method,
    headers: {
      "Content-Type": "application/json",
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    cache: "no-store",
  });

  if (response.status === 401) {
    window.location.replace("/login?reason=expired");

    throw new HmsApiError(
      401,
      "Session expired. Please sign in again.",
      "SESSION_EXPIRED",
    );
  }

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

    throw new HmsApiError(
      response.status,
      payload.message ??
        payload.error ??
        `Request failed (${response.status})`,
      payload.code,
      validFieldErrors(payload.fieldErrors),
    );
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