import React, {useEffect, useMemo, useState} from "react";
import {useParams, useSearchParams, useNavigate} from "react-router-dom";

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

function resolveBoardType(scope: Scope, type: BoardKind): string {
    if (type === "notice") return "ADMIN_NOTICE";
    if (scope === "idol") return type === "official" ? "IDOL_OFFICIAL" : "IDOL_FAN";
    return type === "official" ? "GROUP_OFFICIAL" : "GROUP_FAN";
}

function readMockPosts(): PostListResponse[] {
    try {
        const raw = localStorage.getItem("mock_posts");
        if (!raw) return [];
        const arr = JSON.parse(raw) as PostListResponse[];
        return Array.isArray(arr) ? arr : [];
    } catch {
        return [];
    }
}

const BoardPage: React.FC = () => {
    const {groupId} = useParams();
    const [sp, setSp] = useSearchParams();
    const navigate = useNavigate();

    const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

    const scope = (sp.get("scope") as Scope) || "group";
    const board = (sp.get("type") as BoardKind) || "official";
    const page = Number(sp.get("page") || "1");
    const size = Number(sp.get("size") || "20");
    const sort = sp.get("sort") || "latest";
    const q = sp.get("q") || "";
    const idolId = sp.get("idolId");

    const [posts, setPosts] = useState<PostListResponse[]>([]);
    const [totalPages, setTotalPages] = useState(1);
    const [totalElements, setTotalElements] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const MOCK_ALL_POSTS: PostListResponse[] = useMemo(() => {
        const total = 137;
        return Array.from({length: total}).map((_, i) => {
            const postId = i + 1;
            return {
                postId,
                boardType: "GROUP_OFFICIAL",
                idolId: null,
                groupId: 1,
                authorId: 100 + (postId % 20),
                title: `더미 게시글 제목 ${postId}`,
                viewCount: (postId * 37) % 5000,
                likeCount: (postId * 11) % 300,
                dislikeCount: (postId * 5) % 50,
                createdAt: "2026-02-10 12:00",
                updatedAt: "2026-02-10 12:00",
            };
        });
    }, []);

    useEffect(() => {
        if (USE_MOCK) {
            setLoading(false);
            setError("");

            const stored = readMockPosts();
            let list = [...stored, ...MOCK_ALL_POSTS];

            if (sort === "top") {
                list.sort((a, b) => {
                    if (b.likeCount !== a.likeCount) return b.likeCount - a.likeCount;
                    return b.postId - a.postId;
                });
            } else {
                list.sort((a, b) => b.postId - a.postId);
            }

            const total = list.length;
            const pages = Math.max(1, Math.ceil(total / size));
            const safePage = Math.min(Math.max(1, page), pages);

            const startIdx = (safePage - 1) * size;
            const sliced = list.slice(startIdx, startIdx + size);

            setTotalElements(total);
            setTotalPages(pages);
            setPosts(sliced);
            return;
        }

        if (!API_BASE_URL) return;

        const controller = new AbortController();
        const boardType = resolveBoardType(scope, board);

        const params = new URLSearchParams();
        params.set("boardType", boardType);
        params.set("page", String(page - 1));
        params.set("size", String(size));

        if (sort === "top") params.set("sort", "likeCount,desc");
        else params.set("sort", "createdAt,desc");

        if (boardType.startsWith("GROUP_") && groupId) params.set("groupId", groupId);
        if (boardType.startsWith("IDOL_") && idolId) params.set("idolId", idolId);

        const url = `${API_BASE_URL}/board/posts?${params.toString()}`;

        setLoading(true);
        setError("");

        fetch(url, {signal: controller.signal})
            .then((res) => {
                if (!res.ok) throw new Error("게시글 조회 실패");
                return res.json();
            })
            .then((data) => {
                setPosts(data.content ?? []);
                setTotalPages(data.totalPages ?? 1);
                setTotalElements(data.totalElements ?? 0);
            })
            .catch((e) => {
                if (e.name === "AbortError") return;
                setError(e.message);
                setPosts([]);
                setTotalPages(1);
                setTotalElements(0);
            })
            .finally(() => setLoading(false));

        return () => controller.abort();
    }, [API_BASE_URL, USE_MOCK, scope, board, page, size, sort, idolId, q, groupId, MOCK_ALL_POSTS]);

    const leftFilters = useMemo(() => {
        const base = [
            {label: "그룹 공식", scope: "group" as Scope, type: "official" as BoardKind},
            {label: "그룹 팬", scope: "group" as Scope, type: "fan" as BoardKind},
            {label: "공지", scope: "global" as Scope, type: "notice" as BoardKind},
        ];

        const idolExtra = [
            {label: "아이돌 공식", scope: "idol" as Scope, type: "official" as BoardKind},
            {label: "아이돌 팬", scope: "idol" as Scope, type: "fan" as BoardKind},
        ];

        return scope === "idol" ? [...idolExtra, ...base] : base;
    }, [scope]);

    const isActiveFilter = (f: { scope: Scope; type: BoardKind }) => {
        if (f.type === "notice") return board === "notice";
        return scope === f.scope && board === f.type;
    };

    const setFilter = (nextScope: Scope, nextType: BoardKind) => {
        const next = new URLSearchParams(sp);
        next.set("scope", nextScope);
        next.set("type", nextType);
        next.set("page", "1");
        setSp(next);
    };

    const setSort = (nextSort: "latest" | "top") => {
        const next = new URLSearchParams(sp);
        next.set("sort", nextSort);
        next.set("page", "1");
        setSp(next);
    };

    const setSize = (nextSize: number) => {
        const next = new URLSearchParams(sp);
        next.set("size", String(nextSize));
        next.set("page", "1");
        setSp(next);
    };

    const goPage = (p: number) => {
        const next = new URLSearchParams(sp);
        next.set("page", String(p));
        setSp(next);
    };

    const pageBlock = useMemo(() => {
        const blockSize = 5;
        const safeTotal = Math.max(1, totalPages);
        const safePage = Math.min(Math.max(1, page), safeTotal);

        const start = Math.floor((safePage - 1) / blockSize) * blockSize + 1;
        const end = Math.min(start + blockSize - 1, safeTotal);

        const nums: number[] = [];
        for (let p = start; p <= end; p++) nums.push(p);

        const prevBlock = Math.max(1, start - blockSize);
        const nextBlock = Math.min(safeTotal, start + blockSize);

        return {safePage, safeTotal, start, end, nums, prevBlock, nextBlock};
    }, [page, totalPages]);

    const rowNo = (indexInPage: number) => {
        const base = totalElements - (pageBlock.safePage - 1) * size;
        return Math.max(0, base - indexInPage);
    };

    return (
        <div className="space-y-4 relative">
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

                <div className="flex items-center gap-2">
                    <div className="flex rounded-full border border-gray-200 overflow-hidden">
                        <button
                            onClick={() => setSort("latest")}
                            className={[
                                "px-3 py-2 text-sm font-semibold",
                                sort === "latest" ? "bg-[#1FBFB8] text-white" : "bg-white text-gray-800 hover:bg-gray-200",
                            ].join(" ")}
                        >
                            최신순
                        </button>
                        <button
                            onClick={() => setSort("top")}
                            className={[
                                "px-3 py-2 text-sm font-semibold border-l border-gray-200",
                                sort === "top" ? "bg-[#1FBFB8] text-white" : "bg-white text-gray-800 hover:bg-gray-200",
                            ].join(" ")}
                        >
                            추천순
                        </button>
                    </div>

                    <select
                        value={size}
                        onChange={(e) => setSize(Number(e.target.value))}
                        className="px-3 py-2 rounded-full border border-gray-200 text-sm font-semibold bg-white"
                    >
                        <option value={20}>20개</option>
                        <option value={50}>50개</option>
                    </select>
                </div>
            </div>

            {loading && <div className="text-sm text-gray-600">불러오는 중...</div>}
            {error && <div className="text-sm text-red-600">{error}</div>}

            <div className="border border-gray-200 rounded-2xl overflow-hidden bg-white">
                <div
                    className="grid grid-cols-[90px_1fr_120px_140px_90px_90px] px-4 py-3 text-sm font-semibold text-gray-700 bg-gray-50 border-b border-gray-200">
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
                            onClick={() => navigate(`./${p.postId}`)}
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

            {/* 글쓰기 */}
            <div className="flex justify-end">
                <button
                    type="button"
                    onClick={() => navigate("./write")}
                    className="
                    px-5 py-3 rounded-2xl
                    bg-[#1FBFB8] text-white text-sm font-semibold
                    shadow-md hover:bg-[#17AFA8]
                    "
                >
                    글쓰기
                </button>
            </div>


            {/* 페이지네이션 */}
            <div className="flex items-center justify-center gap-1 pt-2">
                <button
                    type="button"
                    onClick={() => goPage(pageBlock.prevBlock)}
                    disabled={pageBlock.start === 1}
                    className="w-9 h-9 rounded-full border border-gray-200 text-sm font-semibold disabled:opacity-40"
                >
                    {"<<"}
                </button>

                <button
                    type="button"
                    onClick={() => goPage(Math.max(1, pageBlock.safePage - 1))}
                    disabled={pageBlock.safePage === 1}
                    className="w-9 h-9 rounded-full border border-gray-200 text-sm font-semibold disabled:opacity-40"
                >
                    {"<"}
                </button>

                {pageBlock.nums.map((p) => (
                    <button
                        key={p}
                        type="button"
                        onClick={() => goPage(p)}
                        className={[
                            "w-9 h-9 rounded-full border text-sm font-semibold",
                            p === pageBlock.safePage
                                ? "bg-[#1FBFB8] text-white border-[#1FBFB8]"
                                : "bg-white border-gray-200 hover:bg-gray-200",
                        ].join(" ")}
                    >
                        {p}
                    </button>
                ))}

                <button
                    type="button"
                    onClick={() => goPage(Math.min(pageBlock.safeTotal, pageBlock.safePage + 1))}
                    disabled={pageBlock.safePage === pageBlock.safeTotal}
                    className="w-9 h-9 rounded-full border border-gray-200 text-sm font-semibold disabled:opacity-40"
                >
                    {">"}
                </button>

                <button
                    type="button"
                    onClick={() => goPage(pageBlock.nextBlock)}
                    disabled={pageBlock.end === pageBlock.safeTotal}
                    className="w-9 h-9 rounded-full border border-gray-200 text-sm font-semibold disabled:opacity-40"
                >
                    {">>"}
                </button>
            </div>

            {/* 검색 (UI만) */}
            <div className="flex justify-center pt-4">
                <div className="w-full max-w-xl flex items-center border border-[#1FBFB8] rounded-sm bg-white overflow-hidden">
                    <select
                        defaultValue="title"
                        className="h-12 px-3 text-sm bg-white outline-none border-r border-blue-200"
                    >
                        <option value="title">제목</option>
                        <option value="title_content">제목+내용</option>
                        <option value="content">내용</option>
                    </select>

                    <input
                        value={q}
                        onChange={(e) => {
                            const next = new URLSearchParams(sp);
                            next.set("q", e.target.value);
                            next.set("page", "1");
                            setSp(next);
                        }}
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



        </div>
    );
};

export default BoardPage;
