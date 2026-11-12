-- Furniture store bootstrap, MariaDB 12.2 (InnoDB/utf8mb4).
-- Run ONCE. CREATE DATABASE deliberately fails if the database already exists.
-- Legacy database bootstrap/seed scripts have been removed from this project.
CREATE DATABASE furniture_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE furniture_shop;
SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    email_verified_at DATETIME(6),
    last_login_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','LOCKED','DISABLED'))
) ENGINE=InnoDB;

CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uq_roles_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE user_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(80),
    recipient_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    ward_name VARCHAR(120),
    district_name VARCHAR(120),
    province_name VARCHAR(120),
    postal_code VARCHAR(20),
    country_code CHAR(2) NOT NULL DEFAULT 'VN',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    default_user_id BIGINT AS (CASE WHEN is_default=1 THEN user_id ELSE NULL END) PERSISTENT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_user_addresses_default UNIQUE(default_user_id),
    CONSTRAINT ck_user_addresses_default CHECK(is_default IN (0,1)),
    CONSTRAINT fk_user_addresses_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description TEXT,
    image_url VARCHAR(1000),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_categories_slug UNIQUE(slug),
    CONSTRAINT fk_categories_parent FOREIGN KEY(parent_id) REFERENCES categories(id)
) ENGINE=InnoDB;

CREATE TABLE rooms (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    description TEXT,
    image_url VARCHAR(1000),
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_rooms_slug UNIQUE(slug)
) ENGINE=InnoDB;

CREATE TABLE materials (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    description TEXT,
    care_instructions TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_materials_slug UNIQUE(slug)
) ENGINE=InnoDB;

CREATE TABLE brands (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    logo_url VARCHAR(1000),
    description TEXT,
    country_code CHAR(2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_brands_slug UNIQUE(slug)
) ENGINE=InnoDB;

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    brand_id BIGINT,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    short_description VARCHAR(500),
    description LONGTEXT,
    care_instructions TEXT,
    warranty_months INT NOT NULL DEFAULT 0,
    assembly_required BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    seo_title VARCHAR(200),
    seo_description VARCHAR(320),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_products_slug UNIQUE(slug),
    CONSTRAINT ck_products_warranty CHECK(warranty_months>=0),
    CONSTRAINT ck_products_status CHECK(status IN ('DRAFT','ACTIVE','DISCONTINUED','ARCHIVED')),
    CONSTRAINT fk_products_category FOREIGN KEY(category_id) REFERENCES categories(id),
    CONSTRAINT fk_products_brand FOREIGN KEY(brand_id) REFERENCES brands(id),
    KEY idx_products_status_category(status,category_id,id),
    KEY idx_products_name(name)
) ENGINE=InnoDB;

CREATE TABLE product_rooms (
    product_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    PRIMARY KEY(product_id,room_id),
    CONSTRAINT fk_product_rooms_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_rooms_room FOREIGN KEY(room_id) REFERENCES rooms(id)
) ENGINE=InnoDB;

