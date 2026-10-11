import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { adminApiService } from "../../services/admin/api";

const emptyForm = { vehicleId: "", dateDebut: "", dateFin: "", prixPaye: "", devise: "DT" };

function formatDate(value) {
  if (!value) return "—";
  return new Date(`${value}T00:00:00`).toLocaleDateString("fr-FR", {
    day: "2-digit", month: "short", year: "numeric",
  });
}

function formatAmount(value) {
  return new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 3 }).format(Number(value) || 0);
}

function getErrorMessage(error, fallback) {
  if (error.response?.status === 401) return "Session administrateur expirée ou invalide. Reconnectez-vous puis réessayez.";
  if (error.response?.status === 403) return "Votre compte n’a pas les droits administrateur pour consulter cet historique.";
  const serverMessage = error.response?.data?.detail || error.response?.data?.message;
  if (error.response?.status === 400) return serverMessage || "Les données de la location sont invalides.";
  if (error.response?.status === 404) return serverMessage || "Utilisateur ou véhicule introuvable.";
  if (error.response?.status >= 500) return serverMessage || "Une erreur serveur est survenue. Réessayez plus tard.";
  return serverMessage || fallback;
}

export default function RentalHistoryModal({ user, onClose }) {
  const navigate = useNavigate();
  const { logout } = useAuth();
  const [rentals, setRentals] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(true);
  const [loadingVehicles, setLoadingVehicles] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [authRequired, setAuthRequired] = useState(false);

  useEffect(() => {
    let active = true;
    adminApiService.userRentals(user.id)
      .then(({ data }) => { if (active) setRentals(data || []); })
      .catch((err) => {
        if (!active) return;
        setAuthRequired(err.response?.status === 401);
        setError(getErrorMessage(err, "Impossible de charger l’historique."));
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [user.id]);

  useEffect(() => {
    let active = true;
    adminApiService.vehicles()
      .then(({ data }) => { if (active) setVehicles(data || []); })
      .catch((err) => {
        if (!active) return;
        setAuthRequired(err.response?.status === 401);
        setError(getErrorMessage(err, "Impossible de charger les véhicules."));
      })
      .finally(() => { if (active) setLoadingVehicles(false); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    const onKeyDown = (event) => { if (event.key === "Escape" && !saving) onClose(); };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [onClose, saving]);

  const totals = useMemo(() => rentals.reduce((sum, rental) => {
    sum[rental.devise] = (sum[rental.devise] || 0) + Number(rental.prixPaye || 0);
    return sum;
  }, {}), [rentals]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setAuthRequired(false);
    if (form.dateFin < form.dateDebut) {
      setError("La date de fin doit être après la date de début.");
      return;
    }
    try {
      setSaving(true);
      const { data } = await adminApiService.createUserRental(user.id, {
        ...form,
        vehicleId: Number(form.vehicleId),
        prixPaye: Number(form.prixPaye),
      });
      setRentals((current) => [data, ...current].sort((a, b) => b.dateDebut.localeCompare(a.dateDebut)));
      setForm(emptyForm);
      try {
        const { data: refreshedRentals } = await adminApiService.userRentals(user.id);
        setRentals(refreshedRentals || []);
      } catch (refreshError) {
        setAuthRequired(refreshError.response?.status === 401);
        setError(`Location enregistrée, mais l’historique n’a pas pu être actualisé. ${getErrorMessage(refreshError, "Réessayez de recharger l’historique.")}`);
      }
    } catch (err) {
      setAuthRequired(err.response?.status === 401);
      setError(getErrorMessage(err, "Impossible d’ajouter cette location."));
    } finally {
      setSaving(false);
    }
  };

  const reconnect = () => {
    logout();
    navigate("/admin/login", { replace: true });
  };

  return (
    <div className="user-modal-overlay rental-history-overlay" onMouseDown={(event) => {
      if (event.target === event.currentTarget && !saving) onClose();
    }}>
      <section className="rental-history-modal" role="dialog" aria-modal="true" aria-labelledby="rental-history-title">
        <header className="rental-history-header">
          <h2 id="rental-history-title">Historique des locations</h2>
          <button type="button" className="user-modal-close" onClick={onClose} disabled={saving} aria-label="Fermer">×</button>
        </header>

        <div className="rental-history-content">
          <div className="rental-history-user">
            <strong>{`${user.prenom || ""} ${user.nom || ""}`.trim() || "Utilisateur"}</strong>
            <span>{user.email}</span>
          </div>

          <div className="rental-history-total">
            <span>Total payé</span>
            <strong>
              {Object.keys(totals).length
                ? Object.entries(totals).map(([currency, amount]) => `${formatAmount(amount)} ${currency}`).join(" · ")
                : "0 DT"}
            </strong>
          </div>

          {error && (
            <div className="admin-error rental-history-error" role="alert">
              <span>{error}</span>
              {authRequired && <button type="button" onClick={reconnect}>Se reconnecter</button>}
            </div>
          )}

          <div className="rental-history-table-wrap">
            <table className="rental-history-table">
              <thead><tr><th>Véhicule</th><th>Période</th><th>Prix payé</th></tr></thead>
              <tbody>
                {loading ? (
                  <tr><td colSpan="3" className="admin-muted">Chargement de l’historique…</td></tr>
                ) : rentals.length ? rentals.map((rental) => (
                  <tr key={rental.id}>
                    <td><span className="rental-car-icon" aria-hidden="true">🚘</span>{rental.vehicle}</td>
                    <td>{formatDate(rental.dateDebut)} → {formatDate(rental.dateFin)}</td>
                    <td>{formatAmount(rental.prixPaye)} {rental.devise}</td>
                  </tr>
                )) : (
                  <tr><td colSpan="3" className="admin-muted">Aucune location enregistrée pour cet utilisateur.</td></tr>
                )}
              </tbody>
            </table>
          </div>

          <form className="rental-history-form" onSubmit={handleSubmit}>
            <h3>Ajouter une location</h3>
            <div className="rental-history-form-row rental-history-form-row-main">
              <label className="admin-field">
                <span>Véhicule</span>
                <select required value={form.vehicleId} onChange={(e) => setForm({ ...form, vehicleId: e.target.value })}>
                  <option value="" disabled>{loadingVehicles ? "Chargement des véhicules…" : "Sélectionner un véhicule"}</option>
                  {vehicles.map((vehicle) => (
                    <option key={vehicle.id} value={vehicle.id}>
                      {[vehicle.marque, vehicle.modele].filter(Boolean).join(" ")}
                    </option>
                  ))}
                </select>
              </label>
              <label className="admin-field">
                <span>Date de début</span>
                <input type="date" required value={form.dateDebut} onChange={(e) => setForm({ ...form, dateDebut: e.target.value })} />
              </label>
              <label className="admin-field">
                <span>Date de fin</span>
                <input type="date" required min={form.dateDebut || undefined} value={form.dateFin} onChange={(e) => setForm({ ...form, dateFin: e.target.value })} />
              </label>
            </div>
            <div className="rental-history-form-row rental-history-form-row-bottom">
              <label className="admin-field">
                <span>Prix payé</span>
                <input type="number" required min="0.001" step="0.001" placeholder="Prix payé" value={form.prixPaye} onChange={(e) => setForm({ ...form, prixPaye: e.target.value })} />
              </label>
              <label className="admin-field">
                <span>Devise</span>
                <select value={form.devise} onChange={(e) => setForm({ ...form, devise: e.target.value })}>
                  <option value="DT">DT</option>
                  <option value="EUR">EUR</option>
                </select>
              </label>
              <button type="submit" className="admin-button primary" disabled={saving || loading || loadingVehicles || !vehicles.length}>
                {saving ? "Ajout…" : "Ajouter"}
              </button>
            </div>
          </form>
        </div>
      </section>
    </div>
  );
}
