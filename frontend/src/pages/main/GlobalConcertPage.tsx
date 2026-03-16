import React, { useEffect, useState } from "react";
import Header from "../../pages/main/Header";
import { api } from "../../api/axios";
import { ConcertDetailModal } from "../../components/concert/ConcertDetailModal";
import type { ConcertDetail } from "../../types/concert";

interface Concert {
    concertId: number;
    title: string;
    description: string;
    startDate: string;
    endDate: string;
    imageUrl: string;
    address: string;
    price: number;
    capacity: number;
}

const GlobalConcertPage: React.FC = () => {
    const [concerts, setConcerts] = useState<Concert[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [selectedConcert, setSelectedConcert] = useState<ConcertDetail | null>(null);

    useEffect(() => {
        const fetchConcerts = async () => {
            try {
                // groupId를 주지 않으면 전체 콘서트 응답
                const response = await api.get("/concerts");
                setConcerts(response.data);
            } catch (error) {
            } finally {
                setIsLoading(false);
            }
        };

        fetchConcerts();
    }, []);

    const fetchImage = (id: number): string => {
        return `http://localhost:8080/concerts/${id}/image`;
    }

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Header />
            <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24">
                <div className="mb-10 text-center sm:text-left">
                    <h1 className="text-3xl sm:text-4xl font-extrabold text-gray-900 tracking-tight mb-3">
                        전체 콘서트 일정
                    </h1>
                    <p className="text-gray-500 text-lg sm:text-xl">
                        놓칠 수 없는 무대, 지금 바로 확인하세요.
                    </p>
                </div>

                {isLoading ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-8">
                        {[1, 2, 3, 4].map(n => (
                            <div key={n} className="bg-white rounded-3xl overflow-hidden shadow-sm animate-pulse border border-gray-100 flex flex-col h-full">
                                <div className="aspect-[4/5] bg-gray-200"></div>
                                <div className="p-6">
                                    <div className="h-6 bg-gray-200 rounded-md w-3/4 mb-3"></div>
                                    <div className="h-4 bg-gray-200 rounded-md w-1/2 mb-4"></div>
                                    <div className="h-4 bg-gray-200 rounded-md w-full mb-2"></div>
                                    <div className="h-4 bg-gray-200 rounded-md w-2/3"></div>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : concerts.length > 0 ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-8">
                        {concerts.map((concert) => (
                            <div
                                key={concert.concertId}
                                className="group bg-white rounded-3xl overflow-hidden shadow-sm hover:shadow-2xl transition-all duration-300 border border-gray-100 flex flex-col h-full cursor-pointer hover:-translate-y-2"
                                onClick={() =>
                                    setSelectedConcert({
                                        id: concert.concertId,
                                        title: concert.title,
                                        description: concert.description,
                                        venue: concert.address,
                                        concertDate: concert.startDate,
                                        ticketSaleDate: undefined,
                                    })
                                }
                            >
                                <div className="relative aspect-[4/5] overflow-hidden bg-gray-100 shrink-0">
                                    <img
                                        src={concert.imageUrl ? concert.imageUrl : fetchImage(concert.concertId)}
                                        alt={concert.title}
                                        className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                                        onError={(e) => {
                                            const target = e.target as HTMLImageElement;
                                            target.src = "https://via.placeholder.com/400x500?text=No+Image";
                                        }}
                                    />
                                    {/* 날짜 뱃지 */}
                                    <div className="absolute top-4 right-4 bg-black/70 backdrop-blur-md text-white text-xs font-bold px-3 py-1.5 rounded-full shadow-lg">
                                        {new Date(concert.startDate).toLocaleDateString()}
                                    </div>
                                    {/* 오버레이 그라데이션 */}
                                    <div className="absolute inset-x-0 bottom-0 h-1/2 bg-gradient-to-t from-black/60 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 pointer-events-none"></div>
                                </div>
                                <div className="p-6 flex flex-col flex-1">
                                    <div className="flex-1">
                                        <h3 className="text-xl font-bold text-gray-900 group-hover:text-idol transition-colors mb-2 line-clamp-1">
                                            {concert.title}
                                        </h3>
                                        <p className="text-gray-500 text-sm line-clamp-2 mb-4">
                                            {concert.description || "상세 설명이 없습니다."}
                                        </p>
                                    </div>
                                    <div className="space-y-2 text-sm text-gray-600 border-t border-gray-100 pt-4 mt-auto">
                                        <div className="flex items-center gap-2 font-medium">
                                            <svg className="w-4 h-4 text-idol" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                                            <span className="truncate">{concert.address}</span>
                                        </div>
                                        <div className="flex justify-between items-center text-gray-800 font-bold">
                                            <span>&#8361; {concert.price}</span>
                                            <span className="text-xs text-gray-400 font-normal border border-gray-200 px-2 py-1 rounded-md">{concert.capacity}석</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="bg-white rounded-3xl p-16 text-center border border-gray-100 shadow-sm mt-8">
                        <svg className="w-16 h-16 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" />
                        </svg>
                        <h3 className="text-lg font-bold text-gray-900 mb-2">예정된 콘서트가 없습니다</h3>
                        <p className="text-gray-500">현재 계획된 콘서트 일정이 존재하지 않습니다.</p>
                    </div>
                )}
            </main>

            {selectedConcert && (
                <ConcertDetailModal
                    concert={selectedConcert}
                    seats={[]}
                    seatsLoading={false}
                    onClose={() => setSelectedConcert(null)}
                    bookingEnabled={false}
                />
            )}
        </div>
    );
};

export default GlobalConcertPage;
