import { expect, test } from '@playwright/test';

test.describe('2Auth Responsive Design', () => {
  test.beforeEach(async ({ page }) => {
    // Mock login and users API
    await page.route('**/api/auth/login', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          accessToken: 'mock-token',
          user: { id: 1, username: 'admin', email: 'admin@example.com' },
          requireTotp: false
        })
      });
    });

    await page.route('**/api/admin/users', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [] })
      });
    });

    await page.goto('/');
  });

  test('should be responsive on mobile devices', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });

    // Check login form is properly sized on mobile
    await expect(page.locator('#loginForm .card')).toBeVisible();
    
    // Login to dashboard
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('#adminDashboard')).toBeVisible();

    // Check header is responsive
    await expect(page.locator('header.dashboard-header')).toBeVisible();
    
    // Check navigation tabs are visible and clickable
    await expect(page.locator('#usersTabBtn')).toBeVisible();
    await expect(page.locator('#profileTabBtn')).toBeVisible();

    // Check statistics cards stack properly on mobile
    const statCards = page.locator('.stat-card');
    await expect(statCards).toHaveCount(4);
    
    // On mobile, some table columns should be hidden
    const emailColumn = page.locator('th:has-text("Email")');
    const nameColumn = page.locator('th:has-text("Name")');
    const lastLoginColumn = page.locator('th:has-text("Last Login")');
    
    // These columns should be hidden on small screens (d-none d-sm-table-cell, etc.)
    await expect(emailColumn).toHaveClass(/d-none/);
    await expect(nameColumn).toHaveClass(/d-none/);
    await expect(lastLoginColumn).toHaveClass(/d-none/);
  });

  test('should work on tablet viewport', async ({ page }) => {
    // Set tablet viewport
    await page.setViewportSize({ width: 768, height: 1024 });

    // Login to dashboard
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('#adminDashboard')).toBeVisible();

    // Check that more columns are visible on tablet
    const emailColumn = page.locator('th:has-text("Email")');
    await expect(emailColumn).toBeVisible();
  });

  test('should work on desktop viewport', async ({ page }) => {
    // Set desktop viewport
    await page.setViewportSize({ width: 1920, height: 1080 });

    // Login to dashboard
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('#adminDashboard')).toBeVisible();

    // All columns should be visible on desktop
    const columns = ['Username', 'Email', 'Name', 'Role', 'Status', 'Last Login', 'Actions'];
    for (const column of columns) {
      await expect(page.locator(`th:has-text("${column}")`)).toBeVisible();
    }
  });
});
