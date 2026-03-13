import React, { useEffect, useState } from "react";
import { showAlert, showConfirm, showErrorToast, showSuccessToast } from "../../../utils/alert";
import { useAuthStore } from "../../../stores/authStore";

type SubscriptionDto = {
    subscriptionId: number;
    providerId: number;
    targetId: number; // 아이돌 ID
    targetName: string; // 백엔드에서 아직 제공하지 않으면 클라이언트에서 조합하거나 추가 필요
    price: number;
    billingKeyId: number;
    createdAt: string;
    updatedAt: string;
    targetType: string;
    autoRenew: boolean;
    status?: string; // Added for cancellation status
};

import { api } from "../../../api/axios";

// 구독료 변수 설정 (나중에 변경 시 여기서 일괄 수정)
// 실제 가격은 백엔드 SubscriptionPlan과 일치해야 합니다.
const IDOL_SUBSCRIPTION_PRICE = 9900; // 월간
const GROUP_SUBSCRIPTION_PRICE = 0;

const formatKstDate = (dateString?: string) => {
    if (!dateString) return "-";
    const parseString = dateString.endsWith('Z') || dateString.includes('+') ? dateString : dateString + 'Z';
    const date = new Date(parseString);
    const kstDate = new Date(date.getTime() + 9 * 60 * 60 * 1000);
    return kstDate.toISOString().split('T')[0];
};

