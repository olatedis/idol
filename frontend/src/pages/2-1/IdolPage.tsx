import React, { useEffect, useRef, useState } from "react";
import Header from "../main/Header";
import { useNavigate } from "react-router-dom";
import { api } from "../../api/axios";
import { useAuthStore } from "../../stores/authStore";
import SignupModal from "../../components/auth/SignupModal";

const CARD_WIDTH = 176;

interface IdolDto {
    idolId: number;
    userId: number;
    username: string;
    stageName: string;
    profileImage: string;
    agencyId: number;
    agencyName: string;
    status: "ACTIVE" | string;
}

interface GroupSubscriptionDto {
    subscriptionId: number;
    userId: number;
    groupId: number;
    status: string;
    startedAt: string;
    expiredAt: string;
    autoRenew: boolean;
}

const IdolPage: React.FC = () => {
    const navigate = useNavigate();
    const scrollRef = useRef<HTMLDivElement>(null);
    
    // user뿐만 아니라 accessToken도 확인하여 로그인 상태 판단 (Hydration 이슈 방지)
    const { user, accessToken } = useAuthStore();
    const isLoggedIn = !!user || !!accessToken;

    const [allIdols, setAllIdols] = useState<IdolDto[]>([]);
    const [subscribedIdols, setSubscribedIdols] = useState<IdolDto[]>([]);
    const [showLeft, setShowLeft] = useState(false);
    const [showRight, setShowRight] = useState(false);
    
    // 모달 상태
    const [isSignupOpen, setIsSignupOpen] = useState(false);

    // 전체 아이돌 조회
    const fetchAllIdols = async () => {
        try {
            const { data } = await api.get("/idols");
            setAllIdols(data);
        } catch (error) {
            console.error("전체 아이돌 조회 실패:", error);
        }
    };

    // 구독 목록 조회 → idol 상세 조회
    const fetchSubscriptions = async () => {
        if (!isLoggedIn) return;

        try {
            const { data: subs } = await api.get<GroupSubscriptionDto[]>("/subscriptions/me");

            const idolPromises = subs.map(sub =>
                api.get(`/idols/${sub.groupId}`).then(res => res.data)
            );

            const idolResults = await Promise.all(idolPromises);
            setSubscribedIdols(idolResults);
        } catch (error) {
            console.error("구독 목록 조회 실패:", error);
        }
    };

    useEffect(() => {
        fetchAllIdols();
        fetchSubscriptions();
    }, [isLoggedIn]);

    // 캐러셀 버튼 제어
    const checkOverflow = () => {
        const el = scrollRef.current;
        if (!el) return;

        setShowLeft(el.scrollLeft > 0);
        setShowRight(el.scrollLeft + el.clientWidth < el.scrollWidth);
    };

    const scrollLeft = () => {
        scrollRef.current?.scrollBy({ left: -CARD_WIDTH, behavior: "smooth" });
    };

    const scrollRight = () => {
        scrollRef.current?.scrollBy({ left: CARD_WIDTH, behavior: "smooth" });
    };

    const handleClick = (idol: IdolDto) => {
        if (idol.status !== "ACTIVE") return;
        navigate(`/group/${idol.idolId}`);
    };

    const IdolCard = ({ idol }: { idol: IdolDto }) => {
        const isInactive = idol.status !== "ACTIVE";

        return (
            <div
                onClick={() => handleClick(idol)}
                className={`flex-shrink-0 flex flex-col items-center
                    ${isInactive ? "cursor-not-allowed opacity-70" : "cursor-pointer"}`}
            >
                <img
                    src={idol.profileImage}
                    alt={idol.stageName}
                    className={`w-40 h-40 rounded-full border-2 border-idol-point object-cover
                        ${isInactive ? "grayscale" : ""}`}
                />
                <p className="mt-4 text-sm">{idol.stageName}</p>
            </div>
        );
    };

    return (
        <div className="min-h-screen bg-idol-bg">
            <Header />

            <main className="pt-[80px] px-6 pb-12">

                {/* 구독중인 아이돌 */}
                <section className="my-8 relative">
                    <div className="bg-idol rounded-lg py-3 text-center text-white font-semibold mb-8">
                        구독중인 아이돌
                    </div>

                    {isLoggedIn ? (
                        <div className="relative">
                            {showLeft && (
                                <button
                                    onClick={scrollLeft}
                                    className="absolute left-0 top-1/2 -translate-y-1/2 z-10
                                               bg-white shadow-md rounded-full w-10 h-10">
                                    ◀
                                </button>
                            )}

                            {showRight && (
                                <button
                                    onClick={scrollRight}
                                    className="absolute right-0 top-1/2 -translate-y-1/2 z-10
                                               bg-white shadow-md rounded-full w-10 h-10">
                                    ▶
                                </button>
                            )}

                            <div
                                ref={scrollRef}
                                onScroll={checkOverflow}
                                className="flex gap-8 overflow-hidden"
                            >
                                {subscribedIdols.length > 0 ? (
                                    subscribedIdols.map(idol => (
                                        <IdolCard key={idol.idolId} idol={idol} />
                                    ))
                                ) : (
                                    <div className="w-full text-center py-10 text-gray-500">
                                        구독 중인 아이돌이 없습니다.
                                    </div>
                                )}
                            </div>
                        </div>
                    ) : (
                        // 로그인/회원가입 유도 UI
                        <div className="flex flex-col items-center justify-center py-12 bg-white/50 rounded-lg border border-idol-point/20">
                            <p className="text-gray-600 mb-6 font-medium">로그인하고 내가 구독한 아이돌을 확인해보세요!</p>
                            <div className="flex gap-4">
                                <button
                                    onClick={() => navigate('/', { state: { scrollToLogin: true } })}
                                    className="px-8 py-2.5 bg-idol text-white rounded-full hover:opacity-90 transition shadow-sm font-semibold"
                                >
                                    로그인
                                </button>
                                <button
                                    onClick={() => setIsSignupOpen(true)}
                                    className="px-8 py-2.5 bg-white text-idol border border-idol rounded-full hover:bg-gray-50 transition shadow-sm font-semibold"
                                >
                                    회원가입
                                </button>
                            </div>
                        </div>
                    )}
                </section>

                {/* 전체 아이돌 */}
                <section>
                    <div className="bg-idol rounded-lg text-white py-3 text-center font-semibold mb-8">
                        전체 아이돌
                    </div>

                    <div className="
                        grid
                        grid-cols-2
                        sm:grid-cols-3
                        md:grid-cols-4
                        lg:grid-cols-5
                        xl:grid-cols-6
                        gap-y-12 gap-x-8
                    ">
                        {allIdols.map(idol => (
                            <IdolCard key={idol.idolId} idol={idol} />
                        ))}
                    </div>
                </section>

            </main>

            {/* 회원가입 모달 */}
            <SignupModal 
                isOpen={isSignupOpen} 
                onClose={() => setIsSignupOpen(false)}
                onSwitchToLogin={() => {
                    setIsSignupOpen(false);
                    navigate('/', { state: { scrollToLogin: true } });
                }}
            />
        </div>
    );
};

export default IdolPage;
