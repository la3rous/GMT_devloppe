import { useEffect, useMemo, useState } from "react";
import { NavLink } from "react-router-dom";
import { CarFront, RotateCw } from "lucide-react";
import useCurrency from "../../../context/useCurrency";
import { API_BASE_URL, categoryApi, vehicleApi } from "../../../services/api";
import { resolveVehicleImage } from "../../../utils/vehicleImage";

import "./Catalog.css";



function Catalog() {
  const { currency, setCurrency, getPrice } = useCurrency();
  const [apiVehicles, setApiVehicles] = useState([]);
  const [apiCategories, setApiCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [categoryError, setCategoryError] = useState("");
  const [failedImages, setFailedImages] = useState(() => new Set());
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    vehicleApi.getAll()
      .then(({ data }) => {
        if (!active) return;
        const vehicles = Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : null;
        if (!vehicles) throw new Error("Réponse invalide de l’API véhicules.");
        setApiVehicles(vehicles);
        setFailedImages(new Set());
      })
      .catch(() => {
        if (active) setLoadError("Impossible de charger les véhicules. Vérifiez que le backend GMT est démarré, puis réessayez.");
      })
      .finally(() => { if (active) setLoading(false); });
    categoryApi.getAll()
      .then(({ data }) => {
        if (!active) return;
        setApiCategories(Array.isArray(data) ? data : Array.isArray(data?.content) ? data.content : []);
        setCategoryError("");
      })
      .catch(() => {
        if (!active) return;
        setApiCategories([]);
        setCategoryError("Les catégories n’ont pas pu être chargées. Les catégories des véhicules reçus restent disponibles dans les filtres.");
      });
    return () => { active = false; };
  }, [reloadKey]);

  const retryLoading = () => {
    setLoading(true);
    setLoadError("");
    setCategoryError("");
    setReloadKey((key) => key + 1);
  };

  const vehicles = useMemo(() => apiVehicles.map((vehicle) => ({
    id: vehicle.id,
    brand: vehicle.marque || "",
    model: vehicle.modele || "",
    category: vehicle.categorie?.nom || "Sans catégorie",
    year: vehicle.annee,
    seats: vehicle.places ?? "—",
    transmission: vehicle.transmission ? vehicle.transmission[0] + vehicle.transmission.slice(1).toLowerCase() : "—",
    fuel: vehicle.carburant === "ELECTRIQUE" ? "Électrique" : vehicle.carburant ? vehicle.carburant[0] + vehicle.carburant.slice(1).toLowerCase() : "—",
    priceDT: Number(vehicle.prixParJour),
    image: vehicle.image?.startsWith("/uploads/") ? `${API_BASE_URL.replace(/\/api\/?$/, "")}${vehicle.image}` : resolveVehicleImage(vehicle.image),
    availability: "Actif",
  })), [apiVehicles]);
  /* =========================
     ETATS
  ========================= */

  const [search, setSearch] = useState("");

  const [category, setCategory] = useState("Toutes");

  const [brand, setBrand] = useState("Toutes");

  const [transmission, setTransmission] = useState("Toutes");

  const [fuel, setFuel] = useState("Tous");

  const [availability, setAvailability] = useState("Toutes");

  /* =========================
     CATEGORIES
  ========================= */

  const categories = ["Toutes", ...new Set([
    ...apiCategories.map((item) => item.nom),
    ...vehicles.map((vehicle) => vehicle.category),
  ].filter((name) => name && name !== "Sans catégorie"))];
  const brands = [...new Set(vehicles.map((vehicle) => vehicle.brand).filter(Boolean))].sort();


  /* =========================
     FILTRAGE
  ========================= */

  const filteredVehicles = useMemo(() => {

    return vehicles.filter((vehicle) => {

      const searchValue = search.toLowerCase();

      const matchesSearch =
        vehicle.brand.toLowerCase().includes(searchValue) ||
        vehicle.model.toLowerCase().includes(searchValue);

      const matchesCategory =
        category === "Toutes" ||
        vehicle.category === category;

      const matchesBrand =
        brand === "Toutes" ||
        vehicle.brand === brand;

      const matchesTransmission =
        transmission === "Toutes" ||
        vehicle.transmission === transmission;

      const matchesFuel =
        fuel === "Tous" ||
        vehicle.fuel === fuel;

      const matchesAvailability =
        availability === "Toutes" ||
        vehicle.availability === availability;

      return (
        matchesSearch &&
        matchesCategory &&
        matchesBrand &&
        matchesTransmission &&
        matchesFuel &&
        matchesAvailability
      );

    });

  }, [
    search,
    category,
    brand,
    transmission,
    fuel,
    availability,
    vehicles
  ]);


  /* =========================
     RESET
  ========================= */

  const resetFilters = () => {

    setSearch("");
    setCategory("Toutes");
    setBrand("Toutes");
    setTransmission("Toutes");
    setFuel("Tous");
    setAvailability("Toutes");

  };


  /* =========================
     RENDER
  ========================= */

  return (

    <main className="catalog-page">


      {/* =========================
          INTRO
      ========================= */}

      <section className="catalog-intro">

        <div className="catalog-intro-text">

          <span className="catalog-label">
            CATALOGUE COMPLET
          </span>

          <h1>
            Notre flotte
          </h1>

          <p>
            Découvrez l'ensemble de nos véhicules premium
            disponibles à la location en Tunisie.
          </p>

          <span className="vehicle-count">
            {loading ? "Chargement des véhicules…" : loadError ? "Catalogue indisponible" : <>{filteredVehicles.length} véhicule
            {filteredVehicles.length > 1 ? "s" : ""} disponible
            {filteredVehicles.length > 1 ? "s" : ""}</>}
          </span>

        </div>


        {/* DEVISE */}

        <div className="catalog-currency">

          <button
            className={currency === "DT" ? "active" : ""}
            onClick={() => setCurrency("DT")}
          >
            DT
          </button>

          <button
            className={currency === "EUR" ? "active" : ""}
            onClick={() => setCurrency("EUR")}
          >
            € EUR
          </button>

        </div>

      </section>


      {/* =========================
          BARRE FILTRES
      ========================= */}

      <section className="catalog-filters">


        {/* CATEGORIES */}

        <div className="category-filters">

          {categories.map((item) => (

            <button
              key={item}
              className={
                category === item
                  ? "category-button active"
                  : "category-button"
              }
              onClick={() => setCategory(item)}
            >
              {item}
            </button>

          ))}

        </div>


        {/* RECHERCHE */}

        <div className="search-box">

          <span>
            ⌕
          </span>

          <input
            type="text"
            placeholder="Rechercher un modèle..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

        </div>

      </section>

      {categoryError && <p className="catalog-load-warning" role="status">{categoryError}</p>}


      {/* =========================
          FILTRES AVANCES
      ========================= */}

      <section className="advanced-filters">

        <select
          value={brand}
          onChange={(e) => setBrand(e.target.value)}
        >
          <option value="Toutes">
            Toutes les marques
          </option>

          {brands.map((item) => <option key={item} value={item}>{item}</option>)}
        </select>


        <select
          value={transmission}
          onChange={(e) => setTransmission(e.target.value)}
        >
          <option value="Toutes">
            Toutes transmissions
          </option>

          <option value="Automatique">
            Automatique
          </option>

          <option value="Manuelle">
            Manuelle
          </option>
        </select>


        <select
          value={fuel}
          onChange={(e) => setFuel(e.target.value)}
        >
          <option value="Tous">
            Tous carburants
          </option>

          <option value="Diesel">
            Diesel
          </option>

          <option value="Essence">
            Essence
          </option>

          <option value="Hybride">
            Hybride
          </option>

          <option value="Électrique">
            Électrique
          </option>
        </select>


        <select
          value={availability}
          onChange={(e) => setAvailability(e.target.value)}
        >
          <option value="Toutes">
            Toutes disponibilités
          </option>

          <option value="Actif">
            Actif
          </option>

        </select>


        <button
          className="reset-filters"
          onClick={resetFilters}
        >
          Réinitialiser
        </button>

      </section>


      {/* =========================
          RESULTATS
      ========================= */}

      <section className="catalog-results">

        <div className="results-header">

          <span>
            {loading ? "Chargement…" : loadError ? "Résultats indisponibles" : <>{filteredVehicles.length} résultat
            {filteredVehicles.length > 1 ? "s" : ""}</>}
          </span>

        </div>


        {/* =========================
            GRID
        ========================= */}

        {loading ? (
          <div className="no-results"><h2>Chargement des véhicules…</h2></div>
        ) : loadError ? (
          <div className="no-results"><h2>{loadError}</h2><button onClick={retryLoading}><RotateCw size={15} /> Réessayer</button></div>
        ) : !vehicles.length ? (
          <div className="no-results"><h2>Aucun véhicule disponible pour le moment</h2></div>
        ) : filteredVehicles.length > 0 ? (

          <div className="catalog-grid">

            {filteredVehicles.map((vehicle) => (

              <article
                className="catalog-card"
                key={vehicle.id}
              >

                {/* IMAGE */}

                <div className="catalog-card-image">

                  {vehicle.image && !failedImages.has(vehicle.id) ? <img
                    src={vehicle.image}
                    alt={`${vehicle.brand} ${vehicle.model}`}
                    onError={() => setFailedImages((current) => new Set(current).add(vehicle.id))}
                  /> : <div className="catalog-image-placeholder" role="img" aria-label={`Photo indisponible pour ${vehicle.brand} ${vehicle.model}`}><CarFront size={34} /><span>Photo indisponible</span></div>}


                  <span className="card-category">
                    {vehicle.category}
                  </span>


                  <span
                    className={
                      vehicle.availability === "Actif"
                        ? "card-availability available"
                        : "card-availability reservation"
                    }
                  >
                    {vehicle.availability}
                  </span>


                  {/* SPECS */}

                  <div className="card-specs">

                    <span>
                      ♙ {vehicle.seats}
                    </span>

                    <span>
                      ⚙ {vehicle.transmission}
                    </span>

                    <span>
                      ⛽ {vehicle.fuel}
                    </span>

                  </div>

                </div>


                {/* CONTENU */}

                <div className="catalog-card-content">

                  <span className="card-brand">
                    {vehicle.brand}
                  </span>

                  <h2>
                    {vehicle.model}
                  </h2>


                  <div className="card-price-label">
                    À partir de
                  </div>


                  <div className="card-price">

                    <strong>
                    {(() => {
                      const price = getPrice(vehicle.priceDT);
                      return price == null ? "—" : Number(price).toLocaleString("fr-TN", { maximumFractionDigits: 2 });
                    })()}
                    </strong>

                    <span>
                      {currency === "DT"
                        ? "DT"
                        : "€"}
                    </span>

                    <small>
                      /jour
                    </small>

                  </div>


                  {/* BOUTONS */}

                  <div className="catalog-card-buttons">

                    <NavLink
                      to={`/vehicule/${vehicle.id}`}
                      className="catalog-details-button"
                    >
                      Voir le véhicule
                    </NavLink>


                    <NavLink
                      to={`/devis?vehicule=${vehicle.id}`}
                      className="catalog-quote-button"
                    >
                      Devis
                    </NavLink>

                  </div>

                </div>

              </article>

            ))}

          </div>

        ) : (

          /* AUCUN RESULTAT */

          <div className="no-results">

            <h2>
              Aucun véhicule ne correspond à vos critères
            </h2>

            <p>
              Essayez de modifier vos filtres ou votre recherche.
            </p>

            <button onClick={resetFilters}>
              Réinitialiser les filtres
            </button>

          </div>

        )}

      </section>

    </main>
  );
}

export default Catalog;

