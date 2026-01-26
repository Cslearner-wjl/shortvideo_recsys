// 管理端统一请求封装，处理 JSON/FormData 与错误透传。
import { clearAuthToken, getAuthToken } from "./token";

type RequestOptions = {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
  authToken?: string | null;
};

export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

const configured = import.meta.env.VITE_API_BASE_URL?.toString().trim();
const baseURL = configured ?? (import.meta.env.DEV ? "" : "http://localhost:18080");

export class HttpError extends Error {
  status: number;
  payload?: unknown;

  constructor(message: string, status: number, payload?: unknown) {
    super(message);
    this.status = status;
    this.payload = payload;
  }
}

const buildBody = (body: unknown) => {
  if (body === undefined || body === null) {
    return undefined;
  }
  if (body instanceof FormData) {
    return body;
  }
  return JSON.stringify(body);
};

export const request = async <T>(path: string, options: RequestOptions = {}) => {
  const token = options.authToken !== undefined ? options.authToken : getAuthToken();
  const isFormData = options.body instanceof FormData;
  const headers: Record<string, string> = {
    ...(options.headers ?? {}),
  };

  if (!isFormData) {
    headers["Content-Type"] = "application/json";
  }

  if (token) {
    headers.Authorization = token;
  }

  const response = await fetch(`${baseURL}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: buildBody(options.body),
  });

  if (!response.ok) {
    let payload: unknown = null;
    try {
      payload = await response.json();
    } catch {
      payload = await response.text();
    }
    if (response.status === 401) {
      clearAuthToken();
    }
    throw new HttpError("Request failed", response.status, payload);
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

export const requestApi = async <T>(path: string, options: RequestOptions = {}) => {
  const result = await request<ApiResponse<T> | T>(path, options);
  if (result && typeof result === "object" && "code" in (result as ApiResponse<T>)) {
    const api = result as ApiResponse<T>;
    if (api.code !== 0) {
      throw new HttpError(api.message || "Request failed", 200, api);
    }
    return api.data;
  }
  return result as T;
};
