import { useContext } from "react";
import CurrencyContext from "./CurrencyContext";

function useCurrency() {
  const context = useContext(CurrencyContext);

  if (!context) {
    throw new Error("useCurrency doit être utilisé dans CurrencyProvider.");
  }

  return context;
}

export default useCurrency;
