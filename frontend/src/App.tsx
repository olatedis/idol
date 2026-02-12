import './App.css'

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import MainPage from "./pages/main/MainPage.tsx";
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

                {/* 카카오 로그인 */}
                <Route path="/oauth/kakao" element={<OAuthKakao />} />

                {/* 그룹 서비스 */}
                <Route path="/group/:groupId" element={<ServicePage />}>
                    <Route index element={<Navigate to="board" replace />} />
                    <Route path="board" element={<BoardPage />} />
                    <Route path="board/:postId" element={<PostDetailPage />} />
                    <Route path="board/write" element={<PostWritePage />} />
                </Route>

                {/* 아이돌 */}
                <Route path="/idol" element={<IdolPage />} />
            </Routes>
        </BrowserRouter>
    );
}

export default App;
