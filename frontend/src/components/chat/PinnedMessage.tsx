import React from "react";
import type { ChatMessage } from "../../types/chat";

interface PinnedMessageProps {
    message: ChatMessage | null;
}

const PinnedMessage: React.FC<PinnedMessageProps> = ({ message }) => {
    if (!message) return null;

    return (
        <div className="bg-[var(--color-idol-bg)] border-b border-[var(--color-idol-point)]/20 px-5 py-3 flex items-start space-x-3 shadow-sm shrink-0 z-10 transition-all">
            <div className="mt-0.5 text-[var(--color-idol-point)] shrink-0 bg-white p-1 rounded-full shadow-sm">
                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M5 4a2 2 0 012-2h6a2 2 0 012 2v14l-5-2.5L5 18V4z" />
                </svg>
            </div>
            <div className="flex-1 min-w-0">
                <div className="text-[10px] font-black tracking-wider text-[var(--color-idol-dark)] mb-0.5 uppercase">공지사항</div>
                <p className="text-sm text-gray-800 break-words line-clamp-2 leading-snug">{message.content}</p>
            </div>
        </div>
    );
};

export default React.memo(PinnedMessage);
