import React, { useEffect, useRef, useState } from "react";
import { Editor } from "@toast-ui/react-editor";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "../../../stores/authStore.ts";
import { api } from "../../../api/axios.ts";
import { showSuccessToast } from "../../../utils/alert";

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
};

type PostUpdateRequest = {
    title?: string | null;
    content?: string | null;
};

const GroupPostEditPage: React.FC = () => {
    const { postId } = useParams();
    const navigate = useNavigate();
    const { accessToken } = useAuthStore();

    const editorRef = useRef<Editor>(null);

    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    const [title, setTitle] = useState("");
    const [contentHtml, setContentHtml] = useState("");
    const [contentInjected, setContentInjected] = useState(false);

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

            try {
                setLoading(true);
                const res = await api.get(`/board/posts/${postId}`);
                const data = res.data as PostResponse;

                setTitle(data.title ?? "");
                setContentHtml(data.content ?? "");
                setContentInjected(false);

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
    }, [postId, accessToken]);

    // ✅ editor가 준비된 뒤 contentHtml을 1번 주입
    useEffect(() => {
        if (loading) return;
        if (contentInjected) return;

        const inst = editorRef.current?.getInstance();
        if (!inst) return;

        // Toast UI는 마운트 타이밍에 setHTML이 씹히는 경우가 있어 next tick에 한번 더 보장
        setTimeout(() => {
            const inst2 = editorRef.current?.getInstance();
            if (!inst2) return;

            inst2.setHTML(contentHtml || "");
            setContentInjected(true);
        }, 0);
    }, [loading, contentHtml, contentInjected]);


    const onSubmit = async () => {
        setError("");

        if (!postId) return;
        if (!accessToken) {
            setError("로그인이 필요합니다.");
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

            // alert("수정되었습니다.");
            showSuccessToast("게시글이 성공적으로 수정되었습니다.");
            navigate(`../`);
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
                <div className="px-4 sm:px-6 py-4 sm:py-5 border-b border-gray-100 flex items-center justify-between gap-3 flex-wrap">
                    <div className="text-lg font-semibold text-gray-900">글 수정</div>
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
                            className="px-4 py-2 rounded-full bg-[var(--color-idol-mid)] text-white text-sm font-semibold
                            hover:bg-[var(--color-idol-dark)] active:scale-[0.99] transition disabled:opacity-60"
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
                        <div className="overflow-x-hidden">
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
        </div>
    );
};

export default GroupPostEditPage;