-- ============================================
-- INOVEXAHUB - Système de Gestion Commerciale et Point de Vente (POS)
-- Sprint 1: Schéma Relationnel SQL
-- Version: 2.0 (ProductBatch, ProductVariant, Delivery Fee)
-- Database: PostgreSQL
-- ============================================

-- Drop tables if they exist (for clean recreation)
DROP TABLE IF EXISTS credit_history CASCADE;
DROP TABLE IF EXISTS payment_receipts CASCADE;
DROP TABLE IF EXISTS document_lines CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS product_batches CASCADE;
DROP TABLE IF EXISTS product_variants CASCADE;
DROP TABLE IF EXISTS product_conditionings CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS suppliers CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS refresh_tokens CASCADE;
DROP TABLE IF EXISTS access_tokens CASCADE;
DROP TABLE IF EXISTS password_reset_tokens CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS audit_logs CASCADE;

-- Create sequences for document number generation (preserving existing state)
CREATE SEQUENCE IF NOT EXISTS seq_quote_number START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_delivery_note_number START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_invoice_number START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_receipt_number START 1 INCREMENT 1;

-- ============================================
-- TABLE: users
-- Section 2: Gestion des Utilisateurs et Droits d'Accès
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL CHECK (email = LOWER(email)),
    password VARCHAR NOT NULL, -- BCrypt hashed
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'EMPLOYEE')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Index on email for fast authentication
CREATE INDEX idx_users_email ON users(email);

-- ============================================
-- TABLE: clients
-- Section 3.4: Module de Gestion des Tiers et du Crédit
-- ============================================
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(255),
    tax_identification_number VARCHAR(50), -- Matricule fiscal
    credit_limit DECIMAL(19,3) NOT NULL DEFAULT 0.000, -- plafond_credit_autorise
    current_debt DECIMAL(19,3) NOT NULL DEFAULT 0.000, -- Dette_Actuelle
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Index on name for search
CREATE INDEX idx_clients_name ON clients(name);
-- Index on tax_identification_number for tax identification lookup
CREATE INDEX idx_clients_tax_identification_number ON clients(tax_identification_number);

-- ============================================
-- TABLE: suppliers
-- Section 3.5: Module de Gestion des Fournisseurs
-- ============================================
CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(255),
    tax_identification_number VARCHAR(50), -- Matricule fiscal
    contact_person VARCHAR(100),
    payment_terms VARCHAR(100),
    notes VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Index on name for search
CREATE INDEX idx_suppliers_name ON suppliers(name);
-- Index on tax_identification_number for tax identification lookup
CREATE INDEX idx_suppliers_tax_identification_number ON suppliers(tax_identification_number);

-- ============================================
-- TABLE: products
-- Section 3.1: Module de Gestion des Articles et du Stock
-- ============================================
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    reference VARCHAR(50) UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image TEXT, -- Base64 encoded image string
    category VARCHAR(50),
    unit_type VARCHAR(20) NOT NULL CHECK (unit_type IN ('UNITARY', 'WEIGHT', 'LENGTH', 'VOLUME')),
    base_unit VARCHAR(20), -- e.g., "m", "kg", "piece"
    stock_quantity DECIMAL(19,3) NOT NULL DEFAULT 0.000,
    average_purchase_price DECIMAL(19,3) NOT NULL DEFAULT 0.000, -- PAMP for margin calculation
    unit_price DECIMAL(19,3), -- Default unit selling price
    supplier_id BIGINT, -- Default supplier reference
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

-- Index on reference for fast POS scanning
CREATE INDEX idx_products_reference ON products(reference);
-- Index on name for predictive search
CREATE INDEX idx_products_name ON products(name);
-- Index on category for filtering
CREATE INDEX idx_products_category ON products(category);

