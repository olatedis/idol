import {Editor} from "@toast-ui/react-editor";
import React, {useEffect, useMemo, useRef, useState} from "react";
import {useNavigate, useParams, useSearchParams} from "react-router-dom";
import {api} from "../../../api/axios.ts";
import {useAuthStore} from "../../../stores/authStore.ts";

type BoardKind = "official" | "fan";

type PostWriteRequest = {
    boardType: string;
    idolId: number | null;
    groupId: number | null;
    title: string;
    content: string;
};

function resolveBoardType(type: BoardKind): string {
    return type === "official" ? "GROUP_OFFICIAL" : "GROUP_FAN";
}

const GroupPostWritePage: React.FC = () => {
    const {groupId} = useParams();
    const [sp] = useSearchParams();
    const navigate = useNavigate();

    // type만 유지(official/fan)
    const board = (sp.get("type") as BoardKind) || "official";
    const boardType = useMemo(() => resolveBoardType(board), [board]);

    const [title, setTitle] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    const editorRef = useRef<Editor>(null);

    const {accessToken, user} = useAuthStore();

    // USER는 GROUP_OFFICIAL 글쓰기 진입 자체 차단 (/write 직접 접근 포함)
    useEffect(() => {
        if (!accessToken || !user) return;

        if (user.status === "RESTRICTED") {
            alert("활동 제한 상태에서는 글을 작성할 수 없습니다.");
            navigate(-1);
            return;
        }

        if (boardType === "GROUP_OFFICIAL" && user.role === "USER") {
            alert("권한이 없습니다. (그룹 공식 글쓰기는 USER가 작성할 수 없습니다.)");
            navigate(-1);
        }
    }, [accessToken, user, boardType, navigate]);

    const onSubmit = async () => {
        setError("");

        if (!accessToken || !user) {
            setError("로그인이 필요합니다.");
            return;
        }

        if (user.status === "RESTRICTED") {
            setError("활동 제한 상태에서는 글을 작성할 수 없습니다.");
            return;
        }

        // 등록 시에도 한 번 더 차단
        if (boardType === "GROUP_OFFICIAL" && user.role === "USER") {
            setError("권한이 없습니다. (그룹 공식 글쓰기는 USER가 작성할 수 없습니다.)");
            return;
        }

        if (!groupId) {
            setError("잘못된 접근입니다. (groupId 없음)");
            return;
        }

        if (!title.trim()) {
            setError("제목을 입력해주세요.");
            return;
        }

        const instance = editorRef.current?.getInstance();
        const md = instance?.getMarkdown().trim() ?? "";
        const html = instance?.getHTML() ?? "";

        if (!md) {
            setError("내용을 입력해주세요.");
            return;
        }

        const req: PostWriteRequest = {
            boardType,
            idolId: null,
            groupId: Number(groupId),
            title: title.trim(),
            content: html,
        };

        if (submitting) return;
        setSubmitting(true);

        try {
            const res = await api.post("/board/posts", req);

            const json = res.data as any;
            const newPostId = json?.postId;

            if (typeof newPostId === "number") {
                navigate(`/group/${groupId}/board/${newPostId}`);
            } else {
                navigate(`/group/${groupId}/board`);
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
                    <div className="text-lg font-semibold text-gray-900">글쓰기</div>
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

export default GroupPostWritePage;