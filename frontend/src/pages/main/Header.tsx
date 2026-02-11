import React from "react";
import {useNavigate} from "react-router-dom";

const Header: React.FC = () => {
    const navigate = useNavigate();
    const handleLogin = () => {
        navigate("/", { state: { scrollToLogin: true } });
    };

    return (
        <header className="w-full flex items-center justify-between px-8 py-4 fixed top-0 z-50 bg-white">
            <div onClick={() => navigate("/")}
                 className="text-xl font-bold text-idol hover:text-idol-dark hover:cursor-pointer">dolchat</div>

            <div className="flex gap-6 text-sm">
                <div className="rounded-md  bg-idol">
                    <button onClick={handleLogin} className="p-2 text-white w-[64px]">login</button>
                </div>
                <div className="rounded-md bg-idol">
                    <button className="p-2 text-white w-[64px]">register</button>
                </div>
            </div>
        </header>
    );
};

export default Header;
