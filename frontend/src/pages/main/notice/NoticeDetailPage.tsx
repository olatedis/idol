import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "../../../stores/authStore";
import { api } from "../../../api/axios";
import Header from "../Header";
import { showConfirm, showErrorToast, showSuccessToast } from "../../../utils/alert";

type NoticeDetail = {
    postId: number;
    title: string;
    content: string;
    authorId: number;
    createdAt: string;
    updatedAt: string;
    viewCount: number;
    likeCount: number;
    dislikeCount: number;
};

// const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

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

const NoticeDetailPage: React.FC = () => {
    const { postId } = useParams();
    const navigate = useNavigate();

    const [data, setData] = useState<NoticeDetail | null>(null);
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
                if (!postId) throw new Error("postId가 없습니다.");
                
                const res = await api.get(`/notices/${postId}`, {
                    signal: controller.signal,
                });

                setData(res.data);
            } catch (e: any) {
                if (e?.name === "AbortError") return;
                setError(e?.message || "공지 상세 조회 실패");
                setData(null);
            } finally {
                setLoading(false);
            }
        };

        run();
        return () => controller.abort();
    }, [postId]);

    const handleDelete = async () => {
        const ok = await showConfirm("공지 삭제", "정말 삭제하시겠습니까? 삭제된 공지는 복구할 수 없습니다.", "삭제");
        if (!ok) return;

        try {
            await api.delete(`/admin/notices/${postId}`);

            showSuccessToast("공지가 성공적으로 삭제되었습니다.");
            navigate("/notices");
        } catch (e: any) {
            showErrorToast(e?.response?.data?.message || e?.message || "삭제 실패");
        }
    };

    return (
        <div className="min-h-screen bg-[#FFF7F8] flex flex-col">
            <Header />

            <main className="flex-1 w-full max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 pt-24 pb-20">
                {/* 수정: 로딩/에러/빈 데이터도 동일 톤으로 정리 */}
                {loading && (
                    <div className="rounded-2xl border border-[#F3D6DC] bg-white px-5 py-4 text-sm text-gray-600 shadow-sm">
                        공지 내용을 불러오는 중입니다...
                    </div>
                )}

                {!loading && error && (
                    <div className="rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-600 shadow-sm">
                        {error}
                    </div>
                )}

                {!loading && !error && !data && (
                    <div className="rounded-2xl border border-[#F3D6DC] bg-white px-5 py-4 text-sm text-gray-600 shadow-sm">
                        데이터가 없습니다.
                    </div>
                )}

                {!loading && !error && data && (
                    <div className="space-y-5">
                        {/* 수정: 상단 헤더 카드 */}
                        <section className="rounded-[28px] border border-[#F3D6DC] bg-white shadow-sm overflow-hidden">
                            <div className="h-2 w-full bg-gradient-to-r from-[var(--color-idol)] via-[var(--color-idol-mid)] to-[var(--color-idol-dark)]" />

                            <div className="px-6 sm:px-8 py-7 sm:py-8">
                                <div className="flex flex-col gap-5 sm:gap-6">
                                    <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
                                        <div className="min-w-0">
                                            {/* 수정: 공지 뱃지 */}
                                            <div className="inline-flex items-center rounded-full bg-[#FFF1F4] px-3 py-1 text-xs font-semibold text-[var(--color-idol-dark)] border border-[#F3D6DC] mb-4">
                                                NOTICE
                                            </div>

                                            <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 leading-snug break-words">
                                                {data.title}
                                            </h1>

                                            <div className="mt-3 flex flex-wrap items-center gap-2 text-sm text-gray-600">
                                                <span className="inline-flex rounded-full bg-[#FFF1F4] px-3 py-1 font-medium text-gray-900">
                                                    작성일 {formatDateToKST(data.createdAt)}
                                                </span>
                                                <span className="inline-flex rounded-full bg-[#FFF1F4] px-3 py-1 font-medium text-gray-900">
                                                    조회 {data.viewCount}
                                                </span>
                                            </div>
                                        </div>

                                        {isAdmin && (
                                            <div className="flex items-center gap-2 shrink-0">
                                                {/* 수정: 관리자 수정 버튼 */}
                                                <button
                                                    type="button"
                                                    onClick={() => navigate(`/admin/notices/edit/${postId}`)}
                                                    className="
                                                        px-4 py-2.5 rounded-full
                                                        border border-[#F3D6DC]
                                                        bg-white text-sm font-semibold text-gray-700
                                                        hover:bg-[#FFF7F8]
                                                        transition-colors
                                                    "
                                                >
                                                    수정
                                                </button>

                                                {/* 수정: 관리자 삭제 버튼 */}
                                                <button
                                                    type="button"
                                                    onClick={handleDelete}
                                                    className="
                                                        px-4 py-2.5 rounded-full
                                                        bg-[var(--color-idol-dark)] text-white text-sm font-semibold
                                                        hover:brightness-95
                                                        transition
                                                    "
                                                >
                                                    삭제
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        </section>

                        {/* 수정: 본문 카드 */}
                        <section className="rounded-[28px] border border-[#F3D6DC] bg-white shadow-sm overflow-hidden">
                            <div className="px-6 sm:px-8 py-7 sm:py-8">
                                <div
                                    className="
                                        prose prose-sm sm:prose-base max-w-none
                                        prose-headings:text-gray-900
                                        prose-p:text-gray-800
                                        prose-strong:text-gray-900
                                        prose-a:text-[var(--color-idol-dark)]
                                        prose-img:rounded-xl
                                        prose-hr:border-[#F3D6DC]
                                    "
                                    dangerouslySetInnerHTML={{ __html: data.content }}
                                />
                            </div>
                        </section>

                        {/* 수정: 하단 액션 버튼 영역 */}
                        <section className="flex items-center justify-between gap-3">
                            <button
                                type="button"
                                onClick={() => navigate("/notices")}
                                className="
                                    px-5 py-3 rounded-2xl
                                    border border-[#F3D6DC] bg-white
                                    text-sm font-semibold text-gray-700
                                    shadow-sm
                                    hover:bg-[#FFF7F8]
                                    transition-colors
                                "
                            >
                                목록으로
                            </button>
                        </section>
                    </div>
                )}
            </main>
        </div>
    );
};

export default NoticeDetailPage;