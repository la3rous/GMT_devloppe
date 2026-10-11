import { useCallback, useEffect, useState } from "react";
import { Pencil, Plus, Trash2, X } from "lucide-react";
import { adminApiService } from "../../services/admin/api";

const blank = { nom: "", description: "" };

export default function AdminCategories() {
  const [items, setItems] = useState([]);
  const [form, setForm] = useState(blank);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const load = useCallback(() => adminApiService.categories().then(({ data }) => setItems([...(data || [])].sort((a, b) => Number(b.id) - Number(a.id)))).catch(() => setError("Impossible de charger les catégories.")), []);
  useEffect(() => { load(); }, [load]);

  const submit = async (event) => {
    event.preventDefault(); setSaving(true); setError("");
    try { if (editingId) await adminApiService.updateCategory(editingId, form); else await adminApiService.createCategory(form); setForm(blank); setEditingId(null); await load(); }
    catch (requestError) { setError(requestError.response?.data?.message || "Enregistrement impossible."); }
    finally { setSaving(false); }
  };
  const edit = (item) => { setEditingId(item.id); setForm({ nom: item.nom || "", description: item.description || "" }); };
  const remove = async (item) => {
    if (!window.confirm(`Supprimer la catégorie « ${item.nom} » ?`)) return;
    try { await adminApiService.deleteCategory(item.id); await load(); }
    catch (requestError) { setError(requestError.response?.data?.message || "Suppression impossible. Vérifiez que la catégorie n’est pas utilisée."); }
  };

  return <section>
    <div className="admin-page-head"><div><h1>Catégories</h1><p>Organisez les véhicules de la flotte.</p></div></div>
    {error && <p className="admin-error">{error}</p>}
    <form className="admin-card" onSubmit={submit} style={{ marginBottom: 18 }}><h2>{editingId ? "Modifier la catégorie" : "Nouvelle catégorie"}</h2>
      <div className="admin-form"><label className="admin-field"><span>Nom</span><input required value={form.nom} onChange={(e) => setForm({ ...form, nom: e.target.value })} /></label><label className="admin-field"><span>Description</span><input value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label></div>
      <div className="admin-actions" style={{ justifyContent: "flex-end", marginTop: 16 }}>{editingId && <button type="button" className="admin-button" onClick={() => { setEditingId(null); setForm(blank); }}><X size={15} /> Annuler</button>}<button className="admin-button primary" disabled={saving}><Plus size={15} />{saving ? "Enregistrement…" : "Enregistrer"}</button></div>
    </form>
    <div className="admin-card admin-table-wrap"><table className="admin-table"><thead><tr><th>Nom</th><th>Description</th><th>Actions</th></tr></thead><tbody>{items.map((item) => <tr key={item.id}><td>{item.nom}</td><td>{item.description || "—"}</td><td><div className="admin-actions"><button className="admin-button" onClick={() => edit(item)} aria-label="Modifier"><Pencil size={15} /></button><button className="admin-button danger" onClick={() => remove(item)} aria-label="Supprimer"><Trash2 size={15} /></button></div></td></tr>)}{!items.length && <tr><td colSpan="3" className="admin-muted">Aucune catégorie enregistrée.</td></tr>}</tbody></table></div>
  </section>;
}
