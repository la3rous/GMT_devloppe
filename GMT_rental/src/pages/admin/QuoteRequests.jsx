import { useCallback, useEffect, useState } from "react";
import { ArrowLeft, Eye } from "lucide-react";
import { Link, useParams } from "react-router-dom";
import { adminApiService } from "../../services/admin/api";
import { requestStatusBadgeClass, requestStatusLabel } from "../../utils/adminStatus";

export default function AdminQuoteRequests() {
  const { id } = useParams();
  const [items, setItems] = useState([]);
  const [selection, setSelection] = useState(null);
  const selected = selection?.id === id ? selection.data : null;
  const [error, setError] = useState("");
  const load = useCallback(() => adminApiService.quoteRequests().then(({ data }) => setItems(data || [])).catch(() => setError("Impossible de charger les demandes de devis.")), []);
  useEffect(() => { load(); }, [load]);
  useEffect(() => {
    if (!id) return;
    let active = true;
    adminApiService.quoteRequest(id)
      .then(({ data }) => { if (active) setSelection({ id, data }); })
      .catch(() => { if (active) setError("Demande introuvable."); });
    return () => { active = false; };
  }, [id]);

  const changeStatus = async (status) => {
    setError("");
    try { await adminApiService.updateQuoteRequestStatus(id, status); const { data } = await adminApiService.quoteRequest(id); setSelection({ id, data }); await load(); }
    catch (requestError) { setError(requestError.response?.data?.message || requestError.response?.data?.detail || requestError.response?.data?.title || "La mise à jour du statut a échoué."); }
  };

  if (id) return <section>
    <div className="admin-page-head"><div><h1>Détail de la demande</h1><p>Demande #{id}</p></div><Link className="admin-button" to="/admin/devis"><ArrowLeft size={15} /> Retour aux demandes</Link></div>
    {error && <p className="admin-error">{error}</p>}
    {selected && <><div className="admin-grid"><article className="admin-card"><h2>Client</h2><p>{selected.prenom} {selected.nom}</p><p className="admin-muted">{selected.email}</p><p className="admin-muted">{selected.telephone}</p></article><article className="admin-card"><h2>Véhicule et période</h2><p>{selected.vehicule?.marque} {selected.vehicule?.modele}</p><p className="admin-muted">{selected.dateDebut} – {selected.dateFin}</p><p>{selected.prixTotal ?? "—"} DT</p></article><article className="admin-card"><h2>Prise en charge</h2><p>{selected.lieuPriseEnCharge || "—"}</p><p className="admin-muted">Restitution : {selected.lieuRestitution || "—"}</p></article></div><article className="admin-card"><h2>Message</h2><p>{selected.message || "Aucun message"}</p><p>Statut : <span className={requestStatusBadgeClass(selected.statut)}>{requestStatusLabel(selected.statut)}</span></p><div className="admin-actions"><button className="admin-button primary" onClick={() => changeStatus("ACCEPTEE")}>Accepter et bloquer les dates</button><button className="admin-button danger" onClick={() => changeStatus("REFUSEE")}>Refuser</button><button className="admin-button danger" onClick={() => changeStatus("ANNULEE")}>Annuler</button><button className="admin-button" onClick={() => changeStatus("NOUVELLE")}>Remettre en attente</button></div></article></>}
  </section>;

  return <section>
    <div className="admin-page-head"><div><h1>Demandes de devis</h1><p>Consultez les demandes envoyées depuis le Front Office.</p></div></div>
    {error && <p className="admin-error">{error}</p>}
    <div className="admin-card admin-table-wrap"><table className="admin-table"><thead><tr><th>Client</th><th>Véhicule</th><th>Période</th><th>Montant estimé</th><th>Statut</th><th></th></tr></thead><tbody>{items.map((item) => { const status = item.statut || "NOUVELLE"; return <tr key={item.id}><td>{item.prenom} {item.nom}<br /><span className="admin-muted">{item.email}</span></td><td>{item.vehicule?.marque} {item.vehicule?.modele}</td><td>{item.dateDebut} – {item.dateFin}</td><td>{item.prixTotal ?? "—"} DT</td><td><span className={requestStatusBadgeClass(status)}>{requestStatusLabel(status)}</span></td><td><Link className="admin-button" to={`/admin/devis/${item.id}`}><Eye size={15} /> Détail</Link></td></tr>; })}{!items.length && <tr><td colSpan="6" className="admin-muted">Aucune demande reçue.</td></tr>}</tbody></table></div>
  </section>;
}
