-- Catalogue Service Database Initialization
-- This file documents the database schema
-- Note: Schema is auto-created by JPA/Hibernate, this is for reference

-- Products Table
CREATE TABLE IF NOT EXISTS products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    category VARCHAR(255) NOT NULL,
    starting_price DECIMAL(10,2) NOT NULL,
    reserve_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    seller_id INTEGER NOT NULL,
    image_url VARCHAR(500),
    condition VARCHAR(50),
    quantity INTEGER,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_starting_price CHECK (starting_price > 0),
    CONSTRAINT chk_reserve_price CHECK (reserve_price > 0),
    CONSTRAINT chk_price_range CHECK (starting_price <= reserve_price),
    CONSTRAINT chk_quantity CHECK (quantity >= 1)
);

-- Sample Data (automatically loaded by LoadCatalogueDatabase.java)
-- This is for reference only

INSERT INTO products (name, description, category, starting_price, reserve_price, status, seller_id, image_url, condition, quantity, created_at, updated_at)
VALUES 
('Dell XPS 15 Laptop', 'High-performance laptop with Intel i7, 16GB RAM, 512GB SSD', 'Electronics', 800.00, 1200.00, 'DRAFT', 1, 'https://example.com/laptop.jpg', 'NEW', 1, datetime('now'), datetime('now')),
('Vintage Rolex Watch', 'Authentic vintage Rolex Submariner from 1980s', 'Jewelry', 5000.00, 8000.00, 'DRAFT', 1, 'https://example.com/watch.jpg', 'USED', 1, datetime('now'), datetime('now')),
('Abstract Art Painting', 'Original abstract painting by local artist, 24x36 inches', 'Art', 200.00, 500.00, 'DRAFT', 1, 'https://example.com/painting.jpg', 'NEW', 1, datetime('now'), datetime('now')),
('Canon EOS R5 Camera', 'Professional mirrorless camera with 45MP sensor', 'Electronics', 2500.00, 3500.00, 'ACTIVE', 1, 'https://example.com/camera.jpg', 'NEW', 1, datetime('now'), datetime('now')),
('First Edition Harry Potter', 'First edition Harry Potter and the Philosopher''s Stone', 'Books', 1000.00, 2000.00, 'DRAFT', 1, 'https://example.com/book.jpg', 'USED', 1, datetime('now'), datetime('now'));

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_products_seller_id ON products(seller_id);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products(created_at);

-- Query Examples

-- Get all active products
-- SELECT * FROM products WHERE status = 'ACTIVE';

-- Search by keyword
-- SELECT * FROM products WHERE LOWER(name) LIKE '%laptop%' OR LOWER(description) LIKE '%laptop%';

-- Get products by category
-- SELECT * FROM products WHERE category = 'Electronics';

-- Get products by seller
-- SELECT * FROM products WHERE seller_id = 1;

-- Get products by price range
-- SELECT * FROM products WHERE starting_price BETWEEN 500 AND 1500 AND status = 'ACTIVE';

-- Get products by seller and status
-- SELECT * FROM products WHERE seller_id = 1 AND status = 'DRAFT';

-- Product lifecycle status values:
-- DRAFT - Product created but not yet listed
-- ACTIVE - Product is active and available for auction
-- INACTIVE - Product temporarily removed from listings
-- IN_AUCTION - Product is currently in an active auction
-- SOLD - Product has been sold
-- ARCHIVED - Product archived (historical record)