CREATE TABLE product_variants (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku VARCHAR(80) NOT NULL,
    variant_key VARCHAR(180) NOT NULL,
    name VARCHAR(200) NOT NULL,
    color_name VARCHAR(80),
    color_hex CHAR(7),
    size_label VARCHAR(100),
    width_mm DECIMAL(10,2),
    depth_mm DECIMAL(10,2),
    height_mm DECIMAL(10,2),
    weight_kg DECIMAL(10,3),
    price DECIMAL(15,2) NOT NULL,
    compare_at_price DECIMAL(15,2),
    lead_time_days INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_variants_sku UNIQUE(sku),
    CONSTRAINT uq_variants_options UNIQUE(product_id,variant_key),
    CONSTRAINT uq_variants_id_product UNIQUE(id,product_id),
    CONSTRAINT ck_variants_price CHECK(price>=0 AND (compare_at_price IS NULL OR compare_at_price>=price)),
    CONSTRAINT ck_variants_dimensions CHECK((width_mm IS NULL OR width_mm>0) AND (depth_mm IS NULL OR depth_mm>0) AND (height_mm IS NULL OR height_mm>0) AND (weight_kg IS NULL OR weight_kg>0)),
    CONSTRAINT ck_variants_lead_time CHECK(lead_time_days>=0),
    CONSTRAINT ck_variants_color CHECK(color_hex IS NULL OR color_hex REGEXP '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_variants_status CHECK(status IN ('ACTIVE','INACTIVE','DISCONTINUED')),
    CONSTRAINT fk_variants_product FOREIGN KEY(product_id) REFERENCES products(id),
    KEY idx_variants_price_status(price,status)
) ENGINE=InnoDB;

CREATE TABLE variant_materials (
    variant_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    component_name VARCHAR(100) NOT NULL DEFAULT 'MAIN',
    note VARCHAR(500),
    PRIMARY KEY(variant_id,material_id,component_name),
    CONSTRAINT fk_variant_materials_variant FOREIGN KEY(variant_id) REFERENCES product_variants(id) ON DELETE CASCADE,
    CONSTRAINT fk_variant_materials_material FOREIGN KEY(material_id) REFERENCES materials(id)
) ENGINE=InnoDB;

CREATE TABLE product_images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    variant_id BIGINT,
    url VARCHAR(1000) NOT NULL,
    alt_text VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_product_images_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_images_variant FOREIGN KEY(variant_id,product_id) REFERENCES product_variants(id,product_id),
    KEY idx_product_images_order(product_id,sort_order,id)
) ENGINE=InnoDB;

CREATE TABLE product_specifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    value VARCHAR(1000) NOT NULL,
    unit VARCHAR(30),
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_product_specs UNIQUE(product_id,name),
    CONSTRAINT fk_product_specs_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE collections (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    description TEXT,
    image_url VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_collections_slug UNIQUE(slug)
) ENGINE=InnoDB;

CREATE TABLE collection_products (
    collection_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY(collection_id,product_id),
    CONSTRAINT fk_collection_products_collection FOREIGN KEY(collection_id) REFERENCES collections(id) ON DELETE CASCADE,
    CONSTRAINT fk_collection_products_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE carts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    guest_token_hash BINARY(32),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at DATETIME(6),
    active_user_id BIGINT AS (CASE WHEN status='ACTIVE' THEN user_id ELSE NULL END) PERSISTENT,
    active_guest_hash BINARY(32) AS (CASE WHEN status='ACTIVE' THEN guest_token_hash ELSE NULL END) PERSISTENT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_carts_active_user UNIQUE(active_user_id),
    CONSTRAINT uq_carts_active_guest UNIQUE(active_guest_hash),
    CONSTRAINT ck_carts_owner CHECK((user_id IS NOT NULL AND guest_token_hash IS NULL) OR (user_id IS NULL AND guest_token_hash IS NOT NULL)),
    CONSTRAINT ck_carts_status CHECK(status IN ('ACTIVE','CONVERTED','ABANDONED')),
    CONSTRAINT fk_carts_user FOREIGN KEY(user_id) REFERENCES users(id),
    KEY idx_carts_expiry(status,expires_at)
) ENGINE=InnoDB;

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_cart_items UNIQUE(cart_id,variant_id),
    CONSTRAINT ck_cart_items_quantity CHECK(quantity>0),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY(cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_variant FOREIGN KEY(variant_id) REFERENCES product_variants(id)
) ENGINE=InnoDB;

