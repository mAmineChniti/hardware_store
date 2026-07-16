-- ============================================
-- INOVEXAHUB - Système de Gestion Commerciale et Point de Vente (POS)
-- Sprint 1: Schéma Relationnel SQL
-- Version: 1.0 (MVP)
-- Database: PostgreSQL
-- ============================================

-- Drop tables if they exist (for clean recreation)
DROP TABLE IF EXISTS credit_history CASCADE;
DROP TABLE IF EXISTS payment_receipts CASCADE;
DROP TABLE IF EXISTS document_lines CASCADE;
DROP TABLE IF EXISTS documents CASCADE;
DROP TABLE IF EXISTS product_costs CASCADE;
DROP TABLE IF EXISTS product_conditionings CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS suppliers CASCADE;
DROP TABLE IF EXISTS clients CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS audit_logs CASCADE;

-- Create sequences for document number generation (preserving existing state)
CREATE SEQUENCE IF NOT EXISTS seq_quote_number START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_delivery_note_number START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS seq_invoice_number START 1 INCREMENT 1;

-- ============================================
-- TABLE: users
-- Section 2: Gestion des Utilisateurs et Droits d'Accès
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR NOT NULL, -- BCrypt hashed
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'EMPLOYEE')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Index on username for fast authentication
CREATE INDEX idx_users_username ON users(username);

-- ============================================
-- TABLE: clients
-- Section 3.4: Module de Gestion des Tiers et du Crédit
-- ============================================
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
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
    reference VARCHAR(50) UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image TEXT, -- Base64 encoded image string
    category VARCHAR(50),
    unit_type VARCHAR(20) NOT NULL CHECK (unit_type IN ('UNITARY', 'WEIGHT', 'LENGTH', 'VOLUME')),
    is_heavy_material BOOLEAN NOT NULL DEFAULT FALSE,
    base_unit VARCHAR(20), -- e.g., "m", "kg", "piece"
    stock_quantity DECIMAL(19,3) NOT NULL DEFAULT 0.000,
    average_purchase_price DECIMAL(19,3) NOT NULL DEFAULT 0.000, -- PAMP for margin calculation
    price_on_site DECIMAL(19,3), -- Prix de Vente Sur Place (for heavy materials)
    price_delivered DECIMAL(19,3), -- Prix de Vente Livré (for heavy materials)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Index on reference for internal lookup
CREATE INDEX idx_products_reference ON products(reference);
-- Index on name for predictive search
CREATE INDEX idx_products_name ON products(name);
-- Index on category for filtering
CREATE INDEX idx_products_category ON products(category);

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
-- TABLE: product_costs
-- Section 3.1.3: Historique des Coûts Unitaires par Date
-- ============================================
CREATE TABLE product_costs (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    unit_cost DECIMAL(19,3) NOT NULL,
    effective_date DATE NOT NULL,
    supplier_id BIGINT,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

-- Index on product_id for fast lookup
CREATE INDEX idx_product_costs_product_id ON product_costs(product_id);
-- Index on effective_date for date-based queries
CREATE INDEX idx_product_costs_effective_date ON product_costs(effective_date);
-- Composite index for product + date queries
CREATE INDEX idx_product_costs_product_date ON product_costs(product_id, effective_date);

-- ============================================
-- TABLE: documents
-- Section 3.3: Module de Facturation et Fiscalité Tunisienne
-- SINGLE_TABLE inheritance for QUOTE, DELIVERY_NOTE, INVOICE
-- ============================================
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
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
    transport_fee DECIMAL(19,3) NOT NULL DEFAULT 10.000, -- Default for BL
    stamp_duty DECIMAL(19,3) NOT NULL DEFAULT 1.000, -- Default for Invoices
    is_credit_sale BOOLEAN NOT NULL DEFAULT FALSE,
    converted_to_invoice_id BIGINT,
    source_delivery_note_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
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
    line_number INTEGER,
    conditioning_description VARCHAR(100), -- Snapshot of how product was sold
    quantity DECIMAL(19,3),
    unit_price DECIMAL(19,3),
    unit_cost DECIMAL(19,3) DEFAULT 0.000, -- Cost per unit snapshot at sale time for margin calculation
    total_line_excluding_tax DECIMAL(19,3) NOT NULL DEFAULT 0.000,
    total_line_including_tax DECIMAL(19,3) NOT NULL DEFAULT 0.000,
    is_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
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
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50),
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
-- SAMPLE DATA (for testing)
-- ============================================

-- Insert default admin user (password: admin123 - BCrypt hashed)
INSERT INTO users (username, password, full_name, role, enabled) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'Administrateur', 'ADMIN', TRUE);

-- Insert sample client
INSERT INTO clients (name, phone, email, address, tin, credit_limit, current_debt) VALUES
('ABC Construction SARL', '+216 71 123 456', 'contact@abc.tn', '123 Rue de l''Industrie, Tunis', '1234567/A/M/000', 5000.000, 0.000);

-- Insert sample products
INSERT INTO products (reference, name, description, category, unit_type, is_heavy_material, base_unit, stock_quantity, average_purchase_price, price_on_site, price_delivered) VALUES
('PROD-001', 'Marteau Professionnel', 'Marteau à tête bombée, manche en fibre de verre', 'Outillage', 'UNITARY', FALSE, 'pièce', 50.000, 25.000, 35.000, NULL),
('PROD-002', 'Fil Électrique 2.5mm²', 'Fil électrique rigide, couleur rouge, vendu au mètre', 'Électricité', 'LENGTH', FALSE, 'm', 500.000, 0.800, 1.500, NULL),
('PROD-003', 'Sac de Ciment 50Kg', 'Ciment Portland CPJ-35', 'Matériaux', 'WEIGHT', TRUE, 'sac', 200.000, 12.000, 14.500, 15.000),
('PROD-004', 'Brique de construction', 'Brique rouge standard 20x10x5cm', 'Matériaux', 'UNITARY', TRUE, 'pièce', 5000.000, 0.500, 0.720, 0.780);

-- Insert sample product conditioning
INSERT INTO product_conditionings (product_id, description, quantity_per_unit, unit_price) VALUES
(2, 'Rouleau 100m', 100.000, 100.000); -- Non-linear pricing: 100m roll = 100 DT (not 150 DT)

-- ============================================
-- END OF SCHEMA
-- ============================================
