import React, { useEffect, useState } from "react";
import { useAuthStore } from "../../../stores/authStore";
import { api } from "../../../api/axios";

type PaymentDto = {
    id: number;
    userId: number;
    amount: number;
    pointAmount: number;
    status: string; // REQUESTED, APPROVING, COMPLETED, FAILED, CANCELED
    paymentMethod: string;
    orderId: string;
    paymentKey: string;
    completedAt: string;
};

const formatKstDateTime = (dateString?: string) => {
    if (!dateString) return "-";
    const parseString = dateString.endsWith('Z') || dateString.includes('+') ? dateString : dateString + 'Z';
    const date = new Date(parseString);
    const kstDate = new Date(date.getTime() + 9 * 60 * 60 * 1000);
    return kstDate.toISOString().replace('T', ' ').substring(0, 16);
};

// const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const PaymentHistoryTab: React.FC = () => {
    const { accessToken } = useAuthStore();
    const [payments, setPayments] = useState<PaymentDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const fetchPayments = async () => {
            try {
                // 백엔드 API 명세에 따라 '/payments/me' (혹은 유사한 endpoint)가 존재한다고 가정합니다.
                const res = await api.get("/payments/me");

                const data = res.data;

                // 최신 결제순으로 정렬
                const sortedData = data.sort((a: PaymentDto, b: PaymentDto) =>
                    new Date(b.completedAt).getTime() - new Date(a.completedAt).getTime()
                );

                setPayments(sortedData);
            } catch (err: any) {
                setError(err.message || "오류가 발생했습니다.");
            } finally {
                setLoading(false);
            }
        };

        if (accessToken) {
            fetchPayments();
        }
    }, [accessToken]);

    if (loading) return <div className="text-gray-500 py-8 text-center">불러오는 중...</div>;
    if (error) return <div className="text-red-500 py-8 text-center">{error}</div>;

    if (payments.length === 0) {
        return (
            <div className="py-12 bg-gray-50 rounded-xl border border-gray-100 flex flex-col items-center justify-center">
                <svg className="w-12 h-12 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
                </svg>
                <div className="text-gray-500 font-medium">결제 내역이 없습니다.</div>
            </div>
        );
    }

    const getStatusStyle = (status: string) => {
        switch (status) {
            case "COMPLETED":
                return "bg-green-100 text-green-700 border-green-200";
            case "FAILED":
                return "bg-red-100 text-red-700 border-red-200";
            case "CANCELED":
                return "bg-gray-100 text-gray-600 border-gray-200";
            case "APPROVING":
            case "REQUESTED":
            default:
                return "bg-yellow-100 text-yellow-700 border-yellow-200";
        }
    };

    const getStatusLabel = (status: string) => {
        switch (status) {
            case "COMPLETED": return "결제 완료";
            case "FAILED": return "결제 실패";
            case "CANCELED": return "결제 취소";
            case "APPROVING": return "승인 대기중";
            case "REQUESTED": return "요청됨";
            default: return status;
        }
    };

    return (
        <div className="space-y-4">
            <h3 className="text-lg font-bold text-gray-800 mb-4">최근 결제 내역</h3>

            <div className="overflow-x-auto rounded-xl border border-gray-200">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th scope="col" className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">주문번호</th>
                            <th scope="col" className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">결제금액</th>
                            <th scope="col" className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">상태</th>
                            <th scope="col" className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">결제일시</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {payments.map((payment) => (
                            <tr key={payment.id} className="hover:bg-gray-50 transition-colors">
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-medium">
                                    {payment.orderId.substring(0, 13)}...
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-bold tabular-nums">
                                    {(payment?.amount || 0).toLocaleString()} <span className="text-gray-500 font-normal">원</span>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm">
                                    <span className={`px-2.5 py-1 inline-flex text-xs leading-5 font-semibold rounded-full border ${getStatusStyle(payment.status)}`}>
                                        {getStatusLabel(payment.status)}
                                    </span>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {formatKstDateTime(payment.completedAt)}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default PaymentHistoryTab;
