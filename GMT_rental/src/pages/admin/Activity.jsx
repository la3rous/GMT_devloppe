import { useCallback, useEffect, useState } from "react";
import { Activity as ActivityIcon, Download, Search } from "lucide-react";
import { adminApiService } from "../../services/admin/api";

const formatDate = (value) => value ? new Intl.DateTimeFormat("fr-FR", { dateStyle: "short", timeStyle: "medium" }).format(new Date(value)) : "—";
const actionLabels = { LOGIN: "Connexion", LOGOUT: "Déconnexion", LOGIN_FAILED: "Connexion refusée", PASSWORD_CHANGE: "Mot de passe changé", CREATE: "Création", UPDATE: "Modification", DELETE: "Suppression" };
const moduleLabels = { security: "Sécurité", vehicules: "Véhicules", categories: "Catégories", demandes: "Devis", users: "Comptes", settings: "Paramètres", account: "Compte", support: "Assistance", notifications: "Notifications" };

export default function AdminActivity() {
  const [filters, setFilters] = useState({ keyword: "", action: "", module: "", from: "", to: "" });
  const [applied, setApplied] = useState(filters);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const params = Object.fromEntries(Object.entries({ ...applied, page, size: 25 }).filter(([, value]) => value !== ""));
      const { data: result } = await adminApiService.activities(params);
      setData(result);
    } catch (requestError) { setError(requestError.response?.data?.message || "Impossible de charger l’historique d’activité."); }
    finally { setLoading(false); }
  }, [applied, page]);
  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const applyFilters = (event) => { event.preventDefault(); setPage(0); setApplied({ ...filters }); };
  const exportCsv = async () => {
    setExporting(true); setError("");
    try {
      const params = Object.fromEntries(Object.entries(applied).filter(([, value]) => value !== ""));
      const { data: blob } = await adminApiService.exportActivities(params);
      const url = URL.createObjectURL(blob); const link = document.createElement("a");
      link.href = url; link.download = "historique-activite.csv"; link.click(); URL.revokeObjectURL(url);
    } catch (requestError) { setError(requestError.response?.data?.message || "Impossible d’exporter l’historique."); }
    finally { setExporting(false); }
  };

  return <section className="account-page">
    <div className="admin-page-head"><div><p className="account-eyebrow">TRAÇABILITÉ</p><h1>Historique d’activité</h1><p>Journal serveur des connexions et opérations administratives.</p></div><button type="button" className="admin-button" onClick={exportCsv} disabled={exporting || !data?.content?.length}><Download size={15} />{exporting ? "Export…" : "Exporter CSV"}</button></div>
    {error && <p className="admin-error" role="alert">{error}</p>}
    <form className="admin-card activity-filter-form" onSubmit={applyFilters}>
      <label className="activity-search"><Search size={16} /><input aria-label="Rechercher dans l’historique" placeholder="Rechercher un mot-clé…" value={filters.keyword} onChange={(event) => setFilters({ ...filters, keyword: event.target.value })} /></label>
      <select aria-label="Filtrer par action" value={filters.action} onChange={(event) => setFilters({ ...filters, action: event.target.value })}><option value="">Toutes les actions</option><option value="LOGIN">Connexion</option><option value="LOGOUT">Déconnexion</option><option value="LOGIN_FAILED">Connexion refusée</option><option value="PASSWORD_CHANGE">Mot de passe changé</option><option value="CREATE">Création</option><option value="UPDATE">Modification</option><option value="DELETE">Suppression</option></select>
      <select aria-label="Filtrer par module" value={filters.module} onChange={(event) => setFilters({ ...filters, module: event.target.value })}><option value="">Tous les modules</option><option value="security">Sécurité</option><option value="vehicules">Véhicules</option><option value="categories">Catégories</option><option value="demandes">Devis</option><option value="users">Comptes</option><option value="settings">Paramètres</option><option value="account">Compte</option><option value="support">Assistance</option><option value="notifications">Notifications</option></select>
      <label className="activity-date"><span>Du</span><input aria-label="Date de début" type="date" value={filters.from} onChange={(event) => setFilters({ ...filters, from: event.target.value })} /></label>
      <label className="activity-date"><span>Au</span><input aria-label="Date de fin" type="date" value={filters.to} onChange={(event) => setFilters({ ...filters, to: event.target.value })} /></label>
      <button className="admin-button primary"><Search size={15} />Filtrer</button>
    </form>
    <div className="admin-card activity-card">
      {loading ? <div className="account-empty">Chargement des événements…</div> : !data?.content?.length ? <div className="account-empty"><ActivityIcon size={24} /><strong>Aucun événement trouvé</strong><span>Les opérations enregistrées apparaîtront ici.</span></div> : <>
        <div className="admin-table-wrap"><table className="admin-table activity-table"><thead><tr><th>Date et heure</th><th>Administrateur</th><th>Utilisateur concerné</th><th>Action</th><th>Module</th><th>Description</th><th>Résultat</th></tr></thead><tbody>
          {data.content.map((entry) => <tr key={entry.id}><td><time dateTime={entry.occurredAt}>{formatDate(entry.occurredAt)}</time></td><td>{entry.actorEmail || "Système"}</td><td>{entry.targetLabel || "—"}</td><td>{actionLabels[entry.action] || entry.action}</td><td>{moduleLabels[entry.module] || entry.module}</td><td>{entry.description}</td><td><span className={`admin-badge ${entry.result === "SUCCESS" ? "admin-badge-success" : "admin-badge-danger"}`}>{entry.result === "SUCCESS" ? "Réussite" : "Échec"}</span></td></tr>)}
        </tbody></table></div>
        <div className="activity-pagination"><span>{data.totalElements} événement{data.totalElements > 1 ? "s" : ""}</span><div><button type="button" className="admin-button" onClick={() => setPage((value) => Math.max(0, value - 1))} disabled={page <= 0}>Précédent</button><span>Page {data.number + 1} / {Math.max(1, data.totalPages)}</span><button type="button" className="admin-button" onClick={() => setPage((value) => value + 1)} disabled={page + 1 >= data.totalPages}>Suivant</button></div></div>
      </>}
    </div>
  </section>;
}
