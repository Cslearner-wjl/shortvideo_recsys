import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import net from "node:net";

const canConnect = (urlString: string, timeoutMs = 200) =>
  new Promise<boolean>((resolve) => {
    let parsed: URL;
    try {
      parsed = new URL(urlString);
    } catch {
      resolve(false);
      return;
    }
    const port = parsed.port ? Number(parsed.port) : parsed.protocol === "https:" ? 443 : 80;
    const socket = net.connect({ host: parsed.hostname, port });
    const done = (ok: boolean) => {
      socket.removeAllListeners();
      socket.destroy();
      resolve(ok);
    };
    socket.setTimeout(timeoutMs);
    socket.once("connect", () => done(true));
    socket.once("timeout", () => done(false));
    socket.once("error", () => done(false));
  });

const pickProxyTarget = async (candidates: string[]) => {
  for (const candidate of candidates) {
    if (!candidate) continue;
    // eslint-disable-next-line no-await-in-loop
    if (await canConnect(candidate)) return candidate;
  }
  return candidates.find(Boolean) ?? "http://localhost:18080";
};

export default defineConfig(async ({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const preferred = env.VITE_PROXY_TARGET || env.VITE_BACKEND_URL || "http://localhost:18080";
  const proxyTarget = await pickProxyTarget([preferred, "http://localhost:8080"]);
  if (proxyTarget !== preferred) {
    // eslint-disable-next-line no-console
    console.log(`[vite] 后端代理不可达，已回退至 ${proxyTarget}（可用 VITE_PROXY_TARGET 指定）`);
  }

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
