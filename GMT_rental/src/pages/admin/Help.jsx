import { useEffect, useState } from "react";
import { ChevronDown, CircleHelp, LifeBuoy, Send } from "lucide-react";
import { adminApiService } from "../../services/admin/api";

const faqs = [
  { q: "Comment ajouter un véhicule ?", a: "Ouvrez Véhicules dans la barre latérale, puis utilisez l’action d’ajout. Renseignez les caractéristiques et enregistrez le formulaire." },
  { q: "Comment modifier les informations d’un véhicule ?", a: "Dans Véhicules, ouvrez la fiche du véhicule concerné avec son action de modification, puis enregistrez les changements." },
  { q: "Comment gérer les catégories de véhicules ?", a: "La page Catégories permet de créer, modifier et désactiver les catégories utilisées dans les fiches véhicule." },
  { q: "Comment consulter et traiter les demandes de devis ?", a: "Ouvrez Demandes de devis depuis la barre latérale. Vous pouvez consulter une demande et faire évoluer son statut depuis sa fiche." },
  { q: "Comment ajouter un administrateur ?", a: "Dans Utilisateurs, créez un compte avec le rôle administrateur. Le nouvel utilisateur doit vérifier son adresse e-mail avant de se connecter." },
  { q: "Comment modifier mes informations personnelles ?", a: "Ouvrez Mon profil depuis le menu en haut à droite. Le nom et le prénom sont modifiables ; le changement d’adresse e-mail nécessite un code envoyé à la nouvelle adresse." },
  { q: "Comment changer mon mot de passe ?", a: "Ouvrez Sécurité et mot de passe. Saisissez le mot de passe actuel, puis le nouveau deux fois. Le serveur vérifie l’ancien mot de passe et stocke uniquement son empreinte." },
  { q: "Comment consulter l’historique d’activité ?", a: "Ouvrez Historique d’activité depuis le menu de profil. Les connexions et opérations administratives prises en charge y sont consignées côté serveur." },
  { q: "Que faire si je rencontre un problème de connexion ?", a: "Vérifiez que le back et sa base de données sont démarrés, que votre compte administrateur est actif et que son adresse e-mail est vérifiée. Si le problème persiste, envoyez une demande d’assistance ci-dessous." },
];

export default function AdminHelp() {
  const [profile, setProfile] = useState(null);
  const [supportRequests, setSupportRequests] = useState([]);
  const [openFaq, setOpenFaq] = useState(0);
  const [form, setForm] = useState({ name: "", email: "", subject: "", category: "Technique", description: "" });
  const [sending, setSending] = useState(false);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    Promise.all([adminApiService.profile(), adminApiService.mySupportRequests()]).then(([profileResult, requestsResult]) => {
      const user = profileResult.data;
      setProfile(user);
      setSupportRequests(requestsResult.data || []);
      setForm((current) => ({ ...current, name: [user.prenom, user.nom].filter(Boolean).join(" "), email: user.email }));
    }).catch(() => setMessage({ type: "error", text: "Impossible de charger les informations de l’assistance." }));
  }, []);

  const submit = async (event) => {
    event.preventDefault(); setSending(true); setMessage(null);
    try {
      const { data } = await adminApiService.submitSupportRequest(form);
      setMessage({ type: data.emailStatus === "ACCEPTED" ? "success" : "error", text: `${data.message} Demande ${data.id} enregistrée le ${new Intl.DateTimeFormat("fr-FR", { dateStyle: "short", timeStyle: "short" }).format(new Date(data.createdAt))}.` });
      setForm((current) => ({ ...current, subject: "", description: "" }));
      const requests = await adminApiService.mySupportRequests(); setSupportRequests(requests.data || []);
    } catch (error) { setMessage({ type: "error", text: error.response?.data?.message || "La demande n’a pas pu être enregistrée." }); }
    finally { setSending(false); }
  };

  return <section className="account-page">
    <div className="admin-page-head"><div><p className="account-eyebrow">CENTRE DE RESSOURCES</p><h1>Aide et assistance</h1><p>Guides pratiques et formulaire de contact pour l’équipe d’administration.</p></div></div>
    <div className="admin-card help-card">
      <div className="account-section-heading"><span className="account-section-icon"><CircleHelp size={19} /></span><div><h2>Questions fréquentes</h2><p>Réponses fondées sur les fonctions disponibles dans Gestion Rental.</p></div></div>
      <div className="help-faq-list">{faqs.map((item, index) => <div className={`help-faq-item${openFaq === index ? " is-open" : ""}`} key={item.q}>
        <button type="button" aria-expanded={openFaq === index} onClick={() => setOpenFaq(openFaq === index ? -1 : index)}><span>{item.q}</span><ChevronDown size={17} aria-hidden="true" /></button>
        {openFaq === index && <p>{item.a}</p>}
      </div>)}</div>
    </div>
    <div className="account-columns help-columns">
      <form className="admin-card account-form" onSubmit={submit}>
        <div className="account-section-heading"><span className="account-section-icon"><LifeBuoy size={19} /></span><div><h2>Contacter l’assistance</h2><p>Votre demande sera enregistrée pour le suivi.</p></div></div>
        {message && <p className={message.type === "success" ? "admin-success" : "admin-error"} role={message.type === "success" ? "status" : "alert"}>{message.text}</p>}
        <div className="admin-form"><label className="admin-field"><span>Nom</span><input required maxLength="160" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
          <label className="admin-field"><span>Adresse e-mail</span><input required type="email" value={form.email} readOnly /></label>
          <label className="admin-field"><span>Sujet</span><input required maxLength="180" value={form.subject} onChange={(event) => setForm({ ...form, subject: event.target.value })} /></label>
          <label className="admin-field"><span>Catégorie</span><select value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })}><option>Compte</option><option>Véhicule</option><option>Devis</option><option>Technique</option><option>Autre</option></select></label>
          <label className="admin-field full"><span>Description détaillée (20 caractères minimum)</span><textarea required minLength="20" maxLength="5000" rows="5" value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
        </div>
        <div className="account-form-actions"><button className="admin-button primary" disabled={sending || !profile}><Send size={15} />{sending ? "Enregistrement…" : "Envoyer la demande"}</button></div>
      </form>
    </div>
    <div className="admin-card help-requests-card"><div className="account-section-heading"><span className="account-section-icon"><LifeBuoy size={19} /></span><div><h2>Mes demandes d’assistance</h2><p>Demandes enregistrées pour ce compte.</p></div></div>
      {supportRequests.length ? <div className="help-request-list">{supportRequests.map((request) => <article key={request.id}><div><strong>{request.subject}</strong><span>{request.category} · {new Intl.DateTimeFormat("fr-FR", { dateStyle: "short", timeStyle: "short" }).format(new Date(request.createdAt))}</span></div><p>{request.description}</p></article>)}</div> : <p className="account-empty">Aucune demande d’assistance enregistrée.</p>}
    </div>
  </section>;
}
