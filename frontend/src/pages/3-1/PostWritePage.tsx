import {Editor} from "@toast-ui/react-editor";
import React, { useMemo, useState, useRef } from "react";
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

type PostListResponse = {
    postId: number;
    boardType: string;
    idolId?: number | null;
    groupId?: number | null;

    authorId: number;
    title: string;

    viewCount: number;
    likeCount: number;
    dislikeCount: number;

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

    comments: any[];
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

function resolveBoardType(scope: Scope, type: BoardKind): string {
    if (type === "notice") return "ADMIN_NOTICE";
    if (scope === "idol") return type === "official" ? "IDOL_OFFICIAL" : "IDOL_FAN";
    return type === "official" ? "GROUP_OFFICIAL" : "GROUP_FAN";
}

function readMockPosts(): PostListResponse[] {
    try {
        const raw = localStorage.getItem("mock_posts");
        if (!raw) return [];
        const arr = JSON.parse(raw) as PostListResponse[];
        return Array.isArray(arr) ? arr : [];
    } catch {
        return [];
    }
}

function writeMockPosts(posts: PostListResponse[]) {
    try {
        localStorage.setItem("mock_posts", JSON.stringify(posts));
    } catch {
        // localStorage 실패는 UI 영향 없이 무시
    }
}

function readMockPostDetails(): Record<string, PostResponse> {
    try {
        const raw = localStorage.getItem("mock_post_details");
        if (!raw) return {};
        const map = JSON.parse(raw) as Record<string, PostResponse>;
        return map ?? {};
    } catch {
        return {};
    }
}

function writeMockPostDetails(map: Record<string, PostResponse>) {
    try {
        localStorage.setItem("mock_post_details", JSON.stringify(map));
    } catch {
        // localStorage 실패는 UI 영향 없이 무시
    }
}

function nowStr() {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    const hh = String(d.getHours()).padStart(2, "0");
    const mi = String(d.getMinutes()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
}

const PostWritePage: React.FC = () => {
    const { groupId } = useParams();
    const [sp] = useSearchParams();
    const navigate = useNavigate();

    const USE_MOCK = import.meta.env.VITE_USE_MOCK === "true";

    const scope = (sp.get("scope") as Scope) || "group";
    const board = (sp.get("type") as BoardKind) || "official";
    const idolId = sp.get("idolId");

    const boardType = useMemo(() => resolveBoardType(scope, board), [scope, board]);

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

        const req: PostWriteRequest = {
            boardType,
            idolId: boardType.startsWith("IDOL_") ? Number(idolId) : null,
            groupId: boardType.startsWith("GROUP_") ? Number(groupId) : null,
            title: title.trim(),
            content: html,
        };

        if (USE_MOCK) {
            const createdAt = nowStr();

            const stored = readMockPosts();
            const newId = Date.now();

            const newListItem: PostListResponse = {
                postId: newId,
                boardType: req.boardType,
                idolId: req.idolId,
                groupId: req.groupId,
                authorId: 1,
                title: req.title,
                viewCount: 0,
                likeCount: 0,
                dislikeCount: 0,
                createdAt,
                updatedAt: createdAt,
            };

            writeMockPosts([newListItem, ...stored]);

            const details = readMockPostDetails();
            details[String(newId)] = {
                postId: newId,
                boardType: req.boardType,
                idolId: req.idolId,
                groupId: req.groupId,
                authorId: 1,
                title: req.title,
                content: req.content,
                viewCount: 0,
                likeCount: 0,
                dislikeCount: 0,
                createdAt,
                updatedAt: createdAt,
                comments: [],
            };
            writeMockPostDetails(details);

            navigate(`../${newId}`);
            return;
        }

        if (!API_BASE_URL) return;

        setSubmitting(true);

        // TODO: 로그인 연동되면 실제 값으로 교체
        const userId = "1";
        const userRole = "USER";

        try {
            const res = await fetch(`${API_BASE_URL}/board/posts`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-User-Id": userId,
                    "X-User-Role": userRole,
                },
                body: JSON.stringify(req),
            });

            if (!res.ok) throw new Error("글 작성 실패");

            const json = (await res.json()) as { postId?: number };
            const newPostId = json?.postId;

            if (typeof newPostId === "number") {
                navigate(`../${newPostId}`);
            } else {
                navigate(`../`);
            }
        } catch (e: any) {
            setError(e.message || "글 작성 실패");
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

export default PostWritePage;
