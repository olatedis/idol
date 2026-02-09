import React, { useEffect, useMemo, useState } from "react";
import { useParams, useSearchParams, useNavigate } from "react-router-dom";

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

const navigate = useNavigate();

// boardType 변환
function resolveBoardType(scope: Scope, type: BoardKind): string {
    if (type === "notice") return "ADMIN_NOTICE";

    if (scope === "idol") {
        return type === "official" ? "IDOL_OFFICIAL" : "IDOL_FAN";
    }

    return type === "official" ? "GROUP_OFFICIAL" : "GROUP_FAN";
}

const BoardPage: React.FC = () => {
    const { groupId } = useParams();
    const [sp, setSp] = useSearchParams();

    // URL 상태
    const scope = (sp.get("scope") as Scope) || "group";
    const board = (sp.get("type") as BoardKind) || "official";
    const page = Number(sp.get("page") || "1");
    const size = Number(sp.get("size") || "20");
    const sort = sp.get("sort") || "latest"; // latest | top
    const q = sp.get("q") || ""; // TODO: 검색 백엔드 연동 시 사용
    const idolId = sp.get("idolId");

    // 게시글 상태
    const [posts, setPosts] = useState<PostListResponse[]>([]);
    const [totalPages, setTotalPages] = useState(1);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");


    // 게시글 목록 fetch
    useEffect(() => {
        if (!API_BASE_URL) return;

        const controller = new AbortController();
        const boardType = resolveBoardType(scope, board);

        const params = new URLSearchParams();
        params.set("boardType", boardType);
        params.set("page", String(page - 1)); // Spring Page는 0-based
        params.set("size", String(size));

        // 정렬
        if (sort === "top") {
            params.set("sort", "likeCount,desc");
        } else {
            params.set("sort", "createdAt,desc");
        }

        // 게시판 범위별 id
        if (boardType.startsWith("GROUP_") && groupId) {
            params.set("groupId", groupId);
        }

        if (boardType.startsWith("IDOL_") && idolId) {
            params.set("idolId", idolId);
        }

        // TODO: 검색 파라미터는 search-service 연동 시 처리
        // if (q) params.set("q", q);

        const url = `${API_BASE_URL}/board/posts?${params.toString()}`;

        setLoading(true);
        setError("");

        fetch(url, { signal: controller.signal })
            .then((res) => {
                if (!res.ok) throw new Error("게시글 조회 실패");
                return res.json();
            })
            .then((data) => {
                setPosts(data.content ?? []);
                setTotalPages(data.totalPages ?? 1);
            })
            .catch((e) => {
                if (e.name === "AbortError") return;
                setError(e.message);
                setPosts([]);
                setTotalPages(1);
            })
            .finally(() => {
                setLoading(false);
            });

        return () => controller.abort();
    }, [API_BASE_URL, scope, board, page, size, sort, idolId, q, groupId]);

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

    const setFilter = (nextScope: Scope, nextType: BoardKind) => {
        const next = new URLSearchParams(sp);
        next.set("scope", nextScope);
        next.set("type", nextType);
        next.set("page", "1");
        setSp(next);
    };

    // 페이지네이션
    const pages = useMemo(() => {
        const result: number[] = [];
        for (let i = 1; i <= totalPages; i++) result.push(i);
        return result;
    }, [totalPages]);

    const goPage = (p: number) => {
        const next = new URLSearchParams(sp);
        next.set("page", String(p));
        setSp(next);
    };

    // Render
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
                        onClick={() => {
                            const next = new URLSearchParams(sp);
                            next.set("sort", "latest");
                            setSp(next);
                        }}
                        className={[
                            "px-3 py-2 rounded-full text-sm font-semibold border",
                            sort === "latest"
                                ? "bg-[#1FBFB8] text-white border-[#1FBFB8]"
                                : "bg-white border-gray-200",
                        ].join(" ")}
                    >
                        최신순
                    </button>

                    <button
                        onClick={() => {
                            const next = new URLSearchParams(sp);
                            next.set("sort", "top");
                            setSp(next);
                        }}
                        className={[
                            "px-3 py-2 rounded-full text-sm font-semibold border",
                            sort === "top"
                                ? "bg-[#1FBFB8] text-white border-[#1FBFB8]"
                                : "bg-white border-gray-200",
                        ].join(" ")}
                    >
                        추천순
                    </button>
                </div>
            </div>

            {/* 게시글 리스트 */}
            {loading && <div className="text-sm text-gray-600">불러오는 중...</div>}
            {error && <div className="text-sm text-red-600">{error}</div>}

            {!loading && posts.length === 0 && (
                <div className="border rounded-2xl p-6 text-sm text-gray-600">
                    게시글이 없습니다.
                </div>
            )}

            <div className="space-y-2">
                {posts.map((p) => (
                    <div
                        key={p.postId}
                        onClick={() => navigate(`./${p.postId}`)}
                        className="border rounded-2xl p-4 hover:border-[#1FBFB8] cursor-pointer"
                    >
                        <div className="font-semibold">{p.title}</div>
                        <div className="mt-2 text-sm text-gray-600 flex gap-4">
                            <span>작성자 {p.authorId}</span>
                            <span>조회 {p.viewCount}</span>
                            <span>추천 {p.likeCount}</span>
                            <span>{new Date(p.createdAt).toLocaleDateString()}</span>
                        </div>
                    </div>
                ))}
            </div>

            {/* 페이지네이션 */}
            <div className="flex justify-center gap-1 pt-4">
                {pages.map((p) => (
                    <button
                        key={p}
                        onClick={() => goPage(p)}
                        className={[
                            "w-9 h-9 rounded-full border text-sm font-semibold",
                            p === page
                                ? "bg-[#1FBFB8] text-white border-[#1FBFB8]"
                                : "bg-white border-gray-200",
                        ].join(" ")}
                    >
                        {p}
                    </button>
                ))}
            </div>

            {/* 검색 (UI만) */}
            <div className="flex justify-center gap-2 pt-4">
                <input
                    value={q}
                    disabled
                    placeholder="검색 (TODO)"
                    className="w-full max-w-md px-4 py-3 rounded-2xl border border-gray-200 bg-gray-100"
                />
            </div>
        </div>
    );
};

export default BoardPage;
