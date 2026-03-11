import React, { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useAuthStore } from "../../../../stores/authStore.ts";
import { api } from "../../../../api/axios.ts";

type BoardKind = "official" | "fan";

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

type IdolDto = {
    idolId: number;
    name?: string | null;
    stageName?: string | null;
    imgUrl?: string | null;
};

function resolveBoardType(type: BoardKind): string {
    return type === "official" ? "GROUP_OFFICIAL" : "GROUP_FAN";
}

const PAGE_SIZE = 20;

// 날짜 문자열을 KST 기준으로 표시하기 위한 헬퍼 함수
const formatDateToKST = (dateString: string) => {
    if (!dateString) return "";

    // 백엔드는 'YYYY-MM-DD HH:mm:ss' (UTC/GMT) 형태로 문자열을 전달한다고 가정
    // JS Date 객체로 파싱 시 UTC로 인식시키기 위해 뒤에 'Z'를 추가
    const utcDate = new Date(`${dateString.replace(" ", "T")}Z`);

    if (isNaN(utcDate.getTime())) return dateString;

    // KST는 UTC+9
    const kstDate = new Date(utcDate.getTime() + 9 * 60 * 60 * 1000);

    const yy = String(kstDate.getUTCFullYear()).slice(2);
    const mm = String(kstDate.getUTCMonth() + 1).padStart(2, "0");
    const dd = String(kstDate.getUTCDate()).padStart(2, "0");
    const hh = String(kstDate.getUTCHours()).padStart(2, "0");
    const min = String(kstDate.getUTCMinutes()).padStart(2, "0");

    return `${yy}.${mm}.${dd} ${hh}:${min}`;
};

