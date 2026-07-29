-- Aligne les types de colonnes du schema cible sur ce que le mapping JPA standard sait exprimer,
-- sans rien relacher des contraintes appliquees par la base.
--
-- 1. product_status : ENUM natif -> VARCHAR + CHECK
-- ------------------------------------------------------------------------------------------------
-- Aligne products.status et audit_entry.from_status/to_status sur le traitement deja retenu pour
-- users.role (V1).
--
-- Deux raisons :
--  1. Un enum natif Postgres n'est mappable qu'avec une annotation propriétaire Hibernate
--     (@JdbcTypeCode(NAMED_ENUM)) portee par l'entite, ce qui ferait dependre le domaine du
--     framework de persistance (interdit par TEC-01, verifie par ArchitectureTest). Avec VARCHAR,
--     le mapping tient en @Enumerated(EnumType.STRING), qui est du JPA standard.
--  2. Faire evoluer un enum natif est couteux : ALTER TYPE ... ADD VALUE ne s'execute pas dans une
--     transaction, donc pas dans une migration Flyway transactionnelle. Une contrainte CHECK se
--     modifie normalement.
--
-- La contrainte reste appliquee par la base : une valeur hors des trois etats de RG-03 est refusee.

ALTER TABLE products
    ALTER COLUMN status DROP DEFAULT,
    ALTER COLUMN status TYPE VARCHAR(20) USING status::TEXT,
    ALTER COLUMN status SET DEFAULT 'DRAFT';

ALTER TABLE products
    ADD CONSTRAINT ck_products_status CHECK (status IN ('DRAFT', 'IN_REVIEW', 'VALIDATED'));

ALTER TABLE audit_entry
    ALTER COLUMN from_status TYPE VARCHAR(20) USING from_status::TEXT,
    ALTER COLUMN to_status TYPE VARCHAR(20) USING to_status::TEXT;

ALTER TABLE audit_entry
    ADD CONSTRAINT ck_audit_entry_from_status
        CHECK (from_status IS NULL OR from_status IN ('DRAFT', 'IN_REVIEW', 'VALIDATED')),
    ADD CONSTRAINT ck_audit_entry_to_status
        CHECK (to_status IS NULL OR to_status IN ('DRAFT', 'IN_REVIEW', 'VALIDATED'));

DROP TYPE product_status;

-- 2. products.country : CHAR(2) -> VARCHAR(2)
-- ------------------------------------------------------------------------------------------------
-- CHAR complete la valeur avec des espaces jusqu'a la longueur declaree, ce qui n'a aucun interet
-- pour un code ISO 3166-1 alpha-2 et rend les comparaisons dependantes de ce remplissage. VARCHAR(2)
-- borne la longueur de la meme facon sans ce comportement, et correspond au type qu'un mapping JPA
-- standard exprime naturellement. La liste fermee de RG-13 reste validee cote applicatif.

ALTER TABLE products
    ALTER COLUMN country TYPE VARCHAR(2);