-- ============================================
-- TABLE: product_variants
-- Section 3.1.1: Variantes Multi-SKU avec Attributs JSON
-- ============================================
CREATE TABLE product_variants (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    product_id BIGINT NOT NULL,
    sku VARCHAR(50) UNIQUE NOT NULL,
    variant_name VARCHAR(100),
    attributes TEXT, -- JSON: {"caliber": "1.5mm", "material": "copper"}
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Index on product_id for fast lookup
CREATE INDEX idx_product_variants_product_id ON product_variants(product_id);

-- ============================================
-- TABLE: product_conditionings
-- Section 3.1.2: Tarification par Conditionnement
-- ============================================
CREATE TABLE product_conditionings (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    description VARCHAR(100), -- e.g., "Rouleau 100m"
    quantity_per_unit DECIMAL(19,3),
    unit_price DECIMAL(19,3),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Index on product_id for fast lookup
CREATE INDEX idx_product_conditionings_product_id ON product_conditionings(product_id);

-- ============================================
-- TABLE: product_batches
-- Section 3.1.3: Lots d'Inventaire FIFO
-- ============================================
CREATE TABLE product_batches (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    product_id BIGINT NOT NULL,
    variant_id BIGINT,
    quantity DECIMAL(19,3) NOT NULL,
    unit_cost DECIMAL(19,3) NOT NULL, -- Purchase cost for this batch
    unit_price DECIMAL(19,3) NOT NULL, -- Selling price for this batch
    supplier_id BIGINT,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT chk_batches_non_negative CHECK (
        quantity >= 0 AND unit_cost >= 0 AND unit_price >= 0
    )
);

-- Partial index for available batches by product (FIFO: created_at ASC, id ASC)
CREATE INDEX idx_batch_product_variant_available
    ON product_batches(product_id, created_at, id)
    WHERE quantity > 0;
-- Partial index for available batches by variant (FIFO ordering)
CREATE INDEX idx_batch_variant_available
    ON product_batches(variant_id, created_at, id)
    WHERE quantity > 0;

-- ============================================
-- TABLE: documents
-- Section 3.3: Module de Facturation et Fiscalité Tunisienne
-- SINGLE_TABLE inheritance for QUOTE, DELIVERY_NOTE, INVOICE
-- ============================================
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    document_number VARCHAR(50) UNIQUE NOT NULL,
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    document_type VARCHAR(20) NOT NULL CHECK (document_type IN ('QUOTE', 'DELIVERY_NOTE', 'INVOICE')),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'VALIDATED', 'CANCELLED')),
    client_id BIGINT,
    user_id BIGINT,
    total_excluding_tax DECIMAL(19,3) NOT NULL DEFAULT 0.000,
    vat_rate DECIMAL(5,2) NOT NULL DEFAULT 19.00,
    total_vat DECIMAL(19,3) NOT NULL DEFAULT 0.000,
    total_including_tax DECIMAL(19,3) NOT NULL DEFAULT 0.000,
    is_delivery BOOLEAN NOT NULL DEFAULT FALSE,
    transport_fee DECIMAL(19,3), -- Nullable: only set when is_delivery = TRUE
    stamp_duty DECIMAL(19,3) NOT NULL DEFAULT 1.000, -- Default for Invoices
    is_credit_sale BOOLEAN NOT NULL DEFAULT FALSE,
    converted_to_invoice_id BIGINT,
    source_delivery_note_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_documents_transport_fee CHECK (
        (is_delivery = TRUE AND transport_fee IS NOT NULL)
        OR (is_delivery = FALSE AND transport_fee IS NULL)
    )
);

-- Index on document_number for fast lookup
CREATE INDEX idx_documents_document_number ON documents(document_number);
-- Index on date for reporting
CREATE INDEX idx_documents_date ON documents(date);
-- Index on document_type for filtering
CREATE INDEX idx_documents_document_type ON documents(document_type);
-- Index on status for workflow
CREATE INDEX idx_documents_status ON documents(status);
-- Index on client_id for customer history
CREATE INDEX idx_documents_client_id ON documents(client_id);

-- ============================================
-- TABLE: document_lines
-- Section 3.3: Lignes de documents
-- ============================================
CREATE TABLE document_lines (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    product_id BIGINT,
    variant_id BIGINT,
    line_number INTEGER,
    conditioning_description VARCHAR(100), -- Snapshot of how product was sold
    conditioning_quantity_per_unit DECIMAL(19,3) DEFAULT 1.000,
    batch_allocations TEXT, -- JSON snapshot of FIFO batch allocation
    quantity DECIMAL(19,3),
    unit_price DECIMAL(19,3),
    unit_cost DECIMAL(19,3) DEFAULT 0.000, -- Cost per unit snapshot at sale time for margin calculation
    total_line_excluding_tax DECIMAL(19,3) NOT NULL DEFAULT 0.000,
    total_line_including_tax DECIMAL(19,3) NOT NULL DEFAULT 0.000,
    is_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (variant_id) REFERENCES product_variants(id),
    CONSTRAINT chk_document_lines_variant_requires_product CHECK (
        variant_id IS NULL OR product_id IS NOT NULL
    ),
    CONSTRAINT uq_document_lines_document_line_number UNIQUE (document_id, line_number)
);

