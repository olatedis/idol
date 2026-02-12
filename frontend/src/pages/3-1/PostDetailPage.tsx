import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";

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
    const navigate = useNavigate();

    const [data, setData] = useState<PostResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const [commentInput, setCommentInput] = useState("");

    useEffect(() => {
        if (!postId) {
            setError("postId가 없습니다.");
            setData(null);
            return;
        }

        if (!API_BASE_URL) {
            setError("VITE_API_BASE_URL이 설정되어 있지 않습니다.")
            setData(null);
            return;
        }

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
            .finally(() => setLoading(false));

        return () => controller.abort();
    }, [API_BASE_URL, postId]);

    const onClickLike = () => {
        // TODO: 추천 API 연동
    };

    const onClickDislike = () => {
        // TODO: 비추천 API 연동
    };

    const onSubmitComment = () => {
        if (!commentInput.trim()) return;

        // TODO: 댓글 작성 API 연동
        // 성공 시에는:
        // 1) input 비우기
        // 2) 댓글 목록 재조회 or optimistic update
    };

    if (loading) return <div className="text-sm text-gray-600">불러오는 중...</div>;
    if (error) return <div className="text-sm text-red-600">{error}</div>;
    if (!data) return <div className="text-sm text-gray-600">데이터가 없습니다.</div>;


    return (
        <div className="space-y-4">
            <div className="border border-gray-200 rounded-2xl bg-white overflow-hidden">
                <div className="px-6 pt-6 pb-4">
                    <div className="text-2xl font-semibold text-gray-900">{data.title}</div>

                    <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-gray-600">
                        <span className="font-medium text-gray-800">{data.authorId}</span>
                        <span>{data.createdAt}</span>
                        <span>조회 {data.viewCount}</span>
                    </div>
                </div>

                <div className="px-6 py-5 border-t border-gray-100">
                    <div
                        className="text-gray-900 leading-relaxed"
                        dangerouslySetInnerHTML={{ __html: data.content }}
                    />
                    <div className="mt-8 flex justify-center gap-10">
                        <button
                            type="button"
                            onClick={onClickLike}
                            className="w-16 h-16 rounded-full border flex flex-col items-center justify-center transition-colors bg-white border-gray-300 text-gray-900 hover:bg-gray-50"
                        >
                            <span className="text-xl">👍</span>
                            <span className="text-sm mt-1">{data.likeCount}</span>
                        </button>

                        <button
                            type="button"
                            onClick={onClickDislike}
                            className="w-16 h-16 rounded-full border flex flex-col items-center justify-center transition-colors bg-white border-gray-300 text-gray-900 hover:bg-gray-50"
                        >
                            <span className="text-xl">👎</span>
                            <span className="text-sm mt-1">{data.dislikeCount}</span>
                        </button>
                    </div>

                    <div className="mt-6 flex justify-center">
                        <button
                            type="button"
                            onClick={() => navigate(-1)}
                            className="px-4 py-2 rounded-full border border-gray-200 text-sm font-semibold hover:bg-gray-50"
                        >
                            목록으로
                        </button>
                    </div>
                </div>
            </div>

            <div className="border border-gray-200 rounded-2xl bg-white overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-100">
                    <div className="font-semibold text-gray-900">댓글 {data.comments.length}</div>
                </div>

                {data.comments.length === 0 ? (
                    <div className="px-6 py-8 text-sm text-gray-600">댓글이 없습니다.</div>
                ) : (
                    <div className="divide-y divide-gray-100">
                        {data.comments.map((c) => (
                            <div key={c.commentId} className="px-6 py-4">
                                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-gray-600">
                                    <span className="font-medium text-gray-800">{c.authorId}</span>
                                    <span>{c.createdAt}</span>
                                    {c.isDeleted ? <span className="text-red-500">삭제됨</span> : null}
                                </div>
                                <div className="mt-2 whitespace-pre-wrap text-gray-900">{c.content}</div>
                            </div>
                        ))}
                    </div>
                )}

                <div className="px-6 py-4 border-t border-gray-100">
                    <div className="flex gap-2">
                        <input
                            value={commentInput}
                            onChange={(e) => setCommentInput(e.target.value)}
                            onKeyDown={(e) => {
                                if (e.key === "Enter") onSubmitComment();
                            }}
                            placeholder="댓글을 입력하세요"
                            className="flex-1 px-4 py-3 rounded-2xl border border-gray-200 text-sm outline-none"
                        />
                        <button
                            type="button"
                            onClick={onSubmitComment}
                            className="px-4 py-3 rounded-2xl bg-[#1FBFB8] text-white text-sm font-semibold hover:bg-[#17AFA8]"
                        >
                            등록
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default PostDetailPage;
