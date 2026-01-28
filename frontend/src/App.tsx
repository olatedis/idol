import './App.css'
import PaymentTestPage from "./pages/paymentTest/PaymentTestPage.tsx";
import {BrowserRouter,Routes,Route} from "react-router-dom";
import SuccessTestPage from "./pages/paymentTest/SuccessTestPage.tsx";

function App() {

  return (
    <>
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<PaymentTestPage />} />
                <Route path="/success" element={<SuccessTestPage />} />
                {/*<Route path="/fail" element={<FailPage />} />*/}
            </Routes>

        </BrowserRouter>
    </>
  )
}

export default App
