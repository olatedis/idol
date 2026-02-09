import React, { useEffect, useMemo, useRef, useState } from "react";
import { useParams, useSearchParams } from "react-router-dom";

type Scope = "group" | "idol" | "global";
type BoardKind = "official" | "fan" | "notice";
type Sort = "latest" | "top";
type Size = 20 | 50;

type IdolMember = {
    idolId: string;
    name: string;
    profileImageUrl?: string | null;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

const GROUP_MEMBER_ENDPOINT = (groupId: string) => `/groups/${groupId}/idols`;

async function fetchGroupMembers(groupId: string): Promise<IdolMember[]> {
    const res = await fetch(`${API_BASE_URL}${GROUP_MEMBER_ENDPOINT(groupId)}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
        },
    });

    if (!res.ok) {
        throw new Error(`그룹 멤버 조회 실패: ${res.status}`);
    }

    const data = await res.json();

    // 백엔드 응답 형태가 다를 수 있으므로 최소 방어
    if (Array.isArray(data)) return data;
    if (Array.isArray(data.items)) return data.items;
    return [];
}

const BoardPage: React.FC = () => {
    const { groupId } = useParams();
    const [sp, setSp] = useSearchParams();

    // ----------------------------
    // 1) URL Query
    // ----------------------------
    const scope = (sp.get("scope") as Scope) || "group";
    const board = (sp.get("type") as BoardKind) || "official";
    const sort = (sp.get("sort") as Sort) || "latest";
    const size = (Number(sp.get("size")) as Size) || 20;
    const page = Number(sp.get("page") || "1");
    const q = sp.get("q") || "";
    const selectedIdolId = sp.get("idolId") || "";

    // ----------------------------
    // 2) URL 기본값/정규화
    // ----------------------------
    useEffect(() => {
        const next = new URLSearchParams(sp);

        if (!sp.get("scope")) next.set("scope", "group");
        if (!sp.get("type")) next.set("type", "official");
        if (!sp.get("sort")) next.set("sort", "latest");
        if (!sp.get("size")) next.set("size", "20");
        if (!sp.get("page")) next.set("page", "1");
        if (!sp.get("q")) next.set("q", "");

        const nextType = (next.get("type") as BoardKind) || "official";
        const nextScope = (next.get("scope") as Scope) || "group";

        // 공지사항은 서버 전체 단일 보드: scope=global 고정 + idolId 제거
        if (nextType === "notice") {
            next.set("scope", "global");
            next.delete("idolId");
        }

        // idol scope인데 idolId 없으면 group으로 복귀
        if (nextScope === "idol" && !next.get("idolId")) {
            next.set("scope", "group");
            next.set("type", "official");
        }

        const changed = next.toString() !== sp.toString();
        if (changed) setSp(next, { replace: true });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // 3) 그룹 멤버(아이돌) 리스트: API 연동
    const [members, setMembers] = useState<IdolMember[]>([]);
    const [membersLoading, setMembersLoading] = useState(false);
    const [membersError, setMembersError] = useState<string>("");

    useEffect(() => {
        if (!groupId) return;

        let cancelled = false;
        setMembersLoading(true);
        setMembersError("");

        fetchGroupMembers(groupId)
            .then((list) => {
                if (cancelled) return;
                setMembers(list);
            })
            .catch((e) => {
                if (cancelled) return;
                setMembersError(e instanceof Error ? e.message : "멤버 조회 실패");
            })
            .finally(() => {
                if (cancelled) return;
                setMembersLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [groupId]);

    // 4) 필터 버튼(왼쪽)
    const leftFilters = useMemo(() => {
        const base = [
            { key: "group_official", label: "그룹 공식", next: { scope: "group" as Scope, type: "official" as BoardKind } },
            { key: "group_fan", label: "그룹 팬", next: { scope: "group" as Scope, type: "fan" as BoardKind } },
            // ✅ 공지는 global
            { key: "notice", label: "공지", next: { scope: "global" as Scope, type: "notice" as BoardKind } },
        ];

        const idolExtra = [
            { key: "idol_official", label: "아이돌 공식", next: { scope: "idol" as Scope, type: "official" as BoardKind } },
            { key: "idol_fan", label: "아이돌 팬", next: { scope: "idol" as Scope, type: "fan" as BoardKind } },
        ];

        return scope === "idol" ? [...idolExtra, ...base] : base;
    }, [scope]);

    const isActiveFilter = (f: { next: { scope: Scope; type: BoardKind } }) => {
        // 공지는 어디서 보든 동일: type=notice면 active
        if (f.next.type === "notice") return board === "notice";
        return scope === f.next.scope && board === f.next.type;
    };

    const setFilter = (nextScope: Scope, nextType: BoardKind) => {
        const next = new URLSearchParams(sp);

        next.set("type", nextType);
        next.set("page", "1");

        // 공지: global 고정, idolId 제거
        if (nextType === "notice") {
            next.set("scope", "global");
            next.delete("idolId");
            setSp(next);
            return;
        }

        next.set("scope", nextScope);

        // idol scope면 idolId 필수
        if (nextScope === "idol" && !next.get("idolId")) {
            next.set("scope", "group");
            next.set("type", "official");
        }

        setSp(next);
    };

    // 5) 정렬/사이즈(오른쪽)
    const setSort = (nextSort: Sort) => {
        const next = new URLSearchParams(sp);
        next.set("sort", nextSort);
        next.set("page", "1");
        setSp(next);
    };

    const setSize = (nextSize: Size) => {
        const next = new URLSearchParams(sp);
        next.set("size", String(nextSize));
        next.set("page", "1");
        setSp(next);
    };

    // 6) 아이돌 리스트 UI(가로 스크롤 + 화살표)
    const idolScrollerRef = useRef<HTMLDivElement | null>(null);

    const enterIdolBoard = (clickedIdolId: string) => {
        const next = new URLSearchParams(sp);
        next.set("scope", "idol");
        next.set("idolId", clickedIdolId);
        next.set("type", "official");
        next.set("page", "1");
        setSp(next);
    };

    const scrollIdols = (dir: "left" | "right") => {
        const el = idolScrollerRef.current;
        if (!el) return;
        const step = 220;
        el.scrollBy({ left: dir === "left" ? -step : step, behavior: "smooth" });
    };

    // 7) 표시용 라벨(임시)
    const currentBoardLabel = useMemo(() => {
        if (board === "notice") return "공지";
        if (scope === "idol") return board === "official" ? "아이돌 공식" : "아이돌 팬";
        return board === "official" ? "그룹 공식" : "그룹 팬";
    }, [scope, board]);

    return (
        <div className="space-y-4">
            {/* 1) 아이돌 얼굴 가로 리스트 */}
            <div className="relative">
                <div
                    ref={idolScrollerRef}
                    className="
            flex gap-3 overflow-x-auto scroll-smooth
            py-2
            [scrollbar-width:none]
          "
                    style={{ msOverflowStyle: "none" }}
                >
                    <style>{`
            div::-webkit-scrollbar { display: none; }
          `}</style>

                    {membersLoading && (
                        <div className="text-sm text-gray-600 py-2">멤버 불러오는 중...</div>
                    )}

                    {membersError && (
                        <div className="text-sm text-red-600 py-2">{membersError}</div>
                    )}

                    {!membersLoading && !membersError && members.map((m) => (
                        <button
                            key={m.idolId}
                            onClick={() => enterIdolBoard(m.idolId)}
                            className="flex-shrink-0 w-16 group"
                            title={`${m.name} 게시판으로 이동`}
                        >
                            <div
                                className={[
                                    "w-16 h-16 rounded-full border overflow-hidden",
                                    selectedIdolId === m.idolId ? "border-[#1FBFB8]" : "border-gray-200",
                                    "bg-gray-100",
                                    "group-hover:border-[#1FBFB8]",
                                ].join(" ")}
                            >
                                {m.profileImageUrl ? (
                                    <img
                                        src={m.profileImageUrl}
                                        alt={m.name}
                                        className="w-full h-full object-cover"
                                        loading="lazy"
                                    />
                                ) : null}
                            </div>

                            <div className="mt-1 text-xs text-gray-700 truncate">{m.name}</div>
                        </button>
                    ))}
                </div>

                {/* 화살표 버튼: 기본 숨김, hover 시만 보이는 느낌 */}
                <button
                    type="button"
                    onClick={() => scrollIdols("left")}
                    className="
            absolute left-0 top-1/2 -translate-y-1/2
            w-9 h-9 rounded-full
            bg-white/80 border border-gray-200
            opacity-0 hover:opacity-100
            transition-opacity
            hidden md:flex items-center justify-center
          "
                    aria-label="왼쪽으로"
                    title="왼쪽으로"
                >
                    ◀
                </button>

                <button
                    type="button"
                    onClick={() => scrollIdols("right")}
                    className="
            absolute right-0 top-1/2 -translate-y-1/2
            w-9 h-9 rounded-full
            bg-white/80 border border-gray-200
            opacity-0 hover:opacity-100
            transition-opacity
            hidden md:flex items-center justify-center
          "
                    aria-label="오른쪽으로"
                    title="오른쪽으로"
                >
                    ▶
                </button>
            </div>

            {/* 2) 게시판 툴바: 왼쪽 필터 / 오른쪽 정렬 */}
            <div className="flex items-center justify-between gap-3 flex-wrap">
                {/* 왼쪽: 필터 */}
                <div className="flex items-center gap-2 flex-wrap">
                    {leftFilters.map((f) => (
                        <button
                            key={f.key}
                            type="button"
                            onClick={() => setFilter(f.next.scope, f.next.type)}
                            className={[
                                "px-3 py-2 rounded-full text-sm font-semibold border",
                                isActiveFilter(f)
                                    ? "bg-[#1FBFB8] text-white border-[#1FBFB8] hover:bg-[#17AFA8]"
                                    : "bg-white text-gray-800 border-gray-200 hover:bg-gray-200",
                            ].join(" ")}
                        >
                            {f.label}
                        </button>
                    ))}
                </div>

                {/* 오른쪽: 정렬/사이즈 */}
                <div className="flex items-center gap-2">
                    <div className="flex rounded-full border border-gray-200 overflow-hidden">
                        <button
                            type="button"
                            onClick={() => setSort("latest")}
                            className={[
                                "px-3 py-2 text-sm font-semibold",
                                sort === "latest"
                                    ? "bg-[#1FBFB8] text-white hover:bg-[#17AFA8]"
                                    : "bg-white text-gray-800 hover:bg-gray-200",
                            ].join(" ")}
                        >
                            최신순
                        </button>
                        <button
                            type="button"
                            onClick={() => setSort("top")}
                            className={[
                                "px-3 py-2 text-sm font-semibold border-l border-gray-200",
                                sort === "top"
                                    ? "bg-[#1FBFB8] text-white hover:bg-[#17AFA8]"
                                    : "bg-white text-gray-800 hover:bg-gray-200",
                            ].join(" ")}
                        >
                            추천순
                        </button>
                    </div>

                    <select
                        value={size}
                        onChange={(e) => setSize(Number(e.target.value) as Size)}
                        className="px-3 py-2 rounded-full border border-gray-200 text-sm font-semibold bg-white"
                    >
                        <option value={20}>20개</option>
                        <option value={50}>50개</option>
                    </select>
                </div>
            </div>

            {/* 3) (임시) 현재 선택 상태 표시 */}
            <div className="p-5 rounded-2xl border border-gray-200">
                <div className="text-sm text-gray-700 space-y-1">
                    <div>
                        <span className="font-semibold">현재 보드:</span> {currentBoardLabel}
                    </div>
                    <div>
                        <span className="font-semibold">scope:</span> {scope} / <span className="font-semibold">idolId:</span>{" "}
                        {selectedIdolId || "-"} / <span className="font-semibold">groupId(param):</span> {groupId || "-"}
                    </div>
                    <div>
                        <span className="font-semibold">sort:</span> {sort} / <span className="font-semibold">size:</span> {size} /
                        <span className="font-semibold"> page:</span> {page}
                    </div>
                    <div>
                        <span className="font-semibold">q:</span> {q || "-"}
                    </div>
                </div>
            </div>

            {/* 4) 게시글 리스트/페이지네이션/검색: 다음 단계에서 붙임 */}
        </div>
    );
};

export default BoardPage;
