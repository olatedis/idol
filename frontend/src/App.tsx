import './App.css'
import PaymentTestPage from "./pages/paymentTest/PaymentTestPage.tsx";
import {BrowserRouter,Routes,Route} from "react-router-dom";
import SuccessTestPage from "./pages/paymentTest/SuccessTestPage.tsx";
import SubscriptionPage from "./pages/subscription/SubscriptionPage.tsx";
import SubscriptionSuccessPage from "./pages/subscription/SubscriptionSuccessPage.tsx";
import SubscriptionFailPage from "./pages/subscription/SubscriptionFailPage.tsx";
import { useEffect } from 'react';

function App() {
  // 토스페이먼츠 스크립트 로드
  useEffect(() => {
    const script = document.createElement('script');
    script.src = 'https://js.tosspayments.com/v1';
    script.async = true;
    document.body.appendChild(script);
    
    return () => {
      if (document.body.contains(script)) {
        document.body.removeChild(script);
      }
    };
  }, []);

  return (
    <>
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<PaymentTestPage />} />
                <Route path="/success" element={<SuccessTestPage />} />
                <Route path="/subscription" element={<SubscriptionPage />} />
                <Route path="/subscription/success" element={<SubscriptionSuccessPage />} />
                <Route path="/subscription/fail" element={<SubscriptionFailPage />} />
            </Routes>

        </BrowserRouter>
    </>
  )
}

export default App
