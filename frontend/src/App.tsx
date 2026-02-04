import './App.css'

import {BrowserRouter,Routes,Route} from "react-router-dom";
import MainPage from "./pages/test/MainPage.tsx";

function App() {
  return (
    <>
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<MainPage />} />
            </Routes>

        </BrowserRouter>
    </>
  )
}

export default App
