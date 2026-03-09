import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../main/Header';
import { getIdol } from '../../api/payment';

const IdolSubscribe: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [idol, setIdol] = useState<any>(null);

    const idolId = location.state?.idolId;

    useEffect(() => {
        if (!idolId) return;
        getIdol(Number(idolId)).then(setIdol).catch(() => null);
    }, [idolId]);

    const handleChoose = (plan: 'MONTHLY' | 'ANNUAL') => {
        navigate(`/payment`, { state: { domain: 'SUBSCRIPTION', idolId, plan } });
    };

    return (
        <div className="min-h-screen bg-idol-bg">
            <Header />
            <main className="pt-[80px] px-6">
                <div className="max-w-3xl mx-auto">
                    <h2 className="text-2xl font-semibold mb-4">아이돌 구독</h2>
                    {idol ? (
                        <div className="bg-white rounded p-6 shadow">
                            <div className="flex items-center gap-6">
                                <img src={idol.profileImage} alt={idol.stageName} className="w-24 h-24 rounded-full object-cover" />
                                <div>
                                    <div className="text-lg font-bold">{idol.stageName}</div>
                                    <div className="text-sm text-gray-600">{idol.agencyName}</div>
                                </div>
                            </div>

                            <div className="mt-6 grid grid-cols-2 gap-4">
                                <button onClick={() => handleChoose('MONTHLY')} className="py-3 rounded bg-idol-point text-white">정기 구독 — 매월 5,000원</button>
                                <button onClick={() => handleChoose('ANNUAL')} className="py-3 rounded bg-gray-800 text-white">연간 결제 — 50,000원</button>
                            </div>
                        </div>
                    ) : (
                        <div>로딩 중...</div>
                    )}
                </div>
            </main>
        </div>
    );
};

export default IdolSubscribe;
