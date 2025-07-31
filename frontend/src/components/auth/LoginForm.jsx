import { zodResolver } from '@hookform/resolvers/zod';
import { Eye, EyeOff, Lock, Shield, User } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import toast from 'react-hot-toast';
import { useAuthStore } from '../../store/authStore.js';
import { cn } from '../../utils/helpers.js';
import { loginSchema, totpSchema } from '../../utils/validationSchemas.js';

export function LoginForm() {
    const [showPassword, setShowPassword] = useState(false);
    const { login, verifyTotp, isLoading, requireTotp, clearTotp } = useAuthStore();

    // Login form
    const {
        register: registerLogin,
        handleSubmit: handleSubmitLogin,
        formState: { errors: loginErrors },
    } = useForm({
        resolver: zodResolver(loginSchema),
    });

    // TOTP form
    const {
        register: registerTotp,
        handleSubmit: handleSubmitTotp,
        formState: { errors: totpErrors },
        reset: resetTotp,
    } = useForm({
        resolver: zodResolver(totpSchema),
    });

    const onLogin = async (data) => {
        try {
            const result = await login(data);
            if (result.success) {
                toast.success('Login successful!');
                // Redirect will be handled by router
            } else if (result.requireTotp) {
                toast.info('Please enter your TOTP code');
            }
        } catch (error) {
            const message = error.response?.data?.message || 'Login failed';
            toast.error(message);
        }
    };

    const onTotpVerify = async (data) => {
        try {
            const result = await verifyTotp(data.totpCode);
            if (result.success) {
                toast.success('Login successful!');
                resetTotp();
            }
        } catch (error) {
            const message = error.response?.data?.message || 'Invalid TOTP code';
            toast.error(message);
        }
    };

    const handleBackToLogin = () => {
        clearTotp();
        resetTotp();
    };

    if (requireTotp) {
        return (
            <div className="min-h-screen bg-gradient-to-br from-blue-600 to-purple-700 flex items-center justify-center p-4">
                <div className="bg-white rounded-2xl shadow-2xl p-8 w-full max-w-md">
                    <div className="text-center mb-8">
                        <div className="mx-auto w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mb-4">
                            <Shield className="w-8 h-8 text-blue-600" />
                        </div>
                        <h1 className="text-2xl font-bold text-gray-900">Two-Factor Authentication</h1>
                        <p className="text-gray-600 mt-2">
                            Enter the 6-digit code from your authenticator app
                        </p>
                    </div>

                    <form onSubmit={handleSubmitTotp(onTotpVerify)} className="space-y-6">
                        <div className="space-y-2">
                            <label htmlFor="totpCode" className="block text-sm font-medium text-gray-700">
                                TOTP Code
                            </label>
                            <input
                                {...registerTotp('totpCode')}
                                type="text"
                                id="totpCode"
                                placeholder="000000"
                                maxLength={6}
                                className={cn(
                                    'w-full px-4 py-3 border rounded-lg text-center text-2xl tracking-widest focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors bg-white',
                                    totpErrors.totpCode
                                        ? 'border-red-300 bg-red-50'
                                        : 'border-gray-300 hover:border-gray-400'
                                )}
                            />
                            {totpErrors.totpCode && (
                                <p className="text-sm text-red-600">{totpErrors.totpCode.message}</p>
                            )}
                        </div>

                        <div className="space-y-3">
                            <button
                                type="submit"
                                disabled={isLoading}
                                className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold py-3 px-4 rounded-lg transition-colors duration-200 flex items-center justify-center"
                            >
                                {isLoading ? (
                                    <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                                ) : (
                                    'Verify & Login'
                                )}
                            </button>

                            <button
                                type="button"
                                onClick={handleBackToLogin}
                                className="w-full bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium py-3 px-4 rounded-lg transition-colors duration-200"
                            >
                                Back to Login
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-blue-600 to-purple-700 flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl shadow-2xl p-8 w-full max-w-md">
                <div className="text-center mb-8">
                    <div className="mx-auto w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mb-4">
                        <Lock className="w-8 h-8 text-blue-600" />
                    </div>
                    <h1 className="text-3xl font-bold text-gray-900">Welcome Back</h1>
                    <p className="text-gray-600 mt-2">Sign in to your 2Auth dashboard</p>
                </div>

                <form onSubmit={handleSubmitLogin(onLogin)} className="space-y-6">
                    <div className="space-y-2">
                        <label htmlFor="username" className="block text-sm font-medium text-gray-700">
                            Username or Email
                        </label>
                        <div className="relative">
                            <User className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5 z-10" />
                            <input
                                {...registerLogin('username')}
                                type="text"
                                id="username"
                                placeholder="Enter your username or email"
                                className={cn(
                                    'w-full pl-10 pr-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors bg-white',
                                    loginErrors.username
                                        ? 'border-red-300 bg-red-50'
                                        : 'border-gray-300 hover:border-gray-400'
                                )}
                            />
                        </div>
                        {loginErrors.username && (
                            <p className="text-sm text-red-600">{loginErrors.username.message}</p>
                        )}
                    </div>

                    <div className="space-y-2">
                        <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                            Password
                        </label>
                        <div className="relative">
                            <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5 z-10" />
                            <input
                                {...registerLogin('password')}
                                type={showPassword ? 'text' : 'password'}
                                id="password"
                                placeholder="Enter your password"
                                className={cn(
                                    'w-full pl-10 pr-12 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors bg-white',
                                    loginErrors.password
                                        ? 'border-red-300 bg-red-50'
                                        : 'border-gray-300 hover:border-gray-400'
                                )}
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600 z-10"
                            >
                                {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                            </button>
                        </div>
                        {loginErrors.password && (
                            <p className="text-sm text-red-600">{loginErrors.password.message}</p>
                        )}
                    </div>

                    <div className="pt-2">
                        <button
                            type="submit"
                            disabled={isLoading}
                            className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold py-3 px-4 rounded-lg transition-colors duration-200 flex items-center justify-center"
                        >
                            {isLoading ? (
                                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
                            ) : (
                                'Sign In'
                            )}
                        </button>
                    </div>
                </form>

                <div className="mt-6 text-center">
                    <p className="text-sm text-gray-600">
                        Secure authentication powered by{' '}
                        <span className="font-semibold text-blue-600">2Auth</span>
                    </p>
                </div>
            </div>
        </div>
    );
}

export default LoginForm;
