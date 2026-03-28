import React from "react";
import type { ConcertDetail, SeatDto } from "../../types/concert";

type Props = {
    concert: ConcertDetail;
    seats: SeatDto[];
    seatsLoading: boolean;
    onConfirmBooking?: () => void;
    onClose: () => void;
    bookingEnabled?: boolean;
    bookingLabel?: string;
};

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

export const ConcertDetailModal: React.FC<Props> = ({
    concert,
    seats,
    seatsLoading,
    onConfirmBooking,
    onClose,
    bookingEnabled = false,
    bookingLabel = "예매하기",
}) => {
    const isSaleOpen = () => {
        if (!concert.ticketSaleDate) return true;
        const now = new Date();
        const sale = new Date(concert.ticketSaleDate);
        return now >= sale;
    };

    const canBook = bookingEnabled && isSaleOpen();

    const getSeatCountByGrade = (grade: string) =>
        seats.filter((s) => s.grade === grade && !s.reservedBy && !s.locked).length;

    const getSeatsByGrade = (grade: string) => seats.filter((s) => s.grade === grade);

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
            onClick={onClose}
        >
            <div
                className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="p-6 sm:p-8">
                    <div className="flex justify-between items-start mb-6">
                        <div>
                            <h2 className="text-2xl font-bold text-gray-900">{concert.title}</h2>
                            <p className="text-gray-600 mt-1">{concert.venue}</p>
                        </div>
                        <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl">
                            ✕
                        </button>
                    </div>

                    <div className="space-y-4 mb-6">
                        <div className="flex flex-col sm:flex-row sm:justify-between gap-1">
                            <span className="text-gray-600">일시</span>
                            <span className="font-medium">{formatKST(concert.concertDate)}</span>
                        </div>
                        {concert.ticketSaleDate && (
                            <div className="flex flex-col sm:flex-row sm:justify-between gap-1">
                                <span className="text-gray-600">티켓 예매시작</span>
                                <span className="font-medium">{formatKST(concert.ticketSaleDate)}</span>
                            </div>
                        )}
                        {concert.description && (
                            <div className="border-t pt-4">
                                <p className="text-sm text-gray-700">{concert.description}</p>
                            </div>
                        )}
                    </div>

                    {concert.img && (
                        <div className="my-4">
                            <img
                                src={concert.img}
                                alt={concert.title}
                                className="w-full max-h-48 sm:max-h-64 object-cover rounded-xl"
                            />
                        </div>
                    )}

                    <div className="border-t pt-6 mb-6">
                        <h3 className="font-bold text-gray-900 mb-4">좌석 현황</h3>
                        {seatsLoading ? (
                            <div className="text-sm text-gray-500">좌석 정보 로딩중...</div>
                        ) : (
                            (() => {
                                const grades = Array.from(new Set(seats.map((s) => s.grade))) as string[];
                                if (grades.length === 0) {
                                    return <div className="text-sm text-gray-500">좌석 정보가 없습니다.</div>;
                                }
                                return (
                                    <div className="grid grid-cols-2 gap-4">
                                        {grades.map((grade) => (
                                            <div key={grade} className="flex justify-between items-center p-3 bg-gray-50 rounded-lg">
                                                <span className="font-medium text-gray-900">{grade}석</span>
                                                <span className="text-sm text-gray-600">
                                                    {getSeatCountByGrade(grade)}석 / {getSeatsByGrade(grade).length}석
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                );
                            })()
                        )}
                    </div>

                    {onConfirmBooking && (
                        <button
                            onClick={onConfirmBooking}
                            disabled={!canBook}
                            className="w-full py-3 bg-[var(--color-idol)] text-white font-bold rounded-lg hover:opacity-90 disabled:bg-gray-300 transition"
                        >
                            {canBook ? bookingLabel : "티켓 예매 시작 전입니다"}
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};
