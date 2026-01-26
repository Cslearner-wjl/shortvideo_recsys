import { clearToken, getToken } from "./token";

type RequestOptions = {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
};

const configured = import.meta.env.VITE_API_BASE_URL?.toString().trim();
// dev 默认走同源 + Vite proxy（避免 Windows/WSL 端口互访问题）
const baseURL = configured ?? (import.meta.env.DEV ? "" : "http://localhost:8080");

export class HttpError extends Error {
  status: number;
  payload?: unknown;

  constructor(message: string, status: number, payload?: unknown) {
    super(message);
    this.status = status;
    this.payload = payload;
  }
}

export const request = async <T>(path: string, options: RequestOptions = {}) => {
  const token = getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${baseURL}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  if (!response.ok) {
    let payload: unknown = null;
    try {
      payload = await response.json();
    } catch {
      payload = await response.text();
    }
    if (response.status === 401 || response.status === 403) {
      clearToken();
      throw new HttpError("请先登录或重新登录", response.status, payload);
    }
    const payloadMessage =
      typeof payload === "string"
        ? payload
        : (payload as { message?: string } | null)?.message;
    throw new HttpError(payloadMessage || "Request failed", response.status, payload);
  }

  if (response.status === 204) {
    return null as T;
  }

  const text = await response.text();
  if (!text) {
    return null as T;
  }
  try {
    return JSON.parse(text) as T;
  } catch {
    return text as T;
  }
};
