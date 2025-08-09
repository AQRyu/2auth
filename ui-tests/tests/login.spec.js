import { expect, test } from '@playwright/test';

test.describe('2Auth Login Page', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the login page
    await page.goto('/');
  });

  test('should display login form elements', async ({ page }) => {
    // Check page title
    await expect(page).toHaveTitle(/HomeLabAuth - Admin Dashboard/);
    
    // Check login form elements are visible
    await expect(page.locator('#loginForm')).toBeVisible();
    await expect(page.locator('#loginUsername')).toBeVisible();
    await expect(page.locator('#loginPassword')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
    
    // Check form labels
    await expect(page.locator('label[for="loginUsername"]')).toHaveText('Username');
    await expect(page.locator('label[for="loginPassword"]')).toHaveText('Password');
    
    // Check button text
    await expect(page.locator('button[type="submit"]')).toHaveText('Login');
  });

  test('should show validation for empty fields', async ({ page }) => {
    // Click login button without filling fields
    await page.click('button[type="submit"]');
    
    // HTML5 validation should prevent submission
    const usernameField = page.locator('#loginUsername');
    const passwordField = page.locator('#loginPassword');
    
    await expect(usernameField).toHaveAttribute('required');
    await expect(passwordField).toHaveAttribute('required');
  });

  test('should handle login attempt with invalid credentials', async ({ page }) => {
    // Mock the API response for invalid login
    await page.route('**/api/auth/login', async route => {
      await route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({
          message: 'Invalid username or password'
        })
      });
    });

    // Fill login form with invalid credentials
    await page.fill('#loginUsername', 'wronguser');
    await page.fill('#loginPassword', 'wrongpassword');
    await page.click('button[type="submit"]');

    // Check error message appears
    await expect(page.locator('#loginError')).toBeVisible();
    await expect(page.locator('#loginError')).toContainText('Invalid username or password');
  });

  test('should handle successful login without TOTP', async ({ page }) => {
    // Mock successful login response
    await page.route('**/api/auth/login', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          accessToken: 'mock-token',
          user: {
            id: 1,
            username: 'admin',
            email: 'admin@example.com'
          },
          requireTotp: false
        })
      });
    });

    // Mock users endpoint for dashboard
    await page.route('**/api/admin/users', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: []
        })
      });
    });

    // Fill and submit login form
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');

    // Should navigate to dashboard
    await expect(page.locator('#adminDashboard')).toBeVisible();
    await expect(page.locator('#loginForm')).not.toBeVisible();
    await expect(page.locator('#currentUser')).toContainText('admin');
  });

  test('should handle login requiring TOTP', async ({ page }) => {
    // Mock login response requiring TOTP
    await page.route('**/api/auth/login', async route => {
      const requestBody = await route.request().postDataJSON();
      
      if (!requestBody.totpCode) {
        // First request without TOTP
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            requireTotp: true
          })
        });
      } else {
        // Second request with TOTP
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            accessToken: 'mock-token',
            user: {
              id: 1,
              username: 'admin',
              email: 'admin@example.com'
            },
            requireTotp: false
          })
        });
      }
    });

    // Mock users endpoint
    await page.route('**/api/admin/users', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [] })
      });
    });

    // Fill and submit login form
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');

    // Should show TOTP form
    await expect(page.locator('#totpForm')).toBeVisible();
    await expect(page.locator('#loginForm')).not.toBeVisible();
    await expect(page.locator('#totpCode')).toBeVisible();

    // Fill TOTP code and submit
    await page.fill('#totpCode', '123456');
    await page.click('#totpForm button[type="submit"]');

    // Should navigate to dashboard
    await expect(page.locator('#adminDashboard')).toBeVisible();
    await expect(page.locator('#totpForm')).not.toBeVisible();
  });

  test('should handle invalid TOTP code', async ({ page }) => {
    // Mock login requiring TOTP first
    await page.route('**/api/auth/login', async route => {
      const requestBody = await route.request().postDataJSON();
      
      if (!requestBody.totpCode) {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ requireTotp: true })
        });
      } else {
        await route.fulfill({
          status: 400,
          contentType: 'application/json',
          body: JSON.stringify({
            message: 'Invalid TOTP code'
          })
        });
      }
    });

    // Login to get to TOTP form
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('#totpForm')).toBeVisible();

    // Enter invalid TOTP code
    await page.fill('#totpCode', '000000');
    await page.click('#totpForm button[type="submit"]');

    // Should show error message
    await expect(page.locator('#totpError')).toBeVisible();
    await expect(page.locator('#totpError')).toContainText('Invalid TOTP code');
  });

  test('should allow going back to login from TOTP form', async ({ page }) => {
    // Mock login requiring TOTP
    await page.route('**/api/auth/login', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ requireTotp: true })
      });
    });

    // Get to TOTP form
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('#totpForm')).toBeVisible();

    // Click back button
    await page.click('#backToLogin');

    // Should return to login form
    await expect(page.locator('#loginForm')).toBeVisible();
    await expect(page.locator('#totpForm')).not.toBeVisible();
  });
});