CREATE TABLE wishlist_items (
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY(user_id,product_id),
    CONSTRAINT fk_wishlist_items_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_items_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE warehouses (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(30),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_warehouses_code UNIQUE(code)
) ENGINE=InnoDB;

CREATE TABLE inventory_balances (
    warehouse_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    on_hand_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    available_quantity INT AS (on_hand_quantity-reserved_quantity) PERSISTENT,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY(warehouse_id,variant_id),
    CONSTRAINT ck_inventory_quantities CHECK(on_hand_quantity>=0 AND reserved_quantity>=0 AND reserved_quantity<=on_hand_quantity),
    CONSTRAINT fk_inventory_balances_warehouse FOREIGN KEY(warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_inventory_balances_variant FOREIGN KEY(variant_id) REFERENCES product_variants(id)
) ENGINE=InnoDB;

CREATE TABLE coupons (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150),
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(15,2) NOT NULL,
    max_discount_amount DECIMAL(15,2),
    minimum_order_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_usage_limit INT,
    per_user_limit INT,
    starts_at DATETIME(6) NOT NULL,
    ends_at DATETIME(6) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_coupons_code UNIQUE(code),
    CONSTRAINT ck_coupons_discount CHECK(discount_value>0 AND ((discount_type='PERCENT' AND discount_value<=100) OR discount_type='FIXED')),
    CONSTRAINT ck_coupons_amounts CHECK(minimum_order_amount>=0 AND (max_discount_amount IS NULL OR max_discount_amount>0)),
    CONSTRAINT ck_coupons_limits CHECK((total_usage_limit IS NULL OR total_usage_limit>0) AND (per_user_limit IS NULL OR per_user_limit>0)),
    CONSTRAINT ck_coupons_dates CHECK(ends_at>starts_at)
) ENGINE=InnoDB;

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL,
    user_id BIGINT,
    idempotency_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    subtotal DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    shipping_fee DECIMAL(15,2) NOT NULL DEFAULT 0,
    assembly_fee DECIMAL(15,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    grand_total DECIMAL(15,2) NOT NULL,
    coupon_id BIGINT,
    coupon_code_snapshot VARCHAR(50),
    customer_note TEXT,
    internal_note TEXT,
    placed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    confirmed_at DATETIME(6),
    completed_at DATETIME(6),
    cancelled_at DATETIME(6),
    cancel_reason VARCHAR(1000),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_orders_number UNIQUE(order_number),
    CONSTRAINT uq_orders_idempotency UNIQUE(idempotency_key),
    CONSTRAINT uq_orders_id_user UNIQUE(id,user_id),
    CONSTRAINT uq_orders_id_currency UNIQUE(id,currency),
    CONSTRAINT uq_orders_id_coupon UNIQUE(id,coupon_id),
    CONSTRAINT ck_orders_status CHECK(status IN ('PENDING_CONFIRMATION','CONFIRMED','PROCESSING','COMPLETED','CANCELLED')),
    CONSTRAINT ck_orders_amounts CHECK(subtotal>=0 AND discount_amount>=0 AND discount_amount<=subtotal AND shipping_fee>=0 AND assembly_fee>=0 AND tax_amount>=0 AND grand_total=subtotal-discount_amount+shipping_fee+assembly_fee+tax_amount),
    CONSTRAINT fk_orders_user FOREIGN KEY(user_id) REFERENCES users(id),
    CONSTRAINT fk_orders_coupon FOREIGN KEY(coupon_id) REFERENCES coupons(id),
    KEY idx_orders_user_time(user_id,placed_at),
    KEY idx_orders_status_time(status,placed_at)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    variant_name VARCHAR(200) NOT NULL,
    sku VARCHAR(80) NOT NULL,
    image_url VARCHAR(1000),
    color_name VARCHAR(80),
    dimensions_snapshot VARCHAR(200),
    materials_snapshot TEXT,
    warranty_months INT NOT NULL DEFAULT 0,
    assembly_required BOOLEAN NOT NULL DEFAULT FALSE,
    quantity INT NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    line_total DECIMAL(15,2) AS (quantity*unit_price-discount_amount) PERSISTENT,
    CONSTRAINT uq_order_items_id_order UNIQUE(id,order_id),
    CONSTRAINT uq_order_items_id_variant UNIQUE(id,variant_id),
    CONSTRAINT uq_order_items_id_order_product UNIQUE(id,order_id,product_id),
    CONSTRAINT ck_order_items_values CHECK(quantity>0 AND unit_price>=0 AND discount_amount>=0 AND discount_amount<=quantity*unit_price AND warranty_months>=0),
    CONSTRAINT fk_order_items_order FOREIGN KEY(order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_variant FOREIGN KEY(variant_id,product_id) REFERENCES product_variants(id,product_id)
) ENGINE=InnoDB;

CREATE TABLE order_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    address_type VARCHAR(20) NOT NULL DEFAULT 'SHIPPING',
    recipient_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(254),
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    ward_name VARCHAR(120),
    district_name VARCHAR(120),
    province_name VARCHAR(120),
    postal_code VARCHAR(20),
    country_code CHAR(2) NOT NULL DEFAULT 'VN',
    company_name VARCHAR(200),
    tax_code VARCHAR(40),
    CONSTRAINT uq_order_addresses_type UNIQUE(order_id,address_type),
    CONSTRAINT ck_order_addresses_type CHECK(address_type IN ('SHIPPING','BILLING')),
    CONSTRAINT fk_order_addresses_order FOREIGN KEY(order_id) REFERENCES orders(id)
) ENGINE=InnoDB;

CREATE TABLE order_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by BIGINT,
    note VARCHAR(1000),
    changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_order_history_from CHECK(from_status IS NULL OR from_status IN ('PENDING_CONFIRMATION','CONFIRMED','PROCESSING','COMPLETED','CANCELLED')),
    CONSTRAINT ck_order_history_to CHECK(to_status IN ('PENDING_CONFIRMATION','CONFIRMED','PROCESSING','COMPLETED','CANCELLED')),
    CONSTRAINT fk_order_history_order FOREIGN KEY(order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_history_actor FOREIGN KEY(changed_by) REFERENCES users(id),
    KEY idx_order_history_time(order_id,changed_at,id)
) ENGINE=InnoDB;

CREATE TABLE inventory_reservations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_item_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    fulfilled_quantity INT NOT NULL DEFAULT 0,
    released_quantity INT NOT NULL DEFAULT 0,
    remaining_quantity INT AS (quantity-fulfilled_quantity-released_quantity) PERSISTENT,
    expires_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_inventory_reservations_item_warehouse UNIQUE(order_item_id,warehouse_id),
    CONSTRAINT ck_reservation_quantities CHECK(quantity>0 AND fulfilled_quantity>=0 AND released_quantity>=0 AND fulfilled_quantity+released_quantity<=quantity),
    CONSTRAINT fk_reservations_item FOREIGN KEY(order_item_id,variant_id) REFERENCES order_items(id,variant_id),
    CONSTRAINT fk_reservations_inventory FOREIGN KEY(warehouse_id,variant_id) REFERENCES inventory_balances(warehouse_id,variant_id),
    KEY idx_reservations_expiry(expires_at,remaining_quantity)
) ENGINE=InnoDB;

