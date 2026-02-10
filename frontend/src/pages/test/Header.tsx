import React from "react";

const Header: React.FC = () => {
    return (
        <header className="w-full flex items-center justify-between px-8 py-4 border-b">
            <span className="text-lg font-bold hover:text-idol">dolchat</span>

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
