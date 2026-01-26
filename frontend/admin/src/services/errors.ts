// 统一解析接口错误信息，便于页面提示。
import { HttpError } from "./http";

type ApiErrorPayload = {
  message?: string;
  error?: string;
  code?: number;
};

export const resolveErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof HttpError) {
    const payload = error.payload as ApiErrorPayload | string | null | undefined;
    if (payload && typeof payload === "object") {
      return payload.message || payload.error || fallback;
    }
    if (typeof payload === "string" && payload.trim()) {
      return payload;
    }
    return fallback;
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
};
