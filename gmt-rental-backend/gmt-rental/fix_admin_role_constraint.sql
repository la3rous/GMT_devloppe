-- Allow the bootstrap admin account while preserving existing role values.
ALTER TABLE users DROP CONSTRAINT IF EXISTS check_role;

ALTER TABLE users
    ADD CONSTRAINT check_role
    CHECK (role IN ('CLIENT', 'USER', 'ADMIN', 'ROLE_ADMIN')) NOT VALID;
