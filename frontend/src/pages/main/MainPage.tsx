import React, { useState, useEffect } from "react";
import { motion } from "framer-motion";
import Header from "./Header";
import { useLocation, useNavigate } from "react-router-dom";
import { api } from "../../api/axios";
import { useAuthStore } from "../../stores/authStore";
import SignupModal from "../../components/auth/SignupModal";
import { showSuccessToast, showErrorToast, showAlert } from "../../utils/alert";
import main1 from "../../assets/main1.png"
import main2 from "../../assets/main2.jpg"
import main3 from "../../assets/main3.jpg"
import main4 from "../../assets/main4.jpg"
// import idolImg from "../../assets/emoji1.png"
// import logoIcon from "../../assets/logo1.png"

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
                {/* Hero Section */}
                <section className="relative w-full h-[700px] bg-idol flex flex-col justify-center items-center text-white overflow-hidden">
                    <img
                        src={main1}
                        alt="Dolchat Hero"
                        className="absolute inset-0 w-full h-full object-cover opacity-60"
                    />
                    <div className="z-10 text-center flex flex-col items-center">
                        <h1 className="text-5xl font-serif mb-4 tracking-wide">MEET YOUR ARTIST</h1>
                        <p className="text-sm tracking-widest mb-10 text-[var(--color-idol-point)] uppercase">
                            Start a private conversation on Dolchat
                        </p>
                        <div onClick={()=>navigate("/idol")} className="border-b border-white pb-1 text-xs tracking-widest hover:text-[var(--color-idol-point)] hover:border-[var(--color-idol-point)] hover:cursor-pointer transition-colors">
                            VIEW ALL ARTISTS
                        </div>
                    </div>
                </section>

                {/* Artists Grid Section */}
                <section className="py-32 px-10 bg-white">
                    <div className="text-center mb-20">
                        <h2 className="text-2xl font-serif text-[var(--color-idol-dark)]">
                            New in selection<br />of ARTISTS
                        </h2>
                    </div>

                    <div className="flex items-center max-w-7xl mx-auto">
                        <div className="w-12 -rotate-90 text-xs tracking-widest whitespace-nowrap text-gray-400 font-serif">
                            Explore all artists
                        </div>

                        <div className="flex-1 grid grid-cols-5 gap-10 content-center">
                            <div className="flex flex-col items-center group cursor-pointer">
                                <div className="w-full aspect-[3/4] mb-6 overflow-hidden bg-[var(--color-idol-bg)]">
                                    <img
                                        src={main2}
                                        alt="dolchat2"
                                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                    />
                                </div>
                                <span className="text-sm font-medium text-[var(--color-idol-dark)] mb-1">ARTIST NAME</span>
                                <span className="text-xs text-[var(--color-idol)]">Group Name</span>
                            </div>
                            <div  className="flex flex-col items-center group cursor-pointer">
                                <div className="w-full aspect-[3/4] mb-6 overflow-hidden bg-[var(--color-idol-bg)]">
                                    <img
                                        src={main3}
                                        alt="dolchat3"
                                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                    />
                                </div>
                                <span className="text-sm font-medium text-[var(--color-idol-dark)] mb-1">ARTIST NAME</span>
                                <span className="text-xs text-[var(--color-idol)]">Group Name</span>
                            </div>
                            <div className="flex flex-col items-center group cursor-pointer">
                                <div className="w-full aspect-[3/4] mb-6 overflow-hidden bg-[var(--color-idol-bg)]">
                                    <img
                                        src={main4}
                                        alt={"dolchat4"}
                                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                    />
                                </div>
                                <span className="text-sm font-medium text-[var(--color-idol-dark)] mb-1">ARTIST NAME</span>
                                <span className="text-xs text-[var(--color-idol)]">Group Name</span>
                            </div>
                        </div>
                    </div>
                </section>

                {/* Private Message Section (Left Image, Right Text) */}
                <section className="py-32 px-20 flex items-center gap-24 max-w-7xl mx-auto">
                    <div className="w-1/2">
                        <img
                            src="/assets/main6.png"
                            alt="Private Message Feature"
                            className="w-full aspect-[4/5] object-cover bg-gray-100"
                        />
                    </div>
                    <div className="w-1/2 pr-10">
                        <h2 className="text-5xl font-serif mb-8 text-[var(--color-idol-dark)]">PRIVATE<br />MESSAGE</h2>
                        <p className="text-sm text-gray-500 leading-loose mb-12">
                            최애 아티스트와 일상을 공유하고 특별한 메시지를 주고받으세요.
                            버블처럼 프라이빗하고, 더욱 특별한 소통 공간이 여러분을 기다립니다.
                            당신만을 위한 아티스트의 메시지를 지금 확인해보세요.
                        </p>
                        <a href="#chat" className="flex items-center gap-4 text-xs font-semibold tracking-widest text-[var(--color-idol)] hover:text-[var(--color-idol-dark)] transition-colors">
                            <span className="w-12 h-[1px] bg-current"></span>
                            START CHATTING
                        </a>
                    </div>
                </section>

                {/* Ticketing Section (Left Text, Right Image) */}
                <section className="py-32 px-20 flex items-center gap-24 bg-white max-w-7xl mx-auto">
                    <div className="w-1/2 pl-10 flex flex-col items-end text-right">
                        <h2 className="text-5xl font-serif mb-8 text-[var(--color-idol-dark)]">EXCLUSIVE<br />TICKETS</h2>
                        <p className="text-sm text-gray-500 leading-loose mb-12">
                            Dolchat에서만 제공하는 독점 팬미팅 및 콘서트 티켓.
                            안정적인 좌석 예매 시스템을 통해 치열한 티켓팅 환경에서도
                            원하는 자리를 놓치지 마세요. 지금 바로 예정된 공연을 확인하세요.
                        </p>
                        <a href="#ticket" className="flex items-center flex-row-reverse gap-4 text-xs font-semibold tracking-widest text-[var(--color-idol)] hover:text-[var(--color-idol-dark)] transition-colors">
                            <span className="w-12 h-[1px] bg-current"></span>
                            RESERVE SEATS
                        </a>
                    </div>
                    <div className="w-1/2">
                        <img
                            src="/assets/main7.png"
                            alt="Ticketing Service"
                            className="w-full aspect-square object-cover bg-gray-100"
                        />
                    </div>
                </section>

                {/* ========================= */}
                {/* Slide 4 : Login */}
                {/* ========================= */}
                {!user && (
                    <section
                        id="login-section"
                        className="relative h-screen  overflow-hidden bg-[#ece8ff]"
                    >

                        <div className="relative z-10 mx-auto flex h-full max-w-6xl flex-col justify-center px-8 md:px-16 ">
                            {/* 타이틀 */}
                            <motion.div {...fadeUp} className="mb-12 text-center">
                                <p className="text-3xl font-black text-[#7D4CDB] md:text-5xl">
                                    Start
                                </p>
                            </motion.div>

                            {/* 좌우 2단 */}
                            <div className="mx-auto grid w-full max-w-4xl grid-cols-1 gap-14 md:grid-cols-2 bg-white p-5 rounded-2xl">
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
                                            className="mb-1 w-full bg-[#8f86e8] px-4 py-2 text-sm font-semibold text-white transition hover:cursor-pointer hover:brightness-105"
                                        >
                                            로그인
                                        </button>

                                        <button
                                            type="button"
                                            onClick={() => setIsSignupOpen(true)}
                                            className="mb-1 w-full bg-[#ff9292] px-4 py-2 text-sm font-semibold text-white transition hover:cursor-pointer hover:brightness-105"
                                        >
                                            가입하기
                                        </button>

                                        <button
                                            type="button"
                                            onClick={handleKakaoLogin}
                                            className="mb-1 w-full bg-yellow-300 px-4 py-2 text-sm font-semibold text-black transition hover:cursor-pointer hover:brightness-105"
                                        >
                                            카카오 로그인
                                        </button>
                                    </form>
                                </motion.div>

                                {/* 오른쪽 안내 */}
                                <motion.div {...fadeUp} className="mx-auto w-full max-w-[280px]">
                                    <p className="mb-4 text-lg font-black text-[#222]">About Us</p>

                                    <div className="space-y-2 text-sm leading-7 text-black/80">
                                        <p>Seoul, South Korea</p>
                                        <p>dolchat fan communication platform</p>

                                        <p className="pt-2">
                                            <span className="font-semibold text-black">github:</span>
                                            <a href="https://github.com/olatedis/idol"> https://github.com/olatedis/idol</a>
                                        </p>
                                        <p>
                                            <span className="font-semibold text-black">Notion:</span>
                                            <a href="https://www.notion.so/2e031fe5bfa680ecb6c4eb870f741392?source=copy_link"> https://www.notion.so</a>
                                        </p>
                                    </div>

                                    {/* 흑백 아이콘 */}
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