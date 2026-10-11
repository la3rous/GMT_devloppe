import { useEffect, useRef, useState } from "react";
import { Activity, CarFront, ChartNoAxesCombined, ChevronDown, CircleHelp, FileText, FolderKanban, LogOut, Settings, Shield, UserRound, Users } from "lucide-react";
import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import NotificationBell from "./NotificationBell";
import { adminApiService } from "../../services/admin/api";
import "./AdminLayout.css";

const links = [
  { to: "/admin/dashboard", label: "Tableau de bord", icon: ChartNoAxesCombined },
  { to: "/admin/vehicules", label: "Véhicules", icon: CarFront },
  { to: "/admin/categories", label: "Catégories", icon: FolderKanban },
  { to: "/admin/devis", label: "Demandes de devis", icon: FileText },
  { to: "/admin/users", label: "Utilisateurs", icon: Users },
  { to: "/admin/settings", label: "Paramètres", icon: Settings },
];

export default function AdminLayout({ theme, setTheme }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);
  const profileTriggerRef = useRef(null);
  const profileMenuRef = useRef(null);
  const signOut = async () => {
    if (!window.confirm("Voulez-vous vraiment vous déconnecter de Gestion Rental ?")) return;
    try { await adminApiService.logout(); } catch { /* La déconnexion locale reste disponible si le serveur est inaccessible. */ }
    logout(); navigate("/admin/login", { replace: true });
  };
  const fullName = [user?.prenom, user?.nom].filter(Boolean).join(" ") || "Administrateur";
  const avatarLetter = (user?.prenom || user?.nom || user?.email || "A").slice(0, 1).toUpperCase();

  useEffect(() => {
    if (!profileMenuOpen) return undefined;

    const closeOnOutsideClick = (event) => {
      if (!profileMenuRef.current?.contains(event.target) && !profileTriggerRef.current?.contains(event.target)) {
        setProfileMenuOpen(false);
      }
    };
    const closeOnEscape = (event) => {
      if (event.key === "Escape") {
        setProfileMenuOpen(false);
        profileTriggerRef.current?.focus();
      }
    };

    document.addEventListener("pointerdown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    profileMenuRef.current?.querySelector('[role="menuitem"]:not([aria-disabled="true"])')?.focus();

    return () => {
      document.removeEventListener("pointerdown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [profileMenuOpen]);

  const handleProfileMenuKeyDown = (event) => {
    const items = Array.from(profileMenuRef.current?.querySelectorAll('[role="menuitem"]:not([aria-disabled="true"])') || []);
    if (!items.length) return;
    const currentIndex = items.indexOf(document.activeElement);
    let nextIndex;

    if (event.key === "ArrowDown") nextIndex = (currentIndex + 1) % items.length;
    else if (event.key === "ArrowUp") nextIndex = (currentIndex - 1 + items.length) % items.length;
    else if (event.key === "Home") nextIndex = 0;
    else if (event.key === "End") nextIndex = items.length - 1;
    else return;

    event.preventDefault();
    items[nextIndex].focus();
  };

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-brand"><span className="admin-brand-mark">GMT</span><span>Gestion Rental<small>ESPACE ADMIN</small></span></div>
        <nav className="admin-nav" aria-label="Navigation administration">
          {links.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} end={to === "/admin/dashboard"} className={({ isActive }) => `admin-nav-link${isActive ? " active" : ""}`}>
              <Icon size={18} /><span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <button type="button" className="admin-logout" onClick={signOut}><LogOut size={17} /> Déconnexion</button>
      </aside>
      <div className="admin-main">
        <header className="admin-topbar">
          <span>Administration</span>
          <div className="admin-topbar-actions">
            <NotificationBell />
            <div className="admin-profile">
              <button
                ref={profileTriggerRef}
                type="button"
                className="admin-profile-trigger"
                aria-haspopup="menu"
                aria-expanded={profileMenuOpen}
                aria-controls="admin-profile-menu"
                aria-label={`Profil de ${fullName}, ${user?.email || ""}`}
                onClick={() => setProfileMenuOpen((open) => !open)}
                onKeyDown={(event) => {
                  if (event.key === "ArrowDown") {
                    event.preventDefault();
                    setProfileMenuOpen(true);
                  }
                }}
              >
                <span className="admin-avatar">{avatarLetter}</span>
                <span className="admin-profile-email">{user?.email || fullName}</span>
                <ChevronDown size={15} aria-hidden="true" />
              </button>
              {profileMenuOpen && (
                <div
                  ref={profileMenuRef}
                  id="admin-profile-menu"
                  className="admin-profile-menu"
                  role="menu"
                  aria-label="Menu du profil"
                  onKeyDown={handleProfileMenuKeyDown}
                >
                  <div className="admin-profile-summary">
                    <span className="admin-avatar admin-profile-avatar">{avatarLetter}</span>
                    <div className="admin-profile-details">
                      <strong>{fullName}</strong>
                      <span>{user?.email || ""}</span>
                      <span className="admin-profile-role">Administrateur</span>
                    </div>
                  </div>
                  <div className="admin-profile-divider" />
                  <NavLink to="/admin/profile" role="menuitem" className="admin-profile-item" onClick={() => setProfileMenuOpen(false)}>
                    <UserRound size={17} aria-hidden="true" /><span>Mon profil</span>
                  </NavLink>
                  <NavLink to="/admin/account-settings" role="menuitem" className="admin-profile-item" onClick={() => setProfileMenuOpen(false)}>
                    <Settings size={17} aria-hidden="true" /><span>Paramètres du compte</span>
                  </NavLink>
                  <NavLink to="/admin/security" role="menuitem" className="admin-profile-item" onClick={() => setProfileMenuOpen(false)}>
                    <Shield size={17} aria-hidden="true" /><span>Sécurité et mot de passe</span>
                  </NavLink>
                  <NavLink to="/admin/activity" role="menuitem" className="admin-profile-item" onClick={() => setProfileMenuOpen(false)}>
                    <Activity size={17} aria-hidden="true" /><span>Historique d’activité</span>
                  </NavLink>
                  <NavLink to="/admin/help" role="menuitem" className="admin-profile-item" onClick={() => setProfileMenuOpen(false)}>
                    <CircleHelp size={17} aria-hidden="true" /><span>Aide et assistance</span>
                  </NavLink>
                  <div className="admin-profile-divider" />
                  <button type="button" role="menuitem" className="admin-profile-item admin-profile-signout" onClick={() => { setProfileMenuOpen(false); signOut(); }}>
                    <LogOut size={17} aria-hidden="true" /><span>Déconnexion</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>
        <main className="admin-content"><Outlet context={{ theme, setTheme }} /></main>
      </div>
    </div>
  );
}
