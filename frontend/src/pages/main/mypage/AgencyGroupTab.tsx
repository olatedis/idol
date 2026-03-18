import React, { useEffect, useState } from 'react';
import { api } from '../../../api/axios';
import { motion, AnimatePresence } from 'framer-motion';
import { showConfirm, showErrorToast, showSuccessToast } from '../../../utils/alert';

interface Idol {
    idolId: number;
    userId: number;
    stageName: string;
    profileImage: string;
    status: string;
}

interface Group {
    groupId: number;
    name: string;
    groupImage: string;
    agencyId?: number;
    members?: Idol[];
}

interface AgencyGroupTabProps {
    agencyId?: number;
}

const AgencyGroupTab: React.FC<AgencyGroupTabProps> = ({ agencyId }) => {
    const [groups, setGroups] = useState<Group[]>([]);
    const [allIdols, setAllIdols] = useState<Idol[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    // 모달 및 선택 상태
    const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null);
    const [isAddMemberModalOpen, setIsAddMemberModalOpen] = useState(false);
    const [newMemberIdolId, setNewMemberIdolId] = useState<string>('');
    const [actionError, setActionError] = useState<string>('');

    useEffect(() => {
        if (!agencyId) return;

        const fetchData = async () => {
            setLoading(true);
            try {
                // 자신이 관리하는 전체 그룹 목록은 백엔드에서 아직 명시적 API가 없으므로 
                // 임시로 그룹 API 조회 후, 필터링 혹은 모든 그룹을 가져오는 것으로 가정합시다.
                // 혹은 /groups 에서 전체를 가져와 agencyId로 필터링합니다.
                const groupResponse = await api.get('/groups');
                const agencyGroups = groupResponse.data.filter((g: Group) => g.agencyId === agencyId);

                // 그룹별 멤버 상세 조회
                const groupsWithMembers = await Promise.all(agencyGroups.map(async (group: Group) => {
                    try {
                        const memberResponse = await api.get(`/groups/${group.groupId}/idols`);
                        return { ...group, members: memberResponse.data };
                    } catch (err) {
                        return { ...group, members: [] };
                    }
                }));

                setGroups(groupsWithMembers);

                // 아이돌 전체 목록 조회 (추가 모달에서 선택을 위함)
                const idolsResponse = await api.get('/idols');
                // 소속사의 아이돌만 필터링 (선택적)
                const myAgencyIdols = idolsResponse.data.filter((i: any) => i.agencyId === agencyId);
                setAllIdols(myAgencyIdols);

            } catch (err: any) {
                console.error("데이터 조회 실패:", err);
                setError("데이터를 불러오는데 실패했습니다.");
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [agencyId]);

    const handleAddMember = async () => {
        if (!selectedGroupId || !newMemberIdolId) {
            setActionError("추가할 아이돌을 선택해주세요.");
            return;
        }

        try {
            setActionError('');
            await api.post(`/groups/${selectedGroupId}/members?idolId=${newMemberIdolId}`);
            
            // 성공 시 로컬 상태 업데이트
            const idolInfo = allIdols.find(i => i.idolId === Number(newMemberIdolId));
            if (idolInfo) {
                setGroups(prevGroups => prevGroups.map(g => {
                    if (g.groupId === selectedGroupId) {
                        return { ...g, members: [...(g.members || []), idolInfo] };
                    }
                    return g;
                }));
            }
            
            setIsAddMemberModalOpen(false);
            setNewMemberIdolId('');
        } catch (err: any) {
            console.error("멤버 추가 실패:", err);
            setActionError(err.response?.data || "멤버 추가에 실패했습니다.");
        }
    };

    const handleRemoveMember = async (groupId: number, idolId: number, stageName: string) => {
        // if (!window.confirm(`'${stageName}' 멤버를 그룹에서 제외하시겠습니까?`)) {
        //     return;
        // }
        const ok = await showConfirm("멤버 제외", `'${stageName}' 멤버를 그룹에서 제외하시겠습니까?`, "제외");
        if (!ok) return;

        try {
            await api.post(`/groups/${groupId}/members/remove?idolId=${idolId}`);
            
            // 성공 시 로컬 상태 업데이트
            setGroups(prevGroups => prevGroups.map((g: Group) => {
                if (g.groupId === groupId) {
                    return { ...g, members: g.members?.filter((m: Idol) => m.idolId !== idolId) };
                }
                return g;
            }));
            
            // alert("멤버가 성공적으로 제외되었습니다.");
            showSuccessToast("멤버가 성공적으로 제외되었습니다.");
        } catch (err: any) {
            console.error("멤버 제외 실패:", err);
            // alert(err.response?.data || "멤버 제외에 실패했습니다.");
            showErrorToast(err.response?.data || "멤버 제외에 실패했습니다.");
        }
    };

    if (loading) return <div className="py-10 text-center">Loading Data...</div>;
    if (error) return <div className="py-10 text-center text-red-500">{error}</div>;

    return (
        <div className="space-y-6 mt-8 border-t border-gray-100 pt-8 animate-fade-in font-sans">
            <div className="flex items-center justify-between">
                <div>
                    <h3 className="text-xl font-bold text-gray-900">소속 그룹 목록 및 멤버 관리</h3>
                    <p className="text-sm text-gray-500 mt-1">그룹의 상세 멤버를 관리하고 수정할 수 있습니다.</p>
                </div>
            </div>

            {groups.length === 0 ? (
                <div className="bg-gray-50 rounded-2xl p-8 text-center text-gray-500 text-sm border border-gray-100">
                    관리 중인 그룹이 없습니다.
                </div>
            ) : (
                <div className="grid grid-cols-1 gap-6">
                    {groups.map((group: Group) => (
                        <div key={group.groupId} className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100 hover:border-idol/30 transition-colors">
                            <div className="flex items-center justify-between mb-4 border-b border-gray-50 pb-4">
                                <div className="flex items-center gap-4">
                                    <div className="w-12 h-12 bg-gray-100 rounded-xl overflow-hidden shadow-inner">
                                        {group.groupImage ? (
                                            <img src={group.groupImage} alt={group.name} className="w-full h-full object-cover" />
                                        ) : (
                                            <div className="w-full h-full flex items-center justify-center text-gray-400">G</div>
                                        )}
                                    </div>
                                    <div>
                                        <h4 className="text-lg font-bold text-gray-900">{group.name}</h4>
                                        <div className="text-xs text-idol font-medium bg-idol/10 px-2 py-0.5 rounded inline-block mt-1">
                                            ID: {group.groupId}
                                        </div>
                                    </div>
                                </div>
                                <button
                                    onClick={() => {
                                        setSelectedGroupId(group.groupId);
                                        setIsAddMemberModalOpen(true);
                                        setActionError('');
                                    }}
                                    className="px-4 py-2 bg-idol hover:bg-idol-hover text-white text-sm font-semibold rounded-lg shadow-sm transition-colors"
                                >
                                    + 멤버 추가
                                </button>
                            </div>

                            <div className="space-y-3">
                                {group.members && group.members.length > 0 ? (
                                    group.members.map((member) => (
                                        <div key={member.idolId} className="flex items-center justify-between p-3 bg-gray-50 rounded-xl border border-gray-100/50 hover:bg-white hover:border-gray-200 transition-colors group">
                                            <div className="flex items-center gap-3">
                                                <div className="w-10 h-10 rounded-full bg-gray-200 overflow-hidden shadow-sm border border-white">
                                                    {member.profileImage ? (
                                                        <img src={member.profileImage} alt={member.stageName} className="w-full h-full object-cover" />
                                                    ) : (
                                                        <div className="w-full h-full flex items-center justify-center text-xs text-gray-400">IDOL</div>
                                                    )}
                                                </div>
                                                <div>
                                                    <div className="text-sm font-bold text-gray-900">{member.stageName}</div>
                                                    <div className="text-xs text-gray-500">Idol ID: {member.idolId}</div>
                                                </div>
                                            </div>
                                            <button
                                                onClick={() => handleRemoveMember(group.groupId, member.idolId, member.stageName)}
                                                className="px-3 py-1.5 text-xs font-semibold text-rose-500 bg-rose-50 hover:bg-rose-100 rounded-lg transition-colors border border-rose-100/50 opacity-0 group-hover:opacity-100 focus:opacity-100"
                                            >
                                                방출(제외)
                                            </button>
                                        </div>
                                    ))
                                ) : (
                                    <div className="text-sm text-gray-400 text-center py-4 bg-gray-50/50 rounded-xl">
                                        소속된 멤버가 없습니다.
                                    </div>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* 모달 공통 컴포넌트 */}
            <AnimatePresence>
                {isAddMemberModalOpen && (
                    <motion.div
                        initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                        className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
                        onClick={() => setIsAddMemberModalOpen(false)}
                    >
                        <motion.div
                            initial={{ scale: 0.95, opacity: 0, y: 10 }}
                            animate={{ scale: 1, opacity: 1, y: 0 }}
                            exit={{ scale: 0.95, opacity: 0, y: 10 }}
                            onClick={(e) => e.stopPropagation()}
                            className="bg-white rounded-2xl shadow-xl w-full max-w-sm overflow-hidden"
                        >
                            <div className="px-6 py-5 border-b border-gray-100">
                                <h3 className="text-lg font-bold text-gray-900">새 멤버 추가</h3>
                                <p className="text-xs text-gray-500 mt-1">자사 소속 아이돌 중 현재 소속 그룹이 없는 아이돌을 선택하세요.</p>
                            </div>
                            
                            <div className="p-6 space-y-4">
                                <div>
                                    <label className="block text-sm font-semibold text-gray-700 mb-2">추가할 아이돌 선택</label>
                                    <select
                                        value={newMemberIdolId}
                                        onChange={(e) => setNewMemberIdolId(e.target.value)}
                                        className="w-full px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-idol/20 focus:border-idol outline-none text-sm transition-all text-gray-700"
                                    >
                                        <option value="">-- 아이돌 선택 --</option>
                                        {allIdols.map(idol => (
                                            <option key={idol.idolId} value={idol.idolId}>
                                                {idol.stageName} (ID: {idol.idolId})
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                {actionError && <div className="text-xs text-rose-500 bg-rose-50 p-2 rounded-lg">{actionError}</div>}
                            </div>
                            
                            <div className="px-6 py-4 bg-gray-50 border-t border-gray-100 flex gap-3 justify-end">
                                <button
                                    onClick={() => setIsAddMemberModalOpen(false)}
                                    className="px-4 py-2 text-sm font-semibold text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
                                >
                                    취소
                                </button>
                                <button
                                    onClick={handleAddMember}
                                    className="px-4 py-2 text-sm font-semibold text-white bg-idol hover:bg-idol-hover rounded-lg shadow-sm transition-colors"
                                >
                                    추가하기
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default AgencyGroupTab;
