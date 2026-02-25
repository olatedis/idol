import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";
import { api } from "../../api/axios";
import { Client } from "@stomp/stompjs";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8000";
const WS_URL = API_BASE_URL.replace("http", "ws") + "/ws-chat";

// 타입 정의
interface ChatRoom {
    idolId: number;
    profileImage: string;
    stageName: string;
    lastMessage: string | null;
    lastMessageAt: string | null;
    unreadCount: number;
    isSubscribed: boolean;
}

interface ChatMessage {
    id?: string;
    idolId: number;
    senderId: number;
    senderRole: string;
    senderNickname: string;
    content: string;
    type: string;
    createdAt?: string;
}

const ChatPage: React.FC = () => {
    const { groupId } = useParams<{ groupId?: string }>();
    const { user } = useAuthStore();

    // UI 상태 관리
    const [selectedIdolId, setSelectedIdolId] = useState<number | null>(null);
    const [chatRooms, setChatRooms] = useState<ChatRoom[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    // 채팅 상태 관리 (Phase 2)
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [newMessage, setNewMessage] = useState("");
    const [isIdolOnline, setIsIdolOnline] = useState(false);
    const stompClientRef = React.useRef<Client | null>(null);
    const messagesEndRef = React.useRef<HTMLDivElement | null>(null);

    // 채팅방 목록(그룹 내 멤버 리스트) 불러오기
    useEffect(() => {
        const fetchChatRooms = async () => {
            if (!groupId || !user) return;

            try {
                setIsLoading(true);
                // 1. 그룹 내 아이돌 리스트 호출 (user-service)
                const idolsRes = await api.get(`/groups/${groupId}/idols`);
                const idols = idolsRes.data;

                // 2. 내 채팅방 상태 및 미리보기 호출 (chat-service)
                const roomsRes = await api.get(`/chat/rooms`);
                const roomInfo = roomsRes.data; // [{ idolId, unreadCount, lastMessage, lastMessageAt }, ...]

                // 병합
                const mergedRooms: ChatRoom[] = idols.map((idol: any) => {
                    const roomData = roomInfo.find((r: any) => r.idolId === idol.idolId) || {};
                    return {
                        idolId: idol.idolId,
                        profileImage: idol.profileImage || "",
                        stageName: idol.stageName || "Unknown",
                        lastMessage: roomData.lastMessage || null,
                        lastMessageAt: roomData.lastMessageAt || null,
                        unreadCount: roomData.unreadCount || 0,
                        isSubscribed: roomData.subscribed !== undefined ? roomData.subscribed : (roomData.isSubscribed || false)
                    };
                });

                // 최신 메시지 순으로 정렬 (메시지가 없는 방은 뒤로)
                mergedRooms.sort((a, b) => {
                    if (!a.lastMessageAt && !b.lastMessageAt) return 0;
                    if (!a.lastMessageAt) return 1;
                    if (!b.lastMessageAt) return -1;
                    return new Date(b.lastMessageAt).getTime() - new Date(a.lastMessageAt).getTime();
                });

                setChatRooms(mergedRooms);
            } catch (error) {
                console.error("채팅방 목록 로딩 실패:", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchChatRooms();
    }, [groupId, user]);

    // 스크롤 하단 이동 보조 함수
    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    // Phase 2: 채팅방(아이돌) 선택 시 STOMP 연결 및 기존 내역 로드
    useEffect(() => {
        if (!selectedIdolId || !user) {
            // 방을 나갈 때 연결 종료
            if (stompClientRef.current && stompClientRef.current.active) {
                stompClientRef.current.deactivate();
            }
            return;
        }

        // 1. 기존 내역 및 온라인 상태 페치
        const fetchHistoryAndStatus = async () => {
            try {
                // 병렬 요청
                const [histRes, statusRes] = await Promise.all([
                    api.get(`/chat/history/${selectedIdolId}`),
                    api.get(`/chat/status/${selectedIdolId}`)
                ]);

                // 메시지 이력 세팅
                const history = Array.isArray(histRes.data) ? histRes.data.reverse() : [];
                setMessages(history);
                setTimeout(scrollToBottom, 100);

                // 온라인 상태 세팅
                setIsIdolOnline(statusRes.data.online === true);
            } catch (err) {
                console.error("채팅 내역/상태 불러오기 실패:", err);
            }
        };

        fetchHistoryAndStatus();

        // 2. STOMP 연결 설정
        const client = new Client({
            brokerURL: WS_URL,
            connectHeaders: {
                Authorization: `Bearer ${useAuthStore.getState().accessToken}`
            },
            reconnectDelay: 5000,
            onConnect: () => {
                console.log("STOMP Connected to idol room", selectedIdolId);

                // 해당 아이돌 구독 채널
                client.subscribe(`/sub/idol/${selectedIdolId}`, (message) => {
                    const parsed: ChatMessage = JSON.parse(message.body);
                    setMessages((prev) => [...prev, parsed]);
                    setTimeout(scrollToBottom, 100);
                });

                // (옵션) 상대방 타이핑 상태 구독 채널
                client.subscribe(`/sub/idol/${selectedIdolId}/typing`, (message) => {
                    // 타이핑 상태 처리 로직 (프론트엔드 UI용)
                    console.log("typing...", message.body);
                });
            },
            onStompError: (frame) => {
                console.error("Broker reported error: " + frame.headers["message"]);
                console.error("Additional details: " + frame.body);
            },
            onWebSocketError: (event) => {
                console.error("WebSocket Error:", event);
            }
        });

        client.activate();
        stompClientRef.current = client;

        // 클린업: 언마운트 혹은 다른 방 선택 시 연결 해제
        return () => {
            if (client.active) {
                client.deactivate();
            }
        };
    }, [selectedIdolId, user]);

    // 메시지 전송 로직
    const handleSendMessage = () => {
        if (!newMessage.trim() || !stompClientRef.current?.active || !selectedIdolId || !user) return;

        const payload = {
            idolId: selectedIdolId,
            content: newMessage,
            type: "TEXT"
        };

        try {
            stompClientRef.current.publish({
                destination: "/pub/chat/send",
                body: JSON.stringify(payload)
            });

            // 내 메시지 즉각 반영 (Optimistic UI - 버블 스타일은 내 메시지가 나에게 에코되지 않음)
            setMessages(prev => [...prev, {
                idolId: selectedIdolId,
                senderId: user.userId,
                senderRole: user.role,
                senderNickname: user.nickname,
                content: newMessage,
                type: "TEXT",
                createdAt: new Date().toISOString()
            }]);

            setNewMessage("");
            setTimeout(scrollToBottom, 100);
        } catch (err) {
            console.error("메시지 전송 에러", err);
        }
    };

    // 엔터키 전송 지원
    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    // Phase 1: 방 목록 렌더링
    const renderRoomList = () => (
        <div className="bg-white/80 backdrop-blur-xl border border-white/50 rounded-2xl shadow-xl w-full max-w-2xl mx-auto overflow-hidden">
            <div className="p-6 bg-gradient-to-r from-purple-100 to-indigo-50 border-b border-purple-100">
                <h2 className="text-2xl font-black text-gray-800">메시지</h2>
                <p className="text-sm text-gray-500 mt-1">그룹 멤버들과 실시간 소통을 즐겨보세요</p>
            </div>

            <div className="divide-y divide-gray-100 max-h-[600px] overflow-y-auto min-h-[300px]">
                {isLoading ? (
                    <div className="p-10 text-center text-gray-400">명단을 불러오는 중...</div>
                ) : chatRooms.length > 0 ? (
                    chatRooms.map(room => (
                        <div
                            key={room.idolId}
                            onClick={() => {
                                if (room.isSubscribed) {
                                    setSelectedIdolId(room.idolId);
                                } else {
                                    alert(`'${room.stageName}' 님과의 1:1 채팅은 구독 플랜 열람권이 필요합니다.\n먼저 구독을 진행해 주세요!`);
                                }
                            }}
                            className={`flex items-center p-5 cursor-pointer transition-colors relative ${room.isSubscribed ? 'hover:bg-gray-50' : 'hover:bg-gray-50/50 opacity-90'}`}
                        >
                            {!room.isSubscribed && (
                                <div className="absolute right-4 top-4 bg-gray-100 text-gray-400 p-1.5 rounded-full shadow-sm z-10" title="구독 필요">
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                                </div>
                            )}
                            <div className={`w-14 h-14 rounded-full bg-gray-200 overflow-hidden shrink-0 border-2 transition-all ${room.isSubscribed ? 'border-transparent hover:border-purple-300' : 'border-gray-200 grayscale'}`}>
                                {room.profileImage ? (
                                    <img src={room.profileImage} alt={room.stageName} className="w-full h-full object-cover" />
                                ) : (
                                    <div className={`w-full h-full flex items-center justify-center font-bold text-xl ${room.isSubscribed ? 'bg-purple-100 text-purple-400' : 'bg-gray-100 text-gray-400'}`}>
                                        {room.stageName?.charAt(0) || "?"}
                                    </div>
                                )}
                            </div>
                            <div className="ml-4 flex-1">
                                <div className="flex justify-between items-center mb-1 pr-6">
                                    <span className={`font-bold text-lg ${room.isSubscribed ? 'text-gray-800' : 'text-gray-500'}`}>{room.stageName}</span>
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
                                    <span className="bg-red-500 text-white text-xs font-bold px-2 py-1 rounded-full shadow-sm">
                                        {room.unreadCount > 99 ? '99+' : room.unreadCount}
                                    </span>
                                </div>
                            )}
                        </div>
                    ))
                ) : (
                    <div className="p-10 text-center text-gray-400 flex flex-col items-center justify-center space-y-3">
                        <svg className="w-12 h-12 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path></svg>
                        <p>소속된 채팅방이 없습니다.</p>
                    </div>
                )}
            </div>
        </div>
    );

    // Phase 2: 채팅방 레이아웃 
    const renderChatRoom = () => (
        <div className="bg-gray-50/50 backdrop-blur-xl border border-white/50 rounded-2xl shadow-2xl w-full max-w-3xl mx-auto flex flex-col h-[700px] overflow-hidden relative">
            {/* 상단 헤더 영역 */}
            <div className="px-5 py-4 bg-white/90 border-b border-gray-100 flex items-center shadow-sm z-10 w-full shrink-0">
                <button
                    onClick={() => setSelectedIdolId(null)}
                    className="p-2 -ml-2 text-gray-500 hover:text-gray-800 hover:bg-gray-100 rounded-full transition-colors mr-3 active:scale-95"
                >
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7"></path></svg>
                </button>
                <div className="flex-1 flex items-center">
                    <div className="w-10 h-10 rounded-full bg-purple-100 flex items-center justify-center font-bold text-purple-500 mr-3 border border-purple-200">
                        {chatRooms.find(r => r.idolId === selectedIdolId)?.stageName?.substring(0, 1) || "I"}
                    </div>
                    <div>
                        <h3 className="font-bold text-gray-800">{chatRooms.find(r => r.idolId === selectedIdolId)?.stageName || "멤버"}</h3>
                        <p className={`text-xs flex items-center mt-0.5 ${isIdolOnline ? 'text-green-500' : 'text-gray-400'}`}>
                            {isIdolOnline ? (
                                <>
                                    <span className="w-1.5 h-1.5 rounded-full bg-green-500 mr-1.5 inline-block animate-pulse"></span>
                                    온라인
                                </>
                            ) : (
                                <>
                                    <span className="w-1.5 h-1.5 rounded-full bg-gray-400 mr-1.5 inline-block"></span>
                                    오프라인
                                </>
                            )}
                        </p>
                    </div>
                </div>
            </div>

            {/* 채팅 내역 영역 */}
            <div className="flex-1 overflow-y-auto p-5 space-y-4 bg-[#b2c7d9] flex flex-col">
                {/* 시스템 알림 라벨 */}
                <div className="flex justify-center my-2 shrink-0">
                    <span className="bg-black/20 text-white text-xs px-4 py-1.5 rounded-full shadow-sm">
                        채팅방에 입장했습니다
                    </span>
                </div>

                {/* 메시지 렌더링 */}
                {messages.map((msg, idx) => {
                    const isMine = msg.senderId === user?.userId;
                    return (
                        <div key={msg.id || idx} className={`flex ${isMine ? 'justify-end' : 'justify-start'} shrink-0 transform transition-all`}>
                            {!isMine && (
                                <div className="w-8 h-8 rounded-full bg-gray-200 mr-2 overflow-hidden shrink-0 border border-gray-300">
                                    {chatRooms.find(r => r.idolId === selectedIdolId)?.profileImage ? (
                                        <img src={chatRooms.find(r => r.idolId === selectedIdolId)!.profileImage} alt="profile" className="w-full h-full object-cover" />
                                    ) : (
                                        <div className="w-full h-full flex items-center justify-center bg-purple-100 text-purple-400 font-bold text-xs">
                                            {chatRooms.find(r => r.idolId === selectedIdolId)?.stageName?.substring(0, 1) || "I"}
                                        </div>
                                    )}
                                </div>
                            )}
                            <div className={`max-w-[70%] px-4 py-2.5 rounded-2xl shadow-sm border ${isMine
                                ? 'bg-[#FFEB33] text-gray-800 rounded-tr-sm border-yellow-200'
                                : 'bg-white text-gray-800 rounded-tl-sm border-gray-100'
                                }`}>
                                {!isMine && <div className="text-xs text-gray-500 mb-1 font-semibold">{msg.senderNickname}</div>}
                                <div>{msg.content}</div>
                                {msg.createdAt && (
                                    <div className={`text-[10px] mt-1 text-gray-400 ${isMine ? 'text-right' : 'text-left'}`}>
                                        {new Date(msg.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                                    </div>
                                )}
                            </div>
                        </div>
                    );
                })}
                <div ref={messagesEndRef} />
            </div>

            {/* 입력창 바텀바 */}
            <div className="p-3 pb-5 sm:p-4 bg-white border-t border-gray-200 shrink-0">
                <div className="flex justify-center items-center h-full max-w-full m-0 p-0">
                    <div className="flex items-center bg-gray-100 rounded-full border border-gray-200 p-1 px-3 w-full focus-within:ring-2 focus-within:ring-purple-200 focus-within:border-purple-300 transition-all shadow-inner">
                        <button className="p-2 text-gray-500 hover:text-purple-600 transition-colors active:scale-95">
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path></svg>
                        </button>
                        <input
                            type="text"
                            placeholder="메시지 전송"
                            value={newMessage}
                            onChange={(e) => setNewMessage(e.target.value)}
                            onKeyDown={handleKeyDown}
                            className="flex-1 bg-transparent border-none focus:ring-0 px-3 py-3 text-gray-800 text-[15px] outline-none"
                        />
                        <button
                            onClick={handleSendMessage}
                            disabled={!newMessage.trim()}
                            className="ml-2 px-4 py-2 bg-gradient-to-r from-purple-500 to-indigo-500 hover:from-purple-600 hover:to-indigo-600 text-white font-medium rounded-full flex items-center justify-center shadow-md transition-all sm:active:scale-95 disabled:opacity-50 min-w-14"
                        >
                            전송
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );

    return (
        <div className="pt-2 pb-12 z-10 relative">
            {/* 배경 블러 효과 */}
            <div className="absolute top-20 right-10 w-96 h-96 bg-purple-300 rounded-full mix-blend-multiply filter blur-3xl opacity-20 pointer-events-none"></div>
            <div className="absolute -bottom-10 left-20 w-80 h-80 bg-pink-300 rounded-full mix-blend-multiply filter blur-3xl opacity-20 pointer-events-none"></div>

            {/* View Switching */}
            {selectedIdolId === null ? renderRoomList() : renderChatRoom()}
        </div>
    );
};

export default ChatPage;
