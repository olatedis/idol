import { Editor } from "@toast-ui/react-editor";
import React, { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

type NoticeDetail = {
    postId: number;
    title: string;
    content: string;
    createdAt: string;
    updatedAt: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const NoticeEditPage: React.FC = () => {
    const { postId } = useParams();
    const navigate = useNavigate();

    const editorRef = useRef<Editor>(null);

    const [title, setTitle] = useState("");
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    // TODO: 로그인 구조 확정되면 교체
    const accessToken = localStorage.getItem("accessToken");
    const userRole = localStorage.getItem("role");
    const isAdmin = userRole === "ADMIN";

    useEffect(() => {
        if (!postId) return;

        const controller = new AbortController();

        const run = async () => {
            setLoading(true);
            setError("");

            try {
                const res = await fetch(`${API_BASE_URL}/board/notices/${postId}`, {
                    method: "GET",
                    signal: controller.signal,
                });

                if (!res.ok) throw new Error("공지 불러오기 실패");

                const json: NoticeDetail = await res.json();
                setTitle(json.title);

                // 에디터에 기존 내용 세팅
                setTimeout(() => {
                    editorRef.current?.getInstance().setHTML(json.content);
                }, 0);
            } catch (e: any) {
                if (e?.name === "AbortError") return;
                setError(e?.message || "공지 불러오기 실패");
            } finally {
                setLoading(false);
            }
        };

        run();
        return () => controller.abort();
    }, [postId]);

    const handleUpdate = async () => {
        if (!accessToken) {
            setError("로그인이 필요합니다.");
            return;
        }

        if (!isAdmin) {
            setError("관리자 권한이 필요합니다.");
            return;
        }

        const instance = editorRef.current?.getInstance();
        const md = instance?.getMarkdown().trim() ?? "";
        const html = instance?.getHTML() ?? "";

        if (!title.trim()) {
            setError("제목을 입력해주세요.");
            return;
        }

        if (!md) {
            setError("내용을 입력해주세요.");
            return;
        }

        setSubmitting(true);
        setError("");

        try {
            const res = await fetch(`${API_BASE_URL}/board/admin/notices/${postId}`, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${accessToken}`,
                },
                body: JSON.stringify({
                    title: title.trim(),
                    content: html,
                }),
            });

            if (res.status === 403) {
                throw new Error("권한이 없습니다.");
            }

            if (!res.ok) {
                throw new Error("공지 수정 실패");
            }

            navigate(`/notices/${postId}`);
        } catch (e: any) {
            setError(e?.message || "공지 수정 실패");
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) return <div>불러오는 중...</div>;

    return (
        <div className="space-y-4">
            <div className="border border-gray-200 rounded-2xl bg-white overflow-hidden">
                <div className="px-6 py-5 border-b flex justify-between">
                    <div className="text-lg font-semibold">공지 수정 (관리자)</div>
                    <div className="flex gap-2">
                        <button
                            onClick={() => navigate(-1)}
                            className="px-4 py-2 rounded-full border text-sm"
                        >
                            취소
                        </button>
                        <button
                            onClick={handleUpdate}
                            disabled={submitting}
                            className="px-4 py-2 rounded-full bg-[#1FBFB8] text-white text-sm disabled:opacity-60"
                        >
                            수정 완료
                        </button>
                    </div>
                </div>

                <div className="px-6 py-5 space-y-4">
                    {error && <div className="text-sm text-red-600">{error}</div>}

                    <div>
                        <div className="text-sm font-semibold mb-2">제목</div>
                        <input
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            className="w-full px-4 py-3 rounded-2xl border text-sm"
                        />
                    </div>

                    <div>
                        <div className="text-sm font-semibold mb-2">내용</div>
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

export default NoticeEditPage;
