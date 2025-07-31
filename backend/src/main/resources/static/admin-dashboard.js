// Global variables
let authToken = null;
let currentUser = null;

// DOM elements
const loginForm = document.getElementById('loginForm');
const adminDashboard = document.getElementById('adminDashboard');
const authFormEl = document.getElementById('authForm');
const userModal = document.getElementById('userModal');
const userForm = document.getElementById('userForm');

// Base API URL
const API_BASE = '/auth/api';

// Authentication functions
async function login(event) {
    event.preventDefault();

    const username = document.getElementById('loginUsername').value;
    const password = document.getElementById('loginPassword').value;
    const totpCode = document.getElementById('loginTotp').value;

    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                usernameOrEmail: username,
                password: password,
                totpCode: totpCode || null
            })
        });

        if (!response.ok) {
            const error = await response.json();
            showError('loginError', error.message || 'Login failed');
            return;
        }

        const result = await response.json();
        authToken = result.accessToken;
        currentUser = result.user;

        // Show dashboard
        loginForm.style.display = 'none';
        adminDashboard.style.display = 'block';
        document.getElementById('currentUser').textContent = currentUser.username;

        // Load dashboard data
        loadDashboard();

    } catch (error) {
        showError('loginError', 'Network error: ' + error.message);
    }
}

function logout() {
    authToken = null;
    currentUser = null;
    loginForm.style.display = 'block';
    adminDashboard.style.display = 'none';
    document.getElementById('authForm').reset();
    hideError('loginError');
}

// Dashboard functions
async function loadDashboard() {
    await loadUsers();
    await loadStats();
}

async function loadUsers() {
    const loading = document.getElementById('usersLoading');
    const error = document.getElementById('usersError');
    const tableBody = document.getElementById('usersTableBody');

    loading.style.display = 'block';
    hideError('usersError');

    try {
        const response = await fetch(`${API_BASE}/admin/users?size=100`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load users');
        }

        const result = await response.json();
        const users = result.content || [];

        // Clear table
        tableBody.innerHTML = '';

        // Populate table
        users.forEach(user => {
            const row = createUserRow(user);
            tableBody.appendChild(row);
        });

    } catch (error) {
        showError('usersError', 'Failed to load users: ' + error.message);
    } finally {
        loading.style.display = 'none';
    }
}

