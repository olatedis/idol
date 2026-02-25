import React, { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate, useParams } from "react-router-dom";
import Header from "../main/Header";
import { useAuthStore } from "../../stores/authStore";
import { api } from "../../api/axios";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

type GroupSubscriptionDto = {
    groupId: number;
};

const GroupServicePage: React.FC = () => {
    const navigate = useNavigate();
    const { groupId } = useParams();

    const [guardChecking, setGuardChecking] = useState(true);

    useEffect(() => {
        const run = async () => {
            // Zustand store에서 토큰을 가져옵니다.
            const { accessToken } = useAuthStore.getState();

            if (!accessToken) {
                alert("로그인이 필요합니다.");

                navigate(-1);
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
                const res = await api.get(`/subscriptions/groups/me`);

                // Axios는 기본값으로 성공(2xx) 시 res.data에 JSON이 반환됨
                const json = res.data;

                const subscribedGroupIds: number[] = Array.isArray(json)
                    ? (json as GroupSubscriptionDto[])
                        .map((x) => Number((x as any)?.groupId))
                        .filter((v) => Number.isFinite(v))
                    : [];

                const gid = Number(groupId);
                const isSubscribed = subscribedGroupIds.includes(gid);

                if (!isSubscribed) {
                    alert("구독하지 않은 그룹입니다.");

                    // TODO: 구독 안내/구독 유도 페이지 라우트가 생기면 그쪽으로 이동
                    // navigate(`/groups/${gid}/subscribe`);

                    // 임시 UX: 이전 화면(2-1)로 되돌림
                    navigate(-1);
                    return;
                }

                setGuardChecking(false);
            } catch {
                alert("구독 확인 중 오류가 발생했습니다.");
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
                <div className="max-w-6xl mx-auto px-4 py-6 text-sm text-gray-600">
                    확인 중...
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-white">
            <Header />

            <div className="pt-[80px]">
                {/* 세미 헤더 */}
                <div className="sticky top-[80px] z-10 bg-white/80 backdrop-blur-md border-b border-gray-200/50 shadow-[0_4px_30px_rgba(0,0,0,0.05)] pb-4 pt-2">
                    <nav
                        className="
                        flex justify-center gap-2 sm:gap-4
                        max-w-4xl mx-auto px-4
                        mt-2
                        "
                    >
                        {tabs.map((t) => (
                            <NavLink
                                key={t.label}
                                to={t.to}
                                className={({ isActive }) =>
                                    [
                                        "flex-1 max-w-[120px] text-center select-none transition-all duration-300 ease-out",
                                        "py-2.5 sm:py-3",
                                        "text-sm sm:text-base font-bold tracking-wide",
                                        "rounded-full shadow-sm",
                                        isActive
                                            ? "bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-indigo-200 shadow-md ring-2 ring-indigo-100 transform scale-105"
                                            : "bg-white text-gray-500 border border-gray-200 hover:bg-gray-50 hover:text-gray-800 hover:shadow-md hover:-translate-y-0.5",
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
                    <Outlet />
                </div>
            </div>
        </div>
    );
};

export default GroupServicePage;