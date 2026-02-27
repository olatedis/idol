import React, { useEffect, useMemo, useState } from "react";
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

const GroupPostDetailPage: React.FC = () => {
    const { postId } = useParams();
    const navigate = useNavigate();

    const { accessToken, user } = useAuthStore();

    const [data, setData] = useState<PostResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const [commentInput, setCommentInput] = useState("");
    const [submittingComment, setSubmittingComment] = useState(false);

    const [reacting, setReacting] = useState(false);
    const [deleting, setDeleting] = useState(false);

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

    useEffect(() => {
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
                const detail = await fetchDetail();
                setData(detail);
            } catch (e: any) {
                const status = e?.response?.status;
                if (status === 401) setError("로그인이 필요합니다.");
                else if (status === 403) setError("권한이 없습니다. (구독 필요 또는 접근 불가)");
                else setError(e?.response?.data?.message || e?.message || "게시글 상세 조회 실패");
                setData(null);
            } finally {
                setLoading(false);
            }
        };

        run();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [postId, accessToken]);

    const canEditOrDelete = useMemo(() => {
        if (!data || !user) return false;

        const role = user.role;
        const bt = data.boardType;

        // ADMIN: 전부 가능
        if (role === "ADMIN") return true;

        // ADMIN_NOTICE: ADMIN만
        if (bt === "ADMIN_NOTICE") return false;

        // FAN: USER는 본인 글만
        if ((bt === "IDOL_FAN" || bt === "GROUP_FAN") && role === "USER") {
            return Number(data.authorId) === Number(user.userId);
        }

        // OFFICIAL: IDOL/AGENCY는 일단 버튼 보여주기(소속 검증은 백엔드에서)
        if ((bt === "IDOL_OFFICIAL" || bt === "GROUP_OFFICIAL") && (role === "IDOL" || role === "AGENCY")) {
            return true;
        }

        return false;
    }, [data, user]);

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
                    likeCount: json.likeCount,
                    dislikeCount: json.dislikeCount,
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
                    likeCount: json.likeCount,
                    dislikeCount: json.dislikeCount,
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

            const detail = await fetchDetail();
            setData(detail);
        } catch (e: any) {
            const status = e?.response?.status;
            if (status === 401) alert("로그인이 필요합니다.");
            else if (status === 403) alert("권한이 없습니다.");
            else alert(e?.response?.data?.message || e?.message || "댓글 작성 실패");
        } finally {
            setSubmittingComment(false);
        }
    };

    const onClickEdit = () => {
        if (!postId) return;
        navigate(`./edit`);
    };

    const onClickDelete = async () => {
        if (!postId) return;
        if (deleting) return;

        const ok = window.confirm("정말 삭제하시겠습니까?");
        if (!ok) return;

        setDeleting(true);
        try {
            await api.delete(`/board/posts/${postId}`);
            alert("삭제되었습니다.");
            navigate(`../`);
        } catch (e: any) {
            const status = e?.response?.status;
            if (status === 401) alert("로그인이 필요합니다.");
            else if (status === 403) alert("권한이 없습니다.");
            else alert(e?.response?.data?.message || e?.message || "삭제 실패");
        } finally {
            setDeleting(false);
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
                    <div className="flex items-start justify-between gap-4">
                        <div className="text-2xl font-semibold text-gray-900">{data.title}</div>

                        {/* 수정/삭제 버튼 */}
                        {canEditOrDelete && (
                            <div className="flex gap-2">
                                <button
                                    type="button"
                                    onClick={onClickEdit}
                                    className="px-4 py-2 rounded-full border border-gray-200 text-sm font-semibold
                             hover:bg-gray-50 hover:border-gray-300 active:scale-[0.99] transition"
                                >
                                    수정
                                </button>
                                <button
                                    type="button"
                                    onClick={onClickDelete}
                                    disabled={deleting}
                                    className="px-4 py-2 rounded-full border border-red-200 text-sm font-semibold text-red-600
                             hover:bg-red-50 hover:border-red-300 active:scale-[0.99] transition disabled:opacity-60"
                                >
                                    {deleting ? "삭제 중..." : "삭제"}
                                </button>
                            </div>
                        )}
                    </div>

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
                            disabled={reacting}
                            className={[
                                "w-16 h-16 rounded-full border flex flex-col items-center justify-center transition",
                                "hover:-translate-y-[1px] hover:shadow-sm active:translate-y-0",
                                "disabled:opacity-60",
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
                                "w-16 h-16 rounded-full border flex flex-col items-center justify-center transition",
                                "hover:-translate-y-[1px] hover:shadow-sm active:translate-y-0",
                                "disabled:opacity-60",
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
                            className="px-4 py-2 rounded-full border border-gray-200 text-sm font-semibold
                         hover:bg-gray-50 hover:border-gray-300 active:scale-[0.99] transition"
                        >
                            목록으로
                        </button>
                    </div>
                </div>
            </div>

            {/* 댓글 */}
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
                            className="px-4 py-3 rounded-2xl bg-[#1FBFB8] text-white text-sm font-semibold
                         hover:bg-[#17AFA8] active:scale-[0.99] transition disabled:opacity-60"
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