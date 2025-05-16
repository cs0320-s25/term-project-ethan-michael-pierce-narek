import { test, expect } from "@playwright/test";

test.describe("Home Page", () => {
    test.beforeEach(async ({ page }) => {
        await page.goto("/");
    });

    test("should show login button if not signed in", async ({ page }) => {
        await expect(page.getByRole("button", { name: "Get Started" })).toBeVisible();
    });

    test("class distribution buttons should update selected class count", async ({ page }) => {
        // Assuming Clerk test mode or user is signed in
        await page.goto("/");
        const totalClassButtons = page.locator(".number-selector button");
        await totalClassButtons.nth(4).click(); // Click on button for 5 classes
        await expect(totalClassButtons.nth(4)).toHaveClass(/selected/);
    });

    test("should add and remove a required course", async ({ page }) => {
        await page.fill('input[placeholder="Enter course codes (e.g., CSCI 0320)"]', "CSCI 0320");
        await page.keyboard.press("Enter");

        const tag = page.locator(".selected-tags span", { hasText: "CSCI 0320" });
        await expect(tag).toBeVisible();

        const closeButton = tag.locator("button");
        await closeButton.click();
        await expect(tag).not.toBeVisible();
    });

    test("should add and remove elective department", async ({ page }) => {
        await page.selectOption("select.dropdown", "CSCI");

        const tag = page.locator(".selected-tags span", { hasText: "CSCI" });
        await expect(tag).toBeVisible();

        const closeButton = tag.locator("button");
        await closeButton.click();
        await expect(tag).not.toBeVisible();
    });

    test("should toggle WRIT checkbox", async ({ page }) => {
        const checkbox = page.locator('input[type="checkbox"]');
        const initial = await checkbox.isChecked();

        await checkbox.setChecked(!initial);
        await expect(checkbox).toHaveJSProperty("checked", !initial);
    });
});
