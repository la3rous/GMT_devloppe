import { useState } from "react";
import { CarFront, LockKeyhole, Mail } from "lucide-react";
import { Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import "./Login.css";

export default function AdminLogin() {
  const { login, isAdmin, user, loading } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [needsVerification, setNeedsVerification] = useState(false);

  if (!loading && user && isAdmin()) return <Navigate to="/admin/dashboard" replace />;

  const submit = async (event) => {
    event.preventDefault();
    setError("");
    setNeedsVerification(false);
    setSubmitting(true);
    try {
      const account = await login(email.trim(), password);
      const role = typeof account.role === "string" ? account.role : account.role?.name;
      if (role === "ADMIN" || role === "ROLE_ADMIN") navigate("/admin/dashboard", { replace: true });
      else setError("Ce compte n'a pas les droits d'administration.");
    } catch (requestError) {
      const status = requestError.response?.status;
      const message = requestError.response?.data?.message || "";
      if (status === 403 && (message.toLowerCase().includes("vérifi") || message.toLowerCase().includes("verifi"))) {
        setNeedsVerification(true);
        setError(message || "Veuillez vérifier votre adresse e-mail avant de vous connecter.");
      } else {
        setError(message || "Connexion impossible. Vérifiez vos identifiants.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="admin-login-page">
      <form className="admin-login-card" onSubmit={submit}>
        <div className="admin-login-mark"><CarFront size={26} /></div>
        <p className="admin-login-eyebrow">GMT RENTAL · ADMINISTRATION</p>
        <h1>Connexion</h1>
        <p className="admin-login-copy">Connectez-vous pour gérer votre activité.</p>
        {error && (
          <div className="admin-error" role="alert">
            {error}
            {needsVerification && (
              <div style={{ marginTop: 10 }}>
                <button
                  type="button"
                  className="admin-button primary"
                  style={{ width: "100%", padding: "9px 12px", fontSize: 13 }}
                  onClick={() => navigate(`/admin/verify-email?email=${encodeURIComponent(email.trim())}`)}
                >
                  Saisir le code de vérification →
                </button>
              </div>
            )}
          </div>
        )}
        <label className="admin-login-field"><span>Adresse e-mail</span><div><Mail size={17} /><input type="email" autoComplete="username" required value={email} onChange={(e) => setEmail(e.target.value)} /></div></label>
        <label className="admin-login-field"><span>Mot de passe</span><div><LockKeyhole size={17} /><input type="password" autoComplete="current-password" required value={password} onChange={(e) => setPassword(e.target.value)} /></div></label>
        <button className="admin-button primary admin-login-submit" disabled={submitting}>{submitting ? "Connexion…" : "Se connecter"}</button>
        <a className="admin-login-back" href="/">Retour au site</a>
      </form>
    </main>
  );
}
