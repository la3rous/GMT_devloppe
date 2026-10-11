import { useEffect, useState } from "react";
import { ArrowLeft } from "lucide-react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { adminApiService } from "../../services/admin/api";

const empty = { marque: "", modele: "", annee: "", places: "", transmission: "", carburant: "", prixParJour: "", description: "", image: "", statut: "ACTIF", categorieId: "" };

export default function AdminVehicleForm() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [form, setForm] = useState(empty);
  const [categories, setCategories] = useState([]);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    adminApiService.categories().then(({ data }) => setCategories(data || [])).catch(() => setError("Impossible de charger les catégories."));
    if (id) adminApiService.vehicle(id).then(({ data }) => setForm({ ...empty, ...data, categorieId: data.categorie?.id ?? data.categorieId ?? "" })).catch(() => setError("Impossible de charger ce véhicule."));
  }, [id]);

  const update = (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setError("");
    const payload = { ...form, annee: form.annee ? Number(form.annee) : null, places: form.places ? Number(form.places) : null, prixParJour: Number(form.prixParJour), categorieId: form.categorieId ? Number(form.categorieId) : null };
    try { if (id) await adminApiService.updateVehicle(id, payload); else await adminApiService.createVehicle(payload); navigate("/admin/vehicules"); }
    catch (requestError) {
      const responseData = requestError.response?.data;
      setError(responseData?.message || responseData?.detail || responseData?.title || "Enregistrement impossible. Consultez le journal du backend pour le détail.");
    }
    finally { setSaving(false); }
  };

  return <section>
    <div className="admin-page-head"><div><h1>{id ? "Modifier un véhicule" : "Ajouter un véhicule"}</h1><p>Renseignez les informations de la fiche véhicule.</p></div><Link className="admin-button" to="/admin/vehicules"><ArrowLeft size={15} /> Retour</Link></div>
    <form className="admin-card" onSubmit={submit}>
      {error && <p className="admin-error">{error}</p>}
      <div className="admin-form">
        {[["marque","Marque"],["modele","Modèle"],["annee","Année"],["places","Nombre de places"],["prixParJour","Prix par jour (DT)"],["image","URL de l’image"]].map(([name,label]) => <label className="admin-field" key={name}><span>{label}</span><input name={name} type={name === "annee" || name === "places" || name === "prixParJour" ? "number" : "text"} step={name === "prixParJour" ? "0.01" : undefined} required={name === "marque" || name === "modele" || name === "places" || name === "prixParJour"} value={form[name] ?? ""} onChange={update} /></label>)}
        <label className="admin-field"><span>Transmission</span><select name="transmission" value={form.transmission || ""} onChange={update}><option value="">Sélectionner</option><option>MANUELLE</option><option>AUTOMATIQUE</option></select></label>
        <label className="admin-field"><span>Carburant</span><select name="carburant" value={form.carburant || ""} onChange={update}><option value="">Sélectionner</option>{["ESSENCE","DIESEL","HYBRIDE","ELECTRIQUE"].map((x) => <option key={x}>{x}</option>)}</select></label>
        <label className="admin-field"><span>Catégorie</span><select name="categorieId" required value={form.categorieId || ""} onChange={update}><option value="">Sélectionner une catégorie</option>{categories.map((c) => <option key={c.id} value={c.id}>{c.nom}</option>)}</select></label>
        <label className="admin-field"><span>Statut</span><select name="statut" value={form.statut || "ACTIF"} onChange={update}><option>ACTIF</option><option>INACTIF</option></select></label>
        <label className="admin-field full"><span>Description</span><textarea name="description" value={form.description || ""} onChange={update} /></label>
      </div>
      <div className="admin-actions" style={{ justifyContent: "flex-end", marginTop: 20 }}><button className="admin-button primary" disabled={saving}>{saving ? "Enregistrement…" : "Enregistrer"}</button></div>
    </form>
  </section>;
}
