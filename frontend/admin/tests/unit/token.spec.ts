import { beforeEach, describe, expect, it } from "vitest";
import { clearAuthToken, getAuthToken, setAuthToken } from "../../src/services/token";

describe("admin token service", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("保存 token 后可读取", () => {
    setAuthToken("Basic e2e-token");
    expect(getAuthToken()).toBe("Basic e2e-token");
    expect(localStorage.getItem("sv_admin_basic")).toBe("Basic e2e-token");
  });

  it("清理 token 后为空", () => {
    setAuthToken("Basic e2e-token");
    clearAuthToken();
    expect(getAuthToken()).toBeNull();
    expect(localStorage.getItem("sv_admin_basic")).toBeNull();
  });
});
