import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import '@fontsource/space-grotesk/500.css'
import '@fontsource/space-grotesk/700.css'
import '@fontsource/bebas-neue/400.css'
import '@fontsource-variable/inter'
import './index.css'
import './styles/theme.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
