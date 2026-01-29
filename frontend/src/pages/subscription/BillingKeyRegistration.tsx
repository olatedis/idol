import React, { useState, useEffect } from 'react';
import './BillingKeyRegistration.css';

interface BillingKeyRegistrationProps {
  idolId: number;
  plan: 'MONTHLY' | 'ANNUAL';
  onSuccess?: (billingKeyId: number) => void;
  onError?: (error: string) => void;
}

export const BillingKeyRegistration: React.FC<BillingKeyRegistrationProps> = ({
  idolId,
  plan,
  onSuccess,
  onError
}) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [scriptLoaded, setScriptLoaded] = useState(false);

  // Toss Payments 스크립트 로드
  useEffect(() => {
    const script = document.querySelector(
      'script[src="https://js.tosspayments.com/v1"]'
    );

    if (script) {
      setScriptLoaded(true);
    } else {
      const newScript = document.createElement('script');
      newScript.src = 'https://js.tosspayments.com/v1';
      newScript.async = true;
      newScript.onload = () => setScriptLoaded(true);
      newScript.onerror = () => {
        setError('결제 시스템 로드 실패. 다시 시도해주세요.');
      };
      document.body.appendChild(newScript);
    }
  }, []);

  const handleBillingKeyRegistration = async () => {
    if (!scriptLoaded) {
      setError('결제 시스템이 준비되지 않았습니다. 잠시만 기다려주세요.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const clientKey = process.env.REACT_APP_TOSS_CLIENT_KEY;
      const userId = localStorage.getItem('userId');

      if (!clientKey || !userId) {
        throw new Error('클라이언트 키 또는 사용자 정보가 없습니다.');
      }

      // Toss Payments 객체 가져오기
      const TossPayments = (window as any).TossPayments;
      if (!TossPayments) {
        throw new Error('Toss Payments 라이브러리를 찾을 수 없습니다.');
      }

      // 빌링키 인증 요청
      const tossPayments = TossPayments(clientKey);
      const response = await tossPayments.requestBillingAuth({
        method: 'CARD'
      });

      if (response.authKey) {
        // 백엔드에 빌링키 발급 요청
        const authResponse = await fetch('/subscriptions/billing/authorize', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-User-Id': userId,
            'X-Role': 'USER'
          },
          body: JSON.stringify({
            idolId,
            authKey: response.authKey,
            plan
          })
        });

        if (!authResponse.ok) {
          const errorData = await authResponse.json();
          throw new Error(errorData.message || '빌링키 발급 실패');
        }

        const billingKeyData = await authResponse.json();
        
        if (onSuccess) {
          onSuccess(billingKeyData.billingKeyId);
        }

        // 성공 메시지
        alert(`
빌링키가 등록되었습니다.
카드: ${billingKeyData.cardIssuer} ${billingKeyData.cardNumber}
이제 자동으로 ${plan === 'MONTHLY' ? '매월' : '매년'} 결제됩니다.
        `);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '알 수 없는 오류 발생';
      setError(errorMessage);
      if (onError) {
        onError(errorMessage);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="billing-key-registration">
      <div className="billing-container">
        <div className="billing-header">
          <h2>결제 수단 등록</h2>
          <p>정기 구독을 위해 결제 수단을 등록해주세요.</p>
        </div>

        <div className="billing-info">
          <div className="info-item">
            <span className="label">구독 플랜</span>
            <span className="value">
              {plan === 'MONTHLY' ? '📅 월간' : '🎁 연간 (10% 할인)'}
            </span>
          </div>
          <div className="info-item">
            <span className="label">결제 방식</span>
            <span className="value">카드 정기결제</span>
          </div>
        </div>

        {error && (
          <div className="error-message">
            <span className="error-icon">⚠️</span>
            <span>{error}</span>
          </div>
        )}

        <button
          className="btn-register"
          onClick={handleBillingKeyRegistration}
          disabled={loading || !scriptLoaded}
        >
          {loading ? '등록 중...' : '카드 등록하기'}
        </button>

        <div className="billing-notice">
          <h3>결제 수단 등록 안내</h3>
          <ul>
            <li>신용카드, 체크카드, 교통카드로 등록 가능합니다.</li>
            <li>첫 번째 구독료는 지금 결제되고, 이후 {plan === 'MONTHLY' ? '매월' : '매년'} 자동 결제됩니다.</li>
            <li>결제 수단은 언제든 삭제할 수 있습니다.</li>
            <li>모든 거래는 Toss Payments를 통해 안전하게 처리됩니다.</li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default BillingKeyRegistration;
