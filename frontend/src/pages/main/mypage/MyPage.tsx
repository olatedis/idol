import React, { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuthStore } from "../../../stores/authStore";
import Swal from 'sweetalert2';
import Header from "../Header";
import ProfileTab from "./ProfileTab";
import SubscriptionTab from "./SubscriptionTab";
import PaymentHistoryTab from "./PaymentHistoryTab";
import BanHistoryTab from "./BanHistoryTab";
import AdminPage from "./AdminPage";
import AgencyPage from "./AgencyPage";
import NotificationPreferenceTab from "./NotificationPreferenceTab";

type UserMyPageDto = {
    userId: number;
    username: string;
    email: string;
    nickname: string;
    role: string;
    profileImageUrl?: string;
    createdAt?: string;
    phone?: string;
    address?: string;
    agencyId?: number;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const MyPage: React.FC = () => {
    const navigate = useNavigate();
    const { accessToken } = useAuthStore();
    const location = useLocation();

    // UI State
    const [activeTab, setActiveTab] = useState<"profile" | "subscription" | "payment" | "bans" | "notification" | "agency" | "admin">("profile");

    // Data State
    const [userInfo, setUserInfo] = useState<UserMyPageDto | null>(null);
    const [loading, setLoading] = useState(true);

    const fetchMyInfo = async () => {
        try {
            const res = await fetch(`${API_BASE_URL}/users/me`, {
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
            });

            if (res.status === 401) {
                Swal.fire({
                    icon: 'warning',
                    title: '인증 만료',
                    text: '인증이 만료되었습니다. 다시 로그인해주세요.',
                });
                useAuthStore.getState().logout();
                navigate("/");
                return;
            }

            if (!res.ok) throw new Error("사용자 정보를 불러올 수 없습니다.");

            const data = await res.json();
            setUserInfo(data);
        } catch (e) {
            Swal.fire({
                icon: 'error',
                title: '오류',
                text: '사용자 정보를 불러오는 중 오류가 발생했습니다.',
            });
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (!accessToken) {
            navigate("/");
            return;
        }
        fetchMyInfo();
    }, [accessToken, navigate]);

    useEffect(() => {
        const initialTab = location.state?.initialTab

        if(initialTab === "notification") {
            setActiveTab("notification");
        }
    }, [location.state]);

    if (loading) {
        return (
            <div className="min-h-screen bg-gray-50 flex flex-col">
                <Header />
                <main className="flex-1 flex items-center justify-center">
                    <div className="text-gray-500">불러오는 중...</div>
                </main>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Header />
            <main className="flex-1 w-full max-w-4xl mx-auto px-4 py-24">
                <div className="text-2xl font-bold text-gray-800 mb-6">마이페이지</div>

                {/* 탭 네비게이션 */}
                <div className="flex space-x-2 border-b border-gray-200 mb-8 backdrop-blur-md sticky top-16 z-10 bg-gray-50/90 pt-2">
                    <button
                        className={`px-4 py-3 text-sm font-semibold transition-colors relative ${activeTab === "profile" ? "text-idol" : "text-gray-500 hover:text-gray-700"}`}
                        onClick={() => setActiveTab("profile")}
                    >
                        내 프로필
                        {activeTab === "profile" && (
                            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-idol rounded-t-full" />
                        )}
                    </button>
                    {userInfo?.role !== "AGENCY" && userInfo?.role !== "ADMIN" && (
                        <button
                            className={`px-4 py-3 text-sm font-semibold transition-colors relative ${activeTab === "subscription" ? "text-idol" : "text-gray-500 hover:text-gray-700"}`}
                            onClick={() => setActiveTab("subscription")}
                        >
                            구독 내역
                            {activeTab === "subscription" && (
                                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-idol rounded-t-full" />
                            )}
                        </button>
                    )}
                    <button
                        className={`px-4 py-3 text-sm font-semibold transition-colors relative ${activeTab === "payment" ? "text-idol" : "text-gray-500 hover:text-gray-700"}`}
                        onClick={() => setActiveTab("payment")}
                    >
                        결제 내역
                        {activeTab === "payment" && (
                            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-idol rounded-t-full" />
                        )}
                    </button>
                    <button
                        className={`px-4 py-3 text-sm font-semibold transition-colors relative ${activeTab === "bans" ? "text-idol" : "text-gray-500 hover:text-gray-700"}`}
                        onClick={() => setActiveTab("bans")}
                    >
                        제재 이력
                        {activeTab === "bans" && (
                            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-idol rounded-t-full" />
                        )}
                    </button>
                    <button
                        className={`px-4 py-3 text-sm font-semibold transition-colors relative ${activeTab === "notification" ? "text-idol" : "text-gray-500 hover:text-gray-700"}`}
                        onClick={() => setActiveTab("notification")}
                    >
                        알림 설정
                        {activeTab === "notification" && (
                            <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-idol rounded-t-full" />
                        )}
                    </button>
                        {userInfo?.role === "AGENCY" && (
                        <button
                            className={`px-4 py-3 text-sm font-semibold transition-colors relative ${activeTab === "agency" ? "text-idol" : "text-gray-500 hover:text-gray-700"}`}
                            onClick={() => setActiveTab("agency")}
                        >
                            소속사 기능
                            {activeTab === "agency" && (
                                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-idol rounded-t-full" />
                            )}
                        </button>
                    )}
                    {userInfo?.role === "ADMIN" && (
                        <button
                            className={`px-4 py-3 text-sm font-semibold transition-colors relative ${activeTab === "admin" ? "text-idol" : "text-gray-500 hover:text-gray-700"}`}
                            onClick={() => setActiveTab("admin")}
                        >
                            관리자 기능
                            {activeTab === "admin" && (
                                <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-idol rounded-t-full" />
                            )}
                        </button>
                    )}
                </div>

                {/* 탭 컨텐츠 */}
                <div className="bg-white rounded-2xl p-6 shadow-sm border border-gray-100">
                    {activeTab === "profile" && userInfo && (
                        <ProfileTab userInfo={userInfo} onRefresh={fetchMyInfo} />
                    )}

                    {activeTab === "subscription" && userInfo?.role !== "AGENCY" && userInfo?.role !== "ADMIN" && (
                        <SubscriptionTab />
                    )}

                    {activeTab === "payment" && (
                        <PaymentHistoryTab />
                    )}

                    {activeTab === "bans" && (
                        <BanHistoryTab />
                    )}

                    {activeTab === "notification" && (
                        <NotificationPreferenceTab />
                    )}

                    {activeTab === "agency" && (
                        <AgencyPage agencyId={userInfo?.agencyId} />
                    )}

                    {activeTab === "admin" && (
                        <AdminPage />
                    )}
                </div>
            </main>
        </div>
    );
};

export default MyPage;
