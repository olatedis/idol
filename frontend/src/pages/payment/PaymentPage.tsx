import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../main/Header';
import { createPaymentReady } from '../../api/payment';
import { loadTossPaymentsScript } from '../../utils/tossPayments';

const PaymentPage: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);

    const concert = location.state?.concert;
    const seats: any[] = location.state?.seats || [];
    const totalPrice: number = location.state?.totalPrice || 0;

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
            const ready = await createPaymentReady({ userId, amount: totalPrice, domain: 'CONCERT', targetId: concert.id });

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

                        <div className="mt-6">
                            <button onClick={handlePay} disabled={loading} className="py-3 px-4 rounded bg-[var(--color-idol-point)] text-white">
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
