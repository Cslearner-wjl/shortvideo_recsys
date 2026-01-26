import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  reporter: [
    ["html", { outputFolder: "../../docs/tests/frontend-admin/e2e-playwright" }],
    ["junit", { outputFile: "../../docs/tests/frontend-admin/e2e.junit.xml" }],
  ],
  use: {
    baseURL: "http://127.0.0.1:4174",
    headless: true,
  },
  webServer: {
    command: "npm run dev -- --host 127.0.0.1 --port 4174",
    url: "http://127.0.0.1:4174",
    reuseExistingServer: true,
    timeout: 120000,
  },
});
