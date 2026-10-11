import { useState } from "react";

import {
  MapPin,
  Phone,
  Mail,
  Clock,
  Send,
  MessageCircle,
  Languages,
  LoaderCircle,
  CheckCircle,
} from "lucide-react";

import "./Contact.css";

function Contact() {
  const [formData, setFormData] = useState({
    nom: "",
    email: "",
    telephone: "",
    sujet: "",
    message: "",
  });

  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));

    setErrors((prev) => ({
      ...prev,
      [name]: "",
    }));
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.nom.trim()) {
      newErrors.nom = "Le nom complet est obligatoire.";
    }

    if (!formData.email.trim()) {
      newErrors.email = "L'email est obligatoire.";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = "Veuillez saisir une adresse email valide.";
    }

    if (!formData.sujet) {
      newErrors.sujet = "Veuillez sélectionner un sujet.";
    }

    if (!formData.message.trim()) {
      newErrors.message = "Le message est obligatoire.";
    }

    setErrors(newErrors);

    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (event) => {
    event.preventDefault();

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    /*
      Pour le moment :
      simulation de l'envoi.

      Plus tard :
      React → Spring Boot → PostgreSQL
      + notification email administrateur
    */

    setTimeout(() => {
      setLoading(false);
      setSuccess(true);
    }, 1500);
  };

  const resetForm = () => {
    setFormData({
      nom: "",
      email: "",
      telephone: "",
      sujet: "",
      message: "",
    });

    setErrors({});
    setSuccess(false);
  };

  return (
    <main className="contact-page">

      {/* =========================
          HERO
      ========================= */}

      <section className="contact-hero">

        <span className="contact-badge">
          CONTACT
        </span>

        <h1>
          Parlons de votre
          <br />
          voyage
        </h1>

        <p>
          Une question, une demande spécifique ou un devis personnalisé ?
          Notre équipe est à votre écoute.
        </p>

      </section>


      {/* =========================
          CONTENU
      ========================= */}

      <section className="contact-grid">

        {/* =========================
            COLONNE GAUCHE
        ========================= */}

        <div className="contact-info">

          <ContactInfoCard
            icon={<MapPin size={27} />}
            label="ADRESSE"
            value="Avenue Habib Bourguiba, Tunis, Tunisie"
            description="Notre équipe vous accueille au centre de Tunis."
          />

          <ContactInfoCard
            icon={<Phone size={27} />}
            label="TÉLÉPHONE"
            value={
              <a href="tel:+21671000000">
                +216 71 000 000
              </a>
            }
            description="Du lundi au vendredi, de 08h à 19h."
          />

          <ContactInfoCard
            icon={<Mail size={27} />}
            label="EMAIL"
            value={
              <a href="mailto:contact@gmt.tn">
                contact@gmt.tn
              </a>
            }
            description="Réponse sous 24h ouvrées."
          />

          <ContactInfoCard
            icon={<Clock size={27} />}
            label="HORAIRES"
            value="Lun — Ven : 08h — 19h"
            description="Sam : 09h — 14h · Dimanche : Sur RDV"
          />


          {/* =========================
              MAP
          ========================= */}

          <div className="contact-map">

            <iframe
              title="Localisation GMT à Tunis"
              src="https://www.openstreetmap.org/export/embed.html?bbox=10.15%2C36.79%2C10.20%2C36.82&layer=mapnik&marker=36.8065%2C10.1815"
              loading="lazy"
            />

          </div>


          {/* =========================
              SOCIAL
          ========================= */}

          <div className="contact-socials">

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


        {/* =========================
            FORMULAIRE
        ========================= */}

        <div className="contact-form-card">

          {!success ? (

            <form onSubmit={handleSubmit}>

              {/* NOM + EMAIL */}

              <div className="form-row">

                <FormInput
                  label="Nom complet"
                  name="nom"
                  placeholder="Votre nom"
                  value={formData.nom}
                  onChange={handleChange}
                  error={errors.nom}
                />

                <FormInput
                  label="Email"
                  name="email"
                  type="email"
                  placeholder="vous@email.com"
                  value={formData.email}
                  onChange={handleChange}
                  error={errors.email}
                />

              </div>


              {/* TELEPHONE */}

              <div className="form-group">

                <label htmlFor="telephone">
                  Téléphone <span>(optionnel)</span>
                </label>

                <input
                  id="telephone"
                  name="telephone"
                  type="tel"
                  placeholder="+216"
                  value={formData.telephone}
                  onChange={handleChange}
                />

              </div>


              {/* SUJET */}

              <div className="form-group">

                <label htmlFor="sujet">
                  Sujet
                </label>

                <select
                  id="sujet"
                  name="sujet"
                  value={formData.sujet}
                  onChange={handleChange}
                >

                  <option value="">
                    Sélectionnez un sujet
                  </option>

                  <option value="Demande de devis">
                    Demande de devis
                  </option>

                  <option value="Question sur un véhicule">
                    Question sur un véhicule
                  </option>

                  <option value="Location longue durée">
                    Location longue durée
                  </option>

                  <option value="Partenariat">
                    Partenariat
                  </option>

                  <option value="Autre">
                    Autre
                  </option>

                </select>

                {errors.sujet && (
                  <small>{errors.sujet}</small>
                )}

              </div>


              {/* MESSAGE */}

              <div className="form-group">

                <label htmlFor="message">
                  Message
                </label>

                <textarea
                  id="message"
                  name="message"
                  rows="6"
                  placeholder="Votre message..."
                  value={formData.message}
                  onChange={handleChange}
                />

                {errors.message && (
                  <small>{errors.message}</small>
                )}

              </div>


              {/* BUTTON */}

              <button
                type="submit"
                className="contact-submit"
                disabled={loading}
              >

                {loading ? (
                  <>
                    <LoaderCircle
                      size={20}
                      className="loading-icon"
                    />

                    Envoi en cours...
                  </>
                ) : (
                  <>
                    Envoyer le message
                    <Send size={20} />
                  </>
                )}

              </button>

            </form>

          ) : (

            /* =========================
               SUCCESS
            ========================= */

            <div className="contact-success">

              <div className="success-icon">
                <CheckCircle size={42} />
              </div>

              <h2>
                Message envoyé !
              </h2>

              <p>
                Nous vous répondrons dans les plus brefs délais.
              </p>

              <button
                className="reset-button"
                onClick={resetForm}
              >
                Envoyer un autre message
              </button>

            </div>

          )}

        </div>

      </section>


      {/* =========================
          RÉASSURANCE
      ========================= */}

      <section className="contact-reassurance">

        <div className="reassurance-card">
          <Clock />
          <span>Réponse sous 24h</span>
        </div>

        <div className="reassurance-card">
          <MessageCircle />
          <span>Conseils personnalisés gratuits</span>
        </div>

        <div className="reassurance-card">
          <Languages />
          <span>Équipe francophone, arabe et anglaise</span>
        </div>

      </section>

    </main>
  );
}


/* =========================
   CONTACT INFO CARD
========================= */

function ContactInfoCard({
  icon,
  label,
  value,
  description,
}) {
  return (
    <div className="contact-info-card">

      <div className="contact-info-icon">
        {icon}
      </div>

      <div className="contact-info-content">

        <span className="contact-info-label">
          {label}
        </span>

        <div className="contact-info-value">
          {value}
        </div>

        <p>
          {description}
        </p>

      </div>

    </div>
  );
}


/* =========================
   INPUT
========================= */

function FormInput({
  label,
  name,
  type = "text",
  placeholder,
  value,
  onChange,
  error,
}) {
  return (
    <div className="form-group">

      <label htmlFor={name}>
        {label}
      </label>

      <input
        id={name}
        name={name}
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
      />

      {error && (
        <small>{error}</small>
      )}

    </div>
  );
}

export default Contact;