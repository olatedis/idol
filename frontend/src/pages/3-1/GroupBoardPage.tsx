import React, { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";

// 타입 (백엔드 스펙 기준)
type Scope = "group" | "idol" | "global";
type BoardKind = "official" | "fan" | "notice";

type PostListResponse = {
    postId: number;
    boardType: string;
    idolId?: number | null;
    groupId?: number | null;

    authorId: number;
    title: string;

    viewCount: number;
    likeCount: number;
    dislikeCount: number;

    createdAt: string;
    updatedAt: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

// 게시판 scope/type → boardType 변환
function resolveBoardType(scope: Scope, type: BoardKind): string {
    if (type === "notice") return "ADMIN_NOTICE";
    if (scope === "idol") return type === "official" ? "IDOL_OFFICIAL" : "IDOL_FAN";
    return type === "official" ? "GROUP_OFFICIAL" : "GROUP_FAN";
}

const PAGE_SIZE = 20;

const GroupBoardPage: React.FC = () => {
    const { groupId } = useParams();
    const [sp, setSp] = useSearchParams();
    const navigate = useNavigate();

    // URL 상태 (필터/정렬/검색만 유지)
    const scope = (sp.get("scope") as Scope) || "group";
    const board = (sp.get("type") as BoardKind) || "official";
    const sort = sp.get("sort") || "latest"; // latest | top
    const q = sp.get("q") || "";
    const idolId = sp.get("idolId");

    // 게시글 상태
    const [posts, setPosts] = useState<PostListResponse[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError] = useState("");

    // 무한 스크롤 상태
    const [page, setPage] = useState(0); // 0-based
    const [hasMore, setHasMore] = useState(true);
    const [totalElements, setTotalElements] = useState<number | null>(null);

    const sentinelRef = useRef<HTMLDivElement | null>(null);

    // TODO: 로그인 연동되면 accessToken 저장 방식/키 확정
    const accessToken = localStorage.getItem("accessToken");

    // 필터 버튼
    const leftFilters = useMemo(() => {
        const base = [
            { label: "그룹 공식", scope: "group" as Scope, type: "official" as BoardKind },
            { label: "그룹 팬", scope: "group" as Scope, type: "fan" as BoardKind },
            { label: "공지", scope: "global" as Scope, type: "notice" as BoardKind },
        ];

        const idolExtra = [
            { label: "아이돌 공식", scope: "idol" as Scope, type: "official" as BoardKind },
            { label: "아이돌 팬", scope: "idol" as Scope, type: "fan" as BoardKind },
        ];

        return scope === "idol" ? [...idolExtra, ...base] : base;
    }, [scope]);

    const isActiveFilter = (f: { scope: Scope; type: BoardKind }) => {
        if (f.type === "notice") return board === "notice";
        return scope === f.scope && board === f.type;
    };

    const resetInfinite = () => {
        setPosts([]);
        setPage(0);
        setHasMore(true);
        setTotalElements(null);
    };

    const setFilter = (nextScope: Scope, nextType: BoardKind) => {
        const next = new URLSearchParams(sp);
        next.set("scope", nextScope);
        next.set("type", nextType);
        setSp(next);
        resetInfinite();
    };

    const setSort = (nextSort: "latest" | "top") => {
        const next = new URLSearchParams(sp);
        next.set("sort", nextSort);
        setSp(next);
        resetInfinite();
    };

    const setQuery = (value: string) => {
        const next = new URLSearchParams(sp);
        next.set("q", value);
        setSp(next);
        resetInfinite();
    };

    // 무한스크롤용 번호 계산
    const rowNo = (idx: number) => {
        if (typeof totalElements === "number") {
            return totalElements - idx;
        }
        return posts.length - idx;
    };

    // page 단위 fetch (append)
    const fetchPage = async (nextPage: number, signal?: AbortSignal) => {
        const boardType = resolveBoardType(scope, board);

        if (!API_BASE_URL) return;

        const params = new URLSearchParams();
        params.set("boardType", boardType);
        params.set("page", String(nextPage));
        params.set("size", String(PAGE_SIZE));

        if (sort === "top") params.set("sort", "likeCount,desc");
        else params.set("sort", "createdAt,desc");

        if (boardType.startsWith("GROUP_") && groupId) params.set("groupId", groupId);
        if (boardType.startsWith("IDOL_") && idolId) params.set("idolId", idolId);

        // TODO: search-service 연동 시 처리
        // if (q) params.set("q", q);

        const url = `${API_BASE_URL}/board/posts?${params.toString()}`;

        const res = await fetch(url, { signal });
        if (!res.ok) throw new Error("게시글 조회 실패");

        const data = await res.json();
        const content = (data.content ?? []) as PostListResponse[];

        if (nextPage === 0) {
            setPosts(content);
        } else {
            setPosts((prev) => [...prev, ...content]);
        }

        if (typeof data.totalElements === "number") {
            setTotalElements(data.totalElements);
        }

        const last = Boolean(data.last);
        setHasMore(!last && content.length > 0);
    };

    // 첫 페이지 로드(필터/정렬/검색 변경 시 0페이지부터 다시)
    useEffect(() => {
        const controller = new AbortController();

        const run = async () => {
            setError("");

            try {
                setLoading(true);
                setLoadingMore(false);

                resetInfinite();
                await fetchPage(0, controller.signal);
            } catch (e: any) {
                if (e?.name === "AbortError") return;
                setError(e?.message || "게시글 조회 실패");
                setPosts([]);
                setHasMore(false);
            } finally {
                setLoading(false);
            }
        };

        run();
        return () => controller.abort();
    }, [API_BASE_URL, scope, board, sort, idolId, groupId, q]);

    // page가 증가하면 다음 페이지 append 로드
    useEffect(() => {
        if (page === 0) return;
        if (!hasMore) return;

        const controller = new AbortController();

        const run = async () => {
            setError("");
            try {
                setLoadingMore(true);
                await fetchPage(page, controller.signal);
            } catch (e: any) {
                if (e?.name === "AbortError") return;
                setError(e?.message || "추가 로딩 실패");
                setHasMore(false);
            } finally {
                setLoadingMore(false);
            }
        };

        run();
        return () => controller.abort();
    }, [page, hasMore]);

    // IntersectionObserver로 page 증가 트리거
    useEffect(() => {
        const el = sentinelRef.current;
        if (!el) return;
        if (!hasMore) return;

        const io = new IntersectionObserver(
            (entries) => {
                const first = entries[0];
                if (!first.isIntersecting) return;
                if (loading) return;
                if (loadingMore) return;

                setPage((prev) => prev + 1);
            },
            { root: null, rootMargin: "200px", threshold: 0 }
        );

        io.observe(el);
        return () => io.disconnect();
    }, [hasMore, loading, loadingMore]);

    const scrollTop = () => {
        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    // 토큰 없으면 이동 차단
    const requireLoginOrStop = () => {
        if (accessToken) return true;

        alert("로그인이 필요합니다.");
        // TODO: 로그인 페이지로 이동
        // navigate("/login");
        return false;
    };

    const onClickRow = (p: PostListResponse) => {
        if (!requireLoginOrStop()) return;
        navigate(`./${p.postId}`);
    };

    const onClickWrite = () => {
        if (!requireLoginOrStop()) return;
        navigate("./write");
    };

    return (
        <div className="space-y-4">
            {/* 상단 툴바 */}
            <div className="flex justify-between flex-wrap gap-2">
                <div className="flex gap-2 flex-wrap">
                    {leftFilters.map((f) => (
                        <button
                            key={f.label}
                            onClick={() => setFilter(f.scope, f.type)}
                            className={[
                                "px-3 py-2 rounded-full text-sm font-semibold border",
                                isActiveFilter(f)
                                    ? "bg-[#1FBFB8] text-white border-[#1FBFB8]"
                                    : "bg-white text-gray-800 border-gray-200 hover:bg-gray-200",
                            ].join(" ")}
                        >
                            {f.label}
                        </button>
                    ))}
                </div>

                <div className="flex gap-2">
                    <button
                        onClick={() => setSort("latest")}
                        className={[
                            "px-3 py-2 rounded-full text-sm font-semibold border",
                            sort === "latest"
                                ? "bg-[#1FBFB8] text-white border-[#1FBFB8]"
                                : "bg-white border-gray-200 hover:bg-gray-200",
                        ].join(" ")}
                    >
                        최신순
                    </button>

                    <button
                        onClick={() => setSort("top")}
                        className={[
                            "px-3 py-2 rounded-full text-sm font-semibold border",
                            sort === "top"
                                ? "bg-[#1FBFB8] text-white border-[#1FBFB8]"
                                : "bg-white border-gray-200 hover:bg-gray-200",
                        ].join(" ")}
                    >
                        추천순
                    </button>
                </div>
            </div>

            {/* 검색창 */}
            <div className="flex justify-center">
                <div className="w-full max-w-xl flex items-center border border-blue-400 rounded-sm bg-white overflow-hidden">
                    <select defaultValue="title" className="h-12 px-3 text-sm bg-white outline-none border-r border-blue-200">
                        <option value="title">제목</option>
                        <option value="title_content">제목+내용</option>
                        <option value="content">내용</option>
                    </select>

                    <input
                        value={q}
                        onChange={(e) => setQuery(e.target.value)}
                        placeholder="단어 위주로 검색하시면 보다 정확한 결과를 얻을 수 있습니다."
                        className="flex-1 h-12 px-4 text-sm outline-none"
                    />

                    <button
                        type="button"
                        className="h-12 px-4 text-sm font-semibold text-blue-600 hover:bg-blue-50"
                        onClick={() => {
                            // TODO: search-service 연동 시 검색 실행
                        }}
                    >
                        🔍
                    </button>
                </div>
            </div>

            {loading && <div className="text-sm text-gray-600">불러오는 중...</div>}
            {error && <div className="text-sm text-red-600">{error}</div>}

            {/* 게시글 리스트 */}
            <div className="border border-gray-200 rounded-2xl overflow-hidden bg-white">
                <div className="grid grid-cols-[90px_1fr_120px_140px_90px_90px] px-4 py-3 text-sm font-semibold text-gray-700 bg-gray-50 border-b border-gray-200">
                    <div className="text-left">번호</div>
                    <div className="text-left">제목</div>
                    <div className="text-left">작성자</div>
                    <div className="text-left">작성일</div>
                    <div className="text-right">조회수</div>
                    <div className="text-right">좋아요</div>
                </div>

                {!loading && posts.length === 0 && <div className="px-4 py-6 text-sm text-gray-600">게시글이 없습니다.</div>}

                {!loading &&
                    posts.map((p, idx) => (
                        <button
                            key={p.postId}
                            type="button"
                            onClick={() => onClickRow(p)}
                            className="
                                w-full text-left
                                grid grid-cols-[90px_1fr_120px_140px_90px_90px]
                                px-4 py-3
                                border-b border-gray-100 last:border-b-0
                                hover:bg-gray-50
                                transition-colors
                            "
                        >
                            <div className="text-sm text-gray-900 tabular-nums">{rowNo(idx)}</div>

                            <div className="min-w-0">
                                <div className="text-sm font-semibold text-gray-900 truncate">{p.title}</div>
                            </div>

                            <div className="text-sm text-gray-700 tabular-nums">{p.authorId}</div>

                            <div className="text-sm text-gray-600">{p.createdAt}</div>

                            <div className="text-sm text-gray-700 text-right tabular-nums">{p.viewCount}</div>

                            <div className="text-sm text-gray-700 text-right tabular-nums">{p.likeCount}</div>
                        </button>
                    ))}
            </div>

            {/* 무한스크롤 sentinel */}
            <div ref={sentinelRef} className="h-10" />

            {loadingMore && <div className="text-sm text-gray-600">더 불러오는 중...</div>}
            {!loading && !loadingMore && posts.length > 0 && !hasMore && (
                <div className="text-sm text-gray-500 text-center py-2">마지막 게시글입니다.</div>
            )}

            {/* 플로팅 버튼 */}
            <div className="fixed right-4 bottom-6 z-40 flex flex-col items-end gap-3">
                <button
                    type="button"
                    onClick={scrollTop}
                    className="
                        w-12 h-12 rounded-full
                        bg-gray-100 border border-gray-200
                        shadow-md
                        text-gray-800 font-semibold
                        hover:bg-gray-200
                    "
                >
                    ↑
                </button>

                <button
                    type="button"
                    onClick={onClickWrite}
                    className="
                        px-5 py-3 rounded-2xl
                        bg-[#1FBFB8] text-white text-sm font-semibold
                        shadow-md
                        hover:bg-[#17AFA8]
                    "
                >
                    글쓰기
                </button>
            </div>
        </div>
    );
};

export default GroupBoardPage;