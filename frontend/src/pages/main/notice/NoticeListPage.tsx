import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuthStore } from "../../../stores/authStore";
import Header from "../Header";

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

// 날짜 문자열을 KST 기준으로 표시하기 위한 헬퍼 함수
const formatDateToKST = (dateString: string) => {
    if (!dateString) return "";

    // 백엔드는 'YYYY-MM-DD HH:mm:ss' (UTC/GMT) 형태로 문자열을 전달한다고 가정
    // JS Date 객체로 파싱 시 UTC로 인식시키기 위해 뒤에 'Z'를 추가
    const utcDate = new Date(`${dateString.replace(' ', 'T')}Z`);

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

const NoticeListPage: React.FC = () => {
    const navigate = useNavigate();
    const [sp, setSp] = useSearchParams();

    const page = Number(sp.get("page") || "0");

    const [data, setData] = useState<PageResponse<NoticeListItem> | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    // TODO: 로그인 확정되면 교체
    const { user } = useAuthStore();
    const isAdmin = user?.role === "ADMIN";

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

                // 백엔드의 실제 엔드포인트는 /notices 입니다.
                const res = await fetch(`${API_BASE_URL}/notices?${params.toString()}`, {
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

    const scrollTop = () => {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    const onClickWrite = () => {
        navigate("/admin/notices/write");
    };

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Header />
            <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24">
                <div className="mb-10 text-center sm:text-left">
                    <h1 className="text-3xl sm:text-4xl font-extrabold text-gray-900 tracking-tight mb-3">
                        공지사항
                    </h1>
                    <p className="text-gray-500 text-lg sm:text-xl">
                        아이돌 서비스의 주요 소식과 안내를 확인하세요.
                    </p>
                </div>

                <div className="space-y-4">

                    {loading && <div className="text-sm text-gray-600">불러오는 중...</div>}
                    {error && <div className="text-sm text-red-600">{error}</div>}

                    {!loading && !error && data && (
                        <div className="border border-gray-200 rounded-2xl overflow-hidden bg-white">
                            <div
                                className="hidden sm:grid grid-cols-[90px_1fr_140px_140px] px-4 py-3 text-sm font-semibold text-gray-700 bg-gray-50 border-b border-gray-200">
                                <div className="text-center sm:text-left">번호</div>
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
                                    grid grid-cols-[50px_1fr] sm:grid-cols-[90px_1fr_140px_140px] gap-x-3 sm:gap-x-0 items-center
                                    px-4 py-4 sm:py-3
                                    border-b border-gray-100 last:border-b-0
                                    hover:bg-gray-50
                                    transition-colors
                                "
                                    >
                                        <div className="text-sm text-gray-500 sm:text-gray-900 tabular-nums text-center sm:text-left">
                                            {data.totalElements - (page * PAGE_SIZE + idx)}
                                        </div>

                                        <div className="min-w-0">
                                            <div className="text-sm font-semibold text-gray-900 truncate">{n.title}</div>
                                            <div className="text-xs text-gray-500 mt-1 sm:hidden">
                                                {n.createdAt.split(" ")[0]} · 조회 {n.viewCount}
                                            </div>
                                        </div>

                                        <div className="hidden sm:block text-sm text-gray-600 truncate">{formatDateToKST(n.createdAt)}</div>

                                        <div className="hidden sm:block text-sm text-gray-700 text-right tabular-nums">{n.viewCount}</div>
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

                        {isAdmin && (
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
                                작성하기
                            </button>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
};

export default NoticeListPage;