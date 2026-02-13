import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

type CommentResponse = {
    commentId: number;
    authorId: number;
    authorNickname?: string | null;

    content: string;
    isDeleted: boolean;

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
    content: string; // HTML

    viewCount: number;
    likeCount: number;
    dislikeCount: number;

    createdAt: string;
    updatedAt: string;

    comments: CommentResponse[];
};

type PostReactionResponse = {
    likeCount: number;
    dislikeCount: number;
    // NONE / LIKE / DISLIKE 중 하나
    myReaction: "NONE" | "LIKE" | "DISLIKE" | string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const PostDetailPage: React.FC = () => {
    const { postId } = useParams();
    const navigate = useNavigate();

    const [data, setData] = useState<PostResponse | null>(null);
    const [reaction, setReaction] = useState<PostReactionResponse | null>(null);

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const [commentInput, setCommentInput] = useState("");
    const [commentSubmitting, setCommentSubmitting] = useState(false);
    const [reactionSubmitting, setReactionSubmitting] = useState(false);

    const getAccessTokenOrThrow = () => {
        // TODO: 로그인 연동되면 accessToken 저장 방식/키 확정
        const accessToken = localStorage.getItem("accessToken");
        if (!accessToken) throw new Error("로그인이 필요합니다. (accessToken 없음)");
        return accessToken;
    };

    const fetchDetailAndReaction = async (signal?: AbortSignal) => {
        if (!postId) throw new Error("postId가 없습니다.");
        if (!API_BASE_URL) throw new Error("VITE_API_BASE_URL이 설정되어 있지 않습니다.");

        const accessToken = getAccessTokenOrThrow();

        const detailReq = fetch(`${API_BASE_URL}/board/posts/${postId}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            signal,
        });

        const reactionReq = fetch(`${API_BASE_URL}/board/posts/${postId}/reaction`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            signal,
        });

        const [detailRes, reactionRes] = await Promise.all([detailReq, reactionReq]);

        if (!detailRes.ok) throw new Error("게시글 상세 조회 실패");
        if (!reactionRes.ok) throw new Error("내 반응 조회 실패");

        const detailJson = (await detailRes.json()) as PostResponse;
        const reactionJson = (await reactionRes.json()) as PostReactionResponse;

        setData(detailJson);
        setReaction(reactionJson);
    };

    useEffect(() => {
        const controller = new AbortController();

        const run = async () => {
            setLoading(true);
            setError("");

            try {
                await fetchDetailAndReaction(controller.signal);
            } catch (e: any) {
                if (e?.name === "AbortError") return;
                setError(e?.message || "상세 조회 실패");
                setData(null);
                setReaction(null);
            } finally {
                setLoading(false);
            }
        };

        run();
        return () => controller.abort();
    }, [API_BASE_URL, postId]);

    const applyReactionToState = (r: PostReactionResponse) => {
        setReaction(r);
        setData((prev) => {
            if (!prev) return prev;
            return {
                ...prev,
                likeCount: r.likeCount,
                dislikeCount: r.dislikeCount,
            };
        });
    };

    const onClickLike = async () => {
        if (!postId) return;
        if (!API_BASE_URL) return;
        if (reactionSubmitting) return;

        setReactionSubmitting(true);
        setError("");

        try {
            const accessToken = getAccessTokenOrThrow();

            const res = await fetch(`${API_BASE_URL}/board/posts/${postId}/like`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
            });

            if (!res.ok) throw new Error("추천 처리 실패");

            const json = (await res.json()) as PostReactionResponse;
            applyReactionToState(json);
        } catch (e: any) {
            setError(e?.message || "추천 처리 실패");
        } finally {
            setReactionSubmitting(false);
        }
    };

    const onClickDislike = async () => {
        if (!postId) return;
        if (!API_BASE_URL) return;
        if (reactionSubmitting) return;

        setReactionSubmitting(true);
        setError("");

        try {
            const accessToken = getAccessTokenOrThrow();

            const res = await fetch(`${API_BASE_URL}/board/posts/${postId}/dislike`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
            });

            if (!res.ok) throw new Error("비추천 처리 실패");

            const json = (await res.json()) as PostReactionResponse;
            applyReactionToState(json);
        } catch (e: any) {
            setError(e?.message || "비추천 처리 실패");
        } finally {
            setReactionSubmitting(false);
        }
    };

    const onSubmitComment = async () => {
        if (!postId) return;
        if (!API_BASE_URL) return;
        if (commentSubmitting) return;

        const content = commentInput.trim();
        if (!content) return;

        setCommentSubmitting(true);
        setError("");

        try {
            const accessToken = getAccessTokenOrThrow();

            const res = await fetch(`${API_BASE_URL}/board/posts/${postId}/comments`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${accessToken}`,
                },
                body: JSON.stringify({ content }),
            });

            if (!res.ok) throw new Error("댓글 작성 실패");

            setCommentInput("");

            // 작성 후: 상세 1회 재조회로 comments 갱신
            await fetchDetailAndReaction();
        } catch (e: any) {
            setError(e?.message || "댓글 작성 실패");
        } finally {
            setCommentSubmitting(false);
        }
    };

    if (loading) return <div className="text-sm text-gray-600">불러오는 중...</div>;
    if (error) return <div className="text-sm text-red-600">{error}</div>;
    if (!data) return <div className="text-sm text-gray-600">데이터가 없습니다.</div>;

    const myReaction = reaction?.myReaction ?? "NONE";
    const likeActive = myReaction === "LIKE";
    const dislikeActive = myReaction === "DISLIKE";

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
                    <div className="text-gray-900 leading-relaxed" dangerouslySetInnerHTML={{ __html: data.content }} />

                    <div className="mt-8 flex justify-center gap-10">
                        <button
                            type="button"
                            onClick={onClickLike}
                            disabled={reactionSubmitting}
                            className={[
                                "w-16 h-16 rounded-full border flex flex-col items-center justify-center transition-colors",
                                likeActive ? "bg-[#1FBFB8] border-[#1FBFB8] text-white" : "bg-white border-gray-300 text-gray-900 hover:bg-gray-50",
                                reactionSubmitting ? "opacity-60" : "",
                            ].join(" ")}
                        >
                            <span className="text-xl">👍</span>
                            <span className="text-sm mt-1">{data.likeCount}</span>
                        </button>

                        <button
                            type="button"
                            onClick={onClickDislike}
                            disabled={reactionSubmitting}
                            className={[
                                "w-16 h-16 rounded-full border flex flex-col items-center justify-center transition-colors",
                                dislikeActive ? "bg-[#1FBFB8] border-[#1FBFB8] text-white" : "bg-white border-gray-300 text-gray-900 hover:bg-gray-50",
                                reactionSubmitting ? "opacity-60" : "",
                            ].join(" ")}
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
                    <div className="font-semibold text-gray-900">댓글 {data.comments?.length ?? 0}</div>
                </div>

                {!data.comments || data.comments.length === 0 ? (
                    <div className="px-6 py-8 text-sm text-gray-600">댓글이 없습니다.</div>
                ) : (
                    <div className="divide-y divide-gray-100">
                        {data.comments.map((c) => (
                            <div key={c.commentId} className="px-6 py-4">
                                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-gray-600">
                                    <span className="font-medium text-gray-800">{c.authorNickname ? c.authorNickname : c.authorId}</span>
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
                            disabled={commentSubmitting}
                        />
                        <button
                            type="button"
                            onClick={onSubmitComment}
                            disabled={commentSubmitting}
                            className="px-4 py-3 rounded-2xl bg-[#1FBFB8] text-white text-sm font-semibold hover:bg-[#17AFA8] disabled:opacity-60"
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
