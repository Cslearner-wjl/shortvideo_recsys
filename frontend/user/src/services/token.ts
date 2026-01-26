import { ref } from "vue";

const TOKEN_KEY = "sv_user_token";

const storedToken = localStorage.getItem(TOKEN_KEY);
export const tokenState = ref<string | null>(storedToken);

export const getToken = () => tokenState.value;

export const setToken = (token: string) => {
  tokenState.value = token;
  localStorage.setItem(TOKEN_KEY, token);
};

export const clearToken = () => {
  tokenState.value = null;
  localStorage.removeItem(TOKEN_KEY);
};
