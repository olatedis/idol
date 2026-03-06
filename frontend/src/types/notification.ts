export type NotificationItem = {
    notificationId: number;
    eventId: string;
    type: string;
    targetType: string;
    targetId: string;
    redirectUrl: string;
    occurredAt: string;
    args?: Record<string, string> | null;
    isRead?: boolean;
};

export type NotificationListResponse = {
    items: NotificationItem[];
    nextCursor?: string | null;
    hasNext?: boolean;
};

export type IdolMessageStackPayload = {
    idolId: number;
    unreadCount: number;
    lastOccurredAt: string | null;
};