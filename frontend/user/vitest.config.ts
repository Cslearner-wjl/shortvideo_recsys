import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    environment: "jsdom",
    include: ["tests/unit/**/*.spec.ts"],
    reporters: ["default", "junit"],
    outputFile: "../../docs/tests/frontend-user/unit.junit.xml",
  },
});
