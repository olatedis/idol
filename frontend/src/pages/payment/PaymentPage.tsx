import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../main/Header';
import { useAuthStore } from "../../stores/authStore";
import { createPaymentReady, getIdol, createSubscription } from '../../api/payment';
import { showErrorToast } from '../../utils/alert';
import { loadTossPaymentsScript } from '../../utils/tossPayments';
import { api } from '../../api/axios';

// base url is handled by axios instance

const PaymentPage: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [method, setMethod] = useState<'toss' | 'card' | 'bank'>('toss');

    const rawDomain = location.state?.domain || 'CONCERT';
    const domain: 'CONCERT' | 'SUBSCRIPTION' =
        String(rawDomain).toUpperCase() === 'SUBSCRIPTION' ? 'SUBSCRIPTION' : 'CONCERT';
    const concert = location.state?.concert;
    const seats: any[] = location.state?.seats || [];
    const totalPrice: number = location.state?.totalPrice || 0;
    const reservationIds: number[] = location.state?.reservationIds || [];

    // subscription-specific
    const idolId: number | null = location.state?.idolId || null;
    const plan: 'MONTHLY' | 'ANNUAL' | null = location.state?.plan || null;
    const [idol, setIdol] = useState<{ stageName: string } | null>(null);

    useEffect(() => {
        if (domain === 'SUBSCRIPTION' && idolId) {
            getIdol(idolId).then(data => setIdol({ stageName: data.stageName })).catch(() => null);
        }
    }, [domain, idolId]);
    const { user } = useAuthStore();

    const [readyOrderId, setReadyOrderId] = useState<string | null>(null);

    const deletePending = async (orderId: string) => {
        try {
            await api.delete(`/payments/${orderId}`, {
                headers: { 'X-User-Id': String(user?.userId) }
            });
        } catch (e) {
        }
    };

    const handlePay = async () => {
        if (!user || !user.userId) {
            showErrorToast('로그인이 필요합니다.');
            return;
        }
        setLoading(true);
        try {
            await loadTossPaymentsScript();
            const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY || 'test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq';
            const TossPayments = (window as any).TossPayments;
            if (!TossPayments) throw new Error('TossPayments not available');

            const toss = TossPayments(clientKey);
            const userId = Number(localStorage.getItem('userId'));

            if (domain === 'CONCERT') {
                if (!concert || seats.length === 0) return;
                // 저장: 결제 완료/실패 시 사용할 대기중 예약 정보
                try { sessionStorage.setItem('pendingReservations', JSON.stringify({ reservationIds })); } catch (e) { /* ignore */ }

                const ready = await createPaymentReady({
                    userId,
                    amount: totalPrice,
                    domain: 'CONCERT',
                    targetId: concert.id,
                    agencyId: concert.agencyId,
                    reservationIds
                });
                setReadyOrderId(ready.orderId);

                toss.requestPayment('카드', {
                    amount: ready.amount,
                    orderId: ready.orderId,
                    orderName: `${concert.title} 예매`,
                    successUrl: `${window.location.origin}/payment/complete`,
                    failUrl: `${window.location.origin}/payment/complete?fail=true`
                });
            } else if (domain === 'SUBSCRIPTION') {
                if (!idolId || !plan) return;
                // 먼저 백엔드에 pending 구독을 생성
                const createRes = await createSubscription(user?.userId, { idolId: idolId!, plan: plan!, autoRenew: true });
                const subscriptionId = createRes.subscriptionId;

                // 생성을 저장할 session (customerKey은 월정기결제시 사용)
                const customerKey = crypto.randomUUID();
                try { sessionStorage.setItem('pendingSubscription', JSON.stringify({ idolId, plan, subscriptionId, customerKey })); } catch (e) {}

                if (plan === 'MONTHLY') {
                    // 월간 구독은 빌링키 발급으로 처리 (정기결제)
                    const billingFunc = toss.requestBillingAuth;
                    if (typeof billingFunc === 'function') {
                        await billingFunc('카드', {
                            customerKey,
                            successUrl: `${window.location.origin}/payment/complete?type=billing`,
                            failUrl: `${window.location.origin}/payment/complete?type=billing&fail=true`
                        });
                    } else {
                        // fallback to global function if instance method missing
                        const globalFunc = (window as any).requestBillingAuth;
                        if (typeof globalFunc === 'function') {
                            await globalFunc(clientKey, '카드', {
                                customerKey,
                                successUrl: `${window.location.origin}/payment/complete?type=billing`,
                                failUrl: `${window.location.origin}/payment/complete?type=billing&fail=true`
                            });
                        } else {
                            throw new Error('Billing auth method unavailable');
                        }
                    }
                } else {
                    // 연간 구독은 일시불 처리
                    const amount = 89100;
                    const ready = await createPaymentReady({
                        userId,
                        amount,
                        domain: 'SUBSCRIPTION',
                        targetId: subscriptionId,
                        agencyId: location.state.agencyId,
                    });
                    setReadyOrderId(ready.orderId);

                    toss.requestPayment('카드', {
                        amount: ready.amount,
                        orderId: ready.orderId,
                        orderName: `${idol?.stageName || '아이돌'} 구독`,
                        successUrl: `${window.location.origin}/payment/complete`,
                        failUrl: `${window.location.origin}/payment/complete?fail=true`
                    });
                }
            }
        } catch (e) {
            showErrorToast('결제 준비 중 오류가 발생했습니다.');
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
                        <div className="space-y-4">
                            {domain === 'CONCERT' ? (
                                <>
                                    <div>
                                        <div className="font-bold text-lg">{concert?.title || '알 수 없는 콘서트'}</div>
                                        <div className="text-sm text-gray-600">{concert ? new Date(concert.concertDate).toLocaleString('ko-KR') : ''}</div>
                                    </div>
                                    <div className="border-t pt-4">
                                        <div className="font-semibold mb-2">선택된 좌석</div>
                                        <ul className="list-disc list-inside text-gray-700 text-sm">
                                            {seats.map((s) => (
                                                <li key={s.id}>{s.seatNumber} - {s.price.toLocaleString()}원</li>
                                            ))}
                                        </ul>
                                    </div>
                                    <div className="border-t pt-4">
                                        <div className="font-bold">총 금액: {totalPrice.toLocaleString()}원</div>
                                    </div>
                                </>
                            ) : (
                                <>
                                    <div>
                                        <div className="font-bold text-lg">{idol?.stageName || '아이돌 구독'}</div>
                                        <div className="text-sm text-gray-600">{plan === 'ANNUAL' ? '연간 결제 89,100원' : '월간 결제 9,900원'}</div>
                                    </div>
                                    <div className="border-t pt-4">
                                        <div className="font-bold">총 금액: {(plan === 'ANNUAL' ? 89100 : 9900).toLocaleString()}원</div>
                                    </div>
                                </>
                            )}
                        </div>
                        <div className="border-t pt-4 mt-4">
                            <div className="font-semibold mb-2">결제 수단</div>
                            <div className="flex gap-3 items-center">
                                <label className="flex items-center gap-2">
                                    <input type="radio" name="method" value="toss" checked={method === 'toss'} onChange={() => setMethod('toss')} />
                                    <span className="text-sm">Toss (카드)</span>
                                </label>
                                <label className="flex items-center gap-2">
                                    <input type="radio" name="method" value="card" checked={method === 'card'} onChange={() => setMethod('card')} />
                                    <span className="text-sm">신용/체크카드</span>
                                </label>
                                <label className="flex items-center gap-2">
                                    <input type="radio" name="method" value="bank" checked={method === 'bank'} onChange={() => setMethod('bank')} />
                                    <span className="text-sm">계좌이체(추후)</span>
                                </label>
                            </div>

                            <div className="mt-6">
                                <button onClick={handlePay} disabled={loading} className="py-3 px-4 rounded bg-[var(--color-idol-point)] text-white">
                                    {loading ? '로딩 중...' : '결제하기'}
                                </button>
                                <button onClick={async () => {
                                    // 취소: domain 따라 대기중 데이터 제거 후 뒤로
                                    try {
                                        if (domain === 'CONCERT') {
                                            if (reservationIds && reservationIds.length > 0 && user?.userId) {
                                                for (const id of reservationIds) {
                                                    await api.delete(`/reservations/${id}`, {
                                                        headers: { 'X-User-Id': String(user.userId) }
                                                    });
                                                }
                                                try { sessionStorage.removeItem('pendingReservations'); } catch {}
                                            }
                                        } else if (domain === 'SUBSCRIPTION') {
                                            const raw = sessionStorage.getItem('pendingSubscription');
                                            if (raw && user?.userId) {
                                                const info = JSON.parse(raw);
                                                if (info.subscriptionId) {
                                                    await api.delete(`/subscriptions/${info.subscriptionId}`, {
                                                        headers: { 'X-User-Id': String(user.userId) }
                                                    });
                                                }
                                            }
                                            try { sessionStorage.removeItem('pendingSubscription'); } catch {}
                                        }
                                        if (readyOrderId) {
                                            await deletePending(readyOrderId);
                                            setReadyOrderId(null);
                                        }
                                    } catch (e) {
                                    }
                                    navigate(-1);
                                }} className="ml-3 py-3 px-4 rounded border">취소</button>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
};

export default PaymentPage;
