import React, {useEffect, useState} from "react";
import {useAuthStore} from "../../../stores/authStore";
import {
    getNotificationPreference,
    updateNotificationPreference,
    type NotificationPreferenceResponse,
} from "../../../api/notificationApi";

const NotificationPreferenceTab: React.FC = () => {
    const {accessToken} = useAuthStore();

    const [preference, setPreference] = useState<NotificationPreferenceResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    const loadPreference = async () => {
        if (!accessToken) return;

        try {
            setLoading(true);
            const data = await getNotificationPreference(accessToken);
            setPreference(data);
        } catch (error) {
            console.error(error);
            alert("알림 설정을 불러오는 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadPreference();
    }, [accessToken]);

    const handleToggle = async (
        key: "chatEnabled" | "voteEnabled" | "ticketEnabled" | "noticeEnabled"
    ) => {
        if (!accessToken || !preference || saving) return;

        const nextValue = !preference[key];
        const nextPreference = {
            ...preference,
            [key]: nextValue,
        };

        setPreference(nextPreference);

        try {
            setSaving(true);
            const saved = await updateNotificationPreference(accessToken, {
                chatEnabled: nextPreference.chatEnabled,
                voteEnabled: nextPreference.voteEnabled,
                ticketEnabled: nextPreference.ticketEnabled,
                noticeEnabled: nextPreference.noticeEnabled,
            });
            setPreference(saved);
        } catch (error) {
            console.error(error);
            alert("알림 설정 저장에 실패했습니다.");
            setPreference(preference);
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return <div className="text-gray-500">불러오는 중...</div>;
    }

    if (!preference) {
        return <div className="text-gray-500">알림 설정을 불러올 수 없습니다.</div>;
    }

    const items = [
        {
            key: "chatEnabled" as const,
            title: "채팅 알림",
            desc: "아이돌 채팅/메시지 관련 알림을 받습니다.",
        },
        {
            key: "voteEnabled" as const,
            title: "투표 알림",
            desc: "투표 시작, 종료, 결과 관련 알림을 받습니다.",
        },
        {
            key: "ticketEnabled" as const,
            title: "티켓 알림",
            desc: "티켓/굿즈 오픈 및 구매 관련 알림을 받습니다.",
        },
        {
            key: "noticeEnabled" as const,
            title: "공지 알림",
            desc: "공식 게시글, 공지 관련 알림을 받습니다.",
        },
    ];

    return (
        <div className="space-y-4">
            <div className="mb-2">
                <h2 className="text-xl font-bold text-gray-800">알림 설정</h2>
                <p className="mt-1 text-sm text-gray-500">
                    받고 싶은 알림만 선택해서 설정할 수 있습니다.
                </p>
            </div>

            {items.map((item) => (
                <div
                    key={item.key}
                    className="flex items-center justify-between rounded-2xl border border-gray-100 bg-gray-50 px-5 py-4"
                >
                    <div>
                        <div className="text-sm font-semibold text-gray-800">{item.title}</div>
                        <div className="mt-1 text-sm text-gray-500">{item.desc}</div>
                    </div>

                    <button
                        onClick={() => handleToggle(item.key)}
                        disabled={saving}
                        className={`relative inline-flex h-7 w-12 items-center rounded-full transition ${
                            preference[item.key] ? "bg-idol" : "bg-gray-300"
                        } ${saving ? "opacity-60 cursor-not-allowed" : ""}`}
                    >
        <span
            className={`inline-block h-5 w-5 transform rounded-full bg-white transition ${
                preference[item.key] ? "translate-x-6" : "translate-x-1"
            }`}
        />
                    </button>
                </div>
            ))}
        </div>
    );
};

export default NotificationPreferenceTab;