-- Create PostgreSQL-compatible sequence for receipt number generation in H2 tests
CREATE SEQUENCE IF NOT EXISTS seq_receipt_number START WITH 1 INCREMENT BY 1;
