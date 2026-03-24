import React, { useEffect, useRef, useState } from "react";
import Header from "../main/Header";
import { useNavigate } from "react-router-dom";
import { api } from "../../api/axios";
import { useAuthStore } from "../../stores/authStore";
import SignupModal from "../../components/auth/SignupModal";
import SafeImage from "../../components/common/SafeImage";

interface GroupDto {
    groupId: number;
    name: string;
    groupImage: string;
    agencyId: number;
    agencyName: string;
    members?: any[];
}

interface SubscriptionDto {
    subscriptionId: number;
    userId: number;
    idolId: number;
    idolStageName: string;
    idolImage?: string;
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

    const [allGroups, setAllGroups] = useState<GroupDto[]>([]);
    const [subscribedGroups, setSubscribedGroups] = useState<GroupDto[]>([]);
    const [subscribedIdols, setSubscribedIdols] = useState<SubscriptionDto[]>([]);
    const [showLeft, setShowLeft] = useState(false);
    const [showRight, setShowRight] = useState(false);
    const [cardsPerView, setCardsPerView] = useState(4);
    const [cardWidth, setCardWidth] = useState(0);

    // 모달 상태
    const [isSignupOpen, setIsSignupOpen] = useState(false);

    // 전체 그룹 조회
    const fetchAllGroups = async () => {
        try {
            const { data } = await api.get<GroupDto[]>("/groups");
            setAllGroups(data);
        } catch (error) {
        }
    };

    // 구독 목록 또는 관리 목록 조회
    const fetchGroupSubscriptions = async () => {
        if (!isLoggedIn) return;

        try {
            if (user?.role === "AGENCY") {
                // 에이전시 계정은 관리 중인 그룹 목록을 서버에서 직접 조회
                const { data: managedGroups } = await api.get<GroupDto[]>("/groups/managed");
                setSubscribedGroups(managedGroups);
            } else {
                // 일반/아이돌 계정은 구독한 아이돌과 그룹 목록 조회
                const [idolRes, groupRes] = await Promise.all([
                    api.get<SubscriptionDto[]>("/subscriptions/me"),
                    api.get<any[]>("/subscriptions/groups/me"),
                ]);

                setSubscribedIdols(idolRes.data);

                // 그룹 데이터 변환
                const groupResults: GroupDto[] = (groupRes.data ?? []).map(sub => ({
                    groupId: sub.groupId,
                    name: sub.groupName,
                    groupImage: sub.groupImage,
                    agencyId: 0,
                    agencyName: ""
                }));

                setSubscribedGroups(groupResults);
            }
        } catch (error) {
        }
    };

    useEffect(() => {
        fetchAllGroups();
        fetchGroupSubscriptions();
    }, [isLoggedIn]);

    useEffect(() => {
        // 데이터 로드 후 또는 cardWidth 변경 후 오버플로우 체크
        setTimeout(() => {
            checkOverflow();
        }, 100);
    }, [subscribedGroups.length, subscribedIdols.length, cardWidth]);

    // 캐러셀 버튼 제어
    const calculateCardsPerView = () => {
        const width = window.innerWidth;
        if (width < 640) return 2;      // sm 이하: 2개
        if (width < 768) return 3;      // sm-md: 3개
        if (width < 1024) return 4;     // md-lg: 4개
        if (width < 1280) return 5;     // lg-xl: 5개
        return 6;                        // xl 이상: 6개
    };

    useEffect(() => {
        const handleResize = () => {
            const newCardsPerView = calculateCardsPerView();
            setCardsPerView(newCardsPerView);
            
            if (scrollRef.current) {
                const containerWidth = scrollRef.current.clientWidth;
                const calculatedCardWidth = (containerWidth - (cardsPerView - 1) * 32) / cardsPerView; // gap-8 = 32px
                setCardWidth(calculatedCardWidth);
            }
        };

        handleResize();
        window.addEventListener("resize", handleResize);
        return () => window.removeEventListener("resize", handleResize);
    }, []);

