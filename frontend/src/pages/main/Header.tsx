import React from "react";
import {useNavigate} from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";

const Header: React.FC = () => {
    const navigate = useNavigate();
    const { user, logout, accessToken } = useAuthStore(); // accessToken 추가
    const isLoggedIn = !!user || !!accessToken; // 로그인 상태 체크 강화

    const handleLogin = () => {
        navigate("/", { state: { scrollToLogin: true } });
    };

    const handleLogout = () => {
        logout();
        alert("로그아웃되었습니다.");
        navigate("/");
    };

    return (
        <header className="w-full flex items-center justify-between px-8 py-4 fixed top-0 z-50 bg-white shadow-sm">
            <div onClick={() => navigate("/")}
                 className="text-xl font-bold text-idol hover:text-idol-dark hover:cursor-pointer">dolchat</div>

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
        </header>
    );
};

export default Header;
