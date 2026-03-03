import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "../../../stores/authStore";
import Header from "../Header";

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

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

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

const NoticeDetailPage: React.FC = () => {
    const { postId } = useParams();
    const navigate = useNavigate();

    const [data, setData] = useState<NoticeDetail | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    // TODO: 로그인 확정되면 교체
    const { user, accessToken } = useAuthStore();
    const isAdmin = user?.role === "ADMIN";

    useEffect(() => {
        const controller = new AbortController();

        const run = async () => {
            setLoading(true);
            setError("");

            try {
                if (!postId) throw new Error("postId가 없습니다.");
                if (!API_BASE_URL) throw new Error("VITE_API_BASE_URL이 설정되지 않았습니다.");

                const res = await fetch(`${API_BASE_URL}/notices/${postId}`, {
                    method: "GET",
                    signal: controller.signal,
                });

                if (!res.ok) throw new Error("공지 상세 조회 실패");

                const json = (await res.json()) as NoticeDetail;
                setData(json);
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
    }, [API_BASE_URL, postId]);

    const handleDelete = async () => {
        if (!API_BASE_URL) {
            alert("VITE_API_BASE_URL이 설정되지 않았습니다.");
            return;
        }

        if (!accessToken) {
            alert("로그인이 필요합니다.");
            return;
        }

        if (!window.confirm("정말 삭제하시겠습니까?")) return;

        try {
            const res = await fetch(`${API_BASE_URL}/admin/notices/${postId}`, {
                method: "DELETE",
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
            });

            if (res.status === 401) {
                alert("로그인이 필요합니다.");
                return;
            }
            if (res.status === 403) {
                alert("권한이 없습니다. (ADMIN 전용)");
                return;
            }
            if (!res.ok) throw new Error("삭제 실패");

            alert("삭제되었습니다.");
            navigate("/notices");
        } catch (e: any) {
            alert(e?.message || "삭제 실패");
        }
    };

    if (loading) return <div className="text-sm text-gray-600">불러오는 중...</div>;
    if (error) return <div className="text-sm text-red-600">{error}</div>;
    if (!data) return <div className="text-sm text-gray-600">데이터가 없습니다.</div>;

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Header />
            <main className="flex-1 w-full max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-24">
                <div className="space-y-4">
                    <div className="border border-gray-200 rounded-2xl bg-white overflow-hidden">
                        <div className="px-5 sm:px-6 pt-6 pb-4 flex flex-col sm:flex-row sm:justify-between items-start gap-4">
                            <div className="w-full">
                                <div className="text-2xl font-semibold">{data.title}</div>
                                <div className="mt-2 text-sm text-gray-600">
                                    작성일 {formatDateToKST(data.createdAt)} · 조회 {data.viewCount}
                                </div>
                            </div>

                            {isAdmin && (
                                <div className="flex gap-2 self-end sm:self-auto shrink-0">
                                    <button
                                        type="button"
                                        onClick={() => navigate(`/admin/notices/edit/${postId}`)}
                                        className="px-3 py-2 rounded-full border text-sm"
                                    >
                                        수정
                                    </button>
                                    <button
                                        type="button"
                                        onClick={handleDelete}
                                        className="px-3 py-2 rounded-full bg-red-500 text-white text-sm"
                                    >
                                        삭제
                                    </button>
                                </div>
                            )}
                        </div>

                        <div className="px-6 py-5 border-t">
                            <div dangerouslySetInnerHTML={{ __html: data.content }} />
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );
};

export default NoticeDetailPage;