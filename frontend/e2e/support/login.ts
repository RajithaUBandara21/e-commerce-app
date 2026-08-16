import type { Page } from "@playwright/test";

// Drives Keycloak's default login theme (fields: #username, #password, submit
// #kc-login) after NextAuth redirects there. Real automation, not a stub —
// only exercised by specs that first check test credentials are configured
// (see e2e/customer-checkout.spec.ts / e2e/seller-flow.spec.ts).
export async function signInViaKeycloak(page: Page, username: string, password: string) {
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.waitForURL(/\/realms\/.+\/protocol\/openid-connect\/auth/);
  await page.locator("#username").fill(username);
  await page.locator("#password").fill(password);
  await page.locator("#kc-login").click();
  await page.waitForURL((url) => !url.pathname.includes("/realms/"));
}
