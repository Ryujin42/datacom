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

ALTER TABLE products
    ALTER COLUMN country TYPE VARCHAR(2);
