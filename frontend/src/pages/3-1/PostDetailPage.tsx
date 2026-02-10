import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

type CommentResponse = {
    commentId: number;
    authorId: number;
    isDeleted: boolean;
    content: string;
    createdAt: string;
    updatedAt: string;
};

type PostResponse = {
    postId: number;
    boardType: string;
    idolId: number | null;
    groupId: number | null;

    authorId: number;
    title: string;
    content: string;

    viewCount: number;
    likeCount: number;
    dislikeCount: number;

    createdAt: string;
    updatedAt: string;

    comments: CommentResponse[];
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const PostDetailPage: React.FC = () => {
    const { postId } = useParams();

    const [data, setData] = useState<PostResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!API_BASE_URL) return;
        if (!postId) return;

        const controller = new AbortController();

        setLoading(true);
        setError("");

        // TODO: 로그인 연동되면 실제 값으로 교체
        const userId = "1";
        const userRole = "USER";

        fetch(`${API_BASE_URL}/board/posts/${postId}`, {
            method: "GET",
            headers: {
                "X-User-Id": userId,
                "X-User-Role": userRole,
            },
            signal: controller.signal,
        })
            .then((res) => {
                if (!res.ok) throw new Error("게시글 상세 조회 실패");
                return res.json() as Promise<PostResponse>;
            })
            .then((json) => {
                setData(json);
            })
            .catch((e) => {
                if (e.name === "AbortError") return;
                setError(e.message);
                setData(null);
            })
            .finally(() => {
                setLoading(false);
            });

        return () => controller.abort();
    }, [postId]);

    if (loading) return <div className="text-sm text-gray-600">불러오는 중...</div>;
    if (error) return <div className="text-sm text-red-600">{error}</div>;
    if (!data) return <div className="text-sm text-gray-600">데이터가 없습니다.</div>;

    return (
        <div className="space-y-4">
            <div className="border rounded-2xl p-5">
                <div className="text-xl font-semibold">{data.title}</div>

                <div className="mt-2 text-sm text-gray-600 flex flex-wrap gap-x-4 gap-y-1">
                    <span>작성자 {data.authorId}</span>
                    <span>조회 {data.viewCount}</span>
                    <span>추천 {data.likeCount}</span>
                    <span>비추천 {data.dislikeCount}</span>
                    <span>{data.createdAt}</span>
                </div>

                <div className="mt-5 whitespace-pre-wrap text-gray-900">{data.content}</div>
            </div>

            <div className="border rounded-2xl p-5">
                <div className="font-semibold mb-3">댓글</div>

                {data.comments.length === 0 ? (
                    <div className="text-sm text-gray-600">댓글이 없습니다.</div>
                ) : (
                    <div className="space-y-3">
                        {data.comments.map((c) => (
                            <div key={c.commentId} className="border rounded-2xl p-4">
                                <div className="text-sm text-gray-600 flex gap-3 flex-wrap">
                                    <span>작성자 {c.authorId}</span>
                                    <span>{c.createdAt}</span>
                                    {c.isDeleted ? <span className="text-red-500">삭제됨</span> : null}
                                </div>
                                <div className="mt-2 whitespace-pre-wrap">{c.content}</div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default PostDetailPage;
