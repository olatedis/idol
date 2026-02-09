import './App.css'

import {BrowserRouter,Routes,Route} from "react-router-dom";
import MainPage from "./pages/test/MainPage.tsx";
import ServicePage from "./pages/3-1/ServicePage.tsx";

function App() {
  return (
    <>
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<MainPage />} />

                {/* 3-1 */}
                <Route path="/idol/:idolId" element={<ServicePage />} />

            </Routes>

        </BrowserRouter>
    </>
  )
}

export default App
