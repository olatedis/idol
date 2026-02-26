import React, { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Header from "../../main/Header";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
const PAGE_SIZE = 20;

type ConcertDto = {
    concertId: number;
    groupId?: number | null;
    title: string;
    venue: string;
    date: string; // ISO string or formatted
    startTime?: string;
    endTime?: string;
    price: number;
    totalTickets: number;
    remainingTickets: number;
    status: "OPEN" | "SOLD_OUT" | "CLOSED" | string;
    createdAt?: string;
};

const ConcertPage: React.FC = () => {
    const { groupId } = useParams<{ groupId?: string }>();
    const navigate = useNavigate();

    const [concerts, setConcerts] = useState<ConcertDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError] = useState("");

    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [totalElements, setTotalElements] = useState<number | null>(null);

    const sentinelRef = useRef<HTMLDivElement | null>(null);

    // TODO: replace with Zustand store when login flow is unified
    const accessToken = localStorage.getItem("accessToken");

    const resetInfinite = () => {
        setConcerts([]);
        setPage(0);
        setHasMore(true);
        setTotalElements(null);
    };

    const rowNo = (idx: number) => {
        if (typeof totalElements === "number") return totalElements - idx;
        return concerts.length - idx;
    };

    const fetchPage = async (nextPage: number, signal?: AbortSignal) => {
        if (!API_BASE_URL) return;

        const params = new URLSearchParams();
        params.set("page", String(nextPage));
        params.set("size", String(PAGE_SIZE));
        params.set("sort", "date,desc");
        if (groupId) params.set("groupId", groupId);

        const url = `${API_BASE_URL}/concerts?${params.toString()}`;
        const res = await fetch(url, { signal });
        if (!res.ok) throw new Error("콘서트 목록 조회 실패");

        const data = await res.json();
        const content = (data.content ?? []) as ConcertDto[];

        if (nextPage === 0) setConcerts(content);
        else setConcerts((prev) => [...prev, ...content]);

        if (typeof data.totalElements === "number") setTotalElements(data.totalElements);
        const last = Boolean(data.last);
        setHasMore(!last && content.length > 0);
    };

    useEffect(() => {
        const controller = new AbortController();

        const run = async () => {
            setError("");
            try {
                setLoading(true);
                resetInfinite();
                await fetchPage(0, controller.signal);
            } catch (e: any) {
                if (e?.name === "AbortError") return;
                setError(e?.message || "콘서트 목록 조회 실패");
                setConcerts([]);
                setHasMore(false);
            } finally {
                setLoading(false);
            }
        };

        run();
        return () => controller.abort();
    }, [API_BASE_URL, groupId]);

    useEffect(() => {
        if (page === 0) return;
        if (!hasMore) return;

        const controller = new AbortController();
        const run = async () => {
            setError("");
            try {
                setLoadingMore(true);
                await fetchPage(page, controller.signal);
            } catch (e: any) {
                if (e?.name === "AbortError") return;
                setError(e?.message || "추가 로딩 실패");
                setHasMore(false);
            } finally {
                setLoadingMore(false);
            }
        };

        run();
        return () => controller.abort();
    }, [page, hasMore]);

    useEffect(() => {
        const el = sentinelRef.current;
        if (!el) return;
        if (!hasMore) return;

        const io = new IntersectionObserver(
            (entries) => {
                const first = entries[0];
                if (!first.isIntersecting) return;
                if (loading) return;
                if (loadingMore) return;
                setPage((prev) => prev + 1);
            },
            { root: null, rootMargin: "200px", threshold: 0 }
        );

        io.observe(el);
        return () => io.disconnect();
    }, [hasMore, loading, loadingMore]);

    const scrollTop = () => window.scrollTo({ top: 0, behavior: "smooth" });

    const requireLoginOrStop = () => {
        if (accessToken) return true;
        alert("로그인이 필요합니다.");
        return false;
    };

    const onClickRow = (c: ConcertDto) => {
        navigate(`./${c.concertId}`);
    };

    const onClickBook = (c: ConcertDto) => {
        if (!requireLoginOrStop()) return;
        if (c.remainingTickets <= 0) {
            alert("예매 가능한 좌석이 없습니다.");
            return;
        }
        navigate(`./booking/${c.concertId}`);
    };

    const onClickCreate = () => {
        if (!requireLoginOrStop()) return;
        navigate(`./create`);
    };

    return (
        <div className="space-y-4">
            <div className="flex justify-between items-center flex-wrap gap-2">
                <div className="text-lg font-semibold text-gray-900">콘서트 예매</div>

                <div className="flex gap-2">
                    <button
                        type="button"
                        onClick={scrollTop}
                        className="px-3 py-2 rounded-full text-sm font-semibold border border-gray-200 hover:bg-gray-50"
                    >
                        ↑
                    </button>

                    <button
                        type="button"
                        onClick={onClickCreate}
                        className="px-4 py-2 rounded-full bg-[#1FBFB8] text-white text-sm font-semibold hover:bg-[#17AFA8]"
                    >
                        콘서트 등록
                    </button>
                </div>
            </div>

            {loading && <div className="text-sm text-gray-600">불러오는 중...</div>}
            {error && <div className="text-sm text-red-600">{error}</div>}

            <div className="border border-gray-200 rounded-2xl overflow-hidden bg-white">
                <div className="grid grid-cols-[90px_1fr_160px_140px_120px_90px] px-4 py-3 text-sm font-semibold text-gray-700 bg-gray-50 border-b border-gray-200">
                    <div className="text-left">번호</div>
                    <div className="text-left">공연</div>
                    <div className="text-left">장소</div>
                    <div className="text-left">일시</div>
                    <div className="text-right">남은 좌석</div>
                    <div className="text-right">예매</div>
                </div>

                {!loading && concerts.length === 0 && (
                    <div className="px-4 py-6 text-sm text-gray-600">등록된 콘서트가 없습니다.</div>
                )}

                {!loading &&
                    concerts.map((c, idx) => (
                        <div
                            key={c.concertId}
                            className="w-full text-left grid grid-cols-[90px_1fr_160px_140px_120px_90px] px-4 py-3 border-b border-gray-100 hover:bg-gray-50 transition-colors items-center"
                        >
                            <div className="text-sm text-gray-900 tabular-nums">{rowNo(idx)}</div>
                            <div className="min-w-0">
                                <div className="text-sm font-semibold text-gray-900 truncate">{c.title}</div>
                            </div>
                            <div className="text-sm text-gray-700">{c.venue}</div>
                            <div className="text-sm text-gray-600">{c.date}</div>
                            <div className="text-sm text-gray-700 text-right tabular-nums">{c.remainingTickets}/{c.totalTickets}</div>
                            <div className="text-right">
                                <button
                                    onClick={() => onClickBook(c)}
                                    className={`px-3 py-1.5 rounded-full text-sm font-medium ${c.remainingTickets > 0 ? "bg-idol text-white" : "bg-gray-200 text-gray-500 cursor-not-allowed"}`}
                                >
                                    예매
                                </button>
                            </div>
                        </div>
                    ))}
            </div>

            <div ref={sentinelRef} className="h-10" />
            {loadingMore && <div className="text-sm text-gray-600">더 불러오는 중...</div>}
            {!loading && !loadingMore && concerts.length > 0 && !hasMore && (
                <div className="text-sm text-gray-500 text-center py-2">마지막 콘서트입니다.</div>
            )}
        </div>
    );
};

export default ConcertPage;
