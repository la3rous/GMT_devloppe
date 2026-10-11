import { useCallback, useEffect, useRef, useState } from "react";
import { Bell, CheckCheck, ExternalLink } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { adminApiService } from "../../services/admin/api";
import "./NotificationBell.css";

function formatDateTime(value) {
  if (!value) return "Date inconnue";
  return new Intl.DateTimeFormat("fr-TN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

export default function NotificationBell() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [isOpen, setIsOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState("");
  const [toast, setToast] = useState(false);
  const [bellPulse, setBellPulse] = useState(false);
  const initialized = useRef(false);
  const latestNotificationId = useRef(0);
  const requestGeneration = useRef(0);

  const refresh = useCallback(async () => {
    const generation = ++requestGeneration.current;
    try {
      const [notificationsResponse, countResponse, preferencesResponse] = await Promise.all([
        adminApiService.notifications(),
        adminApiService.unreadNotificationCount(),
        adminApiService.preferences(),
      ]);
      if (generation !== requestGeneration.current) return true;
      const serverNotifications = notificationsResponse.data || [];
      const notificationsEnabled = preferencesResponse.data?.quoteNotifications !== false;
      const nextNotifications = notificationsEnabled ? serverNotifications : [];
      const newestId = serverNotifications.reduce(
        (highest, notification) => Math.max(highest, Number(notification.id) || 0),
        0,
      );

      if (notificationsEnabled && initialized.current && newestId > latestNotificationId.current) {
        setToast(true);
        setBellPulse(true);
        window.setTimeout(() => setBellPulse(false), 1100);
        window.setTimeout(() => setToast(false), 6000);
      }
      latestNotificationId.current = newestId;
      initialized.current = true;
      setNotifications(nextNotifications);
      setUnreadCount(notificationsEnabled ? Number(countResponse.data?.count) || 0 : 0);
      setError("");
    } catch (requestError) {
      if (generation === requestGeneration.current) {
        setError(requestError.response?.data?.message || "Impossible de charger les notifications.");
      }
      return false;
    } finally {
      if (generation === requestGeneration.current) setLoading(false);
    }
    return true;
  }, []);

  useEffect(() => {
    const initialRefreshId = window.setTimeout(refresh, 0);
    const intervalId = window.setInterval(refresh, 15000);
    const refreshPreferences = () => refresh();
    window.addEventListener("gmt-admin-preferences-change", refreshPreferences);
    return () => {
      window.clearTimeout(initialRefreshId);
      window.clearInterval(intervalId);
      window.removeEventListener("gmt-admin-preferences-change", refreshPreferences);
    };
  }, [refresh]);

  useEffect(() => {
    if (!isOpen) return undefined;
    const closeOnEscape = (event) => {
      if (event.key === "Escape") setIsOpen(false);
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [isOpen]);

  const openQuote = async (notification) => {
    setUpdating(true);
    try {
      if (!notification.lu) {
        await adminApiService.markNotificationAsRead(notification.id);
        requestGeneration.current += 1;
        setNotifications((current) => current.map((item) => (
          item.id === notification.id ? { ...item, lu: true } : item
        )));
        setUnreadCount((count) => Math.max(0, count - 1));
      }
      setIsOpen(false);
      navigate(`/admin/devis/${notification.devisId}`);
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Impossible de marquer cette notification comme lue.");
    } finally {
      setUpdating(false);
    }
  };

  const markAllAsRead = async () => {
    setUpdating(true);
    try {
      await adminApiService.markAllNotificationsAsRead();
      requestGeneration.current += 1;
      setNotifications((current) => current.map((notification) => ({ ...notification, lu: true })));
      setUnreadCount(0);
      setError("");
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Impossible de marquer les notifications comme lues.");
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div className="admin-notification" onBlur={(event) => {
      if (!event.currentTarget.contains(event.relatedTarget)) setIsOpen(false);
    }}>
      <button
        className={`admin-notification-trigger${bellPulse ? " is-pulsing" : ""}`}
        type="button"
        title="Notifications"
        aria-label={`Notifications${unreadCount ? `, ${unreadCount} non lue${unreadCount > 1 ? "s" : ""}` : ""}`}
        aria-expanded={isOpen}
        onClick={() => {
          const nextOpen = !isOpen;
          setIsOpen(nextOpen);
          if (nextOpen) refresh();
        }}
      >
        <Bell size={20} aria-hidden="true" />
        {unreadCount > 0 && (
          <span className="admin-notification-count">{unreadCount > 99 ? "99+" : unreadCount}</span>
        )}
      </button>

      {toast && (
        <div className="admin-notification-toast" role="status">
          <Bell size={17} />
          <span>Nouveau devis reçu ! Un client vient d'envoyer une demande de location.</span>
          <button type="button" aria-label="Fermer" onClick={() => setToast(false)}>×</button>
        </div>
      )}

      {isOpen && (
        <section className="admin-notification-menu" aria-label="Notifications">
          <header className="admin-notification-menu-head">
            <div>
              <h2>Notifications</h2>
              <span>{unreadCount} non lue{unreadCount > 1 ? "s" : ""}</span>
            </div>
            <button
              type="button"
              className="admin-notification-read-all"
              disabled={updating || unreadCount === 0}
              onClick={markAllAsRead}
            >
              <CheckCheck size={15} /> Tout marquer comme lu
            </button>
          </header>

          {error && <p className="admin-notification-error" role="alert">{error}</p>}
          {loading ? (
            <p className="admin-notification-empty">Chargement des notifications…</p>
          ) : notifications.length === 0 ? (
            <p className="admin-notification-empty">Aucune notification pour le moment.</p>
          ) : (
            <div className="admin-notification-list">
              {notifications.map((notification) => (
                <article
                  className={`admin-notification-item${notification.lu ? " is-read" : " is-unread"}`}
                  key={notification.id}
                >
                  <div className="admin-notification-item-heading">
                    <strong>{notification.prenom} {notification.nom}</strong>
                    <span className={`admin-notification-status${notification.lu ? " read" : ""}`}>
                      {notification.lu ? "Lu" : "Nouveau"}
                    </span>
                  </div>
                  <p className="admin-notification-vehicle">{notification.vehicule}</p>
                  <p className="admin-notification-period">
                    {notification.dateDebut || "—"} – {notification.dateFin || "—"}
                  </p>
                  <time dateTime={notification.dateCreation}>{formatDateTime(notification.dateCreation)}</time>
                  <button
                    type="button"
                    className="admin-notification-view"
                    disabled={updating}
                    onClick={() => openQuote(notification)}
                  >
                    Voir le devis <ExternalLink size={14} />
                  </button>
                </article>
              ))}
            </div>
          )}
        </section>
      )}
    </div>
  );
}
