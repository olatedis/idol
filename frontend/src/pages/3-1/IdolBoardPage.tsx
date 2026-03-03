import React, { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";
import { api } from "../../api/axios";

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

const PAGE_SIZE = 20;

const IdolBoardPage: React.FC = () => {
    const { idolId } = useParams();
    const navigate = useNavigate();
    const [sp, setSp] = useSearchParams();

    const { accessToken, user } = useAuthStore();

    const sort = sp.get("sort") || "latest"; // latest | top
    const q = sp.get("q") || "";

    const [posts, setPosts] = useState<PostListResponse[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError] = useState("");

    const [page, setPage] = useState(0);
    const [hasMore, setHasMore] = useState(true);
    const [totalElements, setTotalElements] = useState<number | null>(null);

    const sentinelRef = useRef<HTMLDivElement | null>(null);

    const canWrite = useMemo(() => {
        if (!user) return false;
        return user.role === "ADMIN" || user.role === "IDOL" || user.role === "AGENCY";
    }, [user]);

    const resetInfinite = () => {
        setPosts([]);
        setPage(0);
        setHasMore(true);
        setTotalElements(null);
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

    const rowNo = (idx: number) => {
        if (typeof totalElements === "number") return totalElements - idx;
        return posts.length - idx;
    };

    const fetchPage = async (nextPage: number) => {
        if (!idolId) throw new Error("idolId가 없습니다.");

        const params: any = {
            boardType: "IDOL_OFFICIAL",
            idolId: Number(idolId),
            page: nextPage,
            size: PAGE_SIZE,
            sort: sort === "top" ? "likeCount,desc" : "createdAt,desc",
        };

        // TODO: search-service 연동 시 처리
        // if (q) params.q = q;

        const res = await api.get("/board/posts", { params });
        const data = res.data as any;
        const content = (data.content ?? []) as PostListResponse[];

        if (nextPage === 0) setPosts(content);
        else setPosts((prev) => [...prev, ...content]);

        if (typeof data.totalElements === "number") setTotalElements(data.totalElements);

        const last = Boolean(data.last);
        setHasMore(!last && content.length > 0);
    };

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
    }, [idolId, sort, q]);

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
        if (!canWrite) {
            alert("권한이 없습니다.");
            return;
        }
        navigate(`./write`);
    };

    return (
        <div className="space-y-4">
            {/* 상단 툴바 */}
            <div className="flex justify-between flex-wrap gap-2">
                <div className="text-lg font-semibold text-gray-900">아이돌 공식 게시판</div>

                <div className="flex gap-2">
                    <button
                        onClick={() => setSort("latest")}
                        className={[
                            "px-3 py-2 rounded-full text-sm font-semibold border transition",
                            sort === "latest"
                                ? "bg-[#1FBFB8] text-white border-[#1FBFB8]"
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
                                ? "bg-[#1FBFB8] text-white border-[#1FBFB8]"
                                : "bg-white border-gray-200 hover:bg-gray-200 active:scale-[0.99]",
                        ].join(" ")}
                    >
                        추천순
                    </button>
                </div>
            </div>

            {/* 검색창(UI만 유지) */}
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
                        className="h-12 px-4 text-sm font-semibold text-blue-600 hover:bg-blue-50 active:scale-[0.99] transition"
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
                hover:bg-gray-50 active:bg-gray-100
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
            bg-[#1FBFB8] text-white text-sm font-semibold
            shadow-md
            hover:bg-[#17AFA8] active:scale-[0.99]
            transition
          "
                >
                    글쓰기
                </button>
            </div>
        </div>
    );
};

export default IdolBoardPage;