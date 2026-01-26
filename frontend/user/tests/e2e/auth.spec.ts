import { expect, test } from "@playwright/test";

test.describe("用户端路由鉴权", () => {
  test("未登录访问推荐页会跳转登录页", async ({ page }) => {
    await page.goto("/feed");
    await expect(page).toHaveURL(/\/login$/);
    await expect(page.getByRole("heading", { name: "欢迎回来" })).toBeVisible();
  });

  test("登录态可访问推荐页", async ({ page }) => {
    await page.addInitScript(() => {
      localStorage.setItem("sv_user_token", "e2e-token");
    });
    await page.goto("/feed");
    await expect(page).toHaveURL(/\/feed$/);
    await expect(page.getByText("推荐", { exact: true })).toBeVisible();
  });
});
