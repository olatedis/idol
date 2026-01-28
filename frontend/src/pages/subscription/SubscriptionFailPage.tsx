import React, { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import "./SubscriptionFailPage.css";

const SubscriptionFailPage: React.FC = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const orderId = searchParams.get("orderId");
    const errorCode = searchParams.get("code");
    const errorMessage = searchParams.get("message") || "결제에 실패했습니다.";

    const getErrorDetails = (code: string | null) => {
        const errorMap: Record<string, { title: string; description: string }> = {
            INVALID_CARD: {
                title: "결제 카드 오류",
                description:
                    "카드 정보가 올바르지 않습니다. 다시 확인해주세요.",
            },
            CARD_DECLINED: {
                title: "카드 거절",
                description:
                    "카드사에서 거절했습니다. 카드사에 문의해주세요.",
            },
            INSUFFICIENT_FUNDS: {
                title: "잔액 부족",
                description:
                    "카드의 잔액이 부족합니다. 계좌를 확인해주세요.",
            },
            EXPIRED_CARD: {
                title: "유효기한 만료",
                description:
                    "카드의 유효기한이 지났습니다. 다른 카드를 사용해주세요.",
            },
            PAYMENT_CANCELLED: {
                title: "결제 취소",
                description: "사용자가 결제를 취소했습니다.",
            },
        };

        return (
            errorMap[code || ""] || {
                title: "결제 실패",
                description: errorMessage,
            }
        );
    };

    const { title, description } = getErrorDetails(errorCode);

    const handleRetry = () => {
        navigate("/subscription");
    };

    const handleGoHome = () => {
        navigate("/");
    };

    return (
        <div className="fail-container">
            <div className="result-card error-card">
                <div className="icon">❌</div>
                <h1>{title}</h1>
                <p className="message">{description}</p>

                {orderId && (
                    <div className="error-details">
                        <p>
                            <strong>주문번호:</strong> {orderId}
                        </p>
                        {errorCode && (
                            <p>
                                <strong>오류코드:</strong> {errorCode}
                            </p>
                        )}
                    </div>
                )}

                <div className="help-section">
                    <h2>문제 해결 방법</h2>
                    <ul>
                        <li>올바른 카드 정보를 입력했는지 확인하세요</li>
                        <li>카드의 유효기한을 확인하세요</li>
                        <li>카드사 앱에서 거래 차단을 해제하세요</li>
                        <li>다른 결제 수단을 사용해보세요</li>
                        <li>고객 센터에 문의하세요: 1234-5678</li>
                    </ul>
                </div>

                <div className="actions">
                    <button onClick={handleRetry} className="btn-retry">
                        다시 시도하기
                    </button>
                    <button onClick={handleGoHome} className="btn-home">
                        홈으로 가기
                    </button>
                </div>
            </div>
        </div>
    );
};

export default SubscriptionFailPage;
