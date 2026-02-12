import React from "react";
import {useNavigate} from "react-router-dom";

const Header: React.FC = () => {
    const navigate = useNavigate();
    return (
        <header className="w-full flex items-center justify-between px-8 py-4">
            <div onClick={() => navigate("/")}
                 className="text-xl font-bold text-idol hover:text-idol-dark hover:cursor-pointer">dolchat
            </div>

            <div className="flex gap-6 text-sm">
                <button
                    className="
                        px-4 py-1.5
                        rounded-full
                        border border-gray-300
                        hover:bg-gray-100
                        transition
                    ">
                    Login
                </button>


                <button
                    className="
                        px-4 py-1.5
                        rounded-full
                        bg-[#1FBFB8]
                        text-white
                        hover:bg-[#17AFA8]
                        transition
                    ">
                    Register
                </button>
            </div>
        </header>
    );
};

export default Header;
