import React, { useEffect, useState } from "react";
import { api } from "../../../api/axios";
import Swal from 'sweetalert2';
import AdminHistoryModal from "./AdminHistoryModal";

interface AdminUserDto {
    userId: number;
    username: string;
    email: string;
    nickname: string;
    role: string;
    reportCount: number;
    status: string;
    suspendedUntil?: string;
}

const AdminReportQueue: React.FC = () => {
    const [users, setUsers] = useState<AdminUserDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedUser, setSelectedUser] = useState<number | null>(null);
    const [modalType, setModalType] = useState<"reports" | "bans">("reports");

    // 상태 변경 폼 상태
    const [actionForms, setActionForms] = useState<Record<number, { newStatus: string, durationDays: number | "", reason: string }>>({});

    const fetchQueue = async () => {
        try {
            const res = await api.get("/admin/users/reports");
            setUsers(res.data);
            const initialForms: Record<number, any> = {};
            res.data.forEach((u: AdminUserDto) => {
                initialForms[u.userId] = { newStatus: "SUSPENDED", durationDays: 7, reason: "" };
            });
            setActionForms(initialForms);
        } catch (err) {
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchQueue();
    }, []);

    const handleActionChange = (userId: number, field: string, value: any) => {
        setActionForms(prev => ({
            ...prev,
            [userId]: {
                ...prev[userId],
                [field]: value
            }
        }));
    };

    const handleApplyAction = async (userId: number) => {
        const form = actionForms[userId];
        if (!form.reason) {
            Swal.fire({
                icon: 'warning',
                text: '제재 사유를 입력해주세요.'
            });
            return;
        }

        try {
            await api.post("/admin/users/status", {
                targetUserId: userId,
                newStatus: form.newStatus,
                reason: form.reason,
                durationDays: form.newStatus === "SUSPENDED" ? (form.durationDays === "" ? null : form.durationDays) : null
            });
            Swal.fire({
                icon: 'success',
                title: '완료',
                text: '상태가 변경되었습니다.',
                timer: 1500,
                showConfirmButton: false
            });
            fetchQueue();
        } catch (err: any) {
            Swal.fire({
                icon: 'error',
                title: '오류',
                text: err?.response?.data?.message || "상태 변경에 실패했습니다."
            });
        }
    };

    if (loading) return <div className="p-4 text-gray-500">대기열을 불러오는 중...</div>;

    if (users.length === 0) return <div className="p-4 text-gray-500">현재 신고가 누적된 유저가 없습니다.</div>;

    return (
        <div className="space-y-4">
            <h3 className="text-lg font-semibold text-gray-800">신고 누적 대기열 (ACTIVE 상태 한정)</h3>
            <div className="overflow-x-auto">
                <table className="min-w-full bg-white border border-gray-200 rounded-lg">
                    <thead className="bg-gray-50 border-b border-gray-200 text-gray-600 text-sm">
                        <tr>
                            <th className="py-3 px-4 text-left">닉네임 (Email)</th>
                            <th className="py-3 px-4 text-left">신고수</th>
                            <th className="py-3 px-4 text-left">현재 상태</th>
                            <th className="py-3 px-4 text-left">이력 보기</th>
                            <th className="py-3 px-4 text-left">제재 조치</th>
                        </tr>
                    </thead>
                    <tbody className="text-sm divide-y divide-gray-100">
                        {users.map(u => (
                            <tr key={u.userId} className="hover:bg-gray-50">
                                <td className="py-3 px-4">
                                    <div className="font-semibold text-gray-800">{u.nickname}</div>
                                    <div className="text-gray-500 text-xs">{u.email}</div>
                                </td>
                                <td className="py-3 px-4 text-red-500 font-bold">{u.reportCount}</td>
                                <td className="py-3 px-4">
                                    <span className={`px-2 py-1 rounded-full text-xs font-semibold ${u.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
                                        }`}>
                                        {u.status}
                                    </span>
                                </td>
                                <td className="py-3 px-4 space-x-2">
                                    <button
                                        onClick={() => { setSelectedUser(u.userId); setModalType("reports"); }}
                                        className="text-idol hover:text-idol-dark underline text-xs">피신고</button>
                                    <button
                                        onClick={() => { setSelectedUser(u.userId); setModalType("bans"); }}
                                        className="text-gray-500 hover:text-gray-700 underline text-xs">징계</button>
                                </td>
                                <td className="py-3 px-4">
                                    <div className="flex flex-col gap-2 min-w-[250px]">
                                        <div className="flex space-x-2">
                                            <select
                                                className="border border-gray-300 rounded px-2 py-1 text-xs focus:outline-none focus:border-idol"
                                                value={actionForms[u.userId]?.newStatus || "SUSPENDED"}
                                                onChange={(e) => handleActionChange(u.userId, "newStatus", e.target.value)}
                                            >
                                                <option value="SUSPENDED">정지 (SUSPENDED)</option>
                                                <option value="RESTRICTED">제한 (RESTRICTED)</option>
                                                <option value="BANNED">영구정지 (BANNED)</option>
                                            </select>
                                            {(actionForms[u.userId]?.newStatus || "SUSPENDED") === "SUSPENDED" && (
                                                <select
                                                    className="border border-gray-300 rounded px-2 py-1 text-xs focus:outline-none focus:border-idol"
                                                    value={actionForms[u.userId]?.durationDays ?? 7}
                                                    onChange={(e) => handleActionChange(u.userId, "durationDays", e.target.value === "" ? "" : Number(e.target.value))}
                                                >
                                                    <option value={1}>1일</option>
                                                    <option value={3}>3일</option>
                                                    <option value={7}>7일</option>
                                                    <option value={30}>30일</option>
                                                    <option value="">무기한</option>
                                                </select>
                                            )}
                                        </div>
                                        <div className="flex space-x-2 mt-2">
                                            <input
                                                type="text"
                                                placeholder="제재 사유 입력"
                                                className="border border-gray-300 rounded px-2 py-1 text-xs flex-1 focus:outline-none focus:border-idol"
                                                value={actionForms[u.userId]?.reason || ""}
                                                onChange={(e) => handleActionChange(u.userId, "reason", e.target.value)}
                                            />
                                            <button
                                                onClick={() => handleApplyAction(u.userId)}
                                                className="bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded text-xs font-semibold whitespace-nowrap transition-colors">
                                                적용
                                            </button>
                                        </div>
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {selectedUser && (
                <AdminHistoryModal
                    userId={selectedUser}
                    type={modalType}
                    onClose={() => setSelectedUser(null)}
                />
            )}
        </div>
    );
};

export default AdminReportQueue;
