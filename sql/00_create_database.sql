-- 仅建库（本地已有 MySQL、不用 Docker 时使用）
-- mysql -u root -p < sql/00_create_database.sql

CREATE DATABASE IF NOT EXISTS aios
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
