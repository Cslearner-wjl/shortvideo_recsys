import { request } from "./http";

type AuthResponse = {
  token?: string;
  data?: {
    token?: string;
  };
};

export const login = async (payload: { account: string; password: string }) => {
  const result = await request<AuthResponse>("/api/auth/login", {
    method: "POST",
    body: payload,
  });
  return result?.token ?? result?.data?.token ?? "";
};

export const register = async (payload: {
  username: string;
  phone: string;
  email: string;
  password: string;
  emailCode: string;
}) => {
  return request("/api/auth/register", {
    method: "POST",
    body: payload,
  });
};

export const requestCode = async (payload: { email: string }) => {
  return request("/api/auth/email-code", {
    method: "POST",
    body: payload,
  });
};
