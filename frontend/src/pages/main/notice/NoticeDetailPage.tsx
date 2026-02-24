import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

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

const NoticeDetailPage: React.FC = () => {
    const { postId } = useParams();
    const navigate = useNavigate();

    const [data, setData] = useState<NoticeDetail | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    // TODO: 로그인 구조 확정되면 교체
    const accessToken = localStorage.getItem("accessToken");
    const userRole = localStorage.getItem("role"); // ADMIN / USER
    const isAdmin = userRole === "ADMIN";

    useEffect(() => {
        const controller = new AbortController();

        const run = async () => {
            setLoading(true);
            setError("");

            try {
                if (!postId) throw new Error("postId가 없습니다.");
                if (!API_BASE_URL) throw new Error("VITE_API_BASE_URL이 설정되지 않았습니다.");

                const res = await fetch(`${API_BASE_URL}/board/notices/${postId}`, {
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
            const res = await fetch(`${API_BASE_URL}/board/admin/notices/${postId}`, {
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
        <div className="space-y-4">
            <div className="border border-gray-200 rounded-2xl bg-white overflow-hidden">
                <div className="px-6 pt-6 pb-4 flex justify-between items-start">
                    <div>
                        <div className="text-2xl font-semibold">{data.title}</div>
                        <div className="mt-2 text-sm text-gray-600">
                            작성일 {data.createdAt} · 조회 {data.viewCount}
                        </div>
                    </div>

                    {isAdmin && (
                        <div className="flex gap-2">
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
    );
};

export default NoticeDetailPage;