import './App.css'

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import MainPage from "./pages/main/MainPage.tsx";
import NoticeListPage from "./pages/main/notice/NoticeListPage.tsx";
import NoticeDetailPage from "./pages/main/notice/NoticeDetailPage.tsx";
import NoticeWritePage from "./pages/main/notice/NoticeWritePage.tsx";
import NoticeEditPage from "./pages/main/notice/NoticeEditPage.tsx";
import GroupServicePage from "./pages/3-1/GroupServicePage.tsx";
import GroupBoardPage from "./pages/3-1/GroupBoardPage.tsx";
import IdolPage from "./pages/2-1/IdolPage.tsx";
import GroupPostWritePage from "./pages/3-1/GroupPostWritePage.tsx";
import GroupPostDetailPage from "./pages/3-1/GroupPostDetailPage.tsx";
import OAuthKakao from "./pages/auth/OAuthKakao.tsx";
import IdolSubscribe from "./pages/payment/IdolSubscribe.tsx";
import PaymentPage from "./pages/payment/PaymentPage.tsx";
import PaymentComplete from "./pages/payment/PaymentComplete.tsx";
import VotePage from "./pages/3-1/VotePage.tsx"; // 추가됨

function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* 메인 */}
                <Route path="/" element={<MainPage />} />

                {/* 햄버거바에서 타고가는 공지사항 */}
                <Route path="/notices" element={<NoticeListPage />} />
                <Route path="/notices/:postId" element={<NoticeDetailPage />} />

                {/* 공지 작성/수정 (관리자) */}
                <Route path="/admin/notices/write" element={<NoticeWritePage />} />
                <Route path="/admin/notices/edit/:postId" element={<NoticeEditPage />} />

                {/* 카카오 로그인 */}
                <Route path="/oauth/kakao" element={<OAuthKakao />} />

                {/* 그룹 서비스 */}
                <Route path="/group/:groupId" element={<GroupServicePage />}>
                    <Route index element={<Navigate to="board" replace />} />
                    <Route path="board" element={<GroupBoardPage />} />

                    {/* board/write가 board/:postId에 먹히는 케이스 방지 */}
                    <Route path="board/write" element={<GroupPostWritePage />} />
                    <Route path="board/:postId" element={<GroupPostDetailPage />} />
                </Route>

                {/* 아이돌 */}
                <Route path="/idol" element={<IdolPage />} />

                {/*    테스트중*/}
                <Route path="/idol/:idolId/subscribe" element={<IdolSubscribe />} />

                {/* 결제 플로우 */}
                <Route path="/payment/:idolId" element={<PaymentPage />} />
                <Route path="/payment/complete" element={<PaymentComplete />} />

                {/* 투표 페이지 */}
                <Route path="/vote" element={<VotePage />} />
                <Route path="/vote/:groupId" element={<VotePage />} /> {/* 그룹 전용 투표 */}
            </Routes>
        </BrowserRouter>
    );
}

export default App;
