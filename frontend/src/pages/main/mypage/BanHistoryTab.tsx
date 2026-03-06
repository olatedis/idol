import React, { useEffect, useState } from "react";
import { api } from "../../../api/axios";

interface BanHistoryDto {
    id: number;
    userId: number;
    status: string;
    reason: string;
    createdAt: string;
}

const BanHistoryTab: React.FC = () => {
    const [history, setHistory] = useState<BanHistoryDto[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchHistory = async () => {
            try {
                const res = await api.get("/users/me/bans-history");
                setHistory(res.data);
            } catch (err) {
                console.error("Failed to fetch ban history", err);
            } finally {
                setLoading(false);
            }
        };

        fetchHistory();
    }, []);

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleString("ko-KR", {
            year: "numeric", month: "2-digit", day: "2-digit",
            hour: "2-digit", minute: "2-digit"
        });
    };

    if (loading) {
        return <div className="py-10 text-center text-gray-500">징계 내역을 불러오는 중...</div>;
    }

    if (history.length === 0) {
        return (
            <div className="py-16 text-center bg-gray-50 rounded-lg">
                <div className="text-gray-400 mb-2">
                    <svg className="w-12 h-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                </div>
                <p className="text-gray-600 font-medium">징계 내역이 없습니다.</p>
                <p className="text-sm text-gray-500 mt-1">깨끗하고 매너 있는 활동에 감사드립니다.</p>
            </div>
        );
    }

    return (
        <div className="space-y-6 animate-fade-in">
            <div className="mb-4">
                <h3 className="text-lg font-bold text-gray-800">내 징계 내역</h3>
                <p className="text-sm text-gray-500 mt-1">과거에 신고가 누적되어 제재를 받은 이력을 보여줍니다.</p>
            </div>

            <div className="space-y-4">
                {history.map((record, index) => (
                    <div key={record.id || index} className="p-4 bg-white border border-gray-200 rounded-xl shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
                        <div className="flex-1">
                            <div className="flex items-center gap-3 mb-2">
                                <span className={`px-2.5 py-1 text-xs font-bold rounded-full ${record.status === 'ACTIVE' ? 'bg-green-100 text-green-700' :
                                        record.status === 'RESTRICTED' ? 'bg-yellow-100 text-yellow-700' :
                                            'bg-red-100 text-red-700'
                                    }`}>
                                    {record.status === 'ACTIVE' ? '제재 해제 (ACTIVE)' :
                                        record.status === 'RESTRICTED' ? '활동 제한 (RESTRICTED)' :
                                            record.status === 'SUSPENDED' ? '일시 정지 (SUSPENDED)' :
                                                '영구 정지 (BANNED)'}
                                </span>
                                <span className="text-sm text-gray-500">{formatDate(record.createdAt)}</span>
                            </div>
                            <div className="text-gray-800 font-medium whitespace-pre-line">
                                사유: {record.reason}
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default BanHistoryTab;
