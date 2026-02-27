import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import CryptoJS from 'crypto-js';

const SECRET_KEY = import.meta.env.VITE_STORAGE_SECRET_KEY || 'idol-fallback-secret-key-2026';

const customStorage = {
    getItem: (name: string) => {
        const storedValue = localStorage.getItem(name);
        if (!storedValue) return null;
        try {
            const bytes = CryptoJS.AES.decrypt(storedValue, SECRET_KEY);
            const decryptedString = bytes.toString(CryptoJS.enc.Utf8);
            return decryptedString || null;
        } catch (e) {
            console.error("Storage decryption failed", e);
            return null;
        }
    },
    setItem: (name: string, value: string) => {
        const encryptedValue = CryptoJS.AES.encrypt(value, SECRET_KEY).toString();
        localStorage.setItem(name, encryptedValue);
    },
    removeItem: (name: string) => {
        localStorage.removeItem(name);
    }
};

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
            storage: createJSONStorage(() => customStorage),
        }
    )
);
