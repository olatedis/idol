import React, { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "../../../stores/authStore";
import { AnimatePresence, motion } from "framer-motion";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
const PAGE_SIZE = 20;

type SeatGrade = "VIP" | "R" | "S" | "A";

type SeatDto = {
    id: number;
    seatNumber: string;
    grade: SeatGrade;
    price: number;
    locked: boolean;
    lockedBy?: number | null;
};

type ConcertDto = {
    id: number;
    groupId?: number | null;
    title: string;
    description?: string;
    venue: string;
    concertDate: string;
    startTime?: string;
    price?: number;
    totalTickets?: number;
    status: "OPEN" | "SOLD_OUT" | "CLOSED" | string;
    createdAt?: string;
    agencyId: number;
};

const ConcertPage: React.FC = () => {
    const { groupId } = useParams<{ groupId?: string }>();
    const navigate = useNavigate();

    const { user } = useAuthStore();

    const [concerts, setConcerts] = useState<ConcertDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError] = useState("");

    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [totalElements, setTotalElements] = useState<number | null>(null);

    const sentinelRef = useRef<HTMLDivElement | null>(null);

    const accessToken = localStorage.getItem("accessToken");

    // 모달 상태
    const [selectedConcert, setSelectedConcert] = useState<ConcertDto | null>(null);
    const [concertSeats, setConcertSeats] = useState<SeatDto[]>([]);
    const [seatsLoading, setSeatsLoading] = useState(false);
    const [isBookingModalOpen, setIsBookingModalOpen] = useState(false);
    const [selectedSeats, setSelectedSeats] = useState<number[]>([]);

    const formatKST = (iso?: string) => {
        if (!iso) return "";
        const d = new Date(iso);
        const kst = new Date(d.getTime() + 9 * 60 * 60 * 1000);
        return kst.toLocaleString("ko-KR", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
        });
    };

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
        params.set("sort", "concertDate,desc");
        if (groupId) params.set("groupId", groupId);

        let url = `${API_BASE_URL}/concerts`;
        const qs = params.toString();
        if (qs) url += `?${qs}`;
        const res = await fetch(url, { signal });
        if (!res.ok) throw new Error("콘서트 목록 조회 실패");

        const data = await res.json();
        let content: ConcertDto[] = [];
        let last = true;

        if (Array.isArray(data)) {
            content = data as ConcertDto[];
            last = true;
        } else {
            content = (data.content ?? []) as ConcertDto[];
            last = Boolean(data.last);
            if (typeof data.totalElements === "number") {
                setTotalElements(data.totalElements);
            }
        }

        if (nextPage === 0) {
            setConcerts(content);
        } else {
            setConcerts((prev) => [...prev, ...content]);
        }

        setHasMore(!last && content.length > 0);
    };

    const fetchConcertSeats = async (concertId: number) => {
        if (!API_BASE_URL) return;
        try {
            setSeatsLoading(true);
            const res = await fetch(`${API_BASE_URL}/concerts/${concertId}/seats`);
            if (!res.ok) throw new Error("좌석 조회 실패");
            const seats = await res.json();
            setConcertSeats(seats);
        } catch (e) {
            console.error("좌석 조회 실패:", e);
            setConcertSeats([]);
        } finally {
            setSeatsLoading(false);
        }
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

    const requireLogin = () => {
        if (accessToken) return true;
        alert("로그인이 필요합니다.");
        return false;
    };

    const onOpenDetail = async (concert: ConcertDto) => {
        setSelectedConcert(concert);
        await fetchConcertSeats(concert.id);
    };

    const onOpenBooking = () => {
        if (!requireLogin()) return;
        setIsBookingModalOpen(true);
        setSelectedSeats([]);
    };

    const toggleSeatSelection = (seatId: number) => {
        setSelectedSeats((prev) =>
            prev.includes(seatId) ? prev.filter((s) => s !== seatId) : [...prev, seatId]
        );
    };

    const getSeatsByGrade = (grade: SeatGrade): SeatDto[] => {
        return concertSeats.filter((s) => s.grade === grade);
    };

    const getSeatCountByGrade = (grade: SeatGrade): number => {
        return concertSeats.filter((s) => s.grade === grade && !s.locked).length;
    };

    const onConfirmBooking = async () => {
        if (selectedSeats.length === 0) {
            alert("좌석을 선택해주세요.");
            return;
        }
        // TODO: 예매 처리
        alert(`선택한 좌석: ${selectedSeats.length}개`);
        setIsBookingModalOpen(false);
    };

    const onCreateConcert = () => {
        if (!requireLogin()) return;
        navigate("./create");
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50">
            <main className="pt-[100px] px-6 pb-12 max-w-7xl mx-auto relative z-10">
                {/* 제목 영역 */}
                <div className="mb-8">
                    <h1 className="text-3xl font-black text-gray-800">콘서트 예매소</h1>
                    <p className="text-gray-500 mt-2 font-medium">
                        {groupId ? "우리 그룹의 콘서트를 확인하세요" : "모든 그룹의 콘서트를 확인하세요"}
                    </p>
                </div>

                {/* 배경 장식 */}
                <div className="absolute top-20 left-10 w-72 h-72 bg-purple-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob"></div>
                <div className="absolute top-20 right-10 w-72 h-72 bg-pink-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-2000"></div>
                <div className="absolute -bottom-8 left-40 w-72 h-72 bg-indigo-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-4000"></div>

                <div className="space-y-4">
                    <div className="flex justify-between items-center flex-wrap gap-2">
                        <div className="text-lg font-semibold text-gray-900">콘서트 목록</div>
                        <div className="flex gap-2">
                            <button
                                onClick={scrollTop}
                                className="px-3 py-2 rounded-full text-sm font-semibold border border-gray-200 hover:bg-gray-50"
                            >
                                ↑
                            </button>
                            {user?.role === "AGENCY" && (
                                <button
                                    onClick={onCreateConcert}
                                    className="px-4 py-2 rounded-full bg-[#1FBFB8] text-white text-sm font-semibold hover:bg-[#17AFA8]"
                                >
                                    콘서트 등록
                                </button>
                            )}
                        </div>
                    </div>

                    {loading && <div className="text-sm text-gray-600">불러오는 중...</div>}
                    {error && <div className="text-sm text-red-600">{error}</div>}

                    <div className="border border-gray-200 rounded-2xl overflow-hidden bg-white">
                        <div className="grid grid-cols-[90px_1fr_160px_140px_90px] px-4 py-3 text-sm font-semibold text-gray-700 bg-gray-50 border-b border-gray-200">
                            <div className="text-left">번호</div>
                            <div className="text-left">공연명</div>
                            <div className="text-left">장소</div>
                            <div className="text-left">일시</div>
                            <div className="text-right">상세</div>
                        </div>

                        {!loading && concerts.length === 0 && (
                            <div className="px-4 py-6 text-sm text-gray-600">등록된 콘서트가 없습니다.</div>
                        )}

                        {!loading &&
                            concerts.map((c, idx) => (
                                <div
                                    key={c.id}
                                    className="w-full text-left grid grid-cols-[90px_1fr_160px_140px_90px] px-4 py-3 border-b border-gray-100 hover:bg-gray-50 transition-colors items-center"
                                >
                                    <div className="text-sm text-gray-900 tabular-nums">{rowNo(idx)}</div>
                                    <div className="min-w-0">
                                        <div className="text-sm font-semibold text-gray-900 truncate">{c.title}</div>
                                    </div>
                                    <div className="text-sm text-gray-700">{c.venue}</div>
                                    <div className="text-sm text-gray-600">{formatKST(c.concertDate)}</div>
                                    <div className="text-right">
                                        <button
                                            onClick={() => onOpenDetail(c)}
                                            className="px-3 py-1.5 rounded-full text-sm font-medium bg-idol text-white hover:bg-idol/90"
                                        >
                                            상세보기
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
            </main>

            {/* 콘서트 상세 모달 */}
            <AnimatePresence>
                {selectedConcert && (
                    <div
                        className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
                        onClick={() => setSelectedConcert(null)}
                    >
                        <motion.div
                            className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto"
                            onClick={(e) => e.stopPropagation()}
                            initial={{ scale: 0.95, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            exit={{ scale: 0.95, opacity: 0 }}
                        >
                            <div className="p-6 sm:p-8">
                                <div className="flex justify-between items-start mb-6">
                                    <div>
                                        <h2 className="text-2xl font-bold text-gray-900">{selectedConcert.title}</h2>
                                        <p className="text-gray-600 mt-1">{selectedConcert.venue}</p>
                                    </div>
                                    <button
                                        onClick={() => setSelectedConcert(null)}
                                        className="text-gray-400 hover:text-gray-600 text-2xl"
                                    >
                                        ✕
                                    </button>
                                </div>

                                <div className="space-y-4 mb-6">
                                    <div className="flex justify-between">
                                        <span className="text-gray-600">일시</span>
                                        <span className="font-medium">{formatKST(selectedConcert.concertDate)}</span>
                                    </div>
                                    {selectedConcert.description && (
                                        <div className="border-t pt-4">
                                            <p className="text-sm text-gray-700">{selectedConcert.description}</p>
                                        </div>
                                    )}
                                </div>

                                {/* 좌석 정보 */}
                                <div className="border-t pt-6 mb-6">
                                    <h3 className="font-bold text-gray-900 mb-4">좌석 현황</h3>
                                    {seatsLoading ? (
                                        <div className="text-sm text-gray-500">좌석 정보 로딩중...</div>
                                    ) : (
                                        <div className="grid grid-cols-2 gap-4">
                                            {(["VIP", "R", "S", "A"] as SeatGrade[]).map((grade) => (
                                                <div
                                                    key={grade}
                                                    className="flex justify-between items-center p-3 bg-gray-50 rounded-lg"
                                                >
                                                    <span className="font-medium text-gray-900">{grade}석</span>
                                                    <span className="text-sm text-gray-600">
                                                        {getSeatCountByGrade(grade)}석 / {getSeatsByGrade(grade).length}석
                                                    </span>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                <button
                                    onClick={onOpenBooking}
                                    className="w-full py-3 bg-idol text-white font-bold rounded-lg hover:bg-idol/90 transition"
                                >
                                    예매하기
                                </button>
                            </div>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>

            {/* 예매 모달 */}
            <AnimatePresence>
                {isBookingModalOpen && selectedConcert && (
                    <div
                        className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
                        onClick={() => setIsBookingModalOpen(false)}
                    >
                        <motion.div
                            className="bg-white rounded-2xl shadow-2xl max-w-3xl w-full max-h-[90vh] overflow-y-auto"
                            onClick={(e) => e.stopPropagation()}
                            initial={{ scale: 0.95, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            exit={{ scale: 0.95, opacity: 0 }}
                        >
                            <div className="p-6 sm:p-8">
                                <div className="flex justify-between items-center mb-6">
                                    <h2 className="text-2xl font-bold text-gray-900">좌석 선택</h2>
                                    <button
                                        onClick={() => setIsBookingModalOpen(false)}
                                        className="text-gray-400 hover:text-gray-600 text-2xl"
                                    >
                                        ✕
                                    </button>
                                </div>

                                <div className="mb-6 space-y-6">
                                    {(["VIP", "R", "S", "A"] as SeatGrade[]).map((grade) => {
                                        const seatsOfGrade = getSeatsByGrade(grade);
                                        if (seatsOfGrade.length === 0) return null;

                                        return (
                                            <div key={grade}>
                                                <h3 className="font-bold text-gray-900 mb-3">{grade}석</h3>
                                                <div className="grid gap-2" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(50px, 1fr))" }}>
                                                    {seatsOfGrade.map((seat) => (
                                                        <button
                                                            key={seat.id}
                                                            onClick={() => {
                                                                if (!seat.locked) {
                                                                    toggleSeatSelection(seat.id);
                                                                }
                                                            }}
                                                            disabled={seat.locked}
                                                            className={`
                                                                p-2 rounded border-2 font-bold text-sm transition
                                                                ${
                                                                    seat.locked
                                                                        ? "border-gray-300 bg-gray-100 text-gray-400 cursor-not-allowed"
                                                                        : selectedSeats.includes(seat.id)
                                                                        ? "border-idol bg-idol text-white"
                                                                        : "border-gray-300 bg-white text-gray-900 hover:border-idol"
                                                                }
                                                            `}
                                                        >
                                                            {seat.seatNumber}
                                                        </button>
                                                    ))}
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>

                                <div className="border-t pt-6">
                                    <div className="mb-6 p-4 bg-gray-50 rounded-lg">
                                        <p className="text-sm text-gray-600 mb-2">선택한 좌석</p>
                                        <p className="font-bold text-gray-900">
                                            {selectedSeats.length > 0
                                                ? concertSeats
                                                      .filter((s) => selectedSeats.includes(s.id))
                                                      .map((s) => s.seatNumber)
                                                      .join(", ")
                                                : "선택된 좌석이 없습니다"}
                                        </p>
                                    </div>

                                    <button
                                        onClick={onConfirmBooking}
                                        disabled={selectedSeats.length === 0}
                                        className="w-full py-3 bg-idol text-white font-bold rounded-lg hover:bg-idol/90 disabled:bg-gray-300 transition"
                                    >
                                        {selectedSeats.length > 0
                                            ? `${selectedSeats.length}개 좌석 예매하기`
                                            : "좌석을 선택해주세요"}
                                    </button>
                                </div>
                            </div>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default ConcertPage;
