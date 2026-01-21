import React, { useState } from "react";

// ⚠️ 테스트 전제
// 토스페이먼츠 테스트 클라이언트 키 사용
// 주문 생성 → 결제창 → 결제 승인(confirm) 흐름 테스트용

const CLIENT_KEY = "test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq"; // 토스 테스트 클라이언트 키
const API_BASE = "http://localhost:8087";

const PaymentTestPage: React.FC = () => {
    const [amount, setAmount] = useState<number>(12000);
    const [orderId, setOrderId] = useState<string | null>(null);

    // 1️⃣ 주문 생성
    const createOrder = async () => {
        const res = await fetch(`${API_BASE}/payments/ready`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-User-Id": "1" // 테스트용 유저
            },
            body: JSON.stringify({
                amount,
                type: "CONCERT",
                targetId: 1
            })
        });

        const data = await res.json();
        setOrderId(data.orderId);
    };

    // 2️⃣ 토스 결제창 호출
    const requestPayment = async () => {
        if (!orderId) return;

        // @ts-ignore
        const tossPayments = window.TossPayments(CLIENT_KEY);

        tossPayments.requestPayment("CARD", {
            amount,
            orderId,
            orderName: "콘서트 좌석 결제",
            successUrl: window.location.origin + "/success",
            failUrl: window.location.origin + "/fail"
        });
    };

    return (
        <div style={{ padding: "40px" }}>
            <h2>토스 결제 테스트</h2>

            <div>
                <label>결제 금액: </label>
                <input
                    type="number"
                    value={amount}
                    onChange={(e) => setAmount(Number(e.target.value))}
                />
            </div>

            <button onClick={createOrder} style={{ marginTop: 20 }}>
                1. 주문 생성
            </button>

            {orderId && (
                <>
                    <p>orderId: {orderId}</p>
                    <button onClick={requestPayment}>
                        2. 토스 결제창 열기
                    </button>
                </>
            )}
        </div>
    );
};

export default PaymentTestPage;
