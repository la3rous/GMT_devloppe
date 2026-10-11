import { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";
import CarCard from "../CarCard/CarCard";
import useCurrency from "../../../context/useCurrency";
import { API_BASE_URL, vehicleApi } from "../../../services/api";
import { resolveVehicleImage } from "../../../utils/vehicleImage";
import "./VehiclesSection.css";

function vehicleImage(vehicle) {
  if (vehicle.image?.startsWith("/uploads/")) return `${API_BASE_URL.replace(/\/api\/?$/, "")}${vehicle.image}`;
  return resolveVehicleImage(vehicle.image);
}

function VehiclesSection() {
  const { currency, setCurrency } = useCurrency();
  const [vehicles, setVehicles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    vehicleApi.getAll()
      .then(({ data }) => { if (active) setVehicles(Array.isArray(data) ? data.slice(0, 4) : []); })
      .catch(() => { if (active) setError("Impossible de charger les véhicules."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  return (
    <section className="vehicles-section">
      <div className="vehicles-header">
        <div className="vehicles-title">
          <span className="section-label">NOTRE FLOTTE</span>
          <h2>Véhicules populaires</h2>
          <p>Une sélection de nos modèles les plus demandés pour explorer la Tunisie en toute liberté.</p>
        </div>
        <div className="vehicles-actions">
          <div className="currency-toggle">
            <button className={currency === "DT" ? "currency-active" : ""} onClick={() => setCurrency("DT")}>DT</button>
            <button className={currency === "EUR" ? "currency-active" : ""} onClick={() => setCurrency("EUR")}>€ EUR</button>
          </div>
          <NavLink to="/catalogue" className="see-all">Voir tous les véhicules <span>→</span></NavLink>
        </div>
      </div>

      {loading ? <p role="status">Chargement des véhicules…</p> : error ? <p role="alert">{error}</p> : vehicles.length ? (
        <div className="cars-grid">
          {vehicles.map((vehicle) => <CarCard
            key={vehicle.id}
            id={vehicle.id}
            image={vehicleImage(vehicle)}
            brand={vehicle.marque}
            name={vehicle.modele}
            category={vehicle.categorie?.nom || "Véhicule"}
            seats={`${vehicle.places ?? "—"} places`}
            transmission={vehicle.transmission || "—"}
            fuel={vehicle.carburant || "—"}
            priceDT={vehicle.prixParJour}
          />)}
        </div>
      ) : <p>Aucun véhicule disponible pour le moment.</p>}
    </section>
  );
}

export default VehiclesSection;
