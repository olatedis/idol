import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import Header from './Header.tsx';
import {
    getIdol,
    fetchGroups,
    fetchGroupIdols,
    subscribeGroup
} from '../../api/payment.ts';
import {api} from '../../api/axios.ts';
import { useAuthStore } from '../../stores/authStore.ts';
import { showErrorToast, showSuccessToast } from '../../utils/alert';

interface GroupDto {
    groupId: number;
    name: string;
    groupImage?: string;
}

interface IdolDto {
    idolId: number;
    stageName: string;
    profileImage?: string;
    agencyId: number;
    groupId: number;
}

const IdolSubscribe: React.FC = () => {

    const location = useLocation();
    const navigate = useNavigate();
    const { user } = useAuthStore();

    const idolId = location.state?.idolId;

    const [groups, setGroups] = useState<GroupDto[]>([]);
    const [groupModalOpen, setGroupModalOpen] = useState(false);
    const [selectedGroup, setSelectedGroup] = useState<GroupDto | null>(null);
    const [groupIdols, setGroupIdols] = useState<IdolDto[]>([]);

    const [selectedIdol, setSelectedIdol] = useState<IdolDto | null>(null);

    const [subscribedIdols, setSubscribedIdols] = useState<Set<number>>(new Set());
    const [subscribedGroups, setSubscribedGroups] = useState<Set<number>>(new Set());

    const [loading, setLoading] = useState(true);

    const isSubscribedIdol = (idolId: number) => subscribedIdols.has(idolId);
    const isSubscribedGroup = (groupId: number) => subscribedGroups.has(groupId);

    useEffect(() => {
        const loadSubscriptions = async () => {
            try {

                let idolSubs: any[] = [];
                try {
                    const idolRes = await api.get(`/subscriptions/me`);
                    idolSubs = Array.isArray(idolRes.data) ? idolRes.data : [];
                } catch {}

                let groupSubs: any[] = [];
                try {
                    const groupRes = await api.get(`/subscriptions/groups/me`);
                    groupSubs = Array.isArray(groupRes.data) ? groupRes.data : [];
                } catch {}

                const idolSet = new Set<number>(idolSubs.map(sub => sub.idolId));
                const groupSet = new Set<number>(groupSubs.map(sub => sub.groupId));

                setSubscribedIdols(idolSet);
                setSubscribedGroups(groupSet);

            } catch {
                showErrorToast('구독 정보를 불러오지 못했습니다.');
            } finally {
                setLoading(false);
            }
        };

        if (user?.userId) {
            loadSubscriptions();
        } else {
            setLoading(false);
        }
    }, [user]);

    useEffect(() => {
        if (idolId) {
            getIdol(Number(idolId))
                .then(data => setSelectedIdol(data))
                .catch(() => null);

            setSelectedIdol({
                idolId,
                stageName: '',
                profileImage: '',
                agencyId: 0,
                groupId: 0
            });

        } else {
            fetchGroups().then(setGroups).catch(() => {});
        }
    }, [idolId]);

    const handleChoose = (plan: 'MONTHLY' | 'ANNUAL') => {

        if (!selectedIdol) return;

        if (isSubscribedIdol(selectedIdol.idolId)) {
            showErrorToast('이미 구독중인 아이돌입니다.');
            return;
        }

        navigate(`/payment`, {
            state: {
                domain: 'SUBSCRIPTION',
                idolId: selectedIdol.idolId,
                agencyId: selectedIdol.agencyId,
                plan
            }
        });
    };

    const openGroup = (group: GroupDto) => {

        setSelectedGroup(group);
        setGroupModalOpen(true);

        fetchGroupIdols(group.groupId)
            .then(setGroupIdols)
            .catch(() => setGroupIdols([]));
    };

    const handleGroupSubscribe = async () => {

        if (!selectedGroup) return;

        if (isSubscribedGroup(selectedGroup.groupId)) {
            showErrorToast('이미 구독중인 그룹입니다.');
            return;
        }

        try {

            await subscribeGroup(
                user?.userId || 0,
                selectedGroup.groupId,
                selectedGroup.name
            );

            showSuccessToast('그룹 구독이 완료되었습니다.');

            setSubscribedGroups(prev => new Set(prev).add(selectedGroup.groupId));

            setGroupModalOpen(false);

        } catch {
            showErrorToast('그룹 구독에 실패했습니다.');
        }
    };

    const handleIdolClick = (idol: IdolDto) => {
        setSelectedIdol(idol);
        setGroupModalOpen(false);
    };

    const closeIdolModal = () => setSelectedIdol(null);

    if (loading) {
        return <div className="p-10 text-center">Loading...</div>;
    }

    return (
        <div className="min-h-screen bg-idol-bg">

            <Header />

            <main className="pt-[80px] px-6">

                <div className="max-w-3xl mx-auto">

                    <h2 className="text-2xl font-semibold mb-4">아이돌 구독</h2>

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

                                    <h3 className="text-lg font-bold mb-4">
                                        {selectedIdol.stageName || '구독 대상'}
                                    </h3>

                                    {selectedIdol.profileImage && (
                                        <img
                                            src={selectedIdol.profileImage}
                                            alt={selectedIdol.stageName}
                                            className="w-24 h-24 rounded-full mb-4"
                                        />
                                    )}

                                    <div className="mt-6 grid grid-cols-2 gap-4">

                                        <button
                                            disabled={isSubscribedIdol(selectedIdol.idolId)}
                                            onClick={() => handleChoose('MONTHLY')}
                                            className={`py-3 rounded text-white 
                                            ${isSubscribedIdol(selectedIdol.idolId)
                                                ? 'bg-gray-400 cursor-not-allowed'
                                                : 'bg-idol-point'
                                            }`}
                                        >
                                            정기 구독 — 매월 9,900원
                                        </button>

                                        <button
                                            disabled={isSubscribedIdol(selectedIdol.idolId)}
                                            onClick={() => handleChoose('ANNUAL')}
                                            className={`py-3 rounded text-white 
                                            ${isSubscribedIdol(selectedIdol.idolId)
                                                ? 'bg-gray-400 cursor-not-allowed'
                                                : 'bg-gray-800'
                                            }`}
                                        >
                                            연간 결제 — 89,100원
                                        </button>

                                    </div>

                                    {isSubscribedIdol(selectedIdol.idolId) && (
                                        <div className="text-center text-sm text-red-500 mt-3">
                                            이미 구독중인 아이돌입니다
                                        </div>
                                    )}

                                    <button
                                        onClick={closeIdolModal}
                                        className="mt-4 text-sm text-gray-500"
                                    >
                                        닫기
                                    </button>

                                </motion.div>

                            </motion.div>

                        </AnimatePresence>
                    )}

                    {!idolId && (

                        <>
                            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-6">

                                {groups.map(group => {

                                    const subscribed = isSubscribedGroup(group.groupId);

                                    return (
                                        <div
                                            key={group.groupId}
                                            className="cursor-pointer"
                                            onClick={() => openGroup(group)}
                                        >

                                            <img
                                                src={
                                                    group.groupImage ||
                                                    `https://api.dicebear.com/7.x/identicon/svg?seed=${group.groupId}`
                                                }
                                                alt={group.name}
                                                className="w-full h-32 object-cover rounded-lg"
                                            />

                                            <div className="mt-2 text-center font-medium">
                                                {group.name}
                                            </div>

                                            {subscribed && (
                                                <div className="text-xs text-center text-red-500">
                                                    구독중
                                                </div>
                                            )}

                                        </div>
                                    );
                                })}

                            </div>

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
                                            initial={{ scale: 0.95 }}
                                            animate={{ scale: 1 }}
                                            exit={{ scale: 0.95 }}
                                            onClick={(e) => e.stopPropagation()}
                                        >

                                            <h3 className="text-lg font-bold mb-4">
                                                {selectedGroup.name} 구독
                                            </h3>

                                            <button
                                                disabled={isSubscribedGroup(selectedGroup.groupId)}
                                                onClick={handleGroupSubscribe}
                                                className={`mb-4 py-2 px-4 rounded text-white
                                                ${isSubscribedGroup(selectedGroup.groupId)
                                                    ? 'bg-gray-400 cursor-not-allowed'
                                                    : 'bg-idol-point'
                                                }`}
                                            >
                                                그룹 구독
                                            </button>

                                            <div className="mb-4">

                                                <div className="font-semibold mb-2">
                                                    아이돌 선택
                                                </div>

                                                <ul className="max-h-60 overflow-y-auto">
                                                    {groupIdols.map(i => {

                                                        const subscribed = isSubscribedIdol(i.idolId);

                                                        return (
                                                            <li
                                                                key={i.idolId}
                                                                className={`py-2 px-2 rounded
                                                                ${subscribed
                                                                    ? 'bg-gray-100 text-gray-400'
                                                                    : 'cursor-pointer hover:bg-gray-100'
                                                                }`}
                                                                onClick={() =>
                                                                    !subscribed && handleIdolClick(i)
                                                                }
                                                            >
                                                                {i.stageName}

                                                                {subscribed && (
                                                                    <span className="ml-2 text-xs text-red-500">
                                                                        (구독중)
                                                                    </span>
                                                                )}

                                                            </li>
                                                        );
                                                    })}

                                                    {groupIdols.length === 0 && (
                                                        <li>아이돌 정보가 없습니다.</li>
                                                    )}

                                                </ul>

                                            </div>

                                            <button
                                                onClick={() => setGroupModalOpen(false)}
                                                className="mt-2 text-sm text-gray-500"
                                            >
                                                닫기
                                            </button>

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