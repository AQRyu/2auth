import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import authService from '../services/authService.js';

export const useAuthStore = create(
    persist(
        (set, get) => ({
            // State
            user: null,
            token: null,
            isAuthenticated: false,
            isLoading: false,
            requireTotp: false,
            pendingLogin: null,

            // Actions
            login: async (credentials) => {
                set({ isLoading: true });
                try {
                    const result = await authService.login(credentials);

                    if (result.requireTotp) {
                        // TOTP is required, store pending login data
                        set({
                            requireTotp: true,
                            pendingLogin: {
                                username: credentials.username,
                                password: credentials.password,
                            },
                            isLoading: false,
                        });
                        return { requireTotp: true };
                    } else {
                        // Login successful
                        localStorage.setItem('authToken', result.accessToken);
                        localStorage.setItem('currentUser', JSON.stringify(result.user));

                        set({
                            user: result.user,
                            token: result.accessToken,
                            isAuthenticated: true,
                            isLoading: false,
                            requireTotp: false,
                            pendingLogin: null,
                        });

                        return { success: true, user: result.user };
                    }
                } catch (error) {
                    set({ isLoading: false });
                    throw error;
                }
            },

            verifyTotp: async (totpCode) => {
                const { pendingLogin } = get();
                if (!pendingLogin) {
                    throw new Error('No pending login found');
                }

                set({ isLoading: true });
                try {
                    const result = await authService.verifyTotp(
                        pendingLogin.username,
                        pendingLogin.password,
                        totpCode
                    );

                    localStorage.setItem('authToken', result.accessToken);
                    localStorage.setItem('currentUser', JSON.stringify(result.user));

                    set({
                        user: result.user,
                        token: result.accessToken,
                        isAuthenticated: true,
                        isLoading: false,
                        requireTotp: false,
                        pendingLogin: null,
                    });

                    return { success: true, user: result.user };
                } catch (error) {
                    set({ isLoading: false });
                    throw error;
                }
            },

            logout: async () => {
                try {
                    await authService.logout();
                } catch (error) {
                    console.error('Logout error:', error);
                } finally {
                    localStorage.removeItem('authToken');
                    localStorage.removeItem('currentUser');
                    set({
                        user: null,
                        token: null,
                        isAuthenticated: false,
                        requireTotp: false,
                        pendingLogin: null,
                    });
                }
            },

            clearTotp: () => {
                set({
                    requireTotp: false,
                    pendingLogin: null,
                });
            },

            // Initialize auth state from localStorage
            initializeAuth: () => {
                const token = localStorage.getItem('authToken');
                const userStr = localStorage.getItem('currentUser');

                if (token && userStr) {
                    try {
                        const user = JSON.parse(userStr);
                        set({
                            user,
                            token,
                            isAuthenticated: true,
                        });
                    } catch (error) {
                        console.error('Failed to parse user data:', error);
                        localStorage.removeItem('authToken');
                        localStorage.removeItem('currentUser');
                    }
                }
            },
        }),
        {
            name: 'auth-storage',
            partialize: (state) => ({
                user: state.user,
                token: state.token,
                isAuthenticated: state.isAuthenticated,
            }),
        }
    )
);

export default useAuthStore;
