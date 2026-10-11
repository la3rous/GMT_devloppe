import { Link, useLocation } from "react-router-dom";
import "./QuoteConfirmation.css";

function QuoteConfirmation() {
  const { state } = useLocation();
  const formData = state?.formData || {};
  const demande = state?.demande || {};
  const vehicle = state?.vehicle || {};

  return (
    <main className="quote-confirmation">
      <h1>Demande<br />reçue !</h1>
      <p className="confirmation-message">
        Merci {formData.prenom || ""} {formData.nom || ""}. Votre demande de devis a bien été enregistrée.
        <br />Notre équipe vous contactera sous 24h.
      </p>
      <section className="confirmation-summary">
        <h2>Récapitulatif</h2>
        <dl>
          <div><dt>Véhicule</dt><dd>{vehicle.marque || demande.vehicule?.marque || demande.marque || "Véhicule sélectionné"} {vehicle.modele || demande.vehicule?.modele || demande.modele || ""}</dd></div>
          <div><dt>Période</dt><dd>{formData.dateDebut || "—"} → {formData.dateFin || "—"}</dd></div>
          <div><dt>Prise en charge</dt><dd>{formData.lieuPrise || "—"}</dd></div>
          <div><dt>Email</dt><dd>{formData.email || "—"}</dd></div>
          <div><dt>Téléphone</dt><dd>{formData.telephone || "—"}</dd></div>
        </dl>
      </section>
      <div className="confirmation-actions">
        <Link to="/catalogue" className="confirmation-primary">Voir d'autres véhicules →</Link>
        <Link to="/" className="confirmation-secondary">Retour à l'accueil</Link>
      </div>
    </main>
  );
}

export default QuoteConfirmation;
