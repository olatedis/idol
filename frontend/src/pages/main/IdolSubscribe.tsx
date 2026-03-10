import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import Header from './Header.tsx';
import { getIdol, fetchGroups, fetchGroupIdols, subscribeGroup } from '../../api/payment.ts';
import { useAuthStore } from '../../stores/authStore.ts';

interface GroupDto { groupId: number; name: string; groupImage?: string; }
interface IdolDto { id: number; stageName: string; profileImage?: string; }

const IdolSubscribe: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const { user } = useAuthStore();

    // group mode states
    const [groups, setGroups] = useState<GroupDto[]>([]);
    const [groupModalOpen, setGroupModalOpen] = useState(false);
    const [selectedGroup, setSelectedGroup] = useState<GroupDto | null>(null);
    const [groupIdols, setGroupIdols] = useState<IdolDto[]>([]);

    // idol subscription modal
    const [selectedIdol, setSelectedIdol] = useState<IdolDto | null>(null);

    const idolId = location.state?.idolId;

    useEffect(() => {
        if (idolId) {
            getIdol(Number(idolId))
                .then(data => {
                    setSelectedIdol(data);
                })
                .catch(() => null);
            // open idol modal right away (will be replaced when data arrives)
            setSelectedIdol({ id: idolId, stageName: '', profileImage: '' } as any);
        } else {
            fetchGroups().then(setGroups).catch(() => {});
        }
    }, [idolId]);

    const handleChoose = (plan: 'MONTHLY' | 'ANNUAL') => {
        if (!selectedIdol) return;
        navigate(`/payment`, { state: { domain: 'SUBSCRIPTION', idolId: selectedIdol.id, plan } });
    };

    const openGroup = (group: GroupDto) => {
        setSelectedGroup(group);
        setGroupModalOpen(true);
        fetchGroupIdols(group.groupId).then(setGroupIdols).catch(() => setGroupIdols([]));
    };

    const handleGroupSubscribe = async () => {
        if (!selectedGroup) return;
        try {
            await subscribeGroup(user?.userId || 0, selectedGroup.groupId);
            alert('그룹 구독이 완료되었습니다.');
            setGroupModalOpen(false);
        } catch (e) {
            console.error(e);
            alert('그룹 구독에 실패했습니다.');
        }
    };

    const handleIdolClick = (idol: IdolDto) => {
        // open subscription modal for idol
        setSelectedIdol(idol);
        setGroupModalOpen(false);
    };

    const closeIdolModal = () => setSelectedIdol(null);

    // render
    return (
        <div className="min-h-screen bg-idol-bg">
            <Header />
            <main className="pt-[80px] px-6">
                <div className="max-w-3xl mx-auto">
                    <h2 className="text-2xl font-semibold mb-4">아이돌 구독</h2>
                    {/* direct idol card - rendered inside modal as well */}
                    {selectedIdol && (
                        <AnimatePresence>
                            <motion.div
                                className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                exit={{ opacity: 0 }}
                                onClick={closeIdolModal}
                            >
                                <motion.div
                                    className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6"
                                    initial={{ scale: 0.95, opacity: 0 }}
                                    animate={{ scale: 1, opacity: 1 }}
                                    exit={{ scale: 0.95, opacity: 0 }}
                                    onClick={(e) => e.stopPropagation()}
                                >
                                    <h3 className="text-lg font-bold mb-4">{selectedIdol.stageName || '구독 대상'}</h3>
                                    {selectedIdol.profileImage && (
                                        <img src={selectedIdol.profileImage} alt={selectedIdol.stageName} className="w-24 h-24 rounded-full mb-4" />
                                    )}
                                    <div className="mt-6 grid grid-cols-2 gap-4">
                                        <button onClick={() => handleChoose('MONTHLY')} className="py-3 rounded bg-idol-point text-white">정기 구독 — 매월 9,900원</button>
                                        <button onClick={() => handleChoose('ANNUAL')} className="py-3 rounded bg-gray-800 text-white">연간 결제 — 89,100원</button>
                                    </div>
                                    <button onClick={closeIdolModal} className="mt-4 text-sm text-gray-500">닫기</button>
                                </motion.div>
                            </motion.div>
                        </AnimatePresence>
                    )}

                    {!idolId && (
                        <>
                            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-6">
                                {groups.map(group => (
                                    <div key={group.groupId} className="cursor-pointer" onClick={() => openGroup(group)}>
                                        <img src={group.groupImage || `https://api.dicebear.com/7.x/identicon/svg?seed=${group.groupId}`} alt={group.name} className="w-full h-32 object-cover rounded-lg" />
                                        <div className="mt-2 text-center font-medium">{group.name}</div>
                                    </div>
                                ))}
                            </div>

                            {/* 그룹 모달 */}
                            <AnimatePresence>
                                {groupModalOpen && selectedGroup && (
                                    <motion.div
                                        className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
                                        initial={{ opacity: 0 }}
                                        animate={{ opacity: 1 }}
                                        exit={{ opacity: 0 }}
                                        onClick={() => setGroupModalOpen(false)}
                                    >
                                        <motion.div
                                            className="bg-white rounded-2xl shadow-2xl max-w-md w-full p-6"
                                            initial={{ scale: 0.95, opacity: 0 }}
                                            animate={{ scale: 1, opacity: 1 }}
                                            exit={{ scale: 0.95, opacity: 0 }}
                                            onClick={(e) => e.stopPropagation()}
                                        >
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
                                            <button onClick={() => setGroupModalOpen(false)} className="mt-2 text-sm text-gray-500">닫기</button>
                                        </motion.div>
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </>
                    )}
                </div>
            </main>
        </div>
    );
};

export default IdolSubscribe;
