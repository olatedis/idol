import React, { useState, useEffect } from "react";
import { api } from "../../../api/axios";
import AdminHistoryModal from "./AdminHistoryModal";
import Swal from 'sweetalert2';

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

const AdminUserSearch: React.FC = () => {
    const [keyword, setKeyword] = useState("");
    const [statusFilter, setStatusFilter] = useState("ALL");
    const [users, setUsers] = useState<AdminUserDto[]>([]);
    const [loading, setLoading] = useState(false);
    const [hasSearched, setHasSearched] = useState(false);

    // 전체 유저 페이징 상태
    const [currentPage, setCurrentPage] = useState(0);
    const [totalPages, setTotalPages] = useState(1);
    const [paginatedUsers, setPaginatedUsers] = useState<AdminUserDto[]>([]);
    const [isFetchingPage, setIsFetchingPage] = useState(false);

    const [selectedUser, setSelectedUser] = useState<number | null>(null);
    const [modalType, setModalType] = useState<"reports" | "bans">("reports");

    // 상태 변경 폼 상태
    const [actionForms, setActionForms] = useState<Record<number, { newStatus: string, durationDays: number | "", reason: string }>>({});

    // Пей징된 전체 유저 목록 불러오기
    const fetchPaginatedUsers = async (page: number, status: string = "ALL") => {
        setIsFetchingPage(true);
        try {
            const statusQuery = status !== "ALL" ? `&status=${status}` : "";
            const res = await api.get(`/admin/users?page=${page}&size=10${statusQuery}`);
            setPaginatedUsers(res.data.content);
            setTotalPages(res.data.totalPages);

            setActionForms(prev => {
                const updated = { ...prev };
                res.data.content.forEach((u: AdminUserDto) => {
                    if (!updated[u.userId]) {
                        updated[u.userId] = { newStatus: u.status === 'ACTIVE' ? "SUSPENDED" : "ACTIVE", durationDays: 7, reason: "" };
                    }
                });
                return updated;
            });
        } catch (err) {
            console.error("Failed to fetch paginated users", err);
        } finally {
            setIsFetchingPage(false);
        }
    };

    useEffect(() => {
        if (!hasSearched) {
            fetchPaginatedUsers(currentPage, statusFilter);
        }
    }, [currentPage, hasSearched, statusFilter]);

    const handleSearch = async () => {
        if (!keyword.trim() && statusFilter === "ALL") {
            setHasSearched(false);
            setUsers([]);
            return;
        }
        setLoading(true);
        setHasSearched(true);
        try {
            const statusQuery = statusFilter !== "ALL" ? `&status=${statusFilter}` : "";
            const res = await api.get(`/admin/users/search?keyword=${encodeURIComponent(keyword)}${statusQuery}`);
            setUsers(res.data);
            setActionForms(prev => {
                const updated = { ...prev };
                res.data.forEach((u: AdminUserDto) => {
                    if (!updated[u.userId]) {
                        updated[u.userId] = { newStatus: u.status === 'ACTIVE' ? "SUSPENDED" : "ACTIVE", durationDays: 7, reason: "" };
                    }
                });
                return updated;
            });
        } catch (err) {
            console.error("Search failed", err);
        } finally {
            setLoading(false);
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') handleSearch();
    };

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
                text: '상태 변경 사유를 입력해주세요.'
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
            if (hasSearched) {
                handleSearch(); // 재검색
            } else {
                fetchPaginatedUsers(currentPage, statusFilter); // 현재 페이지 재로딩
            }
        } catch (err: any) {
            console.error("상태 변경 에러", err);
            Swal.fire({
                icon: 'error',
                title: '오류',
                text: err?.response?.data?.message || "상태 변경에 실패했습니다."
            });
        }
    };

    const formatDate = (dateString?: string) => {
        if (!dateString) return "-";
        const parseString = dateString.endsWith('Z') || dateString.includes('+') ? dateString : dateString + 'Z';
        const date = new Date(parseString);
        const kstDate = new Date(date.getTime() + 9 * 60 * 60 * 1000);
        return kstDate.toISOString().replace('T', ' ').substring(0, 16);
    };

    const displayUsers = hasSearched ? users : paginatedUsers;

    return (
        <div className="space-y-6">
            <h3 className="text-lg font-semibold text-gray-800">유저 검색 및 관리 (키워드 검색)</h3>

            <div className="flex space-x-2">
                <select
                    className="border border-gray-300 rounded-lg px-4 py-2 focus:outline-none focus:border-idol focus:ring-1 focus:ring-idol transition-colors bg-white text-gray-700"
                    value={statusFilter}
                    onChange={(e) => {
                        setStatusFilter(e.target.value);
                        if (hasSearched) {
                            handleSearch();
                        } else {
                            setCurrentPage(0);
                        }
                    }}
                >
                    <option value="ALL">전체 상태</option>
                    <option value="ACTIVE">활성 (ACTIVE)</option>
                    <option value="RESTRICTED">제한 (RESTRICTED)</option>
                    <option value="SUSPENDED">정지 (SUSPENDED)</option>
                    <option value="BANNED">영구정지 (BANNED)</option>
                </select>
                <input
                    type="text"
                    placeholder="이메일 또는 닉네임 입력 (검색어 없이 상태만 선택 가능)"
                    className="border border-gray-300 rounded-lg px-4 py-2 flex-1 focus:outline-none focus:border-idol focus:ring-1 focus:ring-idol transition-colors"
                    value={keyword}
                    onChange={e => setKeyword(e.target.value)}
                    onKeyDown={handleKeyDown}
                />
                <button
                    onClick={handleSearch}
                    disabled={loading}
                    className="bg-gray-800 hover:bg-gray-900 text-white px-6 py-2 rounded-lg font-semibold transition-colors disabled:opacity-50">
                    {loading ? "검색 중..." : "검색"}
                </button>
            </div>

            {hasSearched && !loading && users.length === 0 && (
                <div className="text-gray-500 py-4 text-center bg-gray-50 rounded-lg">검색 결과가 없습니다.</div>
            )}

            {!hasSearched && (
                <div className="flex items-center justify-between mb-2">
                    <h4 className="text-gray-700 font-medium">전체 유저 목록</h4>
                    {isFetchingPage && <span className="text-xs text-idol">불러오는 중...</span>}
                </div>
            )}

            {displayUsers.length > 0 && (
                <div className="overflow-x-auto">
                    <table className="min-w-full bg-white border border-gray-200 rounded-lg">
                        <thead className="bg-gray-50 border-b border-gray-200 text-gray-600 text-sm">
                            <tr>
                                <th className="py-3 px-4 text-left">유저 정보</th>
                                <th className="py-3 px-4 text-left">상태 및 제한 만료</th>
                                <th className="py-3 px-4 text-left">역대이력</th>
                                <th className="py-3 px-4 text-left">상태 변경</th>
                            </tr>
                        </thead>
                        <tbody className="text-sm divide-y divide-gray-100">
                            {displayUsers.map(u => (
                                <tr key={u.userId} className="hover:bg-gray-50">
                                    <td className="py-3 px-4">
                                        <div className="font-semibold text-gray-800">{u.nickname} <span className="text-xs font-normal text-gray-500 bg-gray-100 px-1 rounded">{u.role}</span></div>
                                        <div className="text-gray-500 text-xs">{u.email}</div>
                                        <div className="text-red-500 text-xs mt-1 font-medium">현재 신고 누적: {u.reportCount}회</div>
                                    </td>
                                    <td className="py-3 px-4">
                                        <span className={`px-2 py-1 rounded-full text-xs font-semibold block w-max mb-1 ${u.status === 'ACTIVE' ? 'bg-green-100 text-green-700' :
                                            u.status === 'RESTRICTED' ? 'bg-yellow-100 text-yellow-700' :
                                                'bg-red-100 text-red-700'
                                            }`}>
                                            {u.status}
                                        </span>
                                        {u.suspendedUntil && u.status !== 'ACTIVE' && (
                                            <div className="text-[10px] text-gray-500">~ {formatDate(u.suspendedUntil)}</div>
                                        )}
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
                                                    value={actionForms[u.userId]?.newStatus || "ACTIVE"}
                                                    onChange={(e) => handleActionChange(u.userId, "newStatus", e.target.value)}
                                                >
                                                    <option value="ACTIVE">활성 (ACTIVE)</option>
                                                    <option value="SUSPENDED">정지 (SUSPENDED)</option>
                                                    <option value="RESTRICTED">제한 (RESTRICTED)</option>
                                                    <option value="BANNED">영구정지 (BANNED)</option>
                                                </select>
                                                {(actionForms[u.userId]?.newStatus || "ACTIVE") === "SUSPENDED" && (
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
                                                    placeholder="변경 사유 입력"
                                                    className="border border-gray-300 rounded px-2 py-1 text-xs flex-1 focus:outline-none focus:border-idol"
                                                    value={actionForms[u.userId]?.reason || ""}
                                                    onChange={(e) => handleActionChange(u.userId, "reason", e.target.value)}
                                                />
                                                <button
                                                    onClick={() => handleApplyAction(u.userId)}
                                                    className="bg-gray-800 hover:bg-gray-900 text-white px-3 py-1 rounded text-xs font-semibold whitespace-nowrap transition-colors">
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
            )}

            {!hasSearched && totalPages > 1 && (
                <div className="flex justify-center items-center space-x-4 mt-6">
                    <button
                        onClick={() => setCurrentPage(prev => Math.max(0, prev - 1))}
                        disabled={currentPage === 0 || isFetchingPage}
                        className="px-4 py-2 bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 rounded-lg disabled:opacity-50 transition-colors shadow-sm text-sm font-medium"
                    >
                        이전
                    </button>
                    <span className="px-3 py-1 text-gray-700 font-semibold bg-gray-100 rounded-lg">
                        {currentPage + 1} / {totalPages}
                    </span>
                    <button
                        onClick={() => setCurrentPage(prev => Math.min(totalPages - 1, prev + 1))}
                        disabled={currentPage >= totalPages - 1 || isFetchingPage}
                        className="px-4 py-2 bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 rounded-lg disabled:opacity-50 transition-colors shadow-sm text-sm font-medium"
                    >
                        다음
                    </button>
                </div>
            )}

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

export default AdminUserSearch;