-- Index on document_id for fast lookup
CREATE INDEX idx_document_lines_document_id ON document_lines(document_id);
-- Index on product_id for sales analysis
CREATE INDEX idx_document_lines_product_id ON document_lines(product_id);

-- ============================================
-- TABLE: payment_receipts
-- Section 3.4.2: Règlements Partiels et Traçabilité
-- ============================================
CREATE TABLE payment_receipts (
    id BIGSERIAL PRIMARY KEY,
    receipt_number VARCHAR(50) UNIQUE NOT NULL,
    client_id BIGINT NOT NULL,
    user_id BIGINT,
    amount_paid DECIMAL(19,3) NOT NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(20) NOT NULL CHECK (payment_method IN ('CASH', 'TRANSFER', 'CHECK', 'CREDIT')),
    previous_debt DECIMAL(19,3), -- Snapshot before payment
    new_debt DECIMAL(19,3), -- Snapshot after payment
    credit_history_id BIGINT UNIQUE, -- Generated credit history entry
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Add foreign key constraints after table creation
ALTER TABLE payment_receipts ADD CONSTRAINT fk_payment_receipts_credit_history 
    FOREIGN KEY (credit_history_id) REFERENCES credit_history(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE credit_history ADD CONSTRAINT fk_credit_history_payment_receipt 
    FOREIGN KEY (payment_receipt_id) REFERENCES payment_receipts(id) DEFERRABLE INITIALLY DEFERRED;

-- Index on receipt_number for fast lookup
CREATE INDEX idx_payment_receipts_receipt_number ON payment_receipts(receipt_number);
-- Index on client_id for payment history
CREATE INDEX idx_payment_receipts_client_id ON payment_receipts(client_id);
-- Index on payment_date for reporting
CREATE INDEX idx_payment_receipts_payment_date ON payment_receipts(payment_date);

-- ============================================
-- TABLE: credit_history
-- Section 3.4.2: Historique des écritures de crédit (IMMUTABLE)
-- ============================================
CREATE TABLE credit_history (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT NOT NULL,
    document_id BIGINT, -- Nullable if direct payment
    payment_receipt_id BIGINT, -- Nullable if sale
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('SALE', 'PAYMENT', 'ADJUSTMENT')),
    amount DECIMAL(19,3) NOT NULL, -- Positive for debt increase, Negative for payments
    running_balance DECIMAL(19,3) NOT NULL, -- Client's total debt after this operation
    deleted BOOLEAN NOT NULL DEFAULT FALSE, -- Soft delete for immutability
    entry_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, -- Immutable
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (document_id) REFERENCES documents(id),
    FOREIGN KEY (payment_receipt_id) REFERENCES payment_receipts(id)
);

-- Index on client_id for credit history lookup
CREATE INDEX idx_credit_history_client_id ON credit_history(client_id);
-- Index on entry_date for chronological view
CREATE INDEX idx_credit_history_entry_date ON credit_history(entry_date);
-- Index on transaction_type for filtering
CREATE INDEX idx_credit_history_transaction_type ON credit_history(transaction_type);

-- ============================================
-- TABLE: audit_logs
-- Section 6.3: Sécurité Logicielle
-- ============================================
-- user_id is an OPTIONAL identity snapshot (nullable, no FK) paired with an email
-- snapshot. Audit rows deliberately retain identity even if the acting user is later
-- deleted, and system-triggered actions may have no user at all. There is intentionally
-- NO foreign key from audit_logs to users.
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    email VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    details TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index on timestamp for log viewing
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);
-- Index on entity_type and entity_id for entity-specific audits
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- ============================================
-- TABLE: refresh_tokens
-- Section 6.1: JWT Refresh Token Management
-- ============================================
-- Tokens are bound to the immutable user ID rather than the (mutable) email.
-- When a user changes email via PUT /api/auth/me, all active refresh tokens for
-- that user are revoked; they are never re-bound to a different identity.
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) UNIQUE NOT NULL, -- SHA-256 hash of the refresh token
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index on user_id for bulk revocation
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ============================================
-- TABLE: access_tokens
-- Section 6.1: JWT Access Token Revocation
-- ============================================
-- Server-side allowlist of issued access tokens. Tokens are bound to the immutable user
-- ID; when the email changes (PUT /api/auth/me) all active records for the user are
-- revoked, immediately invalidating already-issued access tokens.
CREATE TABLE access_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) UNIQUE NOT NULL, -- SHA-256 hash of the access token
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index on user_id for bulk revocation
CREATE INDEX idx_access_tokens_user_id ON access_tokens(user_id);

