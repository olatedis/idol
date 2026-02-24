import React, { useEffect, useState } from "react";
import { NavLink, Outlet, useNavigate, useParams } from "react-router-dom";
import Header from "../main/Header";

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
            // TODO: 로그인 연동되면 accessToken 저장 방식/키 확정
            const accessToken = localStorage.getItem("accessToken");

            if (!accessToken) {
                alert("로그인이 필요합니다.");

                // TODO: 로그인 페이지 라우트 생기면 아래로 교체
                // navigate("/login");

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
                const res = await fetch(`${API_BASE_URL}/subscriptions/groups/me`, {
                    method: "GET",
                    headers: {
                        Authorization: `Bearer ${accessToken}`,
                    },
                });

                if (!res.ok) {
                    alert("구독 정보를 확인할 수 없습니다.");
                    navigate(-1);
                    return;
                }

                const json = (await res.json()) as unknown;

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

            {/* 세미 헤더 */}
            <div className="sticky top-0 z-10 bg-white">
                <nav
                    className="
                    grid grid-cols-4 gap-3
                    w-full px-6
                    mt-3
                    "
                >
                    {tabs.map((t) => (
                        <NavLink
                            key={t.label}
                            to={t.to}
                            className={({ isActive }) =>
                                [
                                    "w-full text-center select-none",
                                    "py-[clamp(4px,0.9vw,14px)]",
                                    "text-[clamp(19px,1.2vw,16px)] font-semibold",
                                    "rounded-xl border border-gray-200",
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
                <Outlet />
            </div>
        </div>
    );
};

export default GroupServicePage;