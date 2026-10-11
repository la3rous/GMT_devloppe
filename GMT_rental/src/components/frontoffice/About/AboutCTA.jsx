import { NavLink } from "react-router-dom";

function AboutCTA() {
  return (
    <section className="about-cta">

      <div className="about-cta-content">

        <span className="about-label">
          GMT LOCATION
        </span>

        <h2>
          Prêt à partir sur les routes
          <br />
          de Tunisie ?
        </h2>

        <p>
          Découvrez nos véhicules et préparez votre
          prochaine aventure avec GMT.
        </p>

        <div className="about-cta-buttons">

          <NavLink
            to="/catalogue"
            className="about-primary-button"
          >
            Voir nos véhicules
            <span>→</span>
          </NavLink>

          <NavLink
            to="/contact"
            className="about-secondary-button"
          >
            Nous contacter
          </NavLink>

        </div>

      </div>

    </section>
  );
}

export default AboutCTA;