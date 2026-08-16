import { test, expect } from "@playwright/test";
import { signInViaKeycloak } from "./support/login";

// Golden path: sign in -> add a product to cart -> reach checkout. Needs a
// real Keycloak customer account, so it's opt-in via env vars rather than
// run unconditionally — this environment has no seeded e2e test user (see
// PLAN.md's Phase 16 writeup for why this is written-but-not-yet-executed).
const USERNAME = process.env.E2E_CUSTOMER_USERNAME;
const PASSWORD = process.env.E2E_CUSTOMER_PASSWORD;

test.describe("customer checkout", () => {
  test.skip(!USERNAME || !PASSWORD, "E2E_CUSTOMER_USERNAME/PASSWORD not configured");

  test("sign in, add an item to cart, and reach checkout", async ({ page }) => {
    await page.goto("/");
    await signInViaKeycloak(page, USERNAME!, PASSWORD!);

    const productCard = page.locator("a[href^='/products/']").first();
    await expect(productCard).toBeVisible({ timeout: 15_000 });
    await productCard.click();

    await page.getByRole("button", { name: "Add to cart" }).click();
    await expect(page.getByText("Added to cart.")).toBeVisible();

    await page.goto("/cart");
    await expect(page.getByRole("button", { name: /checkout/i })).toBeVisible();
  });
});
