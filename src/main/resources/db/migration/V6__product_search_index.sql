CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE FUNCTION immutable_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT public.unaccent('public.unaccent', $1) $$;

ALTER TABLE products
    ADD COLUMN search_text TEXT GENERATED ALWAYS AS (
        immutable_unaccent(lower(
            coalesce(reference, '') || ' ' ||
            coalesce(name, '') || ' ' ||
            coalesce(manufacturer, '')
        ))
    ) STORED;

CREATE INDEX idx_products_search_trgm ON products USING gin (search_text gin_trgm_ops);

DROP INDEX IF EXISTS idx_products_search;

CREATE INDEX idx_products_updated_at ON products (updated_at DESC);

CREATE INDEX idx_audit_user ON audit_entry (user_id, occurred_at DESC);
