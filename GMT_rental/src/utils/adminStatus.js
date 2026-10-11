const requestStatusVariants = {
    NOUVELLE: "warning",
    EN_COURS: "info",
    TRAITEE: "neutral",
    ACCEPTEE: "success",
    REFUSEE: "danger",
    ANNULEE: "danger",
};

const requestStatusLabels = {
  NOUVELLE: "En attente",
  EN_COURS: "En cours de traitement",
  TRAITEE: "Traitée (ancienne valeur, non confirmée)",
  ACCEPTEE: "Acceptée — dates bloquées",
  REFUSEE: "Refusée",
  ANNULEE: "Annulée",
};

export function requestStatusBadgeClass(status) {
  const normalizedStatus = (status || "NOUVELLE").toUpperCase();
  return `admin-badge admin-badge-${requestStatusVariants[normalizedStatus] || "neutral"}`;
}

export function requestStatusLabel(status) {
  const normalizedStatus = (status || "NOUVELLE").toUpperCase();
  return requestStatusLabels[normalizedStatus] || `Statut à vérifier (${normalizedStatus})`;
}
