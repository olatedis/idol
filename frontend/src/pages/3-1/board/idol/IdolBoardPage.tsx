import React, { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
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
};

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

const IdolBoardPage: React.FC = () => {
    const { groupId, idolId } = useParams();
    const navigate = useNavigate();
    const [sp, setSp] = useSearchParams();

    const sort = sp.get("sort") || "latest";
    const q = sp.get("q") || "";

    const [posts, setPosts] = useState<PostListResponse[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);

    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [totalElements, setTotalElements] = useState<number | null>(null);

    const sentinelRef = useRef<HTMLDivElement | null>(null);

    const leftFilters = useMemo(() => {
        return [
            { label: "그룹 공식", type: "official" as BoardKind },
            { label: "그룹 팬", type: "fan" as BoardKind },
        ];
    }, []);

    const setFilter = (nextType: BoardKind) => {
        if (!groupId) return;
        navigate(`/group/${groupId}/board?type=${nextType}`);
    };

    const setSort = (nextSort: "latest" | "top") => {
        const next = new URLSearchParams(sp);
        next.set("sort", nextSort);
        setSp(next);
    };

    const setQuery = (value: string) => {
        const next = new URLSearchParams(sp);
        next.set("q", value);
        setSp(next);
    };

    const [idols, setIdols] = useState<IdolDto[]>([]);
    const [idolLoading, setIdolLoading] = useState(false);
    const [idolOpen, setIdolOpen] = useState(false);
    const idolWrapRef = useRef<HTMLDivElement | null>(null);

    useEffect(() => {
        const run = async () => {
            if (!groupId) return;
            setIdolLoading(true);
            try {
                const res = await api.get(`/groups/${groupId}/idols`);
                const list = (res.data ?? []) as IdolDto[];
                setIdols(Array.isArray(list) ? list : []);
            } finally {
                setIdolLoading(false);
            }
        };
        run();
    }, [groupId]);

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

    const idolLabel = (i: IdolDto) =>
        i.stageName || i.name || `아이돌 ${i.idolId}`;

    const selectedIdol = idols.find(
        (i) => i.idolId === Number(idolId)
    );

    const orderedIdols = useMemo(() => {
        if (!selectedIdol) return idols;
        const others = idols.filter((i) => i.idolId !== selectedIdol.idolId);
        return [selectedIdol, ...others];
    }, [idols, selectedIdol]);

    const onClickIdolBoard = (id: number) => {
        if (!groupId) return;
        setIdolOpen(false);
        navigate(`/group/${groupId}/idol/${id}/board`);
    };

    const fetchPage = async (nextPage: number) => {
        if (!idolId) return;

        const params: any = {
            boardType: "IDOL_OFFICIAL",
            idolId: Number(idolId),
            page: nextPage,
            size: PAGE_SIZE,
            sort: sort === "top" ? "likeCount,desc" : "createdAt,desc",
        };

        // search-service 연동: keyword가 있을 때만 전달 (서버 파라미터명: keyword)
        if (q && q.trim()) params.keyword = q.trim();

        const res = await api.get("/board/posts", { params });
        const data = res.data as any;
        const content = (data.content ?? []) as PostListResponse[];

        if (nextPage === 0) setPosts(content);
        else setPosts((prev) => [...prev, ...content]);

        if (typeof data.totalElements === "number")
            setTotalElements(data.totalElements);

        setHasMore(!data.last && content.length > 0);
    };

    useEffect(() => {
        const run = async () => {
            setLoading(true);
            setPosts([]);
            setPage(0);
            await fetchPage(0);
            setLoading(false);
        };
        run();
    }, [idolId, sort, q]);

    useEffect(() => {
        if (page === 0) return;
        if (!hasMore) return;
        const run = async () => {
            setLoadingMore(true);
            await fetchPage(page);
            setLoadingMore(false);
        };
        run();
    }, [page]);

    useEffect(() => {
        const el = sentinelRef.current;
        if (!el) return;
        if (!hasMore) return;

        const io = new IntersectionObserver(
            (entries) => {
                if (!entries[0].isIntersecting) return;
                if (loading || loadingMore) return;
                setPage((prev) => prev + 1);
            },
            { rootMargin: "200px" }
        );

        io.observe(el);
        return () => io.disconnect();
    }, [hasMore, loading, loadingMore]);

    const rowNo = (idx: number) =>
        typeof totalElements === "number"
            ? totalElements - idx
            : posts.length - idx;

    const onClickRow = (p: PostListResponse) => {
        navigate(`./${p.postId}`);
    };

    const onClickWrite = () => {
        navigate(`./write`);
    };

    return (
        <div className="space-y-4">
            <div className="flex justify-between flex-wrap gap-2">
                <div className="flex gap-2 flex-wrap items-center">
                    {leftFilters.map((f) => (
                        <button
                            key={f.label}
                            onClick={() => setFilter(f.type)}
                            className="px-3 py-2 rounded-full text-sm font-semibold border transition bg-white border-gray-200 hover:bg-gray-200"
                        >
                            {f.label}
                        </button>
                    ))}

                    <div ref={idolWrapRef} className="relative">
                        <button
                            type="button"
                            onClick={() => setIdolOpen((v) => !v)}
                            className="px-3 py-2 rounded-full text-sm font-semibold border transition flex items-center gap-2 bg-[#1FBFB8] text-white border-[#1FBFB8]"
                        >
                            {selectedIdol
                                ? idolLabel(selectedIdol)
                                : "아이돌 게시판"}
                            <span
                                className={[
                                    "transition-transform",
                                    idolOpen ? "rotate-180" : "",
                                ].join(" ")}
                            >
                                ▾
                            </span>
                        </button>

                        {idolOpen && (
                            <div className="absolute left-0 mt-2 w-56 rounded-2xl border border-gray-200 bg-white shadow-lg overflow-hidden z-50">
                                {idolLoading ? (
                                    <div className="px-4 py-3 text-sm text-gray-600">
                                        불러오는 중...
                                    </div>
                                ) : orderedIdols.length === 0 ? (
                                    <div className="px-4 py-3 text-sm text-gray-600">
                                        소속 아이돌이 없습니다.
                                    </div>
                                ) : (
                                    <div className="max-h-72 overflow-auto">
                                        {orderedIdols.map((i) => (
                                            <button
                                                key={i.idolId}
                                                type="button"
                                                onClick={() =>
                                                    onClickIdolBoard(i.idolId)
                                                }
                                                className="w-full text-left px-4 py-3 text-sm hover:bg-gray-50 transition"
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
                                ? "bg-[#1FBFB8] text-white border-[#1FBFB8]"
                                : "bg-white border-gray-200 hover:bg-gray-200",
                        ].join(" ")}
                    >
                        최신순
                    </button>

                    <button
                        onClick={() => setSort("top")}
                        className={[
                            "px-3 py-2 rounded-full text-sm font-semibold border transition",
                            sort === "top"
                                ? "bg-[#1FBFB8] text-white border-[#1FBFB8]"
                                : "bg-white border-gray-200 hover:bg-gray-200",
                        ].join(" ")}
                    >
                        추천순
                    </button>
                </div>
            </div>

            <div className="flex justify-center">
                <div className="w-full max-w-xl flex items-center border border-blue-400 rounded-sm bg-white overflow-hidden">

                    <input
                        value={q}
                        onChange={(e) => setQuery(e.target.value)}
                        placeholder="단어 위주로 검색하시면 보다 정확한 결과를 얻을 수 있습니다."
                        className="flex-1 h-12 px-4 text-sm outline-none"
                    />

                    <button
                        type="button"
                        className="h-12 px-4 text-sm font-semibold text-blue-600 hover:bg-blue-50 active:bg-blue-100 transition"
                    >
                        🔍
                    </button>
                </div>
            </div>

            <div className="border border-gray-200 rounded-2xl overflow-hidden bg-white">
                <div className="grid grid-cols-[90px_1fr_120px_140px_90px_90px] px-4 py-3 text-sm font-semibold text-gray-700 bg-gray-50 border-b border-gray-200">
                    <div>번호</div>
                    <div>제목</div>
                    <div>작성자</div>
                    <div>작성일</div>
                    <div className="text-right">조회수</div>
                    <div className="text-right">좋아요</div>
                </div>

                {!loading &&
                    posts.map((p, idx) => (
                        <button
                            key={p.postId}
                            onClick={() => onClickRow(p)}
                            className="w-full text-left grid grid-cols-[90px_1fr_120px_140px_90px_90px] px-4 py-3 border-b border-gray-100 last:border-b-0 hover:bg-gray-50 active:bg-gray-100 transition-colors"
                        >
                            <div className="text-sm tabular-nums">
                                {rowNo(idx)}
                            </div>
                            <div className="min-w-0">
                                <div className="text-sm font-semibold truncate">
                                    {p.title}
                                    {Number((p as any).commentCount) > 0 &&(
                                        <span className="ml-3 text-gray-500 text-sm font-normal">
                                            [ {Number((p as any).commentCount)} ]
                                        </span>
                                    )}
                                </div>
                            </div>
                            <div className="text-sm tabular-nums">
                                {p.authorId}
                            </div>

                            {/* 작성일은 KST 기준으로 표시 */}
                            <div className="text-sm">
                                {formatDateToKST(p.createdAt)}
                            </div>

                            <div className="text-sm text-right tabular-nums">
                                {p.viewCount}
                            </div>
                            <div className="text-sm text-right tabular-nums">
                                {p.likeCount}
                            </div>
                        </button>
                    ))}
            </div>

            <div ref={sentinelRef} className="h-10" />

            {loadingMore && (
                <div className="text-sm text-gray-600">더 불러오는 중...</div>
            )}

            {!loading && !loadingMore && posts.length > 0 && !hasMore && (
                <div className="text-sm text-gray-500 text-center py-2">
                    마지막 게시글입니다.
                </div>
            )}

            <div className="fixed right-4 bottom-6 z-40 flex flex-col items-end gap-3">
                <button
                    onClick={() =>
                        window.scrollTo({ top: 0, behavior: "smooth" })
                    }
                    className="w-12 h-12 rounded-full bg-gray-100 border border-gray-200 shadow-md"
                >
                    ↑
                </button>

                <button
                    onClick={onClickWrite}
                    className="px-5 py-3 rounded-2xl bg-[#1FBFB8] text-white text-sm font-semibold shadow-md"
                >
                    글쓰기
                </button>
            </div>
        </div>
    );
};

export default IdolBoardPage;