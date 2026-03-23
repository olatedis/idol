import axios from 'axios';
import type { AxiosError, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import { useAuthStore } from '../stores/authStore';
import Swal from 'sweetalert2';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

// Axios 인스턴스 생성
export const api = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

// --- Request Interceptor ---
api.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const { accessToken } = useAuthStore.getState();
        if (accessToken) {
            config.headers.Authorization = `Bearer ${accessToken}`;
        }
        return config;
    },
    (error: any) => Promise.reject(error)
);

// --- Response Interceptor ---
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

const onRefreshed = (accessToken: string) => {
    refreshSubscribers.forEach((callback) => callback(accessToken));
    refreshSubscribers = [];
};

const addRefreshSubscriber = (callback: (token: string) => void) => {
    refreshSubscribers.push(callback);
};

api.interceptors.response.use(
    (response: AxiosResponse) => response,
    async (error: any) => {
        const axiosError = error as AxiosError;
        const originalRequest = axiosError.config as InternalAxiosRequestConfig & { _retry?: boolean };

        // 로그인이나 재발급 요청에서 발생한 401/403은 인터셉터 처리를 패스하고 바로 에러 던짐
        if (originalRequest.url?.includes('/auth/login') || originalRequest.url?.includes('/auth/reissue')) {
            return Promise.reject(error);
        }

        // 제재(RESTRICTED/SUSPENDED) 등으로 인한 403 통제 처리
        if (axiosError.response?.status === 403) {
            const message = (axiosError.response?.data as any)?.message || axiosError.response?.data || '이용이 제한된 서비스이거나 권한이 없습니다.';
            // alert(typeof message === 'string' ? message : '권한이 없습니다.');
            Swal.fire({
                icon: 'error',
                title: '접근 거부',
                text: typeof message === 'string' ? message : '권한이 없습니다.',
                confirmButtonColor: '#FF9292'
            });

            // 만약 토큰 검증 단계의 완전 정지(SUSPENDED)라면 강제 로그아웃
            if (typeof message === 'string' && message.includes('정지')) {
                useAuthStore.getState().logout();
                window.location.href = '/';
            }
            return Promise.reject(error);
        }

        if (axiosError.response?.status === 401 && originalRequest && !originalRequest._retry) {
            if (isRefreshing) {
                return new Promise((resolve) => {
                    addRefreshSubscriber((token: string) => {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                        resolve(api(originalRequest));
                    });
                });
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                const { refreshToken, setTokens } = useAuthStore.getState();

                if (!refreshToken) {
                    throw new Error('No refresh token');
                }

                const { data } = await axios.post(`${BASE_URL}/auth/reissue`, {}, {
                    headers: {
                        RefreshToken: refreshToken,
                    },
                });

                const { accessToken: newAccessToken, refreshToken: newRefreshToken } = data;

                setTokens(newAccessToken, newRefreshToken);

                isRefreshing = false;
                onRefreshed(newAccessToken);

                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                return api(originalRequest);

            } catch (refreshError) {
                isRefreshing = false;
                useAuthStore.getState().logout();
                // 로그인 페이지가 없으므로 메인으로 이동
                window.location.href = '/';
                return Promise.reject(refreshError);
            }
        }

        // 5xx 서버 오류 또는 네트워크 연결 오류 처리
        if (axiosError.response?.status && axiosError.response.status >= 500) {
            Swal.fire({
                icon: 'error',
                title: '서버 오류',
                text: '서버와 연결이 원활하지 않습니다. 잠시 후 다시 시도해주세요.',
                confirmButtonColor: '#FF9292'
            });
        } else if (!axiosError.response) {
            // 응답 자체가 없는 경우 (네트워크 다운 등)
            Swal.fire({
                icon: 'warning',
                title: '네트워크 연결 확인',
                text: '인터넷 연결을 확인하거나 서버가 구동 중인지 확인해 주세요.',
                confirmButtonColor: '#FF9292'
            });
        }

        return Promise.reject(error);
    }
);
