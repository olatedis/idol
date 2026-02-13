import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface User {
    userId: number;
    email: string;
    nickname: string;
    role: string;
}

interface AuthState {
    user: User | null;
    accessToken: string | null;
    refreshToken: string | null;

    login: (user: User, accessToken: string, refreshToken: string) => void;
    logout: () => void;
    setTokens: (accessToken: string, refreshToken: string) => void;
}

export const useAuthStore = create(
    persist<AuthState>(
        (set) => ({
            user: null,
            accessToken: null,
            refreshToken: null,

            login: (user, accessToken, refreshToken) => 
                set({ user, accessToken, refreshToken }),
            
            logout: () => 
                set({ user: null, accessToken: null, refreshToken: null }),

            setTokens: (accessToken, refreshToken) => 
                set({ accessToken, refreshToken }),
        }),
        {
            name: 'auth-storage', // localStorage key
        }
    )
);
