import { expect, test } from "@playwright/test";

test.describe("管理端路由鉴权", () => {
  test("未登录访问看板会跳转登录页", async ({ page }) => {
    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByText("管理端登录")).toBeVisible();
  });

  test("登录态可进入管理端首页", async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem("sv_admin_basic", "Basic e2e-token");
    });
    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/dashboard$/);
    await expect(page.getByRole("menuitem", { name: "数据看板" })).toBeVisible();
  });
});
