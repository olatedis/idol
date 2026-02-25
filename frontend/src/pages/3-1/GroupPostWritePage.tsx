import { Editor } from "@toast-ui/react-editor";
import React, { useMemo, useRef, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { api } from "../../api/axios"; // ✅ axios 인스턴스 사용 (baseURL/토큰/리프레시 자동)
import { useAuthStore } from "../../stores/authStore"; // ✅ 로그인 UI 체크용 (선택)

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
    const { groupId } = useParams();
    const [sp] = useSearchParams();
    const navigate = useNavigate();

    // type만 유지(official/fan)
    const board = (sp.get("type") as BoardKind) || "official";
    const boardType = useMemo(() => resolveBoardType(board), [board]);

    const [title, setTitle] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    const editorRef = useRef<Editor>(null);

    const { accessToken } = useAuthStore();

    const onSubmit = async () => {
        setError("");

        const gid = Number(groupId);
        if (!Number.isFinite(gid)) {
            setError("잘못된 접근입니다. (groupId 없음)");
            return;
        }

        if (!accessToken) {
            setError("로그인이 필요합니다.");
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
            groupId: gid,
            title: title.trim(),
            content: html,
        };

        setSubmitting(true);

        try {
            // 변경: fetch + API_BASE_URL + localStorage 토큰 제거
            // api(axios)가 baseURL과 Authorization을 자동 처리
            const res = await api.post("/board/posts", req);

            const newPostId = (res.data as any)?.postId;

            if (typeof newPostId === "number") {
                navigate(`../${newPostId}`);
            } else {
                navigate(`../`);
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
                            className="px-4 py-2 rounded-full border border-gray-200 text-sm font-semibold hover:bg-gray-50"
                        >
                            취소
                        </button>
                        <button
                            type="button"
                            onClick={onSubmit}
                            disabled={submitting}
                            className="px-4 py-2 rounded-full bg-[#1FBFB8] text-white text-sm font-semibold hover:bg-[#17AFA8] disabled:opacity-60"
                        >
                            등록
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