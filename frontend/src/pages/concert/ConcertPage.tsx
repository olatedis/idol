import React, { useEffect, useRef, useState } from "react";
import { useNavigate, useParams, useLocation } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";
import { AnimatePresence, motion } from "framer-motion";
import { showErrorToast } from "../../utils/alert";

import { api } from '../../api/axios';
import type { ConcertDto, SeatDto, SeatGrade } from "../../types/concert";
import { ConcertDetailModal } from "../../components/concert/ConcertDetailModal";

const PAGE_SIZE = 10;

const ConcertPage: React.FC = () => {
    const { groupId } = useParams<{ groupId?: string }>();
    const navigate = useNavigate();
    const location = useLocation();

    const { user } = useAuthStore();

    // 탭 상태: 진행중(OPEN) / 종료됨(CLOSED)
    type TabType = 'OPEN' | 'CLOSED';
    const [activeTab, setActiveTab] = useState<TabType>('OPEN');

    const [concerts, setConcerts] = useState<ConcertDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError] = useState("");

    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);

    const sentinelRef = useRef<HTMLDivElement | null>(null);
    const fetchingRef = useRef(false);


    // 모달 상태
    const [selectedConcert, setSelectedConcert] = useState<ConcertDto | null>(null);
    const [concertSeats, setConcertSeats] = useState<SeatDto[]>([]);
    const [seatsLoading, setSeatsLoading] = useState(false);
    const [isBookingModalOpen, setIsBookingModalOpen] = useState(false);
    const [selectedSeats, setSelectedSeats] = useState<number[]>([]);

    const formatKST = (iso?: string) => {
        if (!iso) return "";
        const kst = new Date(iso);
        return kst.toLocaleString("ko-KR", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
        });
    };

    // 콘서트가 종료되었는지 확인 (날짜 기반)
    const isConcertEnded = (concertDate?: string): boolean => {
        if (!concertDate) return false;
        const concert = new Date(concertDate);
        const now = new Date();
        return concert < now;
    };

    const getConcertStatusForFilter = (concert: ConcertDto): 'OPEN' | 'CLOSED' => {
        if (isConcertEnded(concert.concertDate)) {
            return 'CLOSED';
        }
        return 'OPEN';
    };
    const resetInfinite = () => {
        setHasMore(false); // observer 잠시 차단
        setConcerts([]);
        setPage(0);

        setTimeout(() => {
            setHasMore(true);
        }, 0);
    };


    const fetchPage = async (nextPage: number) => {
        if (fetchingRef.current) return;

        fetchingRef.current = true;

        try {
            const params = new URLSearchParams();
            params.set("page", String(nextPage));
            params.set("size", String(PAGE_SIZE));
            params.set("sort", "concertDate,desc");
            if (groupId) params.set("groupId", groupId);

            let url = `/concerts`;
            const qs = params.toString();
            if (qs) url += `?${qs}`;

            const res = await api.get(url);
            if (res.status !== 200) throw new Error("콘서트 목록 조회 실패");

            const data = res.data;
            let content: ConcertDto[] = [];
            let last = true;

            if (Array.isArray(data)) {
                content = data;
                last = true;
            } else {
                content = data.content ?? [];
                last = Boolean(data.last);
            }

            setConcerts((prev) => {
                const base = nextPage === 0 ? [] : prev;
                const map = new Map(base.map((c) => [c.id, c]));
                content.forEach((c) => map.set(c.id, c));
                return Array.from(map.values());
            });

            setHasMore(!last && content.length > 0);

        } finally {
            fetchingRef.current = false;
        }
    };

    const fetchConcertSeats = async (concertId: number) => {
        try {
            setSeatsLoading(true);
            const url = `/concerts/${concertId}/seats`;
            const res = await api.get(url);

            if (res.status !== 200) {
                throw new Error(`좌석 조회 실패 (${res.status})`);
            }

            const seats = res.data;

            if (Array.isArray(seats)) {
                setConcertSeats(seats as SeatDto[]);
            } else {
                setConcertSeats([]);
            }
        } catch (e) {
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
                await fetchPage(0);
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
    }, [groupId, activeTab]);

    useEffect(() => {
        const state = (location.state as { openConcertId?: number } | null) ?? null;
        if (!state?.openConcertId) return;

        const openFromGlobal = async () => {
            try {
                const res = await api.get(`/concerts/${state.openConcertId}`);
                if (res.status === 200) {
                    await onOpenDetail(res.data as ConcertDto);
                }
            } catch {
                // ignore
            }
        };

        openFromGlobal();
    }, [location.state]);

    useEffect(() => {
        if (page === 0) return;
        if (!hasMore) return;

        const controller = new AbortController();
        const run = async () => {
            setError("");
            try {
                setLoadingMore(true);
                await fetchPage(page);
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
                if (loading || loadingMore) return;
                if (fetchingRef.current) return;
                if (!hasMore) return;

                setPage((prev) => prev + 1);
            },
            { root: null, rootMargin: "200px", threshold: 0 }
        );

        io.observe(el);
        return () => io.disconnect();
    }, [hasMore, loading, loadingMore]);


    const onOpenDetail = async (concert: ConcertDto) => {
        setSelectedConcert(concert);
        await fetchConcertSeats(concert.id);
    };

    const onOpenBooking = () => {
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

    const onConfirmBooking = async () => {
        if (selectedSeats.length === 0) {
            showErrorToast("좌석을 선택해주세요.");
            return;
        }
        if (!selectedConcert) {
            showErrorToast("콘서트 정보가 없습니다.");
            return;
        }
        // 선택한 좌석 정보 준비
        const chosenSeats = concertSeats.filter((s) => selectedSeats.includes(s.id));
        const totalPrice = chosenSeats.reduce((sum, s) => sum + s.price, 0);

        // 로그인 확인
        if (!user || !user.userId) {
            showErrorToast('로그인이 필요합니다.');
            return;
        }

        // 즉시 좌석 락(예매) 요청 - Reservation API 호출
        try {
            const seatIds = chosenSeats.map((seat) => seat.id);
            const seatPrices = chosenSeats.map((seat) => seat.price);

            const res = await api.post(`/reservations/bulk`, {
                userId: user.userId,
                concertId: selectedConcert.id,
                seatIds,
                seatPrices,
                price: totalPrice,
            }, {
                headers: {
                    'X-User-Id': String(user.userId),
                }
            });

            if (res.status !== 200 && res.status !== 201) {
                throw new Error('예약 실패 (bulk)');
            }

            const reservationIds: number[] = res.data;

            // 이동: 결제 페이지로 reservationIds 포함하여 전달
            navigate('/payment', {
                state: {
                    domain: 'CONCERT',
                    concert: selectedConcert,
                    seats: chosenSeats,
                    totalPrice,
                    reservationIds,
                },
            });

            setIsBookingModalOpen(false);
        } catch (e: any) {
            console.log(e.message);
            showErrorToast('이미 선택된 자리입니다. 다른 좌석을 선택해주세요.');

        }
    };

    const onCreateConcert = () => {
        navigate("./create");
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-[var(--color-idol-bg)] via-white to-[var(--color-idol-bg)]">
            <main className="px-4 sm:px-6 pb-12 max-w-7xl mx-auto relative z-10">
                {/* 제목 영역 */}
                <div className="mb-8">
                    <h1 className="text-3xl font-black text-gray-800">콘서트 예매</h1>
                    <p className="text-gray-500 mt-2 font-medium">
                        우리 그룹의 콘서트를 확인하세요
                    </p>
                </div>

                {/* 배경 장식 */}
                <div className="hidden sm:block absolute top-20 left-10 w-72 h-72 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob" style={{ backgroundColor: 'var(--color-idol-dark)' }}></div>
                <div className="hidden sm:block absolute top-20 right-10 w-72 h-72 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-2000" style={{ backgroundColor: 'var(--color-idol)' }}></div>
                <div className="hidden sm:block absolute -bottom-8 left-40 w-72 h-72 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-4000" style={{ backgroundColor: 'var(--color-idol-point)' }}></div>

                <div className="space-y-4">
                    <div className="flex justify-between items-center flex-wrap gap-2">
                        <div className="text-lg font-semibold text-gray-900">콘서트 목록</div>
                        <div className="flex gap-2">
                            {user?.role === "AGENCY" && (
                                <button
                                    onClick={onCreateConcert}
                                    className="px-4 py-2 rounded-full bg-[var(--color-idol)] text-white text-sm font-semibold hover:opacity-90 transition"
                                >
                                    콘서트 등록
                                </button>
                            )}
                        </div>
                    </div>

                    {/* 탭 버튼 */}
                    <div className="bg-white/70 backdrop-blur-md p-1.5 rounded-full shadow-lg border border-white/50 flex max-w-md overflow-x-auto custom-scrollbar">
                        {(['OPEN', 'CLOSED'] as TabType[]).map((tab) => (
                            <button
                                key={tab}
                                onClick={() => setActiveTab(tab)}
                                className={`px-4 py-1.5 text-sm font-semibold rounded-full transition 
                                    ${activeTab === tab ? 'bg-[var(--color-idol)] text-white' : 'text-gray-700 hover:bg-gray-100'}`}
                            >
                                {tab === 'OPEN' ? '진행중' : '종료됨'}
                            </button>
                        ))}
                    </div>

                    {loading && <div className="text-sm text-gray-600">불러오는 중...</div>}
                    {error && <div className="text-sm text-red-600">{error}</div>}

                    {/* 카드 그리드 */}
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        <AnimatePresence>
                            {concerts
                                .filter((c) => getConcertStatusForFilter(c) === activeTab)
                                .map((c) => {
                                    const isEnded = isConcertEnded(c.concertDate);
                                    return (
                                        <motion.div
                                            layout
                                            initial={{ opacity: 0, scale: 0.9 }}
                                            animate={{ opacity: 1, scale: 1 }}
                                            exit={{ opacity: 0, scale: 0.9 }}
                                            whileHover={{ y: -4, scale: 1.02 }}
                                            transition={{ type: "spring", stiffness: 300, damping: 20 }}
                                            key={c.id}
                                            onClick={() => onOpenDetail(c)}
                                            className="bg-white/70 backdrop-blur-md rounded-2xl shadow-xl overflow-hidden cursor-pointer border border-white hover:shadow-lg transition-all relative group"
                                        >
                                            <div className="h-2 w-full bg-gradient-to-r from-[var(--color-idol)] via-[var(--color-idol-point)] to-[var(--color-idol-dark)]"></div>
                                            <div className="p-6">
                                                <div className="flex justify-between items-center mb-3">
                                                    <span className={`px-3 py-1 rounded-full text-xs font-bold tracking-wider
                                                        ${isEnded ? 'bg-gray-100 text-gray-500' : 'bg-[var(--color-idol)] text-white'}`}>
                                                        {isEnded ? '⚫ 종료됨' : '🟢 진행중'}
                                                    </span>
                                                    <span className="text-gray-400 text-xs">
                                                        {formatKST(c.concertDate).split(' ')[0]}
                                                    </span>
                                                </div>
                                                <h3 className="text-xl font-black text-gray-800 mb-2 group-hover:text-gray-900 transition-colors line-clamp-2">
                                                    {c.title}
                                                </h3>
                                                <p className="text-gray-600 text-sm line-clamp-2">
                                                    {c.venue}
                                                </p>
                                            </div>
                                        </motion.div>
                                    );
                                })}
                        </AnimatePresence>
                    </div>

                    {/* no items message */}
                    {!loading && concerts.filter((c) => getConcertStatusForFilter(c) === activeTab).length === 0 && (
                        <div className="flex flex-col items-center justify-center py-20 opacity-50">
                            <div className="text-6xl mb-4">📭</div>
                            <div className="text-xl font-medium text-gray-500">조회 가능한 콘서트가 없습니다.</div>
                        </div>
                    )}
                </div>

                    <div ref={sentinelRef} className="h-10" />
                    {loadingMore && <div className="text-sm text-gray-600">더 불러오는 중...</div>}
                    {!loading && !loadingMore && concerts.length > 0 && !hasMore && (
                        <div className="text-sm text-gray-500 text-center py-2"></div>
                    )}
            </main>

            {/* 콘서트 상세 모달 */}
            <AnimatePresence>
                {selectedConcert && (
                    <ConcertDetailModal
                        concert={selectedConcert}
                        seats={concertSeats}
                        seatsLoading={seatsLoading}
                        onConfirmBooking={onOpenBooking}
                        onClose={() => {
                            setSelectedConcert(null);
                            setIsBookingModalOpen(false);
                            setSelectedSeats([]);
                        }}
                        bookingEnabled={true}
                    />
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
                                                                if (!seat.reservedBy && !seat.locked) {
                                                                    toggleSeatSelection(seat.id);
                                                                }
                                                            }}
                                                            disabled={!!seat.reservedBy || seat.locked}
                                                            className={`
                                                                p-2 rounded border-2 font-bold text-sm transition flex flex-col items-center
                                                                ${
                                                                    seat.reservedBy || seat.locked
                                                                        ? "border-gray-300 bg-gray-100 text-gray-400 cursor-not-allowed"
                                                                        : selectedSeats.includes(seat.id)
                                                                        ? "border-[var(--color-idol)] bg-[var(--color-idol)] text-white"
                                                                        : "border-gray-300 bg-white text-gray-900 hover:border-[var(--color-idol)]"
                                                                }
                                                            `}
                                                        >
                                                            <span>{seat.seatNumber}</span>
                                                            <span className="text-[10px] text-gray-500 mt-1">
                                                                {seat.price.toLocaleString()}원
                                                            </span>
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
                                        className="w-full py-3 bg-[var(--color-idol)] text-white font-bold rounded-lg hover:opacity-90 disabled:bg-gray-300 transition"
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
