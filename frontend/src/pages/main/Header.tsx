import React, { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuthStore } from "../../stores/authStore";
import { motion, AnimatePresence } from "framer-motion";
import {
    getNotificationList,
    readAllNotifications,
    readOneNotification,
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

    const unreadCount = notifications.filter((item) => !item.isRead).length;

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
        navigate("/mypage", {state: { initialTab: "notification" } })
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
            navigate(notification.redirectUrl);
        }
    };

    const handleReadAllNotifications = async () => {

        if (!accessToken) return;

        const targetIds = notifications.map((item) => item.notificationId);

        if (targetIds.length === 0) return;

        try {
            await readAllNotifications();

            setRemovingIds(targetIds);

            window.setTimeout(() => {
                setNotifications((prev) =>
                    prev.filter((item) => !targetIds.includes(item.notificationId))
                );
                setRemovingIds([]);
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

    const formatNotificationTimeToKST = (value?: string | null) => {
        if (!value) return "";

        const utcDate = value.endsWith("Z") ? new Date(value) : new Date(`${value}Z`);

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

    const getNotificationTitle = (notification: NotificationItem) => {
        const voteTitle = notification.args?.voteTitle;
        const boardTitle = notification.args?.title;

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

        return boardTitle || notification.type;
    };

    const getNotificationLabel = (notification: NotificationItem) => {
        const boardType = notification.args?.boardType;

        if (boardType === "ADMIN_NOTICE") return "공지";
        if (boardType === "GROUP_OFFICIAL") return "그룹 공식";
        if (boardType === "GROUP_FAN") return "그룹 팬";
        if (boardType === "IDOL_OFFICIAL") return "아이돌 공식";
        if (notification.type === "IDOL_MESSAGE") return "아이돌 메시지";

        // 추가: 투표 알림 라벨
        if (
            notification.type === "VOTE_OPENED" ||
            notification.type === "VOTE_CLOSED" ||
            notification.type === "VOTE_CLOSING_SOON" ||
            notification.type === "MY_VOTE_SUBMITTED"
        ) {
            return "투표";
        }

        return "알림";
    };

    const getNotificationIcon = (notification: NotificationItem) => {
        const boardType = notification.args?.boardType;

        if (boardType === "ADMIN_NOTICE") return "📢";
        if (boardType === "GROUP_OFFICIAL" || boardType === "GROUP_FAN") return "👥";
        if (boardType === "IDOL_OFFICIAL" || notification.type === "IDOL_MESSAGE") return "🎤";

        // 추가: 투표 알림 아이콘
        if (
            notification.type === "VOTE_OPENED" ||
            notification.type === "VOTE_CLOSED" ||
            notification.type === "VOTE_CLOSING_SOON" ||
            notification.type === "MY_VOTE_SUBMITTED"
        ) {
            return "🗳️";
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

            setNotifications((prev) => [...prev, ...(data.items ?? [])]);
            setNextCursor(data.nextCursor ?? null);
            setHasNext(data.hasNext ?? false);
        } catch (error) {
            console.error(error);
        } finally {
            setLoadingMoreNotifications(false);
        }
    };

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
                const data = await getNotificationList(20);
                setNotifications((data.items ?? []).filter(n => !n.isRead));
                setNextCursor(data.nextCursor ?? null);
                setHasNext(data.hasNext ?? false);
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
                        ...prev.filter(n => !n.isRead),
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
            <header className="w-full px-8 py-4 fixed top-0 z-50 bg-white shadow-sm">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        {/* 햄버거 아이콘 */}
                        <button
                            onClick={toggleMenu}
                            className="p-2 text-gray-700 hover:text-idol-dark transition focus:outline-none"
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

                        {/* 로고 */}
                        <div
                            onClick={() => navigate("/")}
                            className="text-xl font-bold text-idol hover:text-idol-dark hover:cursor-pointer"
                        >
                            dolchat
                        </div>
                    </div>

                    <div className="flex gap-6 text-sm items-center relative">
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
                                                className="absolute right-0 mt-2 w-[360px] rounded-xl border border-gray-200 bg-white shadow-xl z-50 overflow-hidden"
                                            >
                                                <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
                                                    <div className="font-semibold text-gray-800">알림</div>

                                                    <div className="flex items-center gap-3">
                                                        <button
                                                            onClick={handleReadAllNotifications}
                                                            className="text-xs text-idol hover:text-idol-dark transition"
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
                                                    className="divide-y divide-gray-100 max-h-[340px] overflow-y-auto"
                                                >
                                                    {loadingNotifications ? (
                                                        <div className="px-4 py-6 text-sm text-gray-500 text-center">
                                                            불러오는 중...
                                                        </div>
                                                    ) : notifications.length === 0 ? (
                                                        <div className="px-4 py-6 text-sm text-gray-500 text-center">
                                                            알림이 없습니다.
                                                        </div>
                                                    ) : (
                                                        notifications.map((notification) => (
                                                            <button
                                                                key={notification.notificationId}
                                                                onClick={() => handleNotificationClick(notification)}
                                                                className={`w-full px-4 py-3 text-left hover:bg-gray-50 transition ${
                                                                    removingIds.includes(notification.notificationId)
                                                                        ? "translate-x-8 opacity-0"
                                                                        : "translate-x-0 opacity-100"
                                                                } ${
                                                                    notification.isRead ? "bg-white" : "bg-idol/5"
                                                                }`}
                                                            >
                                                                <div className="flex items-start gap-3">
                                                                    <div className="w-10 h-10 rounded-full bg-gray-100 flex items-center justify-center shrink-0 text-base">
                                                                        {getNotificationIcon(notification)}
                                                                    </div>

                                                                    <div className="flex-1 min-w-0">
                                                                        <div className="flex items-center justify-between gap-2">
                                                                            <span className="text-xs font-semibold text-gray-500">
                                                                                {getNotificationLabel(notification)}
                                                                            </span>
                                                                            <span className="text-[11px] text-gray-400 shrink-0">
                                                                                {formatNotificationTimeToKST(notification.occurredAt)}
                                                                            </span>
                                                                        </div>

                                                                        <div className="mt-1 text-sm font-medium text-gray-800 truncate">
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
                                                        ))
                                                    )}
                                                </div>

                                                {loadingMoreNotifications && (
                                                    <div className="px-4 py-3 text-xs text-gray-400 text-center border-t border-gray-100">
                                                        알림 더 불러오는 중...
                                                    </div>
                                                )}

                                                {idolMessageStacks.length > 0 && (
                                                    <div className="border-t border-gray-100 bg-gray-50 px-4 py-3">
                                                        <div className="text-xs font-semibold text-gray-600 mb-2">
                                                            아이돌 메시지 스택
                                                        </div>
                                                        <div className="space-y-1">
                                                            {idolMessageStacks.map((stack) => (
                                                                <div
                                                                    key={stack.idolId}
                                                                    className="text-xs text-gray-600"
                                                                >
                                                                    idolId {stack.idolId} : {stack.unreadCount}개
                                                                </div>
                                                            ))}
                                                        </div>
                                                    </div>
                                                )}
                                            </motion.div>
                                        )}
                                    </AnimatePresence>
                                </div>

                                <span className="font-semibold text-gray-700">
                                    {user?.nickname || "회원"}님
                                </span>

                                <div className="rounded-md bg-gray-200 hover:bg-gray-300 transition">
                                    <button
                                        onClick={handleLogout}
                                        className="p-2 text-gray-700 w-[80px]"
                                    >
                                        logout
                                    </button>
                                </div>
                            </>
                        ) : (
                            <>
                                <div className="rounded-md bg-idol hover:bg-idol-dark transition">
                                    <button onClick={handleLogin} className="p-2 text-white w-[64px]">
                                        login
                                    </button>
                                </div>
                                <div className="rounded-md bg-idol hover:bg-idol-dark transition">
                                    <button onClick={handleLogin} className="p-2 text-white w-[64px]">
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
                        className="fixed top-0 left-0 h-full w-64 bg-white shadow-2xl z-50 flex flex-col"
                    >
                        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50">
                            <span className="text-xl font-bold text-idol">Menu</span>
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
                            <ul className="space-y-2 px-4 text-gray-700">
                                <li>
                                    <Link
                                        to="/notices"
                                        onClick={closeMenu}
                                        className="block p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors"
                                    >
                                        공지사항
                                    </Link>
                                </li>
                                <li>
                                    <Link
                                        to="/idol"
                                        onClick={closeMenu}
                                        className="block p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors"
                                    >
                                        아이돌 페이지
                                    </Link>
                                </li>
                                <li>
                                    <Link
                                        to="/concert"
                                        onClick={closeMenu}
                                        className="block p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors"
                                    >
                                        콘서트 페이지
                                    </Link>
                                </li>
                                <li>
                                    <Link
                                        to="/mypage"
                                        onClick={closeMenu}
                                        className="block p-3 rounded-xl hover:bg-idol/10 hover:text-idol font-medium transition-colors"
                                    >
                                        마이페이지
                                    </Link>
                                </li>
                            </ul>
                        </nav>

                        <div className="p-6 border-t border-gray-100 bg-gray-50 text-sm">
                            {isLoggedIn ? (
                                <button
                                    onClick={handleLogout}
                                    className="w-full p-3 bg-white border border-gray-200 text-gray-600 rounded-xl hover:bg-gray-100 transition shadow-sm font-medium"
                                >
                                    로그아웃
                                </button>
                            ) : (
                                <div className="space-y-3">
                                    <button
                                        onClick={handleLogin}
                                        className="w-full p-3 bg-idol text-white rounded-xl hover:bg-idol-dark transition shadow-md font-medium"
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