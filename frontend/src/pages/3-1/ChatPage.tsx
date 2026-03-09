import React, { useState, useEffect, useCallback } from "react";
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
    isOnline: boolean;
}

interface ChatMessage {
    id?: string;
    idolId: number;
    senderId: number;
    senderRole: string;
    senderNickname: string;
    content: string;
    type: string;
    thumbnailUrl?: string; // Added thumbnailUrl
    parentId?: string | null;
    createdAt?: string;
    me?: boolean;
    reactions?: Record<string, number>;
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
    const [myIdolId, setMyIdolId] = useState<number | null>(null);
    const [isIdolTyping, setIsIdolTyping] = useState(false);
    const [isUploading, setIsUploading] = useState(false);

    // 무한 스크롤 상태
    const [hasMore, setHasMore] = useState(true);
    const [isLoadingMore, setIsLoadingMore] = useState(false);
    const [isSending, setIsSending] = useState(false); // 도배 방지 락 상태

    // 공지사항 등 추가 상태
    const [pinnedMessage, setPinnedMessage] = useState<ChatMessage | null>(null);

    // 검색 상태 관리
    const [isSearchOpen, setIsSearchOpen] = useState(false);
    const [searchKeyword, setSearchKeyword] = useState("");
    const [searchResults, setSearchResults] = useState<ChatMessage[]>([]);
    const [isSearching, setIsSearching] = useState(false);

