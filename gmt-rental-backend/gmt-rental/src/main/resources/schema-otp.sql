-- ============================================================
-- Migration DDL pour le système de vérification OTP par e-mail
-- Projet : GMT_rental
-- ============================================================

-- 1. Ajout de la colonne email_verified sur la table users (si absente)
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE;

-- 2. Création de la table des codes de vérification OTP
CREATE TABLE IF NOT EXISTS verification_codes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    attempts INTEGER NOT NULL DEFAULT 0
);

-- 3. Indexation pour optimiser la recherche par email et type d'OTP
CREATE INDEX IF NOT EXISTS idx_verification_email_type ON verification_codes (email, type);

