import React from "react";
import {useNavigate} from "react-router-dom";

const Header: React.FC = () => {
    const navigate = useNavigate();
    return (
        <header className="w-full flex items-center justify-between px-8 py-4">
            <div onClick={(event ) => navigate("/")}
                 className="text-xl font-bold text-idol hover:text-idol-dark hover:cursor-pointer">dolchat</div>

            <div className="flex gap-6 text-sm">
                <div className="rounded-md  bg-idol">
                    <button className="p-2 text-white">login</button>
                </div>
                <div className="rounded-md bg-idol">
                    <button className="p-2 text-white">register</button>
                </div>
            </div>
        </header>
    );
};

export default Header;
