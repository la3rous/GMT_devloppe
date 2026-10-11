import { useCallback, useEffect, useMemo, useState } from "react";
import { CarFront, CircleDollarSign, ClipboardList, Tags, Users, Gauge, ChartNoAxesCombined, FileText } from "lucide-react";
import { Link } from "react-router-dom";
import { adminApiService } from "../../services/admin/api";
import { requestStatusBadgeClass, requestStatusLabel } from "../../utils/adminStatus";
import "./Dashboard.css";

const today = () => new Date().toISOString().slice(0, 10);
const yearStart = () => `${today().slice(0, 4)}-01-01`;
const number = (value) => Number(value || 0).toLocaleString("fr-TN", { maximumFractionDigits: 1 });
const money = (value, currency) => `${number(value)} ${currency}`;
const monthKey = (year, month) => `${year}-${String(month).padStart(2, "0")}`;
const monthLabel = (key) => new Intl.DateTimeFormat("fr-TN", { month: "short", year: "2-digit" }).format(new Date(`${key}-01T12:00:00`));
const statuses = ["NOUVELLE", "EN_COURS", "TRAITEE", "ACCEPTEE", "REFUSEE", "ANNULEE"];
const statusColors = { NOUVELLE: "var(--warning)", EN_COURS: "var(--info)", TRAITEE: "var(--muted)", ACCEPTEE: "var(--success)", REFUSEE: "var(--danger)", ANNULEE: "var(--danger)", OTHER: "var(--muted)" };

function MonthBars({ rows, valueKey, label, currency }) {
  const max = Math.max(1, ...rows.map((row) => row[valueKey]));
  return <div className="dashboard-chart" role="img" aria-label={label}>
    {rows.length ? rows.map((row) => <div className="dashboard-chart-column" key={row.key} title={`${monthLabel(row.key)} : ${number(row[valueKey])}${currency ? ` ${currency}` : " demandes"}`}>
      <div className="dashboard-chart-track"><span className="dashboard-chart-bar" style={{ height: `${Math.max(row[valueKey] ? 5 : 0, row[valueKey] / max * 100)}%` }} /></div>
      <small>{monthLabel(row.key)}</small>
    </div>) : <p className="admin-muted">Aucune donnée sur cette période.</p>}
  </div>;
}

