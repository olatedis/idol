import React, { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate, useParams } from "react-router-dom";
import Header from "../main/Header";
import { useAuthStore } from "../../stores/authStore";
import { api } from "../../api/axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

type GroupSubscriptionDto = {
    groupId: number;
};

const DefaultServicePage: React.FC = () => {
    const navigate = useNavigate();
    const { groupId } = useParams();
    const location = useLocation();

    const isChatRoute = location.pathname.endsWith("/chat");

    const isBoardPath = location.pathname.includes("/board");

    const [guardChecking, setGuardChecking] = useState(true);

    useEffect(() => {
        const run = async () => {
            // Zustand store에서 토큰과 유저 정보를 가져옵니다.
            const { accessToken, user } = useAuthStore.getState();

            if (!accessToken) {
                // 로그아웃 시나 권한 만료 시 불필요한 alert를 제거하고 메인으로 리다이렉트합니다.
                navigate("/");
                return;
            }

            // 아이돌 권한, 관리자, 또는 소속사인 경우 소속 그룹 구독 검증을 프리패스합니다.
            if (user?.role === "IDOL" || user?.role === "ADMIN" || user?.role === "AGENCY") {
                setGuardChecking(false);
                return;
            }

            if (!groupId) {
                alert("잘못된 접근입니다. (groupId 없음)");
                navigate(-1);
                return;
            }

            if (!API_BASE_URL) {
                alert("VITE_API_BASE_URL이 설정되어 있지 않습니다.");
                navigate(-1);
                return;
            }

            try {
                let isSubscribed = false;
                const gid = Number(groupId);

                if (user?.role === "AGENCY") {
                    // 에이전시는 관리하는 그룹 목록을 조회합니다.
                    const res = await api.get("/groups/managed");
                    const managedGroups = res.data;
                    const managedGroupIds: number[] = Array.isArray(managedGroups)
                        ? managedGroups.map((g: any) => Number(g.id))
                        : [];
                    isSubscribed = managedGroupIds.includes(gid);
                } else {
                    // 일반 유저는 구독 정보를 조회합니다.
                    const res = await api.get(`/subscriptions/groups/me`);
                    const json = res.data;
                    const subscribedGroupIds: number[] = Array.isArray(json)
                        ? (json as GroupSubscriptionDto[])
                            .map((x) => Number((x as any)?.groupId))
                            .filter((v) => Number.isFinite(v))
                        : [];
                    isSubscribed = subscribedGroupIds.includes(gid);
                }

                if (!isSubscribed) {
                    alert(user?.role === "AGENCY" ? "관리 권한이 없는 그룹입니다." : "구독하지 않은 그룹입니다.");
                    navigate(-1);
                    return;
                }

                setGuardChecking(false);
            } catch {
                alert("권한 확인 중 오류가 발생했습니다.");
                navigate(-1);
            }
        };

        run();
    }, [API_BASE_URL, groupId, navigate]);

    const tabs = [
        { label: "게시판", to: "board" },
        { label: "투표", to: "vote" },
        { label: "콘서트", to: "concert" },
        { label: "채팅", to: "chat" },
    ];

    if (guardChecking) {
        return (
            <div className="min-h-screen bg-white">
                <Header />
                <div className="max-w-6xl mx-auto px-4 py-6 text-sm text-gray-400">확인 중...</div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-white">
            <Header />

            <div className={`pt-[80px] ${isChatRoute ? "h-[100dvh] flex flex-col" : ""}`}>
                {/* 세미 헤더 */}
                <div
                    className={`${
                        isChatRoute
                            ? "bg-white/80 backdrop-blur-md border-b border-gray-200/50 pb-4 pt-2 shrink-0"
                            : "sticky top-[80px] z-10 bg-white/80 backdrop-blur-md border-b border-gray-200/50 shadow-[0_4px_30px_rgba(0,0,0,0.05)] pb-4 pt-2"
                    }`}
                >
                    <nav
                        className="
                        flex justify-center gap-2 sm:gap-4
                        max-w-4xl mx-auto px-4
                        "
                    >
                        {tabs.map((t) => {
                            const forceActive = t.to === "board" ? isBoardPath : false; // [추가]

                            return (
                                <NavLink
                                    key={t.label}
                                    to={t.to}
                                    className={({ isActive }) => {
                                        const active = forceActive || isActive;

                                        return [
                                            "flex-1 max-w-[120px] text-center select-none transition-all duration-300 ease-out",
                                            "py-2.5 sm:py-3",
                                            "text-sm sm:text-base font-bold tracking-wide",
                                            "rounded-full shadow-sm",
                                            active
                                                ? "bg-gradient-to-r from-[var(--color-idol)] to-[var(--color-idol-dark)] text-white shadow-md shadow-[var(--color-idol-point)]/30 ring-2 ring-[var(--color-idol-bg)] transform scale-105"
                                                : "bg-white text-gray-500 border border-gray-200 hover:bg-gray-50 hover:text-gray-800 hover:shadow-md hover:-translate-y-0.5",
                                        ].join(" ");
                                    }}
                                >
                                    {t.label}
                                </NavLink>
                            );
                        })}
                    </nav>
                </div>

                {/* Content */}
                <div className={`max-w-6xl mx-auto px-4 w-full ${isChatRoute ? "flex-1 flex flex-col py-0" : "py-6"}`}>
                    <Outlet />
                </div>
            </div>
        </div>
    );
};

export default DefaultServicePage;