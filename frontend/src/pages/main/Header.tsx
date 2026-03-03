import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";
import { motion, AnimatePresence } from "framer-motion";

const Header: React.FC = () => {
    const navigate = useNavigate();
    const { user, logout, accessToken } = useAuthStore(); // accessToken 추가
    const isLoggedIn = !!user || !!accessToken; // 로그인 상태 체크 강화

    const [isMenuOpen, setIsMenuOpen] = useState(false);

    const handleLogin = () => {
        navigate("/", { state: { scrollToLogin: true } });
        setIsMenuOpen(false);
    };

    const handleLogout = () => {
        logout();
        alert("로그아웃되었습니다.");
        navigate("/");
        setIsMenuOpen(false);
    };

    const toggleMenu = () => setIsMenuOpen(!isMenuOpen);
    const closeMenu = () => setIsMenuOpen(false);

    return (
        <>
            <header className="w-full px-8 py-4 fixed top-0 z-50 bg-white shadow-sm">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        {/* 햄버거 아이콘 */}
                        <button onClick={toggleMenu} className="p-2 text-gray-700 hover:text-idol-dark transition focus:outline-none">
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
                            </svg>
                        </button>
                        {/* 로고 */}
                        <div onClick={() => navigate("/")}
                            className="text-xl font-bold text-idol hover:text-idol-dark hover:cursor-pointer">
                            dolchat
                        </div>
                    </div>

                    <div className="flex gap-6 text-sm items-center">
                        {isLoggedIn ? (
                            // 로그인 상태일 때
                            <>
                                <span className="font-semibold text-gray-700">{user?.nickname || '회원'}님</span>
                                <div className="rounded-md bg-gray-200 hover:bg-gray-300 transition">
                                    <button onClick={handleLogout} className="p-2 text-gray-700 w-[80px]">logout</button>
                                </div>
                            </>
                        ) : (
                            // 비로그인 상태일 때
                            <>
                                <div className="rounded-md bg-idol hover:bg-idol-dark transition">
                                    <button onClick={handleLogin} className="p-2 text-white w-[64px]">login</button>
                                </div>
                                <div className="rounded-md bg-idol hover:bg-idol-dark transition">
                                    <button onClick={handleLogin} className="p-2 text-white w-[64px]">register</button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </header>

            {/* 오버레이 배경 */}
            <AnimatePresence>
                {isMenuOpen && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.2 }}
                        className="fixed inset-0 bg-black/50 z-40"
                        onClick={closeMenu}
                    />
                )}
            </AnimatePresence>

            {/* 사이드바 드로어 */}
            <AnimatePresence>
                {isMenuOpen && (
                    <motion.div
                        initial={{ x: "-100%" }}
                        animate={{ x: 0 }}
                        exit={{ x: "-100%" }}
                        transition={{ type: "tween", duration: 0.3 }}
                        className="fixed top-0 left-0 h-full w-64 bg-white shadow-2xl z-50 flex flex-col"
                    >
                        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50">
                            <span className="text-xl font-bold text-idol">Menu</span>
                            <button onClick={closeMenu} className="text-gray-400 hover:text-gray-600 focus:outline-none">
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        <nav className="flex-1 overflow-y-auto py-4">
                            <ul className="space-y-2 px-4 text-gray-700">
                                <li>
                                    <Link to="/notices" onClick={closeMenu} className="block p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors">
                                        공지사항
                                    </Link>
                                </li>
                                <li>
                                    <Link to="/idol" onClick={closeMenu} className="block p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors">
                                        아이돌 페이지
                                    </Link>
                                </li>
                                <li>
                                    <Link to="/concert" onClick={closeMenu} className="block p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors">
                                        콘서트 페이지
                                    </Link>
                                </li>
                                <li>
                                    <Link to="/mypage" onClick={closeMenu} className="block p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors">
                                        마이페이지
                                    </Link>
                                </li>
                            </ul>
                        </nav>

                        <div className="p-6 border-t border-gray-100 bg-gray-50 text-sm">
                            {isLoggedIn ? (
                                <button onClick={handleLogout} className="w-full p-3 bg-white border border-gray-200 text-gray-600 rounded-xl hover:bg-gray-100 transition shadow-sm font-medium">로그아웃</button>
                            ) : (
                                <div className="space-y-3">
                                    <button onClick={handleLogin} className="w-full p-3 bg-idol text-white rounded-xl hover:bg-idol-dark transition shadow-md font-medium">로그인</button>
                                </div>
                            )}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </>
    );
};

export default Header;
