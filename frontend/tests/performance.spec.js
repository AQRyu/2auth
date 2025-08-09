import { expect, test } from '@playwright/test';

test.describe('2Auth Performance Tests', () => {
  test('should load page within acceptable time', async ({ page }) => {
    const startTime = Date.now();
    
    await page.goto('/');
    
    // Wait for page to be fully loaded
    await page.waitForLoadState('networkidle');
    
    const loadTime = Date.now() - startTime;
    
    // Page should load within 3 seconds
    expect(loadTime).toBeLessThan(3000);
    
    // Check that essential elements are visible
    await expect(page.locator('#loginForm')).toBeVisible();
    await expect(page.locator('#loginUsername')).toBeVisible();
    await expect(page.locator('#loginPassword')).toBeVisible();
  });

  test('should have efficient resource loading', async ({ page }) => {
    const responses = [];
    
    // Monitor network requests
    page.on('response', response => {
      responses.push(response);
    });
    
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    
    // Check that we're loading expected resources
    const htmlResponse = responses.find(r => r.url().includes('index.html') || r.url().endsWith('/'));
    const jsResponse = responses.find(r => r.url().includes('admin-dashboard.js'));
    const cssResponse = responses.find(r => r.url().includes('bootstrap') && r.url().includes('.css'));
    
    // HTML should load successfully
    expect(htmlResponse?.status()).toBe(200);
    
    // Check that Bootstrap CSS loads (from CDN)
    expect(cssResponse?.status()).toBe(200);
    
    // Check response times
    if (jsResponse) {
      expect(jsResponse.status()).toBe(200);
    }
  });

  test('should handle multiple rapid interactions', async ({ page }) => {
    await page.goto('/');
    
    const startTime = Date.now();
    
    // Rapidly interact with form elements
    for (let i = 0; i < 10; i++) {
      await page.fill('#loginUsername', `user${i}`);
      await page.fill('#loginPassword', `pass${i}`);
      await page.locator('#loginUsername').clear();
      await page.locator('#loginPassword').clear();
    }
    
    const interactionTime = Date.now() - startTime;
    
    // Multiple interactions should complete within reasonable time
    expect(interactionTime).toBeLessThan(5000);
    
    // Form should still be functional
    await page.fill('#loginUsername', 'finaluser');
    await page.fill('#loginPassword', 'finalpass');
    
    await expect(page.locator('#loginUsername')).toHaveValue('finaluser');
    await expect(page.locator('#loginPassword')).toHaveValue('finalpass');
  });

  test('should handle API response delays gracefully', async ({ page }) => {
    // Mock slow API response
    await page.route('**/api/auth/login', async route => {
      // Simulate slow network
      await new Promise(resolve => setTimeout(resolve, 2000));
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          accessToken: 'mock-token',
          user: { id: 1, username: 'admin' },
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
    
    const startTime = Date.now();
    
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    // Should eventually navigate to dashboard
    await expect(page.locator('#adminDashboard')).toBeVisible({ timeout: 10000 });
    
    const totalTime = Date.now() - startTime;
    
    // Should handle the delay and still work
    expect(totalTime).toBeGreaterThan(2000);
    expect(totalTime).toBeLessThan(10000);
  });

  test('should be responsive to user input during loading', async ({ page }) => {
    // Mock slow users API
    await page.route('**/api/admin/users', async route => {
      await new Promise(resolve => setTimeout(resolve, 1000));
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [] })
      });
    });

    await page.route('**/api/auth/login', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          accessToken: 'mock-token',
          user: { id: 1, username: 'admin' },
          requireTotp: false
        })
      });
    });

    await page.goto('/');
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('#adminDashboard')).toBeVisible();
    
    // Should show loading indicator while API is slow
    await expect(page.locator('#usersLoading')).toBeVisible();
    
    // UI should still be responsive (can click tabs)
    await page.click('#profileTabBtn');
    await expect(page.locator('#profileTab')).toHaveClass(/active/);
    
    // Switch back to users tab
    await page.click('#usersTabBtn');
    await expect(page.locator('#usersTab')).toHaveClass(/active/);
  });

  test('should handle large datasets efficiently', async ({ page }) => {
    // Mock large user dataset
    const largeUserList = Array.from({ length: 100 }, (_, i) => ({
      id: i + 1,
      username: `user${i + 1}`,
      email: `user${i + 1}@example.com`,
      firstName: `User`,
      lastName: `${i + 1}`,
      roles: ['USER'],
      accountEnabled: true,
      accountLocked: false,
      lastLogin: '2023-12-01T10:00:00Z'
    }));

    await page.route('**/api/auth/login', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          accessToken: 'mock-token',
          user: { id: 1, username: 'admin' },
          requireTotp: false
        })
      });
    });

    await page.route('**/api/admin/users', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: largeUserList })
      });
    });

    await page.goto('/');
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('#adminDashboard')).toBeVisible();
    
    const startTime = Date.now();
    
    // Wait for all users to be rendered
    await expect(page.locator('#usersTableBody tr')).toHaveCount(100, { timeout: 10000 });
    
    const renderTime = Date.now() - startTime;
    
    // Should render 100 users within reasonable time
    expect(renderTime).toBeLessThan(5000);
    
    // Check that statistics are calculated correctly
    await expect(page.locator('#totalUsers')).toHaveText('100');
    await expect(page.locator('#activeUsers')).toHaveText('100');
  });

  test('should maintain performance with repeated operations', async ({ page }) => {
    await page.route('**/api/auth/login', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          accessToken: 'mock-token',
          user: { id: 1, username: 'admin' },
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

    await page.route('**/api/user/profile', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          username: 'admin',
          email: 'admin@example.com',
          role: 'ADMIN',
          totpEnabled: false
        })
      });
    });

    await page.goto('/');
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('#adminDashboard')).toBeVisible();
    
    const startTime = Date.now();
    
    // Switch between tabs multiple times
    for (let i = 0; i < 10; i++) {
      await page.click('#profileTabBtn');
      await expect(page.locator('#profileTab')).toHaveClass(/active/);
      
      await page.click('#usersTabBtn');
      await expect(page.locator('#usersTab')).toHaveClass(/active/);
    }
    
    const operationTime = Date.now() - startTime;
    
    // Multiple tab switches should complete quickly
    expect(operationTime).toBeLessThan(3000);
  });
});
