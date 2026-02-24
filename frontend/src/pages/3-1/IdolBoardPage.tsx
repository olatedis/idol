import React, { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

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

const PAGE_SIZE = 20;

const IdolBoardPage: React.FC = () => {
    const { idolId } = useParams();
    const navigate = useNavigate();

    const [posts, setPosts] = useState<PostListResponse[]>([]);
    const [loading, setLoading] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);
    const [error, setError] = useState("");

    const [page, setPage] = useState(0); // 0-based
    const [hasMore, setHasMore] = useState(true);
    const [totalElements, setTotalElements] = useState<number | null>(null);

    const sentinelRef = useRef<HTMLDivElement | null>(null);

    // TODO: 로그인 연동되면 accessToken 저장 방식/키 확정
    const accessToken = localStorage.getItem("accessToken");

    const resetInfinite = () => {
        setPosts([]);
        setPage(0);
        setHasMore(true);
        setTotalElements(null);
    };

    const rowNo = (idx: number) => {
        if (typeof totalElements === "number") {
            return totalElements - idx;
        }
        return posts.length - idx;
    };

    const fetchPage = async (nextPage: number, signal?: AbortSignal) => {
        if (!API_BASE_URL) return;
        if (!idolId) throw new Error("idolId가 없습니다.");

        const params = new URLSearchParams();
        params.set("boardType", "IDOL_OFFICIAL"); // [CHANGED] 아이돌은 공식만
        params.set("idolId", idolId);
        params.set("page", String(nextPage));
        params.set("size", String(PAGE_SIZE));
        params.set("sort", "createdAt,desc");

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
    }, [API_BASE_URL, idolId]);

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
            <div className="flex justify-between items-center flex-wrap gap-2">
                <div className="text-lg font-semibold text-gray-900">아이돌 공식 게시판</div>

                <div className="flex gap-2">
                    <button
                        type="button"
                        onClick={scrollTop}
                        className="px-3 py-2 rounded-full text-sm font-semibold border border-gray-200 hover:bg-gray-50"
                    >
                        ↑
                    </button>

                    <button
                        type="button"
                        onClick={onClickWrite}
                        className="px-4 py-2 rounded-full bg-[#1FBFB8] text-white text-sm font-semibold hover:bg-[#17AFA8]"
                    >
                        글쓰기
                    </button>
                </div>
            </div>

            {loading && <div className="text-sm text-gray-600">불러오는 중...</div>}
            {error && <div className="text-sm text-red-600">{error}</div>}

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

            <div ref={sentinelRef} className="h-10" />
            {loadingMore && <div className="text-sm text-gray-600">더 불러오는 중...</div>}
            {!loading && !loadingMore && posts.length > 0 && !hasMore && (
                <div className="text-sm text-gray-500 text-center py-2">마지막 게시글입니다.</div>
            )}
        </div>
    );
};

export default IdolBoardPage;