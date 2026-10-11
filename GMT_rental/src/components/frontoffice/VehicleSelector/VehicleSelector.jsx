import { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";
import { vehicleApi } from "../../../services/api";
import "./VehicleSelector.css";

function VehicleSelector() {
  const [vehicles, setVehicles] = useState([]);
  const [vehicleId, setVehicleId] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    vehicleApi.getAll()
      .then(({ data }) => { if (active) setVehicles(Array.isArray(data) ? data : []); })
      .catch(() => { if (active) setError("Impossible de charger les véhicules."); });
    return () => { active = false; };
  }, []);

  return (
    <section className="vehicle-selector">
      <div className="selector-title">CHOISISSEZ VOTRE VÉHICULE</div>
      <select value={vehicleId} onChange={(event) => setVehicleId(event.target.value)}>
        <option value="">Sélectionnez un modèle</option>
        {vehicles.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>
          {vehicle.marque} {vehicle.modele} — {vehicle.categorie?.nom || "Véhicule"}
        </option>)}
      </select>
      {error && <small role="alert">{error}</small>}
      <NavLink className="vehicle-selector-reserve" to={vehicleId ? `/devis?vehicule=${vehicleId}` : "/devis"}>Demander un devis</NavLink>
    </section>
  );
}

export default VehicleSelector;