CREATE TABLE inventory_movements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    warehouse_id BIGINT NOT NULL,
    variant_id BIGINT NOT NULL,
    order_item_id BIGINT,
    movement_type VARCHAR(30) NOT NULL,
    quantity_delta INT NOT NULL,
    reference_code VARCHAR(100),
    idempotency_key VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    reason VARCHAR(1000),
    performed_by BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_inventory_movements_key UNIQUE(idempotency_key),
    CONSTRAINT ck_inventory_movements_type CHECK(movement_type IN ('RECEIPT','SHIPMENT','RETURN','ADJUSTMENT','TRANSFER_IN','TRANSFER_OUT')),
    CONSTRAINT ck_inventory_movements_delta CHECK(quantity_delta<>0 AND (movement_type NOT IN ('RECEIPT','RETURN','TRANSFER_IN') OR quantity_delta>0) AND (movement_type NOT IN ('SHIPMENT','TRANSFER_OUT') OR quantity_delta<0)),
    CONSTRAINT fk_movements_inventory FOREIGN KEY(warehouse_id,variant_id) REFERENCES inventory_balances(warehouse_id,variant_id),
    CONSTRAINT fk_movements_item FOREIGN KEY(order_item_id,variant_id) REFERENCES order_items(id,variant_id),
    CONSTRAINT fk_movements_actor FOREIGN KEY(performed_by) REFERENCES users(id),
    KEY idx_movements_inventory_time(warehouse_id,variant_id,created_at)
) ENGINE=InnoDB;

