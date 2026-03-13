import React, {useEffect, useState} from "react";
import {useAuthStore} from "../../../stores/authStore";
import {
    getNotificationPreference,
    updateNotificationPreference,
    type NotificationPreferenceResponse,
} from "../../../api/notificationApi";
import { showErrorToast } from "../../../utils/alert";

const NotificationPreferenceTab: React.FC = () => {
    const {accessToken} = useAuthStore();

    const [preference, setPreference] = useState<NotificationPreferenceResponse | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    const loadPreference = async () => {
        if (!accessToken) return;

        try {
            setLoading(true);
            const data = await getNotificationPreference();
            setPreference(data);
        } catch (error) {
            showErrorToast("알림 설정을 불러오는 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadPreference();
    }, [accessToken]);

    const handleToggle = async (
        key: "allEnabled" | "chatEnabled" | "voteEnabled" | "ticketEnabled" | "boardEnabled"
    ) => {
        if (!accessToken || !preference || saving) return;

        let nextPreference: NotificationPreferenceResponse = {...preference};

        if (key === "allEnabled") {
            const nextAllEnabled = !preference.allEnabled;

            nextPreference = {
                ...preference,
                allEnabled: nextAllEnabled,
                chatEnabled: nextAllEnabled,
                voteEnabled: nextAllEnabled,
                ticketEnabled: nextAllEnabled,
                boardEnabled: nextAllEnabled,
            };
        } else {
            const nextValue = !preference[key as keyof NotificationPreferenceResponse];

            nextPreference = {
                ...preference,
                [key]: nextValue,
            };

            const hasAnyEnabled =
                nextPreference.chatEnabled ||
                nextPreference.voteEnabled ||
                nextPreference.ticketEnabled ||
                nextPreference.boardEnabled;

            nextPreference.allEnabled = hasAnyEnabled;
        }

        setPreference(nextPreference);

        try {
            setSaving(true);
            const saved = await updateNotificationPreference({
                allEnabled: nextPreference.allEnabled,
                chatEnabled: nextPreference.chatEnabled,
                voteEnabled: nextPreference.voteEnabled,
                ticketEnabled: nextPreference.ticketEnabled,
                boardEnabled: nextPreference.boardEnabled,
            });
            setPreference(saved);
        } catch (error) {
            showErrorToast("알림 설정 저장에 실패했습니다.");
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
            key: "allEnabled" as const,
            title: "전체 알림",
            desc: "모든 알림을 한 번에 켜거나 끕니다.",
        },
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
            key: "boardEnabled" as const,
            title: "게시판 알림",
            desc: "그룹 공식글, 아이돌 공식글, 공지사항 알림을 받습니다.",
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
                        disabled={saving || (item.key !== "allEnabled" && !preference.allEnabled)}
                        className={`relative inline-flex h-7 w-12 items-center rounded-full transition ${
                            preference[item.key as keyof NotificationPreferenceResponse] ? "bg-idol" : "bg-gray-300"
                        } ${
                            saving || (item.key !== "allEnabled" && !preference.allEnabled)
                                ? "opacity-60 cursor-not-allowed"
                                : ""
                        }`}
                    >
                        <span
                            className={`inline-block h-5 w-5 transform rounded-full bg-white transition ${
                                preference[item.key as keyof NotificationPreferenceResponse] ? "translate-x-6" : "translate-x-1"
                            }`}
                        />
                    </button>
                </div>
            ))}
        </div>
    );
};

export default NotificationPreferenceTab;