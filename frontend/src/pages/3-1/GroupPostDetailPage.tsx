import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";
import { api } from "../../api/axios";

type CommentResponse = {
    commentId: number;
    authorId: number;
    authorNickname: string | null;

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
    content: string;

    viewCount: number;
    likeCount: number;
    dislikeCount: number;

    myReaction: string;

    createdAt: string;
    updatedAt: string;

    comments: CommentResponse[];
};

type PostReactionResponse = {
    likeCount: number;
    dislikeCount: number;
    myReaction: string;
};

const clamp0 = (n: number) => (Number.isFinite(n) ? Math.max(0, n) : 0);

const GroupPostDetailPage: React.FC = () => {
    const { postId } = useParams();
    const navigate = useNavigate();

    const { accessToken } = useAuthStore();

    const [data, setData] = useState<PostResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const [commentInput, setCommentInput] = useState("");
    const [submittingComment, setSubmittingComment] = useState(false);

    const [reacting, setReacting] = useState(false);

    const fetchDetail = async () => {
        if (!postId) throw new Error("postId가 없습니다.");
        if (!accessToken) throw new Error("로그인이 필요합니다.");

        const res = await api.get(`/board/posts/${postId}`);
        const json = res.data as PostResponse;

        return {
            ...json,
            comments: Array.isArray(json.comments) ? json.comments : [],
            myReaction: (json.myReaction || "NONE") as string,
        };
    };

    // 상세 진입 시 내 반응/카운트 동기화(목록 갔다 와도 민트색 유지)
    const fetchReaction = async () => {
        if (!postId) throw new Error("postId가 없습니다.");
        if (!accessToken) throw new Error("로그인이 필요합니다.");

        const res = await api.get(`/board/posts/${postId}/reaction`);
        return res.data as PostReactionResponse;
    };

    useEffect(() => {
        let cancelled = false;

        const run = async () => {
            setError("");

            if (!postId) {
                setError("postId가 없습니다.");
                setData(null);
                return;
            }

            if (!accessToken) {
                setError("로그인이 필요합니다.");
                setData(null);
                return;
            }

            try {
                setLoading(true);

                // 1) 상세 먼저 로드
                const detail = await fetchDetail();
                if (cancelled) return;
                setData(detail);

                // 2) reaction으로 myReaction/카운트 재동기화
                try {
                    const reaction = await fetchReaction();
                    if (cancelled) return;

                    setData((prev) => {
                        if (!prev) return prev;
                        return {
                            ...prev,
                            likeCount: clamp0(reaction.likeCount),
                            dislikeCount: clamp0(reaction.dislikeCount),
                            myReaction: reaction.myReaction || "NONE",
                        };
                    });
                } catch {
                    // reaction 동기화 실패해도 상세는 보여주도록 무시
                }
            } catch (e: any) {
                const status = e?.response?.status;
                if (status === 401) setError("로그인이 필요합니다.");
                else if (status === 403) setError("권한이 없습니다. (구독 필요 또는 접근 불가)");
                else setError(e?.response?.data?.message || e?.message || "게시글 상세 조회 실패");
                setData(null);
            } finally {
                if (!cancelled) setLoading(false);
            }
        };

        run();

        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [postId, accessToken]);

    const onClickLike = async () => {
        if (!data) return;
        if (!postId) return;

        if (!accessToken) {
            alert("로그인이 필요합니다.");
            return;
        }

        if (reacting) return;
        setReacting(true);

        try {
            const res = await api.post(`/board/posts/${postId}/like`);
            const json = res.data as PostReactionResponse;

            setData((prev) => {
                if (!prev) return prev;
                return {
                    ...prev,
                    likeCount: clamp0(json.likeCount),
                    dislikeCount: clamp0(json.dislikeCount),
                    myReaction: json.myReaction || "NONE",
                };
            });
        } catch (e: any) {
            const status = e?.response?.status;
            if (status === 401) alert("로그인이 필요합니다.");
            else if (status === 403) alert("권한이 없습니다.");
            else alert(e?.response?.data?.message || e?.message || "추천 처리 실패");
        } finally {
            setReacting(false);
        }
    };

    const onClickDislike = async () => {
        if (!data) return;
        if (!postId) return;

        if (!accessToken) {
            alert("로그인이 필요합니다.");
            return;
        }

        if (reacting) return;
        setReacting(true);

        try {
            const res = await api.post(`/board/posts/${postId}/dislike`);
            const json = res.data as PostReactionResponse;

            setData((prev) => {
                if (!prev) return prev;
                return {
                    ...prev,
                    likeCount: clamp0(json.likeCount),
                    dislikeCount: clamp0(json.dislikeCount),
                    myReaction: json.myReaction || "NONE",
                };
            });
        } catch (e: any) {
            const status = e?.response?.status;
            if (status === 401) alert("로그인이 필요합니다.");
            else if (status === 403) alert("권한이 없습니다.");
            else alert(e?.response?.data?.message || e?.message || "비추천 처리 실패");
        } finally {
            setReacting(false);
        }
    };

    const onSubmitComment = async () => {
        if (!data) return;
        if (!postId) return;

        if (!commentInput.trim()) return;

        if (!accessToken) {
            alert("로그인이 필요합니다.");
            return;
        }

        if (submittingComment) return;
        setSubmittingComment(true);

        try {
            await api.post(`/board/posts/${postId}/comments`, {
                content: commentInput.trim(),
            });

            setCommentInput("");

            // 댓글 작성 후 "상세 재조회"로 완전 동기화
            const detail = await fetchDetail();
            setData(detail);

            // 댓글 작성 후에도 reaction 한 번 동기화(선택)
            try {
                const reaction = await fetchReaction();
                setData((prev) => {
                    if (!prev) return prev;
                    return {
                        ...prev,
                        likeCount: clamp0(reaction.likeCount),
                        dislikeCount: clamp0(reaction.dislikeCount),
                        myReaction: reaction.myReaction || "NONE",
                    };
                });
            } catch {
                // 무시
            }
        } catch (e: any) {
            const status = e?.response?.status;
            if (status === 401) alert("로그인이 필요합니다.");
            else if (status === 403) alert("권한이 없습니다.");
            else alert(e?.response?.data?.message || e?.message || "댓글 작성 실패");
        } finally {
            setSubmittingComment(false);
        }
    };

    if (loading) return <div className="text-sm text-gray-600">불러오는 중...</div>;
    if (error) return <div className="text-sm text-red-600">{error}</div>;
    if (!data) return <div className="text-sm text-gray-600">데이터가 없습니다.</div>;

    const likeActive = data.myReaction === "LIKE";
    const dislikeActive = data.myReaction === "DISLIKE";

    const commentCount = Array.isArray(data.comments) ? data.comments.length : 0;

    const safeLikeCount = clamp0(data.likeCount);
    const safeDislikeCount = clamp0(data.dislikeCount);

    const reactionBtnBase =
        "w-16 h-16 rounded-full border flex flex-col items-center justify-center " +
        "transition-all duration-150 " +
        "hover:shadow-md hover:-translate-y-[1px] active:translate-y-0 active:scale-[0.98] " +
        "disabled:opacity-60 disabled:cursor-not-allowed";

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
                    {/* content는 HTML 저장이므로 렌더링 */}
                    <div className="text-gray-900 leading-relaxed" dangerouslySetInnerHTML={{ __html: data.content }} />

                    <div className="mt-8 flex justify-center gap-10">
                        <button
                            type="button"
                            onClick={onClickLike}
                            disabled={reacting}
                            className={[
                                reactionBtnBase,
                                likeActive
                                    ? "bg-[#1FBFB8] border-[#1FBFB8] text-white"
                                    : "bg-white border-gray-300 text-gray-900 hover:bg-gray-50",
                            ].join(" ")}
                        >
                            <span className="text-xl">👍</span>
                            <span className="text-sm mt-1">{safeLikeCount}</span>
                        </button>

                        <button
                            type="button"
                            onClick={onClickDislike}
                            disabled={reacting}
                            className={[
                                reactionBtnBase,
                                dislikeActive
                                    ? "bg-[#1FBFB8] border-[#1FBFB8] text-white"
                                    : "bg-white border-gray-300 text-gray-900 hover:bg-gray-50",
                            ].join(" ")}
                        >
                            <span className="text-xl">👎</span>
                            <span className="text-sm mt-1">{safeDislikeCount}</span>
                        </button>
                    </div>

                    <div className="mt-6 flex justify-center">
                        <button
                            type="button"
                            onClick={() => navigate(-1)}
                            className={[
                                "px-4 py-2 rounded-full border border-gray-200 text-sm font-semibold",
                                "transition-all duration-150",
                                "hover:bg-gray-50 hover:shadow-sm hover:-translate-y-[1px]",
                                "active:translate-y-0 active:scale-[0.99]",
                            ].join(" ")}
                        >
                            목록으로
                        </button>
                    </div>
                </div>
            </div>

            <div className="border border-gray-200 rounded-2xl bg-white overflow-hidden">
                <div className="px-6 py-4 border-b border-gray-100">
                    <div className="font-semibold text-gray-900">댓글 {commentCount}</div>
                </div>

                {commentCount === 0 ? (
                    <div className="px-6 py-8 text-sm text-gray-600">댓글이 없습니다.</div>
                ) : (
                    <div className="divide-y divide-gray-100">
                        {(data.comments ?? []).map((c) => {
                            const nickname = c.authorNickname ? c.authorNickname : String(c.authorId);

                            return (
                                <div key={c.commentId} className="px-6 py-4">
                                    <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-gray-600">
                                        <span className="font-medium text-gray-800">{nickname}</span>
                                        <span>{c.createdAt}</span>
                                        {c.isDeleted ? <span className="text-red-500">삭제됨</span> : null}
                                    </div>

                                    <div className="mt-2 whitespace-pre-wrap text-gray-900">
                                        {c.isDeleted ? "삭제된 댓글입니다." : c.content}
                                    </div>
                                </div>
                            );
                        })}
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
                            disabled={submittingComment}
                            className={[
                                "px-4 py-3 rounded-2xl bg-[#1FBFB8] text-white text-sm font-semibold",
                                "transition-all duration-150",
                                "hover:bg-[#17AFA8] hover:shadow-md hover:-translate-y-[1px]",
                                "active:translate-y-0 active:scale-[0.99]",
                                "disabled:opacity-60 disabled:cursor-not-allowed",
                            ].join(" ")}
                        >
                            등록
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default GroupPostDetailPage;