CREATE TABLE coupon_redemptions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT,
    discount_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RESERVED',
    expires_at DATETIME(6),
    redeemed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_coupon_redemptions_order UNIQUE(order_id),
    CONSTRAINT ck_coupon_redemptions_amount CHECK(discount_amount>=0),
    CONSTRAINT ck_coupon_redemptions_status CHECK(status IN ('RESERVED','APPLIED','RELEASED')),
    CONSTRAINT fk_redemptions_coupon FOREIGN KEY(coupon_id) REFERENCES coupons(id),
    CONSTRAINT fk_redemptions_order_coupon FOREIGN KEY(order_id,coupon_id) REFERENCES orders(id,coupon_id),
    CONSTRAINT fk_redemptions_owner FOREIGN KEY(order_id,user_id) REFERENCES orders(id,user_id),
    KEY idx_redemptions_coupon_user(coupon_id,user_id,status),
    KEY idx_redemptions_expiry(status,expires_at)
) ENGINE=InnoDB;

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    method VARCHAR(20) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_transaction_id VARCHAR(150) CHARACTER SET ascii COLLATE ascii_bin,
    idempotency_key VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_at DATETIME(6),
    expires_at DATETIME(6),
    verified_by BIGINT,
    failure_reason VARCHAR(1000),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_payments_provider_tx UNIQUE(provider,provider_transaction_id),
    CONSTRAINT uq_payments_key UNIQUE(idempotency_key),
    CONSTRAINT uq_payments_id_provider UNIQUE(id,provider),
    CONSTRAINT uq_payments_id_order_currency UNIQUE(id,order_id,currency),
    CONSTRAINT ck_payments_amount CHECK(amount>0),
    CONSTRAINT ck_payments_method CHECK(method IN ('COD','BANK_TRANSFER','GATEWAY')),
    CONSTRAINT ck_payments_status CHECK(status IN ('PENDING','SUCCEEDED','FAILED','CANCELLED','EXPIRED')),
    CONSTRAINT fk_payments_order FOREIGN KEY(order_id,currency) REFERENCES orders(id,currency),
    CONSTRAINT fk_payments_verifier FOREIGN KEY(verified_by) REFERENCES users(id),
    KEY idx_payments_order_status(order_id,status),
    KEY idx_payments_expiry(status,expires_at)
) ENGINE=InnoDB;

CREATE TABLE payment_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT,
    provider VARCHAR(50) NOT NULL,
    provider_event_id VARCHAR(150) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload JSON,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    error_message VARCHAR(1000),
    received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6),
    CONSTRAINT uq_payment_events_provider_id UNIQUE(provider,provider_event_id),
    CONSTRAINT ck_payment_events_status CHECK(processing_status IN ('RECEIVED','PROCESSED','FAILED','IGNORED')),
    CONSTRAINT fk_payment_events_payment FOREIGN KEY(payment_id,provider) REFERENCES payments(id,provider),
    KEY idx_payment_events_processing(processing_status,received_at)
) ENGINE=InnoDB;

