// Global variables
let authToken = null;
let currentUser = null;
let pendingLoginData = null;

// Base API URL
const API_BASE = '/api';

// jQuery ready function
$(document).ready(function() {
    initializeEventHandlers();
    checkAuthentication();
});

// Initialize all event handlers
function initializeEventHandlers() {
    // Login form handlers
    $('#authForm').on('submit', handleLogin);
    $('#totpAuthForm').on('submit', handleTotpVerification);
    $('#backToLogin').on('click', backToLogin);
    $('#logoutBtn').on('click', logout);

    // User management handlers
    $('#addUserBtn').on('click', () => showUserModal());
    $('#saveUserBtn').on('click', saveUser);
    $('#userForm').on('submit', (e) => { e.preventDefault(); saveUser(); });

    // Profile management handlers
    $('#enableTotpBtn').on('click', enableTotp);
    $('#disableTotpBtn').on('click', disableTotp);
    $('#confirmTotpBtn').on('click', confirmTotp);
    $('#cancelTotpBtn').on('click', cancelTotpSetup);
    $('#changePasswordForm').on('submit', changePassword);

    // Bootstrap tab events
    $('button[data-bs-toggle="tab"]').on('shown.bs.tab', function (e) {
        const target = $(e.target).data('bs-target');
        if (target === '#usersTab') {
            loadUsers();
        } else if (target === '#profileTab') {
            loadProfile();
        }
    });

    // Modal events
    $('#userModal').on('hidden.bs.modal', function () {
        clearUserForm();
    });
}

// Authentication functions
async function handleLogin(event) {
    event.preventDefault();
    
    const username = $('#loginUsername').val();
    const password = $('#loginPassword').val();

    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                usernameOrEmail: username,
                password: password,
                totpCode: null
            })
        });

        const result = await response.json();

        if (!response.ok) {
            showError('loginError', result.message || 'Login failed');
            return;
        }

        if (result.requireTotp) {
            pendingLoginData = { username, password };
            $('#loginForm').addClass('d-none');
            $('#totpForm').removeClass('d-none');
            hideError('loginError');
            $('#totpCode').focus();
        } else {
            authToken = result.accessToken;
            currentUser = result.user;
            showDashboard();
        }
    } catch (error) {
        showError('loginError', 'Network error. Please try again.');
        console.error('Login error:', error);
    }
}

async function handleTotpVerification(event) {
    event.preventDefault();

    if (!pendingLoginData) {
        showError('totpError', 'Session expired. Please login again.');
        backToLogin();
        return;
    }

    const totpCode = $('#totpCode').val();

    try {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                usernameOrEmail: pendingLoginData.username,
                password: pendingLoginData.password,
                totpCode: totpCode
            })
        });

        const result = await response.json();

        if (!response.ok) {
            showError('totpError', result.message || 'Invalid TOTP code');
            return;
        }

        authToken = result.accessToken;
        currentUser = result.user;
        pendingLoginData = null;
        showDashboard();
    } catch (error) {
        showError('totpError', 'Network error. Please try again.');
        console.error('TOTP verification error:', error);
    }
}

function backToLogin() {
    $('#totpForm').addClass('d-none');
    $('#loginForm').removeClass('d-none');
    $('#totpCode').val('');
    hideError('totpError');
    pendingLoginData = null;
}

function logout() {
    authToken = null;
    currentUser = null;
    pendingLoginData = null;
    
    $('#adminDashboard').addClass('d-none');
    $('#loginForm').removeClass('d-none');
    $('#totpForm').addClass('d-none');
    
    // Clear forms
    $('#loginUsername, #loginPassword, #totpCode').val('');
    hideError('loginError');
    hideError('totpError');
}

function showDashboard() {
    $('#loginForm, #totpForm').addClass('d-none');
    $('#adminDashboard').removeClass('d-none');
    $('#currentUser').text(currentUser.username);
    
    loadUsers();
    loadStats();
}

function checkAuthentication() {
    // In a real app, you might check localStorage or validate an existing token
    // For now, just show the login form
}

// User Management Functions
async function loadUsers() {
    if (!authToken) return;

    try {
        $('#usersLoading').removeClass('d-none');
        hideError('usersError');

        const response = await fetch(`${API_BASE}/admin/users`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });

        if (!response.ok) {
            throw new Error('Failed to load users');
        }

        const users = await response.json();
        displayUsers(users);
    } catch (error) {
        showError('usersError', 'Failed to load users: ' + error.message);
        console.error('Error loading users:', error);
    } finally {
        $('#usersLoading').addClass('d-none');
    }
}

