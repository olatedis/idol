import React, { useState, useEffect, useCallback, useMemo } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore.ts";
import { api } from "../../api/axios.ts";
import { Client } from "@stomp/stompjs";
import { showErrorToast, showAlert } from "../../utils/alert";
import type { ChatRoom, ChatMessage } from "../../types/chat";

// 컴포넌트 임포트
import ChatRoomList from "../../components/chat/ChatRoomList";
import MessageItem from "../../components/chat/MessageItem";
import ChatInput from "../../components/chat/ChatInput";
import SearchDrawer from "../../components/chat/SearchDrawer";
import PinnedMessage from "../../components/chat/PinnedMessage";
import TypingIndicator from "../../components/chat/TypingIndicator";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8000";
const WS_URL = API_BASE_URL.replace("http", "ws") + "/ws-chat";

const ChatPage: React.FC = () => {
    const { groupId } = useParams<{ groupId?: string }>();
    const [searchParams] = useSearchParams();
    const { user } = useAuthStore();

    // UI 상태 관리
    const [selectedIdolId, setSelectedIdolId] = useState<number | null>(null);
    const [chatRooms, setChatRooms] = useState<ChatRoom[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    // 채팅 상태 관리 (Phase 2)
    const [messages, setMessages] = useState<ChatMessage[]>([]);
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
                .catch(() => {});
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
                    isSubscribed: roomData.subscribed || false,
                    isOnline: roomData.online || false
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

    // 딥링크 대응: URL에 idolId가 있으면 해당 방 자동 선택
    useEffect(() => {
        const idolIdParam = searchParams.get("idolId");
        if (idolIdParam && !selectedIdolId) {
            setSelectedIdolId(Number(idolIdParam));
        }
    }, [searchParams, selectedIdolId]);

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
                // 에러 무시
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
                // 

                const handleIncomingMessage = (message: any) => {
                    const parsed: ChatMessage = JSON.parse(message.body);

                    // --- 이벤트 핸들링 시작 ---
                    // 1. 메시지 삭제
                    if (parsed.type === "DELETE") {
                        setMessages((prev) => {
                            // ID가 일치하는 메시지 찾기
                            let targetId = parsed.id;
                            
                            // 만약 본인이 보낸 메시지라면, temp ID 상태일 수 있으므로 가장 최근 메시지를 타겟으로 시도
                            const currentUserId = user?.userId ? String(user.userId) : null;
                            const eventSenderId = parsed.senderId ? String(parsed.senderId) : null;

                            if (currentUserId && eventSenderId === currentUserId) {
                                const lastMine = [...prev].reverse().find(m => String(m.senderId) === currentUserId);
                                if (lastMine && String(lastMine.id).startsWith("temp-")) {
                                    targetId = lastMine.id;
                                }
                            }

                            return prev.map(m => m.id === targetId ? { ...m, content: "삭제된 메시지입니다.", type: "DELETED" } : m);
                        });
                        
                        // AI 필터링에 의한 본인 메시지 삭제 시 즉각 피드백 (조건 비교 강화)
                        const isAiFiltered = parsed.deleteReason === "AI_FILTERED";
                        const isMyMessage = String(parsed.senderId) === String(user?.userId);
                        
                        if (isAiFiltered && isMyMessage) {
                            showErrorToast("작성하신 메시지가 AI 필터링에 의해 부적절하다고 판단되어 삭제되었습니다.");
                        }
                        return;
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

                    // 4. 아이돌 접속 상태 실시간 변경 이벤트 처리
                    if (parsed.type === "STATUS") {
                        setIsIdolOnline(parsed.content === "ON");
                        return;
                    }

                    // 5. 아이돌 타이핑 상태 실시간 변경 이벤트 처리
                    if (parsed.type === "TYPING") {
                        setIsIdolTyping(true);
                        if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
                        typingTimeoutRef.current = setTimeout(() => setIsIdolTyping(false), 3000);
                        return;
                    }
                    // --- 이벤트 핸들링 끝 ---

                    setMessages((prev) => {
                        // 1. 서버에서 온 진짜 ID가 이미 목록에 있는지 확인 (중복 방지)
                        if (parsed.id && !String(parsed.id).startsWith("temp-") && prev.some(m => m.id === parsed.id)) {
                            return prev;
                        }

                        // 2. 내가 보낸 메시지인 경우, 기존의 Optimistic UI(temp-) 메시지를 서버 데이터로 교체
                        if (String(parsed.senderId) === String(user.userId) && parsed.id && !String(parsed.id).startsWith("temp-")) {
                            // 가장 최근의 매칭되는 임시 메시지 찾기
                            const lastTempIndex = [...prev].reverse().findIndex(m => 
                                String(m.id).startsWith("temp-") && 
                                (m.content === parsed.content || m.type !== "TEXT") // 미디어는 URL이 다를 수 있으므로 타입으로 체크
                            );
                            
                            if (lastTempIndex !== -1) {
                                const realIndex = prev.length - 1 - lastTempIndex;
                                const newMessages = [...prev];
                                newMessages[realIndex] = { ...parsed, me: true };
                                return newMessages;
                            }
                        }

                        // 3. 남이 보낸 메시지이거나 매칭되는 임시 메시지가 없는 경우 새로 추가
                        return [...prev, { ...parsed, me: String(parsed.senderId) === String(user.userId) }];
                    });
                    setTimeout(scrollToBottom, 100);
                    setIsIdolTyping(false); 
                };

                // 공지성 및 아이돌 발송 메시지용 공용 채널
                client.subscribe(`/sub/idol/${selectedIdolId}`, handleIncomingMessage);

                // 에러 발생 및 도배 방지 시 서버 브로드캐스팅 수신
                client.subscribe(`/queue/errors/${user.userId}`, (message: any) => {
                    const errorPayload = JSON.parse(message.body);
                    // console.error("STOMP Error Received:", errorPayload);

                    if (errorPayload.code === "RATE_LIMIT") {
                        showErrorToast(`전송 제한: ${errorPayload.message}`);
                    } else if (errorPayload.code === "FORBIDDEN") {
                        showErrorToast(`권한 오류: ${errorPayload.message}`);
                    } else {
                        showErrorToast(`서버 오류: ${errorPayload.message || "메시지 전송 중 문제가 발생했습니다."}`);
                    }

                    // Optimistic UI로 올라간 실패한 임시 메시지 즉각 롤백(제거)
                    setMessages(prev => prev.filter(m => !String(m.id).startsWith("temp-")));
                });

                // IDOL 권한일 경우 팬들이 나에게 보내는 프라이빗 큐 채널 추가 구독
                if (user.role === "IDOL") {
                    client.subscribe(`/queue/idol/${selectedIdolId}`, handleIncomingMessage);
                }
            },
            onStompError: () => {
                // Broker error ignored
            },
            onWebSocketError: () => {
                // WebSocket error ignored
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
        } finally {
            setIsLoadingMore(false);
        }
    }, [isLoadingMore, hasMore, selectedIdolId, messages]);

    // IntersectionObserver 자동 스크롤 하단 감지 로직은 '이전 대화 더 보기' 수동 버튼 방식으로 교체되었습니다.

    // 메시지 전송 로직
    const handleSendMessage = (content: string) => {
        if (!content.trim() || !stompClientRef.current?.active || !selectedIdolId || !user || isSending || isUploading) return;

        // 서버 부담 및 도배를 막기 위해 팬 계정은 3초 연속 전송 비활성화 락
        if (user.role === 'USER') {
            setIsSending(true);
            setTimeout(() => setIsSending(false), 3000);
        }

        const payload = {
            idolId: selectedIdolId,
            content: content,
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
                content: content,
                type: "TEXT",
                createdAt: new Date().toISOString()
            }]);

            setTimeout(scrollToBottom, 100);
        } catch (err) {
        }
    };

    // 미디어 파일 업로드 핸들러
    const handleFileUpload = async (file: File) => {
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
            showErrorToast("파일 업로드에 실패했습니다.");
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
            showErrorToast("채팅 내역 검색에 실패했습니다.");
        } finally {
            setIsSearching(false);
        }
    };


    const handleSelectRoom = useCallback((idolId: number, stageName: string, isSubscribed: boolean) => {
        if (user?.role === 'IDOL' || isSubscribed) {
            setSelectedIdolId(idolId);
        } else {
            showAlert("구독 안내", `'${stageName}' 님과의 1:1 채팅은 구독 플랜 열람권이 필요합니다.\n먼저 구독을 진행해 주세요!`, "warning");
        }
    }, [user?.role]);

    const handleBackToList = useCallback(() => {
        setChatRooms(prev => prev.map(r => r.idolId === selectedIdolId ? { ...r, unreadCount: 0 } : r));
        if (selectedIdolId) {
            api.post(`/chat/read/${selectedIdolId}`).catch(() => {});
        }
        setIsSearchOpen(false);
        setSelectedIdolId(null);
    }, [selectedIdolId]);

    const isOtherIdolRoom = useMemo(() => user?.role === 'IDOL' && myIdolId !== null && selectedIdolId !== myIdolId, [user?.role, myIdolId, selectedIdolId]);
    const isRestricted = useMemo(() => user?.status === 'RESTRICTED', [user?.status]);
    const isInputDisabled = isOtherIdolRoom || isSending || isUploading || isRestricted;

    const currentRoom = useMemo(() => chatRooms.find(r => r.idolId === selectedIdolId), [chatRooms, selectedIdolId]);

    return (
        <div className="flex-1 flex flex-col w-full h-full pt-2 pb-6 sm:pb-12 z-10 relative">
            <div className="absolute top-20 right-10 w-96 h-96 bg-[var(--color-idol-point)] rounded-full mix-blend-multiply filter blur-3xl opacity-20 pointer-events-none"></div>
            <div className="absolute -bottom-10 left-20 w-80 h-80 bg-[var(--color-idol)] rounded-full mix-blend-multiply filter blur-3xl opacity-20 pointer-events-none"></div>

            {selectedIdolId === null ? (
                <ChatRoomList 
                    rooms={chatRooms} 
                    isLoading={isLoading} 
                    onSelectRoom={handleSelectRoom}
                    currentUserIdolId={myIdolId}
                    userRole={user?.role}
                />
            ) : (
                <div className="bg-gray-50/50 backdrop-blur-xl border border-white/50 rounded-2xl shadow-2xl w-[calc(100%-0.5rem)] sm:w-full max-w-3xl mx-auto flex flex-col flex-1 overflow-hidden relative mt-2 sm:mt-4 mb-2 sm:mb-4">
                    {/* 상단 헤더 */}
                    <div className="px-4 sm:px-5 py-3 sm:py-4 bg-white/95 border-b border-[var(--color-idol-bg)] flex items-center shadow-sm z-10 w-full shrink-0">
                        <button onClick={handleBackToList} className="p-2 -ml-2 text-gray-500 hover:text-gray-800 hover:bg-gray-100 rounded-full transition-colors mr-3 active:scale-95">
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7"></path></svg>
                        </button>
                        <div className="flex-1 flex items-center">
                            <div className="w-10 h-10 rounded-full bg-[var(--color-idol-bg)] flex items-center justify-center font-bold text-[var(--color-idol-dark)] mr-3 border border-[var(--color-idol-point)]/30">
                                {currentRoom?.stageName?.substring(0, 1) || "I"}
                            </div>
                            <div>
                                <h3 className="font-bold text-gray-800">
                                    {currentRoom?.stageName || "멤버"}
                                    {isOtherIdolRoom && <span className="ml-2 text-xs font-normal text-red-500 bg-red-50 px-2 py-0.5 rounded-full">읽기 전용</span>}
                                </h3>
                                <p className={`text-xs flex items-center mt-0.5 ${isIdolOnline ? 'text-green-500' : 'text-gray-400'}`}>
                                    <span className={`w-1.5 h-1.5 rounded-full mr-1.5 inline-block ${isIdolOnline ? 'bg-green-500 animate-pulse' : 'bg-gray-400'}`}></span>
                                    {isIdolOnline ? '온라인' : '오프라인'}
                                </p>
                            </div>
                        </div>
                        <button onClick={() => setIsSearchOpen(!isSearchOpen)} className={`p-2 ml-2 rounded-full transition-colors active:scale-95 ${isSearchOpen ? 'bg-[var(--color-idol)] text-white' : 'text-gray-500 hover:text-[var(--color-idol)] hover:bg-gray-100'}`}>
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                        </button>
                    </div>

                    <SearchDrawer 
                        isOpen={isSearchOpen} 
                        onClose={() => setIsSearchOpen(false)} 
                        keyword={searchKeyword} 
                        onKeywordChange={setSearchKeyword}
                        onSearch={handleSearch}
                        isSearching={isSearching}
                        results={searchResults}
                    />

                    <PinnedMessage message={pinnedMessage} />

                    <div ref={scrollContainerRef} className="flex-1 overflow-y-auto p-5 space-y-4 bg-[var(--color-idol-bg)]/40 flex flex-col custom-scrollbar">
                        <div className="flex justify-center my-2 shrink-0">
                            <span className="bg-[var(--color-idol-point)] text-white text-xs px-4 py-1.5 rounded-full shadow-sm opacity-90">채팅방에 입장했습니다</span>
                        </div>

                        {hasMore && (
                            <div className="flex justify-center w-full pb-2 shrink-0">
                                <button onClick={loadMoreHistory} disabled={isLoadingMore} className="bg-white/80 hover:bg-white text-[var(--color-idol)] text-xs font-semibold px-5 py-2 rounded-full shadow-sm border border-[var(--color-idol)]/20 transition-all">
                                    {isLoadingMore ? '불러오는 중...' : '이전 대화 더 보기'}
                                </button>
                            </div>
                        )}

                        {messages.map((msg, idx) => (
                            <MessageItem 
                                key={msg.id || `msg-${idx}`}
                                message={msg}
                                isMine={Boolean(msg.me || String(msg.senderId) === String(user?.userId))}
                                profileImage={msg.senderRole === 'IDOL' ? currentRoom?.profileImage : undefined}
                                stageName={currentRoom?.stageName}
                                onImageClick={(url) => window.open(url, "_blank")}
                                scrollToBottom={scrollToBottom}
                            />
                        ))}

                        {isIdolTyping && selectedIdolId && !isOtherIdolRoom && user?.role !== 'IDOL' && (
                            <TypingIndicator profileImage={currentRoom?.profileImage} stageName={currentRoom?.stageName} />
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    <ChatInput 
                        onSendMessage={handleSendMessage}
                        onFileUpload={handleFileUpload}
                        onTyping={handleTyping}
                        isDisabled={isInputDisabled}
                        isUploading={isUploading}
                        isRestricted={isRestricted}
                        isOtherIdolRoom={isOtherIdolRoom}
                        isSending={isSending}
                    />
                </div>
            )}
        </div>
    );
};

export default ChatPage;
