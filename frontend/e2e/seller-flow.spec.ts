import { test, expect } from "@playwright/test";
import { signInViaKeycloak } from "./support/login";

// Golden path: sign in as an already-ACTIVE seller -> reach the dashboard ->
// see the fulfillment queue. Needs a real Keycloak account already holding
// the `seller` role (an admin has to approve a registration for that to be
// true), so this is opt-in the same way customer-checkout.spec.ts is — see
// PLAN.md's Phase 16 writeup.
const USERNAME = process.env.E2E_SELLER_USERNAME;
const PASSWORD = process.env.E2E_SELLER_PASSWORD;

test.describe("seller dashboard", () => {
  test.skip(!USERNAME || !PASSWORD, "E2E_SELLER_USERNAME/PASSWORD not configured");

  test("active seller reaches the overview and orders-to-fulfill views", async ({ page }) => {
    await page.goto("/seller");
    await signInViaKeycloak(page, USERNAME!, PASSWORD!);

    await expect(page.getByRole("heading", { name: "Overview" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Orders" })).toBeVisible();

    await page.getByRole("link", { name: "Orders" }).click();
    await expect(page.getByRole("heading", { name: "Orders to fulfill" })).toBeVisible();
  });
});