const GroupBoardPage: React.FC = () => {
    const { groupId } = useParams();
    const [sp, setSp] = useSearchParams();
    const navigate = useNavigate();

    const { accessToken } = useAuthStore();

    // URL 상태 (필터/정렬/검색만 유지)
    const board = (sp.get("type") as BoardKind) || "official";
    const sort = sp.get("sort") || "latest"; // latest | top
    const q = sp.get("q") || ""; // 확정 검색어(버튼/엔터로만 변경)

    // 입력창 상태 (타이핑은 여기만 변경)
    const [inputQ, setInputQ] = useState(q);

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

    // 아이돌 드롭다운
    const [idols, setIdols] = useState<IdolDto[]>([]);
    const [idolLoading, setIdolLoading] = useState(false);
    const [idolOpen, setIdolOpen] = useState(false);
    const idolWrapRef = useRef<HTMLDivElement | null>(null);

    // 필터 버튼
    const leftFilters = useMemo(() => {
        return [
            { label: "그룹 공식", type: "official" as BoardKind },
            { label: "그룹 팬", type: "fan" as BoardKind },
        ];
    }, []);

    const isActiveFilter = (f: { type: BoardKind }) => board === f.type;

    const resetInfinite = () => {
        setPosts([]);
        setPage(0);
        setHasMore(true);
        setTotalElements(null);
    };

    const setFilter = (nextType: BoardKind) => {
        const next = new URLSearchParams(sp);
        next.set("type", nextType);

        next.delete("scope");
        next.delete("idolId");

        setSp(next);
        resetInfinite();
    };

    const setSort = (nextSort: "latest" | "top") => {
        const next = new URLSearchParams(sp);
        next.set("sort", nextSort);
        setSp(next);
        resetInfinite();
    };

    // 검색 실행(버튼/엔터 전용): URL의 q를 갱신하고 무한스크롤 리셋
    const applySearch = () => {
        const next = new URLSearchParams(sp);

        const trimmed = inputQ.trim();
        if (trimmed) next.set("q", trimmed);
        else next.delete("q");

        setSp(next);
        resetInfinite();
    };

    // 무한스크롤용 번호 계산
    const rowNo = (idx: number) => {
        if (typeof totalElements === "number") return totalElements - idx;
        return posts.length - idx;
    };

    const fetchPage = async (nextPage: number) => {
        const boardType = resolveBoardType(board);

        const params: any = {
            boardType,
            page: nextPage,
            size: PAGE_SIZE,
            sort: sort === "top" ? "likeCount,desc" : "createdAt,desc",
        };

        if (boardType.startsWith("GROUP_") && groupId) params.groupId = groupId;

        // search-service 연동: keyword가 있을 때만 전달 (서버 파라미터명: keyword)
        if (q && q.trim()) params.keyword = q.trim();

        const res = await api.get("/board/posts", { params });
        const data = res.data as any;
        const content = (data.content ?? []) as PostListResponse[];

        if (nextPage === 0) setPosts(content);
        else setPosts((prev) => [...prev, ...content]);

        if (typeof data.totalElements === "number") setTotalElements(data.totalElements);

        const last = Boolean(data.last);
        setHasMore(!last && content.length > 0);
    };

    // URL q가 바뀌면 입력창도 동기화 (뒤로가기/링크 공유 대응)
    useEffect(() => {
        setInputQ(q);
    }, [q]);

    //  그룹 소속 아이돌 목록 로드
    useEffect(() => {
        const run = async () => {
            if (!groupId) {
                setIdols([]);
                return;
            }

            setIdolLoading(true);
            try {
                const res = await api.get(`/groups/${groupId}/idols`);
                const list = (res.data ?? []) as IdolDto[];
                setIdols(Array.isArray(list) ? list : []);
            } catch {
                setIdols([]);
            } finally {
                setIdolLoading(false);
            }
        };

        run();
    }, [groupId]);

    // 첫 페이지 로드 (board/sort/groupId/q 변경 시)
    useEffect(() => {
        const run = async () => {
            setError("");
            try {
                setLoading(true);
                setLoadingMore(false);

                resetInfinite();
                await fetchPage(0);
            } catch (e: any) {
                const msg = e?.response?.data?.message || e?.message || "게시글 조회 실패";
                setError(msg);
                setPosts([]);
                setHasMore(false);
            } finally {
                setLoading(false);
            }
        };

        run();
    }, [board, sort, groupId, q]);

    // page가 증가하면 다음 페이지 append 로드
    useEffect(() => {
        if (page === 0) return;
        if (!hasMore) return;

        const run = async () => {
            setError("");
            try {
                setLoadingMore(true);
                await fetchPage(page);
            } catch (e: any) {
                const msg = e?.response?.data?.message || e?.message || "추가 로딩 실패";
                setError(msg);
                setHasMore(false);
            } finally {
                setLoadingMore(false);
            }
        };

        run();
    }, [page, hasMore]);

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

    useEffect(() => {
        const onDocDown = (e: MouseEvent) => {
            if (!idolOpen) return;
            const wrap = idolWrapRef.current;
            if (!wrap) return;
            if (wrap.contains(e.target as Node)) return;
            setIdolOpen(false);
        };

        document.addEventListener("mousedown", onDocDown);
        return () => document.removeEventListener("mousedown", onDocDown);
    }, [idolOpen]);

    const scrollTop = () => window.scrollTo({ top: 0, behavior: "smooth" });

    const requireLoginOrStop = () => {
        if (accessToken) return true;
        alert("로그인이 필요합니다.");
        return false;
    };

    const onClickRow = (p: PostListResponse) => {
        if (!requireLoginOrStop()) return;
        navigate(`./${p.postId}`);
    };

    const onClickWrite = () => {
        if (!requireLoginOrStop()) return;
        navigate(`./write?type=${board}`);
    };

    const onClickIdolBoard = (idolId: number) => {
        if (!requireLoginOrStop()) return;
        if (!groupId) return;
        setIdolOpen(false);
        navigate(`/group/${groupId}/idol/${idolId}/board`);
    };

    const idolLabel = (i: IdolDto) => {
        return (
            (i.stageName && String(i.stageName)) ||
            (i.name && String(i.name)) ||
            `아이돌 ${i.idolId}`
        );
    };

    return (
        <div className="space-y-4">
            {/* 상단 툴바 */}
            <div className="flex justify-between flex-wrap gap-2">
                <div className="flex gap-2 flex-wrap items-center">
                    {leftFilters.map((f) => (
                        <button
                            key={f.label}
                            onClick={() => setFilter(f.type)}
                            className={[
                                "px-3 py-2 rounded-full text-sm font-semibold border transition",
                                isActiveFilter(f)
                                    ? "bg-gradient-to-r from-[var(--color-idol)] to-[var(--color-idol-dark)] text-white border-transparent shadow-md shadow-[var(--color-idol-point)]/20 hover:brightness-90"
                                    : "bg-white text-gray-800 border-gray-200 hover:bg-gray-200 active:scale-[0.99]",
                            ].join(" ")}
                        >
                            {f.label}
                        </button>
                    ))}

                    {/* 아이돌 드롭다운 */}
                    <div ref={idolWrapRef} className="relative">
                        <button
                            type="button"
                            onClick={() => setIdolOpen((v) => !v)}
                            className={[
                                "px-3 py-2 rounded-full text-sm font-semibold border transition flex items-center gap-2",
                                "bg-white text-gray-800 border-gray-200 hover:bg-gray-200 active:scale-[0.99]",
                            ].join(" ")}
                        >
                            아이돌 게시판
                            <span className={["transition-transform", idolOpen ? "rotate-180" : ""].join(" ")}>▾</span>
                        </button>

                        {idolOpen && (
                            <div
                                className="
                                absolute left-0 mt-2 w-56
                                rounded-2xl border border-[var(--color-idol)]/20 bg-white shadow-lg overflow-hidden
                                z-50
                            "
                            >
                                {idolLoading ? (
                                    <div className="px-4 py-3 text-sm text-gray-600">불러오는 중...</div>
                                ) : idols.length === 0 ? (
                                    <div className="px-4 py-3 text-sm text-gray-600">소속 아이돌이 없습니다.</div>
                                ) : (
                                    <div className="max-h-72 overflow-auto">
                                        {idols.map((i) => (
                                            <button
                                                key={i.idolId}
                                                type="button"
                                                onClick={() => onClickIdolBoard(i.idolId)}
                                                className="
                                                w-full text-left px-4 py-3 text-sm
                                                hover:bg-gray-50 active:bg-gray-100
                                                transition
                                            "
                                            >
                                                {idolLabel(i)}
                                            </button>
                                        ))}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>

                <div className="flex gap-2">
                    <button
                        onClick={() => setSort("latest")}
                        className={[
                            "px-3 py-2 rounded-full text-sm font-semibold border transition",
                            sort === "latest"
                                ? "bg-gradient-to-r from-[var(--color-idol)] to-[var(--color-idol-dark)] text-white border-transparent shadow-md shadow-[var(--color-idol-point)]/20 hover:brightness-90"
                                : "bg-white border-gray-200 hover:bg-gray-200 active:scale-[0.99]",
                        ].join(" ")}
                    >
                        최신순
                    </button>

                    <button
                        onClick={() => setSort("top")}
                        className={[
                            "px-3 py-2 rounded-full text-sm font-semibold border transition",
                            sort === "top"
                                ? "bg-gradient-to-r from-[var(--color-idol)] to-[var(--color-idol-dark)] text-white border-transparent shadow-md shadow-[var(--color-idol-point)]/20 hover:brightness-90"
                                : "bg-white border-gray-200 hover:bg-gray-200 active:scale-[0.99]",
                        ].join(" ")}
                    >
                        추천순
                    </button>
                </div>
            </div>

            {/* 검색창 */}
            <div className="flex justify-center">
                <div
                    className="
                    w-full max-w-xl flex items-center
                    rounded-2xl bg-white overflow-hidden
                    border border-[var(--color-idol)]/25
                    shadow-sm
                    focus-within:border-[var(--color-idol)]/60
                    focus-within:shadow-[0_0_0_4px_rgba(255,146,146,0.12)]
                    transition
                "
                >
                    <input
                        value={inputQ}
                        onChange={(e) => setInputQ(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key !== "Enter") return;
                            applySearch();
                        }}
                        placeholder="단어 위주로 검색하시면 보다 정확한 결과를 얻을 수 있습니다."
                        className="
                        flex-1 h-12 px-4 text-sm
                        bg-[var(--color-idol-bg)]/20 text-gray-700
                        placeholder:text-gray-400
                        outline-none
                    "
                    />

                    <button
                        type="button"
                        className="
                        h-12 px-4 text-sm font-semibold
                        text-[var(--color-idol-point)]
                        hover:bg-[var(--color-idol-bg)]
                        hover:text-[var(--color-idol-dark)]
                        active:bg-[var(--color-idol-bg)]
                        transition
                    "
                        onClick={() => {
                            applySearch();
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

                {!loading && posts.length === 0 && (
                    <div className="px-4 py-6 text-sm text-gray-600">게시글이 없습니다.</div>
                )}

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
                            hover:bg-[var(--color-idol-bg)]/35 active:bg-[var(--color-idol-bg)]/60
                            transition-colors
                        "
                        >
                            <div className="text-sm text-gray-900 tabular-nums">{rowNo(idx)}</div>

                            <div className="min-w-0">
                                <div className="text-sm font-semibold text-gray-900 truncate">
                                    {p.title}
                                    {Number((p as any).commentCount) > 0 && (
                                        <span className="ml-3 text-[var(--color-idol-dark)]/80 text-sm font-normal">
                                        [ {Number((p as any).commentCount)} ]
                                    </span>
                                    )}
                                </div>
                            </div>

                            <div className="text-sm text-gray-700 tabular-nums">{p.authorId}</div>

                            <div className="text-sm text-gray-600">{formatDateToKST(p.createdAt)}</div>

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
                    hover:bg-gray-200 active:scale-[0.99]
                    transition
                "
                >
                    ↑
                </button>

                <button
                    type="button"
                    onClick={onClickWrite}
                    className="
                    px-5 py-3 rounded-2xl
                    bg-gradient-to-r from-[var(--color-idol)] to-[var(--color-idol-dark)]
                    text-white text-sm font-semibold
                    shadow-md shadow-[var(--color-idol-point)]/20
                    hover:brightness-90 active:scale-[0.99]
                    transition
                "
                >
                    글쓰기
                </button>
            </div>
        </div>
    );
};

export default GroupBoardPage;