import { Editor } from "@toast-ui/react-editor";
import React, { useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../../stores/authStore";
import Header from "../Header";

type NoticeWriteRequest = {
    boardType: string;
    idolId: number | null;
    groupId: number | null;
    title: string;
    content: string;
};

/*type PostResponse = {
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

    comments: any[];
};*/

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const NoticeWritePage: React.FC = () => {
    const navigate = useNavigate();

    const [title, setTitle] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    const editorRef = useRef<Editor>(null);

    const onSubmit = async () => {
        setError("");

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

        if (!API_BASE_URL) {
            setError("VITE_API_BASE_URL이 설정되어 있지 않습니다.");
            return;
        }

        // TODO: 로그인 연동되면
        const { accessToken } = useAuthStore.getState();
        if (!accessToken) {
            setError("로그인이 필요합니다. (accessToken 없음)");
            return;
        }

        const req: NoticeWriteRequest = {
            boardType: "ADMIN_NOTICE",
            idolId: null,
            groupId: null,
            title: title.trim(),
            content: html,
        };

        setSubmitting(true);

        try {
            // /board/admin/** -> board-service /admin/**
            const res = await fetch(`${API_BASE_URL}/admin/notices`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${accessToken}`,
                },
                body: JSON.stringify(req),
            });

            if (res.status === 401) throw new Error("로그인이 필요합니다.");
            if (res.status === 403) throw new Error("권한이 없습니다. (ADMIN 전용)");
            if (!res.ok) throw new Error("공지 작성 실패");

            const json = (await res.json()) as any;
            const newPostId = json?.postId;

            if (typeof newPostId === "number") {
                navigate(`/notices/${newPostId}`);
            } else {
                navigate(`/notices`);
            }
        } catch (e: any) {
            setError(e?.message || "공지 작성 실패");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Header />
            <main className="flex-1 w-full max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-24">
                <div className="space-y-4">
                    <div className="border border-gray-200 rounded-2xl bg-white overflow-hidden">
                        <div className="px-5 sm:px-6 py-5 border-b border-gray-100 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                            <div className="text-lg font-semibold text-gray-900">공지 작성(관리자)</div>
                            <div className="flex gap-2 w-full sm:w-auto justify-end">
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
                                    placeholder="공지 제목을 입력해주세요."
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
            </main>
        </div>
    );
};

export default NoticeWritePage;