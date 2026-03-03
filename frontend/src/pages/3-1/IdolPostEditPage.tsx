import React, { useEffect, useMemo, useRef, useState } from "react";
import { Editor } from "@toast-ui/react-editor";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";
import { api } from "../../api/axios";

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

    myReaction?: string;

    createdAt: string;
    updatedAt: string;
};

type PostUpdateRequest = {
    title?: string | null;
    content?: string | null;
};

const IdolPostEditPage: React.FC = () => {
    const { postId } = useParams();
    const navigate = useNavigate();
    const { accessToken, user } = useAuthStore();

    const editorRef = useRef<Editor>(null);

    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    const [title, setTitle] = useState("");

    const canEdit = useMemo(() => {
        if (!user) return false;
        return user.role === "ADMIN" || user.role === "IDOL" || user.role === "AGENCY";
    }, [user]);

    useEffect(() => {
        const run = async () => {
            setError("");

            if (!postId) {
                setError("postId가 없습니다.");
                setLoading(false);
                return;
            }
            if (!accessToken) {
                setError("로그인이 필요합니다.");
                setLoading(false);
                return;
            }
            if (!canEdit) {
                setError("권한이 없습니다.");
                setLoading(false);
                return;
            }

            try {
                setLoading(true);
                const res = await api.get(`/board/posts/${postId}`);
                const data = res.data as PostResponse;

                setTitle(data.title ?? "");

                const inst = editorRef.current?.getInstance();
                if (inst) inst.setHTML(data.content ?? "");
            } catch (e: any) {
                const status = e?.response?.status;
                if (status === 401) setError("로그인이 필요합니다.");
                else if (status === 403) setError("권한이 없습니다.");
                else setError(e?.response?.data?.message || e?.message || "게시글 불러오기 실패");
            } finally {
                setLoading(false);
            }
        };

        run();
    }, [postId, accessToken, canEdit]);

    const onSubmit = async () => {
        setError("");

        if (!postId) return;

        if (!accessToken) {
            setError("로그인이 필요합니다.");
            return;
        }
        if (!canEdit) {
            setError("권한이 없습니다.");
            return;
        }
        if (!title.trim()) {
            setError("제목을 입력해주세요.");
            return;
        }

        const inst = editorRef.current?.getInstance();
        const html = inst?.getHTML()?.trim() ?? "";
        if (!html) {
            setError("내용을 입력해주세요.");
            return;
        }

        if (submitting) return;
        setSubmitting(true);

        try {
            const req: PostUpdateRequest = {
                title: title.trim(),
                content: html,
            };

            await api.put(`/board/posts/${postId}`, req);

            alert("수정되었습니다.");
            navigate(`../`); // 상세로
        } catch (e: any) {
            const status = e?.response?.status;
            if (status === 401) setError("로그인이 필요합니다.");
            else if (status === 403) setError("권한이 없습니다.");
            else setError(e?.response?.data?.message || e?.message || "수정 실패");
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) return <div className="text-sm text-gray-600">불러오는 중...</div>;
    if (error) return <div className="text-sm text-red-600">{error}</div>;

    return (
        <div className="space-y-4">
            <div className="border border-gray-200 rounded-2xl bg-white overflow-hidden">
                <div className="px-6 py-5 border-b border-gray-100 flex items-center justify-between">
                    <div className="text-lg font-semibold text-gray-900">아이돌 공식 글 수정</div>
                    <div className="flex gap-2">
                        <button
                            type="button"
                            onClick={() => navigate(-1)}
                            className="px-4 py-2 rounded-full border border-gray-200 text-sm font-semibold
                         hover:bg-gray-50 hover:border-gray-300 active:scale-[0.99] transition"
                        >
                            취소
                        </button>
                        <button
                            type="button"
                            onClick={onSubmit}
                            disabled={submitting}
                            className="px-4 py-2 rounded-full bg-[#1FBFB8] text-white text-sm font-semibold
                         hover:bg-[#17AFA8] active:scale-[0.99] transition disabled:opacity-60"
                        >
                            {submitting ? "저장 중..." : "저장"}
                        </button>
                    </div>
                </div>

                <div className="px-6 py-5 space-y-4">
                    <div>
                        <div className="text-sm font-semibold text-gray-700 mb-2">제목</div>
                        <input
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            placeholder="제목을 입력해주세요."
                            className="w-full px-4 py-3 rounded-2xl border border-gray-200 text-sm outline-none"
                        />
                    </div>

                    <div>
                        <div className="text-sm font-semibold text-gray-700 mb-2">내용</div>
                        <Editor
                            ref={editorRef}
                            initialValue=""
                            initialEditType="wysiwyg"
                            previewStyle="vertical"
                            height="360px"
                            useCommandShortcut={true}
                        />
                    </div>
                </div>
            </div>
        </div>
    );
};

export default IdolPostEditPage;