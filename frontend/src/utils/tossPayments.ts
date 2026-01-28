/**
 * 토스페이먼츠 스크립트를 로드합니다
 */
export function loadTossPaymentsScript(): Promise<void> {
    return new Promise((resolve, reject) => {
        // 이미 로드된 경우
        if ((window as any).TossPayments) {
            resolve();
            return;
        }

        const script = document.createElement('script');
        script.src = 'https://js.tosspayments.com/v1';
        script.async = true;

        script.onload = () => {
            resolve();
        };

        script.onerror = () => {
            reject(new Error('토스페이먼츠 스크립트 로드 실패'));
        };

        document.body.appendChild(script);
    });
}