function displayUsers(users) {
    const tbody = $('#usersTableBody');
    tbody.empty();

    users.forEach(user => {
        const statusBadge = getStatusBadge(user.status);
        const lastLogin = user.lastLogin ? new Date(user.lastLogin).toLocaleDateString() : 'Never';
        const fullName = `${user.firstName || ''} ${user.lastName || ''}`.trim() || 'Not set';
        
        const row = $(`
            <tr>
                <td>
                    <div class="fw-bold">${escapeHtml(user.username)}</div>
                    <div class="text-muted small d-sm-none">${escapeHtml(user.email)}</div>
                </td>
                <td class="d-none d-sm-table-cell">${escapeHtml(user.email)}</td>
                <td class="d-none d-md-table-cell">${escapeHtml(fullName)}</td>
                <td><span class="badge bg-${user.role === 'ADMIN' ? 'primary' : 'secondary'}">${user.role}</span></td>
                <td>${statusBadge}</td>
                <td class="d-none d-lg-table-cell">${lastLogin}</td>
                <td>
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary btn-sm" onclick="editUser('${user.id}')" title="Edit User">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-outline-danger btn-sm" onclick="deleteUser('${user.id}', '${escapeHtml(user.username)}')" title="Delete User">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `);
        tbody.append(row);
    });
}

function getStatusBadge(status) {
    const statusMap = {
        'ACTIVE': '<span class="badge bg-success">Active</span>',
        'LOCKED': '<span class="badge bg-danger">Locked</span>',
        'DISABLED': '<span class="badge bg-warning">Disabled</span>'
    };
    return statusMap[status] || '<span class="badge bg-secondary">Unknown</span>';
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
        const adminUsers = users.filter(u => u.roles?.includes('ADMIN')).length;

        document.getElementById('totalUsers').textContent = totalUsers;
        document.getElementById('activeUsers').textContent = activeUsers;
        document.getElementById('lockedUsers').textContent = lockedUsers;
        document.getElementById('adminUsers').textContent = adminUsers;

    } catch (error) {
        console.error('Failed to load stats:', error);
    }
}

function showUserModal(userId = null) {
    clearUserForm();
    hideError('modalError');
    hideSuccess('modalSuccess');
    
    if (userId) {
        $('#modalTitle').text('Edit User');
        loadUserForEdit(userId);
    } else {
        $('#modalTitle').text('Add New User');
    }
    
    const modal = new bootstrap.Modal('#userModal');
    modal.show();
}

function clearUserForm() {
    $('#userForm')[0].reset();
    $('#userId').val('');
}

async function loadUserForEdit(userId) {
    try {
        const response = await fetch(`${API_BASE}/admin/users/${userId}`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });

        if (response.ok) {
            const user = await response.json();
            $('#userId').val(user.id);
            $('#userUsername').val(user.username);
            $('#userEmail').val(user.email);
            $('#userFirstName').val(user.firstName || '');
            $('#userLastName').val(user.lastName || '');
            $('#userRole').val(user.role);
            $('#userPassword').prop('required', false);
        }
    } catch (error) {
        showError('modalError', 'Failed to load user data');
        console.error('Error loading user for edit:', error);
    }
}

async function saveUser() {
    const userId = $('#userId').val();
    const userData = {
        username: $('#userUsername').val(),
        email: $('#userEmail').val(),
        firstName: $('#userFirstName').val(),
        lastName: $('#userLastName').val(),
        role: $('#userRole').val()
    };

    const password = $('#userPassword').val();
    if (password || !userId) {
        userData.password = password;
    }

    try {
        const url = userId ? `${API_BASE}/admin/users/${userId}` : `${API_BASE}/admin/users`;
        const method = userId ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify(userData)
        });

        const result = await response.json();

        if (response.ok) {
            showSuccess('modalSuccess', userId ? 'User updated successfully' : 'User created successfully');
            setTimeout(() => {
                bootstrap.Modal.getInstance('#userModal').hide();
                loadUsers();
            }, 1000);
        } else {
            showError('modalError', result.message || 'Failed to save user');
        }
    } catch (error) {
        showError('modalError', 'Network error. Please try again.');
        console.error('Error saving user:', error);
    }
}

