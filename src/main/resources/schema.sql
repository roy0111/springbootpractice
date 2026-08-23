-- Schema for Product Catalog (R2DBC / H2)
-- This file replaces Hibernate's create-drop DDL auto-generation.
-- Spring Boot picks this up via: spring.sql.init.mode=always

CREATE TABLE IF NOT EXISTS products (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100)    NOT NULL,
    description    VARCHAR(500),
    price          DECIMAL(10, 2)  NOT NULL,
    category       VARCHAR(50)     NOT NULL,
    stock_quantity INT             NOT NULL DEFAULT 0,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tags (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(50) NOT NULL UNIQUE,
    color VARCHAR(20) NOT NULL DEFAULT '#000000'
);

