import "./CTASection.css";

function CTASection() {
  return (
    <section className="cta-section">

      <div className="cta-overlay" />

      <div className="cta-content">

        <span className="cta-badge">
          Devis gratuit — réponse sous 24h
        </span>

        <h2>
          Prêt à prendre
          <br />
          la route ?
        </h2>

        <p>
          Choisissez votre véhicule et laissez-nous préparer
          votre prochaine escapade en Tunisie.
        </p>

        <div className="cta-stats">

          <div>
            <strong>5000+</strong>
            <span>clients satisfaits</span>
          </div>

          <div>
            <strong>50+</strong>
            <span>véhicules premium</span>
          </div>

          <div>
            <strong>24/7</strong>
            <span>assistance</span>
          </div>

        </div>

      </div>

    </section>
  );
}

export default CTASection;
