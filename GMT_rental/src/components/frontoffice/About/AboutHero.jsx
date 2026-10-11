import { NavLink } from "react-router-dom";
import aproposImage from "../../../assets/images/about/apropos.jpg";

function AboutHero() {
  return (
    <section className="about-hero">

      <div className="about-container">

        <div className="about-hero-content">

          <span className="about-label">
            À PROPOS DE GMT
          </span>

          <h1>
            L'art du voyage
            <br />
            <span>tunisien</span>
          </h1>

          <p>
            Depuis plus de 12 ans, GMT accompagne les voyageurs
            et professionnels à la découverte de la Tunisie.
            Nous croyons que la location de véhicule n'est pas
            un simple service, mais le premier chapitre d'une
            histoire de voyage.
          </p>

          <p>
            Notre mission : offrir une mobilité premium, fiable
            et accessible, tout en valorisant le patrimoine et
            l'hospitalité tunisienne. De Tunis à Djerba, des
            côtes de Sidi Bou Saïd aux dunes du Sahara, nous
            rendons chaque trajet inoubliable.
          </p>

          <NavLink
            to="/devis"
            className="about-primary-button"
          >
            Demander un devis
            <span>→</span>
          </NavLink>

        </div>

        <div className="about-hero-image">

          <img
            src={aproposImage}
            alt="Voyage en Tunisie avec GMT"
          />

        </div>

      </div>

    </section>
  );
}

export default AboutHero;