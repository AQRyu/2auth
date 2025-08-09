import { expect, test } from '@playwright/test';

test.describe('2Auth Admin Dashboard', () => {
  // Helper function to login and get to dashboard
  async function loginToAdmin(page) {
    await page.route('**/api/auth/login', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          accessToken: 'mock-token',
          user: {
            id: 1,
            username: 'admin',
            email: 'admin@example.com',
            role: 'ADMIN'
          },
          requireTotp: false
        })
      });
    });

    await page.goto('/');
    await page.fill('#loginUsername', 'admin');
    await page.fill('#loginPassword', 'password');
    await page.click('button[type="submit"]');
    await expect(page.locator('#adminDashboard')).toBeVisible();
  }

  test('should display dashboard after login', async ({ page }) => {
    await loginToAdmin(page);

    // Check dashboard elements
    await expect(page.locator('header.dashboard-header')).toBeVisible();
    await expect(page.locator('#currentUser')).toHaveText('admin');
    await expect(page.locator('#logoutBtn')).toBeVisible();
    
    // Check navigation tabs
    await expect(page.locator('#usersTabBtn')).toBeVisible();
    await expect(page.locator('#profileTabBtn')).toBeVisible();
    
    // Check users tab is active by default
    await expect(page.locator('#usersTabBtn')).toHaveClass(/active/);
    await expect(page.locator('#usersTab')).toHaveClass(/active/);
  });

  test('should display statistics cards', async ({ page }) => {
    await page.route('**/api/admin/users', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 1,
              username: 'admin',
              email: 'admin@example.com',
              roles: ['ADMIN'],
              accountEnabled: true,
              accountLocked: false
            },
            {
              id: 2,
              username: 'user1',
              email: 'user1@example.com',
              roles: ['USER'],
              accountEnabled: true,
              accountLocked: false
            },
            {
              id: 3,
              username: 'locked_user',
              email: 'locked@example.com',
              roles: ['USER'],
              accountEnabled: true,
              accountLocked: true
            }
          ]
        })
      });
    });

    await loginToAdmin(page);

    // Check statistics cards are visible
    await expect(page.locator('#totalUsers')).toBeVisible();
    await expect(page.locator('#activeUsers')).toBeVisible();
    await expect(page.locator('#lockedUsers')).toBeVisible();
    await expect(page.locator('#adminUsers')).toBeVisible();

    // Wait for stats to load and check values
    await page.waitForTimeout(1000);
    await expect(page.locator('#totalUsers')).toHaveText('3');
    await expect(page.locator('#activeUsers')).toHaveText('2');
    await expect(page.locator('#lockedUsers')).toHaveText('1');
    await expect(page.locator('#adminUsers')).toHaveText('1');
  });

  test('should display users table', async ({ page }) => {
    await page.route('**/api/admin/users', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 1,
              username: 'admin',
              email: 'admin@example.com',
              firstName: 'Admin',
              lastName: 'User',
              roles: ['ADMIN'],
              accountEnabled: true,
              accountLocked: false,
              lastLogin: '2023-12-01T10:00:00Z'
            },
            {
              id: 2,
              username: 'testuser',
              email: 'test@example.com',
              firstName: 'Test',
              lastName: 'User',
              roles: ['USER'],
              accountEnabled: true,
              accountLocked: false,
              lastLogin: null
            }
          ]
        })
      });
    });

    await loginToAdmin(page);

    // Check table structure
    await expect(page.locator('#usersTable')).toBeVisible();
    await expect(page.locator('#usersTableBody')).toBeVisible();

    // Check table headers
    const headers = ['Username', 'Email', 'Name', 'Role', 'Status', 'Last Login', 'Actions'];
    for (const header of headers) {
      await expect(page.locator('th').filter({ hasText: header })).toBeVisible();
    }

    // Wait for users to load
    await page.waitForTimeout(1000);

    // Check user rows
    await expect(page.locator('#usersTableBody tr')).toHaveCount(2);
    
    // Check first user (admin)
    const adminRow = page.locator('#usersTableBody tr').first();
    await expect(adminRow.locator('td').first()).toContainText('admin');
    await expect(adminRow.locator('.badge').filter({ hasText: 'ADMIN' })).toBeVisible();
    await expect(adminRow.locator('.badge').filter({ hasText: 'Active' })).toBeVisible();

    // Check action buttons
    await expect(adminRow.locator('button[title="Edit User"]')).toBeVisible();
    await expect(adminRow.locator('button[title="Delete User"]')).toBeVisible();
  });

  test('should open add user modal', async ({ page }) => {
    await page.route('**/api/admin/users', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [] })
      });
    });

    await loginToAdmin(page);

    // Click add user button
    await page.click('#addUserBtn');

    // Check modal is visible
    await expect(page.locator('#userModal')).toBeVisible();
    await expect(page.locator('#modalTitle')).toHaveText('Add New User');
    await expect(page.locator('#saveUserBtn')).toHaveText('Save User');

    // Check form fields
    await expect(page.locator('#userUsername')).toBeVisible();
    await expect(page.locator('#userEmail')).toBeVisible();
    await expect(page.locator('#userFirstName')).toBeVisible();
    await expect(page.locator('#userLastName')).toBeVisible();
    await expect(page.locator('#userPassword')).toBeVisible();
    await expect(page.locator('#userRole')).toBeVisible();
  });

  test('should create new user', async ({ page }) => {
    await page.route('**/api/admin/users', async route => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ content: [] })
        });
      } else if (route.request().method() === 'POST') {
        const userData = await route.request().postDataJSON();
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            id: 3,
            ...userData
          })
        });
      }
    });

    await loginToAdmin(page);

    // Open add user modal
    await page.click('#addUserBtn');
    await expect(page.locator('#userModal')).toBeVisible();

    // Fill form
    await page.fill('#userUsername', 'newuser');
    await page.fill('#userEmail', 'newuser@example.com');
    await page.fill('#userFirstName', 'New');
    await page.fill('#userLastName', 'User');
    await page.fill('#userPassword', 'password123');
    await page.selectOption('#userRole', 'USER');

    // Submit form
    await page.click('#saveUserBtn');

    // Check success message
    await expect(page.locator('#modalSuccess')).toBeVisible();
    await expect(page.locator('#modalSuccess')).toContainText('User created successfully');
  });

  test('should handle user creation validation errors', async ({ page }) => {
    await loginToAdmin(page);

    // Open add user modal
    await page.click('#addUserBtn');
    await expect(page.locator('#userModal')).toBeVisible();

    // Try to submit without filling required fields
    await page.click('#saveUserBtn');

    // Should show validation error
    await expect(page.locator('#modalError')).toBeVisible();
    await expect(page.locator('#modalError')).toContainText('Password is required for new users');
  });

  test('should switch to profile tab', async ({ page }) => {
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
          firstName: 'Admin',
          lastName: 'User',
          role: 'ADMIN',
          totpEnabled: false
        })
      });
    });

    await loginToAdmin(page);

    // Click profile tab
    await page.click('#profileTabBtn');

    // Check profile tab is active
    await expect(page.locator('#profileTabBtn')).toHaveClass(/active/);
    await expect(page.locator('#profileTab')).toHaveClass(/active/);

    // Check profile information
    await expect(page.locator('#profileUsername')).toHaveText('admin');
    await expect(page.locator('#profileEmail')).toHaveText('admin@example.com');
    await expect(page.locator('#profileRole')).toHaveText('ADMIN');

    // Check TOTP section
    await expect(page.locator('#totpDisabled')).toBeVisible();
    await expect(page.locator('#enableTotpBtn')).toBeVisible();
  });

  test('should logout successfully', async ({ page }) => {
    await page.route('**/api/admin/users', async route => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [] })
      });
    });

    await loginToAdmin(page);

    // Click logout button
    await page.click('#logoutBtn');

    // Should return to login form
    await expect(page.locator('#loginForm')).toBeVisible();
    await expect(page.locator('#adminDashboard')).not.toBeVisible();

    // Fields should be cleared
    await expect(page.locator('#loginUsername')).toHaveValue('');
    await expect(page.locator('#loginPassword')).toHaveValue('');
  });
});
