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

    // Form validation handlers
    $('#userUsername').on('blur', validateUsername);
    $('#userEmail').on('blur', validateEmail);

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

        const result = await response.json();
        const users = result.content || result || []; // Handle both paginated and direct array responses
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
        const statusBadge = getStatusBadge(user);
        const lastLogin = user.lastLogin ? new Date(user.lastLogin).toLocaleDateString() : 'Never';
        const fullName = `${user.firstName || ''} ${user.lastName || ''}`.trim() || 'Not set';
        const userRole = getUserRole(user);
        
        const row = $(`
            <tr>
                <td>
                    <div class="fw-bold">${escapeHtml(user.username)}</div>
                    <div class="text-muted small d-sm-none">${escapeHtml(user.email)}</div>
                </td>
                <td class="d-none d-sm-table-cell">${escapeHtml(user.email)}</td>
                <td class="d-none d-md-table-cell">${escapeHtml(fullName)}</td>
                <td><span class="badge bg-${userRole === 'ADMIN' ? 'primary' : 'secondary'}">${userRole}</span></td>
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

function getUserRole(user) {
    // Check if user has roles array/set and includes ADMIN
    if (user.roles && Array.isArray(user.roles)) {
        return user.roles.includes('ADMIN') ? 'ADMIN' : 'USER';
    } else if (user.roles?.has?.('ADMIN')) {
        return 'ADMIN';
    } else if (user.role) {
        return user.role; // Fallback to single role property
    }
    return 'USER'; // Default fallback
}

function getStatusBadge(user) {
    if (user.accountLocked) {
        return '<span class="badge bg-danger">Locked</span>';
    } else if (!user.accountEnabled) {
        return '<span class="badge bg-warning">Disabled</span>';
    } else {
        return '<span class="badge bg-success">Active</span>';
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
        const adminUsers = users.filter(u => {
            if (Array.isArray(u.roles)) {
                return u.roles.includes('ADMIN');
            } else if (u.roles && typeof u.roles === 'object') {
                // Handle Set-like object
                return Object.values(u.roles).includes('ADMIN') || u.roles.ADMIN;
            }
            return false;
        }).length;

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
        $('#saveUserBtn').text('Update User');
        loadUserForEdit(userId);
    } else {
        $('#modalTitle').text('Add New User');
        $('#saveUserBtn').text('Save User');
    }
    
    const modal = new bootstrap.Modal('#userModal');
    modal.show();
}

function clearUserForm() {
    $('#userForm')[0].reset();
    $('#userId').val('');
    
    // Reset password field to be required for new users
    $('#userPassword').prop('required', true);
    $('label[for="userPassword"]').text('Password *');
    
    // Clear all validation states
    $('#userForm .form-control').removeClass('is-valid is-invalid');
    $('#userForm .invalid-feedback').remove();
}

async function loadUserForEdit(userId) {
    try {
        // Show loading state
        $('#saveUserBtn').text('Loading...').prop('disabled', true);
        
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
            
            // Use getUserRole helper to handle roles array/set properly
            const userRole = getUserRole(user);
            $('#userRole').val(userRole);
            
            // Clear password field and make it optional for editing
            $('#userPassword').val('').prop('required', false);
            
            // Update password label to indicate it's optional
            $('label[for="userPassword"]').text('Password (leave blank to keep current)');
            
            console.log('Loaded user for edit:', user.username, 'Role:', userRole);
        } else {
            const error = await response.json();
            showError('modalError', error.message || 'Failed to load user data');
        }
    } catch (error) {
        showError('modalError', 'Failed to load user data');
        console.error('Error loading user for edit:', error);
    } finally {
        // Reset button state
        $('#saveUserBtn').text('Update User').prop('disabled', false);
    }
}

async function saveUser() {
    const userId = $('#userId').val();
    
    // Clear previous errors
    hideError('modalError');
    
    // Validate form
    const isUsernameValid = validateUsername();
    const isEmailValid = validateEmail();
    
    if (!isUsernameValid || !isEmailValid) {
        showError('modalError', 'Please fix the validation errors above');
        return;
    }
    
    const userData = {
        username: $('#userUsername').val().trim(),
        email: $('#userEmail').val().trim(),
        firstName: $('#userFirstName').val().trim(),
        lastName: $('#userLastName').val().trim(),
        role: $('#userRole').val()
    };

    const password = $('#userPassword').val();
    // Include password if provided or if it's a new user
    if (password || !userId) {
        if (!password && !userId) {
            showError('modalError', 'Password is required for new users');
            return;
        }
        userData.password = password;
    }

    try {
        // Disable save button during request
        $('#saveUserBtn').prop('disabled', true);
        
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
    } finally {
        // Re-enable save button
        $('#saveUserBtn').prop('disabled', false);
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
        const response = await fetch(`${API_BASE}/user/profile`, {
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
        const response = await fetch(`${API_BASE}/user/totp/enable`, {
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
        const response = await fetch(`${API_BASE}/user/totp/confirm`, {
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
        const response = await fetch(`${API_BASE}/user/totp/disable`, {
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

// Form validation functions
function validateUsername() {
    const username = $('#userUsername').val().trim();
    const field = $('#userUsername');
    
    if (username.length < 3) {
        field.addClass('is-invalid');
        field.next('.invalid-feedback').remove();
        field.after('<div class="invalid-feedback">Username must be at least 3 characters long</div>');
        return false;
    } else {
        field.removeClass('is-invalid').addClass('is-valid');
        field.next('.invalid-feedback').remove();
        return true;
    }
}

function validateEmail() {
    const email = $('#userEmail').val().trim();
    const field = $('#userEmail');
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    
    if (!emailRegex.test(email)) {
        field.addClass('is-invalid');
        field.next('.invalid-feedback').remove();
        field.after('<div class="invalid-feedback">Please enter a valid email address</div>');
        return false;
    } else {
        field.removeClass('is-invalid').addClass('is-valid');
        field.next('.invalid-feedback').remove();
        return true;
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
    if (!text) return '';
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
