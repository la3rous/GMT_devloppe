import { useState } from "react"
import { NavLink } from "react-router-dom"
import "./Header.css"
import carLogo from "../../../assets/images/logo/car-logo.png"
import { Moon, Sun } from "lucide-react"

function Header({ theme, setTheme }) {
  const [menuOpen, setMenuOpen] = useState(false)

  const closeMenu = () => {
    setMenuOpen(false)
  }

  return (
    <header>

      {/* LOGO */}
      <div className="logo">
        <img src={carLogo} alt="GMT" />
        <span>GMT</span>
      </div>

      {/* MENU DESKTOP */}
      <div className="header-right">

        <nav>
          <NavLink to="/" end>
            Accueil
          </NavLink>

          <NavLink to="/catalogue">
            Véhicules
          </NavLink>

          <NavLink to="/about">
            À propos
          </NavLink>

          <NavLink to="/contact">
            Contact
          </NavLink>

          <NavLink to="/faq">
            FAQ
          </NavLink>
        </nav>

      </div>

      <button
        type="button"
        className="theme-toggle"
        onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
        aria-label={theme === "dark" ? "Passer en mode jour" : "Passer en mode nuit"}
        aria-pressed={theme === "light"}
        title={theme === "dark" ? "Passer en mode jour" : "Passer en mode nuit"}
      >
        {theme === "dark" ? <Sun size={20} aria-hidden="true" /> : <Moon size={20} aria-hidden="true" />}
        <span>{theme === "dark" ? "Jour" : "Nuit"}</span>
      </button>

      {/* BOUTON HAMBURGER MOBILE */}
      <button
        className="menu-button"
        onClick={() => setMenuOpen(!menuOpen)}
        aria-label="Ouvrir le menu"
      >
        {menuOpen ? "✕" : "☰"}
      </button>

      {/* MENU MOBILE */}
      {menuOpen && (
        <div className="mobile-menu">

          <nav>

            <NavLink to="/" end onClick={closeMenu}>
              Accueil
            </NavLink>

            <NavLink to="/catalogue" onClick={closeMenu}>
              Véhicules
            </NavLink>

            <NavLink to="/about" onClick={closeMenu}>
              À propos
            </NavLink>

            <NavLink to="/contact" onClick={closeMenu}>
              Contact
            </NavLink>

            <NavLink to="/faq" onClick={closeMenu}>
              FAQ
            </NavLink>

          </nav>

        </div>
      )}

    </header>
  )
}

export default Header
