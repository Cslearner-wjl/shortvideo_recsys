import { beforeEach, describe, expect, it } from "vitest";
import { clearToken, getToken, setToken } from "../../src/services/token";

describe("token service", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("保存 token 后可读取", () => {
    setToken("demo-token");
    expect(getToken()).toBe("demo-token");
    expect(localStorage.getItem("sv_user_token")).toBe("demo-token");
  });

  it("清理 token 后为空", () => {
    setToken("demo-token");
    clearToken();
    expect(getToken()).toBeNull();
    expect(localStorage.getItem("sv_user_token")).toBeNull();
  });
});