CREATE TABLE shipments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    carrier VARCHAR(100),
    tracking_number VARCHAR(150),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    scheduled_from DATETIME(6),
    scheduled_to DATETIME(6),
    shipped_at DATETIME(6),
    delivered_at DATETIME(6),
    installation_required BOOLEAN NOT NULL DEFAULT FALSE,
    installation_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED',
    installation_scheduled_at DATETIME(6),
    installation_completed_at DATETIME(6),
    installation_staff_id BIGINT,
    note TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_shipments_id_order UNIQUE(id,order_id),
    CONSTRAINT uq_shipments_carrier_tracking UNIQUE(carrier,tracking_number),
    CONSTRAINT ck_shipments_status CHECK(status IN ('PENDING','READY','IN_TRANSIT','DELIVERED','FAILED','RETURNED','CANCELLED')),
    CONSTRAINT ck_shipments_schedule CHECK(scheduled_to IS NULL OR (scheduled_from IS NOT NULL AND scheduled_to>=scheduled_from)),
    CONSTRAINT ck_shipments_installation CHECK((installation_required=0 AND installation_status='NOT_REQUIRED') OR (installation_required=1 AND installation_status IN ('PENDING','SCHEDULED','COMPLETED','CANCELLED'))),
    CONSTRAINT fk_shipments_order FOREIGN KEY(order_id) REFERENCES orders(id),
    CONSTRAINT fk_shipments_warehouse FOREIGN KEY(warehouse_id) REFERENCES warehouses(id),
    CONSTRAINT fk_shipments_installer FOREIGN KEY(installation_staff_id) REFERENCES users(id),
    KEY idx_shipments_order_status(order_id,status),
    KEY idx_shipments_schedule(status,scheduled_from)
) ENGINE=InnoDB;

CREATE TABLE shipment_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT uq_shipment_items_line UNIQUE(shipment_id,order_item_id),
    CONSTRAINT ck_shipment_items_quantity CHECK(quantity>0),
    CONSTRAINT fk_shipment_items_shipment FOREIGN KEY(shipment_id,order_id) REFERENCES shipments(id,order_id),
    CONSTRAINT fk_shipment_items_order_item FOREIGN KEY(order_item_id,order_id) REFERENCES order_items(id,order_id)
) ENGINE=InnoDB;

