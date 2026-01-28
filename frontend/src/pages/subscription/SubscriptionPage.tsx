import React, { useState, useEffect } from "react";
import axios from "axios";
import "./SubscriptionPage.css";

interface Idol {
    id: number;
    name: string;
    imageUrl?: string;
    subscriptionAmount: number;
}

interface UserSubscription {
    id: number;
    idolId: number;
    idolName: string;
    status: "ACTIVE" | "PENDING" | "CANCELED" | "EXPIRED";
    startedAt: string;
    expiredAt?: string;
    autoRenew: boolean;
}

const API_BASE = "http://localhost:8080";
const SUBSCRIPTION_AMOUNT = 9900; // 기본 구독료

const SubscriptionPage: React.FC = () => {
    const [idols, setIdols] = useState<Idol[]>([]);
    const [mySubscriptions, setMySubscriptions] = useState<UserSubscription[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [selectedIdol, setSelectedIdol] = useState<number | null>(null);
    const [autoRenew, setAutoRenew] = useState(true);
    const userId = localStorage.getItem("userId") || "1"; // 테스트용

    useEffect(() => {
        fetchIdols();
        fetchMySubscriptions();
    }, []);

    const fetchIdols = async () => {
        try {
            setLoading(true);
            // 실제로는 idol-service에서 가져오기
            // 여기서는 임시 데이터 사용
            const mockIdols: Idol[] = [
                { id: 1, name: "아이유", subscriptionAmount: 9900, imageUrl: "/idols/iu.jpg" },
                { id: 2, name: "제니", subscriptionAmount: 9900, imageUrl: "/idols/jennie.jpg" },
                { id: 3, name: "지수", subscriptionAmount: 9900, imageUrl: "/idols/jisoo.jpg" },
                { id: 4, name: "로제", subscriptionAmount: 9900, imageUrl: "/idols/rose.jpg" },
            ];
            setIdols(mockIdols);
        } catch (err) {
            setError("아이돌 목록을 불러올 수 없습니다.");
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const fetchMySubscriptions = async () => {
        try {
            const response = await axios.get(`${API_BASE}/subscriptions/me`, {
                headers: {
                    "X-User-Id": userId,
                    "X-Role": "USER",
                },
            });
            setMySubscriptions(response.data);
        } catch (err) {
            console.error("구독 목록 조회 실패:", err);
        }
    };

    const handleSubscribe = async (idolId: number) => {
        try {
            setLoading(true);
            setSelectedIdol(idolId);
            setError(null);

            const orderResponse = await axios.post(
                `${API_BASE}/payments/ready`,
                {
                    userId: parseInt(userId),
                    amount: SUBSCRIPTION_AMOUNT,
                    domain: "SUBSCRIPTION",
                    targetId: idolId,
                },
                {
                    headers: {
                        "Content-Type": "application/json",
                    },
                }
            );

            const orderId = orderResponse.data.orderId;

            const tossPayments = (window as any).TossPayments(
                "test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq"
            );

            const idol = idols.find((i) => i.id === idolId);
            const orderName = `${idol?.name} 구독 - 월간`;

            tossPayments.requestPayment("CARD", {
                amount: SUBSCRIPTION_AMOUNT,
                orderId: orderId,
                orderName: orderName,
                customerEmail: "test@example.com",
                customerName: "구매자",
                successUrl: `${window.location.origin}/subscription/success?orderId=${orderId}&idolId=${idolId}`,
                failUrl: `${window.location.origin}/subscription/fail?orderId=${orderId}`,
            });
        } catch (err) {
            setError("구독 결제를 시작할 수 없습니다.");
            console.error("구독 오류:", err);
        } finally {
            setLoading(false);
            setSelectedIdol(null);
        }
    };

    const handleCancel = async (idolId: number) => {
        if (!window.confirm("정말 구독을 취소하시겠습니까?")) {
            return;
        }

        try {
            setLoading(true);
            await axios.post(
                `${API_BASE}/subscriptions/cancel`,
                { idolId },
                {
                    headers: {
                        "X-User-Id": userId,
                        "X-Role": "USER",
                    },
                }
            );

            setError(null);
            alert("구독이 취소되었습니다.");
            fetchMySubscriptions();
        } catch (err) {
            setError("구독 취소에 실패했습니다.");
            console.error("취소 오류:", err);
        } finally {
            setLoading(false);
        }
    };

    const isSubscribed = (idolId: number) => {
        return mySubscriptions.some(
            (sub) => sub.idolId === idolId && sub.status === "ACTIVE"
        );
    };

    return (
        <div className="subscription-container">
            <div className="subscription-header">
                <h1>🎤 아이돌 구독</h1>
                <p>좋아하는 아이돌을 구독하고 독점 컨텐츠를 즐겨보세요!</p>
            </div>

            {error && <div className="error-message">{error}</div>}

            <div className="subscription-section">
                <h2>내 구독</h2>
                {mySubscriptions.length > 0 ? (
                    <div className="subscription-list">
                        {mySubscriptions
                            .filter((sub) => sub.status === "ACTIVE")
                            .map((sub) => (
                                <div key={sub.id} className="subscription-item">
                                    <div className="subscription-info">
                                        <h3>{sub.idolName}</h3>
                                        <p>상태: {sub.status === "ACTIVE" ? "✅ 활성" : "⏸️ 비활성"}</p>
                                        <p>시작: {new Date(sub.startedAt).toLocaleDateString()}</p>
                                        {sub.autoRenew && (
                                            <p className="auto-renew">🔄 자동 갱신 활성화</p>
                                        )}
                                    </div>
                                    <button
                                        onClick={() => handleCancel(sub.idolId)}
                                        className="btn-cancel"
                                        disabled={loading}
                                    >
                                        구독 취소
                                    </button>
                                </div>
                            ))}
                    </div>
                ) : (
                    <p className="no-subscription">아직 구독한 아이돌이 없습니다.</p>
                )}
            </div>

            <div className="subscription-section">
                <h2>아이돌 구독하기</h2>
                <div className="idols-grid">
                    {idols.map((idol) => (
                        <div key={idol.id} className="idol-card">
                            <div className="idol-image-placeholder">
                                {idol.imageUrl ? (
                                    <img src={idol.imageUrl} alt={idol.name} />
                                ) : (
                                    <div className="placeholder">🎬</div>
                                )}
                            </div>
                            <h3>{idol.name}</h3>
                            <p className="price">월 {idol.subscriptionAmount.toLocaleString()}원</p>

                            {isSubscribed(idol.id) ? (
                                <button className="btn-subscribed" disabled>
                                    ✓ 구독 중
                                </button>
                            ) : (
                                <button
                                    className="btn-subscribe"
                                    onClick={() => handleSubscribe(idol.id)}
                                    disabled={loading}
                                >
                                    {loading && selectedIdol === idol.id
                                        ? "처리 중..."
                                        : "구독하기"}
                                </button>
                            )}
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default SubscriptionPage;
