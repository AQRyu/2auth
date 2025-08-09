/**
 * Helper functions extracted from admin-dashboard.js for testing
 * These are the authentication-related functions
 */

// Extract function implementations for testing
function extractedFunctions() {
  // Authentication functions
  async function handleLogin(event) {
    event.preventDefault();
    
    const username = $('#loginUsername').val();
    const password = $('#loginPassword').val();

    try {
      const response = await fetch(`${global.API_BASE}/auth/login`, {
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
        global.showError('loginError', result.message || 'Login failed');
        return;
      }

      if (result.requireTotp) {
        global.pendingLoginData = { username, password };
        $('#loginForm').addClass('d-none');
        $('#totpForm').removeClass('d-none');
        global.hideError('loginError');
        $('#totpCode').focus();
      } else {
        global.authToken = result.accessToken;
        global.currentUser = result.user;
        global.showDashboard();
      }
    } catch (error) {
      global.showError('loginError', 'Network error. Please try again.');
      console.error('Login error:', error);
    }
  }

  async function handleTotpVerification(event) {
    event.preventDefault();

    if (!global.pendingLoginData) {
      global.showError('totpError', 'Session expired. Please login again.');
      global.backToLogin();
      return;
    }

    const totpCode = $('#totpCode').val();

    try {
      const response = await fetch(`${global.API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          usernameOrEmail: global.pendingLoginData.username,
          password: global.pendingLoginData.password,
          totpCode: totpCode
        })
      });

      const result = await response.json();

      if (!response.ok) {
        global.showError('totpError', result.message || 'Invalid TOTP code');
        return;
      }

      global.authToken = result.accessToken;
      global.currentUser = result.user;
      global.pendingLoginData = null;
      global.showDashboard();
    } catch (error) {
      global.showError('totpError', 'Network error. Please try again.');
      console.error('TOTP verification error:', error);
    }
  }

  function logout() {
    global.authToken = null;
    global.currentUser = null;
    global.pendingLoginData = null;
    
    $('#adminDashboard').addClass('d-none');
    $('#loginForm').removeClass('d-none');
    $('#totpForm').addClass('d-none');
    
    // Clear forms
    $('#loginUsername, #loginPassword, #totpCode').val('');
    global.hideError('loginError');
    global.hideError('totpError');
  }

  function backToLogin() {
    $('#totpForm').addClass('d-none');
    $('#loginForm').removeClass('d-none');
    $('#totpCode').val('');
    global.hideError('totpError');
    global.pendingLoginData = null;
  }

  return {
    handleLogin,
    handleTotpVerification,
    logout,
    backToLogin
  };
}

module.exports = extractedFunctions();