async function loadStats() {
    try {
        const response = await fetch(`${API_BASE}/admin/users?size=1000`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (!response.ok) return;

        const result = await response.json();
        const users = result.content || [];

        const totalUsers = users.length;
        const activeUsers = users.filter(u => !u.accountLocked && u.accountEnabled).length;
        const lockedUsers = users.filter(u => u.accountLocked).length;
        const adminUsers = users.filter(u => u.roles && u.roles.includes('ADMIN')).length;

        document.getElementById('totalUsers').textContent = totalUsers;
        document.getElementById('activeUsers').textContent = activeUsers;
        document.getElementById('lockedUsers').textContent = lockedUsers;
        document.getElementById('adminUsers').textContent = adminUsers;

    } catch (error) {
        console.error('Failed to load stats:', error);
    }
}

function createUserRow(user) {
    const row = document.createElement('tr');

    let statusClass, statusText;

    if (user.accountLocked) {
        statusClass = 'status-locked';
        statusText = 'Locked';
    } else if (user.accountEnabled) {
        statusClass = 'status-active';
        statusText = 'Active';
    } else {
        statusClass = 'status-disabled';
        statusText = 'Disabled';
    }

    const lastLogin = user.lastLogin ? new Date(user.lastLogin).toLocaleDateString() : 'Never';
    const roles = user.roles ? user.roles.join(', ') : 'USER';

    row.innerHTML = `
        <td>${user.username}</td>
        <td>${user.email}</td>
        <td>${user.firstName || ''} ${user.lastName || ''}</td>
        <td>${roles}</td>
        <td><span class="status-badge ${statusClass}">${statusText}</span></td>
        <td>${lastLogin}</td>
        <td>
            <button onclick="editUser(${user.id})" class="btn btn-secondary" style="margin-right: 5px;">Edit</button>
            <button onclick="deleteUser(${user.id})" class="btn btn-danger">Delete</button>
        </td>
    `;

    return row;
}

// User management functions
function showAddUserModal() {
    document.getElementById('modalTitle').textContent = 'Add New User';
    document.getElementById('userId').value = '';
    document.getElementById('userForm').reset();
    hideError('modalError');
    hideSuccess('modalSuccess');
    userModal.style.display = 'block';
}

async function editUser(userId) {
    try {
        const response = await fetch(`${API_BASE}/admin/users/${userId}`, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load user data');
        }

        const user = await response.json();

        document.getElementById('modalTitle').textContent = 'Edit User';
        document.getElementById('userId').value = user.id;
        document.getElementById('userUsername').value = user.username;
        document.getElementById('userEmail').value = user.email;
        document.getElementById('userFirstName').value = user.firstName || '';
        document.getElementById('userLastName').value = user.lastName || '';
        document.getElementById('userPassword').value = '';
        document.getElementById('userPassword').placeholder = 'Leave blank to keep current password';
        document.getElementById('userPassword').required = false;

        // Set role
        const roleSelect = document.getElementById('userRole');
        if (user.roles?.includes('ADMIN')) {
            roleSelect.value = 'ADMIN';
        } else {
            roleSelect.value = 'USER';
        }

        hideError('modalError');
        hideSuccess('modalSuccess');
        userModal.style.display = 'block';

    } catch (error) {
        alert('Failed to load user data: ' + error.message);
    }
}

async function saveUser() {
    const userId = document.getElementById('userId').value;
    const username = document.getElementById('userUsername').value;
    const email = document.getElementById('userEmail').value;
    const firstName = document.getElementById('userFirstName').value;
    const lastName = document.getElementById('userLastName').value;
    const password = document.getElementById('userPassword').value;
    const role = document.getElementById('userRole').value;

    const userData = {
        username,
        email,
        firstName,
        lastName,
        roles: [role]
    };

    if (password) {
        userData.password = password;
    }

    try {
        let response;

        if (userId) {
            // Update existing user
            response = await fetch(`${API_BASE}/admin/users/${userId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${authToken}`
                },
                body: JSON.stringify(userData)
            });
        } else {
            // Create new user
            if (!password) {
                showError('modalError', 'Password is required for new users');
                return;
            }

            response = await fetch(`${API_BASE}/admin/users`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${authToken}`
                },
                body: JSON.stringify(userData)
            });
        }

        if (!response.ok) {
            const error = await response.json();
            showError('modalError', error.message || 'Failed to save user');
            return;
        }

        showSuccess('modalSuccess', 'User saved successfully');
        setTimeout(() => {
            closeModal();
            loadUsers();
        }, 1000);

    } catch (error) {
        showError('modalError', 'Network error: ' + error.message);
    }
}

async function deleteUser(userId) {
    if (!confirm('Are you sure you want to delete this user?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/admin/users/${userId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (!response.ok) {
            const error = await response.json();
            alert('Failed to delete user: ' + (error.message || 'Unknown error'));
            return;
        }

        loadUsers();

    } catch (error) {
        alert('Network error: ' + error.message);
    }
}

function closeModal() {
    userModal.style.display = 'none';
    document.getElementById('userForm').reset();
    document.getElementById('userPassword').required = true;
    document.getElementById('userPassword').placeholder = '';
}

// Utility functions
function showMessage(elementId, message, isSuccess = false) {
    const element = document.getElementById(elementId);
    element.textContent = message;
    element.style.display = 'block';
    // You could add different styling based on isSuccess if needed
}

function showError(elementId, message) {
    showMessage(elementId, message, false);
}

function hideError(elementId) {
    const element = document.getElementById(elementId);
    element.style.display = 'none';
}

function showSuccess(elementId, message) {
    showMessage(elementId, message, true);
}

function hideSuccess(elementId) {
    const element = document.getElementById(elementId);
    element.style.display = 'none';
}

// Event listeners
document.addEventListener('DOMContentLoaded', function () {
    authFormEl.addEventListener('submit', login);
    document.getElementById('logoutBtn').addEventListener('click', logout);
    document.getElementById('addUserBtn').addEventListener('click', showAddUserModal);
    document.getElementById('saveUserBtn').addEventListener('click', saveUser);
    document.getElementById('cancelBtn').addEventListener('click', closeModal);

    // Close modal when clicking outside
    userModal.addEventListener('click', function (event) {
        if (event.target === userModal) {
            closeModal();
        }
    });
});
