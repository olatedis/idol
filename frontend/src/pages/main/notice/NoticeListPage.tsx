import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

type NoticeListItem = {
    postId: number;
    title: string;
    authorId: number;
    createdAt: string;
    viewCount: number;
    likeCount: number;
};

type PageResponse<T> = {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number; // current page (0-based)
    size: number;
    last: boolean;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const PAGE_SIZE = 20;

const NoticeListPage: React.FC = () => {
    const navigate = useNavigate();
    const [sp, setSp] = useSearchParams();

    const page = Number(sp.get("page") || "0");

    const [data, setData] = useState<PageResponse<NoticeListItem> | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        const controller = new AbortController();

        const run = async () => {
            setLoading(true);
            setError("");

            try {
                if (!API_BASE_URL) {
                    throw new Error("VITE_API_BASE_URL이 설정되어 있지 않습니다.");
                }

                const params = new URLSearchParams();
                params.set("page", String(page));
                params.set("size", String(PAGE_SIZE));
                params.set("sort", "createdAt,desc");

                // 게이트웨이 라우팅 전제: /board/notices/** -> board-service /notices/**
                const res = await fetch(`${API_BASE_URL}/board/notices?${params.toString()}`, {
                    method: "GET",
                    signal: controller.signal,
                });

                if (!res.ok) throw new Error("공지 목록 조회 실패");

                const json = (await res.json()) as PageResponse<NoticeListItem>;
                setData(json);
            } catch (e: any) {
                if (e?.name === "AbortError") return;
                setError(e?.message || "공지 목록 조회 실패");
                setData(null);
            } finally {
                setLoading(false);
            }
        };

        run();
        return () => controller.abort();
    }, [page]);

    const goPage = (next: number) => {
        const nextSp = new URLSearchParams(sp);
        nextSp.set("page", String(Math.max(0, next)));
        setSp(nextSp);
    };

    return (
        <div className="space-y-4">
            <div className="text-xl font-semibold text-gray-900">공지사항</div>

            {loading && <div className="text-sm text-gray-600">불러오는 중...</div>}
            {error && <div className="text-sm text-red-600">{error}</div>}

            {!loading && !error && data && (
                <div className="border border-gray-200 rounded-2xl overflow-hidden bg-white">
                    <div className="grid grid-cols-[90px_1fr_140px_140px] px-4 py-3 text-sm font-semibold text-gray-700 bg-gray-50 border-b border-gray-200">
                        <div className="text-left">번호</div>
                        <div className="text-left">제목</div>
                        <div className="text-left">작성일</div>
                        <div className="text-right">조회수</div>
                    </div>

                    {data.content.length === 0 ? (
                        <div className="px-4 py-6 text-sm text-gray-600">공지사항이 없습니다.</div>
                    ) : (
                        data.content.map((n, idx) => (
                            <button
                                key={n.postId}
                                type="button"
                                onClick={() => navigate(`/notices/${n.postId}`)}
                                className="
                  w-full text-left
                  grid grid-cols-[90px_1fr_140px_140px]
                  px-4 py-3
                  border-b border-gray-100 last:border-b-0
                  hover:bg-gray-50
                  transition-colors
                "
                            >
                                <div className="text-sm text-gray-900 tabular-nums">
                                    {data.totalElements - (page * PAGE_SIZE + idx)}
                                </div>

                                <div className="min-w-0">
                                    <div className="text-sm font-semibold text-gray-900 truncate">{n.title}</div>
                                </div>

                                <div className="text-sm text-gray-600">{n.createdAt}</div>

                                <div className="text-sm text-gray-700 text-right tabular-nums">{n.viewCount}</div>
                            </button>
                        ))
                    )}
                </div>
            )}

            {!loading && data && (
                <div className="flex items-center justify-center gap-2">
                    <button
                        type="button"
                        onClick={() => goPage(page - 1)}
                        disabled={page <= 0}
                        className="px-3 py-2 rounded-full border border-gray-200 text-sm font-semibold hover:bg-gray-50 disabled:opacity-60"
                    >
                        이전
                    </button>

                    <div className="text-sm text-gray-700 tabular-nums">
                        {page + 1} / {data.totalPages}
                    </div>

                    <button
                        type="button"
                        onClick={() => goPage(page + 1)}
                        disabled={data.last}
                        className="px-3 py-2 rounded-full border border-gray-200 text-sm font-semibold hover:bg-gray-50 disabled:opacity-60"
                    >
                        다음
                    </button>
                </div>
            )}
        </div>
    );
};

export default NoticeListPage;
