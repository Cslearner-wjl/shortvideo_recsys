// 管理端鉴权 token 的本地存储封装。
const TOKEN_KEY = "sv_admin_basic";

export const getAuthToken = () => localStorage.getItem(TOKEN_KEY);

export const setAuthToken = (token: string) => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const clearAuthToken = () => {
  localStorage.removeItem(TOKEN_KEY);
};
