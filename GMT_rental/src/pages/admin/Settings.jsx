import { useEffect, useState } from "react";
import { adminApiService } from "../../services/admin/api";

export default function AdminSettings() {
  const [value, setValue] = useState("");
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);
  useEffect(() => { adminApiService.settings().then(({ data }) => { const setting = (data || []).find((item) => item.cle === "TAUX_EUR_TND"); if (setting) setValue(setting.valeur); }).catch((requestError) => setError(requestError.response?.status === 401 ? "Session expirée. Reconnectez-vous à l’administration." : "Impossible de charger les paramètres.")); }, []);
  const submit = async (event) => { event.preventDefault(); setError(""); setSaved(false); setSaving(true); try { const { data } = await adminApiService.saveSetting({ cle: "TAUX_EUR_TND", valeur: value, description: "Taux de conversion EUR vers TND" }); setValue(data.valeur); setSaved(true); } catch (requestError) { const responseData = requestError.response?.data; setError(responseData?.message || responseData?.detail || responseData?.title || "Impossible d’enregistrer le taux."); } finally { setSaving(false); } };
  return <section><div className="admin-page-head"><div><h1>Paramètres</h1><p>Valeurs utilisées par la plateforme GMT Rental.</p></div></div>{error && <p className="admin-error">{error}</p>}{saved && <p className="admin-success">Paramètre enregistré.</p>}<form className="admin-card" onSubmit={submit}><h2>Conversion de devise</h2><p className="admin-muted">Valeur indicative de 1 euro en dinars tunisiens.</p><label className="admin-field" style={{ maxWidth: 360 }}><span>Taux EUR → TND</span><input type="number" min="0" step="0.001" required value={value} onChange={(e) => setValue(e.target.value)} /></label><div style={{ marginTop: 17 }}><button className="admin-button primary" disabled={saving}>{saving ? "Enregistrement…" : "Enregistrer"}</button></div></form></section>;
}
