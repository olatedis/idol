import React, { useEffect, useState } from "react";
import { api } from "../../../api/axios";

interface AdminHistoryModalProps {
    userId: number;
    type: "reports" | "bans";
    onClose: () => void;
}

interface ReportDto {
    id: number;
    reporterId: number;
    targetUserId: number;
    reason: string;
    description: string;
    createdAt: string;
}

interface BanHistoryDto {
    id: number;
    userId: number;
    status: string;
    reason: string;
    createdAt: string;
}

const AdminHistoryModal: React.FC<AdminHistoryModalProps> = ({ userId, type, onClose }) => {
    const [data, setData] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const endpoint = type === "reports"
                    ? `/admin/users/${userId}/reports-history`
                    : `/admin/users/${userId}/bans-history`;
                const res = await api.get(endpoint);
                setData(res.data);
            } catch (err) {
                console.error("Failed to fetch history", err);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, [userId, type]);

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleString("ko-KR");
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-fade-in">
            <div className="bg-white rounded-2xl p-6 w-full max-w-2xl max-h-[80vh] flex flex-col shadow-xl">
                <div className="flex justify-between items-center border-b border-gray-100 pb-4 mb-4">
                    <h3 className="text-lg font-bold text-gray-900">
                        {type === "reports" ? "피신고 이력 상세" : "징계 이력 상세"} (User ID: {userId})
                    </h3>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-2xl font-light">&times;</button>
                </div>

                <div className="overflow-y-auto flex-1 pr-2">
                    {loading ? (
                        <div className="py-10 text-center text-gray-500">데이터를 불러오는 중...</div>
                    ) : data.length === 0 ? (
                        <div className="py-10 text-center text-gray-500 bg-gray-50 rounded-lg">이력이 존재하지 않습니다.</div>
                    ) : (
                        <div className="space-y-3">
                            {data.map((item, idx) => (
                                <div key={item.id || idx} className="bg-gray-50 p-4 rounded-lg border border-gray-200">
                                    <div className="flex justify-between items-start mb-2">
                                        <div className="text-xs text-gray-400">{formatDate(item.createdAt)}</div>
                                        {type === "bans" && (
                                            <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${item.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                                                }`}>
                                                {item.status}
                                            </span>
                                        )}
                                    </div>
                                    <div className="font-semibold text-gray-800 text-sm mb-1">사유: {item.reason}</div>
                                    {type === "reports" && item.description && (
                                        <div className="text-xs text-gray-600 mt-2 bg-white p-2 border border-gray-100 rounded">
                                            {item.description}
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default AdminHistoryModal;
