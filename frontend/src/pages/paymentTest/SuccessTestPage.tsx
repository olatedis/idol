// SuccessPage.tsx
import {useEffect} from "react";
import {useLocation} from 'react-router-dom';



const SuccessTestPage = () => {
    const {search} = useLocation();

    useEffect(() => {
        const params = new URLSearchParams(search);
        console.log(JSON.stringify({
            paymentKey: params.get("paymentKey"),
            orderId: params.get("orderId"),
            amount: Number(params.get("amount"))
        }));
        const paymentConfirmDto = {
            paymentKey: params.get("paymentKey"),
            orderId: params.get("orderId"),
            amount: Number(params.get("amount")),
        };

        fetch("http://localhost:8087/payments/confirm", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(paymentConfirmDto)
        });
    },);

    return <h2>결제 성공 처리 중입니다...</h2>;
};

export default SuccessTestPage;
