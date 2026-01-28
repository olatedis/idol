import React, {useState, useEffect} from "react";
import axios from "axios";
import {loadTossPaymentsScript} from "../../utils/tossPayments";
import "./SubscriptionPage.css";

type SubscriptionPlan = "MONTHLY" | "ANNUAL";

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
    nextRenewalAt?: string;
    plan: SubscriptionPlan;
    autoRenew: boolean;
}

const API_BASE = "http://localhost:8080";
const SUBSCRIPTION_AMOUNTS = {
    MONTHLY: 9900,
    ANNUAL: 89100  // 월간 * 12 * 0.9 = 10% 할인
};

const SubscriptionPage: React.FC = () => {
    const [idols, setIdols] = useState<Idol[]>([]);
    const [mySubscriptions, setMySubscriptions] = useState<UserSubscription[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [selectedIdol, setSelectedIdol] = useState<number | null>(null);
    const [scriptLoaded, setScriptLoaded] = useState(false);
    const userId = localStorage.getItem("userId") || "1";

    const fetchIdols = async () => {
        try {
            const response = await fetch(`${API_BASE}/idols`, {
                method: "GET",
            headers: {
                "X-User-Id": userId
            }
            });
            const data = await response.json();
            setIdols(data);
        } catch (err) {
            console.error("아이돌 목록 조회 실패:", err);
            setError("아이돌 목록을 불러올 수 없습니다.");
        }
    };

    const fetchMySubscriptions = async () => {
        try {
            const response = await axios.get(`${API_BASE}/subscriptions/me`);
            setMySubscriptions(response.data);
        } catch (err) {
            console.error("내 구독 조회 실패:", err);
        }
    };

    const handleCancel = async (idolId: number) => {
        if (!window.confirm("구독을 취소하시겠습니까?")) return;

        try {
            setLoading(true);
            await fetch(`${API_BASE}/subscriptions/cancel`, {
                method:"POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-User-Id": userId
                },
                body: JSON.stringify({
                    "idolId": idolId
                })
            });
            setError(null);
            fetchMySubscriptions();
        } catch (err) {
            setError("구독 취소에 실패했습니다.");
            console.error("취소 오류:", err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        // 토스페이먼츠 스크립트 로드
        loadTossPaymentsScript()
            .then(() => setScriptLoaded(true))
            .catch((err) => {
                console.error("토스페이먼츠 스크립트 로드 실패:", err);
                setError("결제 시스템을 초기화할 수 없습니다.");
            });

        fetchIdols();
        fetchMySubscriptions();
    }, []);

    const handleSubscribe = async (idolId: number, plan: SubscriptionPlan) => {
        if (!scriptLoaded) {
            setError("결제 시스템이 아직 준비 중입니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        try {
            setLoading(true);
            setSelectedIdol(idolId);
            setError(null);

            // 1단계: 결제 주문 생성
            const orderResponse = await axios.post(
                `${API_BASE}/payments/ready`,
                {
                    userId: parseInt(userId),
                    amount: SUBSCRIPTION_AMOUNTS[plan],
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

            // 2단계: 토스페이먼츠 결제창 호출
            const tossPayments = (window as any).TossPayments(
                "test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq"
            );

            const idol = idols.find((i) => i.id === idolId);
            const planLabel = plan === "MONTHLY" ? "월간" : "연간 (10% 할인)";
            const orderName = `${idol?.name} 구독 - ${planLabel}`;

            tossPayments.requestPayment("CARD", {
                amount: SUBSCRIPTION_AMOUNTS[plan],
                orderId: orderId,
                orderName: orderName,
                customerEmail: "test@example.com",
                customerName: "구매자",
                successUrl: `${window.location.origin}/subscription/success?orderId=${orderId}&idolId=${idolId}&plan=${plan}`,
                failUrl: `${window.location.origin}/subscription/fail?orderId=${orderId}`,
            });
        } catch (err) {
            setError("구독 결제를 시작할 수 없습니다.");
            console.error("구독 오류:", err);
            setLoading(false);
            setSelectedIdol(null);
        }
    };

    const isSubscribed = (idolId: number) => {
        return mySubscriptions.some(
            (sub) => sub.idolId === idolId && sub.status === "ACTIVE"
        );
    };

    const getSubscription = (idolId: number) => {
        return mySubscriptions.find(
            (sub) => sub.idolId === idolId && sub.status === "ACTIVE"
        );
    };

    return (
        <div className="subscription-container">
            <div className="subscription-header">
                <h1>아이돌 구독</h1>
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
                                        <p>플랜: {sub.plan === "MONTHLY" ? "📅 월간" : "🎁 연간 (10% 할인)"}</p>
                                        <p>시작: {new Date(sub.startedAt).toLocaleDateString()}</p>
                                        {sub.nextRenewalAt && (
                                            <p>다음 갱신: {new Date(sub.nextRenewalAt).toLocaleDateString()}</p>
                                        )}
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
                <div className="plan-info">
                    <div className="plan-card monthly">
                        <h3>📅 월간 구독</h3>
                        <p className="price">9,900원/월</p>
                        <ul>
                            <li>✓ 매달 자동 갱신</li>
                            <li>✓ 언제든 취소 가능</li>
                        </ul>
                    </div>
                    <div className="plan-card annual">
                        <h3>🎁 연간 구독</h3>
                        <p className="price">89,100원/연</p>
                        <p className="discount">10% 할인 (월간 대비)</p>
                        <ul>
                            <li>✓ 최대 12개월 이용</li>
                            <li>✓ 언제든 취소 가능</li>
                        </ul>
                    </div>
                </div>

                <div className="idols-grid">
                    {idols.map((idol) => {
                        const subscription = getSubscription(idol.id);
                        return (
                            <div key={idol.id} className="idol-card">
                                <div className="idol-image-placeholder">
                                    {idol.imageUrl ? (
                                        <img src={idol.imageUrl} alt={idol.name}/>
                                    ) : (
                                        <div className="placeholder">🎬</div>
                                    )}
                                </div>
                                <h3>{idol.name}</h3>

                                {isSubscribed(idol.id) ? (
                                    <div className="subscribed-info">
                                        <button className="btn-subscribed" disabled>
                                            ✓ 구독 중
                                        </button>
                                        <p className="plan-label">
                                            {subscription?.plan === "MONTHLY" ? "📅 월간" : "🎁 연간"}
                                        </p>
                                    </div>
                                ) : (
                                    <div className="plan-buttons">
                                        <button
                                            className="btn-subscribe"
                                            onClick={() => handleSubscribe(idol.id, "MONTHLY")}
                                            disabled={loading && selectedIdol === idol.id}
                                            title="월간 구독 - 9,900원"
                                        >
                                            {loading && selectedIdol === idol.id ? "처리 중..." : "월간 구독"}
                                        </button>
                                        <button
                                            className="btn-subscribe-annual"
                                            onClick={() => handleSubscribe(idol.id, "ANNUAL")}
                                            disabled={loading && selectedIdol === idol.id}
                                            title="연간 구독 - 89,100원 (10% 할인)"
                                        >
                                            {loading && selectedIdol === idol.id ? "처리 중..." : "연간 구독"}
                                        </button>
                                    </div>
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
};

export default SubscriptionPage;
