import './App.css'

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import MainPage from "./pages/main/MainPage.tsx";
import NoticeListPage from "./pages/main/notice/NoticeListPage.tsx";
import NoticeDetailPage from "./pages/main/notice/NoticeDetailPage.tsx";
import NoticeWritePage from "./pages/main/notice/NoticeWritePage.tsx";
import NoticeEditPage from "./pages/main/notice/NoticeEditPage.tsx";
import DefaultServicePage from "./pages/service/DefaultServicePage.tsx";
import GroupBoardPage from "./pages/board/group/GroupBoardPage.tsx";
import IdolPage from "./pages/idol/IdolPage.tsx";
import GroupPostWritePage from "./pages/board/group/GroupPostWritePage.tsx";
import GroupPostDetailPage from "./pages/board/group/GroupPostDetailPage.tsx";
import OAuthKakao from "./pages/auth/OAuthKakao.tsx";
import IdolSubscribe from "./pages/main/IdolSubscribe.tsx";
import PaymentPage from "./pages/payment/PaymentPage.tsx";
import PaymentComplete from "./pages/payment/PaymentComplete.tsx";
import VotePage from "./pages/vote/VotePage.tsx";
import ConcertCreatePage from "./pages/concert/ConcertCreatePage.tsx";
import IdolBoardPage from "./pages/board/idol/IdolBoardPage.tsx";
import IdolPostWritePage from "./pages/board/idol/IdolPostWritePage.tsx";
import IdolPostDetailPage from "./pages/board/idol/IdolPostDetailPage.tsx";
import ChatPage from "./pages/chat/ChatPage.tsx";
import GlobalConcertPage from "./pages/main/GlobalConcertPage.tsx";
import GroupPostEditPage from "./pages/board/group/GroupPostEditPage.tsx";
import MyPage from "./pages/main/mypage/MyPage.tsx";
import IdolPostEditPage from "./pages/board/idol/IdolPostEditPage.tsx";
import ConcertPage from "./pages/concert/ConcertPage.tsx";

function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* 메인 */}
                <Route path="/" element={<MainPage />} />

                {/* 햄버거바 메뉴 */}
                <Route path="/notices" element={<NoticeListPage />} />
                <Route path="/notices/:postId" element={<NoticeDetailPage />} />
                <Route path="/mypage" element={<MyPage />} /> // 마이페이지 추가

                {/* 공지 작성/수정 (관리자) */}
                <Route path="/admin/notices/write" element={<NoticeWritePage />} />
                <Route path="/admin/notices/edit/:postId" element={<NoticeEditPage />} />

                {/* 카카오 로그인 */}
                <Route path="/oauth/kakao" element={<OAuthKakao />} />

                {/* 그룹 서비스 */}
                <Route path="/group/:groupId" element={<DefaultServicePage />}>
                    <Route index element={<Navigate to="board" replace />} />

                    {/* 그룹 게시판 */}
                    <Route path="board" element={<GroupBoardPage />} />

                    {/* board/write가 board/:postId에 먹히는 케이스 방지 */}
                    <Route path="board/write" element={<GroupPostWritePage />} />
                    <Route path="board/:postId" element={<GroupPostDetailPage />} />
                    <Route path="board/:postId/edit" element={<GroupPostEditPage />} />"

                    {/* 아이돌 공식 게시판 */}
                    <Route path="idol/:idolId/board" element={<IdolBoardPage />} />

                    <Route path="idol/:idolId/board/write" element={<IdolPostWritePage />} />
                    <Route path="idol/:idolId/board/:postId" element={<IdolPostDetailPage />} />
                    <Route path="idol/:idolId/board/:postId/edit" element={<IdolPostEditPage />} />

                    {/* 투표 페이지 */}
                    <Route path="vote" element={<VotePage />} />

                    {/* 콘서트 페이지 */}
                    <Route path="concert" element={<ConcertPage />} />
                    <Route path="concert/create" element={<ConcertCreatePage />} />

                    {/* 채팅 페이지 */}
                    <Route path="chat" element={<ChatPage />} />
                </Route>

                {/* 아이돌 */}
                <Route path="/idol" element={<IdolPage />} />

                {/* 전체 콘서트 */}
                <Route path="/concert" element={<GlobalConcertPage />} />

                {/* 아이돌 구독 페이지 (그룹/아이돌 선택) */}
                <Route path="/idol/subscribe" element={<IdolSubscribe />} />

                {/* 결제 플로우 */}
                <Route path="/payment" element={<PaymentPage />} />
                <Route path="/payment/complete" element={<PaymentComplete />} />

            </Routes>
        </BrowserRouter>
    );
}

export default App;