-- ============================================
-- TABLE: password_reset_tokens
-- Section 6.2: Password Reset OTP Management
-- ============================================
-- Email is a mutable snapshot, not a foreign key: ownership is tracked via the
-- immutable user_id so email changes (PUT /api/auth/me) never violate a constraint.
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email VARCHAR(100) NOT NULL,
    otp_code VARCHAR(100) NOT NULL, -- BCrypt hashed
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index on user_id for fast lookup during reset and bulk revocation
CREATE INDEX idx_prt_user_id ON password_reset_tokens(user_id);
-- Index on email for display/lookup
CREATE INDEX idx_prt_email ON password_reset_tokens(email);
-- Partial unique index: at most one active (unused) token per user
CREATE UNIQUE INDEX idx_prt_active_user ON password_reset_tokens(user_id) WHERE used = false;

-- ============================================
-- SAMPLE DATA (for testing)
-- ============================================

-- Insert sample supplier
INSERT INTO suppliers (name, phone, email, address, tax_identification_number, contact_person, payment_terms) VALUES
('Distribution Bâtiment Tunisie', '+216 71 234 567', 'contact@dbt.tn', '45 Rue du Commerce, Tunis', '9876543/B/M/000', 'Ahmed Ben Ali', '30 jours');

-- Insert sample client
INSERT INTO clients (version, name, phone, email, address, tax_identification_number, credit_limit, current_debt) VALUES
(0, 'ABC Construction SARL', '+216 71 123 456', 'contact@abc.tn', '123 Rue de l''Industrie, Tunis', '1234567/A/M/000', 5000.000, 0.000);

-- Insert sample products (with unit_price and supplier_id)
INSERT INTO products (reference, name, description, category, unit_type, base_unit, stock_quantity, average_purchase_price, unit_price, supplier_id) VALUES
('PROD-001', 'Marteau Professionnel', 'Marteau à tête bombée, manche en fibre de verre', 'Outillage', 'UNITARY', 'pièce', 50.000, 25.000, 35.000, 1),
('PROD-002', 'Fil Électrique 2.5mm²', 'Fil électrique rigide, couleur rouge, vendu au mètre', 'Électricité', 'LENGTH', 'm', 350.000, 0.821, 1.500, 1),
('PROD-003', 'Sac de Ciment 50Kg', 'Ciment Portland CPJ-35', 'Matériaux', 'WEIGHT', 'sac', 200.000, 12.000, 15.000, 1),
('PROD-004', 'Brique de construction', 'Brique rouge standard 20x10x5cm', 'Matériaux', 'UNITARY', 'pièce', 5000.000, 0.500, 0.720, 1);

-- Insert sample product variants
INSERT INTO product_variants (product_id, sku, variant_name, attributes) VALUES
(2, 'FIL-2.5-ROUGE', 'Fil 2.5mm² Rouge', '{"color": "red", "caliber": "2.5mm²"}'),
(2, 'FIL-2.5-BLEU', 'Fil 2.5mm² Bleu', '{"color": "blue", "caliber": "2.5mm²"}'),
(3, 'CIM-CPJ35-50KG', 'Ciment CPJ-35 50Kg', '{"grade": "CPJ-35", "weight": "50Kg"}');

-- Insert sample product conditionings
INSERT INTO product_conditionings (product_id, description, quantity_per_unit, unit_price) VALUES
(2, 'Rouleau 100m', 100.000, 100.000); -- Non-linear pricing: 100m roll = 100 DT (not 150 DT)

-- Insert sample product batches (FIFO inventory)
INSERT INTO product_batches (product_id, variant_id, quantity, unit_cost, unit_price, supplier_id, notes) VALUES
(1, NULL, 50.000, 25.000, 35.000, 1, 'Stock initial'),
(2, 1, 200.000, 0.800, 1.500, 1, 'Lot initial fil rouge'),
(2, 2, 150.000, 0.850, 1.500, 1, 'Lot initial fil bleu'),
(3, 3, 200.000, 12.000, 15.000, 1, 'Stock initial ciment'),
(4, NULL, 5000.000, 0.500, 0.720, 1, 'Stock initial');

-- ============================================
-- END OF SCHEMA
-- ============================================
