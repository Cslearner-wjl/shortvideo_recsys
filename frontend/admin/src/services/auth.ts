// 管理端 Basic Auth 登录与退出处理。
import { requestApi } from "./http";
import { clearAuthToken, setAuthToken } from "./token";

const encodeBasic = (account: string, password: string) => {
  const raw = `${account}:${password}`;
  const encoded = window.btoa(unescape(encodeURIComponent(raw)));
  return `Basic ${encoded}`;
};

export const login = async (account: string, password: string) => {
  const token = encodeBasic(account, password);
  await requestApi("/api/admin/users?page=1&size=1", {
    authToken: token,
  });
  setAuthToken(token);
  return token;
};

export const logout = () => {
  clearAuthToken();
};
