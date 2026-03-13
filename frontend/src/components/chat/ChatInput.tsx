import React, { useRef } from "react";

interface ChatInputProps {
    onSendMessage: (msg: string) => void;
    onFileUpload: (file: File) => void;
    onTyping: () => void;
    isDisabled: boolean;
    isUploading: boolean;
    isRestricted: boolean;
    isOtherIdolRoom: boolean;
    isSending: boolean;
}

const ChatInput: React.FC<ChatInputProps> = ({
    onSendMessage,
    onFileUpload,
    onTyping,
    isDisabled,
    isUploading,
    isRestricted,
    isOtherIdolRoom,
    isSending
}) => {
    const [newMessage, setNewMessage] = React.useState("");
    const fileInputRef = useRef<HTMLInputElement | null>(null);

    const handleSend = () => {
        if (!newMessage.trim() || isDisabled) return;
        onSendMessage(newMessage);
        setNewMessage("");
    };

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.nativeEvent.isComposing) return;
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            onFileUpload(file);
            if (fileInputRef.current) fileInputRef.current.value = "";
        }
    };

    const placeholder = isRestricted 
        ? "활동이 제한되어 메시지를 보낼 수 없습니다." 
        : isOtherIdolRoom 
            ? "자신의 채팅방에서만 메시지를 보낼 수 있습니다." 
            : isSending 
                ? "도배 방지: 3초 후 입력 가능합니다." 
                : "메시지 전송";

    return (
        <div className="p-3 pb-5 sm:p-4 bg-white border-t border-gray-200 shrink-0 relative">
            {isUploading && (
                <div className="absolute -top-10 left-0 w-full flex justify-center">
                    <div className="bg-black/60 text-white text-xs px-4 py-1.5 rounded-full shadow-lg flex items-center">
                        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                        미디어 업로드 중...
                    </div>
                </div>
            )}
            <div className="flex justify-center items-center h-full max-w-full m-0 p-0">
                <div className={`flex items-center rounded-full border p-1 px-3 w-full transition-all shadow-inner ${isDisabled ? 'bg-gray-200 border-gray-300 opacity-70 cursor-not-allowed' : 'bg-gray-50 border-gray-200 focus-within:ring-2 focus-within:ring-[var(--color-idol-bg)] focus-within:border-[var(--color-idol-point)]'}`}>
                    <button
                        className={`p-2 transition-colors active:scale-95 flex-shrink-0 ${isDisabled ? 'text-gray-400 cursor-not-allowed' : 'text-gray-500 hover:text-[var(--color-idol-dark)]'}`}
                        disabled={isDisabled}
                        onClick={() => fileInputRef.current?.click()}
                    >
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
                        </svg>
                    </button>
                    <input
                        type="file"
                        ref={fileInputRef}
                        style={{ display: "none" }}
                        accept="image/*,video/*"
                        onChange={handleFileChange}
                        disabled={isDisabled}
                    />
                    <input
                        type="text"
                        placeholder={placeholder}
                        value={newMessage}
                        onChange={(e) => {
                            setNewMessage(e.target.value);
                            onTyping();
                        }}
                        onKeyDown={handleKeyDown}
                        disabled={isDisabled}
                        className={`flex-1 bg-transparent border-none focus:ring-0 px-3 py-3 text-[15px] outline-none ${isDisabled ? 'text-gray-500 cursor-not-allowed' : 'text-gray-800'}`}
                    />
                    <button
                        onClick={handleSend}
                        disabled={!newMessage.trim() || isDisabled}
                        className={`ml-2 px-4 py-2 font-medium rounded-full flex items-center justify-center shadow-md transition-all sm:active:scale-95 min-w-14 ${isDisabled ? 'bg-gray-400 text-gray-200 cursor-not-allowed' : 'bg-gradient-to-r from-[var(--color-idol)] to-[var(--color-idol-dark)] hover:from-[var(--color-idol-dark)] hover:to-[var(--color-idol-dark)] text-white disabled:opacity-50'}`}
                    >
                        전송
                    </button>
                </div>
            </div>
        </div>
    );
};

export default React.memo(ChatInput);
