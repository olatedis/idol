import type { NotificationListResponse } from "../types/notification";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const getNotificationList = async (
    accessToken: string,
    size = 20,
    cursor?: string
): Promise<NotificationListResponse> => {
    const query = new URLSearchParams();
    query.set("size", String(size));
    if (cursor) query.set("cursor", cursor);

    const response = await fetch(`${API_BASE_URL}/notify/notifications?${query.toString()}`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        throw new Error("알림 목록 조회 실패");
    }

    return response.json();
};