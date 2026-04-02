import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../main/Header';
import { confirmPayment, authorizeBillingKey } from '../../api/payment';
import { useAuthStore } from "../../stores/authStore.ts";
import { api } from '../../api/axios';


const PaymentComplete: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [status, setStatus] = useState<'processing' | 'success' | 'failed'>('processing');
    const [pending, setPending] = useState<any>(null);
    const { user } = useAuthStore();
    const userId = user?.userId

    useEffect(() => {
        const qs = new URLSearchParams(location.search);
        const type = qs.get('type');
        const paymentKey = qs.get('paymentKey') || qs.get('payment_key') || '';
        const orderId = qs.get('orderId') || qs.get('order_id') || '';
        const amountStr = qs.get('amount') || '0';
        const amount = Number(amountStr);
        const authKey = qs.get('authKey') || qs.get('auth_key') || '';
        // 이전 페이지에서 저장한 대기중 구독 정보를 사용 (sessionStorage)
        let pendingLocal: { idolId?: number; plan?: string; customerKey?: string; subscriptionId?: number } | null = null;
        try {
            const raw = sessionStorage.getItem('pendingSubscription');
            if (raw) pendingLocal = JSON.parse(raw);
        } catch (e) {
            console.warn('sessionStorage read failed', e);
        }
        setPending(pendingLocal);

        const cancelPendingReservations = async () => {
            try {
                const rawPending = sessionStorage.getItem('pendingReservations');
                if (!rawPending) return;
                const parsed = JSON.parse(rawPending) as { reservationIds?: number[] };
                const ids = parsed?.reservationIds || [];
                if (ids.length === 0) return;
                for (const id of ids) {
                    await api.delete(`/reservations/${id}`, {
                        headers: { 'X-User-Id': String(userId) }
                    });
                }
                try { sessionStorage.removeItem('pendingReservations'); } catch {}
            } catch (e) {
                console.error('예약 취소 실패', e);
            }
        };

        const deletePendingPayment = async () => {
            if (orderId && userId) {
                try {
                    await api.delete(`/payments/${orderId}`, {
                        headers: { 'X-User-Id': String(userId) }
                    });
                } catch (e) {
                    console.error('pending payment delete failed', e);
                }
            }
        };

        if (type === 'billing') {
            // 빌링키 발급 처리
            const pendingIdolId = pendingLocal?.idolId;
            const customerKey = pendingLocal?.customerKey;
            if (!authKey || !pendingIdolId || !customerKey) {
                setStatus('failed');
                return;
            }

            authorizeBillingKey({ idolId: pendingIdolId as number, authKey, plan: 'MONTHLY', customerKey })
                .then(() => {
                    // 처리 후 세션청소
                    try { sessionStorage.removeItem('pendingSubscription'); } catch { }
                    setStatus('success');
                })
                .catch(async () => {
                    await cancelPendingReservations();
                    // also clear any pending subscription data
                    try { sessionStorage.removeItem('pendingSubscription'); } catch {}
                    // delete possible pending payment
                    await deletePendingPayment();
                    setStatus('failed');
                });
        } else {
            // 일반 결제 처리
            if (!paymentKey || !orderId) {
                setStatus('failed');
                return;
            }

            confirmPayment({ paymentKey, orderId, amount }, userId)
                .then(() => {
                    try { sessionStorage.removeItem('pendingSubscription'); } catch { }
                    try { sessionStorage.removeItem('pendingReservations'); } catch {}
                    setStatus('success');
                })
                .catch(async () => {
                    await cancelPendingReservations();
                    // cancel pending subscription if exists
                    try {
                        const raw = sessionStorage.getItem('pendingSubscription');
                        if (raw && userId) {
                            const info = JSON.parse(raw) as any;
                            if (info.subscriptionId) {
                                await api.delete(`/subscriptions/${info.subscriptionId}`, {
                                    headers: { 'X-User-Id': String(userId) }
                                });
                            }
                        }
                        try { sessionStorage.removeItem('pendingSubscription'); } catch {}
                    } catch (e) {
                        console.error('구독 취소 실패', e);
                    }
                    // also clean pending payment record
                    await deletePendingPayment();
                    setStatus('failed');
                });
        }
    }, [location.search]);

    return (
        <div className="min-h-screen bg-idol-bg">
            <Header />
            <main className="pt-[80px] px-6">
                <div className="max-w-2xl mx-auto">
                    <div className="bg-white rounded p-6 shadow text-center">
                        {status === 'processing' && <div>결제 처리 중입니다...</div>}
                        {status === 'success' && (
                            <div>
                                <h3 className="text-xl font-semibold">결제 완료</h3>
                                <p className="mt-3">
                                    {pending?.subscriptionId ? '구독이 정상적으로 등록되었습니다.' : '결제가 정상적으로 완료되었습니다.'}
                                </p>
                                <div className="mt-6">
                                    <button onClick={() => navigate(pending?.subscriptionId ? '/idol' : '/')} className="py-2 px-4 rounded bg-idol-point text-white">
                                        {pending?.subscriptionId ? '아이돌 목록으로' : '메인으로'}
                                    </button>
                                </div>
                            </div>
                        )}

                        {status === 'failed' && (
                            <div>
                                <h3 className="text-xl font-semibold">결제 실패</h3>
                                <p className="mt-3">결제 정보가 없거나 처리 중 오류가 발생했습니다.</p>
                                <div className="mt-6">
                                    <button onClick={() => navigate('/idol')} className="py-2 px-4 rounded border">돌아가기</button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
};

export default PaymentComplete;
