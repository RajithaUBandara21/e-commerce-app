import { test, expect } from "@playwright/test";

// proxy.ts gates /cart, /checkout, /account, /orders, /seller, /admin —
// verifies the gate actually redirects an anonymous visitor to Keycloak
// rather than silently rendering a gated page.
test.describe("auth gates redirect anonymous visitors", () => {
  for (const path of ["/cart", "/orders", "/seller", "/seller/products", "/admin"]) {
    test(`${path} redirects to sign-in`, async ({ page }) => {
      await page.goto(path);
      await expect(page).toHaveURL(/\/realms\/.+\/protocol\/openid-connect\/auth|\/api\/auth\/signin/);
    });
  }

  test("PDP's add-to-cart button routes an anonymous visitor toward sign-in", async ({ page }) => {
    await page.goto("/");
    const productCard = page.locator("a[href^='/products/']").first();
    test.skip(!(await productCard.isVisible().catch(() => false)), "No products in the catalog right now");

    await productCard.click();
    const addToCart = page.getByRole("button", { name: "Sign in to add to cart" });
    await expect(addToCart).toBeVisible();
    await addToCart.click();
    await expect(page).toHaveURL(/\/realms\/.+\/protocol\/openid-connect\/auth|\/api\/auth\/signin/);
  });
});
