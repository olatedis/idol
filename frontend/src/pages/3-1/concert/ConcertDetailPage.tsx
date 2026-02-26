import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "../../../stores/authStore";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

type ConcertDto = {
    concertId: number;
    groupId?: number | null;
    title: string;
    venue: string;
    date: string;
    startTime?: string;
    endTime?: string;
    price: number;
    totalTickets: number;
    remainingTickets: number;
    status: string;
};

const ConcertDetailPage: React.FC = () => {
    const { concertId } = useParams<{ concertId?: string }>();
    const navigate = useNavigate();

    const accessToken = useAuthStore.getState().accessToken;

    const [data, setData] = useState<ConcertDto | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

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

    const fetchDetail = async () => {
        if (!API_BASE_URL || !concertId) return;

        try {
            setLoading(true);
            const res = await fetch(`${API_BASE_URL}/concerts/${concertId}`);
            if (!res.ok) throw new Error("콘서트 상세 조회 실패");
            const json = (await res.json()) as ConcertDto;
            setData(json);
        } catch (e: any) {
            setError(e?.message || "콘서트 상세 조회 실패");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchDetail();
    }, [concertId]);

    const requireLogin = () => {
        if (accessToken) return true;
        alert("로그인이 필요합니다.");
        return false;
    };

    const onBook = () => {
        if (!requireLogin()) return;
        if (data && data.remainingTickets <= 0) {
            alert("예매 가능한 좌석이 없습니다.");
            return;
        }
        navigate(`../booking/${concertId}`);
    };

    if (loading) return <div>Loading...</div>;
    if (error) return <div className="text-red-600">{error}</div>;
    if (!data) return <div>콘서트 정보를 불러올 수 없습니다.</div>;

    return (
        <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50">
            <main className="pt-[100px] px-6 pb-12 max-w-3xl mx-auto relative z-10">
                <div className="mb-8">
                    <h1 className="text-3xl font-black text-gray-800">{data.title}</h1>
                </div>

                <div className="absolute top-20 left-10 w-72 h-72 bg-purple-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob"></div>
                <div className="absolute top-20 right-10 w-72 h-72 bg-pink-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-2000"></div>
                <div className="absolute -bottom-8 left-40 w-72 h-72 bg-indigo-300 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-4000"></div>

                <div className="space-y-4">
                    <div className="text-sm text-gray-600">장소: {data.venue}</div>
                    <div className="text-sm text-gray-600">일시: {formatKST(data.date)}</div>
                    <div className="text-sm text-gray-600">가격: {data.price.toLocaleString()}원</div>
                    <div className="text-sm text-gray-600">
                        잔여 좌석: {data.remainingTickets}/{data.totalTickets}
                    </div>

                    <button
                        onClick={onBook}
                        className={`px-4 py-2 rounded-full text-white font-semibold ${
                            data.remainingTickets > 0 ? "bg-idol" : "bg-gray-300 cursor-not-allowed"
                        }`}
                    >
                        예매하기
                    </button>
                </div>
            </main>
        </div>
    );
};

export default ConcertDetailPage;