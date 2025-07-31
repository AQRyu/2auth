import api from './api.js';

export const authService = {
    // Login with username/password (first step)
    async login(credentials) {
        const response = await api.post('/auth/login', {
            usernameOrEmail: credentials.username,
            password: credentials.password,
            totpCode: credentials.totpCode || null,
        });
        return response.data;
    },

    // Verify TOTP code (second step if required)
    async verifyTotp(username, password, totpCode) {
        const response = await api.post('/auth/login', {
            usernameOrEmail: username,
            password: password,
            totpCode: totpCode,
        });
        return response.data;
    },

    // Logout
    async logout() {
        try {
            await api.post('/auth/logout');
        } catch (error) {
            console.error('Logout error:', error);
        } finally {
            // Clear local storage regardless of API response
            localStorage.removeItem('authToken');
            localStorage.removeItem('currentUser');
        }
    },

    // Get current user info
    async getCurrentUser() {
        const response = await api.get('/auth/me');
        return response.data;
    },

    // Refresh token
    async refreshToken() {
        const response = await api.post('/auth/refresh');
        return response.data;
    },
};

export default authService;
