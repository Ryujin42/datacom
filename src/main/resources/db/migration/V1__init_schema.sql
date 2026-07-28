-- Schema cible defini en SPECIFICATIONS.md §10.
-- Aucune modification manuelle du schema : toute evolution passe par une nouvelle migration (TEC-05).

CREATE TYPE product_status AS ENUM ('DRAFT', 'IN_REVIEW', 'VALIDATED');

CREATE TABLE users
(
    id              BIGSERIAL PRIMARY KEY,
    login           VARCHAR(64)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    firstname       VARCHAR(100) NOT NULL,
    lastname        VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('OPERATOR', 'VALIDATOR')),
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_attempts SMALLINT     NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
-- Comptes crees exclusivement par migration Flyway (D2) : aucun ecran de gestion en v1.

CREATE TABLE products
(
    id              BIGSERIAL PRIMARY KEY,
    reference       VARCHAR(32),
    name            VARCHAR(150),
    description     VARCHAR(2000),
    category        VARCHAR(80),
    subcategory     VARCHAR(80),
    manufacturer    VARCHAR(150),
    country         CHAR(2),
    lot_number      VARCHAR(50),
    certification   VARCHAR(100),
    author_comment  VARCHAR(1000),
    status          product_status NOT NULL DEFAULT 'DRAFT',
    current_step    SMALLINT       NOT NULL DEFAULT 1 CHECK (current_step BETWEEN 1 AND 4),
    created_by      BIGINT         NOT NULL REFERENCES users (id),
    validated_by    BIGINT         REFERENCES users (id),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    submitted_at    TIMESTAMPTZ,
    validated_at    TIMESTAMPTZ,
    version         BIGINT         NOT NULL DEFAULT 0
);

-- RG-11 : unicite globale de la reference (aucun etat ne "libere" une reference)
CREATE UNIQUE INDEX uq_products_reference
    ON products (reference) WHERE reference IS NOT NULL;

CREATE TABLE audit_entry
(
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT      NOT NULL REFERENCES products (id),
    user_id     BIGINT      NOT NULL REFERENCES users (id),
    action      VARCHAR(32) NOT NULL,
    from_status product_status,
    to_status   product_status,
    comment     VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_created_by ON products (created_by, updated_at DESC);
CREATE INDEX idx_products_search ON products USING gin (
    to_tsvector('simple', coalesce(name, '') || ' ' || coalesce(manufacturer, ''))
);
CREATE INDEX idx_audit_product ON audit_entry (product_id, occurred_at DESC);
