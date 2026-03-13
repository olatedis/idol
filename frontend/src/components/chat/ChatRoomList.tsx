import React from "react";
import type { ChatRoom } from "../../types/chat";

interface ChatRoomListProps {
    rooms: ChatRoom[];
    isLoading: boolean;
    onSelectRoom: (idolId: number, stageName: string, isSubscribed: boolean) => void;
    currentUserIdolId: number | null;
    userRole: string | undefined;
}

const ChatRoomList: React.FC<ChatRoomListProps> = ({
    rooms,
    isLoading,
    onSelectRoom,
    currentUserIdolId,
    userRole
}) => {
    return (
        <div className="bg-white/90 backdrop-blur-xl border border-[var(--color-idol-bg)] rounded-2xl shadow-xl w-[calc(100%-1rem)] sm:w-full max-w-2xl mx-auto flex flex-col flex-1 overflow-hidden mt-2 sm:mt-4 mb-2 sm:mb-4">
            <div className="p-4 sm:p-6 bg-gradient-to-r from-[var(--color-idol-bg)] to-white border-b border-[var(--color-idol-bg)] shrink-0">
                <h2 className="text-xl sm:text-2xl font-black text-[var(--color-idol-dark)]">메시지</h2>
                <p className="text-xs sm:text-sm text-gray-500 mt-1">그룹 멤버들과 실시간 소통을 즐겨보세요</p>
            </div>

            <div className="divide-y divide-gray-100 flex-1 overflow-y-auto custom-scrollbar">
                {isLoading ? (
                    <div className="p-10 text-center text-gray-400">명단을 불러오는 중...</div>
                ) : rooms.length > 0 ? (
                    rooms.map(room => {
                        const isMyRoom = userRole === 'IDOL' && currentUserIdolId === room.idolId;
                        const canEnter = userRole === 'IDOL' || room.isSubscribed;

                        return (
                            <div
                                key={room.idolId}
                                onClick={() => onSelectRoom(room.idolId, room.stageName, room.isSubscribed)}
                                className={`flex items-center p-5 cursor-pointer transition-colors relative ${isMyRoom
                                    ? 'bg-[var(--color-idol-bg)] hover:bg-[var(--color-idol-bg)]/80 border-l-4 border-[var(--color-idol-dark)]'
                                    : canEnter
                                        ? 'hover:bg-gray-50 border-l-4 border-transparent'
                                        : 'hover:bg-gray-50/50 opacity-90 border-l-4 border-transparent'
                                    }`}
                            >
                                {!canEnter && (
                                    <div className="absolute right-4 top-4 bg-gray-100 text-gray-400 p-1.5 rounded-full shadow-sm z-10" title="구독 필요">
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                                    </div>
                                )}
                                <div className={`w-14 h-14 rounded-full bg-gray-200 overflow-hidden shrink-0 border-2 transition-all ${canEnter ? 'border-transparent hover:border-[var(--color-idol-point)]' : 'border-gray-200 grayscale'}`}>
                                    {room.profileImage ? (
                                        <img src={room.profileImage} alt={room.stageName} className="w-full h-full object-cover" />
                                    ) : (
                                        <div className={`w-full h-full flex items-center justify-center font-bold text-xl ${canEnter ? 'bg-[var(--color-idol-bg)] text-[var(--color-idol-dark)]' : 'bg-gray-100 text-gray-400'}`}>
                                            {room.stageName?.charAt(0) || "?"}
                                        </div>
                                    )}
                                </div>
                                <div className="ml-4 flex-1">
                                    <div className="flex justify-between items-center mb-1 pr-6 relative">
                                        <div className="flex items-center">
                                            <span className={`font-bold text-lg ${canEnter ? 'text-gray-800' : 'text-gray-500'}`}>{room.stageName}</span>
                                            {isMyRoom && (
                                                <span className="ml-2 text-[10px] font-bold text-white bg-[var(--color-idol-point)] px-2 py-0.5 rounded-full shadow-sm">
                                                    내 채팅방
                                                </span>
                                            )}
                                            {room.isOnline && (
                                                <span className="ml-2 w-2 h-2 rounded-full bg-green-500 animate-pulse shadow-sm shadow-green-200"></span>
                                            )}
                                        </div>
                                        {room.lastMessageAt && (
                                            <span className="text-xs text-gray-400">
                                                {new Date(room.lastMessageAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                                            </span>
                                        )}
                                    </div>
                                    <div className="text-sm text-gray-500 truncate mt-0.5 max-w-[90%]">
                                        {room.lastMessage || "새로운 메시지를 기다리고 있어요!"}
                                    </div>
                                </div>
                                {room.unreadCount > 0 && (
                                    <div className="ml-3 shrink-0 flex items-center justify-center">
                                        <span className="bg-[var(--color-idol)] text-white text-xs font-bold px-2 py-1 rounded-full shadow-sm">
                                            {room.unreadCount > 99 ? '99+' : room.unreadCount}
                                        </span>
                                    </div>
                                )}
                            </div>
                        );
                    })
                ) : (
                    <div className="p-10 text-center text-gray-400 flex flex-col items-center justify-center space-y-3">
                        <svg className="w-12 h-12 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path></svg>
                        <p>소속된 채팅방이 없습니다.</p>
                    </div>
                )}
            </div>
        </div>
    );
};

export default React.memo(ChatRoomList);
