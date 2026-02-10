import './App.css'

import {BrowserRouter, Routes, Route, Navigate} from "react-router-dom";
import MainPage from "./pages/test/MainPage.tsx";
import ServicePage from "./pages/3-1/ServicePage.tsx";
import BoardPage from "./pages/3-1/BoardPage.tsx";
import OAuthKakao from "./pages/auth/OAuthKakao.tsx";

function App() {
    return (
        <>
            <BrowserRouter>
                <Routes>
                    <Route path="/" element={<MainPage />} />
                    
                    {/* 카카오 로그인 리다이렉트 */}
                    <Route path="/oauth/kakao" element={<OAuthKakao />} />

                    {/* 3-1 */}
                    <Route path="/group/:groupId" element={<ServicePage />}>
                        <Route index element={<Navigate to="board" replace />} />
                        <Route path="board" element={<BoardPage />} />
                        {/*<Route path="board/:postId" element={<PostDetailPage />} />*/}


                    </Route>
                </Routes>

            </BrowserRouter>
        </>
    )
}

export default App
