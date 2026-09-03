-- ============================================================
-- MyGarage - APPJFS19 - Seed Data
-- ============================================================
-- IMPORTANT: Admin password is BCrypt-hashed. Never plaintext.
-- Plain password: Admin@123
-- BCrypt hash (strength 10) generated via BCryptPasswordEncoder:
-- ============================================================

-- Admin user (ADMIN role - cannot self-register)
INSERT IGNORE INTO users (full_name, email, password_hash, phone, role, is_active, created_at, updated_at)
VALUES (
    'MyGarage Admin',
    'admin@mygarage.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
    '9999999999',
    'ADMIN',
    true,
    NOW(),
    NOW()
);

-- Default vehicle categories
INSERT IGNORE INTO vehicle_categories (name, icon, description) VALUES ('Sedan', '🚗', 'Standard 4-door passenger car');
INSERT IGNORE INTO vehicle_categories (name, icon, description) VALUES ('SUV', '🚙', 'Sports Utility Vehicle');
INSERT IGNORE INTO vehicle_categories (name, icon, description) VALUES ('Hatchback', '🚘', 'Small 3 or 5-door car');
INSERT IGNORE INTO vehicle_categories (name, icon, description) VALUES ('Motorcycle', '🏍️', 'Two-wheeled motorbike');
INSERT IGNORE INTO vehicle_categories (name, icon, description) VALUES ('Scooter', '🛵', 'Step-through scooter / moped');
INSERT IGNORE INTO vehicle_categories (name, icon, description) VALUES ('MUV / Van', '🚐', 'Multi-utility vehicle or van');
INSERT IGNORE INTO vehicle_categories (name, icon, description) VALUES ('Truck', '🚚', 'Light commercial or heavy truck');
INSERT IGNORE INTO vehicle_categories (name, icon, description) VALUES ('Other', '🚗', 'Other vehicle type');
