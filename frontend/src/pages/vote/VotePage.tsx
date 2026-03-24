import React, { useEffect, useState, useRef } from "react";
import { api } from "../../api/axios";
import { useAuthStore } from "../../stores/authStore";
import { useParams } from "react-router-dom";
import { motion, AnimatePresence, useSpring, useTransform } from "framer-motion";
import { showSuccessToast, showErrorToast, showConfirm } from "../../utils/alert";
import { Client } from "@stomp/stompjs";

interface CandidateDto {
    number: number;
    name: string;
    image: string;
    voteCount: number;
    delta?: number;
}

interface VoteInfo {
    id: number;
    title: string;
    description: string;
    startDate: string;
    endDate: string;
    status: "OPEN" | "CLOSED" | "UPCOMING";
    candidates: CandidateDto[];
}

interface MyVoteRecordDto {
    voteId: number;
    voteTitle: string;
    candidateName: string;
    votedAt: string;
}

interface RankingDto {
    candidateNumber: number;
    score: number;
    delta: number;
}

type TabType = 'OPEN' | 'CLOSED' | 'MY';

const AnimatedNumber = ({ value }: { value: number }) => {
    const spring = useSpring(value, { mass: 0.8, stiffness: 75, damping: 15 });
    const display = useTransform(spring, (current) => Math.round(current).toLocaleString());

    useEffect(() => {
        spring.set(value);
    }, [spring, value]);

    return <motion.span>{display}</motion.span>;
};

