import { useCallback, useEffect, useMemo, useState } from "react";
import { adminApiService } from "../../services/admin/api";
import UserModal from "./UserModal";
import RentalHistoryModal from "./RentalHistoryModal";

function getRequestErrorMessage(error, fallback) {
  const message = error.response?.data?.message || error.response?.data?.detail || error.response?.data?.title;

  switch (error.response?.status) {
    case 400:
      return message || "Vérifiez les informations saisies.";
    case 401:
      return "La session administrateur a expiré. Reconnectez-vous.";
    case 403:
      return "Votre compte n'a pas les droits nécessaires pour cette action.";
    case 404:
      return message || "Cet utilisateur n'existe plus.";
    case 409:
      return message || "Cette adresse e-mail est déjà utilisée.";
    case 500:
      return "Une erreur serveur est survenue. Réessayez plus tard.";
    default:
      return message || fallback;
  }
}

export default function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [resendingUserId, setResendingUserId] = useState(null);

  // Modal
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [historyUser, setHistoryUser] = useState(null);
  const [modalRole, setModalRole] = useState("USER");
  const [saving, setSaving] = useState(false);
  const [updatingStatusUserId, setUpdatingStatusUserId] = useState(null);

  const loadUsers = useCallback(async () => {
    try {
      const { data } = await adminApiService.users();

      setUsers(data || []);
      setError("");
    } catch (err) {
      console.error(err);
      setError(getRequestErrorMessage(err, "Impossible de charger les utilisateurs."));
    }
  }, []);

  useEffect(() => {
    let active = true;
    const fetchUsers = async () => {
      try {
        const { data } = await adminApiService.users();
        if (active) {
          setUsers(data || []);
          setError("");
        }
      } catch (err) {
        console.error(err);
        if (active) setError(getRequestErrorMessage(err, "Impossible de charger les utilisateurs."));
      }
    };
    void fetchUsers();
    const refreshWhenVisible = () => {
      if (document.visibilityState === "visible") void loadUsers();
    };
    document.addEventListener("visibilitychange", refreshWhenVisible);

    return () => {
      active = false;
      document.removeEventListener("visibilitychange", refreshWhenVisible);
    };
  }, [loadUsers]);

  // =========================
  // OUVRIR MODAL AJOUT
  // =========================

  const openCreateModal = (role) => {
    setError("");
    setSuccessMessage("");
    setEditingUser(null);
    setModalRole(role);
    setModalOpen(true);
  };

  // =========================
  // OUVRIR MODAL MODIFICATION
  // =========================

  const openEditModal = (user) => {
    setError("");
    setSuccessMessage("");
    setEditingUser(user);
    setModalRole(user.role || "USER");
    setModalOpen(true);
  };

  const openHistory = (user) => setHistoryUser(user);

  // =========================
  // FERMER MODAL
  // =========================

  const closeModal = () => {
    if (saving) return;

    setModalOpen(false);
    setEditingUser(null);
  };

  // =========================
  // ENREGISTRER
  // =========================

  const handleSaveUser = async (data) => {
    try {
      setSaving(true);
      setError("");
      setSuccessMessage("");

      if (editingUser) {
        // MODIFICATION
        await adminApiService.updateUser(
          editingUser.id,
          data
        );
        setSuccessMessage("Compte utilisateur mis à jour avec succès.");
      } else {
        // AJOUT
        await adminApiService.createUser(data);
        setSuccessMessage(`Compte créé avec succès. Un code de vérification OTP à 6 chiffres a été envoyé à ${data.email}.`);
      }

      // Fermer le modal
      setModalOpen(false);
      setEditingUser(null);

      // Recharger les utilisateurs
      await loadUsers();

    } catch (err) {
      console.error(err);
      setError(getRequestErrorMessage(err, "Impossible d'enregistrer cet utilisateur."));
    } finally {
      setSaving(false);
    }
  };

  const handleResendVerification = async (user) => {
    try {
      setResendingUserId(user.id);
      setError("");
      setSuccessMessage("");
      await adminApiService.resendUserVerification(user.id);
      setSuccessMessage(`Un code de vérification OTP a été renvoyé à ${user.email}.`);
    } catch (err) {
      console.error(err);
      setError(getRequestErrorMessage(err, "Impossible de renvoyer le code de vérification."));
    } finally {
      setResendingUserId(null);
    }
  };

  const handleSetUserActive = async (user, active) => {
    const displayName = `${user.prenom || ""} ${user.nom || ""}`.trim() || user.email;
    if (!active && !window.confirm(`Désactiver le compte de ${displayName} ? Ses devis, locations et préférences seront conservés.`)) return;

    try {
      setUpdatingStatusUserId(user.id);
      setError("");
      setSuccessMessage("");
      if (active) await adminApiService.setUserActive(user.id, true);
      else await adminApiService.deactivateUser(user.id);
      setUsers((currentUsers) => currentUsers.map((currentUser) =>
        currentUser.id === user.id ? { ...currentUser, actif: active } : currentUser
      ));
      setSuccessMessage(active
        ? `Le compte de ${displayName} a été réactivé.`
        : `Le compte de ${displayName} a été désactivé. Son historique est conservé.`);
    } catch (err) {
      console.error(err);
      setError(getRequestErrorMessage(err, "Impossible de modifier l’état de cet utilisateur."));
    } finally {
      setUpdatingStatusUserId(null);
    }
  };
  // =========================
  // RECHERCHE
  // =========================

  const filteredUsers = useMemo(() => {
    const value = search.trim().toLowerCase();

    if (!value) {
      return users;
    }

    return users.filter((user) => {
      const fullName =
        `${user.prenom || ""} ${user.nom || ""}`.toLowerCase();

      return (
        fullName.includes(value) ||
        (user.nom || "").toLowerCase().includes(value) ||
        (user.prenom || "").toLowerCase().includes(value) ||
        (user.email || "").toLowerCase().includes(value)
      );
    });
  }, [users, search]);

  // =========================
  // SÉPARATION ADMIN / USERS
  // =========================

  const administrators = filteredUsers.filter(
    (user) => user.role?.toUpperCase() === "ADMIN"
  );

  const regularUsers = filteredUsers.filter(
    (user) => user.role?.toUpperCase() !== "ADMIN"
  );

  return (
    <section className="admin-users-page">

      {/* =========================
          HEADER
          ========================= */}

      <div className="admin-page-head">
        <div>
          <h1>Utilisateurs</h1>

          <p>
            {users.length} compte
            {users.length !== 1 ? "s" : ""} enregistré
            {users.length !== 1 ? "s" : ""}.
          </p>
        </div>
      </div>

      {/* =========================
          ERROR
          ========================= */}

      {error && (
        <p className="admin-error">
          {error}
        </p>
      )}

      {successMessage && (
        <p className="admin-success">
          {successMessage}
        </p>
      )}

      {/* =========================
          SEARCH
          ========================= */}

      <div className="admin-users-search">
        <span>⌕</span>

        <input
          type="text"
          placeholder="Rechercher par nom ou email..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {/* =========================
          ADMINISTRATEURS
          ========================= */}

      <div className="admin-users-section">

        <div className="admin-users-section-head">

          <div>
            <h2>Administrateurs</h2>

            <span>
              {administrators.length}
            </span>
          </div>

          <button
            type="button"
            className="admin-primary-button"
            onClick={() => openCreateModal("ADMIN")}
          >
            + Ajouter un administrateur
          </button>

        </div>

        <div className="admin-card admin-table-wrap">

          <table className="admin-table">

            <thead>
              <tr>
                <th>Nom complet</th>
                <th>E-mail</th>
                <th>Rôle</th>
                <th>État</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>

              {administrators.map((user) => (

                <tr key={user.id}>

                  <td>
                    <strong>
                      {user.prenom} {user.nom}
                    </strong>
                  </td>

                  <td>
                    {user.email}
                  </td>

                  <td>
                    {user.role}
                  </td>

                  <td>
                    <div style={{ display: "flex", flexDirection: "column", gap: 4, alignItems: "flex-start" }}>
                      <span
                        className={
                          user.actif === false
                            ? "user-status inactive"
                            : "user-status active"
                        }
                      >
                        {user.actif === false ? "Inactif" : "Actif"}
                      </span>
                      <span
                        className={
                          user.emailVerified
                            ? "user-status active"
                            : "user-status warning"
                        }
                      >
                        {user.emailVerified ? "Email vérifié" : "Non vérifié"}
                      </span>
                    </div>
                  </td>

                  <td>
                    <div className="user-actions">
                      <button type="button" onClick={() => openHistory(user)}>
                        Historique
                      </button>

                      <button
                        type="button"
                        onClick={() => openEditModal(user)}
                      >
                        Modifier
                      </button>

                      {!user.emailVerified && (
                        <button
                          type="button"
                          onClick={() => handleResendVerification(user)}
                          disabled={resendingUserId === user.id}
                          title="Renvoyer le code de vérification"
                        >
                          {resendingUserId === user.id ? "Envoi…" : "Renvoyer code"}
                        </button>
                      )}

                      <button
                        type="button"
                        className={user.actif === false ? "" : "user-delete-button"}
                        onClick={() => handleSetUserActive(user, user.actif === false)}
                        disabled={updatingStatusUserId === user.id}
                      >
                        {updatingStatusUserId === user.id
                          ? user.actif === false ? "Réactivation…" : "Désactivation…"
                          : user.actif === false ? "Réactiver" : "Désactiver"}
                      </button>
                    </div>
                  </td>

                </tr>

              ))}

              {administrators.length === 0 && (

                <tr>
                  <td
                    colSpan="5"
                    className="admin-muted"
                  >
                    Aucun administrateur.
                  </td>
                </tr>

              )}

            </tbody>

          </table>

        </div>

      </div>

      {/* =========================
          UTILISATEURS
          ========================= */}

      <div className="admin-users-section">

        <div className="admin-users-section-head">

          <div>
            <h2>Utilisateurs</h2>

            <span>
              {regularUsers.length}
            </span>
          </div>

        </div>

        <p className="admin-muted">
          Les comptes utilisateurs sont créés automatiquement lors d’une demande de devis.
        </p>

        <div className="admin-card admin-table-wrap">

          <table className="admin-table">

            <thead>
              <tr>
                <th>Nom complet</th>
                <th>E-mail</th>
                <th>Rôle</th>
                <th>État</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>

              {regularUsers.map((user) => (

                <tr key={user.id}>

                  <td>
                    <strong>
                      {user.prenom} {user.nom}
                    </strong>
                  </td>

                  <td>
                    {user.email}
                  </td>

                  <td>
                    {user.role}
                  </td>

                  <td>
                    <div style={{ display: "flex", flexDirection: "column", gap: 4, alignItems: "flex-start" }}>
                      <span
                        className={
                          user.actif === false
                            ? "user-status inactive"
                            : "user-status active"
                        }
                      >
                        {user.actif === false ? "Inactif" : "Actif"}
                      </span>
                      <span
                        className={
                          user.emailVerified
                            ? "user-status active"
                            : "user-status warning"
                        }
                      >
                        {user.emailVerified ? "Email vérifié" : "Non vérifié"}
                      </span>
                    </div>
                  </td>

                  <td>
                    <div className="user-actions">
                      <button type="button" onClick={() => openHistory(user)}>
                        Historique
                      </button>

                      <button
                        type="button"
                        onClick={() => openEditModal(user)}
                      >
                        Modifier
                      </button>

                      {!user.emailVerified && (
                        <button
                          type="button"
                          onClick={() => handleResendVerification(user)}
                          disabled={resendingUserId === user.id}
                          title="Renvoyer le code de vérification"
                        >
                          {resendingUserId === user.id ? "Envoi…" : "Renvoyer code"}
                        </button>
                      )}

                      <button
                        type="button"
                        className={user.actif === false ? "" : "user-delete-button"}
                        onClick={() => handleSetUserActive(user, user.actif === false)}
                        disabled={updatingStatusUserId === user.id}
                      >
                        {updatingStatusUserId === user.id
                          ? user.actif === false ? "Réactivation…" : "Désactivation…"
                          : user.actif === false ? "Réactiver" : "Désactiver"}
                      </button>
                    </div>
                  </td>

                </tr>

              ))}

              {regularUsers.length === 0 && (

                <tr>
                  <td
                    colSpan="5"
                    className="admin-muted"
                  >
                    Aucun utilisateur.
                  </td>
                </tr>

              )}

            </tbody>

          </table>

        </div>

      </div>

      {/* =========================
          MODAL
          ========================= */}

      {modalOpen && (

        <UserModal
          user={editingUser}
          role={modalRole}
          saving={saving}
          error={error}
          onClearError={() => setError("")}
          onClose={closeModal}
          onSave={handleSaveUser}
        />

      )}

      {historyUser && (
        <RentalHistoryModal
          user={historyUser}
          onClose={() => setHistoryUser(null)}
        />
      )}

    </section>
  );
}