const SubscriptionTab: React.FC = () => {
    const { accessToken } = useAuthStore();
    const [subscriptions, setSubscriptions] = useState<SubscriptionDto[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const fetchSubscriptions = async () => {
            try {
                // 1. 아이돌 구독 내역 가져오기
                let idolSubs = [];
                try {
                    const idolRes = await api.get(`/subscriptions/me`);
                    idolSubs = Array.isArray(idolRes.data) ? idolRes.data : [];
                } catch (err) {
                    console.warn("Failed to fetch idol subscriptions");
                }

                // 각 아이돌 구독에 targetName(stageName) 매핑
                const enrichedIdolSubs = await Promise.all(
                    idolSubs.map(async (sub: any) => {
                        let name = String(sub.idolId);
                        try {
                            const detailRes = await api.get(`/idols/${sub.idolId}`);
                            const detailData = detailRes.data;
                            name = detailData.stageName;
                            if (detailData.groupName) {
                                name = `[${detailData.groupName}] ${name}`;
                            }
                        } catch (e) {
                            console.warn("Idol name fetch failed", sub.idolId);
                        }
                        return {
                            ...sub,
                            targetType: "IDOL",
                            targetId: sub.idolId,
                            targetName: sub.idolStageName || name,
                            price: IDOL_SUBSCRIPTION_PRICE,
                            createdAt: sub.startedAt || sub.createdAt || "",
                            updatedAt: sub.expiredAt || sub.updatedAt || "",
                            autoRenew: sub.autoRenew,
                            status: sub.status || "ACTIVE", // Default status
                        };
                    })
                );

                // 2. 그룹 구독 내역 가져오기
                let groupSubs = [];
                try {
                    const groupRes = await api.get(`/subscriptions/groups/me`);
                    groupSubs = Array.isArray(groupRes.data) ? groupRes.data : [];
                } catch (err) {
                    console.warn("Failed to fetch group subscriptions");
                }

                // 그룹 구독 데이터를 동일한 SubscriptionDto 포맷으로 매핑
                const enrichedGroupSubs = groupSubs.map((sub: any) => {
                    return {
                        subscriptionId: sub.subscriptionId || Math.random(), // 그룹 구독엔 subscriptionId가 별도 키일 수 있음
                        providerId: 0,
                        targetId: sub.groupId,
                        targetName: sub.groupName || String(sub.groupId),
                        price: GROUP_SUBSCRIPTION_PRICE,
                        billingKeyId: 0,
                        createdAt: sub.startedAt || sub.createdAt || "",
                        updatedAt: sub.expiredAt || sub.updatedAt || "",
                        targetType: "GROUP",
                        autoRenew: sub.autoRenew,
                        status: sub.status || "ACTIVE", // Default status
                    };
                });

                // 두 목록 합치기
                setSubscriptions([...enrichedIdolSubs, ...enrichedGroupSubs]);
            } catch (err: any) {
                setError(err.message || "오류가 발생했습니다.");
            } finally {
                setLoading(false);
            }
        };

        if (accessToken) {
            fetchSubscriptions();
        }
    }, [accessToken]);

    const handleCancelSubscription = async (sub: SubscriptionDto) => {
        const ok = await showConfirm("구독을 해지하시겠습니까?", `'${sub.targetName}' 구독을 해지하시겠습니까?`, "해지");

        if (!ok) return;

        try {
            await api.post(`/subscriptions/${sub.subscriptionId}/cancel`);
            // UI를 즉시 갱신 (상태를 CANCELLED로 변경)
            setSubscriptions((prev) =>
                prev.map((item) =>
                    item.subscriptionId === sub.subscriptionId
                        ? { ...item, status: "CANCELLED", autoRenew: false } // Also set autoRenew to false
                        : item
                )
            );
            showSuccessToast("구독이 해지되었습니다.");
        } catch (err: any) {
            console.error("구독 해지 실패", err);
            showErrorToast(err?.response?.data?.message || "구독 해지 중 오류가 발생했습니다.");
        }
    };

    if (loading) return <div className="text-gray-500 py-8 text-center">불러오는 중...</div>;
    if (error) return <div className="text-red-500 py-8 text-center">{error}</div>;

    if (subscriptions.length === 0) {
        return (
            <div className="py-12 bg-gray-50 rounded-xl border border-gray-100 flex flex-col items-center justify-center">
                <svg className="w-12 h-12 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                </svg>
                <div className="text-gray-500 font-medium">현재 구독 중인 내역이 없습니다.</div>
            </div>
        );
    }

    const idolSubscriptions = subscriptions.filter(s => s.targetType === "IDOL");
    const groupSubscriptions = subscriptions.filter(s => s.targetType === "GROUP");

    const renderCard = (sub: SubscriptionDto) => (
        <div key={sub.subscriptionId} className="border border-idol/20 bg-white rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow relative overflow-hidden">
            <div className="absolute top-0 right-0 w-16 h-16 bg-gradient-to-br from-idol/10 to-transparent rounded-bl-full pointer-events-none" />

            <div className="flex justify-between items-start mb-4">
                <div>
                    <span className={`inline-block px-2 py-1 text-xs font-bold rounded mb-2 ${sub.autoRenew ? "bg-idol/10 text-idol" : "bg-gray-100 text-gray-500"
                        }`}>
                        {sub?.targetType === "IDOL" ? "아이돌 구독" : "그룹 구독"}
                        {!sub.autoRenew && " (해지 대기)"}
                    </span>
                    <h3 className="text-lg font-bold text-gray-900">
                        {sub?.targetName || `Target ID: ${sub?.targetId}`}
                    </h3>
                </div>
                {sub?.targetType === "IDOL" && (
                    <div className="text-right">
                        {sub.autoRenew ? (
                            <>
                                <div className="text-sm font-semibold text-gray-900">{(sub?.price || 0).toLocaleString()} 원</div>
                                <div className="text-xs text-gray-500">/ 월 단위 결제</div>
                            </>
                        ) : (
                            <div className="text-xs font-semibold text-red-500">
                                {formatKstDate(sub?.updatedAt)} 까지 이용 가능
                            </div>
                        )}
                    </div>
                )}
            </div>

            <div className="text-xs text-gray-500 space-y-1 mt-4 pt-4 border-t border-gray-100">
                <div className="flex justify-between items-center">
                    <span>구독 기간</span>
                    <span className="text-gray-900 font-medium">
                        {formatKstDate(sub?.createdAt)} ~ {formatKstDate(sub?.updatedAt)}
                    </span>
                </div>
            </div>

            <button
                onClick={() => handleCancelSubscription(sub)}
                disabled={!sub.autoRenew}
                className={`w-full mt-4 py-2 border text-sm font-semibold rounded-xl transition-colors ${!sub.autoRenew
                    ? "border-gray-200 bg-gray-50 text-gray-400 cursor-not-allowed"
                    : "border-red-100 text-red-500 hover:bg-red-50"
                    }`}
            >
                {sub.autoRenew ? "구독 해지" : "해지된 구독입니다"}
            </button>
        </div>
    );

    return (
        <div className="space-y-8">
            {/* 아이돌 구독 섹션 */}
            <div>
                <h2 className="text-xl font-bold text-gray-800 mb-4 border-b border-gray-200 pb-2">아이돌 구독</h2>
                {idolSubscriptions.length > 0 ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        {idolSubscriptions.map(renderCard)}
                    </div>
                ) : (
                    <div className="text-gray-500 text-sm py-4">구독 중인 아이돌이 없습니다.</div>
                )}
            </div>

            {/* 그룹 구독 섹션 */}
            <div>
                <h2 className="text-xl font-bold text-gray-800 mb-4 border-b border-gray-200 pb-2">그룹 구독</h2>
                {groupSubscriptions.length > 0 ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        {groupSubscriptions.map(renderCard)}
                    </div>
                ) : (
                    <div className="text-gray-500 text-sm py-4">구독 중인 그룹이 없습니다.</div>
                )}
            </div>
        </div>
    );
};

export default SubscriptionTab;
