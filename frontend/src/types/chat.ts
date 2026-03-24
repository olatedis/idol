export interface ChatRoom {
    idolId: number;
    profileImage: string;
    stageName: string;
    lastMessage: string | null;
    lastMessageAt: string | null;
    unreadCount: number;
    isSubscribed: boolean;
    isOnline: boolean;
}

export interface ChatMessage {
    id?: string;
    idolId: number;
    senderId: number;
    senderRole: string;
    senderNickname: string;
    content: string;
    type: string;
    thumbnailUrl?: string;
    senderProfileImage?: string; // 추가: 발신자 프로필 이미지
    parentId?: string | null;
    createdAt?: string;
    me?: boolean;
    reactions?: Record<string, number>;
    translatedContent?: string; // 번역된 내용
    deleteReason?: string; // 삭제 사유
}
