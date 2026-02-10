import React, { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../../api/axios';

const OAuthKakao: React.FC = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const code = searchParams.get('code');
    const processed = useRef(false); // 중복 호출 방지용 Ref

    useEffect(() => {
        if (!code || processed.current) return;

        processed.current = true; // 처리 시작 표시

        const loginKakao = async () => {
            try {
                // 백엔드에 인가 코드 전송
                const response = await api.post('/auth/login/kakao', { code });
                
                const { accessToken, refreshToken } = response.data;

                // 토큰 저장
                localStorage.setItem('accessToken', accessToken);
                localStorage.setItem('refreshToken', refreshToken);

                // 로그인 성공 후 메인 페이지로 이동
                navigate('/');
            } catch (error) {
                console.error('카카오 로그인 실패:', error);
                alert('로그인에 실패했습니다.');
                navigate('/login');
            }
        };

        loginKakao();
    }, [code, navigate]);

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
