import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { resolveVehicleImage } from "../../../utils/vehicleImage";
import { quoteRequestApi, vehicleApi } from "../../../services/api";
import "./QuoteRequest.css";

function createIdempotencyKey() {
  return window.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function QuoteRequest() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();


  // =========================
  // PARAMÈTRES URL
  // =========================

  const vehiculeParam = searchParams.get("vehicule");
  const dateDebutParam = searchParams.get("dateDebut");
  const dateFinParam = searchParams.get("dateFin");
  const lieuPriseParam = searchParams.get("lieuPrise");
  const lieuRestitutionParam = searchParams.get("lieuRestitution");
  const personnesParam = searchParams.get("personnes");
  const categorieParam = searchParams.get("categorie");
  const etapeParam = searchParams.get("etape");

  // =========================
  // FORMULAIRE
  // =========================

  const [initialState] = useState(() => {
    let draft = null;
    const savedDraft = sessionStorage.getItem("gmt_quote_draft");
    if (savedDraft) {
      try { draft = JSON.parse(savedDraft); } catch (error) { console.error("Erreur lecture brouillon :", error); }
    }
    return {
      formData: {
        dateDebut: dateDebutParam || draft?.formData?.dateDebut || "",
        dateFin: dateFinParam || draft?.formData?.dateFin || "",
        lieuPrise: lieuPriseParam || draft?.formData?.lieuPrise || "",
        lieuRestitution: lieuRestitutionParam || draft?.formData?.lieuRestitution || "",
        personnes: personnesParam || draft?.formData?.personnes || "",
        categorie: categorieParam || draft?.formData?.categorie || "",
        vehiculeId: vehiculeParam || draft?.formData?.vehiculeId || "",
        nom: draft?.formData?.nom || "",
        prenom: draft?.formData?.prenom || "",
        email: draft?.formData?.email || "",
        telephone: draft?.formData?.telephone || "",
        message: draft?.formData?.message || "",
      },
      step: etapeParam === "1" || etapeParam === "2" ? Number(etapeParam) : draft?.step ?? 0,
    };
  });
  const [formData, setFormData] = useState(initialState.formData);

  // 0 = Dates & lieux
  // 1 = Véhicule
  // 2 = Informations
  const [step, setStep] = useState(initialState.step);

  const [vehicles, setVehicles] = useState([]);
  const [vehiclesLoading, setVehiclesLoading] = useState(false);
  const [vehiclesError, setVehiclesError] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [conflictingPeriods, setConflictingPeriods] = useState([]);
  const submitted = useRef(false);
  const sending = useRef(false);
  const requestId = useRef(null);

  // =========================
  // SAUVEGARDE BROUILLON
  // =========================

  useEffect(() => {
    if (submitted.current) return;
    const draft = {
      formData,
      step,
    };

    sessionStorage.setItem(
      "gmt_quote_draft",
      JSON.stringify(draft)
    );
  }, [formData, step]);

  // =========================
  // CHARGER LES VÉHICULES
  // =========================

  useEffect(() => {
    const fetchVehicles = async () => {
      setVehiclesLoading(true);
      setVehiclesError("");

      try {
        const response = await vehicleApi.getAll();
        const payload = response.data;
        const vehicleList = Array.isArray(payload)
          ? payload
          : payload.vehicules || payload.vehicles || payload.data || [];
        const data = Array.isArray(vehicleList) ? vehicleList : [];

        console.log("Véhicules reçus :", data);

        setVehicles(data);
        setVehiclesError(
          data.length ? "" : "Aucun véhicule n'est enregistré dans la base de données."
        );
        setFormData((previous) => ({
          ...previous,
          vehiculeId: data.some(
            (vehicle) => Number(vehicle.id) === Number(previous.vehiculeId)
          )
            ? previous.vehiculeId
            : "",
        }));
      } catch (err) {
        console.error(err);
        setVehicles([]);
        setVehiclesError("Impossible de charger les véhicules depuis le backend.");
        setFormData((previous) => ({ ...previous, vehiculeId: "" }));
      } finally {
        setVehiclesLoading(false);
      }
    };

    fetchVehicles();
  }, []);

  // =========================
  // MODIFICATION FORMULAIRE
  // =========================

  const handleChange = (e) => {
    const { name, value } = e.target;

    if (["dateDebut", "dateFin", "vehiculeId"].includes(name)) {
      setError("");
      setConflictingPeriods([]);
    }

    setFormData((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  // =========================
  // SUIVANT
  // =========================

  const handleNext = () => {
    setError("");

    if (step === 0) {
      if (
        !formData.dateDebut ||
        !formData.dateFin ||
        !formData.lieuPrise ||
        !formData.lieuRestitution
      ) {
        setError(
          "Veuillez compléter les dates et les lieux."
        );
        return;
      }
    }

    if (step === 1) {
      if (!formData.vehiculeId) {
        setError(
          "Veuillez sélectionner un véhicule."
        );
        return;
      }
    }

    setStep((previous) => previous + 1);
  };

  // =========================
  // RETOUR
  // =========================

  const handlePrevious = () => {
    setError("");
    setConflictingPeriods([]);
    setStep((previous) => previous - 1);
  };

  // =========================
  // CHANGER VÉHICULE
  // =========================

  const handleChangeVehicle = () => {
    setError("");
    setConflictingPeriods([]);
    setFormData((previous) => ({
      ...previous,
      vehiculeId: "",
    }));

    setStep(1);
  };

  // =========================
  // ENVOYER AU BACKEND
  // =========================

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (submitted.current || sending.current || loading) return;

    if (!vehicles.some((vehicle) => Number(vehicle.id) === Number(formData.vehiculeId))) {
      setError("Veuillez sélectionner un véhicule disponible dans la liste.");
      setStep(1);
      return;
    }

    sending.current = true;
    setLoading(true);
    setError("");
    setConflictingPeriods([]);

    try {
      const { data: availability } = await quoteRequestApi.checkAvailability({
        vehiculeId: Number(formData.vehiculeId),
        dateDebut: formData.dateDebut,
        dateFin: formData.dateFin,
      });

      if (!availability.available) {
        setConflictingPeriods(availability.conflictingPeriods || []);
        setError(availability.message || "The selected car is unavailable for these dates.");
        return;
      }

      const data = {
        nom: formData.nom,
        prenom: formData.prenom,
        email: formData.email,
        telephone: formData.telephone,

        vehiculeId: Number(formData.vehiculeId),

        dateDebut: formData.dateDebut,
        dateFin: formData.dateFin,

        // IMPORTANT :
        // le backend demande lieuPriseEnCharge
        lieuPriseEnCharge: formData.lieuPrise,

        lieuRestitution: formData.lieuRestitution,

        message: formData.message,
      };

      const idempotencyKey = requestId.current || sessionStorage.getItem("gmt_quote_request_key") || createIdempotencyKey();
      requestId.current = idempotencyKey;
      if (idempotencyKey) sessionStorage.setItem("gmt_quote_request_key", idempotencyKey);
      const { data: result } = await quoteRequestApi.create(data, idempotencyKey);
      submitted.current = true;

      // Supprimer le brouillon
      sessionStorage.removeItem(
        "gmt_quote_draft"
      );
      sessionStorage.removeItem("gmt_quote_request_key");

      // Aller vers confirmation
      navigate(
        "/devis/confirmation",
        {
          state: {
            demande: result,
            formData: formData,
            vehicle: selectedVehicle,
          },
        }
      );
    } catch (err) {
      console.error(err);
      const responseData = err.response?.data;
      if (err.response?.status === 409 && responseData?.conflictingPeriods) {
        setConflictingPeriods(responseData.conflictingPeriods);
      }
      const backendMessage = typeof responseData === "string" ? responseData : responseData?.message || responseData?.detail || responseData?.title;
      const status = err.response?.status;
      setError(backendMessage || (status === 401
        ? "Le backend refuse cette demande publique (401). Redémarrez le backend GMT avec la dernière version puis réessayez."
        : err.code === "ERR_NETWORK"
          ? "Impossible de joindre le backend GMT. Vérifiez qu'il est démarré sur le port 8081."
          : err.message || "Une erreur est survenue lors de l'envoi de votre demande."));
    } finally {
      if (!submitted.current) sending.current = false;
      setLoading(false);
    }
  };

  // =========================
  // VÉHICULE SÉLECTIONNÉ
  // =========================

  const selectedVehicle = vehicles.find(
    (vehicle) =>
      Number(vehicle.id) ===
      Number(formData.vehiculeId)
  );

  return (
    <main className="quote-page">

      <section className="quote-container">
        <button type="button" className="quote-return" onClick={() => navigate(-1)}>
          <span aria-hidden="true">←</span> Retour
        </button>
        <header className="quote-intro">
          <h1>Demande de<br />devis</h1>
          <p>Remplissez ce formulaire en 3 étapes. Notre équipe vous répondra sous 24h.</p>
        </header>

        <div className="quote-progress">
          <div className="quote-progress-heading">
            <span>Étape {step + 1} / 3 — {[
              "Dates & lieux", "Véhicule", "Contact",
            ][step]}</span>
            <span className="quote-progress-percent">◴ {Math.round(((step + 1) / 3) * 100)}%</span>
          </div>
          <div className="quote-progress-track">
            <span style={{ width: `${((step + 1) / 3) * 100}%` }} />
          </div>
        </div>

        {/* =====================
            BANDEAU VÉHICULE
        ===================== */}

        {selectedVehicle && (
          <div className="selected-vehicle-banner">

            <div className="selected-vehicle-image">

              {resolveVehicleImage(selectedVehicle.image) && (
                <img
                  src={resolveVehicleImage(selectedVehicle.image)}
                  alt={`${selectedVehicle.marque} ${selectedVehicle.modele}`}
                />
              )}

            </div>

            <div className="selected-vehicle-info">

              <span>
                {selectedVehicle.marque}
              </span>

              <strong>
                {selectedVehicle.modele}
              </strong>

              <small>
                {selectedVehicle.categorie?.nom ||
                  selectedVehicle.categorie ||
                  "Véhicule"}
              </small>

            </div>

            <button
              type="button"
              onClick={handleChangeVehicle}
            >
              Changer
            </button>

          </div>
        )}

        {/* =====================
            FORMULAIRE
        ===================== */}

        <form
          className="quote-form"
          onSubmit={handleSubmit}
        >

          {/* =====================
              ÉTAPE 1
          ===================== */}

          {step === 0 && (
            <div className="form-section">

              <h2>
                Dates & lieux
              </h2>

              <p className="section-description">
                Quand et où souhaitez-vous louer
                votre véhicule ?
              </p>

              <div className="form-grid">

                <div className="form-group">

                  <label htmlFor="dateDebut">
                    Date de début *
                  </label>

                  <input
                    id="dateDebut"
                    name="dateDebut"
                    type="date"
                    value={formData.dateDebut}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div className="form-group">

                  <label htmlFor="dateFin">
                    Date de fin *
                  </label>

                  <input
                    id="dateFin"
                    name="dateFin"
                    type="date"
                    value={formData.dateFin}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div className="form-group">

                  <label htmlFor="lieuPrise">
                    Lieu de prise en charge *
                  </label>

                  <input
                    id="lieuPrise"
                    name="lieuPrise"
                    type="text"
                    value={formData.lieuPrise}
                    onChange={handleChange}
                    placeholder="Aéroport Tunis-Carthage"
                    required
                  />

                </div>

                <div className="form-group">

                  <label htmlFor="lieuRestitution">
                    Lieu de restitution *
                  </label>

                  <input
                    id="lieuRestitution"
                    name="lieuRestitution"
                    type="text"
                    value={formData.lieuRestitution}
                    onChange={handleChange}
                    placeholder="Agence GMT Sousse"
                    required
                  />

                </div>

              </div>

              <div className="form-group">

                <label htmlFor="personnes">
                  Nombre de personnes
                </label>

                <input
                  id="personnes"
                  name="personnes"
                  type="number"
                  min="1"
                  value={formData.personnes}
                  onChange={handleChange}
                  placeholder="Ex : 4"
                />

              </div>

              {error && (
                <div className="quote-error">
                  {error}
                </div>
              )}

              <div className="quote-actions">

                <button
                  type="button"
                  className="quote-next"
                  onClick={handleNext}
                >
                  Continuer →
                </button>

              </div>

            </div>
          )}

          {/* =====================
              ÉTAPE 2
          ===================== */}

          {step === 1 && (
            <div className="form-section">

              <div className="form-group">
                <label htmlFor="vehiculeId">
                  Véhicule souhaité
                </label>
                <select
                  id="vehiculeId"
                  name="vehiculeId"
                  value={formData.vehiculeId}
                  onChange={handleChange}
                  required
                >
                  <option value="">
                    {vehiclesLoading ? "Chargement des véhicules..." : "Sélectionnez un véhicule"}
                  </option>
                  {vehicles.map((vehicle) => {
                    const category = vehicle.categorie?.nom || vehicle.categorie || "Véhicule";
                    return (
                      <option key={vehicle.id} value={vehicle.id}>
                        {vehicle.marque} {vehicle.modele} — {category}
                      </option>
                    );
                  })}
                </select>
              </div>
              {vehiclesError && (
                <div className="vehicle-error">
                  {vehiclesError}
                </div>
              )}
              <div className="form-group">
                <label htmlFor="personnes">Nombre de personnes</label>
                <input id="personnes" name="personnes" type="number" min="1" value={formData.personnes} onChange={handleChange} placeholder="1" />
              </div>
              <div className="form-group">
                <label htmlFor="message">Message (optionnel)</label>
                <textarea id="message" name="message" value={formData.message} onChange={handleChange} placeholder="Précisez vos besoins..." rows="4" />
              </div>
              {error && (
                <div className="quote-error">
                  {error}
                </div>
              )}

              <div className="quote-actions">

                <button
                  type="button"
                  className="quote-back"
                  onClick={handlePrevious}
                >
                  ← Retour
                </button>

                <button
                  type="button"
                  className="quote-next"
                  onClick={handleNext}
                >
                  Continuer →
                </button>

              </div>

            </div>
          )}

          {/* =====================
              ÉTAPE 3
          ===================== */}

          {step === 2 && (
            <div className="form-section">

              <h2>
                Vos informations
              </h2>

              <p className="section-description">
                Comment pouvons-nous vous contacter ?
              </p>

              <div className="form-grid">

                <div className="form-group">

                  <label htmlFor="prenom">
                    Prénom *
                  </label>

                  <input
                    id="prenom"
                    name="prenom"
                    type="text"
                    value={formData.prenom}
                    onChange={handleChange}
                    placeholder="Votre prénom"
                    required
                  />

                </div>

                <div className="form-group">

                  <label htmlFor="nom">
                    Nom *
                  </label>

                  <input
                    id="nom"
                    name="nom"
                    type="text"
                    value={formData.nom}
                    onChange={handleChange}
                    placeholder="Votre nom"
                    required
                  />

                </div>

                <div className="form-group">

                  <label htmlFor="email">
                    Email *
                  </label>

                  <input
                    id="email"
                    name="email"
                    type="email"
                    value={formData.email}
                    onChange={handleChange}
                    placeholder="exemple@email.com"
                    required
                  />

                </div>

                <div className="form-group">

                  <label htmlFor="telephone">
                    Téléphone *
                  </label>

                  <input
                    id="telephone"
                    name="telephone"
                    type="tel"
                    value={formData.telephone}
                    onChange={handleChange}
                    placeholder="+216 XX XXX XXX"
                    required
                  />

                </div>

              </div>

              <div className="form-group">

                <label htmlFor="message">
                  Message
                </label>

                <textarea
                  id="message"
                  name="message"
                  value={formData.message}
                  onChange={handleChange}
                  placeholder="Une demande particulière ?"
                  rows="5"
                />

              </div>

              {error && (
                <>
                  <div className="quote-error" role="alert">{error}</div>
                  {conflictingPeriods.length > 0 && (
                    <div className="quote-conflict-actions">
                      <button type="button" className="quote-back" onClick={() => {
                        setError("");
                        setConflictingPeriods([]);
                        setStep(0);
                      }}>
                        Modifier les dates
                      </button>
                      <button type="button" className="quote-back" onClick={handleChangeVehicle}>
                        Choisir un autre véhicule
                      </button>
                    </div>
                  )}
                </>
              )}

              <div className="quote-actions">

                <button
                  type="button"
                  className="quote-back"
                  onClick={handlePrevious}
                >
                  ← Retour
                </button>

                <button
                  type="submit"
                  className="quote-next"
                  disabled={loading}
                >
                  {loading
                    ? "Envoi..."
                    : "Envoyer ma demande →"}
                </button>

              </div>

            </div>
          )}

        </form>

        <div className="quote-step-dots" aria-label={`Étape ${step + 1} sur 3`}>
          {[0, 1, 2].map((index) => (
            <span key={index} className={index <= step ? "complete" : ""}>
              {index < step ? "✓" : index + 1}
            </span>
          ))}
        </div>

      </section>

    </main>
  );
}

export default QuoteRequest;

