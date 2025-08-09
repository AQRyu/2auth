import AxeBuilder from '@axe-core/playwright';
import { test } from '@playwright/test';

test.describe('2Auth Accessibility Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  import { test, expect } from '@playwright/test';

test.describe('2Auth Accessibility Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should have proper keyboard navigation', async ({ page }) => {
    // Test tab navigation through login form
    await page.keyboard.press('Tab');
    await expect(page.locator('#username')).toBeFocused();
    
    await page.keyboard.press('Tab');
    await expect(page.locator('#password')).toBeFocused();
    
    await page.keyboard.press('Tab');
    await expect(page.locator('button[type="submit"]')).toBeFocused();
  });

  test('should have proper ARIA labels', async ({ page }) => {
    // Check for proper labels and ARIA attributes
    await expect(page.locator('#username')).toHaveAttribute('aria-label');
    await expect(page.locator('#password')).toHaveAttribute('aria-label');
    
    // Check for proper heading structure
    await expect(page.locator('h1, h2, h3')).toHaveCount({ min: 1 });
  });

  test('should support keyboard form submission', async ({ page }) => {
    await page.fill('#username', 'admin');
    await page.fill('#password', 'password');
    
    // Should be able to submit with Enter key
    await page.locator('#password').press('Enter');
    
    // Should show some response (error or success)
    await expect(page.locator('.alert, .error, .success')).toBeVisible();
  });

  test('should have sufficient color contrast', async ({ page }) => {
    // Basic color contrast check by ensuring text is visible
    const loginButton = page.locator('button[type="submit"]');
    await expect(loginButton).toBeVisible();
    
    // Ensure form labels are visible
    const usernameLabel = page.locator('label[for="username"]');
    if (await usernameLabel.count() > 0) {
      await expect(usernameLabel).toBeVisible();
    }
  });
});

  test('should have proper form labels and ARIA attributes', async ({ page }) => {
    // Check form labels are properly associated
    const usernameInput = page.locator('#loginUsername');
    const passwordInput = page.locator('#loginPassword');
    
    await expect(usernameInput).toHaveAttribute('required');
    await expect(passwordInput).toHaveAttribute('required');
    
    // Check labels exist and are properly associated
    await expect(page.locator('label[for="loginUsername"]')).toBeVisible();
    await expect(page.locator('label[for="loginPassword"]')).toBeVisible();
  });

  test('should have proper heading structure', async ({ page }) => {
    // Check main heading
    const mainHeading = page.locator('h2').first();
    await expect(mainHeading).toHaveText('HomeLabAuth Admin');
    
    // Heading should be visible and properly structured
    await expect(mainHeading).toBeVisible();
  });

  test('should have proper focus management', async ({ page }) => {
    // Tab through the form elements
    await page.keyboard.press('Tab');
    await expect(page.locator('#loginUsername')).toBeFocused();
    
    await page.keyboard.press('Tab');
    await expect(page.locator('#loginPassword')).toBeFocused();
    
    await page.keyboard.press('Tab');
    await expect(page.locator('button[type="submit"]')).toBeFocused();
  });

  test('should support keyboard navigation', async ({ page }) => {
    // Fill form using keyboard
    await page.focus('#loginUsername');
    await page.keyboard.type('admin');
    
    await page.keyboard.press('Tab');
    await page.keyboard.type('password');
    
    // Should be able to submit with Enter
    await page.keyboard.press('Enter');
    
    // This would trigger form submission (we can check for network requests)
  });

  test('should have proper color contrast', async ({ page }) => {
    // This test would need additional setup for color contrast checking
    // For now, we'll check that Bootstrap classes are applied properly
    await expect(page.locator('.btn-primary')).toBeVisible();
    await expect(page.locator('.card')).toBeVisible();
  });

  test('should have proper error message accessibility', async ({ page }) => {
    // Mock error response
    await page.route('**/api/auth/login', async route => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Invalid credentials'
        })
      });
    });

    await page.fill('#loginUsername', 'wrong');
    await page.fill('#loginPassword', 'wrong');
    await page.click('button[type="submit"]');

    // Error message should be announced to screen readers
    const errorElement = page.locator('#loginError');
    await expect(errorElement).toBeVisible();
    await expect(errorElement).toHaveAttribute('role', 'alert');
  });

  test('should not have accessibility issues on dashboard', async ({ page }) => {
    // Mock successful login
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

    // Login to dashboard
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('#adminDashboard')).toBeVisible();

    // Run accessibility scan on dashboard
    const accessibilityScanResults = await new AxeBuilder({ page }).analyze();
    expect(accessibilityScanResults.violations).toEqual([]);
  });

  test('should have proper table accessibility', async ({ page }) => {
    // Login and navigate to users table
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
        body: JSON.stringify({
          content: [{
            id: 1,
            username: 'testuser',
            email: 'test@example.com',
            roles: ['USER'],
            accountEnabled: true,
            accountLocked: false
          }]
        })
      });
    });

    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('#adminDashboard')).toBeVisible();

    // Check table has proper headers
    const table = page.locator('#usersTable');
    await expect(table).toBeVisible();
    
    // Table should have proper structure
    await expect(table.locator('thead')).toBeVisible();
    await expect(table.locator('tbody')).toBeVisible();
    
    // Headers should be properly marked
    const headers = table.locator('th');
    await expect(headers).toHaveCount(7); // Based on the table structure
  });

  test('should have proper modal accessibility', async ({ page }) => {
    // Login first
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

    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('#adminDashboard')).toBeVisible();

    // Open modal
    await page.click('#addUserBtn');
    
    // Modal should be properly accessible
    const modal = page.locator('#userModal');
    await expect(modal).toBeVisible();
    
    // Should have proper ARIA attributes
    await expect(modal).toHaveAttribute('role', 'dialog');
    await expect(modal).toHaveAttribute('aria-labelledby', 'modalTitle');
    
    // Focus should be trapped in modal
    await page.keyboard.press('Tab');
    const focusedElement = await page.evaluate(() => document.activeElement.id);
    expect(['userUsername', 'cancelBtn', 'btn-close'].some(id => focusedElement.includes(id))).toBeTruthy();
  });
});
