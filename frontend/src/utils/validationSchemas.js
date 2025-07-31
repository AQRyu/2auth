import { z } from 'zod';

// Login form validation schema
export const loginSchema = z.object({
    username: z
        .string()
        .min(1, 'Username is required')
        .min(3, 'Username must be at least 3 characters'),
    password: z
        .string()
        .min(1, 'Password is required')
        .min(6, 'Password must be at least 6 characters'),
});

// TOTP verification schema
export const totpSchema = z.object({
    totpCode: z
        .string()
        .min(6, 'TOTP code must be 6 digits')
        .max(6, 'TOTP code must be 6 digits')
        .regex(/^\d+$/, 'TOTP code must contain only numbers'),
});

// User creation/update schema
export const userSchema = z.object({
    username: z
        .string()
        .min(1, 'Username is required')
        .min(3, 'Username must be at least 3 characters')
        .max(50, 'Username must be less than 50 characters')
        .regex(/^[a-zA-Z0-9_-]+$/, 'Username can only contain letters, numbers, underscores, and hyphens'),
    email: z
        .string()
        .min(1, 'Email is required')
        .email('Please enter a valid email address'),
    password: z
        .string()
        .min(8, 'Password must be at least 8 characters')
        .regex(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/, 'Password must contain at least one lowercase letter, one uppercase letter, and one number'),
    confirmPassword: z.string(),
    role: z.enum(['USER', 'ADMIN'], {
        required_error: 'Please select a role',
    }),
    enabled: z.boolean().default(true),
}).refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
});

// User update schema (password is optional)
export const userUpdateSchema = userSchema.partial({
    password: true,
    confirmPassword: true,
});

export default {
    loginSchema,
    totpSchema,
    userSchema,
    userUpdateSchema,
};