async function deleteUser(userId, username) {
    if (!confirm(`Are you sure you want to delete user "${username}"?`)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/admin/users/${userId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${authToken}` }
        });

        if (response.ok) {
            loadUsers();
        } else {
            const error = await response.json();
            alert('Failed to delete user: ' + (error.message || 'Unknown error'));
        }
    } catch (error) {
        alert('Network error. Please try again.');
        console.error('Error deleting user:', error);
    }
}

// Profile Management Functions
async function loadProfile() {
    if (!authToken) return;

    try {
        const response = await fetch(`${API_BASE}/profile`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });

        if (response.ok) {
            const profile = await response.json();
            $('#profileUsername').text(profile.username);
            $('#profileEmail').text(profile.email);
            $('#profileName').text(`${profile.firstName || ''} ${profile.lastName || ''}`.trim() || 'Not set');
            $('#profileRole').text(profile.role);

            // Update TOTP status
            if (profile.totpEnabled) {
                $('#totpDisabled').addClass('d-none');
                $('#totpEnabled').removeClass('d-none');
            } else {
                $('#totpEnabled').addClass('d-none');
                $('#totpDisabled').removeClass('d-none');
            }
        }
    } catch (error) {
        console.error('Error loading profile:', error);
    }
}

async function enableTotp() {
    try {
        const response = await fetch(`${API_BASE}/profile/totp/enable`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${authToken}` }
        });

        if (response.ok) {
            const result = await response.json();
            $('#qrCodeContainer').html(`<img src="${result.qrCodeUrl}" alt="QR Code" class="img-fluid">`);
            $('#totpSetup').removeClass('d-none');
            $('#enableTotpBtn').prop('disabled', true);
        }
    } catch (error) {
        alert('Failed to enable TOTP. Please try again.');
        console.error('Error enabling TOTP:', error);
    }
}

async function confirmTotp() {
    const totpCode = $('#totpConfirmCode').val();
    
    try {
        const response = await fetch(`${API_BASE}/profile/totp/confirm`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify({ totpCode })
        });

        const result = await response.json();

        if (response.ok) {
            $('#totpSetup').addClass('d-none');
            $('#totpDisabled').addClass('d-none');
            $('#totpEnabled').removeClass('d-none');
            $('#enableTotpBtn').prop('disabled', false);
            $('#totpConfirmCode').val('');
            hideError('totpSetupError');
        } else {
            showError('totpSetupError', result.message || 'Invalid TOTP code');
        }
    } catch (error) {
        showError('totpSetupError', 'Network error. Please try again.');
        console.error('Error confirming TOTP:', error);
    }
}

async function disableTotp() {
    if (!confirm('Are you sure you want to disable two-factor authentication?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/profile/totp/disable`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${authToken}` }
        });

        if (response.ok) {
            $('#totpEnabled').addClass('d-none');
            $('#totpDisabled').removeClass('d-none');
        }
    } catch (error) {
        alert('Failed to disable TOTP. Please try again.');
        console.error('Error disabling TOTP:', error);
    }
}

function cancelTotpSetup() {
    $('#totpSetup').addClass('d-none');
    $('#enableTotpBtn').prop('disabled', false);
    $('#totpConfirmCode').val('');
    hideError('totpSetupError');
}

async function changePassword(event) {
    event.preventDefault();

    const currentPassword = $('#currentPassword').val();
    const newPassword = $('#newPassword').val();
    const confirmPassword = $('#confirmPassword').val();

    hideError('passwordChangeError');
    hideSuccess('passwordChangeSuccess');

    if (newPassword !== confirmPassword) {
        showError('passwordChangeError', 'New passwords do not match');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/profile/password`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${authToken}`
            },
            body: JSON.stringify({
                currentPassword,
                newPassword
            })
        });

        const result = await response.json();

        if (response.ok) {
            showSuccess('passwordChangeSuccess', 'Password changed successfully');
            $('#changePasswordForm')[0].reset();
        } else {
            showError('passwordChangeError', result.message || 'Failed to change password');
        }
    } catch (error) {
        showError('passwordChangeError', 'Network error. Please try again.');
        console.error('Error changing password:', error);
    }
}

// Utility functions
function showError(elementId, message) {
    $(`#${elementId}`).text(message).removeClass('d-none');
}

function hideError(elementId) {
    $(`#${elementId}`).addClass('d-none');
}

function showSuccess(elementId, message) {
    $(`#${elementId}`).text(message).removeClass('d-none');
}

function hideSuccess(elementId) {
    $(`#${elementId}`).addClass('d-none');
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

// Make functions globally available for onclick handlers
window.editUser = (userId) => showUserModal(userId);
window.deleteUser = deleteUser;
