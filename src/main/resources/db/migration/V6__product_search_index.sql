-- US-14 : recherche sur reference, nom et fabricant, insensible a la casse ET aux accents (CA-2),
-- adossee a un index, sans parcours sequentiel complet (CA-3, ECO-03).
--
-- Trois obstacles, trois reponses :
--
-- 1. Insensibilite aux accents : l'extension unaccent translittere « Crème » en « Creme ».
-- 2. unaccent() est declaree STABLE et non IMMUTABLE — son resultat depend d'un dictionnaire qui
--    pourrait etre modifie — ce qui interdit de l'utiliser dans un index. On l'enveloppe donc dans
--    une fonction IMMUTABLE : le dictionnaire n'est jamais modifie ici, l'hypothese est vraie pour
--    cette application, et elle est ecrite noir sur blanc plutot que subie.
-- 3. Une recherche « contient » (%terme%) ne peut pas utiliser un index B-tree. pg_trgm indexe les
--    trigrammes et rend precisement ce motif indexable.
--
-- Le texte cherche est materialise dans une colonne generee plutot que calcule dans un index
-- d'expression : la requete porte alors directement sur la colonne, sans avoir a reproduire
-- l'expression au caractere pres. Une expression qui differerait — ne serait-ce que par la facon
-- dont l'ORM traduit une concatenation — ferait silencieusement retomber Postgres sur un parcours
-- sequentiel, exactement ce que CA-3 interdit, et sans aucun signal visible.

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

-- L'index plein texte de V1 visait un autre usage (mots entiers, sans les accents ni la reference)
-- et n'est utilise par aucune requete de l'application : le garder couterait une ecriture a chaque
-- enregistrement sans jamais servir une lecture (ECO-16).
DROP INDEX IF EXISTS idx_products_search;

-- US-12 : la liste du validateur est triee par date de mise a jour, toutes fiches confondues.
-- L'index de V1 sur (created_by, updated_at DESC) ne sert que le cas de l'operateur.
CREATE INDEX idx_products_updated_at ON products (updated_at DESC);

-- US-16 : le validateur consulte ses seules decisions, de la plus recente a la plus ancienne.
CREATE INDEX idx_audit_user ON audit_entry (user_id, occurred_at DESC);
