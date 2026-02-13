import React, { useEffect, useState } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import Header from '../../main/Header';
import { createPaymentReady, getIdol } from '../../api/payment';
import { loadTossPaymentsScript } from '../../utils/tossPayments';

const PRICE_MAP: Record<string, number> = {
    MONTHLY: 5000,
    ANNUAL: 50000,
};

const PaymentPage: React.FC = () => {
    const { idolId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [idol, setIdol] = useState<any>(null);

    const qs = new URLSearchParams(location.search);
    const plan = (qs.get('plan') || 'MONTHLY') as 'MONTHLY' | 'ANNUAL';
    const amount = PRICE_MAP[plan];

    useEffect(() => {
        if (!idolId) return;
        getIdol(Number(idolId)).then(setIdol).catch(() => null);
    }, [idolId]);

    const handlePay = async () => {
        if (!idolId) return;
        setLoading(true);
        try {
            const userId = Number(localStorage.getItem('userId') || '1');
            const ready = await createPaymentReady({ userId, amount, domain: 'SUBSCRIPTION', targetId: Number(idolId) });

            await loadTossPaymentsScript();

            const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY || 'test_client_key';
            const TossPayments = (window as any).TossPayments;
            if (!TossPayments) throw new Error('TossPayments not available');

            const toss = TossPayments(clientKey);

            toss.requestPayment('카드', {
                amount: ready.amount,
                orderId: ready.orderId,
                orderName: `${idol?.stageName || '아이돌'} 구독`,
                successUrl: `${window.location.origin}/payment/complete`,
                failUrl: `${window.location.origin}/payment/complete?fail=true`
            });

        } catch (e) {
            console.error(e);
            alert('결제 준비 중 오류가 발생했습니다. 콘솔을 확인하세요.');
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-idol-bg">
            <Header />

            <main className="pt-[80px] px-6">
                <div className="max-w-2xl mx-auto">
                    <h2 className="text-2xl font-semibold mb-4">결제</h2>

                    <div className="bg-white rounded p-6 shadow">
                        <div className="flex items-center gap-4">
                            <img src={idol?.profileImage} alt={idol?.stageName} className="w-20 h-20 rounded-full object-cover" />
                            <div>
                                <div className="font-bold">{idol?.stageName}</div>
                                <div className="text-sm text-gray-600">{plan} • {amount.toLocaleString()}원</div>
                            </div>
                        </div>

                        <div className="mt-6">
                            <button onClick={handlePay} disabled={loading} className="py-3 px-4 rounded bg-idol-point text-white">
                                {loading ? '로딩 중...' : '결제하기'}
                            </button>
                            <button onClick={() => navigate(-1)} className="ml-3 py-3 px-4 rounded border">취소</button>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
};

export default PaymentPage;
