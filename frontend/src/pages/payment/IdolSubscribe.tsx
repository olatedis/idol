import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../main/Header';
import { getIdol, fetchGroups, fetchGroupIdols, subscribeGroup } from '../../api/payment';
import { useAuthStore } from '../../stores/authStore';

interface GroupDto { groupId: number; name: string; groupImage?: string; }
interface IdolDto { id: number; stageName: string; profileImage?: string; }

const IdolSubscribe: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const { user } = useAuthStore();
    const [idol, setIdol] = useState<any>(null);

    // group mode states
    const [groups, setGroups] = useState<GroupDto[]>([]);
    const [modalOpen, setModalOpen] = useState(false);
    const [selectedGroup, setSelectedGroup] = useState<GroupDto | null>(null);
    const [groupIdols, setGroupIdols] = useState<IdolDto[]>([]);

    const idolId = location.state?.idolId;

    useEffect(() => {
        if (idolId) {
            getIdol(Number(idolId)).then(setIdol).catch(() => null);
        } else {
            fetchGroups().then(setGroups).catch(() => {});
        }
    }, [idolId]);

    const handleChoose = (plan: 'MONTHLY' | 'ANNUAL') => {
        navigate(`/payment`, { state: { domain: 'SUBSCRIPTION', idolId, plan } });
    };

    const openGroup = (group: GroupDto) => {
        setSelectedGroup(group);
        setModalOpen(true);
        fetchGroupIdols(group.groupId).then(setGroupIdols).catch(() => setGroupIdols([]));
    };

    const handleGroupSubscribe = async () => {
        if (!selectedGroup) return;
        try {
            await subscribeGroup(user?.userId || 0, selectedGroup.groupId);
            alert('그룹 구독이 완료되었습니다.');
            setModalOpen(false);
        } catch (e) {
            console.error(e);
            alert('그룹 구독에 실패했습니다.');
        }
    };

    const handleIdolClick = (idol: IdolDto) => {
        navigate(`/idol/${idol.id}/subscribe`, { state: { idolId: idol.id } });
    };

    // render
    return (
        <div className="min-h-screen bg-idol-bg">
            <Header />
            <main className="pt-[80px] px-6">
                <div className="max-w-3xl mx-auto">
                    <h2 className="text-2xl font-semibold mb-4">아이돌 구독</h2>
                    {idolId && idol && (
                        <div className="bg-white rounded p-6 shadow">
                            <div className="flex items-center gap-6">
                                <img src={idol.profileImage} alt={idol.stageName} className="w-24 h-24 rounded-full object-cover" />
                                <div>
                                    <div className="text-lg font-bold">{idol.stageName}</div>
                                    <div className="text-sm text-gray-600">{idol.agencyName}</div>
                                </div>
                            </div>

                            <div className="mt-6 grid grid-cols-2 gap-4">
                                <button onClick={() => handleChoose('MONTHLY')} className="py-3 rounded bg-idol-point text-white">정기 구독 — 매월 9,900원</button>
                                <button onClick={() => handleChoose('ANNUAL')} className="py-3 rounded bg-gray-800 text-white">연간 결제 — 89,100원</button>
                            </div>
                        </div>
                    )}

                    {!idolId && (
                        <div>
                            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-6">
                                {groups.map(group => (
                                    <div key={group.groupId} className="cursor-pointer" onClick={() => openGroup(group)}>
                                        <img src={group.groupImage || `https://api.dicebear.com/7.x/identicon/svg?seed=${group.groupId}`} alt={group.name} className="w-full h-32 object-cover rounded-lg" />
                                        <div className="mt-2 text-center font-medium">{group.name}</div>
                                    </div>
                                ))}
                            </div>

                            {modalOpen && selectedGroup && (
                                <div className="fixed inset-0 bg-black bg-opacity-40 flex items-center justify-center z-50">
                                    <div className="bg-white rounded-lg p-6 w-full max-w-md">
                                        <h3 className="text-lg font-bold mb-4">{selectedGroup.name} 구독</h3>
                                        <button onClick={handleGroupSubscribe} className="mb-4 py-2 px-4 bg-idol-point text-white rounded">그룹 구독</button>
                                        <div className="mb-4">
                                            <div className="font-semibold mb-2">아이돌 선택</div>
                                            <ul className="max-h-60 overflow-y-auto">
                                                {groupIdols.map(i => (
                                                    <li key={i.id} className="py-1 cursor-pointer hover:bg-gray-100" onClick={() => handleIdolClick(i)}>
                                                        {i.stageName}
                                                    </li>
                                                ))}
                                                {groupIdols.length === 0 && <li>아이돌 정보가 없습니다.</li>}
                                            </ul>
                                        </div>
                                        <button onClick={() => setModalOpen(false)} className="mt-2 text-sm text-gray-500">닫기</button>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
};

export default IdolSubscribe;
