import './App.css'

import {BrowserRouter, Routes, Route, Navigate} from "react-router-dom";
import MainPage from "./pages/main/MainPage.tsx";
import NoticeListPage from "./pages/main/notice/NoticeListPage.tsx";
import NoticeDetailPage from "./pages/main/notice/NoticeDetailPage.tsx";
import NoticeWritePage from "./pages/main/notice/NoticeWritePage.tsx";
import NoticeEditPage from "./pages/main/notice/NoticeEditPage.tsx";
import ServicePage from "./pages/3-1/ServicePage.tsx";
import BoardPage from "./pages/3-1/BoardPage.tsx";
import IdolPage from "./pages/2-1/IdolPage.tsx";
import PostWritePage from "./pages/3-1/PostWritePage.tsx";
import PostDetailPage from "./pages/3-1/PostDetailPage.tsx";
import OAuthKakao from "./pages/auth/OAuthKakao.tsx";

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
                <Route path="/group/:groupId" element={<ServicePage />}>
                    <Route index element={<Navigate to="board" replace />} />
                    <Route path="board" element={<BoardPage />} />

                    {/* board/write가 board/:postId에 먹히는 케이스 방지 */}
                    <Route path="board/write" element={<PostWritePage />} />
                    <Route path="board/:postId" element={<PostDetailPage />} />
                </Route>

                {/* 아이돌 */}
                <Route path="/idol" element={<IdolPage />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
