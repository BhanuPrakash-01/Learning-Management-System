import { test, expect } from "@playwright/test";

test("register page shows two-step flow", async ({ page }) => {
  await page.goto("/register");
  await expect(page.getByText("Step 1: Personal details")).toBeVisible();
  await page.getByLabel("Full name").fill("Test User");
  await page.getByLabel("Email address").fill("test@anurag.ac.in");
  await page.getByLabel("Password").fill("Password@123");
  await page.getByRole("button", { name: "Continue to Academic Details" }).click();
  await expect(page.getByText("Step 2: Academic details")).toBeVisible();
});
