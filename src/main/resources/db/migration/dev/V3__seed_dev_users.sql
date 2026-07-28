-- Comptes de demonstration, uniquement charges quand le profil dev active ce dossier en plus
-- de db/migration (voir application-dev.yml, spring.flyway.locations). Jamais joue en
-- production : D2 (SPECIFICATIONS.md S0) veut des comptes crees par migration, pas par un
-- ecran, mais pas des identifiants de demo publics dans l'environnement reel.
--
-- operator1 / OperatorPass123!
-- validator1 / ValidatorPass123!
-- Hache avec BCrypt cout 12 (RG-20).

INSERT INTO users (login, password_hash, firstname, lastname, role)
VALUES
    ('operator1', '$2b$12$c9IByhtk5ZUZaWC1kIu16Ovo5ms56TPBQdwq7qYoo.7tOJ9bsckKq', 'Jean', 'Dupont', 'OPERATOR'),
    ('validator1', '$2b$12$Cwfc9zwN5ZFHSlLh9qhNs.zxxmVqq.nYAHyCRL7rxxsKmizfwUv.q', 'Claire', 'Martin', 'VALIDATOR');
