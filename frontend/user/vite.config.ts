import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const proxyTarget = env.VITE_PROXY_TARGET || "http://localhost:18080";

  return {
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 解决 Windows 无法直连 WSL 端口的问题：浏览器请求走 5173，同源由 Vite 代理到后端
      "/api": {
        target: proxyTarget,
        changeOrigin: true,
      },
      "/actuator": {
        target: proxyTarget,
        changeOrigin: true,
      },
    },
  },
  };
});

