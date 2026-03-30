import React, { useEffect, useMemo, useState } from "react";
import {
    deleteManyIdolMessageStacks,
    deleteManyNotifications,
    getIdolMessageStacks,
    getNotificationList,
} from "../../../api/notificationApi";
import type { IdolMessageStackPayload, NotificationItem } from "../../../types/notification";
import { showErrorToast } from "../../../utils/alert";

type FilterType =
    | "all"
    | "vote"
    | "subscription"
    | "chat"
    | "concert"
    | "system";

type HistoryItem = NotificationItem & {
    historyKind?: "notification" | "stack";
    stackUnreadCount?: number;
};

const PAGE_SIZE = 10;
const FETCH_SIZE = 50;

const NotificationHistoryTab: React.FC = () => {
    const [notifications, setNotifications] = useState<NotificationItem[]>([]);
    const [idolStacks, setIdolStacks] = useState<IdolMessageStackPayload[]>([]);

    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [nextCursor, setNextCursor] = useState<string | null>(null);
    const [hasNext, setHasNext] = useState(false);

    const [selectedIds, setSelectedIds] = useState<number[]>([]);
    const [filter, setFilter] = useState<FilterType>("all");
    const [page, setPage] = useState(1);

    const loadInitial = async () => {
        try {
            setLoading(true);

            const [notificationData, stackData] = await Promise.all([
                getNotificationList(FETCH_SIZE),
                getIdolMessageStacks(),
            ]);

            setNotifications(notificationData.items ?? []);
            setNextCursor(notificationData.nextCursor ?? null);
            setHasNext(notificationData.hasNext ?? false);
            setIdolStacks(stackData.items ?? []);
        } catch (error) {
            showErrorToast("알림 히스토리를 불러오는 중 오류가 발생했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadInitial();
    }, []);

    const localTypes = new Set([
        "CHAT_IDOL_ONLINE",
        "IDOL_MESSAGE",
        "REPLY_MESSAGE",
    ]);

    const formatNotificationTimeToKST = (value?: string | null, type?: string) => {
        if (!value) return "";

        const date = localTypes.has(type ?? "")
            ? new Date(value)        // chat-service: 로컬(KST) 시간으로 전송
            : new Date(`${value}Z`); // 그 외 서비스: UTC로 전송

        if (Number.isNaN(date.getTime())) return value;

        return new Intl.DateTimeFormat("ko-KR", {
            timeZone: "Asia/Seoul",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            hour12: false,
        }).format(date);
    };

    const parseIdolId = (notification: NotificationItem) => {
        const idolIdFromArgs = notification.args?.idolId;
        if (idolIdFromArgs) return Number(idolIdFromArgs);

        const match = notification.redirectUrl?.match(/idolId=(\d+)/);
        if (match) return Number(match[1]);

        return null;
    };

    const latestIdolMessageMap = useMemo(() => {
        const map = new Map<number, NotificationItem>();

        notifications
            .filter((item) => item.type === "IDOL_MESSAGE")
            .forEach((item) => {
                const idolId = parseIdolId(item);
                if (!idolId) return;

                const prev = map.get(idolId);
                if (!prev) {
                    map.set(idolId, item);
                    return;
                }

                const prevTime = prev.occurredAt ? new Date(prev.occurredAt).getTime() : 0;
                const currentTime = item.occurredAt ? new Date(item.occurredAt).getTime() : 0;

                if (currentTime > prevTime) {
                    map.set(idolId, item);
                }
            });

        return map;
    }, [notifications]);

    const historyItems = useMemo<HistoryItem[]>(() => {
        const stackHistoryItems: HistoryItem[] = idolStacks
            .filter((stack) => !!stack.lastOccurredAt)
            .map((stack) => {
                const latest = latestIdolMessageMap.get(stack.idolId);

                return {
                    notificationId: -100000 - stack.idolId,
                    eventId: `idol-stack-${stack.idolId}`,
                    type: "IDOL_MESSAGE_STACK",
                    targetType: "USER",
                    targetId: "",
                    redirectUrl: "#",
                    occurredAt: stack.lastOccurredAt ?? "",
                    args: {
                        idolId: String(stack.idolId),
                        idolName: latest?.args?.idolName ?? `아이돌 ${stack.idolId}`,
                        groupName: latest?.args?.groupName ?? "그룹",
                        unreadCount: String(stack.unreadCount ?? 0),
                    },
                    isRead: true,
                    readAt: null,
                    historyKind: "stack",
                    stackUnreadCount: stack.unreadCount ?? 0,
                };
            });

        const normalNotificationItems: HistoryItem[] = notifications
            .filter((item) => item.type !== "IDOL_MESSAGE")
            .map((item) => ({
                ...item,
                historyKind: "notification",
            }));

        return [...stackHistoryItems, ...normalNotificationItems].sort((a, b) => {
            const aTime = a.occurredAt ? new Date(a.occurredAt).getTime() : 0;
            const bTime = b.occurredAt ? new Date(b.occurredAt).getTime() : 0;
            return bTime - aTime;
        });
    }, [idolStacks, latestIdolMessageMap, notifications]);

    const getCategory = (item: HistoryItem): FilterType => {
        const type = item.type;

        if (
            type === "VOTE_OPENED" ||
            type === "VOTE_CLOSED" ||
            type === "VOTE_CLOSING_SOON" ||
            type === "MY_VOTE_SUBMITTED" ||
            type === "RANKING_CHANGED" ||
            type === "VOTE_RESULT"
        ) {
            return "vote";
        }

        if (
            type === "IDOL_SUB_STARTED" ||
            type === "IDOL_SUB_END" ||
            type === "GROUP_SUB_STARTED" ||
            type === "GROUP_SUB_END"
        ) {
            return "subscription";
        }

        if (
            type === "CHAT_IDOL_ONLINE" ||
            type === "IDOL_MESSAGE_STACK"
        ) {
            return "chat";
        }

        if (
            type === "CONCERT_OPENED" ||
            type === "RESERVATION_CREATED"
        ) {
            return "concert";
        }

        return "system";
    };

    const filteredItems = useMemo(() => {
        if (filter === "all") return historyItems;
        return historyItems.filter((item) => getCategory(item) === filter);
    }, [historyItems, filter]);

    const totalPages = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE));

    const pagedItems = useMemo(() => {
        const start = (page - 1) * PAGE_SIZE;
        const end = start + PAGE_SIZE;
        return filteredItems.slice(start, end);
    }, [filteredItems, page]);

    const getNotificationTitle = (notification: HistoryItem) => {
        const voteTitle = notification.args?.voteTitle;
        const boardTitle = notification.args?.title;
        const idolName = notification.args?.idolName;
        const groupName = notification.args?.groupName;
        const concertName = notification.args?.concertName;
        const idolId = notification.args?.idolId;
        const groupId = notification.args?.groupId;
        const unreadCount = notification.args?.unreadCount;

        if (notification.type === "IDOL_MESSAGE_STACK") {
            return groupName && idolName
                ? `${groupName} ${idolName} 채팅 알림 ${unreadCount ? `(${unreadCount}개)` : ""}`.trim()
                : `${idolName ?? "아이돌"} 채팅 알림 ${unreadCount ? `(${unreadCount}개)` : ""}`.trim();
        }

        if (notification.type === "IDOL_MESSAGE") {
            return idolName
                ? `${idolName}의 메시지가 도착했습니다.`
                : "아이돌 메시지가 도착했습니다.";
        }

        if (notification.type === "IDOL_SUB_STARTED") {
            return idolId
                ? `아이돌 구독이 시작되었습니다.`
                : "아이돌 구독이 시작되었습니다.";
        }

        if (notification.type === "IDOL_SUB_END") {
            return idolId
                ? `아이돌 구독이 종료되었습니다.`
                : "아이돌 구독이 종료되었습니다.";
        }

        if (notification.type === "GROUP_SUB_STARTED") {
            return groupId
                ? `그룹 구독이 시작되었습니다.`
                : "그룹 구독이 시작되었습니다.";
        }

        if (notification.type === "GROUP_SUB_END") {
            return groupId
                ? `그룹 구독이 종료되었습니다.`
                : "그룹 구독이 종료되었습니다.";
        }

        if (notification.type === "CONCERT_OPENED") {
            return groupName
                ? `${groupName}의 콘서트가 생성되었습니다.`
                : "콘서트가 생성되었습니다.";
        }

        if (notification.type === "RESERVATION_CREATED") {
            return concertName
                ? `${concertName}의 예매가 완료되었습니다.`
                : "콘서트 예매가 완료되었습니다.";
        }

        if (notification.type === "CHAT_IDOL_ONLINE") {
            return idolName
                ? `${idolName}의 채팅이 시작되었습니다.`
                : "아이돌의 채팅이 시작되었습니다.";
        }

        if (notification.type === "REPORT_RECEIVED") {
            const reportCount = notification.args?.reportCount;

            return reportCount
                ? `신고가 ${reportCount}회 누적되었습니다. 주의해주세요.`
                : "신고가 누적되었습니다. 주의해주세요.";
        }

        if (notification.type === "ACCOUNT_STATUS_CHANGED") {
            return "유저 상태가 변경되었습니다.";
        }

        if (notification.type === "LOGIN_NEW_DEVICE") {
            return "새로운 기기에서 로그인했습니다.";
        }

        if (notification.type === "LOGIN_FAIL_LOCKED") {
            return "로그인 실패가 누적되어 계정이 30분간 잠겼습니다.";
        }

        if (notification.type === "VOTE_OPENED") {
            return voteTitle
                ? `"${voteTitle}" 투표가 시작되었습니다.`
                : "투표가 시작되었습니다.";
        }

        if (notification.type === "VOTE_CLOSED") {
            return voteTitle
                ? `"${voteTitle}" 투표가 종료되었습니다.`
                : "투표가 종료되었습니다.";
        }

        if (notification.type === "VOTE_CLOSING_SOON") {
            return voteTitle
                ? `"${voteTitle}" 투표가 1시간 뒤 종료됩니다.`
                : "투표가 1시간 뒤 종료됩니다.";
        }

        if (notification.type === "MY_VOTE_SUBMITTED") {
            return voteTitle
                ? `"${voteTitle}" 투표를 완료했습니다.`
                : "투표를 완료했습니다.";
        }

        if (notification.type === "RANKING_CHANGED") {
            return voteTitle
                ? `"${voteTitle}" 랭킹이 변경되었습니다.`
                : "랭킹이 변경되었습니다.";
        }

        if (notification.type === "VOTE_RESULT") {
            return voteTitle
                ? `"${voteTitle}" 투표 결과가 공개되었습니다.`
                : "투표 결과가 공개되었습니다.";
        }

        return boardTitle || "알림";
    };

    const getNotificationLabel = (notification: HistoryItem) => {
        const boardType = notification.args?.boardType;

        if (notification.type === "IDOL_MESSAGE_STACK") return "아이돌 채팅";
        if (notification.type === "IDOL_MESSAGE") return "아이돌 메시지";

        if (boardType === "ADMIN_NOTICE") return "공지";
        if (boardType === "GROUP_OFFICIAL") return "그룹 공식";
        if (boardType === "GROUP_FAN") return "그룹 팬";
        if (boardType === "IDOL_OFFICIAL") return "아이돌 공식";

        if (
            notification.type === "IDOL_SUB_STARTED" ||
            notification.type === "IDOL_SUB_END" ||
            notification.type === "GROUP_SUB_STARTED" ||
            notification.type === "GROUP_SUB_END"
        ) {
            return "구독";
        }

        if (
            notification.type === "VOTE_OPENED" ||
            notification.type === "VOTE_CLOSED" ||
            notification.type === "VOTE_CLOSING_SOON" ||
            notification.type === "MY_VOTE_SUBMITTED" ||
            notification.type === "RANKING_CHANGED" ||
            notification.type === "VOTE_RESULT"
        ) {
            return "투표";
        }

        if (
            notification.type === "CONCERT_OPENED" ||
            notification.type === "RESERVATION_CREATED"
        ) {
            return "콘서트";
        }

        if (
            notification.type === "CHAT_IDOL_ONLINE"
        ) {
            return "알림";
        }

        return "시스템";
    };

    const getNotificationIcon = (notification: HistoryItem) => {
        const boardType = notification.args?.boardType;

        if (notification.type === "IDOL_MESSAGE_STACK" || notification.type === "IDOL_MESSAGE") {
            return "🎤";
        }

        if (boardType === "ADMIN_NOTICE") return "📢";
        if (boardType === "GROUP_OFFICIAL" || boardType === "GROUP_FAN") return "👥";
        if (boardType === "IDOL_OFFICIAL") return "🎤";

        if (
            notification.type === "VOTE_OPENED" ||
            notification.type === "VOTE_CLOSED" ||
            notification.type === "VOTE_CLOSING_SOON" ||
            notification.type === "MY_VOTE_SUBMITTED" ||
            notification.type === "RANKING_CHANGED" ||
            notification.type === "VOTE_RESULT"
        ) {
            return "🗳️";
        }

        if (
            notification.type === "IDOL_SUB_STARTED" ||
            notification.type === "IDOL_SUB_END" ||
            notification.type === "GROUP_SUB_STARTED" ||
            notification.type === "GROUP_SUB_END"
        ) {
            return "💎";
        }

        if (
            notification.type === "CONCERT_OPENED" ||
            notification.type === "RESERVATION_CREATED"
        ) {
            return "🎫";
        }

        if (
            notification.type === "CHAT_IDOL_ONLINE"
        ) {
            return "🔔";
        }

        return "⚠️";
    };

    const allVisibleSelected =
        pagedItems.length > 0 &&
        pagedItems.every((item) => selectedIds.includes(item.notificationId));

    const toggleSelectAllVisible = () => {
        const visibleIds = pagedItems.map((item) => item.notificationId);

        if (allVisibleSelected) {
            setSelectedIds((prev) => prev.filter((id) => !visibleIds.includes(id)));
            return;
        }

        setSelectedIds((prev) => Array.from(new Set([...prev, ...visibleIds])));
    };

    const toggleSelectOne = (notificationId: number) => {
        setSelectedIds((prev) =>
            prev.includes(notificationId)
                ? prev.filter((id) => id !== notificationId)
                : [...prev, notificationId]
        );
    };

    const handleDeleteSelected = async () => {
        if (selectedIds.length === 0) return;

        const normalNotificationIds = selectedIds.filter((id) => id > 0);
        const stackIds = selectedIds.filter((id) => id < 0);

        const idolIdsToDelete = stackIds.map((id) => Math.abs(id + 100000));

        try {
            if (normalNotificationIds.length > 0) {
                await deleteManyNotifications(normalNotificationIds);
            }

            if (idolIdsToDelete.length > 0) {
                await deleteManyIdolMessageStacks(idolIdsToDelete);
            }

            setNotifications((prev) =>
                prev.filter((item) => !normalNotificationIds.includes(item.notificationId))
            );

            setIdolStacks((prev) =>
                prev.filter((stack) => !idolIdsToDelete.includes(stack.idolId))
            );

            setSelectedIds([]);
        } catch (error) {
            showErrorToast("알림 삭제 중 오류가 발생했습니다.");
        }
    };

    const handleFilterChange = (nextFilter: FilterType) => {
        setFilter(nextFilter);
        setPage(1);
        setSelectedIds([]);
    };

    const handlePrevPage = () => {
        if (page <= 1) return;
        setPage((prev) => prev - 1);
        setSelectedIds([]);
    };

    const ensureNextPageData = async () => {
        if (!hasNext || !nextCursor || loadingMore) return;

        try {
            setLoadingMore(true);
            const data = await getNotificationList(FETCH_SIZE, nextCursor);

            setNotifications((prev) => {
                const merged = [...prev, ...(data.items ?? [])];
                return merged.filter(
                    (item, index, arr) =>
                        arr.findIndex(
                            (target) => target.notificationId === item.notificationId
                        ) === index
                );
            });
            setNextCursor(data.nextCursor ?? null);
            setHasNext(data.hasNext ?? false);
        } catch (error) {
            showErrorToast("알림을 더 불러오는 중 오류가 발생했습니다.");
        } finally {
            setLoadingMore(false);
        }
    };

    const handleNextPage = async () => {
        const nextPage = page + 1;
        const requiredCount = nextPage * PAGE_SIZE;

        if (filteredItems.length < requiredCount && hasNext) {
            await ensureNextPageData();
        }

        const updatedLength = filteredItems.length;

        if ((nextPage - 1) * PAGE_SIZE < updatedLength || hasNext) {
            setPage(nextPage);
            setSelectedIds([]);
        }
    };

    useEffect(() => {
        const recalculatedTotalPages = Math.max(1, Math.ceil(filteredItems.length / PAGE_SIZE));
        if (page > recalculatedTotalPages) {
            setPage(recalculatedTotalPages);
        }
    }, [filteredItems.length, page]);

    const filterTabs: { key: FilterType; label: string }[] = [
        { key: "all", label: "전체" },
        { key: "vote", label: "투표" },
        { key: "subscription", label: "구독" },
        { key: "chat", label: "채팅" },
        { key: "concert", label: "콘서트" },
        { key: "system", label: "시스템" },
    ];

    if (loading) {
        return <div className="text-gray-500">불러오는 중...</div>;
    }

    return (
        <div className="space-y-5">
            <div className="mb-2">
                <h2 className="text-xl font-bold text-gray-800">알림 히스토리</h2>
                <p className="mt-1 text-sm text-gray-500">
                    지금까지 받은 알림을 확인하고, 필요한 항목만 선택해서 정리할 수 있습니다.
                </p>
            </div>

            <div className="rounded-2xl border border-gray-100 bg-gray-50 px-5 py-4">
                <div className="flex flex-col gap-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                        <div className="flex items-center gap-3">
                            <label className="flex items-center gap-2 text-sm text-gray-700">
                                <input
                                    type="checkbox"
                                    checked={allVisibleSelected}
                                    onChange={toggleSelectAllVisible}
                                    className="h-4 w-4 rounded border-gray-300 text-idol focus:ring-idol"
                                />
                                전체 선택
                            </label>

                            <button
                                onClick={handleDeleteSelected}
                                className="rounded-lg border border-gray-200 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 transition hover:border-idol hover:text-idol"
                            >
                                삭제
                            </button>
                        </div>

                        <div className="text-sm text-gray-400">
                            총 {filteredItems.length}개
                        </div>
                    </div>

                    <div className="flex flex-wrap gap-2">
                        {filterTabs.map((tab) => (
                            <button
                                key={tab.key}
                                onClick={() => handleFilterChange(tab.key)}
                                className={`rounded-full px-3 py-1.5 text-sm font-medium transition ${
                                    filter === tab.key
                                        ? "bg-idol text-white"
                                        : "bg-white text-gray-600 border border-gray-200 hover:border-idol hover:text-idol"
                                }`}
                            >
                                {tab.label}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            <div className="overflow-hidden rounded-2xl border border-gray-100 bg-white">
                {pagedItems.length === 0 ? (
                    <div className="px-6 py-12 text-center text-sm text-gray-500">
                        표시할 알림이 없습니다.
                    </div>
                ) : (
                    <div className="divide-y divide-gray-100">
                        {pagedItems.map((notification) => (
                            <div
                                key={notification.notificationId}
                                className="flex items-start gap-4 px-5 py-4 transition hover:bg-gray-50"
                            >
                                <div className="pt-2">
                                    <input
                                        type="checkbox"
                                        checked={selectedIds.includes(notification.notificationId)}
                                        onChange={() => toggleSelectOne(notification.notificationId)}
                                        className="h-4 w-4 rounded border-gray-300 text-idol focus:ring-idol"
                                    />
                                </div>

                                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-gray-100 text-base">
                                    {getNotificationIcon(notification)}
                                </div>

                                <div className="min-w-0 flex-1">
                                    <div className="flex items-center justify-between gap-2">
                                        <span className="text-xs font-semibold text-gray-500">
                                            {getNotificationLabel(notification)}
                                        </span>
                                        <span className="shrink-0 text-[11px] text-gray-400">
                                            {formatNotificationTimeToKST(notification.occurredAt, notification.type)}
                                        </span>
                                    </div>

                                    <div className="mt-1 text-sm font-medium text-gray-800">
                                        {getNotificationTitle(notification)}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <div className="flex items-center justify-center gap-3 pt-2">
                <button
                    onClick={handlePrevPage}
                    disabled={page === 1}
                    className={`rounded-lg border px-4 py-2 text-sm font-medium transition ${
                        page === 1
                            ? "cursor-not-allowed border-gray-200 text-gray-300"
                            : "border-gray-200 text-gray-700 hover:border-idol hover:text-idol"
                    }`}
                >
                    이전
                </button>

                <div className="text-sm text-gray-600">
                    {page} / {totalPages}
                </div>

                <button
                    onClick={handleNextPage}
                    disabled={loadingMore || (!hasNext && page >= totalPages)}
                    className={`rounded-lg border px-4 py-2 text-sm font-medium transition ${
                        loadingMore || (!hasNext && page >= totalPages)
                            ? "cursor-not-allowed border-gray-200 text-gray-300"
                            : "border-gray-200 text-gray-700 hover:border-idol hover:text-idol"
                    }`}
                >
                    다음
                </button>
            </div>
        </div>
    );
};

export default NotificationHistoryTab;