CREATE TABLE return_requests (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    return_number VARCHAR(40) NOT NULL,
    order_id BIGINT NOT NULL,
    requested_by BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    reason TEXT NOT NULL,
    resolution VARCHAR(20) NOT NULL DEFAULT 'REFUND',
    staff_note TEXT,
    approved_by BIGINT,
    requested_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at DATETIME(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_return_requests_number UNIQUE(return_number),
    CONSTRAINT uq_return_requests_id_order UNIQUE(id,order_id),
    CONSTRAINT ck_returns_status CHECK(status IN ('REQUESTED','APPROVED','REJECTED','IN_TRANSIT','RECEIVED','COMPLETED','CANCELLED')),
    CONSTRAINT ck_returns_resolution CHECK(resolution IN ('REFUND','EXCHANGE','REPAIR')),
    CONSTRAINT fk_returns_order FOREIGN KEY(order_id) REFERENCES orders(id),
    CONSTRAINT fk_returns_requester FOREIGN KEY(requested_by) REFERENCES users(id),
    CONSTRAINT fk_returns_approver FOREIGN KEY(approved_by) REFERENCES users(id),
    KEY idx_returns_order(order_id,status)
) ENGINE=InnoDB;

CREATE TABLE return_items (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    return_request_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    received_quantity INT NOT NULL DEFAULT 0,
    restocked_quantity INT NOT NULL DEFAULT 0,
    reason VARCHAR(1000) NOT NULL,
    condition_note TEXT,
    replacement_order_item_id BIGINT,
    CONSTRAINT uq_return_items_line UNIQUE(return_request_id,order_item_id),
    CONSTRAINT ck_return_items_quantities CHECK(quantity>0 AND received_quantity>=0 AND received_quantity<=quantity AND restocked_quantity>=0 AND restocked_quantity<=received_quantity),
    CONSTRAINT fk_return_items_request FOREIGN KEY(return_request_id,order_id) REFERENCES return_requests(id,order_id),
    CONSTRAINT fk_return_items_order_item FOREIGN KEY(order_item_id,order_id) REFERENCES order_items(id,order_id),
    CONSTRAINT fk_return_items_replacement FOREIGN KEY(replacement_order_item_id) REFERENCES order_items(id)
) ENGINE=InnoDB;

CREATE TABLE refunds (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    return_request_id BIGINT,
    provider VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    provider_refund_id VARCHAR(150) CHARACTER SET ascii COLLATE ascii_bin,
    idempotency_key VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(1000) NOT NULL,
    processed_by BIGINT,
    completed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_refunds_key UNIQUE(idempotency_key),
    CONSTRAINT uq_refunds_provider_tx UNIQUE(provider,provider_refund_id),
    CONSTRAINT ck_refunds_amount CHECK(amount>0),
    CONSTRAINT ck_refunds_status CHECK(status IN ('PENDING','SUCCEEDED','FAILED','CANCELLED')),
    CONSTRAINT fk_refunds_payment FOREIGN KEY(payment_id,order_id,currency) REFERENCES payments(id,order_id,currency),
    CONSTRAINT fk_refunds_return FOREIGN KEY(return_request_id,order_id) REFERENCES return_requests(id,order_id),
    CONSTRAINT fk_refunds_actor FOREIGN KEY(processed_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE reviews (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    title VARCHAR(200),
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    staff_reply TEXT,
    replied_by BIGINT,
    replied_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_reviews_order_item UNIQUE(order_item_id),
    CONSTRAINT ck_reviews_rating CHECK(rating BETWEEN 1 AND 5),
    CONSTRAINT ck_reviews_status CHECK(status IN ('PENDING','APPROVED','REJECTED','HIDDEN')),
    CONSTRAINT fk_reviews_buyer FOREIGN KEY(order_id,user_id) REFERENCES orders(id,user_id),
    CONSTRAINT fk_reviews_item FOREIGN KEY(order_item_id,order_id,product_id) REFERENCES order_items(id,order_id,product_id),
    CONSTRAINT fk_reviews_replier FOREIGN KEY(replied_by) REFERENCES users(id),
    KEY idx_reviews_product_status(product_id,status,created_at)
) ENGINE=InnoDB;

CREATE TABLE review_images (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    url VARCHAR(1000) NOT NULL,
    alt_text VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_review_images_review FOREIGN KEY(review_id) REFERENCES reviews(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE warranty_requests (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_number VARCHAR(40) NOT NULL,
    order_item_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    requested_by BIGINT,
    contact_name VARCHAR(150) NOT NULL,
    contact_phone VARCHAR(30) NOT NULL,
    service_address VARCHAR(1000),
    quantity INT NOT NULL DEFAULT 1,
    issue_description TEXT NOT NULL,
    evidence_urls JSON,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    assigned_to BIGINT,
    scheduled_at DATETIME(6),
    resolution_note TEXT,
    resolved_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_warranty_number UNIQUE(request_number),
    CONSTRAINT ck_warranty_quantity CHECK(quantity>0),
    CONSTRAINT ck_warranty_status CHECK(status IN ('REQUESTED','APPROVED','REJECTED','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT fk_warranty_item FOREIGN KEY(order_item_id,order_id) REFERENCES order_items(id,order_id),
    CONSTRAINT fk_warranty_requester FOREIGN KEY(requested_by) REFERENCES users(id),
    CONSTRAINT fk_warranty_assignee FOREIGN KEY(assigned_to) REFERENCES users(id),
    KEY idx_warranty_status_time(status,created_at)
) ENGINE=InnoDB;

CREATE TABLE tracking_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    session_id VARCHAR(100) NOT NULL,
    user_id BIGINT,
    product_id BIGINT,
    variant_id BIGINT,
    order_id BIGINT,
    category_id BIGINT,
    keyword VARCHAR(500),
    result_count BIGINT,
    quantity INT,
    page_path VARCHAR(500),
    metadata JSON,
    occurred_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_tracking_quantity CHECK(quantity IS NULL OR quantity>0),
    CONSTRAINT ck_tracking_results CHECK(result_count IS NULL OR result_count>=0),
    CONSTRAINT ck_tracking_variant_product CHECK(variant_id IS NULL OR product_id IS NOT NULL),
    CONSTRAINT fk_tracking_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE SET NULL,
    KEY idx_tracking_session_time(session_id,occurred_at),
    KEY idx_tracking_user_time(user_id,occurred_at),
    KEY idx_tracking_product_time(product_id,occurred_at),
    KEY idx_tracking_type_time(event_type,occurred_at)
) ENGINE=InnoDB;

CREATE TABLE banners (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(500),
    image_url VARCHAR(1000) NOT NULL,
    mobile_image_url VARCHAR(1000),
    alt_text VARCHAR(255),
    target_url VARCHAR(1000),
    placement VARCHAR(50) NOT NULL DEFAULT 'HOME_HERO',
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at DATETIME(6),
    ends_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT ck_banners_dates CHECK(ends_at IS NULL OR starts_at IS NULL OR ends_at>starts_at),
    KEY idx_banners_placement(placement,is_active,sort_order)
) ENGINE=InnoDB;

CREATE TABLE articles (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT NOT NULL,
    title VARCHAR(250) NOT NULL,
    slug VARCHAR(250) NOT NULL,
    summary VARCHAR(1000),
    content LONGTEXT NOT NULL,
    cover_image_url VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    seo_title VARCHAR(200),
    seo_description VARCHAR(320),
    published_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_articles_slug UNIQUE(slug),
    CONSTRAINT ck_articles_status CHECK(status IN ('DRAFT','PUBLISHED','ARCHIVED')),
    CONSTRAINT fk_articles_author FOREIGN KEY(author_id) REFERENCES users(id),
    KEY idx_articles_published(status,published_at)
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT,
    actor_name_snapshot VARCHAR(150),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(100),
    before_data JSON,
    after_data JSON,
    request_id VARCHAR(100),
    ip_address VARCHAR(45),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_actor FOREIGN KEY(actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    KEY idx_audit_entity(entity_type,entity_id,created_at),
    KEY idx_audit_actor_time(actor_user_id,created_at)
) ENGINE=InnoDB;

CREATE TABLE chat_conversations (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL DEFAULT (UUID()),
    user_id BIGINT,
    guest_token_hash BINARY(32),
    title VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_chat_public_id UNIQUE(public_id),
    CONSTRAINT ck_chat_owner CHECK((user_id IS NOT NULL AND guest_token_hash IS NULL) OR (user_id IS NULL AND guest_token_hash IS NOT NULL)),
    CONSTRAINT ck_chat_status CHECK(status IN ('OPEN','CLOSED','ARCHIVED')),
    CONSTRAINT fk_chat_user FOREIGN KEY(user_id) REFERENCES users(id),
    KEY idx_chat_user_time(user_id,updated_at)
) ENGINE=InnoDB;

CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content LONGTEXT NOT NULL,
    model VARCHAR(120),
    sources JSON,
    input_tokens INT,
    output_tokens INT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_chat_messages_sequence UNIQUE(conversation_id,sequence_number),
    CONSTRAINT ck_chat_messages_sequence CHECK(sequence_number>0),
    CONSTRAINT ck_chat_messages_role CHECK(role IN ('USER','ASSISTANT','SYSTEM','TOOL')),
    CONSTRAINT ck_chat_messages_tokens CHECK((input_tokens IS NULL OR input_tokens>=0) AND (output_tokens IS NULL OR output_tokens>=0)),
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY(conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE
) ENGINE=InnoDB;

INSERT INTO roles(code,name) VALUES
('ADMIN','Quản trị viên'),('STAFF','Nhân viên'),('CUSTOMER','Khách hàng');
INSERT INTO warehouses(code,name) VALUES('MAIN','Kho chính');

-- Vector-store tables are deliberately not hand-written here. Configure and
-- migrate the Spring AI-managed vector schema during the application update.
