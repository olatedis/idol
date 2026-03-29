import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { api } from '../../api/axios';
import { useAuthStore } from '../../stores/authStore';
import Swal from 'sweetalert2';

interface SignupModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSwitchToLogin: () => void;
}

const SignupModal: React.FC<SignupModalProps> = ({ isOpen, onClose, onSwitchToLogin }) => {
    // 입력 필드
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [nickname, setNickname] = useState('');
    const [email, setEmail] = useState('');
    
    // 이메일 인증 관련 상태
    const [verificationCode, setVerificationCode] = useState('');
    const [verificationToken, setVerificationToken] = useState('');
    const [isEmailSent, setIsEmailSent] = useState(false);
    const [isEmailVerified, setIsEmailVerified] = useState(false);

    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const { login } = useAuthStore();

    // 이메일 인증번호 전송
    const handleSendEmail = async () => {
        if (!email) {
            setError('이메일을 입력해주세요.');
            return;
        }
        try {
            await api.post('/auth/email/send', { email });
            setIsEmailSent(true);
            setError('');
            // @ts-ignore
            Swal.fire({
                icon: 'success',
                title: '인증번호 발송',
                text: '인증번호가 발송되었습니다. 이메일을 확인해주세요.',
                confirmButtonColor: '#FF9292'
            });
        } catch (err: any) {
            console.error(err);
            setError(err.response?.data || '이메일 전송 실패');
        }
    };

    // 이메일 인증번호 확인
    const handleVerifyEmail = async () => {
        if (!verificationCode) {
            setError('인증번호를 입력해주세요.');
            return;
        }
        try {
            const res = await api.post('/auth/email/verify', { email, code: verificationCode });
            setVerificationToken(res.data); // 토큰 저장
            setIsEmailVerified(true);
            setError('');
            // alert('이메일 인증이 완료되었습니다.');
            Swal.fire({
                icon: 'success',
                title: '인증 완료',
                text: '이메일 인증이 완료되었습니다.',
                confirmButtonColor: '#FF9292'
            });
        } catch (err: any) {
            console.error(err);
            setError('인증번호가 올바르지 않습니다.');
        }
    };

    const handleSignup = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!isEmailVerified) {
            setError('이메일 인증을 완료해주세요.');
            return;
        }

        setLoading(true);

        try {
            // 1. 회원가입 요청
            await api.post('/users/register', {
                username,
                password,
                nickname,
                email,
                verificationToken, // 인증 토큰 포함
                role: 'USER'
            });

            // 2. 가입 성공 후 자동 로그인
            const loginRes = await api.post('/auth/login', { username, password });
            const { accessToken, refreshToken } = loginRes.data;

            // 3. 내 정보 조회
            const userRes = await api.get('/users/me', {
                headers: { Authorization: `Bearer ${accessToken}` }
            });

            // 4. Store 저장 및 모달 닫기
            login(userRes.data, accessToken, refreshToken);
            onClose();
            window.scrollTo(0, 0);
            // alert('회원가입을 환영합니다! 🎉');
            Swal.fire({
                icon: 'success',
                title: '환영합니다!',
                text: '회원가입을 환영합니다! 🎉',
                confirmButtonColor: '#FF9292'
            });

        } catch (err: any) {
            console.error(err);
            setError(err.response?.data?.message || '회원가입 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <AnimatePresence>
            {isOpen && (
                <>
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onClose}
                        className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center backdrop-blur-sm"
                    >
                        <motion.div
                            initial={{ scale: 0.9, opacity: 0, y: 20 }}
                            animate={{ scale: 1, opacity: 1, y: 0 }}
                            exit={{ scale: 0.9, opacity: 0, y: 20 }}
                            onClick={(e) => e.stopPropagation()}
                            className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-8 relative overflow-hidden max-h-[90vh] overflow-y-auto"
                        >
                            <button 
                                onClick={onClose}
                                className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition"
                            >
                                ✕
                            </button>

                            <div className="text-center mb-6">
                                <h2 className="text-2xl font-bold text-idol-point">회원가입</h2>
                                <p className="text-gray-500 text-sm mt-2">팬과 아이돌이 하나되는 공간</p>
                            </div>

                            <form onSubmit={handleSignup} className="space-y-4">
                                {/* 아이디 */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">아이디</label>
                                    <input
                                        type="text"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-idol-point outline-none"
                                        placeholder="아이디 입력"
                                        required
                                    />
                                </div>

                                {/* 비밀번호 */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
                                    <input
                                        type="password"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-idol-point outline-none"
                                        placeholder="비밀번호 입력"
                                        required
                                    />
                                </div>

                                {/* 닉네임 */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">닉네임</label>
                                    <input
                                        type="text"
                                        value={nickname}
                                        onChange={(e) => setNickname(e.target.value)}
                                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-idol-point outline-none"
                                        placeholder="닉네임 입력"
                                        required
                                    />
                                </div>

                                {/* 이메일 & 인증 */}
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">이메일</label>
                                    <div className="flex gap-2">
                                        <input
                                            type="email"
                                            value={email}
                                            onChange={(e) => setEmail(e.target.value)}
                                            disabled={isEmailVerified}
                                            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-idol-point outline-none disabled:bg-gray-100"
                                            placeholder="example@email.com"
                                            required
                                        />
                                        <div className="flex items-center gap-2">
                                            <button
                                                type="button"
                                                onClick={handleSendEmail}
                                                disabled={isEmailVerified || isEmailSent}
                                                className="px-3 py-2 bg-gray-800 text-white text-sm rounded-lg hover:bg-gray-700 disabled:opacity-50 whitespace-nowrap transition"
                                            >
                                                {isEmailSent ? '전송됨' : '인증'}
                                            </button>
                                            {isEmailSent && !isEmailVerified && (
                                                <button
                                                    type="button"
                                                    onClick={handleSendEmail}
                                                    className="text-[11px] text-gray-400 hover:text-idol-point hover:underline transition whitespace-nowrap"
                                                >
                                                    재인증
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                </div>

                                {/* 인증번호 입력 (전송된 경우만 표시) */}
                                {isEmailSent && !isEmailVerified && (
                                    <div className="flex gap-2">
                                        <input
                                            type="text"
                                            value={verificationCode}
                                            onChange={(e) => setVerificationCode(e.target.value)}
                                            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-idol-point outline-none"
                                            placeholder="인증번호 6자리"
                                        />
                                        <button
                                            type="button"
                                            onClick={handleVerifyEmail}
                                            className="px-3 py-2 bg-idol-point text-white text-sm rounded-lg hover:opacity-90 whitespace-nowrap"
                                        >
                                            확인
                                        </button>
                                    </div>
                                )}

                                {isEmailVerified && (
                                    <p className="text-green-600 text-sm">✅ 이메일 인증이 완료되었습니다.</p>
                                )}

                                {error && (
                                    <p className="text-red-500 text-sm text-center">{error}</p>
                                )}

                                <button
                                    type="submit"
                                    disabled={loading || !isEmailVerified}
                                    className="w-full bg-idol text-white py-3 rounded-lg font-semibold hover:opacity-90 transition disabled:opacity-50 mt-4"
                                >
                                    {loading ? '가입 중...' : '회원가입 완료'}
                                </button>
                            </form>

                            <div className="mt-6 text-center text-sm text-gray-500">
                                이미 계정이 있으신가요?{' '}
                                <button 
                                    onClick={onSwitchToLogin}
                                    className="text-idol-point font-semibold hover:underline"
                                >
                                    로그인하기
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    );
};

export default SignupModal;
