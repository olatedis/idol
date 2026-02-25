import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../main/Header';
import { createPaymentReady, getIdol, authorizeBillingKey } from '../../api/payment';
import { loadTossPaymentsScript } from '../../utils/tossPayments';

const PRICE_MAP: Record<string, number> = {
    MONTHLY: 5000,
    ANNUAL: 50000,
};

const PaymentPage: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [idol, setIdol] = useState<any>(null);

    const idolId = location.state?.idolId;
    const plan = location.state?.plan || 'MONTHLY';
    const amount = PRICE_MAP[plan];

    useEffect(() => {
        if (!idolId) return;
        getIdol(Number(idolId)).then(setIdol).catch(() => null);
    }, [idolId]);

    const handlePay = async () => {
        if (!idolId) return;
        setLoading(true);
        try {
            await loadTossPaymentsScript();

            const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY || 'test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq';
            const TossPayments = (window as any).TossPayments;
            if (!TossPayments) throw new Error('TossPayments not available');

            const toss = TossPayments(clientKey);

            if (plan === 'MONTHLY') {
                // 빌링키 발급 (정기 구독)
                // 고객키 생성 (일관된 키 사용)
                const customerKey = `customer_user_${idolId}`;
                
                // 이전 페이지에서 사용할 수 있도록 idolId, plan, customerKey를 세션에 저장
                try {
                    sessionStorage.setItem('pendingSubscription', JSON.stringify({ 
                        idolId: Number(idolId), 
                        plan: 'MONTHLY',
                        customerKey: customerKey
                    }));
                } catch (e) {
                    console.warn('sessionStorage not available', e);
                }

                toss.requestBillingAuth('카드', {
                    amount,
                    orderId: `billing_${Date.now()}`,
                    orderName: `${idol?.stageName || '아이돌'} 월간 구독`,
                    customerKey: customerKey,
                    successUrl: `${window.location.origin}/payment/complete?type=billing`,
                    failUrl: `${window.location.origin}/payment/complete?fail=true`
                });
            } else {
                // 일반 결제 (연간)
                // 이전 페이지에서 사용할 수 있도록 idolId와 plan을 세션에 저장
                try {
                    sessionStorage.setItem('pendingSubscription', JSON.stringify({ idolId: Number(idolId), plan: 'ANNUAL' }));
                } catch (e) {
                    console.warn('sessionStorage not available', e);
                }
                const userId = Number(localStorage.getItem('userId') || '1');
                const ready = await createPaymentReady({ userId, amount, domain: 'SUBSCRIPTION', targetId: Number(idolId) });

                toss.requestPayment('카드', {
                    amount: ready.amount,
                    orderId: ready.orderId,
                    orderName: `${idol?.stageName || '아이돌'} 연간 구독`,
                    successUrl: `${window.location.origin}/payment/complete`,
                    failUrl: `${window.location.origin}/payment/complete?fail=true`
                });
            }

        } catch (e) {
            console.error(e);
            alert('결제 준비 중 오류가 발생했습니다.');
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
                                <div className="text-sm text-gray-600">{plan === 'MONTHLY' ? '정기 구독' : '연간 결제'} • {amount.toLocaleString()}원</div>
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