    const checkOverflow = () => {
        const el = scrollRef.current;
        if (!el) return;

        setShowLeft(el.scrollLeft > 0);
        setShowRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 10);
    };

    const scrollLeft = () => {
        if (scrollRef.current && cardWidth > 0) {
            scrollRef.current.scrollBy({ 
                left: -(cardWidth + 32), // gap-8 = 32px
                behavior: "smooth" 
            });
        }
    };

    const scrollRight = () => {
        if (scrollRef.current && cardWidth > 0) {
            scrollRef.current.scrollBy({ 
                left: cardWidth + 32, // gap-8 = 32px
                behavior: "smooth" 
            });
        }
    };

    const handleClick = (group: GroupDto) => {
        navigate(`/group/${group.groupId}`);
    };

    const handleIdolCardClick = async (idol: SubscriptionDto) => {
        try {
            const { data: idolInfo } = await api.get(`/idols/${idol.idolId}`);
            if (idolInfo.groupId) {
                navigate(`/group/${idolInfo.groupId}`);
            }
        } catch (error) {
            console.error("아이돌 정보 조회 실패:", error);
        }
    };

    const GroupCard = ({ group }: { group: GroupDto }) => {
        return (
            <div
                onClick={() => handleClick(group)}
                className="flex-shrink-0 flex flex-col items-center cursor-pointer hover:scale-105 transition-transform"
                style={{ width: cardWidth > 0 ? `${cardWidth}px` : 'auto' }}
            >
                <SafeImage
                    src={group.groupImage}
                    alt={group.name}
                    className="w-32 h-32 rounded-full border border-idol-point object-cover shadow-md"
                    text={group.name}
                />
                <p className="mt-4 text-sm font-medium text-gray-800 text-center line-clamp-2">{group.name}</p>
            </div>
        );
    };

    const IdolCard = ({ idol }: { idol: SubscriptionDto }) => {
        return (
            <div
                onClick={() => handleIdolCardClick(idol)}
                className="flex-shrink-0 flex flex-col items-center cursor-pointer hover:scale-105 transition-transform"
                style={{ width: cardWidth > 0 ? `${cardWidth}px` : 'auto' }}
            >
                <SafeImage
                    src={idol.idolImage || ''}
                    alt={idol.idolStageName}
                    className="w-32 h-32 rounded-full border border-idol-point object-cover shadow-md"
                    text={idol.idolStageName}
                />
                <p className="mt-4 text-sm font-medium text-gray-800 text-center line-clamp-2">{idol.idolStageName}</p>
            </div>
        );
    };

    return (
        <div className="min-h-screen bg-idol-bg">
            <Header />

            <main className="pt-[80px] px-6 pb-12">

                {/* 아이돌 구독 페이지로 이동 */}
                <div className="mb-6 text-right">
                    <button
                        onClick={() => navigate('/idol/subscribe')}
                        className="px-4 py-2 rounded-full bg-idol-point text-white hover:opacity-90 transition"
                    >아이돌 구독하기</button>
                </div>

                {/* 구독중인 그룹/아이돌 */}
                <section className="my-8 relative">
                    <div className="bg-idol rounded-lg py-3 text-center text-white font-semibold mb-8">
                        {user?.role === "AGENCY" ? "관리중인 그룹" : "구독중인 그룹과 아이돌"}
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
                                onLoad={checkOverflow}
                                className="flex gap-8 overflow-x-auto overflow-y-hidden py-4 scroll-smooth"
                            >
                                {user?.role === "AGENCY" ? (
                                    subscribedGroups.length > 0 ? (
                                        subscribedGroups.map(group => (
                                            <GroupCard key={group.groupId} group={group} />
                                        ))
                                    ) : (
                                        <div className="w-full text-center py-10 text-gray-500">
                                            관리 중인 그룹이 없습니다.
                                        </div>
                                    )
                                ) : (
                                    subscribedGroups.length > 0 || subscribedIdols.length > 0 ? (
                                        <>
                                            {subscribedGroups.map(group => (
                                                <GroupCard key={`group-${group.groupId}`} group={group} />
                                            ))}
                                            {subscribedIdols.map(idol => (
                                                <IdolCard key={`idol-${idol.subscriptionId}`} idol={idol} />
                                            ))}
                                        </>
                                    ) : (
                                        <div className="w-full text-center py-10 text-gray-500">
                                            구독 중인 그룹과 아이돌이 없습니다.
                                        </div>
                                    )
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

                {/* 전체 그룹 */}
                <section>
                    <div className="bg-idol rounded-lg text-white py-3 text-center font-semibold mb-8">
                        전체 그룹
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
                        {allGroups.map(group => (
                            <GroupCard key={group.groupId} group={group} />
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