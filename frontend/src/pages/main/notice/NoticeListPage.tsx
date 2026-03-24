import React, { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuthStore } from "../../../stores/authStore";
import { api } from "../../../api/axios";
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
    number: number;
    size: number;
    last: boolean;
};

// const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
const PAGE_SIZE = 20;

// 날짜 문자열을 KST 기준으로 표시하기 위한 헬퍼 함수
const formatDateToKST = (dateString: string) => {
    if (!dateString) return "";

    const utcDate = new Date(`${dateString.replace(" ", "T")}Z`);

    if (isNaN(utcDate.getTime())) return dateString;

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

    const { user } = useAuthStore();
    const isAdmin = user?.role === "ADMIN";

    useEffect(() => {
        const controller = new AbortController();

        const run = async () => {
            setLoading(true);
            setError("");

            try {
                const params = {
                    page,
                    size: PAGE_SIZE,
                    sort: "createdAt,desc"
                };

                const res = await api.get("/notices", {
                    params,
                    signal: controller.signal,
                });

                setData(res.data);
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
        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    const onClickWrite = () => {
        navigate("/admin/notices/write");
    };

    return (
        // 수정: 전체 배경을 핑크 톤과 어울리는 아주 연한 배경으로 변경
        <div className="min-h-screen bg-[#FFF7F8] flex flex-col">
            <Header />

            <main className="flex-1 w-full max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 pt-24 pb-20">
                {/* 수정: 상단 소개 카드 색감 핑크 계열로 정리 + 불필요한 뱃지 제거 */}
                <section className="mb-8">
                    <div className="rounded-[28px] border border-[#F3D6DC] bg-white shadow-sm overflow-hidden">
                        {/* 수정: 상단 포인트 라인을 pink 계열 gradient로 변경 */}
                        <div className="h-2 w-full bg-gradient-to-r from-[var(--color-idol)] via-[var(--color-idol-mid)] to-[var(--color-idol-dark)]" />

                        <div className="px-6 sm:px-8 py-8 sm:py-9">
                            <h1 className="text-3xl sm:text-4xl font-extrabold text-gray-900 tracking-tight leading-tight">
                                공지사항
                            </h1>

                            <p className="mt-3 text-sm sm:text-base text-gray-600 leading-relaxed">
                                서비스 운영 안내와 주요 소식을 한눈에 확인하세요.
                            </p>
                        </div>
                    </div>
                </section>

                <section className="space-y-4">
                    {loading && (
                        <div className="rounded-2xl border border-[#F3D6DC] bg-white px-5 py-4 text-sm text-gray-600 shadow-sm">
                            공지 목록을 불러오는 중입니다...
                        </div>
                    )}

                    {error && (
                        <div className="rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-600 shadow-sm">
                            {error}
                        </div>
                    )}

                    {!loading && !error && data && (
                        <>
                            {/* 수정: 리스트 카드 색감 정리 */}
                            <div className="rounded-[28px] border border-[#F3D6DC] bg-white shadow-sm overflow-hidden">
                                <div className="hidden sm:grid grid-cols-[90px_1fr_160px_100px] items-center px-6 py-4 bg-[#FFF1F4] border-b border-[#F3D6DC] text-sm font-semibold text-gray-700">
                                    <div>번호</div>
                                    <div>제목</div>
                                    <div>작성일</div>
                                    <div className="text-right">조회수</div>
                                </div>

                                {data.content.length === 0 ? (
                                    <div className="px-6 py-14 text-center">
                                        <div className="text-base font-semibold text-gray-800">
                                            등록된 공지사항이 없습니다.
                                        </div>
                                        <div className="mt-1 text-sm text-gray-500">
                                            새로운 공지가 등록되면 이곳에 표시됩니다.
                                        </div>
                                    </div>
                                ) : (
                                    data.content.map((n, idx) => (
                                        <button
                                            key={n.postId}
                                            type="button"
                                            onClick={() => navigate(`/notices/${n.postId}`)}
                                            className="
                                                group w-full text-left
                                                grid grid-cols-[56px_1fr] sm:grid-cols-[90px_1fr_160px_100px]
                                                items-center gap-x-3
                                                px-4 sm:px-6 py-3 sm:py-3.5
                                                border-b border-[#F8E4E8] last:border-b-0
                                                hover:bg-[#FFF7F8]
                                                transition-colors
                                            "
                                        >
                                            {/* 수정: 번호 pill은 유지하되 더 작고 핑크 계열로 변경 */}
                                            <div className="flex items-center justify-center sm:justify-start">
                                                <span className="inline-flex min-w-10 justify-center rounded-full bg-[#FFF1F4] px-2.5 py-1 text-xs sm:text-sm font-semibold text-[var(--color-idol-dark)] tabular-nums">
                                                    {data.totalElements - (page * PAGE_SIZE + idx)}
                                                </span>
                                            </div>

                                            {/* 수정: 제목 앞 점 제거 */}
                                            <div className="min-w-0">
                                                <div className="truncate text-sm sm:text-[15px] font-semibold text-gray-900 group-hover:text-[var(--color-idol-dark)] transition-colors">
                                                    {n.title}
                                                </div>

                                                <div className="mt-1 flex items-center gap-2 text-xs text-gray-500 sm:hidden">
                                                    <span>{formatDateToKST(n.createdAt)}</span>
                                                    <span>·</span>
                                                    <span>조회 {n.viewCount}</span>
                                                </div>
                                            </div>

                                            <div className="hidden sm:block text-sm text-gray-600 tabular-nums">
                                                {formatDateToKST(n.createdAt)}
                                            </div>

                                            <div className="hidden sm:block text-right">
                                                <span className="inline-flex rounded-full bg-[#FFF1F4] px-3 py-1 text-sm font-semibold text-gray-900 tabular-nums">
                                                    {n.viewCount}
                                                </span>
                                            </div>
                                        </button>
                                    ))
                                )}
                            </div>

                            {/* 수정: 페이지네이션도 핑크톤으로 정리 */}
                            <div className="flex items-center justify-center gap-3 pt-2">
                                <button
                                    type="button"
                                    onClick={() => goPage(page - 1)}
                                    disabled={page <= 0}
                                    className="
                                        min-w-[76px] px-4 py-2.5 rounded-full
                                        border border-[#F3D6DC] bg-white
                                        text-sm font-semibold text-gray-700
                                        shadow-sm
                                        hover:bg-[#FFF7F8]
                                        disabled:opacity-50 disabled:cursor-not-allowed
                                    "
                                >
                                    이전
                                </button>

                                <div className="rounded-full bg-white border border-[#F3D6DC] px-4 py-2.5 text-sm font-semibold text-gray-700 shadow-sm">
                                    <span className="text-[var(--color-idol-dark)]">{page + 1}</span>
                                    <span className="mx-1 text-gray-400">/</span>
                                    <span>{data.totalPages}</span>
                                </div>

                                <button
                                    type="button"
                                    onClick={() => goPage(page + 1)}
                                    disabled={data.last}
                                    className="
                                        min-w-[76px] px-4 py-2.5 rounded-full
                                        border border-[#F3D6DC] bg-white
                                        text-sm font-semibold text-gray-700
                                        shadow-sm
                                        hover:bg-[#FFF7F8]
                                        disabled:opacity-50 disabled:cursor-not-allowed
                                    "
                                >
                                    다음
                                </button>
                            </div>
                        </>
                    )}
                </section>

                {/* 수정: 플로팅 버튼도 핑크 계열로 변경 */}
                <div className="fixed right-4 bottom-6 z-40 flex flex-col items-end gap-3">
                    <button
                        type="button"
                        onClick={scrollTop}
                        className="
                            w-12 h-12 rounded-full
                            bg-white border border-[#F3D6DC]
                            shadow-md
                            text-gray-700 text-lg font-semibold
                            hover:bg-[#FFF7F8]
                            transition-colors
                        "
                        aria-label="맨 위로"
                    >
                        ↑
                    </button>

                    {isAdmin && (
                        <button
                            type="button"
                            onClick={onClickWrite}
                            className="
                                px-5 py-3 rounded-2xl
                                bg-[var(--color-idol-dark)] text-white text-sm font-semibold
                                shadow-md
                                hover:brightness-95
                                transition
                            "
                        >
                            작성하기
                        </button>
                    )}
                </div>
            </main>
        </div>
    );
};

export default NoticeListPage;