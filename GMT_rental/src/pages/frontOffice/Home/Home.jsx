import { Component, Suspense, lazy, useState } from "react";
import { Link } from "react-router-dom";
import fallbackPhoto from "../../../assets/images/hero/hero.png";
import "./Home.css";

const CarScene = lazy(() => import("../../../components/frontoffice/CarScene/CarScene.jsx"));
import VehicleSelector from "../../../components/frontoffice/VehicleSelector/VehicleSelector";
import VehiclesSection from "../../../components/frontoffice/VehiclesSection/VehiclesSection";
import AdvantagesSection from "../../../components/frontoffice/AdvantagesSection/AdvantagesSection";
import CTASection from "../../../components/frontoffice/CTASection/CTASection";

const specs = [
  { label: "Puissance", value: "457", unit: "ch", className: "spec-card--comfort" },
  { label: "Vitesse max.", value: "250", unit: "km/h", className: "spec-card--interior" },
  { label: "Moteur", value: "V8", unit: "6,2 L", className: "spec-card--driver" },
  { label: "0–100 km/h", value: "4,5", unit: "s", className: "spec-card--energy" },
];

class SceneErrorBoundary extends Component {
  state = { failed: false };

  static getDerivedStateFromError() { return { failed: true }; }

  render() {
    return this.state.failed ? this.props.fallback : this.props.children;
  }
}

function CarFallback() {
  const [failed, setFailed] = useState(false);
  return (
    <div className="car-fallback" role="img" aria-label="Berline de luxe">
      {!failed && <img src={fallbackPhoto} alt="" onError={() => setFailed(true)} />}
      <span>GMT · COLLECTION PRESTIGE</span>
    </div>
  );
}

function Home() {
  return (
    <main>
      <section className="hero hero--lux" aria-labelledby="hero-title">
        <div className="hero-ambience" aria-hidden="true">
          <span className="hero-glow hero-glow--gold" />
          <span className="hero-glow hero-glow--shadow" />
          <span className="hero-grid" />
          <span className="hero-orbit hero-orbit--outer" />
          <span className="hero-orbit hero-orbit--inner" />
          <span className="hero-mega-word">LUXE</span>
          <span className="hero-particle hero-particle--one" />
          <span className="hero-particle hero-particle--two" />
          <span className="hero-particle hero-particle--three" />
        </div>

        <div className="hero-copy">
          <p className="hero-badge"><span aria-hidden="true" /> Collection Prestige · disponible maintenant</p>
          <h1 id="hero-title">Roulez dans <span>le raffinement.</span></h1>
          <p className="hero-description">
            Berlines de prestige et SUV haut de gamme, avec ou sans chauffeur.
            Réservez en toute simplicité, sans frais cachés.
          </p>
          <Link className="hero-button-primary" to="/devis">Réserver votre véhicule <span aria-hidden="true">↗</span></Link>
        </div>

        <div className="hero-car-stage" aria-label="Présentation 3D d'une Mercedes-Benz C63 AMG 2008">
          <SceneErrorBoundary fallback={<CarFallback />}>
            <Suspense fallback={<CarFallback />}><CarScene fallback={<CarFallback />} /></Suspense>
          </SceneErrorBoundary>
          {specs.map((spec) => (
            <div className={`spec-card ${spec.className}`} key={spec.label}>
              <span className="spec-card-label">{spec.label}</span>
              <span className="spec-card-value">{spec.value}<small>{spec.unit}</small></span>
            </div>
          ))}
        </div>
      </section>

      <VehicleSelector />

      <div className="lux-marquee" aria-label="Services GMT Rent a Car">
        <div className="lux-marquee-track">
          {[0, 1].map((copy) => (
            <span className="lux-marquee-group" key={copy} aria-hidden={copy === 1}>
              { ["Berlines de prestige", "SUV haut de gamme", "Chauffeur privé", "Livraison à domicile", "Assistance 24/7", "Kilométrage illimité"].map((item) => <span key={item}>{item}<b aria-hidden="true">✦</b></span>)}
            </span>
          ))}
        </div>
      </div>

      <VehiclesSection />
      <AdvantagesSection />
      <CTASection />
    </main>
  );
}

export default Home;
