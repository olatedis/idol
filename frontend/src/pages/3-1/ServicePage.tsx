import React from "react";
import {NavLink, Outlet, useParams} from "react-router-dom";
import Header from "../test/Header";

const ServicePage: React.FC = () => {
    const {idolId} = useParams();

    const tabs = [
        {label: "게시판", to: `/idol/${idolId}/board`},
        {label: "투표", to: `/idol/${idolId}/vote`},
        {label: "콘서트", to: `/idol/${idolId}/concert`},
        {label: "채팅", to: `/idol/${idolId}/chat`},
    ];

    return (
        <div className="min-h-screen bg-white">
            <Header/>

            {/* 세미 헤더 */}
            <div className="sticky top-0 z-10 bg-white">

                <nav
                    className="
              grid grid-cols-4 w-full
              border border-gray-200
              rounded-2xl
              shadow-sm
              overflow-hidden
              mt-3
            "
                >
                    {tabs.map((t) => (
                        <NavLink
                            key={t.label}
                            to={t.to}
                            className={({isActive}) =>
                                [
                                    "w-full text-center select-none",
                                    "py-[clamp(10px,1.4vw,14px)]",
                                    "text-[clamp(20px,1.2vw,16px)] font-semibold",
                                    "border-r border-gray-200 last:border-r-0",
                                    isActive
                                        ? "bg-[#1FBFB8] text-white hover:bg-[#17AFA8]"
                                        : "bg-white text-gray-800 hover:bg-gray-200",
                                ].join(" ")
                            }
                        >
                            {t.label}
                        </NavLink>
                    ))}
                </nav>

            </div>

            {/* Content */}
            <div className="max-w-6xl mx-auto px-4 py-6">
                <Outlet/>
            </div>
        </div>
    );
};

export default ServicePage;
