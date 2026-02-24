import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

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

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const GroupPostDetailPage: React.FC = () => {
    const { postId } = useParams();
    const navigate = useNavigate();

    const [data, setData] = useState<PostResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const [commentInput, setCommentInput] = useState("");
    const [submittingComment, setSubmittingComment] = useState(false);

    const [reacting, setReacting] = useState(false);

    // TODO: 로그인 연동되면 accessToken 저장 방식/키 확정
    const accessToken = localStorage.getItem("accessToken");

    const fetchDetail = async (signal?: AbortSignal) => {
        if (!API_BASE_URL) {
            throw new Error("VITE_API_BASE_URL이 설정되어 있지 않습니다.");
        }
        if (!postId) {
            throw new Error("postId가 없습니다.");
        }
        if (!accessToken) {
            // 상세 GET은 토큰 필수 정책
            throw new Error("로그인이 필요합니다.");
        }

        const res = await fetch(`${API_BASE_URL}/board/posts/${postId}`, {
            method: "GET",
            headers: {
                Authorization: `Bearer ${accessToken}`,
            },
            signal,
        });

        if (res.status === 401) throw new Error("로그인이 필요합니다.");
        if (res.status === 403) throw new Error("권한이 없습니다. (구독 필요 또는 접근 불가)");
        if (!res.ok) throw new Error("게시글 상세 조회 실패");

        const json = (await res.json()) as PostResponse;

        return {
            ...json,
            comments: Array.isArray(json.comments) ? json.comments : [],
            myReaction: (json.myReaction || "NONE") as string,
        };
    };

    useEffect(() => {
        const controller = new AbortController();

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
                const detail = await fetchDetail(controller.signal);
                setData(detail);
            } catch (e: any) {
                if (e?.name === "AbortError") return;
                setError(e?.message || "게시글 상세 조회 실패");
                setData(null);
            } finally {
                setLoading(false);
            }
        };

        run();
        return () => controller.abort();
    }, [API_BASE_URL, postId, accessToken]);

    const onClickLike = async () => {
        if (!data) return;
        if (!API_BASE_URL) return;
        if (!postId) return;

        if (!accessToken) {
            alert("로그인이 필요합니다.");
            return;
        }

        if (reacting) return;

        setReacting(true);

        try {
            const res = await fetch(`${API_BASE_URL}/board/posts/${postId}/like`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
            });

            if (res.status === 401) throw new Error("로그인이 필요합니다.");
            if (res.status === 403) throw new Error("권한이 없습니다.");
            if (!res.ok) throw new Error("추천 처리 실패");

            const json = (await res.json()) as PostReactionResponse;

            // 서버 응답으로 동기화
            setData((prev) => {
                if (!prev) return prev;
                return {
                    ...prev,
                    likeCount: json.likeCount,
                    dislikeCount: json.dislikeCount,
                    myReaction: json.myReaction || "NONE",
                };
            });
        } catch (e: any) {
            alert(e?.message || "추천 처리 실패");
        } finally {
            setReacting(false);
        }
    };

    const onClickDislike = async () => {
        if (!data) return;
        if (!API_BASE_URL) return;
        if (!postId) return;

        if (!accessToken) {
            alert("로그인이 필요합니다.");
            return;
        }

        if (reacting) return;

        setReacting(true);

        try {
            const res = await fetch(`${API_BASE_URL}/board/posts/${postId}/dislike`, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
            });

            if (res.status === 401) throw new Error("로그인이 필요합니다.");
            if (res.status === 403) throw new Error("권한이 없습니다.");
            if (!res.ok) throw new Error("비추천 처리 실패");

            const json = (await res.json()) as PostReactionResponse;

            // 서버 응답으로 동기화
            setData((prev) => {
                if (!prev) return prev;
                return {
                    ...prev,
                    likeCount: json.likeCount,
                    dislikeCount: json.dislikeCount,
                    myReaction: json.myReaction || "NONE",
                };
            });
        } catch (e: any) {
            alert(e?.message || "비추천 처리 실패");
        } finally {
            setReacting(false);
        }
    };

    const onSubmitComment = async () => {
        if (!data) return;
        if (!API_BASE_URL) return;
        if (!postId) return;

        if (!commentInput.trim()) return;

        if (!accessToken) {
            alert("로그인이 필요합니다.");
            return;
        }

        if (submittingComment) return;

        setSubmittingComment(true);

        try {
            const res = await fetch(`${API_BASE_URL}/board/posts/${postId}/comments`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${accessToken}`,
                },
                body: JSON.stringify({
                    content: commentInput.trim(),
                }),
            });

            if (res.status === 401) throw new Error("로그인이 필요합니다.");
            if (res.status === 403) throw new Error("권한이 없습니다.");
            if (!res.ok) throw new Error("댓글 작성 실패");

            setCommentInput("");

            // 댓글 작성 후 "상세 재조회"로 완전 동기화
            const detail = await fetchDetail();
            setData(detail);
        } catch (e: any) {
            alert(e?.message || "댓글 작성 실패");
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
                                "w-16 h-16 rounded-full border flex flex-col items-center justify-center transition-colors disabled:opacity-60",
                                likeActive
                                    ? "bg-[#1FBFB8] border-[#1FBFB8] text-white"
                                    : "bg-white border-gray-300 text-gray-900 hover:bg-gray-50",
                            ].join(" ")}
                        >
                            <span className="text-xl">👍</span>
                            <span className="text-sm mt-1">{data.likeCount}</span>
                        </button>

                        <button
                            type="button"
                            onClick={onClickDislike}
                            disabled={reacting}
                            className={[
                                "w-16 h-16 rounded-full border flex flex-col items-center justify-center transition-colors disabled:opacity-60",
                                dislikeActive
                                    ? "bg-[#1FBFB8] border-[#1FBFB8] text-white"
                                    : "bg-white border-gray-300 text-gray-900 hover:bg-gray-50",
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

export default GroupPostDetailPage;