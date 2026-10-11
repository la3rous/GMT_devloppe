import { useEffect, useState } from "react";
import { Bell, Mail, MonitorCog, Save, Send, ShieldCheck } from "lucide-react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { adminApiService } from "../../services/admin/api";

export default function AdminAccountSettings() {
  const { theme, setTheme } = useOutletContext();
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [preferences, setPreferences] = useState({ theme: theme || "dark", quoteNotifications: true, notificationEmail: "" });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null);
  const [testEmail, setTestEmail] = useState("");
  const [testingEmail, setTestingEmail] = useState(false);
  const [testMessage, setTestMessage] = useState(null);

  useEffect(() => {
    adminApiService.preferences().then(({ data }) => {
      setPreferences(data); setTheme(data.theme);
    }).catch((error) => setMessage({ type: "error", text: error.response?.data?.message || "Impossible de charger les préférences." }))
      .finally(() => setLoading(false));
  }, [setTheme]);

  const save = async () => {
    const notificationEmail = (preferences.notificationEmail || "").trim();
    if (notificationEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(notificationEmail)) {
      setMessage({ type: "error", text: "L’adresse e-mail de réception est invalide." });
      return;
    }
    setSaving(true); setMessage(null);
    try {
      const { data } = await adminApiService.updatePreferences(preferences);
      setPreferences(data); setTheme(data.theme);
      window.dispatchEvent(new Event("gmt-admin-preferences-change"));
      setMessage({ type: "success", text: "Préférences enregistrées." });
    } catch (error) { setMessage({ type: "error", text: error.response?.data?.message || "Impossible d’enregistrer les préférences." }); }
    finally { setSaving(false); }
  };

  const sendTestEmail = async () => {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(testEmail.trim())) {
      setTestMessage({ type: "error", text: "Saisissez une adresse e-mail valide." });
      return;
    }
    setTestingEmail(true); setTestMessage(null);
    try {
      const { data } = await adminApiService.testEmail(testEmail);
      setTestMessage({ type: "success", text: `${data.message} Identifiant : ${data.messageId}` });
    } catch (error) {
      const responseMessage = error.response?.data?.message || error.response?.data?.detail || error.response?.data;
      setTestMessage({ type: "error", text: typeof responseMessage === "string" ? responseMessage : "Impossible d’envoyer l’e-mail de test." });
    } finally { setTestingEmail(false); }
  };

  const signOut = async () => {
    if (!window.confirm("Voulez-vous vraiment vous déconnecter de Gestion Rental ?")) return;
    try { await adminApiService.logout(); } catch { /* La session locale doit pouvoir se fermer même si le serveur ne répond pas. */ }
    logout(); navigate("/admin/login", { replace: true });
  };

  return <section className="account-page">
    <div className="admin-page-head"><div><p className="account-eyebrow">VOTRE ESPACE</p><h1>Paramètres du compte</h1><p>Gérez les préférences de votre interface et vos notifications.</p></div></div>
    {message && <p className={message.type === "success" ? "admin-success" : "admin-error"} role={message.type === "success" ? "status" : "alert"}>{message.text}</p>}
    {loading ? <div className="admin-loading">Chargement des préférences…</div> : <>
      <div className="admin-card account-settings-card">
        <div className="account-section-heading"><span className="account-section-icon"><MonitorCog size={19} /></span><div><h2>Préférences générales</h2><p>Personnalisez l’affichage de Gestion Rental.</p></div></div>
        <label className="account-setting-row"><span><strong>Thème</strong><small>Appliqué à l’interface et conservé sur ce compte.</small></span><select value={preferences.theme} onChange={(event) => setPreferences({ ...preferences, theme: event.target.value })}><option value="dark">Sombre</option><option value="light">Clair</option></select></label>
        <div className="account-setting-row"><span><strong>Langue</strong><small>La version actuelle de l’interface est disponible en français.</small></span><span className="admin-badge">Français</span></div>
      </div>
      <div className="admin-card account-settings-card">
        <div className="account-section-heading"><span className="account-section-icon"><Bell size={19} /></span><div><h2>Notifications</h2><p>Choisissez les alertes disponibles dans cette application.</p></div></div>
        <label className="account-setting-row account-toggle-row"><span><strong>Nouvelles demandes de devis</strong><small>Afficher les demandes dans la cloche de notification de l’administration.</small></span><input type="checkbox" checked={preferences.quoteNotifications} onChange={(event) => setPreferences({ ...preferences, quoteNotifications: event.target.checked })} /></label>
        <label className="account-setting-row"><span><strong>Adresse e-mail de réception des notifications</strong><small>Recevez un e-mail lorsqu’une nouvelle demande de devis est créée.</small></span><input type="email" maxLength={254} placeholder="exemple@email.com" value={preferences.notificationEmail || ""} onChange={(event) => setPreferences({ ...preferences, notificationEmail: event.target.value })} /></label>
        <div className="account-email-test">
          <div className="account-section-heading"><span className="account-section-icon"><Mail size={18} /></span><div><h2>Tester l’envoi d’e-mail</h2><p>Un vrai message sera envoyé à l’adresse saisie. Limite : un test par minute.</p></div></div>
          <div className="account-email-test-form"><input type="email" maxLength={254} placeholder="exemple@email.com" value={testEmail} onChange={(event) => setTestEmail(event.target.value)} /><button type="button" className="admin-button secondary" onClick={sendTestEmail} disabled={testingEmail || !testEmail.trim()}><Send size={15} />{testingEmail ? "Envoi…" : "Envoyer un test"}</button></div>
          {testMessage && <p className={testMessage.type === "success" ? "admin-success" : "admin-error"} role={testMessage.type === "success" ? "status" : "alert"}>{testMessage.text}</p>}
        </div>
      </div>
      <div className="admin-card account-settings-card">
        <div className="account-section-heading"><span className="account-section-icon"><ShieldCheck size={19} /></span><div><h2>Session actuelle</h2><p>Compte connecté à cette session.</p></div></div>
        <div className="account-session-info"><span><strong>{user?.email || "Administrateur"}</strong><small>Authentification sécurisée par jeton d’accès.</small></span><button type="button" className="admin-button danger" onClick={signOut}>Se déconnecter</button></div>
      </div>
      <div className="account-page-actions"><button type="button" className="admin-button primary" onClick={save} disabled={saving}><Save size={15} />{saving ? "Enregistrement…" : "Enregistrer"}</button></div>
    </>}
  </section>;
}
