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

    const nextBillingDate = plan === 'MONTHLY'
        ? (() => {
            const d = new Date();
            d.setMonth(d.getMonth() + 1);
            d.setHours(0, 0, 0, 0);
            return d;
        })()
        : null;
    const expiryDate = plan === 'ANNUAL'
        ? (() => {
            const d = new Date();
            d.setFullYear(d.getFullYear() + 1);
            d.setHours(0, 0, 0, 0);
            return d;
        })()
        : null;

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
        console.log('[DEBUG] handlePay 시작 - domain:', domain, 'idolId:', idolId, 'plan:', plan);
        if (!user || !user.userId) {
            showErrorToast('로그인이 필요합니다.');
            return;
        }
        setLoading(true);
        try {
            console.log('[DEBUG] Toss SDK 로드 시도 및 clientKey 확인...');
            await loadTossPaymentsScript();
            const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY || 'test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq';
            console.log('[DEBUG] clientKey:', clientKey);
            
            const TossPayments = (window as any).TossPayments;
            if (!TossPayments) {
                console.error('[DEBUG] TossPayments 객체를 찾을 수 없습니다.');
                throw new Error('TossPayments not available');
            }

            const toss = TossPayments(clientKey);
            const userId = user.userId;
            console.log('[DEBUG] toss 객체 준비 완료, userId:', userId);

            if (domain === 'CONCERT') {
                console.log('[DEBUG] CONCERT 결제 로직 진입');
                if (!concert || seats.length === 0) {
                    console.warn('[DEBUG] 콘서트 정보 또는 좌석 정보 누락');
                    return;
                }
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
                console.log('[DEBUG] createPaymentReady 응답:', ready);
                setReadyOrderId(ready.orderId);

                console.log('[DEBUG] toss.requestPayment 호출 (CONCERT)');
                toss.requestPayment('카드', {
                    amount: ready.amount,
                    orderId: ready.orderId,
                    orderName: `${concert.title} 예매`,
                    successUrl: `${window.location.origin}/payment/complete`,
                    failUrl: `${window.location.origin}/payment/complete?fail=true`
                });
            } else if (domain === 'SUBSCRIPTION') {
                console.log('[DEBUG] SUBSCRIPTION 결제 로직 진입');
                if (!idolId || !plan) {
                    console.warn('[DEBUG] idolId 또는 plan 정보 누락');
                    return;
                }
                // 먼저 백엔드에 pending 구독을 생성
                console.log('[DEBUG] createSubscription 요청 보냄...');
                const createRes = await createSubscription(user?.userId, { idolId: idolId!, plan: plan!, autoRenew: true });
                console.log('[DEBUG] createSubscription 응답 성공:', createRes);
                
                const subscriptionId = createRes.subscriptionId;
                const orderId = createRes.orderId;
                const amount = createRes.amount;

                // 생성을 저장할 session (customerKey은 월정기결제시 사용)
                // crypto.randomUUID()는 HTTPS 환경에서만 작동하므로 폴백 처리
                const customerKey = (window.crypto && window.crypto.randomUUID) 
                    ? window.crypto.randomUUID() 
                    : `user-${userId}-${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
                
                console.log('[DEBUG] customerKey 생성 완료:', customerKey);
                try { sessionStorage.setItem('pendingSubscription', JSON.stringify({ idolId, plan, subscriptionId, customerKey, orderId })); } catch (e) {}

                if (plan === 'MONTHLY') {
                    console.log('[DEBUG] MONTHLY(정기결제) - requestBillingAuth 호출 시도...');
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
                            console.log('[DEBUG] global requestBillingAuth 호출');
                            await globalFunc(clientKey, '카드', {
                                customerKey,
                                successUrl: `${window.location.origin}/payment/complete?type=billing`,
                                failUrl: `${window.location.origin}/payment/complete?type=billing&fail=true`
                            });
                        } else {
                            console.error('[DEBUG] 빌링키 발급 함수를 찾을 수 없습니다.');
                            throw new Error('Billing auth method unavailable');
                        }
                    }
                } else {
                    console.log('[DEBUG] ANNUAL(일시불) - requestPayment 호출 시도...');
                    if (!orderId || !amount) {
                        console.error('[DEBUG] orderId 또는 amount 가 null입니다.');
                        throw new Error('결제 정보가 생성되지 않았습니다.');
                    }
                    
                    setReadyOrderId(orderId);

                    console.log('[DEBUG] toss.requestPayment 최종 호출 파라미터:', { orderId, amount, orderName: `${idol?.stageName || '아이돌'} 구독` });
                    toss.requestPayment('카드', {
                        amount: amount,
                        orderId: orderId,
                        orderName: `${idol?.stageName || '아이돌'} 구독`,
                        successUrl: `${window.location.origin}/payment/complete`,
                        failUrl: `${window.location.origin}/payment/complete?fail=true`
                    });
                    console.log('[DEBUG] toss.requestPayment 호출 완료');
                }
            }
        } catch (e: any) {
            console.error('[DEBUG] 결제 도중 예외 발생:', e);
            // 사용자가 직접 취소한 경우는 정상 흐름 (콘솔 에러 없이 조용히 종료)
            if (e?.code === 'USER_CANCEL' || e?.message === '취소되었습니다.') {
                setLoading(false);
                return;
            }
            showErrorToast('결제 준비 중 오류가 발생했습니다. (자세한 내용은 콘솔 확인)');
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-[var(--color-idol-bg)]">
            <Header />

            <main className="pt-[80px] px-6">
                <div className="max-w-2xl mx-auto">
                    <h2 className="text-3xl font-extrabold mb-4 text-slate-800">결제</h2>
                    <div className="bg-white rounded-2xl p-6 border-idol-mid border-2">
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
                                    <div className="border-t pt-4 mt-3 p-3 rounded-lg bg-slate-50">
                                        <div className="text-sm text-slate-600">
                                            {plan === 'MONTHLY' && nextBillingDate && (
                                                <>다음 결제일: {nextBillingDate.toLocaleDateString('ko-KR')} 00:00</>
                                            )}
                                            {plan === 'ANNUAL' && expiryDate && (
                                                <>구독 만료일: {expiryDate.toLocaleDateString('ko-KR')}</>
                                            )}
                                        </div>
                                        <div className="text-xs text-slate-500 mt-1">토스 결제를 완료하면 구독이 자동 활성화됩니다.</div>
                                    </div>
                                </>
                            )}
                        </div>
                        <div className="border-t pt-4 mt-4">
                            <div className="mt-6 flex text-center">
                                <button onClick={handlePay} disabled={loading} className="hover:cursor-pointer w-full py-3 px-4 rounded-xl bg-[var(--color-idol)] hover:bg-idol-point text-white font-bold shadow-lg hover:shadow-xl transition-all duration-200">
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
                                }} className="ml-3 py-3 px-4 rounded-xl border w-40 hover:cursor-pointer ">결제 취소</button>
                            </div>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
};

export default PaymentPage;
