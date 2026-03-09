import { Editor } from "@toast-ui/react-editor";
import React, { useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "../../../../stores/authStore.ts";
import { api } from "../../../../api/axios.ts";

type PostWriteRequest = {
    boardType: string;
    idolId: number | null;
    groupId: number | null;
    title: string;
    content: string;
};

const IdolPostWritePage: React.FC = () => {
    const { groupId, idolId } = useParams();
    const navigate = useNavigate();
    const { accessToken, user } = useAuthStore();

    const editorRef = useRef<Editor>(null);

    const [title, setTitle] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    const canWrite = useMemo(() => {
        if (!user) return false;
        return user.role === "ADMIN" || user.role === "IDOL" || user.role === "AGENCY";
    }, [user]);

    if (!accessToken) {
        return <div className="text-sm text-red-600">로그인이 필요합니다.</div>;
    }
    if (user?.status === "RESTRICTED") {
        return <div className="text-sm text-red-600">활동 제한 상태에서는 글을 작성할 수 없습니다.</div>;
    }
    if (!canWrite) {
        return <div className="text-sm text-red-600">권한이 없습니다.</div>;
    }

    const onSubmit = async () => {
        setError("");

        if (!accessToken) return setError("로그인이 필요합니다.");
        if (user?.status === "RESTRICTED") return setError("활동 제한 상태에서는 글을 작성할 수 없습니다.");
        if (!canWrite) return setError("권한이 없습니다.");
        if (!groupId) return setError("groupId가 없습니다.");
        if (!idolId) return setError("idolId가 없습니다.");
        if (!title.trim()) return setError("제목을 입력해주세요.");

        const inst = editorRef.current?.getInstance();
        const md = inst?.getMarkdown().trim() ?? "";
        const html = inst?.getHTML()?.trim() ?? "";

        if (!md || !html) return setError("내용을 입력해주세요.");

        const req: PostWriteRequest = {
            boardType: "IDOL_OFFICIAL",
            idolId: Number(idolId),
            groupId: null,
            title: title.trim(),
            content: html,
        };

        if (submitting) return;
        setSubmitting(true);

        try {
            const res = await api.post("/board/posts", req);
            const newPostId = res.data?.postId;

            if (typeof newPostId === "number") {
                navigate(`/group/${groupId}/idol/${idolId}/board/${newPostId}`);
            } else {
                navigate(`/group/${groupId}/idol/${idolId}/board`);
            }
        } catch (e: any) {
            const status = e?.response?.status;
            if (status === 401) setError("로그인이 필요합니다.");
            else if (status === 403) setError("권한이 없습니다.");
            else setError(e?.response?.data?.message || e?.message || "글 작성 실패");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="space-y-4">
            <div className="border border-gray-200 rounded-2xl bg-white overflow-hidden">
                <div className="px-6 py-5 border-b border-gray-100 flex items-center justify-between">
                    <div className="text-lg font-semibold text-gray-900">아이돌 공식 글쓰기</div>
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
                            {submitting ? "등록 중..." : "등록"}
                        </button>
                    </div>
                </div>

                <div className="px-6 py-5 space-y-4">
                    {error && <div className="text-sm text-red-600">{error}</div>}

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

export default IdolPostWritePage;