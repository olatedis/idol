import React, { useEffect, useMemo, useState } from "react";
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

function pad2(n: number) {
    return String(n).padStart(2, "0");
}

function makeCreatedAtForId(id: number) {
    const day = 1 + ((id - 1) % 28);
    const min = (id * 7) % 60;
    return `2026-02-${pad2(day)} 12:${pad2(min)}`;
}

function readMockPostDetail(postId: number): PostResponse | null {
    try {
        const raw = localStorage.getItem("mock_post_details");
        if (!raw) return null;
        const map = JSON.parse(raw) as Record<string, PostResponse>;
        return map[String(postId)] ?? null;
    } catch {
        return null;
    }
}

function writeMockPostDetail(post: PostResponse) {
    try {
        const raw = localStorage.getItem("mock_post_details");
        const map = (raw ? (JSON.parse(raw) as Record<string, PostResponse>) : {}) as Record<string, PostResponse>;
        map[String(post.postId)] = post;
        localStorage.setItem("mock_post_details", JSON.stringify(map));
    } catch {
        // localStorage 실패는 UI 영향 없이 무시
    }
}

const PostDetailPage: React.FC = () => {
    const { groupId, postId } = useParams();
    const navigate = useNavigate();

    const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

    const [data, setData] = useState<PostResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const [commentInput, setCommentInput] = useState("");

    // 추천/비추천 UI 상태(토글)
    const [myReaction, setMyReaction] = useState<"like" | "dislike" | null>(null);

    const mockFallback = useMemo<PostResponse>(() => {
        const id = Number(postId || "1");
        const createdAt = makeCreatedAtForId(id);

        return {
            postId: id,
            boardType: "GROUP_OFFICIAL",
            idolId: null,
            groupId: Number(groupId || "1"),
            authorId: 101,
            title: `더미 상세 제목 ${id}`,
            content:
                "이건 더미 본문입니다.\n\n프런트 UI 확인용으로 줄바꿈/길이 테스트를 합니다.\n\n추천/비추천 버튼과 댓글 영역까지 배치만 확인하면 됩니다.",
            viewCount: 1643,
            likeCount: 12,
            dislikeCount: 1,
            createdAt,
            updatedAt: createdAt,
            comments: [
                {
                    commentId: 1,
                    authorId: 201,
                    isDeleted: false,
                    content: "댓글 1 입니다.",
                    createdAt: "2026-02-06 14:06",
                    updatedAt: "2026-02-06 14:06",
                },
                {
                    commentId: 2,
                    authorId: 202,
                    isDeleted: false,
                    content: "댓글 2 입니다.\n줄바꿈도 됩니다.",
                    createdAt: "2026-02-06 15:04",
                    updatedAt: "2026-02-06 15:04",
                },
                {
                    commentId: 3,
                    authorId: 203,
                    isDeleted: true,
                    content: "삭제된 댓글입니다",
                    createdAt: "2026-02-06 18:55",
                    updatedAt: "2026-02-06 18:55",
                },
            ],
        };
    }, [postId, groupId]);

    useEffect(() => {
        if (!postId) {
            setError("postId가 없습니다.");
            setData(null);
            return;
        }

        if (USE_MOCK) {
            setLoading(false);
            setError("");

            const id = Number(postId);
            const stored = readMockPostDetail(id);
            const picked = stored ?? mockFallback;

            setData(picked);
            setMyReaction(null);
            return;
        }

        if (!API_BASE_URL) return;

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
                setMyReaction(null);
            })
            .catch((e) => {
                if (e.name === "AbortError") return;
                setError(e.message);
                setData(null);
            })
            .finally(() => setLoading(false));

        return () => controller.abort();
    }, [API_BASE_URL, USE_MOCK, postId, mockFallback]);

    const onClickLike = () => {
        if (!data) return;

        setData((prev) => {
            if (!prev) return prev;

            const next = { ...prev };

            if (myReaction === "like") {
                next.likeCount = Math.max(0, next.likeCount - 1);
                setMyReaction(null);
            } else if (myReaction === "dislike") {
                next.dislikeCount = Math.max(0, next.dislikeCount - 1);
                next.likeCount = next.likeCount + 1;
                setMyReaction("like");
            } else {
                next.likeCount = next.likeCount + 1;
                setMyReaction("like");
            }

            if (USE_MOCK) writeMockPostDetail(next);
            return next;
        });

        // TODO: 추천 API 연동
    };

    const onClickDislike = () => {
        if (!data) return;

        setData((prev) => {
            if (!prev) return prev;

            const next = { ...prev };

            if (myReaction === "dislike") {
                next.dislikeCount = Math.max(0, next.dislikeCount - 1);
                setMyReaction(null);
            } else if (myReaction === "like") {
                next.likeCount = Math.max(0, next.likeCount - 1);
                next.dislikeCount = next.dislikeCount + 1;
                setMyReaction("dislike");
            } else {
                next.dislikeCount = next.dislikeCount + 1;
                setMyReaction("dislike");
            }

            if (USE_MOCK) writeMockPostDetail(next);
            return next;
        });

        // TODO: 비추천 API 연동
    };

    const onSubmitComment = () => {
        if (!data) return;
        if (!commentInput.trim()) return;

        const newComment: CommentResponse = {
            commentId: Date.now(),
            authorId: 999,
            isDeleted: false,
            content: commentInput,
            createdAt: "방금 전",
            updatedAt: "방금 전",
        };

        setData((prev) => {
            if (!prev) return prev;
            const next = { ...prev, comments: [newComment, ...prev.comments] };
            if (USE_MOCK) writeMockPostDetail(next);
            return next;
        });

        setCommentInput("");

        // TODO: 댓글 작성 API 연동
    };

    if (loading) return <div className="text-sm text-gray-600">불러오는 중...</div>;
    if (error) return <div className="text-sm text-red-600">{error}</div>;
    if (!data) return <div className="text-sm text-gray-600">데이터가 없습니다.</div>;

    const likeActive = myReaction === "like";
    const dislikeActive = myReaction === "dislike";

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
                    <div className="whitespace-pre-wrap text-gray-900 leading-relaxed">{data.content}</div>

                    <div className="mt-8 flex justify-center gap-10">
                        <button
                            type="button"
                            onClick={onClickLike}
                            className={[
                                "w-16 h-16 rounded-full border flex flex-col items-center justify-center transition-colors",
                                likeActive ? "bg-[#1FBFB8] border-[#1FBFB8] text-white" : "bg-white border-gray-300 text-gray-900 hover:bg-gray-50",
                            ].join(" ")}
                        >
                            <span className="text-xl">👍</span>
                            <span className="text-sm mt-1">{data.likeCount}</span>
                        </button>

                        <button
                            type="button"
                            onClick={onClickDislike}
                            className={[
                                "w-16 h-16 rounded-full border flex flex-col items-center justify-center transition-colors",
                                dislikeActive ? "bg-[#1FBFB8] border-[#1FBFB8] text-white" : "bg-white border-gray-300 text-gray-900 hover:bg-gray-50",
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
