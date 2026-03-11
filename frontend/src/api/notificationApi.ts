import type {NotificationListResponse} from "../types/notification";

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

export const readAllNotifications = async (accessToken: string): Promise<{ updatedCount: number }> => {
    const response = await fetch(`${API_BASE_URL}/notify/notifications/read-all`, {
        method: "POST",
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        throw new Error("전체 읽음 처리 실패");
    }

    return response.json();
};

export const readOneNotification = async (
    accessToken: string,
    notificationId: number
): Promise<{ updatedCount: number }> => {
    const response = await fetch(`${API_BASE_URL}/notify/notifications/${notificationId}/read`, {
        method: "POST",
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        throw new Error("단건 읽음 처리 실패");
    }

    return response.json();
};

export type NotificationPreferenceResponse = {
    userId: number;
    allEnabled: boolean;
    chatEnabled: boolean;
    voteEnabled: boolean;
    ticketEnabled: boolean;
    boardEnabled: boolean;
};

export type UpdateNotificationPreferenceRequest = {
    allEnabled: boolean;
    chatEnabled: boolean;
    voteEnabled: boolean;
    ticketEnabled: boolean;
    boardEnabled: boolean;
};

export const getNotificationPreference = async (
    accessToken: string
): Promise<NotificationPreferenceResponse> => {
    const response = await fetch(`${API_BASE_URL}/notify/preferences`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${accessToken}`,
        },
    });

    if (!response.ok) {
        throw new Error("알림 설정 조회 실패");
    }

    return response.json();
};

export const updateNotificationPreference = async (
    accessToken: string,
    body: UpdateNotificationPreferenceRequest
): Promise<NotificationPreferenceResponse> => {
    const response = await fetch(`${API_BASE_URL}/notify/preferences`, {
        method: "PUT",
        headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
    });

    if (!response.ok) {
        throw new Error("알림 설정 수정 실패");
    }

    return response.json();
};
