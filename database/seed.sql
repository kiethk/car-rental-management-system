-- Full Seed Data for CRMS (all 13 tables)
-- Demo password for all seed accounts: 123456

BEGIN;

-- 1. CUSTOMERS
INSERT INTO customers (customer_id, name, phone, email, password_hash, created_at)
VALUES 
    (1, 'John Doe', '0901234567', 'customer@gmail.com', '$2a$10$f1dC/wkm8nQT1k3YNvBP3e75TsnTOcT4JaMR6LcS.KsJ5nBC0w.nW', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    (2, 'Jane Smith', '0912345678', 'customer2@gmail.com', '$2a$10$f1dC/wkm8nQT1k3YNvBP3e75TsnTOcT4JaMR6LcS.KsJ5nBC0w.nW', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (3, 'David Miller', '0923456789', 'customer3@gmail.com', '$2a$10$f1dC/wkm8nQT1k3YNvBP3e75TsnTOcT4JaMR6LcS.KsJ5nBC0w.nW', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (customer_id) DO NOTHING;

-- 2. EMPLOYEES
-- role: 'admin' | 'staff'
INSERT INTO employees (employee_id, name, phone, email, password_hash, role)
VALUES 
    (1, 'Admin Manager', '0988888888', 'admin@crms.com', '$2a$10$f1dC/wkm8nQT1k3YNvBP3e75TsnTOcT4JaMR6LcS.KsJ5nBC0w.nW', 'admin'),
    (2, 'Support Staff', '0977777777', 'staff@crms.com', '$2a$10$f1dC/wkm8nQT1k3YNvBP3e75TsnTOcT4JaMR6LcS.KsJ5nBC0w.nW', 'staff')
ON CONFLICT (employee_id) DO NOTHING;

-- 3. VEHICLE_TYPES (20 diverse vehicle models with high quality online images)
INSERT INTO vehicle_types (vehicle_type_id, name, brand, category, seats, transmission, fuel_type, price_per_hour, description, image_url)
VALUES 
    (1, 'Vios 2023', 'Toyota', 'sedan', 5, 'automatic', 'gasoline', 150000.00, 'Compact 5-seater sedan, fuel-efficient, ideal for city commute.', '/images/vehicles/vios.jpg'),
    (2, 'CR-V 2024', 'Honda', 'suv', 7, 'automatic', 'gasoline', 300000.00, 'Spacious 7-seater SUV, perfect for family road trips.', '/images/vehicles/crv.jpg'),
    (3, 'VF8 2023', 'VinFast', 'suv', 5, 'automatic', 'electric', 280000.00, 'Smart D-segment electric SUV, premium ride with advanced ADAS technology.', '/images/vehicles/vf8.jpg'),
    (4, 'Morning 2023', 'Kia', 'hatchback', 4, 'automatic', 'gasoline', 120000.00, 'Compact urban hatchback, agile in city traffic, ultra-economical rental.', '/images/vehicles/morning.jpg'),
    (5, 'Accent 2024', 'Hyundai', 'sedan', 5, 'automatic', 'gasoline', 160000.00, 'Next-generation modern sedan, well-equipped and comfortable on all journeys.', '/images/vehicles/accent.jpg'),
    (6, 'Mazda 3 2023', 'Mazda', 'sedan', 5, 'automatic', 'gasoline', 220000.00, 'Sporty KODO styling, near-luxury interior and exhilarating handling.', '/images/vehicles/mazda3.jpg'),
    (7, 'Ranger Wildtrak 2023', 'Ford', 'pickup', 5, 'automatic', 'diesel', 320000.00, 'Powerful pickup truck with 4WD, ready to conquer any rugged terrain.', '/images/vehicles/ranger.jpg'),
    (8, 'Xpander 2024', 'Mitsubishi', 'minivan', 7, 'automatic', 'gasoline', 240000.00, 'Top-selling 7-seater MPV, high ground clearance, versatile family interior.', '/images/vehicles/xpander.jpg'),
    (9, 'Fortuner Legender', 'Toyota', 'suv', 7, 'automatic', 'diesel', 350000.00, 'Rugged full-size 7-seater SUV with superior off-road capabilities.', '/images/vehicles/fortuner.jpg'),
    (10, 'Santa Fe 2023', 'Hyundai', 'suv', 7, 'automatic', 'diesel', 360000.00, 'Luxurious stylish SUV with premium appointments and Harman Kardon audio.', '/images/vehicles/santafe.jpg'),
    (11, 'Carnival 2024', 'Kia', 'minivan', 8, 'automatic', 'diesel', 450000.00, 'Spacious 8-seater luxury MPV, captain seats designed for long distance comfort.', '/images/vehicles/carnival.jpg'),
    (12, 'VF9 Plus 2024', 'VinFast', 'suv', 7, 'automatic', 'electric', 480000.00, 'Flagship full-size electric SUV with premium leather interior and autonomous driving tech.', '/images/vehicles/vf9.jpg'),
    (13, 'Camry 2.5Q 2023', 'Toyota', 'sedan', 5, 'automatic', 'gasoline', 350000.00, 'Refined executive sedan, quiet cabin, executive rear seating.', '/images/vehicles/camry.jpg'),
    (14, 'Civic RS 2023', 'Honda', 'sedan', 5, 'automatic', 'gasoline', 250000.00, 'Sporty sedan with turbocharged VTEC performance and responsive steering.', '/images/vehicles/civic.jpg'),
    (15, '320i M Sport', 'BMW', 'sedan', 5, 'automatic', 'gasoline', 500000.00, 'German luxury sports sedan with M Sport package and perfect 50:50 chassis balance.', '/images/vehicles/bmw320i.jpg'),
    (16, 'C200 Avantgarde', 'Mercedes-Benz', 'sedan', 5, 'automatic', 'gasoline', 520000.00, 'Elegant Mercedes-Benz sedan, 11.9-inch portrait touchscreen, 64-color ambient lighting.', '/images/vehicles/c200.jpg'),
    (17, 'Macan 2023', 'Porsche', 'suv', 5, 'automatic', 'gasoline', 850000.00, 'High-performance luxury sports SUV from Stuttgart with pure sports car DNA.', '/images/vehicles/macan.jpg'),
    (18, 'A6 2023', 'Audi', 'sedan', 5, 'automatic', 'gasoline', 600000.00, 'High-tech executive sedan, Matrix LED lighting, dual touchscreen cockpit.', '/images/vehicles/a6.jpg'),
    (19, 'VF e34', 'VinFast', 'suv', 5, 'automatic', 'electric', 180000.00, 'Eco-friendly electric crossover, low operating cost and smart connected features.', '/images/vehicles/vfe34.jpg'),
    (20, 'Corolla Cross Hybrid', 'Toyota', 'suv', 5, 'automatic', 'hybrid', 270000.00, 'Ultra-quiet and efficient petrol-electric hybrid with Toyota Safety Sense.', '/images/vehicles/corollacross.jpg')
ON CONFLICT (vehicle_type_id) DO UPDATE SET 
    name = EXCLUDED.name, brand = EXCLUDED.brand, category = EXCLUDED.category, seats = EXCLUDED.seats, 
    transmission = EXCLUDED.transmission, fuel_type = EXCLUDED.fuel_type, price_per_hour = EXCLUDED.price_per_hour, 
    description = EXCLUDED.description, image_url = EXCLUDED.image_url;

-- 4. LICENSE_PLATES
INSERT INTO license_plates (license_plate_id, plate, province, registration_expiry)
VALUES 
    (1, '51A-123.45', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '1 year'),
    (2, '51B-678.90', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '1 year'),
    (3, '29A-999.88', 'Hanoi', CURRENT_DATE + INTERVAL '1 year'),
    (4, '43A-555.66', 'Da Nang', CURRENT_DATE - INTERVAL '5 days'), -- Expired registration
    (5, '51K-111.22', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '15 days'), -- Expiring soon in <= 30 days
    (6, '51K-333.44', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '6 months'),
    (7, '60A-888.99', 'Dong Nai', CURRENT_DATE + INTERVAL '1 year'),
    (8, '61A-777.66', 'Binh Duong', CURRENT_DATE + INTERVAL '1 year'),
    (9, '51H-246.80', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '8 months'),
    (10, '30H-135.79', 'Hanoi', CURRENT_DATE + INTERVAL '10 months'),
    (11, '51F-999.99', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '2 years'),
    (12, '51L-888.88', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '18 months'),
    (13, '29B-456.78', 'Hanoi', CURRENT_DATE + INTERVAL '1 year'),
    (14, '43B-123.89', 'Da Nang', CURRENT_DATE + INTERVAL '1 year'),
    (15, '51K-902.10', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '1 year'),
    (16, '51K-777.77', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '1 year'),
    (17, '51A-899.99', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '2 years'),
    (18, '30A-666.88', 'Hanoi', CURRENT_DATE + INTERVAL '1 year'),
    (19, '51E-234.56', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '1 year'),
    (20, '60B-345.67', 'Dong Nai', CURRENT_DATE + INTERVAL '1 year')
ON CONFLICT (license_plate_id) DO NOTHING;

-- 5. VEHICLES
-- status: 'available' | 'rented' | 'maintenance'
INSERT INTO vehicles (vehicle_id, vehicle_type_id, license_plate_id, color, year, odometer, status)
VALUES 
    (1, 1, 1, 'White', 2023, 12000, 'available'),
    (2, 1, 2, 'Black', 2023, 15400, 'rented'),
    (3, 2, 3, 'Silver', 2024, 8500, 'available'),
    (4, 3, 4, 'Blue', 2023, 18000, 'maintenance'),
    (5, 4, 5, 'Red', 2023, 21000, 'available'),
    (6, 5, 6, 'Pearl White', 2024, 6200, 'available'),
    (7, 6, 7, 'Cement Gray', 2023, 14500, 'available'),
    (8, 7, 8, 'Wildtrak Orange', 2023, 28000, 'available'),
    (9, 8, 9, 'White', 2024, 9800, 'available'),
    (10, 9, 10, 'Midnight Black', 2023, 31000, 'available'),
    (11, 10, 11, 'Golden Brown', 2023, 19200, 'available'),
    (12, 11, 12, 'Snow White', 2024, 4300, 'available'),
    (13, 12, 13, 'Moss Green', 2024, 5100, 'available'),
    (14, 13, 14, 'Black', 2023, 17800, 'available'),
    (15, 14, 15, 'Sonic Red', 2023, 13200, 'available'),
    (16, 15, 16, 'Portimao Blue', 2023, 11500, 'available'),
    (17, 16, 17, 'Iridium Silver', 2023, 9000, 'available'),
    (18, 17, 18, 'Matte Volcano Black', 2023, 8200, 'available'),
    (19, 18, 19, 'Glacier White', 2023, 14000, 'available'),
    (20, 19, 20, 'Deep Ocean Blue', 2023, 16500, 'available')
ON CONFLICT (vehicle_id) DO NOTHING;

-- 6. PROMOTIONS
-- discount_type: 'percent' | 'fixed'
INSERT INTO promotions (promotion_id, code, discount_type, discount_value, start_date, end_date, quantity)
VALUES 
    (1, 'WELCOME10', 'percent', 10.00, CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '30 days', 100),
    (2, 'GIAM100K', 'fixed', 100000.00, CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE + INTERVAL '20 days', 50)
ON CONFLICT (promotion_id) DO NOTHING;

-- 7. LICENSE_DOCUMENTS
-- status: 'pending' | 'approved' | 'rejected'
INSERT INTO license_documents (license_document_id, customer_id, reviewed_by, license_number, license_class, license_expiry, license_image_url, status, rejection_reason, submitted_at, reviewed_at)
VALUES 
    (1, 1, 2, 'B2-79123456', 'B2', CURRENT_DATE + INTERVAL '5 years', '/uploads/licenses/cust1_license.jpg', 'approved', NULL, CURRENT_TIMESTAMP - INTERVAL '8 days', CURRENT_TIMESTAMP - INTERVAL '7 days'),
    (2, 2, 2, 'B1-88123999', 'B1', CURRENT_DATE + INTERVAL '3 years', '/uploads/licenses/cust2_license.jpg', 'approved', NULL, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (3, 3, NULL, 'B2-99887766', 'B2', CURRENT_DATE + INTERVAL '8 years', '/uploads/licenses/cust3_license.jpg', 'pending', NULL, CURRENT_TIMESTAMP - INTERVAL '1 days', NULL)
ON CONFLICT (license_document_id) DO NOTHING;

-- 8. SLOTS
-- status: 'open' | 'closed'
INSERT INTO slots (slot_id, vehicle_type_id, start_time, end_time, capacity, status)
VALUES 
    (1, 1, (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '8 hours'), (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '18 hours'), 5, 'open'),
    (2, 1, (CURRENT_DATE + INTERVAL '2 days' + INTERVAL '8 hours'), (CURRENT_DATE + INTERVAL '2 days' + INTERVAL '18 hours'), 5, 'open'),
    (3, 2, (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '8 hours'), (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '20 hours'), 3, 'open'),
    (4, 3, (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '8 hours'), (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '18 hours'), 2, 'closed')
ON CONFLICT (slot_id) DO NOTHING;

-- 9. BOOKINGS
-- status: 'pending_payment' | 'confirmed' | 'active' | 'completed' | 'cancelled'
INSERT INTO bookings (
    booking_id, customer_id, slot_id, vehicle_id, employee_id, promotion_id,
    pickup_time, scheduled_end_time, actual_return_time,
    late_fee, total_price,
    status, created_at,
    price_per_hour, deposit_amount, discount_amount
)
VALUES 
    -- Booking 1: Completed
    (
        1, 1, 1, 1, 2, 1,
        CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days',
        0.00, 1350000.00,
        'completed', CURRENT_TIMESTAMP - INTERVAL '5 days',
        150000.00, 5000000.00, 150000.00
    ),
    -- Booking 2: Active rental
    (
        2, 2, 1, 2, 2, NULL,
        CURRENT_TIMESTAMP - INTERVAL '4 hours', CURRENT_TIMESTAMP + INTERVAL '6 hours', NULL,
        0.00, 1500000.00,
        'active', CURRENT_TIMESTAMP - INTERVAL '1 day',
        150000.00, 5000000.00, 0.00
    ),
    -- Booking 3: Confirmed
    (
        3, 1, 3, 3, 2, 2,
        CURRENT_TIMESTAMP + INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '1 day 10 hours', NULL,
        0.00, 2900000.00,
        'confirmed', CURRENT_TIMESTAMP - INTERVAL '2 hours',
        300000.00, 8000000.00, 100000.00
    )
ON CONFLICT (booking_id) DO NOTHING;

-- 10. PAYMENTS
-- payment_type: 'DEPOSIT' | 'FINAL' | 'REFUND'
-- status: 'pending' | 'paid' | 'failed'
INSERT INTO payments (payment_id, booking_id, payment_type, amount, status, paid_at)
VALUES 
    (1, 1, 'DEPOSIT', 5000000.00, 'paid', CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (2, 1, 'FINAL', 1350000.00, 'paid', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 1, 'REFUND', 5000000.00, 'paid', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (4, 2, 'DEPOSIT', 5000000.00, 'paid', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (5, 3, 'DEPOSIT', 8000000.00, 'paid', CURRENT_TIMESTAMP - INTERVAL '1 hour')
ON CONFLICT (payment_id) DO NOTHING;

-- 11. REVIEWS
-- rating: 1..5
INSERT INTO reviews (review_id, booking_id, rating, comment, created_at)
VALUES 
    (1, 1, 5, 'Clean car, smooth handover, very satisfied!', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (review_id) DO NOTHING;

-- 12. VEHICLE_INSPECTIONS
-- status: 'scheduled' | 'passed' | 'failed'
INSERT INTO vehicle_inspections (vehicle_inspection_id, vehicle_id, scheduled_at, completed_at, status, notes)
VALUES 
    (1, 1, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days', 'passed', 'Routine 10,000km periodic inspection, vehicle in optimal condition.'),
    (2, 4, CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'scheduled', 'Scheduled EV battery and ADAS sensor system diagnostic.')
ON CONFLICT (vehicle_inspection_id) DO NOTHING;

-- 13. ISSUES
-- issue_type, status: 'open' | 'in_progress' | 'resolved'
INSERT INTO issues (issue_id, booking_id, slot_id, vehicle_id, employee_id, issue_type, subject, status, fine_amount, evidence_url, reported_at)
VALUES 
    (1, 1, 1, 1, 2, 'SCRATCH', 'Minor scratch on front bumper', 'resolved', 200000.00, '/uploads/issues/issue_1.jpg', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (issue_id) DO NOTHING;

-- Sequence updates
SELECT setval(pg_get_serial_sequence('customers', 'customer_id'), COALESCE(MAX(customer_id), 1)) FROM customers;
SELECT setval(pg_get_serial_sequence('employees', 'employee_id'), COALESCE(MAX(employee_id), 1)) FROM employees;
SELECT setval(pg_get_serial_sequence('vehicle_types', 'vehicle_type_id'), COALESCE(MAX(vehicle_type_id), 1)) FROM vehicle_types;
SELECT setval(pg_get_serial_sequence('license_plates', 'license_plate_id'), COALESCE(MAX(license_plate_id), 1)) FROM license_plates;
SELECT setval(pg_get_serial_sequence('vehicles', 'vehicle_id'), COALESCE(MAX(vehicle_id), 1)) FROM vehicles;
SELECT setval(pg_get_serial_sequence('promotions', 'promotion_id'), COALESCE(MAX(promotion_id), 1)) FROM promotions;
SELECT setval(pg_get_serial_sequence('license_documents', 'license_document_id'), COALESCE(MAX(license_document_id), 1)) FROM license_documents;
SELECT setval(pg_get_serial_sequence('slots', 'slot_id'), COALESCE(MAX(slot_id), 1)) FROM slots;
SELECT setval(pg_get_serial_sequence('bookings', 'booking_id'), COALESCE(MAX(booking_id), 1)) FROM bookings;
SELECT setval(pg_get_serial_sequence('payments', 'payment_id'), COALESCE(MAX(payment_id), 1)) FROM payments;
SELECT setval(pg_get_serial_sequence('reviews', 'review_id'), COALESCE(MAX(review_id), 1)) FROM reviews;
SELECT setval(pg_get_serial_sequence('vehicle_inspections', 'vehicle_inspection_id'), COALESCE(MAX(vehicle_inspection_id), 1)) FROM vehicle_inspections;
SELECT setval(pg_get_serial_sequence('issues', 'issue_id'), COALESCE(MAX(issue_id), 1)) FROM issues;

COMMIT;

