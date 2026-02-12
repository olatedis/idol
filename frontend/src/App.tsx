import './App.css'

import {BrowserRouter, Routes, Route, Navigate} from "react-router-dom";
import MainPage from "./pages/main/MainPage.tsx";
import ServicePage from "./pages/3-1/ServicePage.tsx";
import BoardPage from "./pages/3-1/BoardPage.tsx";
// import Header from "./pages/main/Header.tsx";
import IdolPage from "./pages/2-1/IdolPage.tsx";

function App() {
    return (
        <>
            <BrowserRouter>
                {/*<Header />*/}
                <Routes>
                    <Route path="/" element={<MainPage />} />
                    
                    {/* 카카오 로그인 리다이렉트 */}
                    {/*<Route path="/oauth/kakao" element={<OAuthKakao />} />*/}

                    {/* 3-1 */}
                    <Route path="/group/:groupId" element={<ServicePage />}>
                        <Route index element={<Navigate to="board" replace />} />
                        <Route path="board" element={<BoardPage />} />
                        {/*<Route path="board/:postId" element={<PostDetailPage />} />*/}
                    </Route>
                    <Route path="/idol" element={<IdolPage/>} />
                </Routes>

            </BrowserRouter>
        </>
    )
}

export default App
