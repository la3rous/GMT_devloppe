import { useEffect, useMemo, useState } from "react";
import { Eye, EyeOff, KeyRound, ShieldCheck } from "lucide-react";
import { adminApiService } from "../../services/admin/api";

const dateLabel = (value) => value ? new Intl.DateTimeFormat("fr-FR", { dateStyle: "long", timeStyle: "short" }).format(new Date(value)) : "Non disponible";

export default function AdminSecurity() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ currentPassword: "", newPassword: "", confirmation: "" });
  const [showPasswords, setShowPasswords] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null);

  useEffect(() => { adminApiService.profile().then(({ data }) => setProfile(data)).catch((error) => setMessage({ type: "error", text: error.response?.data?.message || "Impossible de charger l’état du compte." })); }, []);
  const strength = useMemo(() => {
    const value = form.newPassword;
    if (!value) return 0;
    return [value.length >= 8, /[a-z]/.test(value), /[A-Z]/.test(value), /\d/.test(value), /[^A-Za-z0-9]/.test(value)].filter(Boolean).length;
  }, [form.newPassword]);
  const strengthText = ["", "Très faible", "Faible", "Moyen", "Bon", "Fort"][strength];

  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setMessage(null);
    try {
      await adminApiService.changePassword(form);
      setForm({ currentPassword: "", newPassword: "", confirmation: "" });
      const { data } = await adminApiService.profile(); setProfile(data);
      setMessage({ type: "success", text: "Mot de passe modifié. Il a été enregistré de manière chiffrée." });
    } catch (error) { setMessage({ type: "error", text: error.response?.data?.message || "Impossible de modifier le mot de passe." }); }
    finally { setSaving(false); }
  };

  return <section className="account-page">
    <div className="admin-page-head"><div><p className="account-eyebrow">PROTECTION DU COMPTE</p><h1>Sécurité et mot de passe</h1><p>Modifiez votre mot de passe et vérifiez l’état de sécurité du compte.</p></div></div>
    {message && <p className={message.type === "success" ? "admin-success" : "admin-error"} role={message.type === "success" ? "status" : "alert"}>{message.text}</p>}
    <div className="account-columns">
      <form className="admin-card account-form" onSubmit={submit}>
        <div className="account-section-heading"><span className="account-section-icon"><KeyRound size={19} /></span><div><h2>Modifier le mot de passe</h2><p>Le mot de passe actuel est vérifié côté serveur.</p></div></div>
        <label className="admin-field"><span>Mot de passe actuel</span><div className="password-input-wrap"><input required autoComplete="current-password" type={showPasswords ? "text" : "password"} value={form.currentPassword} onChange={(event) => setForm({ ...form, currentPassword: event.target.value })} /><button type="button" aria-label={showPasswords ? "Masquer les mots de passe" : "Afficher les mots de passe"} onClick={() => setShowPasswords(!showPasswords)}>{showPasswords ? <EyeOff size={17} /> : <Eye size={17} />}</button></div></label>
        <label className="admin-field"><span>Nouveau mot de passe</span><input required minLength="8" maxLength="100" autoComplete="new-password" type={showPasswords ? "text" : "password"} value={form.newPassword} onChange={(event) => setForm({ ...form, newPassword: event.target.value })} /></label>
        <div className="password-strength" aria-live="polite"><span className={`password-strength-bar strength-${strength}`} /><span>Robustesse : {strengthText || "à évaluer"}</span><small>8 caractères minimum. Une majuscule, un chiffre et un symbole renforcent le mot de passe.</small></div>
        <label className="admin-field"><span>Confirmer le nouveau mot de passe</span><input required autoComplete="new-password" type={showPasswords ? "text" : "password"} value={form.confirmation} onChange={(event) => setForm({ ...form, confirmation: event.target.value })} /></label>
        <div className="account-form-actions"><button className="admin-button primary" disabled={saving || form.newPassword !== form.confirmation || strength < 1}>{saving ? "Modification…" : "Modifier mon mot de passe"}</button></div>
      </form>
      <div className="admin-card account-settings-card">
        <div className="account-section-heading"><span className="account-section-icon"><ShieldCheck size={19} /></span><div><h2>État du compte</h2><p>Informations réellement disponibles.</p></div></div>
        {profile ? <dl className="account-security-list"><div><dt>Adresse e-mail vérifiée</dt><dd>{profile.emailVerified ? "Oui" : "Non"}</dd></div><div><dt>Mot de passe modifié le</dt><dd>{dateLabel(profile.passwordUpdatedAt)}</dd></div><div><dt>Dernière connexion</dt><dd>{dateLabel(profile.lastLoginAt)}</dd></div></dl> : <p className="admin-muted">Chargement des informations…</p>}
        <p className="account-note">Les sessions sont stateless avec JWT : l’application ne suit pas les appareils connectés et ne peut pas fermer sélectivement les autres sessions.</p>
        <div className="account-security-note"><strong>Authentification à deux facteurs</strong><span>Cette fonction n’est pas encore prise en charge.</span></div>
      </div>
    </div>
  </section>;
}
