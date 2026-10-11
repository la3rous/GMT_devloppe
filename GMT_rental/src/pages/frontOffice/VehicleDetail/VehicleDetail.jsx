import { useEffect, useState } from "react";
import { NavLink, useParams } from "react-router-dom";
import useCurrency from "../../../context/useCurrency";
import { API_BASE_URL, vehicleApi } from "../../../services/api";
import { resolveVehicleImage } from "../../../utils/vehicleImage";
import "./VehicleDetail.css";

function VehicleDetail() {
  const { id } = useParams();
  const { currency, getPrice } = useCurrency();
  const [result, setResult] = useState({ id: null, vehicle: null, error: "" });
  const loading = result.id !== id;
  const vehicle = loading ? null : result.vehicle;
  const error = loading ? "" : result.error;

  useEffect(() => {
    let active = true;
    vehicleApi.getById(id)
      .then(({ data }) => { if (active) setResult({ id, vehicle: data, error: "" }); })
      .catch((requestError) => {
        if (!active) return;
        const message = requestError.response?.status === 404 ? "Véhicule introuvable" : "Impossible de charger ce véhicule.";
        setResult({ id, vehicle: null, error: message });
      });
    return () => { active = false; };
  }, [id]);

  if (loading) return <main className="vehicle-not-found"><h1>Chargement du véhicule…</h1></main>;
  if (error || !vehicle) return <main className="vehicle-not-found"><h1>{error || "Véhicule introuvable"}</h1><NavLink to="/catalogue">Retour au catalogue</NavLink></main>;

  const category = vehicle.categorie?.nom || "Véhicule";
  const image = vehicle.image?.startsWith("/uploads/")
    ? `${API_BASE_URL.replace(/\/api\/?$/, "")}${vehicle.image}`
    : resolveVehicleImage(vehicle.image);
  const price = getPrice(vehicle.prixParJour);

  return (
    <main className="vehicle-detail-page">
      <div className="vehicle-detail-container">
        <NavLink to="/catalogue" className="back-catalogue">← Retour au catalogue</NavLink>
        <section className="vehicle-detail">
          <div className="vehicle-image-wrapper">
            <span className="vehicle-category">{category}</span>
            {image && <img src={image} alt={`${vehicle.marque} ${vehicle.modele}`} />}
          </div>
          <div className="vehicle-info">
            <span className="vehicle-brand">{vehicle.marque}</span>
            <h1>{vehicle.modele}</h1>
            <div className="vehicle-status"><span className="status-icon">✓</span><span>{vehicle.statut === "ACTIF" ? "Actif" : "Indisponible"}</span></div>
            <div className="price-box">
              <span className="price-title">TARIF À PARTIR DE</span>
              <div className="price"><strong>{price == null ? "—" : Number(price).toLocaleString("fr-TN", { maximumFractionDigits: 2 })}</strong><span className="currency">{currency === "DT" ? "DT" : "€"}</span><span className="per-day">/jour</span></div>
              <NavLink to={`/devis?vehicule=${vehicle.id}`} className="detail-quote-button">Demander un devis</NavLink>
            </div>
            <div className="technical-box">
              <h2>FICHE TECHNIQUE</h2>
              <div className="technical-row"><span><span className="tech-icon">🚗</span>Catégorie</span><strong>{category}</strong></div>
              <div className="technical-row"><span><span className="tech-icon">▣</span>Année</span><strong>{vehicle.annee || "—"}</strong></div>
              <div className="technical-row"><span><span className="tech-icon">♙</span>Places</span><strong>{vehicle.places ?? "—"}</strong></div>
              <div className="technical-row"><span><span className="tech-icon">⚙</span>Transmission</span><strong>{vehicle.transmission || "—"}</strong></div>
              <div className="technical-row"><span><span className="tech-icon">⛽</span>Carburant</span><strong>{vehicle.carburant || "—"}</strong></div>
            </div>
            <div className="description-box"><h2>DESCRIPTION</h2><p>{vehicle.description || "Contactez notre équipe pour plus d’informations sur ce véhicule."}</p></div>
            <div className="vehicle-benefits"><div className="benefit"><span className="benefit-icon">♢</span>Assurance incluse</div><div className="benefit"><span className="benefit-icon">⌖</span>Livraison aéroport</div></div>
          </div>
        </section>
      </div>
    </main>
  );
}

export default VehicleDetail;
