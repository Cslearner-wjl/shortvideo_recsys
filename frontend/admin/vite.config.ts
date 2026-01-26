import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const proxyTarget = env.VITE_PROXY_TARGET || "http://localhost:18080";

  return {
    plugins: [vue()],
    server: {
      port: 5174,
      proxy: {
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
