import {api} from "./axios";
import type {NotificationListResponse} from "../types/notification";

export const getNotificationList = async (
    size = 20,
    cursor?: string
): Promise<NotificationListResponse> => {
    const params = new URLSearchParams();
    params.set("size", String(size));
    if (cursor) params.set("cursor", cursor);

    const response = await api.get(`/notify/notifications?${params.toString()}`);
    return response.data;
};

export const readAllNotifications = async (): Promise<{ updatedCount: number }> => {
    const response = await api.post(`/notify/notifications/read-all`);
    return response.data;
};

export const readOneNotification = async (
    notificationId: number
): Promise<{ updatedCount: number }> => {
    const response = await api.post(`/notify/notifications/${notificationId}/read`);
    return response.data;
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

export const getNotificationPreference = async (): Promise<NotificationPreferenceResponse> => {
    const response = await api.get(`/notify/preferences`);
    return response.data;
};

export const updateNotificationPreference = async (
    body: UpdateNotificationPreferenceRequest
): Promise<NotificationPreferenceResponse> => {
    const response = await api.put(`/notify/preferences`, body);
    return response.data;
};

export const getIdolMessageStacks = async () => {
    const {data} = await api.get("/notify/idol-message-stacks");
    return data;
};

export const resetIdolMessageStack = async (idolId: number) => {
    const {data} = await api.post(`/notify/idol-message-stacks/${idolId}/reset`);
    return data;
};

export const resetAllIdolMessageStacks = async () => {
    const { data } = await api.post("/notify/idol-message-stacks/reset-all");
    return data;
};

export const deleteOneNotification = async (notificationId: number) => {
    const { data } = await api.delete(`/notify/notifications/${notificationId}`);
    return data;
};

export const deleteManyNotifications = async (notificationIds: number[]) => {
    const { data } = await api.post(`/notify/notifications/delete`, {
        notificationIds,
    });
    return data;
};
