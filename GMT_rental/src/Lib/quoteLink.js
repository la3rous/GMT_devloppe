export function buildQuoteUrl({
  vehicule,
  dateDebut,
  dateFin,
  lieuPrise,
  lieuRestitution,
  personnes,
  categorie,
  etape,
} = {}) {
  const params = new URLSearchParams();

  if (vehicule) {
    params.set("vehicule", vehicule);
  }

  if (dateDebut) {
    params.set("dateDebut", dateDebut);
  }

  if (dateFin) {
    params.set("dateFin", dateFin);
  }

  if (lieuPrise) {
    params.set("lieuPrise", lieuPrise);
  }

  if (lieuRestitution) {
    params.set("lieuRestitution", lieuRestitution);
  }

  if (personnes) {
    params.set("personnes", personnes);
  }

  if (categorie) {
    params.set("categorie", categorie);
  }

  if (etape !== undefined && etape !== null) {
    params.set("etape", etape);
  }

  const query = params.toString();

  return query ? `/devis?${query}` : "/devis";
}