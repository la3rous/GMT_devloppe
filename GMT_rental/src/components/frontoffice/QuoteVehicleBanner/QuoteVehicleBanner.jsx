import "./QuoteVehicleBanner.css";

function QuoteVehicleBanner({
  vehicle,
  onChangeVehicle,
}) {
  if (!vehicle) {
    return null;
  }

  const category =
    vehicle.categorie?.nom ||
    vehicle.categorie ||
    "Véhicule";

  const price =
    vehicle.prixParJour ??
    vehicle.prixDT ??
    vehicle.prix ??
    null;

  return (
    <div className="quote-vehicle-banner">

      {/* IMAGE */}
      <div className="quote-vehicle-banner-image">

        {vehicle.image ? (
          <img
            src={vehicle.image}
            alt={`${vehicle.marque} ${vehicle.modele}`}
          />
        ) : (
          <div className="quote-vehicle-no-image">
            GMT
          </div>
        )}

      </div>

      {/* INFORMATIONS */}
      <div className="quote-vehicle-banner-info">

        <span className="quote-vehicle-brand">
          {vehicle.marque}
        </span>

        <h3>
          {vehicle.modele}
        </h3>

        <span className="quote-vehicle-category">
          {category}
        </span>

        {price !== null && (
          <div className="quote-vehicle-price">
            <strong>
              {price} DT
            </strong>

            <span>
              / jour
            </span>
          </div>
        )}

      </div>

      {/* BOUTON */}
      <button
        type="button"
        className="quote-vehicle-change"
        onClick={onChangeVehicle}
      >
        Changer
      </button>

    </div>
  );
}

export default QuoteVehicleBanner;