const VotePage: React.FC = () => {
    const { user, accessToken } = useAuthStore();
    const { groupId } = useParams<{ groupId?: string }>(); // URL에서 groupId 추출

    // 상태
    const [activeTab, setActiveTab] = useState<TabType>('OPEN');
    const [votes, setVotes] = useState<VoteInfo[]>([]);
    const [myVotes, setMyVotes] = useState<MyVoteRecordDto[]>([]);
    const [page, setPage] = useState(0);
    const [hasNext, setHasNext] = useState(false);

    // 모달 상태
    const [selectedVote, setSelectedVote] = useState<VoteInfo | null>(null);
    const [selectedCandidate, setSelectedCandidate] = useState<number | null>(null);
    const [hasVoted, setHasVoted] = useState(false);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

    // 로딩 및 제출 상태 추가 (중복 투표 방지)
    const [isFetchingDetail, setIsFetchingDetail] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);

    // WebSocket
    const stompClient = useRef<Client | null>(null);

    // 투표 생성 폼 상태
    const [newVote, setNewVote] = useState({
        title: '',
        description: '',
        startDate: '',
        endDate: '',
        targetGroupId: groupId ? parseInt(groupId, 10) : undefined, // 현재 그룹 정보 기본 삽입
        candidates: [{ name: '', image: '' }, { name: '', image: '' }]
    });

    // 권한 체크
    const canCreateVote = user && ['ADMIN', 'AGENCY', 'IDOL'].includes(user.role);

    // 데이터 초기화
    useEffect(() => {
        setVotes([]);
        setMyVotes([]);
        setPage(0);
        fetchData(0, activeTab);
    }, [activeTab, groupId]);

    // 모달 열릴 때 시작일 자동 설정
    useEffect(() => {
        if (isCreateModalOpen) {
            const now = new Date();
            const tomorrow = new Date(now);
            tomorrow.setDate(tomorrow.getDate() + 1);

            const format = (d: Date) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);

            setNewVote(prev => ({
                ...prev,
                startDate: format(now),
                endDate: format(tomorrow)
            }));
        }
    }, [isCreateModalOpen]);

    // WebSocket 연결 (상세 모달 열릴 때)
    useEffect(() => {
        const voteId = selectedVote?.id;
        const status = selectedVote?.status;

        if (voteId && status === 'OPEN') {
            connectWebSocket(voteId);
        } else {
            disconnectWebSocket();
        }

        return () => disconnectWebSocket();
    }, [selectedVote?.id, selectedVote?.status]);

    const connectWebSocket = (voteId: number) => {
        let wsUrl = "";
        const envBaseUrl = import.meta.env.VITE_API_BASE_URL;
        if (envBaseUrl && envBaseUrl.startsWith('http')) {
            wsUrl = envBaseUrl.replace(/^http/, "ws") + "/ws-ranking";
        } else {
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
            wsUrl = `${protocol}//${window.location.host}/api/ws-ranking`;
        }

        const client = new Client({
            brokerURL: wsUrl,
            connectHeaders: {},
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            onConnect: () => {
                
                client.subscribe(`/topic/votes/${voteId}/ranking`, (message) => {
                    const rankingList: RankingDto[] = JSON.parse(message.body);
                    updateVoteCounts(rankingList);
                });
            },
            onStompError: (frame) => {
                console.error("Broker reported error: " + frame.headers["message"]);
                console.error("Additional details: " + frame.body);
            },
        });

        client.activate();
        stompClient.current = client;
    };

    const disconnectWebSocket = () => {
        if (stompClient.current) {
            stompClient.current.deactivate();
            stompClient.current = null;
            
        }
    };

    const updateVoteCounts = (rankingList: RankingDto[]) => {
        setSelectedVote(prev => {
            if (!prev) return null;

            const updatedCandidates = prev.candidates.map(c => {
                const ranking = rankingList.find(r => r.candidateNumber === c.number);
                return ranking ? { ...c, voteCount: ranking.score, delta: ranking.delta } : c;
            });

            // 점수순(내림차순) 정렬 적용 (Framer Motion이 layout 스와핑 애니메이션 자동 실행)
            updatedCandidates.sort((a, b) => b.voteCount - a.voteCount);

            return { ...prev, candidates: updatedCandidates };
        });
    };

    const fetchData = async (pageNum: number, tab: TabType) => {
        try {
            if (tab === 'MY') {
                if (!user) return;
                const url = groupId 
                    ? `/api/votes/me?groupId=${groupId}` 
                    : "/api/votes/me";
                const { data } = await api.get(url);
                setMyVotes(data);
            } else {
                // groupId가 있으면 쿼리스트링에 포함하여 요청
                const url = groupId
                    ? `/api/votes?page=${pageNum}&size=10&groupId=${groupId}`
                    : `/api/votes?page=${pageNum}&size=10`;

                const { data } = await api.get(url);

                const filtered = (data.content || []).filter((v: VoteInfo) => {
                    if (tab === 'OPEN') {
                        return v.status === 'OPEN' || v.status === 'UPCOMING';
                    }
                    return v.status === tab;
                });

                if (pageNum === 0) {
                    setVotes(filtered);
                } else {
                    setVotes(prev => [...prev, ...filtered]);
                }
                setHasNext(pageNum < (data?.totalPages || 0) - 1);
            }
        } catch (error) {
            console.error("데이터 조회 실패:", error);
        }
    };

    const handleLoadMore = () => {
        const nextPage = page + 1;
        setPage(nextPage);
        fetchData(nextPage, activeTab);
    };

    // 투표 상세 조회
    const fetchVoteDetail = async (voteId: number) => {
        try {
            setIsFetchingDetail(true); // 데이터 호출 시작점
            setSelectedCandidate(null);

            const { data } = await api.get(`/api/votes/${voteId}`);
            if (data.candidates) {
                data.candidates.sort((a: CandidateDto, b: CandidateDto) => b.voteCount - a.voteCount);
            }
            setSelectedVote(data);

            if (user) {
                const { data: voted } = await api.get(`/api/votes/${voteId}/check`);
                setHasVoted(voted);
            } else {
                setHasVoted(false); // 로그인하지 않은 경우 투표 안 한 것으로 간주
            }
        } catch (error) {
            console.error("투표 상세 조회 실패:", error);
        } finally {
            setIsFetchingDetail(false); // 데이터 호출 완료 지점
        }
    };

    // 투표하기
    const handleVote = async () => {
        if (!selectedVote || selectedCandidate === null || isSubmitting) return;
        if (!user || !accessToken) {
            showErrorToast("로그인이 필요합니다.");
            return;
        }

        try {
            setIsSubmitting(true);
            await api.post(`/api/votes/${selectedVote.id}`, {
                candidateNumber: selectedCandidate
            });
            showSuccessToast("투표가 완료되었습니다!");
            setHasVoted(true);
            // WebSocket이 업데이트해주므로 fetchVoteDetail 호출 안 함
        } catch (error: any) {
            showErrorToast(error.response?.data || "투표 실패");
        } finally {
            setIsSubmitting(false);
        }
    };

    // 투표 취소
    const handleCancelVote = async () => {
        if (!selectedVote || isSubmitting) return;

        const confirmed = await showConfirm("투표 취소", "정말 투표를 취소하시겠습니까?");
        if (!confirmed) return;

        try {
            setIsSubmitting(true);
            await api.post(`/api/votes/${selectedVote.id}/cancel`);
            showSuccessToast("투표가 취소되었습니다.");
            setHasVoted(false);

            // 즉시 투표 정보를 다시 불러와서 프론트 화면 상태(투표 수치 등)를 최신화
            fetchVoteDetail(selectedVote.id);
            if (activeTab === 'MY') {
                fetchData(0, 'MY');
            }
        } catch (error: any) {
            showErrorToast(error.response?.data || "취소 실패");
        } finally {
            setIsSubmitting(false);
        }
    };

    // 이미지 업로드
    const handleImageUpload = async (index: number, file: File) => {
        const formData = new FormData();
        formData.append("file", file);

        try {
            // chat-service의 공용 S3 업로드 기능을 그대로 활용합니다.
            const { data } = await api.post("/chat/upload", formData, {
                headers: { "Content-Type": "multipart/form-data" }
            });

             // 디버깅용 로그

            const imageUrl = data.fileUrl || data.url;
            if (!imageUrl) {
                console.error("응답에 파일 URL이 없습니다.", data);
            }
            updateCandidate(index, 'image', imageUrl);
        } catch (error) {
            console.error("이미지 업로드 실패:", error);
            showErrorToast("이미지 업로드에 실패했습니다.");
        }
    };

    // 투표 생성
    const handleCreateVote = async () => {
        if (!newVote.title || !newVote.startDate || !newVote.endDate) {
            showErrorToast("필수 정보를 입력해주세요.");
            return;
        }
        if (newVote.candidates.some(c => !c.name)) {
            showErrorToast("후보 이름을 모두 입력해주세요.");
            return;
        }

        try {
            // 사용자가 입력한 KST 시각을 UTC로 변환하되, 백엔드 파싱을 위해 끝의 'Z'만 제거하여 전송합니다.
            const adjustToUTC = (dateTimeStr: string) => {
                if (!dateTimeStr) return "";
                const d = new Date(dateTimeStr);
                return d.toISOString().split('.')[0]; // YYYY-MM-DDTHH:mm:ss (UTC 기준 시간)
            };

            const requestBody = {
                ...newVote,
                targetGroupId: newVote.targetGroupId || null, // null 이면 전체를 대상으로 함
                startDate: adjustToUTC(newVote.startDate),
                endDate: adjustToUTC(newVote.endDate),
                candidates: newVote.candidates.map((c, idx) => ({
                    number: idx + 1,
                    name: c.name,
                    image: c.image
                }))
            };

             // 전송될 페이로드 확인용

            await api.post("/api/votes", requestBody);
            showSuccessToast("투표가 생성되었습니다.");
            setIsCreateModalOpen(false);

            setNewVote({
                title: '',
                description: '',
                startDate: '',
                endDate: '',
                targetGroupId: groupId ? parseInt(groupId, 10) : undefined,
                candidates: [{ name: '', image: '' }, { name: '', image: '' }]
            });
            setActiveTab('OPEN');
            fetchData(0, 'OPEN');
        } catch (error: any) {
            console.error("투표 생성 실패 에러 정보:", error.response?.data || error.message);
            showErrorToast(
                error.response?.data?.message ||
                (typeof error.response?.data === 'string' ? error.response.data : "투표 생성 실패")
            );
        }
    };

    const addCandidate = () => {
        setNewVote(prev => ({
            ...prev,
            candidates: [...prev.candidates, { name: '', image: '' }]
        }));
    };

    const removeCandidate = (index: number) => {
        if (newVote.candidates.length <= 2) {
            showErrorToast("최소 2명의 후보가 필요합니다.");
            return;
        }
        setNewVote(prev => ({
            ...prev,
            candidates: prev.candidates.filter((_, i) => i !== index)
        }));
    };

    const updateCandidate = (index: number, field: 'name' | 'image', value: string) => {
        const updated = [...newVote.candidates];
        updated[index] = { ...updated[index], [field]: value };
        setNewVote(prev => ({ ...prev, candidates: updated }));
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-[var(--color-idol-bg)] via-white to-[var(--color-idol-bg)]/50 overflow-x-hidden">
            <main className="pt-[80px] sm:pt-[100px] px-4 sm:px-6 pb-8 sm:pb-12 max-w-7xl mx-auto relative z-10 w-full overflow-hidden">
                {/* 탭 제목 영역 (그룹/전체 명시) */}
                <div className="mb-8">
                    <h1 className="text-3xl font-black text-gray-800">
                        {groupId ? `투표소` : `전체 공개 투표소`}
                    </h1>
                    <p className="text-gray-500 mt-2 font-medium">
                        {groupId ? "우리 그룹만의 투표에 참여해보세요!" : "진행 중인 모든 공식 투표를 확인하세요."}
                    </p>
                </div>

                {/* 배경 장식용 블러 원형 그래픽 */}
                <div className="absolute top-20 left-10 w-72 h-72 bg-[var(--color-idol-point)] rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob"></div>
                <div className="absolute top-20 right-10 w-72 h-72 bg-[var(--color-idol-dark)] rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-2000"></div>
                <div className="absolute -bottom-8 left-40 w-72 h-72 bg-[var(--color-idol)] rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-4000"></div>

                <div className="flex flex-col md:flex-row justify-between items-center mb-8 sm:mb-10 relative z-20 gap-4">
                    <div className="hidden md:block flex-1"></div>
                    <div className="bg-white/70 backdrop-blur-md p-1.5 rounded-full shadow-lg border border-white/50 flex w-full md:w-auto overflow-x-auto justify-start md:justify-center shrink-0 custom-scrollbar">
                        {(['OPEN', 'CLOSED', 'MY'] as const).map((tab) => (
                            <button
                                key={tab}
                                onClick={() => setActiveTab(tab)}
                                className={`px-4 sm:px-8 py-2 sm:py-2.5 rounded-full text-xs sm:text-sm font-bold transition-all duration-300 whitespace-nowrapflex-1 md:flex-none text-center
                                    ${activeTab === tab
                                        ? 'bg-[var(--color-idol-dark)] text-white shadow-md transform scale-105'
                                        : 'text-[var(--color-idol-point)] hover:text-[var(--color-idol-dark)] hover:bg-[var(--color-idol-bg)]'}`}
                            >
                                {tab === 'OPEN' ? '진행 중' : tab === 'CLOSED' ? '종료됨' : '내 기록'}
                            </button>
                        ))}
                    </div>
                    <div className="flex-1 text-center md:text-right w-full md:w-auto">
                        {canCreateVote && (
                            <button
                                onClick={() => setIsCreateModalOpen(true)}
                                className="px-5 py-2.5 bg-[var(--color-idol-dark)] text-white rounded-xl shadow-lg shadow-[var(--color-idol-dark)]/30 hover:bg-[var(--color-idol)] transition-all duration-300 text-sm font-bold hover:-translate-y-1"
                            >
                                ✨ 새 투표 만들기
                            </button>
                        )}
                    </div>
                </div>

                {/* 컨텐츠 영역 */}
                <div className="relative z-20">
                    {activeTab === 'MY' ? (
                        <motion.div
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            className="bg-white/80 backdrop-blur-xl border border-[var(--color-idol-bg)] rounded-2xl shadow-xl p-4 sm:p-8 max-w-4xl mx-auto"
                        >
                            <h2 className="text-xl sm:text-2xl font-black mb-6 sm:mb-8 text-[var(--color-idol-dark)] text-center sm:text-left">내가 참여한 투표</h2>
                            {myVotes.length > 0 ? (
                                <div className="space-y-4">
                                    {myVotes.map((record, idx) => (
                                        <motion.div
                                            whileHover={{ scale: 1.01, backgroundColor: "rgba(255, 255, 255, 0.9)" }}
                                            key={idx}
                                            className="flex flex-col sm:flex-row items-start sm:items-center justify-between p-4 sm:p-5 bg-white/50 backdrop-blur-sm border border-gray-100 rounded-xl hover:border-[var(--color-idol-point)] hover:bg-[var(--color-idol-bg)]/20 hover:shadow-md transition-all cursor-pointer group gap-3 sm:gap-0"
                                            onClick={() => fetchVoteDetail(record.voteId)}
                                        >
                                            <div className="w-full sm:w-auto">
                                                <h3 className="font-bold text-gray-800 text-base sm:text-lg group-hover:text-[var(--color-idol-dark)] transition-colors">{record.voteTitle}</h3>
                                                <p className="text-xs sm:text-sm text-gray-500 mt-1">
                                                    {(() => {
                                                        const date = new Date(record.votedAt + 'Z');
                                                        if (isNaN(date.getTime())) return record.votedAt;
                                                        return date.toLocaleString('ko-KR');
                                                    })()}
                                                </p>
                                            </div>
                                            <div className="text-left sm:text-right w-full sm:w-auto bg-gray-50/50 sm:bg-transparent p-2 sm:p-0 rounded-lg sm:rounded-none">
                                                <span className="text-[10px] sm:text-xs text-[var(--color-idol-point)] font-semibold uppercase tracking-wider block sm:inline">투표한 후보: </span>
                                                <span className="font-black text-[var(--color-idol-dark)] text-base sm:text-lg ml-1 sm:ml-0">{record.candidateName}</span>
                                            </div>
                                        </motion.div>
                                    ))}
                                </div>
                            ) : (
                                <div className="text-center py-16 text-gray-400 font-medium">
                                    {user ? "아직 참여한 투표가 없습니다." : "로그인이 필요합니다."}
                                </div>
                            )}
                        </motion.div>
                    ) : (
                        <>
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                                <AnimatePresence>
                                    {votes.map((vote) => (
                                        <motion.div
                                            layout
                                            initial={{ opacity: 0, scale: 0.9 }}
                                            animate={{ opacity: 1, scale: 1 }}
                                            exit={{ opacity: 0, scale: 0.9 }}
                                            whileHover={{ y: -8, scale: 1.02 }}
                                            transition={{ type: "spring", stiffness: 300, damping: 20 }}
                                            key={vote.id}
                                            onClick={() => fetchVoteDetail(vote.id)}
                                            className="bg-white/70 backdrop-blur-md rounded-2xl shadow-xl overflow-hidden cursor-pointer border border-white border-b-[var(--color-idol-bg)] border-r-[var(--color-idol-bg)] hover:shadow-[var(--color-idol-point)]/20 transition-all relative group"
                                        >
                                            {/* Top decorative gradient bar */}
                                            <div className="h-2 w-full bg-gradient-to-r from-[var(--color-idol-point)] via-[var(--color-idol)] to-[var(--color-idol-dark)]"></div>

                                            <div className="p-7">
                                                <div className="flex justify-between items-center mb-5">
                                                    <span className={`px-4 py-1.5 rounded-full text-xs font-bold tracking-wider
                                                        ${vote.status === 'OPEN' ? 'bg-[var(--color-idol)] text-white shadow-[var(--color-idol)]/30' :
                                                            vote.status === 'UPCOMING' ? 'bg-[var(--color-idol-point)] text-white' : 'bg-gray-100 text-gray-500'}`}>
                                                        {vote.status === 'OPEN' ? '🟢 진행중' : vote.status === 'UPCOMING' ? '⏳ 예정됨' : '⚫ 종료됨'}
                                                    </span>
                                                    <span className="text-gray-400 text-sm font-medium bg-gray-50 px-3 py-1 rounded-lg">~ {(() => {
                                                        const d = new Date(vote.endDate + 'Z');
                                                        if (isNaN(d.getTime())) return vote.endDate;
                                                        return d.toLocaleDateString('ko-KR');
                                                    })()}</span>
                                                </div>
                                                <h3 className="text-2xl font-black text-gray-800 mb-3 group-hover:text-[var(--color-idol-dark)] transition-colors line-clamp-2 leading-tight">{vote.title}</h3>
                                                <p className="text-gray-500 line-clamp-2 leading-relaxed text-sm">{vote.description}</p>
                                            </div>
                                        </motion.div>
                                    ))}
                                </AnimatePresence>
                            </div>

                            {hasNext && (
                                <div className="text-center mt-12">
                                    <button
                                        onClick={handleLoadMore}
                                        className="px-8 py-3 bg-white/50 backdrop-blur-sm border border-[var(--color-idol-bg)] rounded-full text-[var(--color-idol-point)] font-bold hover:bg-white hover:shadow-lg hover:text-[var(--color-idol-dark)] transition-all duration-300"
                                    >
                                        최신 이벤트 더 보기 ↓
                                    </button>
                                </div>
                            )}

                            {votes.length === 0 && (
                                <div className="flex flex-col items-center justify-center py-32 opacity-50">
                                    <div className="text-6xl mb-4">📭</div>
                                    <div className="text-xl font-medium text-gray-500">조회 가능한 투표가 없습니다.</div>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </main>

            {/* 투표 상세 모달 (Glassmorphism & Spring Animations) */}
            <AnimatePresence>
                {selectedVote && (
                    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6"
                        onClick={() => setSelectedVote(null)}>

                        {/* Backdrop Blur */}
                        <motion.div
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                            className="absolute inset-0 bg-gray-900/40 backdrop-blur-sm"
                        ></motion.div>

                        <motion.div
                            initial={{ opacity: 0, y: 50, scale: 0.95 }}
                            animate={{ opacity: 1, y: 0, scale: 1 }}
                            exit={{ opacity: 0, y: 20, scale: 0.95 }}
                            transition={{ type: "spring", stiffness: 300, damping: 25 }}
                            onClick={(e) => e.stopPropagation()}
                            className="bg-white/90 backdrop-blur-2xl border border-white rounded-3xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-hidden flex flex-col relative z-10"
                        >
                            <div className="p-5 sm:p-8 border-b border-gray-100 bg-white/50 flex justify-between items-start">
                                <div>
                                    <h2 className="text-xl sm:text-3xl font-black text-gray-800 mb-2 leading-tight pr-4">{selectedVote.title}</h2>
                                    <div className="flex flex-col sm:flex-row gap-1 sm:gap-4 text-xs sm:text-sm font-medium text-gray-500">
                                        <span className="flex items-center gap-1">⏱ 시작: {(() => {
                                            const d = new Date(selectedVote.startDate + 'Z');
                                            if (isNaN(d.getTime())) return selectedVote.startDate;
                                            return d.toLocaleString('ko-KR');
                                        })()}</span>
                                        <span className="hidden sm:inline text-gray-300">|</span>
                                        <span className="flex items-center gap-1">마감: {(() => {
                                            const d = new Date(selectedVote.endDate + 'Z');
                                            if (isNaN(d.getTime())) return selectedVote.endDate;
                                            return d.toLocaleString('ko-KR');
                                        })()}</span>
                                    </div>
                                </div>
                                <button onClick={() => setSelectedVote(null)} className="p-2 -mr-2 sm:-mr-0 bg-gray-100 rounded-full text-gray-400 hover:bg-gray-200 hover:text-gray-700 transition shrink-0">✕</button>
                            </div>

                            <div className="p-5 sm:p-8 overflow-y-auto custom-scrollbar flex-1 bg-gradient-to-b from-transparent to-gray-50/50">
                                <p className="text-gray-600 mb-6 sm:mb-8 text-sm sm:text-lg leading-relaxed">{selectedVote.description}</p>

                                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-5">
                                    {selectedVote.candidates?.map((candidate) => {
                                        const isSelected = selectedCandidate === candidate.number;
                                        return (
                                            <motion.div
                                                layout
                                                layoutId={`candidate-${candidate.number}`}
                                                whileHover={!hasVoted ? { scale: 1.05, y: -5 } : {}}
                                                whileTap={!hasVoted ? { scale: 0.95 } : {}}
                                                key={candidate.number}
                                                onClick={() => !hasVoted && setSelectedCandidate(candidate.number)}
                                                className={`relative rounded-2xl overflow-hidden cursor-pointer transition-all duration-300
                                                    ${isSelected ? 'ring-4 ring-[var(--color-idol)] shadow-xl shadow-[var(--color-idol)]/30 transform -translate-y-2' : 'border-2 border-[var(--color-idol-bg)] hover:border-[var(--color-idol-point)]'}
                                                    ${hasVoted && !isSelected ? 'opacity-50 grayscale select-none cursor-default' : ''}
                                                    ${hasVoted && isSelected ? 'ring-4 ring-[var(--color-idol-dark)] shadow-xl shadow-[var(--color-idol-dark)]/30' : ''}`}
                                            >
                                                <div className="relative h-48">
                                                    <img src={candidate.image || "https://placehold.co/300"} alt={candidate.name} className="w-full h-full object-cover" />
                                                    {/* Image Gradient Overlay */}
                                                    <div className="absolute inset-0 bg-gradient-to-t from-gray-900/80 to-transparent"></div>

                                                    {/* Candidate Number Badge */}
                                                    <div className="absolute top-3 left-3 w-8 h-8 rounded-full bg-white/20 backdrop-blur-md flex items-center justify-center text-white font-black text-sm border border-white/30">
                                                        {candidate.number}
                                                    </div>
                                                </div>

                                                <div className="p-4 bg-white/80 backdrop-blur-md absolute bottom-0 left-0 right-0 border-t border-white/50 relative">
                                                    <p className="font-black text-gray-800 text-lg mb-3 text-center truncate">{candidate.name}</p>
                                                    <div>
                                                        <div className="w-full bg-gray-200 rounded-full h-3 mb-2 overflow-hidden shadow-inner">
                                                            <motion.div
                                                                initial={{ width: 0 }}
                                                                animate={{ width: `${Math.min(candidate.voteCount * 5, 100)}%` }}
                                                                transition={{ duration: 1, ease: "easeOut" }}
                                                                className={`h-full rounded-full ${isSelected ? 'bg-gradient-to-r from-[var(--color-idol)] to-[var(--color-idol-dark)]' : 'bg-[var(--color-idol-point)]/40'}`}
                                                            ></motion.div>
                                                        </div>
                                                        <div className={`relative flex justify-center items-center text-center font-bold text-sm gap-1 ${isSelected ? 'text-[var(--color-idol-dark)]' : 'text-gray-500'}`}>
                                                            <AnimatedNumber value={candidate.voteCount} /> 표
                                                            <AnimatePresence>
                                                                {candidate.delta != null && candidate.delta > 0 ? (
                                                                    <motion.span
                                                                        key={candidate.voteCount}
                                                                        initial={{ opacity: 0, y: 10, scale: 0.5 }}
                                                                        animate={{ opacity: 1, y: -15, scale: 1.2, rotate: [0, -10, 10, 0] }}
                                                                        exit={{ opacity: 0, y: -25, scale: 0.8 }}
                                                                        transition={{ duration: 1.2, ease: "easeOut" }}
                                                                        className="absolute left-[60%] -top-2 text-[10px] font-black text-white bg-rose-500 px-2 py-0.5 rounded-full shadow-lg pointer-events-none z-10"
                                                                    >
                                                                        +{candidate.delta}
                                                                    </motion.span>
                                                                ) : null}
                                                            </AnimatePresence>
                                                        </div>
                                                    </div>
                                                </div>

                                                {/* Fancy Checkmark for Selected Candidate */}
                                                <AnimatePresence>
                                                    {isSelected && (
                                                        <motion.div
                                                            initial={{ scale: 0, opacity: 0 }}
                                                            animate={{ scale: 1, opacity: 1, rotate: 360 }}
                                                            exit={{ scale: 0, opacity: 0 }}
                                                            className="absolute top-3 right-3 bg-[var(--color-idol-dark)] text-white rounded-full w-8 h-8 flex items-center justify-center shadow-lg border-2 border-[var(--color-idol-bg)]"
                                                        >
                                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" /></svg>
                                                        </motion.div>
                                                    )}
                                                </AnimatePresence>
                                            </motion.div>
                                        );
                                    })}
                                </div>
                            </div>

                            <div className="p-4 sm:p-6 bg-white/80 border-t flex justify-end gap-3 sm:gap-4 shadow-[0_-10px_20px_-10px_rgba(0,0,0,0.05)] relative z-20">
                                {hasVoted ? (
                                    <button
                                        onClick={handleCancelVote}
                                        disabled={isSubmitting || isFetchingDetail}
                                        className="w-full sm:w-auto px-6 sm:px-8 py-3 bg-white text-red-500 border-2 border-red-100 rounded-xl hover:bg-red-50 hover:border-red-500 disabled:opacity-50 disabled:cursor-not-allowed transition font-bold text-base sm:text-lg shadow-sm"
                                    >
                                        {isSubmitting ? '진행 중...' : '✋ 투표 물리기'}
                                    </button>
                                ) : (
                                    <button
                                        onClick={handleVote}
                                        disabled={selectedCandidate === null || selectedVote.status === 'CLOSED' || isSubmitting || isFetchingDetail}
                                        className="w-full sm:w-auto px-6 sm:px-10 py-3 bg-gradient-to-r from-[var(--color-idol)] to-[var(--color-idol-dark)] text-white rounded-xl hover:shadow-lg hover:shadow-[var(--color-idol-dark)]/40 disabled:from-gray-300 disabled:to-gray-400 disabled:shadow-none disabled:cursor-not-allowed transition-all duration-300 transform hover:-translate-y-0.5 disabled:translate-y-0 font-black text-base sm:text-lg letter-spacing-wide flex items-center justify-center gap-2"
                                    >
                                        {selectedVote.status === 'CLOSED' ? '종료된 투표' : isSubmitting ? (
                                            <>
                                                <svg className="animate-spin -ml-1 mr-2 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                                </svg> 투표 전송 중...
                                            </>
                                        ) : '✨ 확정하기'}
                                    </button>
                                )}
                            </div>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>

            {/* 투표 생성 모달 (기존 디자인에서 모서리 등 소폭 라운드 처리 보강) */}
            <AnimatePresence>
                {isCreateModalOpen && (
                    <div className="fixed inset-0 bg-gray-900/60 z-50 flex items-center justify-center p-4 backdrop-blur-md"
                        onClick={() => setIsCreateModalOpen(false)}>
                        <motion.div
                            initial={{ opacity: 0, y: 30, scale: 0.95 }}
                            animate={{ opacity: 1, y: 0, scale: 1 }}
                            exit={{ opacity: 0, y: 30, scale: 0.95 }}
                            transition={{ type: "spring", stiffness: 300, damping: 25 }}
                            onClick={(e) => e.stopPropagation()}
                            className="bg-white rounded-3xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col"
                        >
                            <div className="p-5 sm:p-8 border-b bg-[var(--color-idol-bg)]/20 flex justify-between items-center">
                                <div>
                                    <h2 className="text-xl sm:text-2xl font-black text-[var(--color-idol-dark)] tracking-tight">새 투표 등록</h2>
                                    <p className="text-xs sm:text-sm font-medium text-[var(--color-idol-point)] mt-1">
                                        {groupId ? '👑 특정 그룹 대상 투표' : '🌐 전체 유저 대상 투표'}
                                    </p>
                                </div>
                                <button onClick={() => setIsCreateModalOpen(false)} className="p-2 text-gray-400 hover:bg-gray-100 rounded-full transition">✕</button>
                            </div>

                            <div className="p-5 sm:p-8 space-y-4 sm:space-y-6 overflow-y-auto">
                                <div>
                                    <label className="block text-sm font-bold text-gray-700 mb-2">투표 주제</label>
                                    <input
                                        type="text"
                                        value={newVote.title}
                                        onChange={(e) => setNewVote({ ...newVote, title: e.target.value })}
                                        className="w-full px-5 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-[var(--color-idol-point)] focus:bg-white transition-all outline-none font-medium"
                                        placeholder="어떤 멋진 투표를 만드실 건가요?"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-bold text-gray-700 mb-2">상세 설명</label>
                                    <textarea
                                        value={newVote.description}
                                        onChange={(e) => setNewVote({ ...newVote, description: e.target.value })}
                                        className="w-full px-5 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-[var(--color-idol-point)] focus:bg-white transition-all outline-none min-h-[120px] resize-y font-medium text-sm leading-relaxed"
                                        placeholder="참여자들이 투표에 대해 알 수 있도록 상세히 적어주세요."
                                    />
                                </div>
                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-6">
                                    <div>
                                        <label className="block text-sm font-bold text-gray-700 mb-2">시작일</label>
                                        <input
                                            type="datetime-local"
                                            value={newVote.startDate}
                                            onChange={(e) => setNewVote({ ...newVote, startDate: e.target.value })}
                                            className="w-full px-5 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-[var(--color-idol-point)] outline-none font-medium"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-bold text-gray-700 mb-2">종료일</label>
                                        <input
                                            type="datetime-local"
                                            value={newVote.endDate}
                                            onChange={(e) => setNewVote({ ...newVote, endDate: e.target.value })}
                                            className="w-full px-5 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-[var(--color-idol-point)] outline-none font-medium"
                                        />
                                    </div>
                                </div>

                                <div className="pt-4 border-t">
                                    <label className="block text-lg font-black text-gray-800 mb-4 flex items-center gap-2">
                                        👑 후보 라인업
                                        <span className="text-xs font-normal text-gray-400 bg-gray-100 px-2 py-1 rounded-full">최소 2명 이상</span>
                                    </label>
                                    <div className="space-y-4">
                                        {newVote.candidates.map((candidate, idx) => (
                                            <div key={idx} className="flex flex-col sm:flex-row gap-3 sm:gap-4 items-start sm:items-center bg-gray-50 p-4 rounded-2xl border border-gray-100 group relative">
                                                <div className="flex items-center gap-2 w-full sm:w-auto">
                                                    <div className="w-8 h-8 rounded-full bg-[var(--color-idol-bg)] text-[var(--color-idol-dark)] flex items-center justify-center font-bold text-sm shadow-inner shrink-0 z-10 border border-[var(--color-idol-point)]/30">
                                                        {idx + 1}
                                                    </div>
                                                    <input
                                                        type="text"
                                                        value={candidate.name}
                                                        onChange={(e) => updateCandidate(idx, 'name', e.target.value)}
                                                        className="flex-1 sm:w-auto px-4 py-2.5 bg-white border border-gray-200 rounded-lg focus:ring-2 focus:ring-[var(--color-idol-point)] outline-none font-medium text-sm sm:text-base"
                                                        placeholder="후보자 이름"
                                                    />
                                                </div>

                                                <div className="relative shrink-0 w-full sm:w-auto">
                                                    <input
                                                        type="file"
                                                        accept="image/*"
                                                        onChange={(e) => {
                                                            if (e.target.files?.[0]) {
                                                                handleImageUpload(idx, e.target.files[0]);
                                                            }
                                                        }}
                                                        className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
                                                    />
                                                    <button className={`w-full sm:w-auto px-4 py-2.5 border rounded-lg text-sm font-bold transition flex justify-center items-center gap-2
                                                        ${candidate.image ? 'bg-green-50 text-green-600 border-green-200 hover:bg-green-100' : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-100'}`}>
                                                        {candidate.image ? '🖼 이미지 변경' : '📷 이미지 첨부'}
                                                    </button>
                                                </div>

                                                {candidate.image && (
                                                    <img src={candidate.image} alt="preview" className="w-12 h-12 rounded-lg object-cover border-2 border-white shadow-sm shrink-0 bg-white" />
                                                )}

                                                <button
                                                    onClick={() => removeCandidate(idx)}
                                                    className="absolute -top-2 -right-2 bg-white text-red-500 p-1.5 rounded-full border shadow-sm opacity-0 group-hover:opacity-100 hover:bg-red-50 hover:text-red-600 transition z-20"
                                                    title="후보 삭제"
                                                >
                                                    ✕
                                                </button>
                                            </div>
                                        ))}
                                    </div>
                                    <button
                                        onClick={addCandidate}
                                        className="mt-6 w-full py-4 border-2 border-dashed border-[var(--color-idol-point)]/50 rounded-2xl text-[var(--color-idol-point)] font-bold hover:bg-[var(--color-idol-bg)] hover:border-[var(--color-idol-point)] hover:text-[var(--color-idol-dark)] transition flex flex-col items-center gap-1"
                                    >
                                        <span className="text-xl">+</span> Add More
                                    </button>
                                </div>
                            </div>

                            <div className="p-4 sm:p-6 bg-gray-50 flex flex-col sm:flex-row justify-end gap-3 border-t">
                                <button
                                    onClick={() => setIsCreateModalOpen(false)}
                                    className="w-full sm:w-auto px-6 sm:px-8 py-3 bg-white text-gray-600 border border-gray-200 rounded-xl hover:bg-gray-100 transition font-bold"
                                >
                                    작성 취소
                                </button>
                                <button
                                    onClick={handleCreateVote}
                                    className="w-full sm:w-auto px-6 sm:px-10 py-3 bg-[var(--color-idol-dark)] text-white rounded-xl hover:bg-[var(--color-idol)] transition shadow-lg font-black tracking-wide"
                                >
                                    투표 발행하기
                                </button>
                            </div>
                        </motion.div>
                    </div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default VotePage;
