import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import Header from '../main/Header';
import { confirmPayment, authorizeBillingKey } from '../../api/payment';

const PaymentComplete: React.FC = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const [status, setStatus] = useState<'processing' | 'success' | 'failed'>('processing');

    useEffect(() => {
        const qs = new URLSearchParams(location.search);
        const type = qs.get('type');
        const paymentKey = qs.get('paymentKey') || qs.get('payment_key') || '';
        const orderId = qs.get('orderId') || qs.get('order_id') || '';
        const amountStr = qs.get('amount') || '0';
        const amount = Number(amountStr);
        const authKey = qs.get('authKey') || qs.get('auth_key') || '';
        const customerKey = qs.get('customerKey') || qs.get('customer_key') || '';
        const idolId = Number(customerKey.split('_')[1]); // customer_{idolId}에서 추출

        if (type === 'billing') {
            // 빌링키 발급 처리
            if (!authKey || !idolId) {
                setStatus('failed');
                return;
            }

            console.log(authKey)
            console.log(idolId)

            authorizeBillingKey({ idolId, authKey, plan: 'MONTHLY' })
                .then(() => setStatus('success'))
                .catch(() => setStatus('failed'));
        } else {
            // 일반 결제 처리
            if (!paymentKey || !orderId) {
                setStatus('failed');
                return;
            }

            const userId = Number(localStorage.getItem('userId') || '1');

            confirmPayment({ paymentKey, orderId, amount }, userId)
                .then(() => setStatus('success'))
                .catch(() => setStatus('failed'));
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
                                <p className="mt-3">구독이 정상적으로 등록되었습니다.</p>
                                <div className="mt-6">
                                    <button onClick={() => navigate('/idol')} className="py-2 px-4 rounded bg-idol-point text-white">아이돌 목록으로</button>
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
