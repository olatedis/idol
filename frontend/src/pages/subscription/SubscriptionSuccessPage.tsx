import React, { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import axios from "axios";
import "./SubscriptionSuccessPage.css";

const API_BASE = "http://localhost:8080";

const SubscriptionSuccessPage: React.FC = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [paymentKey, setPaymentKey] = useState<string | null>(null);
    const [confirming, setConfirming] = useState(false);

    const orderId = searchParams.get("orderId");
    const paymentKeyParam = searchParams.get("paymentKey");
    const amount = searchParams.get("amount");
    const idolId = searchParams.get("idolId");
    const userId = localStorage.getItem("userId") || "1";

    useEffect(() => {
        if (!orderId || !paymentKeyParam || !amount) {
            setError("결제 정보가 부족합니다.");
            setLoading(false);
            return;
        }

        setPaymentKey(paymentKeyParam);
        confirmPayment();
    }, []);

    const confirmPayment = async () => {
        try {
            setConfirming(true);
            console.log("결제 승인 요청:", {
                orderId,
                paymentKey: paymentKeyParam,
                amount,
            });

            const response = await axios.post(
                `${API_BASE}/payments/confirm`,
                {
                    orderId: orderId,
                    paymentKey: paymentKeyParam,
                    amount: parseInt(amount || "0"),
                },
                {
                    headers: {
                        "Content-Type": "application/json",
                        "X-User-Id": userId,
                    },
                }
            );

            console.log("결제 승인 성공:", response);
            setError(null);
        } catch (err: any) {
            console.error("결제 승인 실패:", err);
            setError(
                err.response?.data?.message || "결제 승인에 실패했습니다."
            );
        } finally {
            setConfirming(false);
            setLoading(false);
        }
    };

    const handleGoBack = () => {
        navigate("/subscription");
    };

    if (loading && confirming) {
        return (
            <div className="success-container">
                <div className="loading">
                    <div className="spinner"></div>
                    <p>결제를 처리 중입니다...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="success-container">
            {error ? (
                <div className="result-card error">
                    <div className="icon">❌</div>
                    <h1>결제 실패</h1>
                    <p className="message">{error}</p>
                    <p className="order-id">주문번호: {orderId}</p>
                    <button onClick={handleGoBack} className="btn-back">
                        다시 시도하기
                    </button>
                </div>
            ) : (
                <div className="result-card success">
                    <div className="icon">✅</div>
                    <h1>구독 완료!</h1>
                    <p className="message">
                        구독이 성공적으로 결제되었습니다.
                    </p>
                    <div className="details">
                        <div className="detail-row">
                            <span className="label">주문번호</span>
                            <span className="value">{orderId}</span>
                        </div>
                        <div className="detail-row">
                            <span className="label">금액</span>
                            <span className="value">
                                {parseInt(amount || "0").toLocaleString()}원
                            </span>
                        </div>
                        <div className="detail-row">
                            <span className="label">결제수단</span>
                            <span className="value">카드 결제</span>
                        </div>
                        <div className="detail-row">
                            <span className="label">결제일시</span>
                            <span className="value">
                                {new Date().toLocaleString("ko-KR")}
                            </span>
                        </div>
                    </div>

                    <div className="subscription-info">
                        <h2>구독 정보</h2>
                        <ul>
                            <li>✓ 월간 구독이 활성화되었습니다</li>
                            <li>✓ 매월 {parseInt(amount || "0").toLocaleString()}원에 자동 갱신됩니다</li>
                            <li>✓ 언제든 구독을 취소할 수 있습니다</li>
                            <li>✓ 이메일로 청구서가 발송됩니다</li>
                        </ul>
                    </div>

                    <div className="actions">
                        <button onClick={handleGoBack} className="btn-continue">
                            계속하기
                        </button>
                        <a href="/chat" className="btn-chat">
                            채팅방 방문
                        </a>
                    </div>
                </div>
            )}
        </div>
    );
};

export default SubscriptionSuccessPage;
