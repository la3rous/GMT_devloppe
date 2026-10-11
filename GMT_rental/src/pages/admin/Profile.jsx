import { useEffect, useState } from "react";
import { CheckCircle2, Mail, Save, UserRound } from "lucide-react";
import { useAuth } from "../../context/AuthContext";
import { adminApiService } from "../../services/admin/api";

const formatDate = (value) => value ? new Intl.DateTimeFormat("fr-FR", { dateStyle: "long", timeStyle: "short" }).format(new Date(value)) : "Non disponible";

export default function AdminProfile() {
  const { updateAuthenticatedUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ nom: "", prenom: "" });
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [emailStep, setEmailStep] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    adminApiService.profile().then(({ data }) => {
      setProfile(data); setForm({ nom: data.nom || "", prenom: data.prenom || "" }); setEmail(data.email || "");
    }).catch((error) => setMessage({ type: "error", text: error.response?.data?.message || "Impossible de charger le profil." }))
      .finally(() => setLoading(false));
  }, []);

  const syncUser = (nextProfile, token) => {
    const cached = JSON.parse(localStorage.getItem("user") || "{}");
    updateAuthenticatedUser({ ...cached, id: nextProfile.id, nom: nextProfile.nom, prenom: nextProfile.prenom, email: nextProfile.email, role: nextProfile.role }, token);
  };

  const saveProfile = async (event) => {
    event.preventDefault(); setSaving(true); setMessage(null);
    try {
      const { data } = await adminApiService.updateProfile(form);
      setProfile(data); setForm({ nom: data.nom, prenom: data.prenom }); syncUser(data);
      setMessage({ type: "success", text: "Vos informations ont été enregistrées." });
    } catch (error) { setMessage({ type: "error", text: error.response?.data?.message || "Impossible d’enregistrer le profil." }); }
    finally { setSaving(false); }
  };

  const requestCode = async (event) => {
    event.preventDefault(); setSaving(true); setMessage(null);
    try {
      await adminApiService.sendProfileEmailCode(email);
      setEmailStep(true); setMessage({ type: "success", text: "Un code de vérification a été envoyé à cette adresse." });
    } catch (error) { setMessage({ type: "error", text: error.response?.data?.message || "Impossible d’envoyer le code. Vérifiez la configuration e-mail." }); }
    finally { setSaving(false); }
  };

  const verifyEmail = async (event) => {
    event.preventDefault(); setSaving(true); setMessage(null);
    try {
      const { data } = await adminApiService.verifyProfileEmail(email, code);
      setProfile(data.profile); setEmail(data.profile.email); setEmailStep(false); setCode(""); syncUser(data.profile, data.token);
      setMessage({ type: "success", text: "Adresse e-mail vérifiée et mise à jour." });
    } catch (error) { setMessage({ type: "error", text: error.response?.data?.message || "Le code n’a pas pu être vérifié." }); }
    finally { setSaving(false); }
  };

  if (loading) return <div className="admin-loading">Chargement du profil…</div>;

  return <section className="account-page">
    <div className="admin-page-head"><div><p className="account-eyebrow">COMPTE ADMINISTRATEUR</p><h1>Mon profil</h1><p>Consultez et mettez à jour les informations de votre compte.</p></div></div>
    {message && <p className={message.type === "success" ? "admin-success" : "admin-error"} role={message.type === "success" ? "status" : "alert"}>{message.text}</p>}
    {!profile ? <div className="admin-card account-empty">Le profil n’a pas pu être chargé.</div> : <>
      <div className="admin-card account-profile-banner">
        <span className="account-avatar"><UserRound size={27} aria-hidden="true" /></span>
        <div><h2>{[profile.prenom, profile.nom].filter(Boolean).join(" ") || "Administrateur"}</h2><p>{profile.email}</p><span className="account-role">Administrateur</span></div>
      </div>
      <div className="account-columns">
        <form className="admin-card account-form" onSubmit={saveProfile}>
          <h2>Informations personnelles</h2>
          <label className="admin-field"><span>Prénom</span><input required maxLength="100" value={form.prenom} onChange={(event) => setForm({ ...form, prenom: event.target.value })} /></label>
          <label className="admin-field"><span>Nom</span><input required maxLength="100" value={form.nom} onChange={(event) => setForm({ ...form, nom: event.target.value })} /></label>
          <div className="account-form-actions"><button type="button" className="admin-button" onClick={() => setForm({ nom: profile.nom || "", prenom: profile.prenom || "" })}>Annuler</button><button className="admin-button primary" disabled={saving}><Save size={15} />{saving ? "Enregistrement…" : "Enregistrer les modifications"}</button></div>
        </form>
        <div className="admin-card account-form">
          <h2>Adresse e-mail</h2>
          <p className="admin-muted">Une nouvelle adresse doit être vérifiée avant de remplacer celle du compte.</p>
          {!emailStep ? <form onSubmit={requestCode}>
            <label className="admin-field"><span>Adresse e-mail</span><input type="email" required maxLength="320" value={email} onChange={(event) => setEmail(event.target.value)} /></label>
            <div className="account-form-actions"><button type="button" className="admin-button" onClick={() => setEmail(profile.email)}>Annuler</button><button className="admin-button primary" disabled={saving || email.toLowerCase() === profile.email.toLowerCase()}><Mail size={15} />{saving ? "Envoi…" : "Vérifier la nouvelle adresse"}</button></div>
          </form> : <form onSubmit={verifyEmail}>
            <p className="account-note">Saisissez le code à 6 chiffres envoyé à <strong>{email}</strong>.</p>
            <label className="admin-field"><span>Code de vérification</span><input required inputMode="numeric" pattern="[0-9]{6}" maxLength="6" value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, ""))} /></label>
            <div className="account-form-actions"><button type="button" className="admin-button" onClick={() => { setEmailStep(false); setCode(""); }}>Annuler</button><button className="admin-button primary" disabled={saving}><CheckCircle2 size={15} />{saving ? "Vérification…" : "Confirmer l’adresse"}</button></div>
          </form>}
        </div>
      </div>
      <div className="admin-card account-meta"><h2>Détails du compte</h2><dl><div><dt>Rôle</dt><dd>Administrateur</dd></div><div><dt>Adresse vérifiée</dt><dd>{profile.emailVerified ? "Oui" : "Non"}</dd></div><div><dt>Compte créé</dt><dd>{formatDate(profile.createdAt)}</dd></div><div><dt>Dernière connexion</dt><dd>{formatDate(profile.lastLoginAt)}</dd></div></dl></div>
    </>}
  </section>;
}
