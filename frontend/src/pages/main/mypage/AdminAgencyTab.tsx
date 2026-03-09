import React, { useState, useEffect } from "react";
import Swal from 'sweetalert2';
import { api } from "../../../api/axios";

export interface Agency {
    agencyId: number;
    name: string;
}

const AdminAgencyTab: React.FC = () => {
    const [agencies, setAgencies] = useState<Agency[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    // 모달 관리
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);

    // 폼 데이터
    const [newAgencyName, setNewAgencyName] = useState("");
    const [editingAgency, setEditingAgency] = useState<Agency | null>(null);

    const fetchAgencies = async () => {
        try {
            setLoading(true);
            const response = await api.get('/agencies');
            setAgencies(response.data);
            setError(null);
        } catch (err: any) {
            console.error("소속사 목록 조회 실패", err);
            setError("소속사 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAgencies();
    }, []);

    const handleCreateAgency = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!newAgencyName.trim()) return;

        try {
            await api.post('/agencies', { name: newAgencyName });
            setNewAgencyName("");
            setIsCreateModalOpen(false);
            fetchAgencies();
            Swal.fire('추가 완료', '소속사가 추가되었습니다.', 'success');
        } catch (err: any) {
            console.error("소속사 추가 실패", err);
            Swal.fire('추가 실패', '소속사 추가에 실패했습니다.', 'error');
        }
    };

    const handleEditAgency = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!editingAgency || !editingAgency.name.trim()) return;

        try {
            await api.post(`/agencies/${editingAgency.agencyId}/update`, { name: editingAgency.name });
            setEditingAgency(null);
            setIsEditModalOpen(false);
            fetchAgencies();
            Swal.fire('수정 완료', '소속사 정보가 수정되었습니다.', 'success');
        } catch (err: any) {
            console.error("소속사 수정 실패", err);
            Swal.fire('수정 실패', '소속사 수정에 실패했습니다.', 'error');
        }
    };

    const handleDeleteAgency = async (agencyId: number, agencyName: string) => {
        const result = await Swal.fire({
            title: '정말 삭제하시겠습니까?',
            text: `'${agencyName}' 소속사 및 연관된 소속사 계정이 모두 삭제됩니다.`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#fe2a55',
            cancelButtonColor: '#e5e7eb',
            confirmButtonText: '삭제',
            cancelButtonText: '<span class="text-gray-700">취소</span>'
        });

        if (!result.isConfirmed) {
            return;
        }

        try {
            await api.post(`/agencies/${agencyId}/delete`);
            fetchAgencies();
            Swal.fire('삭제 완료', '소속사가 삭제되었습니다.', 'success');
        } catch (err: any) {
            console.error("소속사 삭제 실패", err);
            Swal.fire('삭제 실패', '소속사 삭제에 실패했습니다.', 'error');
        }
    };

    if (loading && agencies.length === 0) return <div className="text-center py-10">로딩 중...</div>;
    if (error) return <div className="text-center py-10 text-red-500">{error}</div>;

    return (
        <div className="space-y-6 animate-fade-in mt-4">
            <div className="flex justify-between items-center mb-4">
                <h3 className="text-lg font-semibold text-gray-900">소속사 목록</h3>
                <button
                    onClick={() => setIsCreateModalOpen(true)}
                    className="px-4 py-2 bg-idol text-white rounded-lg hover:bg-idol/90 transition-colors text-sm font-medium"
                >
                    + 소속사 추가
                </button>
            </div>

            <div className="bg-white rounded-xl border border-gray-100 overflow-hidden">
                <table className="w-full text-left text-sm text-gray-600">
                    <thead className="bg-gray-50 text-gray-900 font-semibold border-b border-gray-100">
                        <tr>
                            <th className="px-6 py-4">ID</th>
                            <th className="px-6 py-4">소속사 이름</th>
                            <th className="px-6 py-4 text-right">관리</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100">
                        {agencies.length === 0 ? (
                            <tr>
                                <td colSpan={3} className="px-6 py-8 text-center text-gray-500">
                                    등록된 소속사가 없습니다.
                                </td>
                            </tr>
                        ) : (
                            agencies.map((agency) => (
                                <tr key={agency.agencyId} className="hover:bg-gray-50 transition-colors">
                                    <td className="px-6 py-4">{agency.agencyId}</td>
                                    <td className="px-6 py-4 font-medium text-gray-900">{agency.name}</td>
                                    <td className="px-6 py-4 text-right space-x-2">
                                        <button
                                            onClick={() => {
                                                setEditingAgency(agency);
                                                setIsEditModalOpen(true);
                                            }}
                                            className="text-indigo-600 hover:text-indigo-900 font-medium"
                                        >
                                            수정
                                        </button>
                                        <button
                                            onClick={() => handleDeleteAgency(agency.agencyId, agency.name)}
                                            className="text-red-500 hover:text-red-700 font-medium ml-4"
                                        >
                                            삭제
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {/* 생성 모달 */}
            {isCreateModalOpen && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-lg animate-scale-up">
                        <h3 className="text-lg font-bold text-gray-900 mb-4">새 소속사 추가</h3>
                        <form onSubmit={handleCreateAgency}>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700 mb-1">소속사 이름</label>
                                <input
                                    type="text"
                                    value={newAgencyName}
                                    onChange={(e) => setNewAgencyName(e.target.value)}
                                    placeholder="소속사 이름을 입력하세요"
                                    className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-idol/50 transition-shadow"
                                    required
                                />
                            </div>
                            <div className="flex justify-end space-x-2 mt-6">
                                <button
                                    type="button"
                                    onClick={() => {
                                        setIsCreateModalOpen(false);
                                        setNewAgencyName("");
                                    }}
                                    className="px-4 py-2 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
                                >
                                    취소
                                </button>
                                <button
                                    type="submit"
                                    disabled={!newAgencyName.trim()}
                                    className="px-4 py-2 text-sm font-medium text-white bg-idol rounded-lg hover:bg-idol/90 transition-colors disabled:opacity-50"
                                >
                                    추가하기
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* 수정 모달 */}
            {isEditModalOpen && editingAgency && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md shadow-lg animate-scale-up">
                        <h3 className="text-lg font-bold text-gray-900 mb-4">소속사 수정</h3>
                        <form onSubmit={handleEditAgency}>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700 mb-1">소속사 이름</label>
                                <input
                                    type="text"
                                    value={editingAgency.name}
                                    onChange={(e) => setEditingAgency({ ...editingAgency, name: e.target.value })}
                                    className="w-full px-4 py-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-idol/50 transition-shadow"
                                    required
                                />
                            </div>
                            <div className="flex justify-end space-x-2 mt-6">
                                <button
                                    type="button"
                                    onClick={() => {
                                        setIsEditModalOpen(false);
                                        setEditingAgency(null);
                                    }}
                                    className="px-4 py-2 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
                                >
                                    취소
                                </button>
                                <button
                                    type="submit"
                                    disabled={!editingAgency.name.trim()}
                                    className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-lg hover:bg-indigo-700 transition-colors disabled:opacity-50"
                                >
                                    반영하기
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default AdminAgencyTab;
