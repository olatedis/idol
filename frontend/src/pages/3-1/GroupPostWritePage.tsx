import { Editor } from "@toast-ui/react-editor";
import React, { useMemo, useRef, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";

type Scope = "group" | "idol" | "global";
type BoardKind = "official" | "fan" | "notice";

type PostWriteRequest = {
    boardType: string;
    idolId: number | null;
    groupId: number | null;
    title: string;
    content: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

function resolveBoardType(scope: Scope, type: BoardKind): string {
    if (type === "notice") return "ADMIN_NOTICE";
    if (scope === "idol") return type === "official" ? "IDOL_OFFICIAL" : "IDOL_FAN";
    return type === "official" ? "GROUP_OFFICIAL" : "GROUP_FAN";
}

const GroupPostWritePage: React.FC = () => {
    const { groupId } = useParams();
    const [sp] = useSearchParams();
    const navigate = useNavigate();

    const scope = (sp.get("scope") as Scope) || "group";
    const board = (sp.get("type") as BoardKind) || "official";
    const idolId = sp.get("idolId");

    const boardType = useMemo(() => resolveBoardType(scope, board), [scope, board]);

    const [title, setTitle] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    const editorRef = useRef<Editor>(null);

    // TODO: 로그인 연동되면 accessToken 저장 방식/키 확정
    const accessToken = localStorage.getItem("accessToken");

    const onSubmit = async () => {
        setError("");

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
            idolId: boardType.startsWith("IDOL_") ? Number(idolId) : null,
            groupId: boardType.startsWith("GROUP_") ? Number(groupId) : null,
            title: title.trim(),
            content: html,
        };

        if (!API_BASE_URL) {
            setError("VITE_API_BASE_URL이 설정되어 있지 않습니다.");
            return;
        }

        setSubmitting(true);

        try {
            const res = await fetch(`${API_BASE_URL}/board/posts`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${accessToken}`,
                },
                body: JSON.stringify(req),
            });

            if (res.status === 401) throw new Error("로그인이 필요합니다.");
            if (res.status === 403) throw new Error("권한이 없습니다.");
            if (!res.ok) throw new Error("글 작성 실패");

            const json = (await res.json()) as any;
            const newPostId = json?.postId;

            if (typeof newPostId === "number") {
                navigate(`../${newPostId}`);
            } else {
                navigate(`../`);
            }
        } catch (e: any) {
            setError(e?.message || "글 작성 실패");
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