    const typingTimeoutRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);
    const lastTypingTimeRef = React.useRef<number>(0);

    const stompClientRef = React.useRef<Client | null>(null);
    const messagesEndRef = React.useRef<HTMLDivElement | null>(null);
    const scrollContainerRef = React.useRef<HTMLDivElement | null>(null);
    const fileInputRef = React.useRef<HTMLInputElement | null>(null);

    // 아이돌 로그인 시 내 idolId 가져오기
    useEffect(() => {
        if (user?.role === 'IDOL') {
            api.get('/idols/me')
                .then(res => setMyIdolId(res.data.idolId))
                .catch(err => console.error("내 아이돌 정보 가져오기 실패:", err));
        }
    }, [user]);

    // 채팅방 목록(그룹 내 멤버 리스트) 불러오기
    const fetchChatRooms = useCallback(async () => {
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
                    isSubscribed: roomData.subscribed !== undefined ? roomData.subscribed : (roomData.isSubscribed || false),
                    isOnline: roomData.online !== undefined ? roomData.online : (roomData.isOnline || false)
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
    }, [groupId, user]);

    useEffect(() => {
        // 목록 화면일 때만(방에 들어가 있지 않을 때) 목록 새로고침
        if (!selectedIdolId) {
            // 뒤로가기로 null이 된 직후, 서버(Redis)에 읽음 처리가 완벽히 반영될 시간을 주기 위해 지연 페치
            const timer = setTimeout(() => {
                fetchChatRooms();
            }, 800); // 0.8초 지연 (백엔드 Redis 동기화 여유 시간)
            return () => clearTimeout(timer);
        }
    }, [fetchChatRooms, selectedIdolId]);

    // 스크롤 하단 이동 보조 함수
    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    // Phase 2: 채팅방(아이돌) 선택 시 STOMP 연결 및 기존 내역 로드
    useEffect(() => {
        if (!selectedIdolId || !user) {
            if (stompClientRef.current && stompClientRef.current.active) {
                stompClientRef.current.deactivate();
            }
            return;
        }

        let isMounted = true;

        const fetchInitialData = async () => {
            try {
                const [histRes, statusRes, pinRes] = await Promise.all([
                    api.get(`/chat/history/${selectedIdolId}`),
                    api.get(`/chat/status/${selectedIdolId}`),
                    api.get(`/chat/pin/${selectedIdolId}`).catch(() => ({ data: null })),
                    api.post(`/chat/read/${selectedIdolId}`)
                ]);

                if (!isMounted) return;

                const history = Array.isArray(histRes.data) ? histRes.data.reverse() : [];
                setMessages(history);
                setHasMore(history.length === 20); // 기본 size가 20이라고 가정
                setPinnedMessage(pinRes.data || null);

                // 처음 로드 시에만 스크롤을 맨 아래로
                setTimeout(scrollToBottom, 100);

                setIsIdolOnline(statusRes.data.online === true);
                setIsIdolTyping(false);
                if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);

                setChatRooms(prev => prev.map(room =>
                    room.idolId === selectedIdolId ? { ...room, unreadCount: 0 } : room
                ));
            } catch (err) {
                console.error("채팅 내역/상태 불러오기 실패:", err);
            }
        };

        fetchInitialData();

        // 2. STOMP 연결 설정
        const client = new Client({
            brokerURL: WS_URL,
            connectHeaders: {
                Authorization: `Bearer ${useAuthStore.getState().accessToken}`
            },
            reconnectDelay: 5000,
            onConnect: () => {
                console.log("STOMP Connected to idol room", selectedIdolId);

                const handleIncomingMessage = (message: any) => {
                    const parsed: ChatMessage = JSON.parse(message.body);

                    // --- 이벤트 핸들링 시작 ---
                    // 1. 메시지 삭제
                    if (parsed.type === "DELETE") {
                        setMessages((prev) => prev.map(m => m.id === parsed.id ? { ...m, content: "삭제된 메시지입니다.", type: "DELETED" } : m));
                        return; // 메시지 배열에 새로 추가하지 않고 종료
                    }

                    // 2. 공지사항 고정/해제
                    if (parsed.type === "PIN") {
                        setPinnedMessage(parsed);
                        return;
                    }
                    if (parsed.type === "UNPIN") {
                        setPinnedMessage(null);
                        return;
                    }

                    // 3. 메시지 반응(리액션) 업데이트
                    if (parsed.type === "REACTION") {
                        setMessages((prev) => prev.map(m => m.id === parsed.id ? { ...m, reactions: parsed.reactions } : m));
                        return;
                    }
                    // --- 이벤트 핸들링 끝 ---

                    // 내가 보낸 메시지가 에코되어 돌아올 경우 렌더링 중복 방지 (Optimistic UI와 충돌 방지)
                    if (String(parsed.senderId) === String(user.userId)) {
                        return; // 이미 화면에 그렸으므로 무시
                    }

                    // 아이돌 접속 상태 실시간 변경 이벤트 처리
                    if (parsed.type === "STATUS") {
                        setIsIdolOnline(parsed.content === "ON");
                        return;
                    }

                    // 아이돌 타이핑 상태 실시간 변경 이벤트 처리
                    if (parsed.type === "TYPING") {
                        setIsIdolTyping(true);
                        if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
                        typingTimeoutRef.current = setTimeout(() => setIsIdolTyping(false), 3000);
                        return;
                    }

                    setMessages((prev) => {
                        // 혹시 모를 중복 ID 제거 (동일한 메시지 ID가 이미 있으면 추가 안 함)
                        if (parsed.id && prev.some(m => m.id === parsed.id)) return prev;
                        return [...prev, parsed];
                    });
                    setTimeout(scrollToBottom, 100);
                    setIsIdolTyping(false); // 메시지가 도착하면 타이핑 표시 즉시 제거
                };

                // 공지성 및 아이돌 발송 메시지용 공용 채널
                client.subscribe(`/sub/idol/${selectedIdolId}`, handleIncomingMessage);

                // 에러 발생 및 도배 방지 시 서버 브로드캐스팅 수신
                client.subscribe(`/queue/errors/${user.userId}`, (message: any) => {
                    const errorPayload = JSON.parse(message.body);
                    alert(`전송 제한: ${errorPayload.message}`);
                    // Optimistic UI로 올라간 실패한 임시 메시지 즉각 롤백(제거)
                    setMessages(prev => prev.filter(m => m.id === undefined || !String(m.id).startsWith("temp-")));
                });

                // IDOL 권한일 경우 팬들이 나에게 보내는 프라이빗 큐 채널 추가 구독
                if (user.role === "IDOL") {
                    client.subscribe(`/queue/idol/${selectedIdolId}`, handleIncomingMessage);
                }
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
            isMounted = false;
            client.deactivate();
        };
    }, [selectedIdolId, user]);

    // 과거 메시지 불러오기 로직 (무한 스크롤)
    const loadMoreHistory = useCallback(async () => {
        if (isLoadingMore || !hasMore || !selectedIdolId || messages.length === 0) return;

        try {
            setIsLoadingMore(true);

            // 현재 스크롤 유지용 값 저장
            const container = scrollContainerRef.current;
            const previousScrollHeight = container?.scrollHeight || 0;
            const previousScrollTop = container?.scrollTop || 0;

            const oldestMessageId = messages[0].id;
            // API에 lastId 파라미터 전달
            const res = await api.get(`/chat/history/${selectedIdolId}?lastId=${oldestMessageId}`);

            const olderMessages = Array.isArray(res.data) ? res.data.reverse() : [];

            if (olderMessages.length > 0) {
                // 새 메시지 배열의 맨 앞에 옛날 메시지 추가
                setMessages(prev => {
                    const existingIds = new Set(prev.map(m => m.id));
                    const uniqueOlderMessages = olderMessages.filter(m => !existingIds.has(m.id));
                    return [...uniqueOlderMessages, ...prev];
                });
                setHasMore(olderMessages.length === 20); // 다시 페이징 분기검사

                // 스크롤 위치 보정
                // React 상태 업데이트 후 DOM에 렌더링될 때까지 기다림
                requestAnimationFrame(() => {
                    if (container) {
                        const newScrollHeight = container.scrollHeight;
                        // (새롭게 늘어난 전체 높이 - 이전에 로드됐던 전체 높이) 만큼 강제로 스크롤을 내려서
                        // 유저가 보던 시점은 변하지 않게 만들어줌
                        container.scrollTop = previousScrollTop + (newScrollHeight - previousScrollHeight);
                    }
                });
            } else {
                setHasMore(false);
            }
        } catch (err) {
            console.error("과거 메시지 불러오기 실패:", err);
        } finally {
            setIsLoadingMore(false);
        }
    }, [isLoadingMore, hasMore, selectedIdolId, messages]);

    // IntersectionObserver 자동 스크롤 하단 감지 로직은 '이전 대화 더 보기' 수동 버튼 방식으로 교체되었습니다.

    // 메시지 전송 로직
    const handleSendMessage = () => {
        if (!newMessage.trim() || !stompClientRef.current?.active || !selectedIdolId || !user || isSending || isUploading) return;

        // 서버 부담 및 도배를 막기 위해 팬 계정은 3초 연속 전송 비활성화 락
        if (user.role === 'USER') {
            setIsSending(true);
            setTimeout(() => setIsSending(false), 3000);
        }

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

            // 내 메시지 즉각 반영 (Optimistic UI)
            const tempId = `temp-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
            setMessages(prev => [...prev, {
                id: tempId, // 고유 임시 ID 부여 (React List Key 에러 방지)
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

    // 미디어 파일 업로드 핸들러
    const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file || !stompClientRef.current?.active || !selectedIdolId || !user || isSending || isUploading) return;

        // 서버 부담 및 도배를 막기 위해 팬 계정은 3초 연속 업로드 비활성화 락
        if (user.role === 'USER') {
            setIsSending(true);
            setTimeout(() => setIsSending(false), 3000);
        }

        try {
            setIsUploading(true);
            const formData = new FormData();
            formData.append("file", file);

            const uploadRes = await api.post("/chat/upload", formData, {
                headers: { "Content-Type": "multipart/form-data" }
            });

            const uploadedData = uploadRes.data; // { url, thumbnailUrl, type }

            const payload = {
                idolId: selectedIdolId,
                content: uploadedData.url,
                thumbnailUrl: uploadedData.thumbnailUrl,
                type: uploadedData.type === "VIDEO" ? "VIDEO" : "IMAGE"
            };

            stompClientRef.current.publish({
                destination: "/pub/chat/send",
                body: JSON.stringify(payload)
            });

            // Optimistic UI 적용
            const tempId = `temp-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
            setMessages(prev => [...prev, {
                id: tempId,
                idolId: selectedIdolId,
                senderId: user.userId,
                senderRole: user.role,
                senderNickname: user.nickname,
                content: uploadedData.url,
                thumbnailUrl: uploadedData.thumbnailUrl,
                type: payload.type,
                createdAt: new Date().toISOString()
            }]);

            setTimeout(scrollToBottom, 500); // 이미지가 로드될 시간을 고려해 여유있게
        } catch (err) {
            console.error("파일 업로드 실패:", err);
            alert("파일 업로드에 실패했습니다.");
        } finally {
            setIsUploading(false);
            if (fileInputRef.current) fileInputRef.current.value = "";
        }
    };

    // 아이돌 타이핑 신호 전송 로직
    const handleTyping = () => {
        if (!stompClientRef.current?.active || !selectedIdolId || user?.role !== 'IDOL') return;

        const now = Date.now();
        if (now - lastTypingTimeRef.current > 2000) {
            stompClientRef.current.publish({
                destination: "/pub/chat/typing",
                body: JSON.stringify({ idolId: selectedIdolId })
            });
            lastTypingTimeRef.current = now;
        }
    };

    // 채팅 검색 API 연동
    const handleSearch = async (e?: React.FormEvent) => {
        if (e) e.preventDefault();
        if (!searchKeyword.trim() || !selectedIdolId || !user) return;

        try {
            setIsSearching(true);
            const res = await api.get(`/search/chat?idolId=${selectedIdolId}&keyword=${encodeURIComponent(searchKeyword)}&page=0&size=50`);
            setSearchResults(res.data.content || []);
        } catch (err) {
            console.error("검색 중 오류 발생:", err);
            alert("채팅 내역 검색에 실패했습니다.");
        } finally {
            setIsSearching(false);
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
        <div className="bg-white/90 backdrop-blur-xl border border-[var(--color-idol-bg)] rounded-2xl shadow-xl w-[calc(100%-1rem)] sm:w-full max-w-2xl mx-auto flex flex-col flex-1 overflow-hidden mt-2 sm:mt-4 mb-2 sm:mb-4">
            <div className="p-4 sm:p-6 bg-gradient-to-r from-[var(--color-idol-bg)] to-white border-b border-[var(--color-idol-bg)] shrink-0">
                <h2 className="text-xl sm:text-2xl font-black text-[var(--color-idol-dark)]">메시지</h2>
                <p className="text-xs sm:text-sm text-gray-500 mt-1">그룹 멤버들과 실시간 소통을 즐겨보세요</p>
            </div>

            <div className="divide-y divide-gray-100 flex-1 overflow-y-auto custom-scrollbar">
                {isLoading ? (
                    <div className="p-10 text-center text-gray-400">명단을 불러오는 중...</div>
                ) : chatRooms.length > 0 ? (
                    chatRooms.map(room => {
                        const isMyRoom = user?.role === 'IDOL' && myIdolId === room.idolId;
                        return (
                            <div
                                key={room.idolId}
                                onClick={() => {
                                    // IDOL 권한이거나 구독한 방이면 자유롭게 입장
                                    if (user?.role === 'IDOL' || room.isSubscribed) {
                                        setSelectedIdolId(room.idolId);
                                    } else {
                                        alert(`'${room.stageName}' 님과의 1:1 채팅은 구독 플랜 열람권이 필요합니다.\n먼저 구독을 진행해 주세요!`);
                                    }
                                }}
                                className={`flex items-center p-5 cursor-pointer transition-colors relative ${isMyRoom
                                    ? 'bg-[var(--color-idol-bg)] hover:bg-[var(--color-idol-bg)]/80 border-l-4 border-[var(--color-idol-dark)]'
                                    : (user?.role === 'IDOL' || room.isSubscribed)
                                        ? 'hover:bg-gray-50 border-l-4 border-transparent'
                                        : 'hover:bg-gray-50/50 opacity-90 border-l-4 border-transparent'
                                    }`}
                            >
                                {!(user?.role === 'IDOL' || room.isSubscribed) && (
                                    <div className="absolute right-4 top-4 bg-gray-100 text-gray-400 p-1.5 rounded-full shadow-sm z-10" title="구독 필요">
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path></svg>
                                    </div>
                                )}
                                <div className={`w-14 h-14 rounded-full bg-gray-200 overflow-hidden shrink-0 border-2 transition-all ${(user?.role === 'IDOL' || room.isSubscribed) ? 'border-transparent hover:border-[var(--color-idol-point)]' : 'border-gray-200 grayscale'}`}>
                                    {room.profileImage ? (
                                        <img src={room.profileImage} alt={room.stageName} className="w-full h-full object-cover" />
                                    ) : (
                                        <div className={`w-full h-full flex items-center justify-center font-bold text-xl ${(user?.role === 'IDOL' || room.isSubscribed) ? 'bg-[var(--color-idol-bg)] text-[var(--color-idol-dark)]' : 'bg-gray-100 text-gray-400'}`}>
                                            {room.stageName?.charAt(0) || "?"}
                                        </div>
                                    )}
                                </div>
                                <div className="ml-4 flex-1">
                                    <div className="flex justify-between items-center mb-1 pr-6 relative">
                                        <div className="flex items-center">
                                            <span className={`font-bold text-lg ${(user?.role === 'IDOL' || room.isSubscribed) ? 'text-gray-800' : 'text-gray-500'}`}>{room.stageName}</span>
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

    const isOtherIdolRoom = user?.role === 'IDOL' && myIdolId !== null && selectedIdolId !== myIdolId;
    const isRestricted = user?.status === 'RESTRICTED';
    const isInputDisabled = isOtherIdolRoom || isSending || isUploading || isRestricted;

    // Phase 2: 채팅방 레이아웃 
    const renderChatRoom = () => (
        <div className="bg-gray-50/50 backdrop-blur-xl border border-white/50 rounded-2xl shadow-2xl w-[calc(100%-0.5rem)] sm:w-full max-w-3xl mx-auto flex flex-col flex-1 overflow-hidden relative mt-2 sm:mt-4 mb-2 sm:mb-4">
            {/* 상단 헤더 영역 */}
            <div className="px-4 sm:px-5 py-3 sm:py-4 bg-white/95 border-b border-[var(--color-idol-bg)] flex items-center shadow-sm z-10 w-full shrink-0">
                <button
                    onClick={() => {
                        // 목록으로 돌아갈 때 안 읽은 개수 즉각 초기화 (눈속임 UI 확보)
                        setChatRooms(prev => prev.map(r => r.idolId === selectedIdolId ? { ...r, unreadCount: 0 } : r));
                        // 백엔드에도 한 번 더 명시적으로 읽음 처리 요청 발송
                        if (selectedIdolId) {
                            api.post(`/chat/read/${selectedIdolId}`).catch(console.error);
                        }
                        setIsSearchOpen(false); // 검색창 초기화
                        setSelectedIdolId(null);
                    }}
                    className="p-2 -ml-2 text-gray-500 hover:text-gray-800 hover:bg-gray-100 rounded-full transition-colors mr-3 active:scale-95"
                >
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7"></path></svg>
                </button>
                <div className="flex-1 flex items-center">
                    <div className="w-10 h-10 rounded-full bg-[var(--color-idol-bg)] flex items-center justify-center font-bold text-[var(--color-idol-dark)] mr-3 border border-[var(--color-idol-point)]/30">
                        {chatRooms.find(r => r.idolId === selectedIdolId)?.stageName?.substring(0, 1) || "I"}
                    </div>
                    <div>
                        <h3 className="font-bold text-gray-800">
                            {chatRooms.find(r => r.idolId === selectedIdolId)?.stageName || "멤버"}
                            {isOtherIdolRoom && <span className="ml-2 text-xs font-normal text-red-500 bg-red-50 px-2 py-0.5 rounded-full">읽기 전용</span>}
                        </h3>
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
                {/* 검색 토글 버튼 */}
                <button
                    onClick={() => { setIsSearchOpen(!isSearchOpen); }}
                    className={`p-2 ml-2 rounded-full transition-colors active:scale-95 ${isSearchOpen ? 'bg-[var(--color-idol)] text-white' : 'text-gray-500 hover:text-[var(--color-idol)] hover:bg-gray-100'}`}
                    title="채팅 내역 검색"
                >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                </button>
            </div>

            {/* 검색 사이드 패널 (Drawer) */}
            {isSearchOpen && (
                <div className="absolute top-[68px] right-0 sm:right-4 w-full sm:w-80 h-[calc(100%-140px)] bg-white/95 backdrop-blur-md shadow-2xl z-30 flex flex-col transform transition-transform rounded-xl border border-gray-200 overflow-hidden">
                    <div className="p-3 border-b bg-gray-50 flex items-center shrink-0">
                        <form onSubmit={handleSearch} className="flex flex-1 items-center bg-white rounded-lg border px-3 py-1.5 focus-within:ring-1 focus-within:ring-[var(--color-idol)]">
                            <input
                                type="text"
                                placeholder="채팅 내역 검색..."
                                value={searchKeyword}
                                onChange={(e) => setSearchKeyword(e.target.value)}
                                className="flex-1 outline-none text-sm bg-transparent"
                            />
                            <button type="submit" disabled={isSearching} className="text-gray-400 hover:text-[var(--color-idol)] p-1">
                                {isSearching ? (
                                    <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
                                ) : (
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                                )}
                            </button>
                        </form>
                        <button onClick={() => setIsSearchOpen(false)} className="ml-2 text-gray-400 p-1 hover:text-gray-800 rounded-lg hover:bg-gray-200">
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                        </button>
                    </div>
                    <div className="flex-1 overflow-y-auto p-3 space-y-3 custom-scrollbar text-sm">
                        {searchResults.length > 0 ? (
                            searchResults.map((res: ChatMessage, idx) => (
                                <div key={idx} className="bg-white border rounded-lg p-2.5 shadow-sm hover:shadow-md transition-shadow">
                                    <div className="flex justify-between items-center mb-1">
                                        <span className="font-bold text-[var(--color-idol-dark)] text-xs">{res.senderNickname || '알 수 없음'}</span>
                                        <span className="text-[10px] text-gray-400">{res.createdAt ? new Date(res.createdAt).toLocaleString('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''}</span>
                                    </div>
                                    <p className="text-gray-700 leading-snug break-words" dangerouslySetInnerHTML={{ __html: res.content.replace(new RegExp(searchKeyword, 'gi'), match => `<mark class="bg-yellow-200 rounded px-0.5 text-[var(--color-idol-dark)] font-medium">${match}</mark>`) }}></p>
                                </div>
                            ))
                        ) : (
                            <div className="flex flex-col items-center justify-center h-full text-gray-400 space-y-2 opacity-70 pb-10">
                                <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 16l2.879-2.879m0 0a3 3 0 104.243-4.242 3 3 0 00-4.243 4.242zM21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
                                <span>{isSearching ? '검색 중...' : '검색 결과가 없습니다.'}</span>
                            </div>
                        )}
                    </div>
                </div>
            )}

            {/* 공지사항 (Pinned Message) 영역 */}
            {pinnedMessage && (
                <div className="bg-[var(--color-idol-bg)] border-b border-[var(--color-idol-point)]/20 px-5 py-3 flex items-start space-x-3 shadow-sm shrink-0 z-10 transition-all">
                    <div className="mt-0.5 text-[var(--color-idol-point)] shrink-0 bg-white p-1 rounded-full shadow-sm">
                        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M5 4a2 2 0 012-2h6a2 2 0 012 2v14l-5-2.5L5 18V4z" />
                        </svg>
                    </div>
                    <div className="flex-1 min-w-0">
                        <div className="text-[10px] font-black tracking-wider text-[var(--color-idol-dark)] mb-0.5 uppercase">공지사항</div>
                        <p className="text-sm text-gray-800 break-words line-clamp-2 leading-snug">{pinnedMessage.content}</p>
                    </div>
                </div>
            )}

            {/* 채팅 내역 영역 */}
            <div ref={scrollContainerRef} className="flex-1 overflow-y-auto p-5 space-y-4 bg-[var(--color-idol-bg)]/40 flex flex-col custom-scrollbar">
                {/* 시스템 알림 라벨 */}
                <div className="flex justify-center my-2 shrink-0">
                    <span className="bg-[var(--color-idol-point)] text-white text-xs px-4 py-1.5 rounded-full shadow-sm opacity-90">
                        채팅방에 입장했습니다
                    </span>
                </div>

                {/* 수동 '이전 대화 더 보기' 버튼 영역 */}
                {hasMore && (
                    <div className="flex justify-center w-full pb-2 shrink-0">
                        <button
                            onClick={loadMoreHistory}
                            disabled={isLoadingMore}
                            className="bg-white/80 hover:bg-white text-[var(--color-idol)] text-xs font-semibold px-5 py-2 rounded-full shadow-sm border border-[var(--color-idol)]/20 transition-all hover:shadow-md disabled:bg-gray-100 disabled:text-gray-400 disabled:shadow-none flex items-center space-x-2"
                        >
                            {isLoadingMore ? (
                                <>
                                    <svg className="animate-spin h-3.5 w-3.5" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    <span>불러오는 중...</span>
                                </>
                            ) : (
                                <>
                                    <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 15l7-7 7 7"></path>
                                    </svg>
                                    <span>이전 대화 더 보기</span>
                                </>
                            )}
                        </button>
                    </div>
                )}

                {/* 메시지 렌더링 */}
                {messages.map((msg, idx) => {
                    // 서버가 던진 me: true 값을 최우선으로, 없으면 아이디 비교
                    const isMine = msg.me === true || String(msg.senderId) === String(user?.userId);
                    const msgKey = msg.id ? msg.id : `msg-${idx}-${msg.createdAt || Date.now()}`;

                    return (
                        <div key={msgKey} className={`flex ${isMine ? 'justify-end' : 'justify-start'} shrink-0 transform transition-all`}>
                            {!isMine && (
                                <div className="w-8 h-8 rounded-full bg-gray-200 mr-2 overflow-hidden shrink-0 border border-[var(--color-idol)]/20">
                                    {user?.role === 'IDOL' ? (
                                        <div className="w-full h-full flex items-center justify-center bg-[var(--color-idol-bg)] text-[var(--color-idol-dark)] font-bold text-xs">
                                            {msg.senderNickname?.substring(0, 1) || "F"}
                                        </div>
                                    ) : chatRooms.find(r => r.idolId === selectedIdolId)?.profileImage ? (
                                        <img src={chatRooms.find(r => r.idolId === selectedIdolId)!.profileImage} alt="profile" className="w-full h-full object-cover" />
                                    ) : (
                                        <div className="w-full h-full flex items-center justify-center bg-[var(--color-idol-point)] text-white font-bold text-xs">
                                            {chatRooms.find(r => r.idolId === selectedIdolId)?.stageName?.substring(0, 1) || "I"}
                                        </div>
                                    )}
                                </div>
                            )}
                            <div className={`max-w-[70%] px-4 py-2.5 rounded-2xl shadow-sm border ${isMine
                                ? 'bg-[var(--color-idol)] text-white rounded-tr-sm border-[var(--color-idol-dark)]/20 shadow-[var(--color-idol)]/20'
                                : 'bg-white text-gray-800 rounded-tl-sm border-[var(--color-idol-point)]/40 shadow-sm'
                                }`}>
                                {!isMine && <div className="text-xs text-gray-500 mb-1 font-semibold">{msg.senderNickname}</div>}

                                {msg.type === 'IMAGE' ? (
                                    <div className="mt-1 mb-1 relative overflow-hidden rounded-xl border border-black/5 bg-white/50 cursor-pointer" onClick={() => window.open(msg.content, "_blank")}>
                                        {/* 원본이 아닌 썸네일 URL 렌더링 (없으면 원본) */}
                                        <img src={msg.thumbnailUrl || msg.content} alt="Media" className="max-w-full max-h-64 object-contain transition-transform hover:scale-105" onLoad={scrollToBottom} />
                                    </div>
                                ) : msg.type === 'VIDEO' ? (
                                    <div className="mt-1 mb-1 relative overflow-hidden rounded-xl border border-black/5 bg-black/50">
                                        <video src={msg.content} controls className="max-w-full max-h-64 object-contain" onLoadedMetadata={scrollToBottom} />
                                    </div>
                                ) : (
                                    <div className="whitespace-pre-wrap word-break">{msg.content}</div>
                                )}

                                {msg.createdAt && (
                                    <div className={`text-[10px] mt-1.5 ${isMine ? 'text-white/70 text-right' : 'text-gray-400 text-left'}`}>
                                        {new Date(msg.createdAt).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })}
                                    </div>
                                )}

                                {/* 리액션 렌더링 */}
                                {msg.reactions && Object.keys(msg.reactions).length > 0 && (
                                    <div className={`flex flex-wrap gap-1 mt-1.5 ${isMine ? 'justify-end' : 'justify-start'}`}>
                                        {Object.entries(msg.reactions).map(([reaction, count]) => (
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
                })}

                {/* 작성 중 표시 (아이돌이 입력 중일 때) */}
                {isIdolTyping && selectedIdolId && (
                    <div className="flex justify-start shrink-0 transform transition-all">
                        <div className="w-8 h-8 rounded-full bg-gray-200 mr-2 overflow-hidden shrink-0 border border-[var(--color-idol)]/20">
                            {chatRooms.find(r => r.idolId === selectedIdolId)?.profileImage ? (
                                <img src={chatRooms.find(r => r.idolId === selectedIdolId)!.profileImage} alt="profile" className="w-full h-full object-cover" />
                            ) : (
                                <div className="w-full h-full flex items-center justify-center bg-[var(--color-idol-point)] text-white font-bold text-xs">
                                    {chatRooms.find(r => r.idolId === selectedIdolId)?.stageName?.substring(0, 1) || "I"}
                                </div>
                            )}
                        </div>
                        <div className="px-4 py-3 rounded-2xl shadow-sm border bg-white border-[var(--color-idol-point)]/40 rounded-tl-sm text-gray-400 flex items-center mb-1">
                            <span className="flex space-x-1.5 items-center justify-center h-4">
                                <span className="w-1.5 h-1.5 bg-[var(--color-idol-point)]/60 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></span>
                                <span className="w-1.5 h-1.5 bg-[var(--color-idol-point)]/60 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></span>
                                <span className="w-1.5 h-1.5 bg-[var(--color-idol-point)]/60 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></span>
                            </span>
                        </div>
                    </div>
                )}
                <div ref={messagesEndRef} />
            </div>

            {/* 입력창 바텀바 */}
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
                    <div className={`flex items-center rounded-full border p-1 px-3 w-full transition-all shadow-inner ${isInputDisabled ? 'bg-gray-200 border-gray-300 opacity-70 cursor-not-allowed' : 'bg-gray-50 border-gray-200 focus-within:ring-2 focus-within:ring-[var(--color-idol-bg)] focus-within:border-[var(--color-idol-point)]'}`}>
                        {/* 더하기 버튼 (미디어 업로드 로직으로 변경됨) */}
                        <button
                            className={`p-2 transition-colors active:scale-95 flex-shrink-0 ${isInputDisabled ? 'text-gray-400 cursor-not-allowed' : 'text-gray-500 hover:text-[var(--color-idol-dark)]'}`}
                            disabled={isInputDisabled}
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
                            onChange={handleFileUpload}
                            disabled={isOtherIdolRoom || isUploading || isSending}
                        />
                        <input
                            type="text"
                            placeholder={isRestricted ? "활동이 제한되어 메시지를 보낼 수 없습니다." : isOtherIdolRoom ? "자신의 채팅방에서만 메시지를 보낼 수 있습니다." : isSending ? "도배 방지: 3초 후 입력 가능합니다." : "메시지 전송"}
                            value={newMessage}
                            onChange={(e) => {
                                setNewMessage(e.target.value);
                                handleTyping();
                            }}
                            onKeyDown={handleKeyDown}
                            disabled={isInputDisabled}
                            className={`flex-1 bg-transparent border-none focus:ring-0 px-3 py-3 text-[15px] outline-none ${isInputDisabled ? 'text-gray-500 cursor-not-allowed' : 'text-gray-800'}`}
                        />
                        <button
                            onClick={handleSendMessage}
                            disabled={!newMessage.trim() || isInputDisabled}
                            className={`ml-2 px-4 py-2 font-medium rounded-full flex items-center justify-center shadow-md transition-all sm:active:scale-95 min-w-14 ${isInputDisabled ? 'bg-gray-400 text-gray-200 cursor-not-allowed' : 'bg-gradient-to-r from-[var(--color-idol)] to-[var(--color-idol-dark)] hover:from-[var(--color-idol-dark)] hover:to-[var(--color-idol-dark)] text-white disabled:opacity-50'}`}
                        >
                            전송
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );

    return (
        <div className="flex-1 flex flex-col w-full h-full pt-2 pb-6 sm:pb-12 z-10 relative">
            {/* 배경 블러 효과 */}
            <div className="absolute top-20 right-10 w-96 h-96 bg-[var(--color-idol-point)] rounded-full mix-blend-multiply filter blur-3xl opacity-20 pointer-events-none"></div>
            <div className="absolute -bottom-10 left-20 w-80 h-80 bg-[var(--color-idol)] rounded-full mix-blend-multiply filter blur-3xl opacity-20 pointer-events-none"></div>

            {/* View Switching */}
            {selectedIdolId === null ? renderRoomList() : renderChatRoom()}
        </div>
    );
};

export default ChatPage;
