import { useCallback, useEffect, useState } from "react";
import { CarFront, Pencil, Plus, Search, ToggleLeft, ToggleRight } from "lucide-react";
import { Link } from "react-router-dom";
import { adminApiService } from "../../services/admin/api";
import { resolveVehicleImage } from "../../utils/vehicleImage";

export default function AdminVehicles() {
  const [vehicles, setVehicles] = useState([]);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const load = useCallback(() => adminApiService.vehicles().then(({ data }) => setVehicles(data || [])).catch(() => setError("Impossible de charger les véhicules.")), []);
  useEffect(() => { load(); }, [load]);

  const toggleStatus = async (vehicle) => {
    const next = vehicle.statut === "ACTIF" ? "INACTIF" : "ACTIF";
    try {
      const { data: updatedVehicle } = await adminApiService.updateVehicleStatus(vehicle.id, next);
      setVehicles((previous) => previous.map((item) => item.id === vehicle.id ? { ...item, ...updatedVehicle } : item));
      await load();
    }
    catch { setError("La mise à jour du statut a échoué."); }
  };
  const filtered = vehicles.filter((v) => `${v.marque} ${v.modele}`.toLowerCase().includes(search.toLowerCase()));

  return <section>
    <div className="admin-page-head"><div><h1>Véhicules</h1><p>Consultez et gérez les véhicules de la flotte.</p></div><Link className="admin-button primary" to="/admin/vehicules/ajouter"><Plus size={16} /> Ajouter</Link></div>
    {error && <p className="admin-error">{error}</p>}
    <div className="admin-card"><div className="admin-toolbar"><Search size={17} /><input aria-label="Rechercher un véhicule" placeholder="Rechercher par marque ou modèle" value={search} onChange={(e) => setSearch(e.target.value)} /></div>
      <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Véhicule</th><th>Catégorie</th><th>Prix / jour</th><th>Places</th><th>Statut</th><th>Actions</th></tr></thead><tbody>
        {filtered.map((v) => { const image = resolveVehicleImage(v.image); const isActive = (v.statut || "ACTIF") === "ACTIF"; return <tr key={v.id}><td><div className="admin-vehicle-cell">{image ? <img className="admin-vehicle-thumb" src={image} alt={`${v.marque} ${v.modele}`} /> : <span className="admin-vehicle-thumb admin-vehicle-placeholder"><CarFront size={18} /></span>}<span>{v.marque} {v.modele} {v.annee ? `(${v.annee})` : ""}</span></div></td><td>{v.categorie?.nom || "—"}</td><td>{v.prixParJour ?? "—"} DT</td><td>{v.places ?? "—"}</td><td><span className={`admin-badge ${isActive ? "admin-badge-success" : "admin-badge-danger"}`}>{v.statut || "ACTIF"}</span></td><td><div className="admin-actions"><Link className="admin-button" aria-label="Modifier" to={`/admin/vehicules/modifier/${v.id}`}><Pencil size={15} /></Link><button className="admin-button" onClick={() => toggleStatus(v)} aria-label={isActive ? "Désactiver" : "Activer"}>{isActive ? <ToggleRight size={17} /> : <ToggleLeft size={17} />}</button></div></td></tr>; })}
        {!filtered.length && <tr><td colSpan="6" className="admin-muted">Aucun véhicule trouvé.</td></tr>}
      </tbody></table></div>
    </div>
  </section>;
}