function Ranking({ rows, value, formatValue, empty }) {
  if (!rows.length) return <p className="admin-muted dashboard-empty">{empty}</p>;
  const max = Math.max(1, ...rows.map((row) => Number(row[value] || 0)));
  return <ol className="dashboard-ranking">{rows.map((row, index) => <li key={`${row.vehicleId ?? row.userId ?? row.currency}-${row.name ?? row.vehicleName}-${index}`}>
    <span className="dashboard-rank-number">{index + 1}</span><span className="dashboard-rank-main"><strong>{row.vehicleName || row.name || "Véhicule sans nom"}</strong><span className="dashboard-rank-meter"><i style={{ width: `${Number(row[value] || 0) / max * 100}%` }} /></span></span>
    <b>{formatValue(row[value], row.currency)}</b>
  </li>)}</ol>;
}

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [analytics, setAnalytics] = useState(null);
  const [requests, setRequests] = useState([]);
  const [from, setFrom] = useState(yearStart);
  const [to, setTo] = useState(today);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    if (!from || !to || from > to) { setError("Choisissez une période valide."); return; }
    setLoading(true);
    setError("");
    try {
      const [summary, analyticalData, list] = await Promise.all([
        adminApiService.dashboard(),
        adminApiService.dashboardAnalytics({ from, to }),
        adminApiService.quoteRequests(),
      ]);
      setStats(summary.data || {});
      setAnalytics(analyticalData.data || null);
      setRequests((list.data || []).slice(0, 5));
    } catch (requestError) {
      setError(requestError.response?.data?.detail || "Les données du tableau de bord ne sont pas accessibles pour le moment.");
    } finally { setLoading(false); }
  }, [from, to]);

  useEffect(() => {
    const timer = window.setTimeout(load, 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  const requestMonths = useMemo(() => {
    const rows = new Map();
    (analytics?.requestsByMonthAndStatus || []).forEach((item) => {
      const key = monthKey(item.year, item.month);
      const row = rows.get(key) || { key, total: 0, statuses: {} };
      row.total += item.count;
      row.statuses[item.status] = item.count;
      rows.set(key, row);
    });
    return [...rows.values()];
  }, [analytics]);

  const revenueByCurrency = useMemo(() => {
    const result = new Map();
    (analytics?.revenueByVehicleMonth || []).forEach((item) => {
      const currency = item.currency || "INCONNUE";
      const key = monthKey(item.year, item.month);
      const rows = result.get(currency) || new Map();
      rows.set(key, (rows.get(key) || 0) + Number(item.amount || 0));
      result.set(currency, rows);
    });
    return [...result.entries()].map(([currency, months]) => ({ currency, rows: [...months.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([key, amount]) => ({ key, amount })) }));
  }, [analytics]);

  const cards = [
    [CarFront, "Véhicules", stats?.totalVehicules ?? stats?.totalVehicles],
    [Tags, "Catégories", stats?.totalCategories],
    [ClipboardList, "Demandes en attente", stats?.demandesEnAttente ?? stats?.pendingRequests],
    [FileText, "Demandes reçues", stats?.totalDemandes ?? stats?.totalRequests],
    [ChartNoAxesCombined, "Locations enregistrées", analytics?.rentalCount],
    [Gauge, "Occupation estimée", `${number(analytics?.estimatedOccupancyPercent)} %`],
    [Users, "Demandes annulées", analytics?.canceledQuoteCount],
  ];

  return <section className="admin-dashboard">
    <div className="admin-page-head dashboard-heading"><div><h1>Tableau de bord</h1><p>Indicateurs calculés à partir des locations et demandes enregistrées.</p></div>
      <form className="dashboard-filters" onSubmit={(event) => { event.preventDefault(); load(); }}><label>Du<input type="date" value={from} max={to} onChange={(event) => setFrom(event.target.value)} /></label><label>Au<input type="date" value={to} min={from} max={today()} onChange={(event) => setTo(event.target.value)} /></label><button className="admin-button" type="submit" disabled={loading}>Actualiser</button></form>
    </div>
    {error && <p className="admin-error" role="alert">{error}</p>}
    {analytics?.demoData && <p className="dashboard-demo-notice" role="status">Mode démonstration : données fictives locales H2, sans valeur métier ni paiement réel.</p>}
    {loading && <p className="admin-muted" role="status">Actualisation des statistiques…</p>}
    <div className="admin-grid dashboard-stat-grid">{cards.map(([Icon, label, value]) => <article className="admin-card admin-stat" key={label}><span className="admin-stat-icon"><Icon size={20} /></span><div><span>{label}</span><strong>{value == null ? "—" : typeof value === "number" ? number(value) : value}</strong></div></article>)}</div>

    <div className="dashboard-grid">
      <article className="admin-card"><h2>Évolution des demandes de devis</h2><p className="admin-muted">Volume par date de création, réparti selon le statut actuel.</p>
        <div className="dashboard-chart" role="img" aria-label="Demandes de devis mensuelles par statut">{requestMonths.length ? requestMonths.map((row) => { const total = Math.max(1, ...requestMonths.map((month) => month.total)); return <div className="dashboard-chart-column" key={row.key} title={`${monthLabel(row.key)} : ${row.total} demandes`}><div className="dashboard-chart-track dashboard-stack">{statuses.map((status) => row.statuses[status] ? <i key={status} style={{ height: `${row.statuses[status] / total * 100}%`, background: statusColors[status] }} /> : null)}</div><small>{monthLabel(row.key)}</small></div>; }) : <p className="admin-muted">Aucune demande sur cette période.</p>}</div>
        <div className="dashboard-legend">{statuses.map((status) => <span key={status}><i style={{ background: statusColors[status] }} />{requestStatusLabel(status)}</span>)}</div>
      </article>
      <article className="admin-card"><h2>Chiffre historique enregistré</h2><p className="admin-muted">Somme de rental_history.prix_paye, groupée par mois de début et devise.</p>
        {revenueByCurrency.length ? revenueByCurrency.map(({ currency, rows }) => <div className="dashboard-currency-chart" key={currency}><strong>{currency}</strong><MonthBars rows={rows} valueKey="amount" currency={currency} label={`Montants historiques en ${currency}`} /></div>) : <p className="admin-muted">Aucun montant de location sur cette période.</p>}
        {(analytics?.revenueByVehicleMonth || []).length > 0 && <div className="dashboard-table-scroll"><table className="admin-table"><thead><tr><th>Véhicule</th><th>Mois de début</th><th>Montant enregistré</th></tr></thead><tbody>{analytics.revenueByVehicleMonth.map((row, index) => <tr key={`${row.vehicleId}-${row.year}-${row.month}-${row.currency}-${index}`}><td>{row.vehicleName || "Véhicule historique"}</td><td>{monthLabel(monthKey(row.year, row.month))}</td><td>{money(row.amount, row.currency)}</td></tr>)}</tbody></table></div>}
      </article>
      <article className="admin-card"><h2>Véhicules les plus loués</h2><p className="admin-muted">Locations enregistrées dans rental_history.</p><Ranking rows={analytics?.topVehicles || []} value="count" formatValue={(value) => `${number(value)} loc.`} empty="Aucune location enregistrée." /></article>
      <article className="admin-card"><h2>Clients les plus actifs</h2><p className="admin-muted">Classement par nombre de locations enregistrées.</p><Ranking rows={analytics?.clientsByRentals || []} value="count" formatValue={(value) => `${number(value)} loc.`} empty="Aucune location enregistrée." /></article>
      <article className="admin-card"><h2>Montants historiques par client</h2><p className="admin-muted">Chaque devise est classée séparément ; aucune conversion n’est appliquée.</p>
        {(analytics?.clientsBySpend || []).length ? <ol className="dashboard-ranking">{analytics.clientsBySpend.map((row, index) => <li key={`${row.userId}-${row.currency}`}><span className="dashboard-rank-number">{index + 1}</span><span className="dashboard-rank-main"><strong>{row.name}</strong><span className="admin-muted">{row.currency}</span></span><b>{money(row.amount, row.currency)}</b></li>)}</ol> : <p className="admin-muted">Aucun montant enregistré.</p>}
      </article>
      <article className="admin-card"><h2>État courant du parc</h2><p className="admin-muted">Répartition selon les valeurs présentes dans vehicles.statut.</p>
        {Object.entries(analytics?.vehicleStatuses || {}).length ? <ul className="dashboard-status-list">{Object.entries(analytics.vehicleStatuses).map(([status, count]) => <li key={status}><span>{status}</span><b>{number(count)}</b></li>)}</ul> : <p className="admin-muted">Aucun statut disponible.</p>}
        <p className="dashboard-note">L’état « disponible », « loué » ou « maintenance » n’est pas déduit des demandes : les libellés affichés reflètent les statuts réellement enregistrés.</p>
      </article>
      <article className="admin-card"><h2>Occupation estimée par véhicule</h2><p className="admin-muted">Jours couverts par l’historique / jours de la période.</p>
        {(analytics?.occupancyByVehicle || []).length ? <ol className="dashboard-ranking">{analytics.occupancyByVehicle.slice(0, 10).map((row, index) => <li key={row.vehicleId}><span className="dashboard-rank-number">{index + 1}</span><span className="dashboard-rank-main"><strong>{row.vehicleName || `Véhicule #${row.vehicleId}`}</strong><span className="dashboard-rank-meter"><i style={{ width: `${Math.min(100, row.estimatedPercent)}%` }} /></span></span><b>{number(row.estimatedPercent)} %</b></li>)}</ol> : <p className="admin-muted">Aucune période de location associée à un véhicule.</p>}
      </article>
    </div>
    <p className="dashboard-caveat"><CircleDollarSign size={16} />{analytics?.limitations || "Ces valeurs sont des estimations fondées sur les données historiques disponibles. Elles ne représentent pas des paiements vérifiés."}</p>

    <section className="admin-card dashboard-recent"><div className="admin-page-head"><div><h2>Demandes récentes</h2><p>Demandes de devis — elles ne sont pas des réservations confirmées.</p></div><Link className="admin-button" to="/admin/devis">Toutes les demandes</Link></div>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Client</th><th>Véhicule</th><th>Période demandée</th><th>Montant du devis</th><th>Statut actuel</th></tr></thead><tbody>
        {requests.map((item) => { const status = item.statut || "NOUVELLE"; return <tr key={item.id}><td>{item.prenom} {item.nom}<br /><span className="admin-muted">{item.email}</span></td><td>{item.vehicule?.marque} {item.vehicule?.modele}</td><td>{item.dateDebut} – {item.dateFin}</td><td>{item.prixTotal ?? "—"} DT</td><td><span className={requestStatusBadgeClass(status)}>{requestStatusLabel(status)}</span></td></tr>; })}
        {!requests.length && <tr><td colSpan="5" className="admin-muted">Aucune demande à afficher.</td></tr>}
      </tbody></table></div>
    </section>
  </section>;
}
