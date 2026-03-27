import React, {useState, useEffect} from "react";
import {motion} from "framer-motion";
import Header from "./Header";
import {useLocation, useNavigate} from "react-router-dom";
import {api} from "../../api/axios";
import {useAuthStore} from "../../stores/authStore";
import SignupModal from "../../components/auth/SignupModal";
import {showSuccessToast, showErrorToast, showAlert} from "../../utils/alert";
import main1 from "../../assets/main1.png"
import main2 from "../../assets/main2.png"
import main3 from "../../assets/main3.png"
import main4 from "../../assets/main4.png"
// import idolImg from "../../assets/emoji1.png"
// import logoIcon from "../../assets/logo1.png"

const MainPage: React.FC = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [isSignupOpen, setIsSignupOpen] = useState(false);

    const location = useLocation();
    const navigate = useNavigate();
    const {login, user} = useAuthStore();

    useEffect(() => {
        if (location.state?.scrollToLogin) {
            const el = document.getElementById("login-section");
            el?.scrollIntoView({behavior: "smooth"});
        }
    }, [location.state]);

    // 수정: 공통 등장 애니메이션
    const fadeUp = {
        initial: {opacity: 0, y: 40},
        whileInView: {opacity: 1, y: 0},
        transition: {duration: 0.7},
        viewport: {once: true},
    };

    // 수정: 카드 등장 애니메이션
    const cardUp = (delay = 0) => ({
        initial: {opacity: 0, y: 30},
        whileInView: {opacity: 1, y: 0},
        transition: {duration: 0.6, delay},
        viewport: {once: true},
    });

    const handleLogin = async (e: React.FormEvent) => {
        e.preventDefault();

        try {
            const response = await api.post("/auth/login", {
                username,
                password,
            });

            const {accessToken, refreshToken, user} = response.data;
            let userObj = user;

            if (!user) {
                const userRes = await api.get("/users/me", {
                    headers: {Authorization: `Bearer ${accessToken}`}
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
                        headers: {Authorization: `Bearer ${accessToken}`}
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
            <Header/>

            <main className="h-full overflow-y-scroll snap-y snap-mandatory scrollbar-hide">
                {/* ========================= */}
                {/* Slide 1 : Hero */}
                {/* ========================= */}
                {/* Hero Section */}
                <section
                    className="relative w-full h-screen sm:h-[600px] md:h-[700px] bg-idol flex flex-col justify-center items-center text-white overflow-hidden">
                    <img
                        src={main1}
                        alt="Dolchat Hero"
                        className="absolute inset-0 w-full h-full object-cover opacity-50"
                    />
                    <div className="z-10 text-center flex flex-col items-center px-4">
                        <h1 className="text-3xl sm:text-4xl md:text-5xl font-black mb-4 tracking-wide">MEET YOUR
                            ARTIST</h1>
                        <p className="text-xs sm:text-sm tracking-widest mb-8 sm:mb-10 text-[var(--color-idol-point)]">
                            Start a private conversation on Dolchat
                        </p>
                        <div onClick={() => navigate("/idol")}
                             className="border-b border-white pb-1 text-xs tracking-widest hover:text-[var(--color-idol-point)] hover:border-[var(--color-idol-point)] hover:cursor-pointer transition-colors">
                            VIEW ALL ARTISTS
                        </div>
                    </div>
                </section>

                {/* 기능 소개 */}
                <section className="py-16 sm:py-24 md:py-32 px-8  bg-white">
                    <div className="text-center mb-12 sm:mb-16 md:mb-20">
                        <h2 className="text-2xl sm:text-3xl md:text-4xl font-black text-[var(--color-idol-dark)]">
                            Dolchat FEATURES
                        </h2>
                    </div>

                    <div className="max-w-7xl mx-auto grid md:grid-cols-1 lg:grid-cols-3 gap-12 md:gap-12">
                        {/* 채팅 */}
                        <motion.div {...cardUp(0)} className="flex flex-col items-center text-center">
                            <div className="relative isolate">
                                <div className="w-full h-2 bg-idol-point absolute bottom-1 z-0"></div>
                                <h3 className="text-lg md:text-xl font-black text-[var(--color-idol)] relative z-10">
                                    CHAT
                                </h3>
                            </div>
                            <p className="text-sm md:text-base text-gray-600 leading-relaxed mb-5">
                                최애 아티스트와 프라이빗한 대화를 나눠보세요.<br/>특별한 메시지와 일상을 함께 할 수 있습니다.
                            </p>
                            <img
                                src={main2}
                                alt="dolchat2"
                                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                            />
                        </motion.div>

                        {/* 투표 */}
                        <motion.div {...cardUp(0.1)} className="flex flex-col items-center text-center">
                            <div className="relative isolate">
                                <div className="w-full h-2 bg-idol-point absolute bottom-1 z-0"></div>
                                <h3 className="text-lg md:text-xl font-black text-[var(--color-idol)] relative z-10">
                                    VOTE
                                </h3>
                            </div>
                            <p className="text-sm md:text-base text-gray-600 leading-relaxed mb-5">
                                팬들의 의견을 들려주세요. 투표에 참여해서 아티스트의 의사결정에 함께합니다.
                            </p>
                            <img
                                src={main3}
                                alt="dolchat3"
                                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                            />
                        </motion.div>

                        {/* 콘서트 좌석 예약 */}
                        <motion.div {...cardUp(0.2)} className="flex flex-col items-center text-center">
                            <div className="relative isolate">
                                <div className="w-full h-2 bg-idol-point absolute bottom-1 z-0"></div>
                                <h3 className="text-lg md:text-xl font-black text-[var(--color-idol)] relative z-10">
                                    CONCERT
                                </h3>
                            </div>

                            <p className="text-sm md:text-base text-gray-600 leading-relaxed mb-5">
                                안정적인 좌석 예매 시스템으로 원하는 자리를 쉽게 확보하세요. Dolchat 팬미팅 및 콘서트 티켓을 지금 예약하세요.
                            </p>
                            <img
                                src={main4}
                                alt="dolchat4"
                                className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                            />
                        </motion.div>
                    </div>
                </section>


                {/* ========================= */}
                {/* Login */}
                {/* ========================= */}
                {!user && (
                    <section
                        id="login-section"
                        className="relative overflow-hidden h-[770px]"
                    >
                        <div
                            className="relative z-10 mx-auto flex h-full max-w-6xl flex-col justify-center px-4 sm:px-8 md:px-16">
                            {/* 타이틀 */}
                            <motion.div {...fadeUp} className="mb-8 sm:mb-10 md:mb-12 text-center">
                                <p className="text-3xl sm:text-4xl md:text-5xl font-black text-idol-dark">
                                    Start
                                </p>
                            </motion.div>

                            {/* 좌우 2단 */}
                            <div
                                className="mx-auto grid w-full max-w-4xl grid-cols-1 gap-8 sm:gap-10 md:gap-14 md:grid-cols-2 bg-[#f3f3f3] p-4 sm:p-5 md:p-8 rounded-2xl">
                                {/* 왼쪽 로그인 */}
                                <motion.div {...fadeUp}
                                            className="mx-auto w-full max-w-xs sm:max-w-sm md:max-w-[260px]">
                                    <p className="mb-3 sm:mb-4 text-base sm:text-lg font-black text-idol">Write Us</p>

                                    <form onSubmit={handleLogin} className="flex flex-col gap-2 sm:gap-3">
                                        <input
                                            type="text"
                                            id="username"
                                            name="username"
                                            placeholder="아이디를 입력하세요."
                                            onChange={(e) => setUsername(e.target.value)}
                                            className="w-full border border-black/30 bg-transparent px-3 py-2 text-xs sm:text-sm text-black placeholder-gray-500 transition focus:border-[#7D4CDB] focus:bg-white/20 focus:outline-none"
                                        />

                                        <input
                                            type="password"
                                            id="password"
                                            name="password"
                                            placeholder="비밀번호를 입력하세요."
                                            onChange={(e) => setPassword(e.target.value)}
                                            className="w-full border border-black/30 bg-transparent px-3 py-2 text-xs sm:text-sm text-black placeholder-gray-500 transition focus:border-[#7D4CDB] focus:bg-white/20 focus:outline-none"
                                        />

                                        <button
                                            type="submit"
                                            className="w-full bg-idol px-4 py-2 text-xs sm:text-sm font-semibold text-white transition hover:cursor-pointer hover:brightness-105"
                                        >
                                            로그인
                                        </button>

                                        <button
                                            type="button"
                                            onClick={() => setIsSignupOpen(true)}
                                            className="w-full bg-idol-dark px-4 py-2 text-xs sm:text-sm font-semibold text-white transition hover:cursor-pointer hover:brightness-105"
                                        >
                                            가입하기
                                        </button>

                                        <button
                                            type="button"
                                            onClick={handleKakaoLogin}
                                            className="w-full bg-yellow-300 px-4 py-2 text-xs sm:text-sm font-semibold text-black transition hover:cursor-pointer hover:brightness-105"
                                        >
                                            👁️‍🗨️카카오 로그인
                                        </button>
                                    </form>
                                </motion.div>

                                {/* 오른쪽 안내 */}
                                <motion.div {...fadeUp}
                                            className="mx-auto w-full max-w-xs sm:max-w-sm md:max-w-[280px]">
                                    <p className="mb-3 sm:mb-4 text-base sm:text-lg font-black text-idol">About Us</p>

                                    <div className="space-y-2 text-xs sm:text-sm leading-6 sm:leading-7 text-black/80">
                                        <p>Seoul, South Korea</p>
                                        <p>dolchat fan communication platform</p>

                                        <p className="pt-2">
                                            <span className="font-semibold text-idol">github:</span>
                                            <a href="https://github.com/olatedis/idol"> https://github.com/olatedis/idol</a>
                                        </p>
                                        <p>
                                            <span className="font-semibold text-idol">Notion:</span>
                                            <a href="https://www.notion.so/2e031fe5bfa680ecb6c4eb870f741392?source=copy_link"> https://www.notion.so</a>
                                        </p>
                                    </div>

                                    {/* 흑백 아이콘 */}
                                    <div
                                        className="mt-6 sm:mt-8 flex items-center gap-3 sm:gap-4 text-lg sm:text-xl grayscale">
                                        <span>📞</span>
                                        <span>✉️</span>
                                        <span>💬</span>
                                    </div>
                                </motion.div>
                            </div>
                        </div>
                        {/*<div className="absolute w-full sm:h-[400px] md:h-[450px] top-80 bg-blend-overlay   mt-3 text-white overflow-hidden">*/}
                        {/*    /!*<img*!/*/}
                        {/*    /!*    src={main5}*!/*/}
                        {/*    /!*    alt="Dolchat footer"*!/*/}
                        {/*    /!*    className="absolute inset-0 w-full h-full object-cover "*!/*/}

                        {/*</div>*/}
                        <div className="absolute bottom-0 left-0 w-full">
                            <svg
                                viewBox="0 0 1440 170"
                                className="h-[110px] w-full md:h-[130px]"
                                preserveAspectRatio="none"
                            >
                                <path
                                    d="M0,96L48,101.3C96,107,192,117,288,117.3C384,117,480,107,576,96C672,85,768,75,864,80C960,85,1056,107,1152,112C1248,117,1344,107,1392,101.3L1440,96L1440,181L1392,181C1344,181,1248,181,1152,181C1056,181,960,181,864,181C768,181,672,181,576,181C480,181,384,181,288,181C192,181,96,181,48,181L0,181Z"
                                    fill="#ffd967"
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
                    el?.scrollIntoView({behavior: "smooth"});
                }}
            />
        </div>
    );
};

export default MainPage;