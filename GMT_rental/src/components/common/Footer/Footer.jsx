import { Link } from "react-router-dom";

import {
  Car,
  MapPin,
  Phone,
  Mail,
  
} from "lucide-react";

import "./Footer.css";

function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="gmt-footer">

      <div className="footer-container">

        <FooterBrand />

        <FooterNav />

        <FooterContact />

        <FooterHours />

      </div>

      <FooterBottom year={currentYear} />

    </footer>
  );
}


/* =========================
   BRAND
========================= */

function FooterBrand() {
  return (
    <div className="footer-brand">

      <Link
        to="/"
        className="footer-logo"
      >

        <span className="footer-logo-icon">
          <Car size={22} />
        </span>

        <span>GMT</span>

      </Link>

      <p>
        L'art du voyage tunisien.
        Location de véhicules premium
        pour explorer la Tunisie avec
        élégance et sérénité.
      </p>

      <div className="footer-socials">

        <a
          href="https://www.facebook.com/"
          target="_blank"
          rel="noreferrer"
          aria-label="Facebook"
        >
          Facebook
        </a>

        <a
          href="https://www.instagram.com/"
          target="_blank"
          rel="noreferrer"
          aria-label="Instagram"
        >
          Instagram
        </a>

        <a
          href="https://www.linkedin.com/"
          target="_blank"
          rel="noreferrer"
          aria-label="LinkedIn"
        >
          LinkedIn
        </a>

      </div>

    </div>
  );
}


/* =========================
   NAVIGATION
========================= */

function FooterNav() {
  return (
    <div className="footer-column">

      <h3>NAVIGATION</h3>

      <Link to="/">
        Accueil
      </Link>

      <Link to="/catalogue">
        Catalogue véhicules
      </Link>

      <Link to="/about">
        À propos
      </Link>

      <Link to="/faq">
        FAQ
      </Link>

      <Link to="/devis">
        Demander un devis
      </Link>

      <Link to="/contact">
        Contact
      </Link>

    </div>
  );
}


/* =========================
   CONTACT
========================= */

function FooterContact() {
  return (
    <div className="footer-column">

      <h3>CONTACT</h3>

      <div className="footer-contact-item">

        <MapPin size={19} />

        <span>
          Avenue Habib Bourguiba,
          Tunis, Tunisie
        </span>

      </div>

      <a
        href="tel:+21671000000"
        className="footer-contact-item"
      >

        <Phone size={19} />

        <span>
          +216 71 000 000
        </span>

      </a>

      <a
        href="mailto:contact@gmt.tn"
        className="footer-contact-item"
      >

        <Mail size={19} />

        <span>
          contact@gmt.tn
        </span>

      </a>

    </div>
  );
}


/* =========================
   HORAIRES
========================= */

function FooterHours() {
  return (
    <div className="footer-column">

      <h3>HORAIRES</h3>

      <div className="footer-hours-row">
        <span>Lun — Ven</span>
        <span>08h — 19h</span>
      </div>

      <div className="footer-hours-row">
        <span>Samedi</span>
        <span>09h — 17h</span>
      </div>

      <div className="footer-hours-row">
        <span>Dimanche</span>
        <span>Sur RDV</span>
      </div>

    </div>
  );
}


/* =========================
   BOTTOM
========================= */

function FooterBottom({ year }) {
  return (
    <div className="footer-bottom">

      <p>
        © {year} GMT. Tous droits réservés.
      </p>

      <div className="footer-legal-links">

        <Link to="/mentions-legales">
          Mentions légales
        </Link>

        <Link to="/confidentialite">
          Confidentialité
        </Link>

        <Link to="/cgu">
          CGU
        </Link>

      </div>

    </div>
  );
}

export default Footer;