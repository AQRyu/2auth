import api from './api.js';

export const userService = {
    // Get all users
    async getUsers() {
        const response = await api.get('/admin/users');
        return response.data;
    },

    // Get user by ID
    async getUser(id) {
        const response = await api.get(`/admin/users/${id}`);
        return response.data;
    },

    // Create new user
    async createUser(userData) {
        const response = await api.post('/admin/users', userData);
        return response.data;
    },

    // Update user
    async updateUser(id, userData) {
        const response = await api.put(`/admin/users/${id}`, userData);
        return response.data;
    },

    // Delete user
    async deleteUser(id) {
        const response = await api.delete(`/admin/users/${id}`);
        return response.data;
    },

    // Get dashboard stats
    async getStats() {
        const response = await api.get('/admin/stats');
        return response.data;
    },

    // Reset user password
    async resetPassword(id, newPassword) {
        const response = await api.post(`/admin/users/${id}/reset-password`, {
            newPassword,
        });
        return response.data;
    },

    // Enable/disable TOTP for user
    async toggleTotp(id, enabled) {
        const response = await api.post(`/admin/users/${id}/totp`, {
            enabled,
        });
        return response.data;
    },
};

export default userService;
