import { useState } from "react";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function getInitialForm(user, role) {
  return {
    nom: user?.nom || "",
    prenom: user?.prenom || "",
    email: user?.email || "",
    motDePasse: "",
    role: user
      ? (user.role || "USER").replace(/^ROLE_/i, "").toUpperCase()
      : role || "USER",
    actif: user ? user.actif !== false : true,
  };
}

export default function UserModal({
  user,
  role,
  saving = false,
  error = "",
  onClearError,
  onClose,
  onSave,
}) {
  const isEditing = Boolean(user);
  const [formError, setFormError] = useState("");
  const [form, setForm] = useState(() => getInitialForm(user, role));

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((previous) => ({ ...previous, [name]: value }));
    setFormError("");
    onClearError?.();
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    setFormError("");

    if (!form.prenom.trim() || !form.nom.trim()) {
      setFormError("Le prénom et le nom sont obligatoires.");
      return;
    }
    if (!EMAIL_PATTERN.test(form.email.trim())) {
      setFormError("Saisissez une adresse e-mail valide.");
      return;
    }
    if (!isEditing && !form.motDePasse.trim()) {
      setFormError("Le mot de passe est obligatoire pour créer un compte.");
      return;
    }
    if (!isEditing && !["USER", "ADMIN"].includes(form.role)) {
      setFormError("Choisissez un rôle valide.");
      return;
    }

    const payload = {
      ...form,
      nom: form.nom.trim(),
      prenom: form.prenom.trim(),
      email: form.email.trim().toLowerCase(),
      ...(isEditing && !form.motDePasse ? { motDePasse: undefined } : {}),
    };

    if (isEditing) delete payload.role;
    onSave(payload);
  };

  const visibleError = formError || error;

  return (
    <div className="user-modal-overlay">
      <div className="user-modal" role="dialog" aria-modal="true" aria-labelledby="user-modal-title">
        <div className="user-modal-header">
          <div>
            <h2 id="user-modal-title">
              {isEditing
                ? "Modifier l'utilisateur"
                : role === "ADMIN"
                  ? "Ajouter un administrateur"
                  : "Ajouter un utilisateur"}
            </h2>
            <p>
              {isEditing
                ? "Modifiez les informations du compte."
                : "Créez un nouveau compte GMT Rental."}
            </p>
          </div>
          <button
            type="button"
            className="user-modal-close"
            onClick={onClose}
            disabled={saving}
            aria-label="Fermer"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <div className="user-form-grid">
            <div className="admin-field">
              <label htmlFor="user-prenom">Prénom</label>
              <input id="user-prenom" name="prenom" value={form.prenom} onChange={handleChange} autoComplete="given-name" required />
            </div>

            <div className="admin-field">
              <label htmlFor="user-nom">Nom</label>
              <input id="user-nom" name="nom" value={form.nom} onChange={handleChange} autoComplete="family-name" required />
            </div>

            <div className="admin-field full">
              <label htmlFor="user-email">Email</label>
              <input id="user-email" type="email" name="email" value={form.email} onChange={handleChange} autoComplete="email" required />
            </div>

            <div className="admin-field full">
              <label htmlFor="user-password">
                {isEditing ? "Nouveau mot de passe (optionnel)" : "Mot de passe"}
              </label>
              <input
                id="user-password"
                type="password"
                name="motDePasse"
                value={form.motDePasse}
                onChange={handleChange}
                autoComplete="new-password"
                required={!isEditing}
              />
            </div>

            {!isEditing && <div className="admin-field">
              <label htmlFor="user-actif">État</label>
              <select
                id="user-actif"
                name="actif"
                value={form.actif ? "true" : "false"}
                onChange={(event) => {
                  setForm((previous) => ({ ...previous, actif: event.target.value === "true" }));
                  setFormError("");
                  onClearError?.();
                }}
              >
                <option value="true">Actif</option>
                <option value="false">Inactif</option>
              </select>
            </div>}
          </div>

          {visibleError && <p className="admin-error" role="alert">{visibleError}</p>}

          <div className="user-modal-footer">
            <button type="button" className="admin-button" onClick={onClose} disabled={saving}>
              Annuler
            </button>
            <button type="submit" className="admin-button primary" disabled={saving}>
              {saving ? "Enregistrement…" : isEditing ? "Enregistrer les modifications" : "Créer le compte"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
