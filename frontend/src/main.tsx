import { createRoot } from 'react-dom/client'
import './index.css'
import "@toast-ui/editor/dist/toastui-editor.css";
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(

    <App />,
)
