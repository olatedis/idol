import React, { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";
import { motion, AnimatePresence } from "framer-motion";
import dolchatText from "../../assets/dolchatText.png";
import dolchatLogo from "../../assets/dolchatLogo.png"
import {
    getIdolMessageStacks,
    getNotificationList,
    readAllNotifications,
    readOneNotification,
    resetAllIdolMessageStacks,
    resetIdolMessageStack,
} from "../../api/notificationApi";
import { connectNotificationSse } from "../../utils/notificationSse";
import type { IdolMessageStackPayload, NotificationItem } from "../../types/notification";

import { showSuccessToast } from "../../utils/alert";

const Header: React.FC = () => {
    const navigate = useNavigate();
    const { user, logout, accessToken } = useAuthStore();
    const isLoggedIn = !!user || !!accessToken;

    const [isMenuOpen, setIsMenuOpen] = useState(false);

    const [isNotificationOpen, setIsNotificationOpen] = useState(false);
    const [notifications, setNotifications] = useState<NotificationItem[]>([]);
    const [idolMessageStacks, setIdolMessageStacks] = useState<IdolMessageStackPayload[]>([]);
    const [loadingNotifications, setLoadingNotifications] = useState(false);

    const [nextCursor, setNextCursor] = useState<string | null>(null);
    const [hasNext, setHasNext] = useState(false);
    const [loadingMoreNotifications, setLoadingMoreNotifications] = useState(false);
    const [bellAnimating, setBellAnimating] = useState(false);
    const [removingIds, setRemovingIds] = useState<number[]>([]);

    const notificationRef = useRef<HTMLDivElement | null>(null);

    const notificationListRef = useRef<HTMLDivElement | null>(null);

    const normalUnreadCount = notifications.filter(
        (item) => !item.isRead && item.type !== "IDOL_MESSAGE"
    ).length;

    const stackUnreadCount = idolMessageStacks.reduce(
        (sum, item) => sum + (item.unreadCount || 0),
        0
    );

    const unreadCount = normalUnreadCount + stackUnreadCount;

    const handleLogin = () => {
        navigate("/", { state: { scrollToLogin: true } });
        setIsMenuOpen(false);
    };

    const handleLogout = () => {
        setIsMenuOpen(false);
        logout();
        showSuccessToast("로그아웃되었습니다.");
        navigate("/", { replace: true });
    };

    const toggleMenu = () => setIsMenuOpen(!isMenuOpen);
    const closeMenu = () => setIsMenuOpen(false);

    const toggleNotification = () => setIsNotificationOpen((prev) => !prev);

    const goToNotificationSetting = () => {
        setIsNotificationOpen(false);
        navigate("/mypage", { state: { initialTab: "notification" } })
    }

    const goToNotificationHistory = () => {
        setIsNotificationOpen(false);
        navigate("/mypage", { state: { initialTab: "notificationHistory" } })
    }

    const handleNotificationClick = async (notification: NotificationItem) => {
        try {
            await readOneNotification(notification.notificationId);

            setNotifications((prev) =>
                prev.filter((item) => item.notificationId !== notification.notificationId)
            );
        } catch (error) {
        } finally {
            setIsNotificationOpen(false);

            if (
                notification.type !== "RESERVATION_CREATED" &&
                notification.type !== "LOGIN_NEW_DEVICE" &&
                notification.type !== "LOGIN_FAIL_LOCKED" &&
                notification.redirectUrl &&
                notification.redirectUrl.trim() !== "" &&
                notification.redirectUrl !== "#"
            ) {
                navigate(notification.redirectUrl);
            }
        }
    };

    const handleReadAllNotifications = async () => {
        if (!accessToken) return;

        const targetIds = notifications.map((item) => item.notificationId);
        const hasStacks = idolMessageStacks.some((item) => item.unreadCount > 0);

        if (targetIds.length === 0 && !hasStacks) return;

        try {
            await Promise.all([
                targetIds.length > 0 ? readAllNotifications() : Promise.resolve(),
                hasStacks ? resetAllIdolMessageStacks() : Promise.resolve(),
            ]);

            if (targetIds.length > 0) {
                setRemovingIds(targetIds);
            }

            window.setTimeout(() => {
                if (targetIds.length > 0) {
                    setNotifications((prev) =>
                        prev.filter((item) => !targetIds.includes(item.notificationId))
                    );
                    setRemovingIds([]);
                }

                // 수정: 스택형 알림도 전체읽음 시 unreadCount 0으로 즉시 반영
                setIdolMessageStacks((prev) =>
                    prev.map((item) => ({
                        ...item,
                        unreadCount: 0,
                    }))
                );
            }, 320);
        } catch (error) {
        }
    };

    const upsertIdolMessageStack = (payload: IdolMessageStackPayload) => {
        setIdolMessageStacks((prev) => {
            const found = prev.find((item) => item.idolId === payload.idolId);
            if (found) {
                return prev.map((item) =>
                    item.idolId === payload.idolId ? payload : item
                );
            }
            return [payload, ...prev];
        });
    };

    const formatLocalNotificationTimeToKST = (value?: string | null) => {
        if (!value) return "";

        const date = new Date(value);

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

    // 추가: 로그인 알림 전용 UTC -> KST 변환
    const formatUtcNotificationTimeToKST = (value?: string | null) => {
        if (!value) return "";

        const utcDate = new Date(`${value}Z`);

        if (Number.isNaN(utcDate.getTime())) return value;

        return new Intl.DateTimeFormat("ko-KR", {
            timeZone: "Asia/Seoul",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            hour12: false,
        }).format(utcDate);
    };

    const formatNotificationTime = (notification: NotificationItem) => {
        const localTypes = new Set([
            "CHAT_IDOL_ONLINE",
            "IDOL_MESSAGE",
            "REPLY_MESSAGE",
        ]);

        return localTypes.has(notification.type)
            ? formatLocalNotificationTimeToKST(notification.occurredAt)
            : formatUtcNotificationTimeToKST(notification.occurredAt);
    }

    const parseIdolId = (notification: NotificationItem) => {
        const idolIdFromArgs = notification.args?.idolId;
        if (idolIdFromArgs) return Number(idolIdFromArgs);

        const match = notification.redirectUrl?.match(/idolId=(\d+)/);
        if (match) return Number(match[1]);

        return null;
    };

    const getLatestIdolMessageNotification = (idolId: number) => {
        return notifications.find(
            (item) => item.type === "IDOL_MESSAGE" && parseIdolId(item) === idolId
        );
    };

    const handleIdolStackClick = async (stack: IdolMessageStackPayload) => {
        const latest = getLatestIdolMessageNotification(stack.idolId);

        const groupId = latest?.args?.groupId;
        const redirectUrl =
            latest?.redirectUrl ||
            (groupId
                ? `/group/${groupId}/chat?idolId=${stack.idolId}`
                : `/mypage`);

        try {
            await resetIdolMessageStack(stack.idolId);

            setIdolMessageStacks((prev) =>
                prev.map((item) =>
                    item.idolId === stack.idolId
                        ? { ...item, unreadCount: 0 }
                        : item
                )
            );
        } catch (error) {
        } finally {
            setIsNotificationOpen(false);
            navigate(redirectUrl);
        }
    };




    const getNotificationTitle = (notification: NotificationItem) => {
        const voteTitle = notification.args?.voteTitle;
        const boardTitle = notification.args?.title;
        const idolName = notification.args?.idolName;
        const groupName = notification.args?.groupName;
        const concertName = notification.args?.concertName;
        const idolId = notification.args?.idolId;
        const groupId = notification.args?.groupId;

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
                : "콘서트가 생성되었습니다."
        }

        if (notification.type === "RESERVATION_CREATED") {
            return concertName
                ? `${concertName}의 예매가 완료되었습니다.`
                : "콘서트 예매가 완료되었습니다.";
        }

        if (notification.type === "CHAT_IDOL_ONLINE") {
            return idolName
                ? `${idolName}님이 채팅을 시작했습니다.`
                : "아이돌이 채팅을 시작했습니다.";
        }

        if (notification.type === "REPLY_MESSAGE") {
            const replierName = notification.args?.replierName;
            return replierName
                ? `${replierName}님이 답장을 남겼습니다.`
                : "새로운 답장이 도착했습니다.";
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

        return boardTitle || notification.type;
    };

    const getNotificationLabel = (notification: NotificationItem) => {
        const boardType = notification.args?.boardType;

        if (boardType === "ADMIN_NOTICE") return "공지";
        if (boardType === "GROUP_OFFICIAL") return "그룹 공식";
        if (boardType === "GROUP_FAN") return "그룹 팬";
        if (boardType === "IDOL_OFFICIAL") return "아이돌 공식";
        if (notification.type === "IDOL_MESSAGE" || notification.type === "CHAT_IDOL_ONLINE" || notification.type === "REPLY_MESSAGE") return "채팅";

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
            notification.type === "LOGIN_NEW_DEVICE" ||
            notification.type === "LOGIN_FAIL_LOCKED"
        ) {
            return "로그인";
        }

        return "알림";
    };

    const getNotificationIcon = (notification: NotificationItem) => {
        const boardType = notification.args?.boardType;

        if (boardType === "ADMIN_NOTICE") return "📢";
        if (boardType === "GROUP_OFFICIAL" || boardType === "GROUP_FAN") return "👥";
        if (boardType === "IDOL_OFFICIAL" || notification.type === "IDOL_MESSAGE" || notification.type === "CHAT_IDOL_ONLINE") return "🎤";
        if (notification.type === "REPLY_MESSAGE") return "💬";

        // 추가: 투표 알림 아이콘
        if (
            notification.type === "VOTE_OPENED" ||
            notification.type === "VOTE_CLOSED" ||
            notification.type === "VOTE_CLOSING_SOON" ||
            notification.type === "MY_VOTE_SUBMITTED"
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
            notification.type === "LOGIN_NEW_DEVICE" ||
            notification.type === "LOGIN_FAIL_LOCKED"
        ) {
            return "🔐";
        }

        return "🔔";
    };

    const triggerBellAnimation = () => {
        setBellAnimating(true);
        window.setTimeout(() => {
            setBellAnimating(false);
        }, 900);
    };

    const loadMoreNotifications = async () => {
        if (!accessToken || !hasNext || !nextCursor || loadingMoreNotifications) return;

        try {
            setLoadingMoreNotifications(true);

            const data = await getNotificationList(20, nextCursor);

            setNotifications((prev) => {
                const unreadItems = (data.items ?? []).filter((item) => !item.isRead);
                const merged = [...prev, ...unreadItems];

                return merged.filter(
                    (item, index, arr) =>
                        arr.findIndex((target) => target.notificationId === item.notificationId) === index
                );
            });
            setNextCursor(data.nextCursor ?? null);
            setHasNext(data.hasNext ?? false);
        } catch (error) {
            console.error(error);
        } finally {
            setLoadingMoreNotifications(false);
        }
    };

    const visibleNotifications = notifications.filter(
        (item) => !item.isRead && item.type !== "IDOL_MESSAGE"
    );

    useEffect(() => {
        if (!isLoggedIn || !accessToken) {
            setNotifications([]);
            setIdolMessageStacks([]);
            setNextCursor(null);
            setHasNext(false);
            return;
        }

        const loadNotifications = async () => {
            try {
                setLoadingNotifications(true);

                const [notificationData, stackData] = await Promise.all([
                    getNotificationList(20),
                    getIdolMessageStacks(),
                ]);

                setNotifications((notificationData.items ?? []).filter((item) => !item.isRead));
                setNextCursor(notificationData.nextCursor ?? null);
                setHasNext(notificationData.hasNext ?? false);
                setIdolMessageStacks(stackData.items ?? []);
            } catch (error) {
            } finally {
                setLoadingNotifications(false);
            }
        };

        loadNotifications();
    }, [isLoggedIn, accessToken]);

    useEffect(() => {
        if (!isLoggedIn || !accessToken) return;

        let connection: { close: () => void } | null = null;

        const connect = async () => {
            connection = await connectNotificationSse(accessToken, {
                onConnected: () => {
                },
                onNotification: (payload) => {
                    setNotifications((prev) => [
                        {
                            ...payload,
                            isRead: false,
                            readAt: null,
                        },
                        ...prev.filter((item) => item.notificationId !== payload.notificationId),
                    ]);
                    triggerBellAnimation();
                },
                onIdolMessageStack: (payload) => {
                    upsertIdolMessageStack(payload);
                },
                onError: () => {
                },
            });
        };

        connect();

        return () => {
            connection?.close();
        };
    }, [isLoggedIn, accessToken]);

    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (
                isNotificationOpen &&
                notificationRef.current &&
                !notificationRef.current.contains(event.target as Node)
            ) {
                setIsNotificationOpen(false);
            }
        };

        document.addEventListener("mousedown", handleClickOutside);

        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, [isNotificationOpen]);

    useEffect(() => {
        const el = notificationListRef.current;
        if (!el || !isNotificationOpen) return;

        const handleScroll = () => {
            const threshold = 40;
            const reachedBottom =
                el.scrollTop + el.clientHeight >= el.scrollHeight - threshold;

            if (reachedBottom) {
                loadMoreNotifications();
            }
        };

        el.addEventListener("scroll", handleScroll);

        return () => {
            el.removeEventListener("scroll", handleScroll);
        };
    }, [isNotificationOpen, nextCursor, hasNext, loadingMoreNotifications, notifications]);

    return (
        <>
            <header className="w-full px-4 sm:px-6 md:px-8 py-4 fixed top-0 z-50 bg-white/30 backdrop-blur-sm">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        {/* 햄버거 아이콘 */}
                        <button
                            onClick={toggleMenu}
                            className="p-2 text-idol hover:text-idol-point transition focus:outline-none"
                        >
                            <svg
                                className="w-6 h-6"
                                fill="none"
                                stroke="currentColor"
                                viewBox="0 0 24 24"
                                xmlns="http://www.w3.org/2000/svg"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M4 6h16M4 12h16M4 18h16"
                                />
                            </svg>
                        </button>

                        <div className="flex items-center">
                        {/* 로고 */}
                        <img
                            src={dolchatLogo}
                            alt="dolchat"
                            className="h-6 hover:cursor-pointer transition"
                            onClick={() => navigate("/")}
                        />
                        <img
                            src={dolchatText}
                            alt="dolchat"
                            className="h-6 hidden sm:block md:h-8 hover:cursor-pointer transition"
                            onClick={() => navigate("/")}
                        />
                    </div>
                    </div>

                    <div className="flex gap-3 sm:gap-4 md:gap-6 text-xs sm:text-sm items-center relative">
                        {isLoggedIn ? (
                            <>
                                <div className="relative" ref={notificationRef}>
                                    <button
                                        onClick={toggleNotification}
                                        className="relative p-2 text-gray-700 hover:text-idol transition"
                                        title="알림"
                                    >
                                        <motion.svg
                                            animate={
                                                bellAnimating
                                                    ? { rotate: [0, -18, 18, -12, 12, -6, 6, 0] }
                                                    : { rotate: 0 }
                                            }
                                            transition={{ duration: 0.8 }}
                                            className="w-6 h-6"
                                            fill="none"
                                            stroke="currentColor"
                                            viewBox="0 0 24 24"
                                        >
                                            <path
                                                strokeLinecap="round"
                                                strokeLinejoin="round"
                                                strokeWidth={2}
                                                d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0a3 3 0 11-6 0m6 0H9"
                                            />
                                        </motion.svg>

                                        {unreadCount > 0 && (
                                            <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-red-500 text-white text-[10px] flex items-center justify-center">
                                            {unreadCount > 99 ? "99+" : unreadCount}
                                        </span>
                                        )}
                                    </button>

                                    <AnimatePresence>
                                        {isNotificationOpen && (
                                            <motion.div
                                                initial={{ opacity: 0, y: 8 }}
                                                animate={{ opacity: 1, y: 0 }}
                                                exit={{ opacity: 0, y: 8 }}
                                                transition={{ duration: 0.2 }}
                                                className="fixed right-4 top-[72px] w-[calc(100vw-2rem)] max-w-[360px] rounded-xl border border-gray-200 bg-white shadow-xl z-50 overflow-hidden"
                                            >
                                                <div className="px-3 sm:px-4 py-2 sm:py-3 border-b border-gray-100 flex items-center justify-between">
                                                    <div className="font-semibold text-xs sm:text-sm text-gray-800">알림</div>

                                                    <div className="flex items-center gap-2 sm:gap-3">
                                                        <button
                                                            onClick={goToNotificationHistory}
                                                            className="text-[10px] sm:text-xs text-gray-600 hover:text-idol transition"
                                                        >
                                                            히스토리
                                                        </button>

                                                        <button
                                                            onClick={handleReadAllNotifications}
                                                            className="text-[10px] sm:text-xs text-gray-600 hover:text-idol transition"
                                                        >
                                                            전체 읽음
                                                        </button>

                                                        <button
                                                            onClick={goToNotificationSetting}
                                                            className="text-gray-500 hover:text-idol transition"
                                                            title="알림 설정"
                                                        >
                                                            <svg
                                                                className="w-4 h-4"
                                                                fill="none"
                                                                stroke="currentColor"
                                                                viewBox="0 0 24 24"
                                                            >
                                                                <path
                                                                    strokeLinecap="round"
                                                                    strokeLinejoin="round"
                                                                    strokeWidth={2}
                                                                    d="M10.325 4.317a1.724 1.724 0 013.35 0 1.724 1.724 0 002.573 1.066 1.724 1.724 0 012.365.997 1.724 1.724 0 001.995 1.995 1.724 1.724 0 01.997 2.365 1.724 1.724 0 001.066 2.573 1.724 1.724 0 010 3.35 1.724 1.724 0 00-1.066 2.573 1.724 1.724 0 01-.997 2.365 1.724 1.724 0 00-1.995 1.995 1.724 1.724 0 01-2.365.997 1.724 1.724 0 00-2.573 1.066 1.724 1.724 0 01-3.35 0 1.724 1.724 0 00-2.573-1.066 1.724 1.724 0 01-2.365-.997 1.724 1.724 0 00-1.995-1.995 1.724 1.724 0 01-.997-2.365 1.724 1.724 0 00-1.066-2.573 1.724 1.724 0 010-3.35 1.724 1.724 0 001.066-2.573 1.724 1.724 0 01.997-2.365 1.724 1.724 0 001.995-1.995 1.724 1.724 0 012.365-.997 1.724 1.724 0 002.573-1.066z"
                                                                />
                                                                <path
                                                                    strokeLinecap="round"
                                                                    strokeLinejoin="round"
                                                                    strokeWidth={2}
                                                                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                                                                />
                                                            </svg>
                                                        </button>
                                                    </div>
                                                </div>

                                                <div
                                                    ref={notificationListRef}
                                                    className="max-h-[300px] sm:max-h-[340px] overflow-y-auto"
                                                >
                                                    {loadingNotifications ? (
                                                        <div className="px-3 sm:px-4 py-6 text-xs sm:text-sm text-gray-500 text-center">
                                                            불러오는 중...
                                                        </div>
                                                    ) : visibleNotifications.length === 0 &&
                                                    idolMessageStacks.filter((stack) => stack.unreadCount > 0).length === 0 ? (
                                                        <div className="px-3 sm:px-4 py-6 text-xs sm:text-sm text-gray-500 text-center">
                                                            알림이 없습니다.
                                                        </div>
                                                    ) : (
                                                        <>
                                                            {idolMessageStacks
                                                                .filter((stack) => stack.unreadCount > 0)
                                                                .sort((a, b) => {
                                                                    const aTime = a.lastOccurredAt
                                                                        ? new Date(a.lastOccurredAt).getTime()
                                                                        : 0;
                                                                    const bTime = b.lastOccurredAt
                                                                        ? new Date(b.lastOccurredAt).getTime()
                                                                        : 0;
                                                                    return bTime - aTime;
                                                                })
                                                                .map((stack) => {
                                                                    const latest = getLatestIdolMessageNotification(stack.idolId);

                                                                    const idolName =
                                                                        latest?.args?.idolName || `아이돌 ${stack.idolId}`;
                                                                    const groupName =
                                                                        latest?.args?.groupName || "그룹";
                                                                    const idolImageUrl = latest?.args?.idolImageUrl;

                                                                    return (
                                                                        <button
                                                                            key={`stack-${stack.idolId}`}
                                                                            onClick={() => handleIdolStackClick(stack)}
                                                                            className="w-full px-3 sm:px-4 py-2 sm:py-3 text-left hover:bg-gray-50 transition border-b border-gray-100"
                                                                        >
                                                                            <div className="flex items-start gap-2 sm:gap-3">
                                                                                <div className="w-8 sm:w-10 h-8 sm:h-10 rounded-full bg-gray-100 overflow-hidden shrink-0 flex items-center justify-center text-[10px] sm:text-xs text-gray-400">
                                                                                    {idolImageUrl ? (
                                                                                        <img
                                                                                            src={idolImageUrl}
                                                                                            alt={idolName}
                                                                                            className="w-full h-full object-cover"
                                                                                        />
                                                                                    ) : (
                                                                                        "👤"
                                                                                    )}
                                                                                </div>

                                                                                <div className="flex-1 min-w-0">
                                                                                    <div className="flex items-center justify-between gap-1 sm:gap-2">
                                                                                    <span className="text-[10px] sm:text-xs font-semibold text-gray-500">
                                                                                        아이돌 채팅
                                                                                    </span>
                                                                                        <span className="text-[9px] sm:text-[11px] text-gray-400 shrink-0">
                                                                                        {formatLocalNotificationTimeToKST(
                                                                                            stack.lastOccurredAt
                                                                                        )}
                                                                                    </span>
                                                                                    </div>

                                                                                    <div className="mt-1 text-xs sm:text-sm font-medium text-gray-800 truncate">
                                                                                        {groupName} {idolName}
                                                                                    </div>
                                                                                </div>

                                                                                <div className="shrink-0 self-center">
                                                                                    <span className="inline-flex min-w-[20px] sm:min-w-[22px] h-[20px] sm:h-[22px] px-1 sm:px-1.5 rounded-full bg-red-500 text-white text-[9px] sm:text-[11px] font-semibold items-center justify-center">
                                                                                        {stack.unreadCount > 99
                                                                                            ? "99+"
                                                                                            : stack.unreadCount}
                                                                                    </span>
                                                                                </div>
                                                                            </div>
                                                                        </button>
                                                                    );
                                                                })}

                                                            {visibleNotifications.map((notification) => (
                                                                <button
                                                                    key={notification.notificationId}
                                                                    onClick={() => handleNotificationClick(notification)}
                                                                    className={`w-full px-3 sm:px-4 py-2 sm:py-3 text-left hover:bg-gray-50 transition border-b border-gray-100 ${
                                                                        removingIds.includes(notification.notificationId)
                                                                            ? "translate-x-8 opacity-0"
                                                                            : "translate-x-0 opacity-100"
                                                                    } ${
                                                                        notification.isRead ? "bg-white" : "bg-idol/5"
                                                                    }`}
                                                                >
                                                                    <div className="flex items-start gap-2 sm:gap-3">
                                                                        <div className="w-8 sm:w-10 h-8 sm:h-10 rounded-full bg-gray-100 flex items-center justify-center shrink-0 text-sm sm:text-base">
                                                                            {getNotificationIcon(notification)}
                                                                        </div>

                                                                        <div className="flex-1 min-w-0">
                                                                            <div className="flex items-center justify-between gap-1 sm:gap-2">
                                                                                <span className="text-[10px] sm:text-xs font-semibold text-gray-500">
                                                                                    {getNotificationLabel(notification)}
                                                                                </span>
                                                                                <span className="text-[9px] sm:text-[11px] text-gray-400 shrink-0">
                                                                                    {formatNotificationTime(notification)}
                                                                                </span>
                                                                            </div>

                                                                            <div className="mt-1 text-xs sm:text-sm font-medium text-gray-800 truncate">
                                                                                {getNotificationTitle(notification)}
                                                                            </div>

                                                                            {!notification.isRead && (
                                                                                <div className="mt-2 flex justify-end">
                                                                                    <span className="inline-block w-2 h-2 rounded-full bg-idol"></span>
                                                                                </div>
                                                                            )}
                                                                        </div>
                                                                    </div>
                                                                </button>
                                                            ))}
                                                        </>
                                                    )}
                                                </div>

                                                {hasNext && (
                                                    <div className="px-4 py-3 text-xs text-center text-gray-400 border-t border-gray-100">
                                                        {loadingMoreNotifications ? (
                                                            "알림 더 불러오는 중..."
                                                        ) : (
                                                            "스크롤하여 더 보기"
                                                        )}
                                                    </div>
                                                )}
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </div>

                                <span className="hidden sm:block font-semibold text-xs md:text-sm text-gray-700">
                                    {user?.nickname || "회원"}님
                                </span>

                                <div className="rounded-md bg-gray-200 hover:bg-gray-300 transition">
                                    <button
                                        onClick={handleLogout}
                                        className="px-2 sm:px-3 md:px-4 py-2 text-gray-700 text-xs sm:text-sm font-medium"
                                    >
                                        logout
                                    </button>
                                </div>
                            </>
                        ) : (
                            <>
                                <div className="rounded-md bg-idol hover:bg-idol-point transition">
                                    <button onClick={handleLogin} className="px-2 sm:px-3 md:px-4 py-2 hover:cursor-pointer text-white text-xs sm:text-sm font-medium">
                                        login
                                    </button>
                                </div>
                                <div className="rounded-md bg-idol hover:bg-idol-point transition">
                                    <button onClick={handleLogin} className="px-2 sm:px-3 md:px-4 py-2 hover:cursor-pointer text-white text-xs sm:text-sm font-medium">
                                        register
                                    </button>
                                </div>
                            </>
                        )}
                    </div>
                </div>
            </header>

            <AnimatePresence>
                {isMenuOpen && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.2 }}
                        className="fixed inset-0 bg-black/50 z-40"
                        onClick={closeMenu}
                    />
                )}
            </AnimatePresence>

            <AnimatePresence>
                {isMenuOpen && (
                    <motion.div
                        initial={{ x: "-100%" }}
                        animate={{ x: 0 }}
                        exit={{ x: "-100%" }}
                        transition={{ type: "tween", duration: 0.3 }}
                        className="fixed top-0 left-0 h-full w-60 sm:w-64 bg-white shadow-2xl z-50 flex flex-col"
                    >
                        <div className="p-4 sm:p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50">
                            <span className="text-lg sm:text-xl font-bold text-idol">Menu</span>
                            <button
                                onClick={closeMenu}
                                className="text-gray-400 hover:text-gray-600 focus:outline-none"
                            >
                                <svg
                                    className="w-6 h-6"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                    xmlns="http://www.w3.org/2000/svg"
                                >
                                    <path
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                        strokeWidth={2}
                                        d="M6 18L18 6M6 6l12 12"
                                    />
                                </svg>
                            </button>
                        </div>

                        <nav className="flex-1 overflow-y-auto py-4">
                            <ul className="space-y-2 px-3 sm:px-4 text-xs sm:text-sm text-gray-700">
                                <li>
                                    <Link
                                        to="/notices"
                                        onClick={closeMenu}
                                        className="block p-2 sm:p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors text-xs sm:text-sm"
                                    >
                                        공지사항
                                    </Link>
                                </li>
                                <li>
                                    <Link
                                        to="/idol"
                                        onClick={closeMenu}
                                        className="block p-2 sm:p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors text-xs sm:text-sm"
                                    >
                                        아이돌 페이지
                                    </Link>
                                </li>
                                <li>
                                    <Link
                                        to="/concert"
                                        onClick={closeMenu}
                                        className="block p-2 sm:p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors text-xs sm:text-sm"
                                    >
                                        콘서트 페이지
                                    </Link>
                                </li>
                                {isLoggedIn && (
                                    <li>
                                        <Link
                                            to="/mypage"
                                            onClick={closeMenu}
                                            className="block p-2 sm:p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors text-xs sm:text-sm"
                                        >
                                            마이페이지
                                        </Link>
                                    </li>
                                )}
                            </ul>
                        </nav>

                        <div className="p-4 sm:p-6 border-t border-gray-100 bg-gray-50 text-xs sm:text-sm">
                            {isLoggedIn ? (
                                <button
                                    onClick={handleLogout}
                                    className="w-full p-2 sm:p-3 bg-white border border-gray-200 text-gray-600 rounded-xl hover:bg-gray-100 transition shadow-sm font-medium text-xs sm:text-sm"
                                >
                                    로그아웃
                                </button>
                            ) : (
                                <div className="space-y-2 sm:space-y-3">
                                    <button
                                        onClick={handleLogin}
                                        className="w-full p-2 sm:p-3 bg-idol text-white rounded-xl hover:bg-idol-point transition shadow-md font-medium text-xs sm:text-sm"
                                    >
                                        로그인
                                    </button>
                                </div>
                            )}
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </>
    );
};

export default Header;