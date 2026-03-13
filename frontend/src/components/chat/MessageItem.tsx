import React from "react";
import type { ChatMessage } from "../../types/chat";

interface MessageItemProps {
    message: ChatMessage;
    isMine: boolean;
    profileImage?: string;
    stageName?: string;
    onImageClick: (url: string) => void;
    scrollToBottom: () => void;
}

const MessageItem: React.FC<MessageItemProps> = ({
    message,
    isMine,
    profileImage,
    stageName,
    onImageClick,
    scrollToBottom
}) => {
    return (
        <div className={`flex ${isMine ? 'justify-end' : 'justify-start'} shrink-0 transform transition-all`}>
            {!isMine && (
                <div className="w-8 h-8 rounded-full bg-gray-200 mr-2 overflow-hidden shrink-0 border border-[var(--color-idol)]/20">
                    {profileImage ? (
                        <img src={profileImage} alt="profile" className="w-full h-full object-cover" />
                    ) : (
                        <div className="w-full h-full flex items-center justify-center bg-[var(--color-idol-point)] text-white font-bold text-xs">
                            {stageName?.substring(0, 1) || "I"}
                        </div>
                    )}
                </div>
            )}
            <div className={`max-w-[70%] px-4 py-2.5 rounded-2xl shadow-sm border ${isMine
                ? 'bg-[var(--color-idol)] text-white rounded-tr-sm border-[var(--color-idol-dark)]/20 shadow-[var(--color-idol)]/20'
                : 'bg-white text-gray-800 rounded-tl-sm border-[var(--color-idol-point)]/40 shadow-sm'
                }`}>
                {!isMine && (
                    <div className="text-xs text-gray-500 mb-1 font-semibold">
                        {message.senderRole === "IDOL" ? stageName : message.senderNickname}
                    </div>
                )}

                {message.type === 'IMAGE' ? (
                    <div className="mt-1 mb-1 relative overflow-hidden rounded-xl border border-black/5 bg-white/50 cursor-pointer" onClick={() => onImageClick(message.content)}>
                        <img src={message.thumbnailUrl || message.content} alt="Media" className="max-w-full max-h-64 object-contain transition-transform hover:scale-105" onLoad={scrollToBottom} />
                    </div>
                ) : message.type === 'VIDEO' ? (
                    <div className="mt-1 mb-1 relative overflow-hidden rounded-xl border border-black/5 bg-black/50">
                        <video src={message.content} controls className="max-w-full max-h-64 object-contain" onLoadedMetadata={scrollToBottom} />
                    </div>
                ) : (
                    <div className="whitespace-pre-wrap word-break">{message.content}</div>
                )}

                {message.createdAt && (
                    <div className={`text-[10px] mt-1.5 ${isMine ? 'text-white/70 text-right' : 'text-gray-400 text-left'}`}>
                        {new Date(message.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                    </div>
                )}

                {message.reactions && Object.keys(message.reactions).length > 0 && (
                    <div className={`flex flex-wrap gap-1 mt-1.5 ${isMine ? 'justify-end' : 'justify-start'}`}>
                        {Object.entries(message.reactions).map(([reaction, count]) => (
                            <span key={reaction} className={`inline-flex items-center space-x-1 rounded-full px-1.5 py-0.5 text-[10px] shadow-sm border ${isMine ? 'bg-white/20 border-white/30 text-white' : 'bg-gray-50 border-gray-200 text-gray-700'}`}>
                                <span>{reaction === 'like' ? '❤️' : reaction === 'smile' ? '😄' : reaction === 'thumb' ? '👍' : reaction}</span>
                                <span className="font-bold">{count}</span>
                            </span>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default React.memo(MessageItem);
