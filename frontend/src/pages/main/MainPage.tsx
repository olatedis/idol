import React, { useState, useEffect } from "react";
import { motion } from "framer-motion";
import Header from "./Header";
import { useLocation, useNavigate } from "react-router-dom";
import { api } from "../../api/axios";
import { useAuthStore } from "../../stores/authStore";
import SignupModal from "../../components/auth/SignupModal";
import { showSuccessToast, showErrorToast, showAlert } from "../../utils/alert";
import idolImg from "../../assets/emoji1.png"
import logoIcon from "../../assets/logo1.png"

const MainPage: React.FC = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [isSignupOpen, setIsSignupOpen] = useState(false);

    const location = useLocation();
    const navigate = useNavigate();
    const { login, user } = useAuthStore();

    useEffect(() => {
        if (location.state?.scrollToLogin) {
            const el = document.getElementById("login-section");
            el?.scrollIntoView({ behavior: "smooth" });
        }
    }, [location.state]);

    // 수정: 공통 등장 애니메이션
    const fadeUp = {
        initial: { opacity: 0, y: 40 },
        whileInView: { opacity: 1, y: 0 },
        transition: { duration: 0.7 },
        viewport: { once: true },
    };

    // 수정: 카드 등장 애니메이션
    const cardUp = (delay = 0) => ({
        initial: { opacity: 0, y: 30 },
        whileInView: { opacity: 1, y: 0 },
        transition: { duration: 0.6, delay },
        viewport: { once: true },
    });

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();

        try {
            const response = await api.post("/auth/login", {
                username,
                password,
            });

            const { accessToken, refreshToken, user } = response.data;
            let userObj = user;

            if (!user) {
                const userRes = await api.get("/users/me", {
                    headers: { Authorization: `Bearer ${accessToken}` }
                });
                login(userRes.data, accessToken, refreshToken);
                userObj = userRes.data;
            } else {
                login(user, accessToken, refreshToken);
                userObj = user;
            }

            showSuccessToast("로그인되었습니다.");

            // 기존 내부 로직 유지
            if (userObj.role === "IDOL") {
                try {
                    const idolInfoRes = await api.get("/idols/me", {
                        headers: { Authorization: `Bearer ${accessToken}` }
                    });
                    const groupId = idolInfoRes.data.groupId;

                    if (groupId) {
                        navigate(`/group/${groupId}`);
                    } else {
                        navigate("/idol");
                    }
                } catch (err) {
                    navigate("/idol");
                }
            } else {
                navigate("/idol");
            }

        } catch (error: any) {
            const message =
                error?.response?.data?.message ||
                error?.response?.data ||
                "";

            if (typeof message === "string" && message.includes("잠겼습니다")) {
                showErrorToast("로그인 실패가 누적되어 계정이 30분간 잠겼습니다.");
                return;
            }

            showErrorToast("아이디 또는 비밀번호를 확인해주세요.");
        }
    };

    const handleKakaoLogin = () => {
        const REST_API_KEY = import.meta.env.VITE_KAKAO_API_KEY;
        const REDIRECT_URI = `${window.location.origin}/oauth/kakao`;

        if (!REST_API_KEY) {
            showAlert("오류", "카카오 API 키가 설정되지 않았습니다.", "error");
            return;
        }

        const KAKAO_URL = `https://kauth.kakao.com/oauth/authorize?client_id=${REST_API_KEY}&redirect_uri=${REDIRECT_URI}&response_type=code`;

        window.location.href = KAKAO_URL;
    };

    return (
        <div className="h-screen overflow-hidden bg-idol-bg">
            <Header />

            <main className="h-full overflow-y-scroll snap-y snap-mandatory scrollbar-hide">
                {/* ========================= */}
                {/* Slide 1 : Hero */}
                {/* ========================= */}
                <section className="relative h-screen snap-start overflow-hidden bg-[#ff916f]">
                    {/* 수정: 배경 장식만 남기고 우측 네모 오브젝트 제거 */}
                    <div className="absolute inset-0 overflow-hidden">
                        <div className="absolute -left-28 top-16 h-72 w-72 rounded-full bg-[#7D8ABC]/80" />
                        <div className="absolute right-[-110px] top-8 h-80 w-80 rounded-full bg-[#ffe96a]/80" />
                        <div className="absolute left-24 top-36 h-10 w-10 rounded-full bg-[#6cffb2]" />
                        <div className="absolute right-40 top-48 h-8 w-8 rounded-full bg-white/80" />
                        <div className="absolute left-1/2 top-20 h-14 w-14 -translate-x-1/2 rounded-full bg-[#fff4c2]" />
                        <div className="absolute bottom-36 left-20 h-16 w-16 rounded-3xl bg-[#77e296] rotate-12" />
                        <div className="absolute bottom-44 right-24 h-12 w-12 rounded-full border-4 border-[#7D8ABC]" />
                    </div>

                    {/* 수정: dolchat 중앙 크게 */}
                    <div className="relative z-10 mx-auto flex h-full max-w-7xl items-center justify-center px-8 pt-10 md:px-16">
                        <motion.div
                            {...fadeUp}
                            className="flex w-full flex-col items-center justify-center text-center"
                        >
                            <div className="relative inline-block">
                                {/* 로고 오브젝트 */}
                                <img
                                    src={logoIcon}
                                    alt="dolchat logo object"
                                    className="absolute left-[2%] top-[95%] z-10 w-[170px] -translate-x-1/2 -translate-y-1/2 -rotate-12 object-contain md:w-[350px]"
                                />

                                {/* 메인 텍스트 */}
                                <h1 className="relative z-20 text-[120px] md:text-[260px] font-black tracking-[-0.08em] text-white leading-none">
                                    dolchat
                                </h1>
                            </div>

                        </motion.div>
                    </div>

                    <div className="absolute bottom-0 left-0 w-full">
                        <svg
                            viewBox="0 0 1440 180"
                            className="h-[120px] w-full md:h-[160px]"
                            preserveAspectRatio="none"
                        >
                            <path
                                d="M0,96L34.3,106.7C68.6,117,137,139,206,138.7C274.3,139,343,117,411,106.7C480,96,549,96,617,106.7C685.7,117,754,139,823,138.7C891.4,139,960,117,1029,106.7C1097.1,96,1166,96,1234,106.7C1302.9,117,1371,139,1406,149.3L1440,160L1440,181L1405.7,181C1371.4,181,1303,181,1234,181C1165.7,181,1097,181,1029,181C960,181,891,181,823,181C754.3,181,686,181,617,181C548.6,181,480,181,411,181C342.9,181,274,181,206,181C137.1,181,69,181,34,181L0,181Z"
                                fill="#7D8ABC"
                            />
                        </svg>
                    </div>
                </section>

                {/* ========================= */}
                {/* Slide 2 : About */}
                {/* ========================= */}
                <section className="relative h-screen snap-start overflow-hidden bg-[#7d78d8]">
                    <div className="absolute inset-0 overflow-hidden">
                        <div className="absolute -left-20 top-24 h-72 w-72 rounded-full bg-[#8ef4af]/60" />
                        <div className="absolute right-[-80px] top-12 h-80 w-80 rounded-full bg-[#91f5d0]/70" />
                        <div className="absolute right-12 top-20 h-64 w-64 rounded-full bg-transparent [background-image:radial-gradient(#77e296_2px,transparent_2px)] [background-size:18px_18px]" />
                        <div className="absolute left-10 bottom-20 h-48 w-48 rounded-full border-[10px] border-[#b6ff73]/60" />
                        <div className="absolute left-32 bottom-8 h-32 w-32 rounded-full border-[8px] border-[#ffe96a]/50" />
                    </div>

                    <div className="relative z-10 mx-auto flex h-full max-w-7xl items-center justify-between gap-10 px-8 py-16 md:px-16">
                        {/* 수정: About 가운데 정렬 느낌으로 이동 */}
                        <motion.div
                            {...fadeUp}
                            className="flex w-full flex-col justify-center md:w-[48%]"
                        >
                            <p className="mb-5 text-center text-4xl font-black tracking-tight text-[#77e296] md:text-6xl md:text-left">
                                About
                            </p>

                            {/* 수정: 메인 문구 크기 줄임 */}
                            <h2 className="mb-6 text-center text-2xl font-black leading-tight text-white md:text-left md:text-4xl">
                                팬과 아이돌이
                                <br />
                                더 가까워지는 공간
                            </h2>

                            <p className="mx-auto max-w-xl text-center text-sm leading-7 text-white/85 md:mx-0 md:text-left md:text-base">
                                dolchat은 좋아하는 아이돌의 채팅, 공식 소식, 공연과 투표까지
                                한곳에서 자연스럽게 이어지는 팬 플랫폼입니다.
                                팬은 더 빠르게 소식을 확인하고, 더 가깝게 반응하고,
                                더 편하게 함께 즐길 수 있습니다.
                            </p>
                        </motion.div>

                        <motion.div
                            initial={{ opacity: 0, x: 50, y: 20 }}
                            whileInView={{ opacity: 1, x: 0, y: 0 }}
                            transition={{ duration: 0.8 }}
                            viewport={{ once: true }}
                            className="relative hidden md:flex md:w-[40%] items-center justify-center"
                        >
                            <img
                                src={idolImg}
                                alt="idol"
                                className="h-[420px] w-auto object-contain"
                            />
                        </motion.div>


                    </div>

                    <div className="absolute bottom-0 left-0 w-full">
                        <svg
                            viewBox="0 0 1440 180"
                            className="h-[120px] w-full md:h-[160px]"
                            preserveAspectRatio="none"
                        >
                            <path
                                d="M0,64L34.3,80C68.6,96,137,128,206,138.7C274.3,149,343,139,411,117.3C480,96,549,64,617,58.7C685.7,53,754,75,823,90.7C891.4,107,960,117,1029,106.7C1097.1,96,1166,64,1234,58.7C1302.9,53,1371,75,1406,85.3L1440,96L1440,181L1405.7,181C1371.4,181,1303,181,1234,181C1165.7,181,1097,181,1029,181C960,181,891,181,823,181C754.3,181,686,181,617,181C548.6,181,480,181,411,181C342.9,181,274,181,206,181C137.1,181,69,181,34,181L0,181Z"
                                fill="#77e296"
                            />
                        </svg>
                    </div>
                </section>

                {/* ========================= */}
                {/* Slide 3 : Service Cards */}
                {/* ========================= */}
                <section className="relative h-screen snap-start overflow-hidden bg-[#77e296]">
                    {/* 배경 패턴 + 원 장식 */}
                    <div className="absolute inset-0 overflow-hidden">
                        <div className="absolute inset-0 opacity-70 [background-image:radial-gradient(circle_at_20%_20%,rgba(255,233,106,0.55)_0,rgba(255,233,106,0.55)_8px,transparent_8px),radial-gradient(circle_at_80%_30%,rgba(125,138,188,0.20)_0,rgba(125,138,188,0.20)_10px,transparent_10px),radial-gradient(circle_at_30%_80%,rgba(255,255,255,0.28)_0,rgba(255,255,255,0.28)_7px,transparent_7px)]" />
                        <div className="absolute -left-28 top-16 h-[320px] w-[320px] rounded-full bg-white/10" />
                        <div className="absolute right-[-60px] top-20 h-[260px] w-[260px] rounded-full bg-[#fff8db]/15" />
                        <div className="absolute left-1/2 bottom-10 h-[220px] w-[220px] -translate-x-1/2 rounded-full bg-[#7D8ABC]/10" />
                    </div>

                    <div className="relative z-10 mx-auto flex h-full max-w-7xl flex-col items-center justify-center px-8 py-16 md:px-16">
                        <motion.div {...fadeUp} className="mb-14 text-center">
                            <p className="mb-3 text-4xl font-black tracking-tight text-[#7D4CDB] md:text-6xl">
                                Service
                            </p>
                        </motion.div>

                        {/* 카드 영역 */}
                        <div className="grid w-full max-w-6xl grid-cols-1 gap-12 md:grid-cols-3 md:gap-16">
                            {/* 콘서트 카드 */}
                            <motion.div
                                {...cardUp(0)}
                                className="bg-white shadow-[0_10px_24px_rgba(0,0,0,0.18)]"
                            >
                                <div className="flex h-52 items-center justify-center bg-[#fff1c7]">
                                    <span className="text-6xl">🎫</span>
                                </div>

                                <div className="px-5 py-5">
                                    <h3 className="mb-2 text-xl font-black text-[#23314f]">콘서트</h3>
                                    <p className="text-sm leading-7 text-gray-600">
                                        공연 일정과 예매 오픈 시점을 확인하고,
                                        관심 있는 콘서트를 빠르게 찾아볼 수 있습니다.
                                    </p>
                                </div>

                                {/* 스샷 느낌 하단 컬러 바 */}
                                <div className="h-[6px] w-full bg-[#7D4CDB]" />
                            </motion.div>

                            {/* 채팅 카드 */}
                            <motion.div
                                {...cardUp(0.1)}
                                className="bg-white shadow-[0_10px_24px_rgba(0,0,0,0.18)] "
                            >
                                <div className="flex h-52 items-center justify-center bg-[#ffd8f2]">
                                    <span className="text-6xl">💬</span>
                                </div>

                                <div className="px-5 py-5">
                                    <h3 className="mb-2 text-xl font-black text-[#23314f]">채팅</h3>
                                    <p className="text-sm leading-7 text-gray-600">
                                        아이돌과 더 가깝게 소통하고,
                                        새 메시지는 알림으로 바로 받아볼 수 있습니다.
                                    </p>
                                </div>

                                <div className="h-[6px] w-full bg-[#7D4CDB]" />
                            </motion.div>

                            {/* 투표 카드 */}
                            <motion.div
                                {...cardUp(0.2)}
                                className="bg-white shadow-[0_10px_24px_rgba(0,0,0,0.18)] "
                            >
                                <div className="flex h-52 items-center justify-center bg-[#dce4ff]">
                                    <span className="text-6xl">🗳️</span>
                                </div>

                                <div className="px-5 py-5">
                                    <h3 className="mb-2 text-xl font-black text-[#23314f]">투표</h3>
                                    <p className="text-sm leading-7 text-gray-600">
                                        진행 중인 투표를 확인하고,
                                        좋아하는 아이돌에게 직접 응원의 마음을 전할 수 있습니다.
                                    </p>
                                </div>

                                <div className="h-[6px] w-full bg-[#7D4CDB]" />
                            </motion.div>
                        </div>
                    </div>

                    <div className="absolute bottom-0 left-0 w-full">
                        <svg
                            viewBox="0 0 1440 180"
                            className="h-[120px] w-full md:h-[160px]"
                            preserveAspectRatio="none"
                        >
                            <path
                                d="M0,160L34.3,149.3C68.6,139,137,117,206,106.7C274.3,96,343,96,411,106.7C480,117,549,139,617,138.7C685.7,139,754,117,823,101.3C891.4,85,960,75,1029,85.3C1097.1,96,1166,128,1234,133.3C1302.9,139,1371,117,1406,106.7L1440,96L1440,181L1405.7,181C1371.4,181,1303,181,1234,181C1165.7,181,1097,181,1029,181C960,181,891,181,823,181C754.3,181,686,181,617,181C548.6,181,480,181,411,181C342.9,181,274,181,206,181C137.1,181,69,181,34,181L0,181Z"
                                fill="#ece8ff"
                            />
                        </svg>
                    </div>
                </section>

                {/* ========================= */}
                {/* Slide 4 : Login */}
                {/* ========================= */}
                {!user && (
                    <section
                        id="login-section"
                        className="relative h-screen snap-start overflow-hidden bg-[#ece8ff]"
                    >
                        {/* 배경 장식 약하게 */}
                        <div className="absolute inset-0 overflow-hidden">
                            <div className="absolute left-10 top-20 h-28 w-28 rounded-full bg-white/10" />
                            <div className="absolute right-20 top-28 h-20 w-20 rounded-full bg-[#7D8ABC]/10" />
                            <div className="absolute left-1/3 top-32 h-10 w-10 rounded-full bg-[#ff9292]/15" />
                        </div>

                        <div className="relative z-10 mx-auto flex h-full max-w-6xl flex-col justify-center px-8 md:px-16">
                            {/* 타이틀 */}
                            <motion.div {...fadeUp} className="mb-12 text-center">
                                <p className="text-3xl font-black text-[#7D4CDB] md:text-5xl">
                                    Start
                                </p>
                            </motion.div>

                            {/* 좌우 2단 */}
                            <div className="mx-auto grid w-full max-w-4xl grid-cols-1 gap-14 md:grid-cols-2">
                                {/* 왼쪽 로그인 */}
                                <motion.div {...fadeUp} className="mx-auto w-full max-w-[260px]">
                                    <p className="mb-4 text-lg font-black text-[#222]">Write Us</p>

                                    <form onSubmit={handleLogin} className="flex flex-col">
                                        <input
                                            type="text"
                                            id="username"
                                            name="username"
                                            placeholder="아이디를 입력하세요."
                                            onChange={(e) => setUsername(e.target.value)}
                                            className="mb-3 w-full border border-black/30 bg-transparent px-3 py-2 text-sm text-black placeholder-gray-500 transition focus:border-[#7D4CDB] focus:bg-white/20 focus:outline-none"
                                        />

                                        <input
                                            type="password"
                                            id="password"
                                            name="password"
                                            placeholder="비밀번호를 입력하세요."
                                            onChange={(e) => setPassword(e.target.value)}
                                            className="mb-4 w-full border border-black/30 bg-transparent px-3 py-2 text-sm text-black placeholder-gray-500 transition focus:border-[#7D4CDB] focus:bg-white/20 focus:outline-none"
                                        />

                                        <button
                                            type="submit"
                                            className="mb-3 w-[110px] bg-[#8f86e8] px-4 py-2 text-sm font-semibold text-white transition hover:cursor-pointer hover:brightness-105"
                                        >
                                            로그인
                                        </button>

                                        <button
                                            type="button"
                                            onClick={() => setIsSignupOpen(true)}
                                            className="mb-3 w-[110px] bg-[#ff9292] px-4 py-2 text-sm font-semibold text-white transition hover:cursor-pointer hover:brightness-105"
                                        >
                                            가입하기
                                        </button>

                                        <button
                                            type="button"
                                            onClick={handleKakaoLogin}
                                            className="w-[180px] bg-yellow-300 px-4 py-2 text-sm font-semibold text-black transition hover:cursor-pointer hover:brightness-105"
                                        >
                                            카카오 로그인
                                        </button>
                                    </form>
                                </motion.div>

                                {/* 오른쪽 안내 */}
                                <motion.div {...fadeUp} className="mx-auto w-full max-w-[280px]">
                                    <p className="mb-4 text-lg font-black text-[#222]">Reach Us</p>

                                    <div className="space-y-2 text-sm leading-7 text-black/80">
                                        <p>Seoul, South Korea</p>
                                        <p>dolchat fan communication platform</p>

                                        <p className="pt-2">
                                            <span className="font-semibold text-black">Phone:</span> +82 10-1234-5678
                                        </p>

                                        <p>
                                            <span className="font-semibold text-black">Email:</span> dolchat@idolchat.com
                                        </p>
                                    </div>

                                    {/* 흑백 아이콘 느낌 */}
                                    <div className="mt-8 flex items-center gap-4 text-xl grayscale">
                                        <span>📞</span>
                                        <span>✉️</span>
                                        <span>💬</span>
                                    </div>
                                </motion.div>
                            </div>
                        </div>

                        {/* 하단 물결 */}
                        <div className="absolute bottom-0 left-0 w-full">
                            <svg
                                viewBox="0 0 1440 170"
                                className="h-[110px] w-full md:h-[130px]"
                                preserveAspectRatio="none"
                            >
                                <path
                                    d="M0,96L48,101.3C96,107,192,117,288,117.3C384,117,480,107,576,96C672,85,768,75,864,80C960,85,1056,107,1152,112C1248,117,1344,107,1392,101.3L1440,96L1440,181L1392,181C1344,181,1248,181,1152,181C1056,181,960,181,864,181C768,181,672,181,576,181C480,181,384,181,288,181C192,181,96,181,48,181L0,181Z"
                                    fill="#8f86e8"
                                />
                            </svg>
                        </div>
                    </section>
                )}
            </main>

            <SignupModal
                isOpen={isSignupOpen}
                onClose={() => setIsSignupOpen(false)}
                onSwitchToLogin={() => {
                    setIsSignupOpen(false);
                    const el = document.getElementById("login-section");
                    el?.scrollIntoView({ behavior: "smooth" });
                }}
            />
        </div>
    );
};

export default MainPage;