import { useEffect, useRef, useState } from "react";
import { CheckCircle2, Mail, ShieldCheck, ArrowLeft, RefreshCw } from "lucide-react";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import { verificationApi } from "../../services/admin/api";
import "./VerifyEmail.css";

const RESEND_COOLDOWN = 60;

export default function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const initialEmail = searchParams.get("email") || "";
  const [email, setEmail] = useState(initialEmail);
  const [isEditingEmail, setIsEditingEmail] = useState(!initialEmail);

  const [digits, setDigits] = useState(["", "", "", "", "", ""]);
  const inputRefs = useRef([]);

  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [countdown, setCountdown] = useState(RESEND_COOLDOWN);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Countdown timer effect
  useEffect(() => {
    if (countdown <= 0) return;
    const interval = setInterval(() => {
      setCountdown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);
    return () => clearInterval(interval);
  }, [countdown]);

  // Focus first input on mount
  useEffect(() => {
    if (inputRefs.current[0] && !isEditingEmail) {
      inputRefs.current[0].focus();
    }
  }, [isEditingEmail]);

  const handleDigitChange = (index, value) => {
    // Keep only numbers
    const cleaned = value.replace(/\D/g, "");
    if (!cleaned) {
      const nextDigits = [...digits];
      nextDigits[index] = "";
      setDigits(nextDigits);
      return;
    }

    const nextDigits = [...digits];
    // If pasted or multi-char in one box
    if (cleaned.length > 1) {
      const chars = cleaned.slice(0, 6).split("");
      chars.forEach((c, i) => {
        if (i < 6) nextDigits[i] = c;
      });
      setDigits(nextDigits);
      const nextFocus = Math.min(chars.length, 5);
      inputRefs.current[nextFocus]?.focus();
      return;
    }

    nextDigits[index] = cleaned[0];
    setDigits(nextDigits);
    setError("");

    // Auto-advance
    if (index < 5 && cleaned[0]) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index, event) => {
    if (event.key === "Backspace") {
      if (!digits[index] && index > 0) {
        inputRefs.current[index - 1]?.focus();
      }
    } else if (event.key === "ArrowLeft" && index > 0) {
      inputRefs.current[index - 1]?.focus();
    } else if (event.key === "ArrowRight" && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handlePaste = (event) => {
    event.preventDefault();
    const pasted = event.clipboardData.getData("text").replace(/\D/g, "").slice(0, 6);
    if (!pasted) return;

    const nextDigits = [...digits];
    pasted.split("").forEach((char, idx) => {
      if (idx < 6) nextDigits[idx] = char;
    });
    setDigits(nextDigits);
    setError("");

    const targetFocus = Math.min(pasted.length, 5);
    inputRefs.current[targetFocus]?.focus();
  };

  const code = digits.join("");
  const isCodeComplete = code.length === 6;

  const handleVerify = async (event) => {
    event?.preventDefault();
    if (!email.trim()) {
      setError("Veuillez renseigner votre adresse e-mail.");
      return;
    }
    if (!isCodeComplete) {
      setError("Veuillez saisir les 6 chiffres du code de vérification.");
      return;
    }

    setLoading(true);
    setError("");
    setSuccess("");

    try {
      const response = await verificationApi.verify(email.trim(), code);
      setSuccess(response.data?.message || "Adresse e-mail vérifiée avec succès !");
      setTimeout(() => {
        navigate("/admin/login", { replace: true });
      }, 2000);
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Code de vérification invalide ou expiré.";
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (!email.trim()) {
      setError("Veuillez renseigner une adresse e-mail valide.");
      return;
    }
    if (countdown > 0 || resending) return;

    setResending(true);
    setError("");
    setSuccess("");

    try {
      const response = await verificationApi.resend(email.trim());
      setSuccess(
        response.data?.message ||
          "Un nouveau code de vérification à 6 chiffres a été envoyé par e-mail."
      );
      setCountdown(RESEND_COOLDOWN);
      setDigits(["", "", "", "", "", ""]);
      inputRefs.current[0]?.focus();
    } catch (err) {
      const message =
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Impossible de renvoyer le code. Veuillez réessayer plus tard.";
      setError(message);
    } finally {
      setResending(false);
    }
  };

  return (
    <main className="verify-email-page">
      <div className="verify-email-card">
        <div className="verify-email-mark">
          {success ? <CheckCircle2 size={28} color="var(--btn-ink)" /> : <ShieldCheck size={28} />}
        </div>

        <p className="verify-email-eyebrow">GMT RENTAL · SÉCURITÉ</p>
        <h1>Vérifiez votre adresse email</h1>

        {!isEditingEmail && email ? (
          <p className="verify-email-copy">
            Un code de confirmation à 6 chiffres a été envoyé à :<br />
            <strong className="verify-email-target">{email}</strong>
            <button
              type="button"
              className="verify-email-change-btn"
              onClick={() => setIsEditingEmail(true)}
            >
              Modifier
            </button>
          </p>
        ) : (
          <div className="verify-email-edit-email">
            <label htmlFor="verify-email-input">Adresse e-mail associée au compte :</label>
            <input
              id="verify-email-input"
              type="email"
              value={email}
              placeholder="nom@exemple.com"
              onChange={(e) => setEmail(e.target.value)}
              onBlur={() => email && setIsEditingEmail(false)}
            />
          </div>
        )}

        {error && (
          <div className="verify-alert error" role="alert">
            {error}
          </div>
        )}

        {success && (
          <div className="verify-alert success" role="status">
            {success}
          </div>
        )}

        <form onSubmit={handleVerify}>
          <div className="verify-otp-inputs" onPaste={handlePaste}>
            {digits.map((digit, idx) => (
              <input
                key={idx}
                ref={(el) => (inputRefs.current[idx] = el)}
                type="text"
                inputMode="numeric"
                pattern="[0-9]*"
                maxLength={1}
                value={digit}
                disabled={loading || Boolean(success)}
                className={`verify-otp-digit ${digit ? "has-value" : ""}`}
                onChange={(e) => handleDigitChange(idx, e.target.value)}
                onKeyDown={(e) => handleKeyDown(idx, e)}
                autoComplete="one-time-code"
                aria-label={`Chiffre ${idx + 1}`}
              />
            ))}
          </div>

          <div className="verify-email-actions">
            <button
              type="submit"
              className="verify-submit-button"
              disabled={!isCodeComplete || loading || Boolean(success)}
            >
              {loading ? (
                <>
                  <span className="verify-spinner" />
                  Vérification en cours…
                </>
              ) : (
                "Vérifier mon email"
              )}
            </button>

            <button
              type="button"
              className="verify-resend-button"
              disabled={countdown > 0 || resending || loading}
              onClick={handleResend}
            >
              {resending ? (
                <>
                  <RefreshCw size={14} className="verify-spinner" />
                  Envoi du code…
                </>
              ) : countdown > 0 ? (
                `Renvoyer le code (${countdown}s)`
              ) : (
                <>
                  <Mail size={14} />
                  Renvoyer un nouveau code
                </>
              )}
            </button>
          </div>
        </form>

        <div className="verify-email-footer">
          <Link to="/admin/login">
            <ArrowLeft size={13} style={{ verticalAlign: "middle", marginRight: 4 }} />
            Retour à la page de connexion
          </Link>
        </div>
      </div>
    </main>
  );
}

