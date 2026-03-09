import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../main/Header';
import { useAuthStore } from "../../stores/authStore";
import { createPaymentReady } from '../../api/payment';
import { loadTossPaymentsScript } from '../../utils/tossPayments';
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const PaymentPage: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [method, setMethod] = useState<'toss' | 'card' | 'bank'>('toss');

    const concert = location.state?.concert;
    const seats: any[] = location.state?.seats || [];
    const totalPrice: number = location.state?.totalPrice || 0;
    const reservationIds: number[] = location.state?.reservationIds || [];
    const { user } = useAuthStore();

    const handlePay = async () => {
        if (!concert || seats.length === 0) return;
        setLoading(true);
        try {
            await loadTossPaymentsScript();
            const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY || 'test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq';
            const TossPayments = (window as any).TossPayments;
            if (!TossPayments) throw new Error('TossPayments not available');

            const toss = TossPayments(clientKey);
            const userId = Number(localStorage.getItem('userId') || '1');
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

            // 현재는 toss 결제만 연결. 추후 method에 따라 분기 가능.
            toss.requestPayment('카드', {
                amount: ready.amount,
                orderId: ready.orderId,
                orderName: `${concert.title} 예매`,
                successUrl: `${window.location.origin}/payment/complete`,
                failUrl: `${window.location.origin}/payment/complete?fail=true`
            });
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
                        <div className="space-y-4">
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
                                    // 취소: 대기중 예약이 있으면 해제 후 뒤로
                                    try {
                                        if (reservationIds && reservationIds.length > 0 && user?.userId) {
                                            for (const id of reservationIds) {
                                                await fetch(`${API_BASE_URL}/reservations/${id}`, {
                                                    method: 'DELETE',
                                                    headers: { 'X-User-Id': String(user.userId) }
                                                });
                                            }
                                            try { sessionStorage.removeItem('pendingReservations'); } catch { }
                                        }
                                    } catch (e) {
                                        console.error('예약 취소 실패', e);
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
