import { defineConfig, devices } from "@playwright/test";

// Golden-path e2e, not a full browser matrix — chromium only, matching the
// scope this suite actually needs (see PLAN.md's Phase 16 writeup).
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: "line",
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    trace: "on-first-retry",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  // Reuses whatever dev server is already running locally; in CI the
  // Jenkinsfile starts one explicitly before this config's webServer would
  // otherwise need to (kept undefined here so this file also works against a
  // developer's already-running `npm run dev`).
  webServer: process.env.CI
    ? {
        command: "npm run start",
        url: "http://localhost:3000",
        reuseExistingServer: false,
        timeout: 60_000,
      }
    : undefined,
});
