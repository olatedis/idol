import React from "react";

const Header: React.FC = () => {
    return (
        <header className="w-full flex items-center justify-between px-8 py-4 border-b">
            <span className="text-lg font-bold">dolchat</span>

            <div className="flex gap-6 text-sm">
                <button>login</button>
                <button>register</button>
            </div>
        </header>
    );
};

export default Header;
