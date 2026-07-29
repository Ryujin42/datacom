-- Second operateur de demonstration, uniquement charge sous le profil dev (voir
-- application-dev.yml, spring.flyway.locations) au meme titre que V3.
--
-- Raison d'etre : RG-01 et US-08 CA-4 distinguent « un OPERATOR » de « l'auteur de la fiche ».
-- Avec un seul compte operateur, cette distinction n'est pas testable de bout en bout — le refus
-- oppose a un operateur qui n'est pas l'auteur ne peut se prouver qu'avec un second operateur.
--
-- operator2 / OperatorPass123!
-- Hache avec BCrypt cout 12 (RG-20).

INSERT INTO users (login, password_hash, firstname, lastname, role)
VALUES ('operator2', '$2b$12$c9IByhtk5ZUZaWC1kIu16Ovo5ms56TPBQdwq7qYoo.7tOJ9bsckKq',
        'Paul', 'Bernard', 'OPERATOR');
