import { useMemo, useState } from "react";
import {
  HelpCircle,
  ChevronDown,
  Search,
  MessageCircle,
  FileText,
} from "lucide-react";

import { Link } from "react-router-dom";
import "./FAQ.css";

const faqData = [
  {
    id: 1,
    categorie: "RÉSERVATION & DEVIS",
    question: "Comment demander un devis ?",
    reponse:
      "Rendez-vous sur la page « Demander un devis », remplissez le formulaire en 3 étapes (dates, véhicule, contact) et envoyez votre demande. Notre équipe vous répond sous 24h avec une proposition personnalisée.",
  },
  {
    id: 2,
    categorie: "RÉSERVATION & DEVIS",
    question: "Y a-t-il un paiement en ligne ?",
    reponse:
      "Non. Le règlement s'effectue sur place au moment de la prise en charge du véhicule, par carte bancaire ou en espèces.",
  },
  {
    id: 3,
    categorie: "RÉSERVATION & DEVIS",
    question: "Puis-je annuler ma demande ?",
    reponse:
      "Oui, vous pouvez annuler votre demande sans frais tant qu'aucun contrat de location n'a été signé.",
  },
  {
    id: 4,
    categorie: "RÉSERVATION & DEVIS",
    question: "Les prix sont-ils en DT et en € ?",
    reponse:
      "Oui. Le catalogue permet de consulter les tarifs en dinars tunisiens et en euros grâce au sélecteur de devise.",
  },
  {
    id: 5,
    categorie: "LOCATION & CONDITIONS",
    question: "Quels documents sont nécessaires pour louer un véhicule ?",
    reponse:
      "Vous devez présenter un permis de conduire valide depuis au moins 2 ans, une pièce d'identité ou un passeport, ainsi qu'une carte bancaire au nom du conducteur.",
  },
  {
    id: 6,
    categorie: "LOCATION & CONDITIONS",
    question: "Quel est l'âge minimum ?",
    reponse:
      "L'âge minimum est de 21 ans avec un permis obtenu depuis au moins 2 ans. Une majoration peut être appliquée pour les conducteurs de moins de 25 ans.",
  },
  {
    id: 7,
    categorie: "LOCATION & CONDITIONS",
    question: "L'assurance est-elle incluse ?",
    reponse:
      "Oui, une assurance tous risques est incluse. Des options complémentaires peuvent être proposées, notamment pour un conducteur additionnel ou une franchise réduite.",
  },
  {
    id: 8,
    categorie: "LOCATION & CONDITIONS",
    question: "Y a-t-il une limitation de kilométrage ?",
    reponse:
      "Le kilométrage illimité peut être proposé sur demande. Dans les autres cas, un forfait journalier standard s'applique.",
  },
  {
    id: 9,
    categorie: "LOCATION & CONDITIONS",
    question: "Puis-je récupérer le véhicule à l'aéroport ?",
    reponse:
      "Oui. La prise en charge peut être organisée à l'aéroport Tunis-Carthage, dans un hôtel ou à toute autre adresse en Tunisie.",
  },
  {
    id: 10,
    categorie: "LOCATION & CONDITIONS",
    question: "Que faire en cas de panne ou d'accident ?",
    reponse:
      "Une assistance est disponible 24h/24 et 7j/7. Le numéro d'assistance vous est communiqué avec votre contrat de location.",
  },
];

function FAQ() {
  const [openId, setOpenId] = useState(1);
  const [search, setSearch] = useState("");

  const filteredFaqs = useMemo(() => {
    const value = search.toLowerCase().trim();

    if (!value) {
      return faqData;
    }

    return faqData.filter(
      (faq) =>
        faq.question.toLowerCase().includes(value) ||
        faq.reponse.toLowerCase().includes(value)
    );
  }, [search]);

  const reservationFaqs = filteredFaqs.filter(
    (faq) => faq.categorie === "RÉSERVATION & DEVIS"
  );

  const locationFaqs = filteredFaqs.filter(
    (faq) => faq.categorie === "LOCATION & CONDITIONS"
  );

  const toggleFaq = (id) => {
    setOpenId((currentId) => (currentId === id ? null : id));
  };

  return (
    <main className="faq-page">

      {/* HERO */}
      <section className="faq-hero">

        <div className="faq-badge">
          <HelpCircle size={18} />
          <span>Centre d'aide</span>
        </div>

        <h1>
          Questions
          <br />
          fréquentes
        </h1>

        <p>
          Tout ce que vous devez savoir avant de louer un véhicule chez GMT.
        </p>

      </section>

      {/* FAQ */}
      <section className="faq-section">

        {/* SEARCH */}
        <div className="faq-search-wrapper">

          <Search size={20} />

          <input
            type="text"
            placeholder="Rechercher une question..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />

          {search && (
            <button
              type="button"
              className="faq-clear"
              onClick={() => setSearch("")}
            >
              ×
            </button>
          )}

        </div>

        <div className="faq-result-count">
          {filteredFaqs.length}{" "}
          {filteredFaqs.length > 1 ? "questions" : "question"}
        </div>

        {/* EMPTY STATE */}
        {filteredFaqs.length === 0 && (
          <div className="faq-empty">

            <div className="faq-empty-icon">
              <Search size={26} />
            </div>

            <h2>
              Aucun résultat pour « {search} »
            </h2>

            <p>
              Nous n'avons pas trouvé de réponse correspondant à votre
              recherche.
            </p>

            <Link to="/contact" className="faq-empty-link">
              Contactez-nous
            </Link>

          </div>
        )}

        {/* RESERVATION */}
        {reservationFaqs.length > 0 && (
          <FAQCategory
            title="RÉSERVATION & DEVIS"
            faqs={reservationFaqs}
            openId={openId}
            onToggle={toggleFaq}
          />
        )}

        {/* LOCATION */}
        {locationFaqs.length > 0 && (
          <FAQCategory
            title="LOCATION & CONDITIONS"
            faqs={locationFaqs}
            openId={openId}
            onToggle={toggleFaq}
          />
        )}

      </section>

      {/* CTA */}
      <section className="faq-cta">

        <div className="faq-cta-icon">
          <MessageCircle size={28} />
        </div>

        <h2>
          Vous ne trouvez pas votre réponse ?
        </h2>

        <p>
          Notre équipe est disponible pour répondre à toutes vos questions.
        </p>

        <div className="faq-cta-buttons">

          <Link to="/contact" className="faq-btn-primary">
            Nous contacter
          </Link>

          <Link to="/devis" className="faq-btn-ghost">
            <FileText size={18} />
            Demander un devis
          </Link>

        </div>

      </section>

    </main>
  );
}

function FAQCategory({
  title,
  faqs,
  openId,
  onToggle,
}) {
  return (
    <div className="faq-category">

      <h2 className="faq-category-title">
        {title}
      </h2>

      <div className="faq-list">

        {faqs.map((faq) => {
          const isOpen = openId === faq.id;

          return (
            <div
              className={`faq-item ${isOpen ? "faq-item-open" : ""}`}
              key={faq.id}
            >

              <button
                type="button"
                className="faq-question"
                aria-expanded={isOpen}
                onClick={() => onToggle(faq.id)}
              >

                <span>
                  {faq.question}
                </span>

                <span className="faq-chevron">
                  <ChevronDown size={20} />
                </span>

              </button>

              <div
                className={`faq-answer-wrapper ${
                  isOpen ? "faq-answer-open" : ""
                }`}
              >
                <div className="faq-answer">
                  <p>{faq.reponse}</p>
                </div>
              </div>

            </div>
          );
        })}

      </div>

    </div>
  );
}

export default FAQ;