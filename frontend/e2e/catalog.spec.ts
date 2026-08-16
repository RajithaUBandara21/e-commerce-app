import { test, expect } from "@playwright/test";

// The one golden path that needs no auth at all: browse -> search -> PDP.
// Runs against whatever catalog data the backend currently has — asserts on
// structure/behavior, never on specific product names, since this suite
// doesn't seed its own data.
test.describe("catalog browsing", () => {
  test("homepage loads and either shows products or degrades gracefully", async ({ page }) => {
    await page.goto("/");

    const catalogUnavailable = page.getByText("Couldn't load the catalog right now");
    const productCard = page.locator("a[href^='/products/']").first();

    await expect(catalogUnavailable.or(productCard)).toBeVisible({ timeout: 15_000 });
  });

  test("search narrows results via the query string", async ({ page }) => {
    await page.goto("/");

    await page.getByRole("button", { name: "Search" }).click();
    await page.getByPlaceholder("Search products…").fill("this-should-not-match-anything-xyz");
    await page.getByPlaceholder("Search products…").press("Enter");

    await expect(page).toHaveURL(/\?q=this-should-not-match-anything-xyz/);
    await expect(page.getByText("No products match your search.")).toBeVisible();
  });

  test("opening a product shows its detail page", async ({ page }) => {
    await page.goto("/");

    const productCard = page.locator("a[href^='/products/']").first();
    test.skip(!(await productCard.isVisible().catch(() => false)), "No products in the catalog right now");

    await productCard.click();
    await expect(page).toHaveURL(/\/products\/\d+/);
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    // VariantPicker is present regardless of sign-in state.
    await expect(page.getByText(/Add to cart|Sign in to add to cart/)).toBeVisible();
  });
});
