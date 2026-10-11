import {
  ShieldCheck,
  Clock,
  MapPin,
  Sparkles,
  Wallet,
  Headset,
  Wifi,
  Baby,
  Navigation,
} from "lucide-react";

import aproposImage from "../../../assets/images/about/apropos.jpg";

import "./AdvantagesSection.css";

const advantages = [
  {
    icon: ShieldCheck,
    title: "Sécurité avant tout",
    text: "Des véhicules entretenus et contrôlés avant chaque location.",
  },
  {
    icon: Clock,
    title: "Service ponctuel",
    text: "Une prise en charge organisée selon vos horaires et votre itinéraire.",
  },
  {
    icon: MapPin,
    title: "Partout en Tunisie",
    text: "Livraison et récupération du véhicule selon vos besoins.",
  },
  {
    icon: Sparkles,
    title: "Véhicules premium",
    text: "Une sélection de véhicules modernes, confortables et élégants.",
  },
  {
    icon: Wallet,
    title: "Tarifs transparents",
    text: "Des tarifs clairs avec un devis gratuit avant votre réservation.",
  },
  {
    icon: Headset,
    title: "Assistance 24/7",
    text: "Notre équipe reste disponible lorsque vous en avez besoin.",
  },
];

function AdvantagesSection() {
  return (
    <section className="advantages-section">

      <div className="advantages-glow" />

      <div className="advantages-container">

        <div className="advantages-header">

          <span className="advantages-label">
            POURQUOI GMT
          </span>

          <h2>
            Une expérience
            <br />
            pensée pour vous
          </h2>

          <p>
            Bien plus qu'une location de voiture, nous vous accompagnons
            pour rendre chaque trajet plus simple et plus agréable.
          </p>

        </div>

        <div className="advantages-grid">

          {advantages.map((advantage, index) => (
            <AdvantageCard
              key={advantage.title}
              {...advantage}
              delay={index}
            />
          ))}

          {/* SERVICE GMT EN PLUS */}
          <div
            className="advantage-special"
            style={{
              backgroundImage: `url(${aproposImage})`,
            }}
          >

            <div className="advantage-special-overlay" />

            <div className="advantage-special-content">

              <div className="advantage-special-icons">

                <Wifi size={19} />
                <Baby size={19} />
                <Navigation size={19} />

              </div>

              <span className="advantage-special-label">
                LE SERVICE GMT EN PLUS
              </span>

              <h3>
                Voyagez comme vous l'imaginez.
              </h3>

              <p>
                Conciergerie voyage : itinéraires conseillés,
                siège enfant, GPS et wifi à bord sur demande.
              </p>

            </div>

          </div>

        </div>

        <div className="advantages-reassurance">

          <span>✓ Sans paiement en ligne</span>
          <span>✓ Devis gratuit</span>
          <span>✓ Réponse sous 24h</span>

        </div>

        <div className="advantages-proof">

          <strong>5000+</strong>
          <span>clients accompagnés</span>

        </div>

      </div>

    </section>
  );
}

function AdvantageCard({
  icon: Icon,
  title,
  text,
  delay,
}) {
  return (
    <article
      className="advantage-card"
      style={{
        "--delay": `${delay * 0.08}s`,
      }}
    >

      <div className="advantage-icon">
        <Icon size={23} />
      </div>

      <h3>{title}</h3>

      <p>{text}</p>

    </article>
  );
}

export default AdvantagesSection;