import React, { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../../api/axios';
import { useAuthStore } from '../../stores/authStore';

const OAuthKakao: React.FC = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const code = searchParams.get('code');
    const processed = useRef(false);
    const { login } = useAuthStore();

    useEffect(() => {
        if (!code || processed.current) return;

        processed.current = true;

        const loginKakao = async () => {
            try {
                // 1. 카카오 로그인 (토큰 발급)
                const tokenRes = await api.post('/auth/login/kakao', { code });
                const { accessToken, refreshToken } = tokenRes.data;

                // 2. 내 정보 조회 (User Service)
                const userRes = await api.get('/users/me', {
                    headers: { Authorization: `Bearer ${accessToken}` }
                });
                
                const user = userRes.data;

                // 3. Zustand Store에 저장
                login(user, accessToken, refreshToken);

                // 4. 아이돌 페이지로 이동
                navigate('/idol'); 
            } catch (error) {
                console.error('카카오 로그인 실패:', error);
                alert('로그인에 실패했습니다.');
                // 로그인 페이지가 없으므로 메인으로 이동 (로그인 섹션 스크롤)
                navigate('/', { state: { scrollToLogin: true } });
            }
        };

        loginKakao();
    }, [code, navigate, login]);

    return (
        <div className="flex items-center justify-center min-h-screen">
            <div className="text-center">
                <h2 className="text-xl font-bold mb-4">로그인 중입니다...</h2>
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900 mx-auto"></div>
            </div>
        </div>
    );
};

export default OAuthKakao;
