import { useEffect, useState } from "react";
import CurrencyContext from "./CurrencyContext";
import { publicApi } from "../services/api";

function CurrencyProvider({ children }) {
  const [currency, setCurrency] = useState(() => {
    return localStorage.getItem("currency") || "DT";
  });
  const [eurToDtRate, setEurToDtRate] = useState(null);
  const [rateError, setRateError] = useState("");

  useEffect(() => {
    let active = true;
    publicApi.get("/parametres")
      .then(({ data }) => {
        const rate = Number(data?.tauxEurDt);
        if (!Number.isFinite(rate) || rate <= 0) throw new Error("Le taux de change reçu est invalide.");
        if (active) {
          setEurToDtRate(rate);
          setRateError("");
        }
      })
      .catch(() => {
        if (active) setRateError("Le taux EUR/DT est indisponible.");
      });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    localStorage.setItem("currency", currency);
  }, [currency]);

  const toggleCurrency = () => {
    setCurrency((current) =>
      current === "DT" ? "EUR" : "DT"
    );
  };

  const getPrice = (priceDT) => {
    if (currency === "DT") return priceDT;
    if (!eurToDtRate) return null;
    return Number(priceDT) / eurToDtRate;
  };

  const currencySymbol = currency === "DT" ? "DT" : "€";

  return (
    <CurrencyContext.Provider
      value={{
        currency,
        setCurrency,
        toggleCurrency,
        getPrice,
        currencySymbol,
        eurToDtRate,
        rateError
      }}
    >
      {children}
    </CurrencyContext.Provider>
  );
}

export default CurrencyProvider;
