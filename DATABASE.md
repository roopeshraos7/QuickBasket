# QUICKBASKET: DATABASE SPECIFICATION

> **Database Engine**: PostgreSQL 16  
> **ORM Framework**: Spring Data JPA / Hibernate  
> **Strategy**: Relational Normalization + Timeseries Price Logging  

---

## 1. Entity-Relationship Diagram (ASCII)

```text
 ┌──────────────┐         ┌────────────────┐         ┌────────────────┐
 │    USERS     │         │   WATCHLISTS   │         │  PRICE_ALERTS  │
 ├──────────────┤         ├────────────────┤         ├────────────────┤
 │ PK id        │1       *│ PK id          │1       *│ PK id          │
 │    email     ├─────────┤ FK user_id     ├─────────┤ FK user_id     │
 │    password  │         │ FK product_id  │         │ FK product_id  │
 └──────────────┘         └───────┬────────┘         │    target_price│
                                  │                  └────────────────┘
                                  │
 ┌──────────────┐         ┌───────▼────────┐         ┌────────────────┐
 │  PLATFORMS   │         │    PRODUCTS    │         │ PRICE_HISTORY  │
 ├──────────────┤         ├────────────────┤         ├────────────────┤
 │ PK id        │1       *│ PK id          │1       *│ PK id          │
 │    code      ├─────────┤    name        ├─────────┤ FK product_id  │
 │    name      │         │    brand       │         │ FK platform_id │
 └──────┬───────┘         │    category    │         │    price       │
        │                 └───────┬────────┘         │    recorded_at │
        │                         │                  └────────────────┘
        │   ┌─────────────────────┘
        │ 1 │ *
 ┌──────▼───▼───┐
 │PLATFORM_OFFERS│
 ├──────────────┤
 │ PK id        │
 │ FK product_id│
 │ FK platform_id│
 │    price     │
 │    mrp       │
 │    in_stock  │
 └──────────────┘
```

---

## 2. Table DDL Definitions

```sql
-- 1. USERS TABLE
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'ROLE_USER' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. PRODUCTS TABLE (Canonical Product Catalog)
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    brand VARCHAR(100),
    category VARCHAR(100),
    image_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 3. PLATFORMS TABLE (Blinkit, Zepto, Swiggy Instamart, BigBasket)
CREATE TABLE platforms (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    logo_url TEXT,
    is_active BOOLEAN DEFAULT TRUE NOT NULL
);

-- Seed Platform Masters
INSERT INTO platforms (code, display_name) VALUES 
('BLINKIT', 'Blinkit'),
('ZEPTO', 'Zepto'),
('INSTAMART', 'Swiggy Instamart'),
('BIGBASKET', 'BigBasket');

-- 4. PLATFORM_OFFERS TABLE (Live Price Snapshot)
CREATE TABLE platform_offers (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    platform_id INT NOT NULL REFERENCES platforms(id),
    external_item_id VARCHAR(100) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    mrp NUMERIC(10, 2) NOT NULL,
    discount_percentage NUMERIC(5, 2) DEFAULT 0.00,
    in_stock BOOLEAN DEFAULT TRUE NOT NULL,
    eta_minutes INT,
    product_url TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_product_platform UNIQUE (product_id, platform_id)
);

-- 5. PRICE_HISTORY TABLE (Timeseries Data)
CREATE TABLE price_history (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    platform_id INT NOT NULL REFERENCES platforms(id),
    price NUMERIC(10, 2) NOT NULL,
    mrp NUMERIC(10, 2) NOT NULL,
    in_stock BOOLEAN DEFAULT TRUE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 6. WATCHLISTS TABLE
CREATE TABLE watchlists (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_user_watchlist_product UNIQUE (user_id, product_id)
);

-- 7. PRICE_ALERTS TABLE
CREATE TABLE price_alerts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    target_price NUMERIC(10, 2) NOT NULL,
    is_triggered BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

---

## 3. Database Indexing Strategy

To support fast search lookups and timeseries queries, the following indexes are configured:

```sql
-- Fast search indexing on product name & brand
CREATE INDEX idx_products_search ON products(name, brand);

-- Fast lookup for current offers per product
CREATE INDEX idx_offers_product_id ON platform_offers(product_id);

-- High-performance composite index for 30-day price trend queries
CREATE INDEX idx_price_history_lookup ON price_history(product_id, platform_id, recorded_at DESC);

-- Fast lookup for active non-triggered price alerts during cron runs
CREATE INDEX idx_price_alerts_active ON price_alerts(is_triggered, target_price) WHERE is_triggered = FALSE;
```

---

## 4. Price History Strategy

* **In-Place Updates**: `platform_offers` is updated in-place whenever a fresh search occurs to maintain live snapshots.
* **Append-Only Timeseries**: Every price change or scheduled check appends a new record into `price_history`.
* **Pruning Strategy**: A scheduled cron job prunes records older than 90 days (`DELETE FROM price_history WHERE recorded_at < NOW() - INTERVAL '90 days'`) to keep table sizes manageable on a single node.
