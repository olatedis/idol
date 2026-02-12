import React, { useEffect, useRef, useState } from "react";
import Header from "../main/Header";
import { useNavigate } from "react-router-dom";

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

    const [allIdols, setAllIdols] = useState<IdolDto[]>([]);
    const [subscribedIdols, setSubscribedIdols] = useState<IdolDto[]>([]);
    const [showLeft, setShowLeft] = useState(false);
    const [showRight, setShowRight] = useState(false);

    const token = localStorage.getItem("accessToken");

    // 전체 아이돌 조회
    const fetchAllIdols = async () => {
        const res = await fetch("http://localhost:8000/idols");
        const data = await res.json();
        setAllIdols(data);
    };

    // 구독 목록 조회 → idol 상세 조회
    const fetchSubscriptions = async () => {
        if (!token) return;

        const res = await fetch("http://localhost:8000/subscriptions/me", {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        });

        const subs: GroupSubscriptionDto[] = await res.json();

        const idolPromises = subs.map(sub =>
            fetch(`http://localhost:8080/idols/${sub.groupId}`)
                .then(res => res.json())
        );

        const idolResults = await Promise.all(idolPromises);
        setSubscribedIdols(idolResults);
    };

    useEffect(() => {
        fetchAllIdols();
        fetchSubscriptions();
    }, []);

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
                            {subscribedIdols.map(idol => (
                                <IdolCard key={idol.idolId} idol={idol} />
                            ))}
                        </div>

                    </div>
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
        </div>
    );
};

export default IdolPage;
