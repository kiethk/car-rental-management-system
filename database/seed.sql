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
    (1, 'Vios', 'Toyota', 'sedan', 5, 'automatic', 'gasoline', 150000.00, 'Compact 5-seater sedan, fuel-efficient, ideal for city commute.', '/images/vehicles/vios.jpg'),
    (2, 'CR-V', 'Honda', 'suv', 7, 'automatic', 'gasoline', 300000.00, 'Spacious 7-seater SUV, perfect for family road trips.', '/images/vehicles/crv.jpg'),
    (3, 'VF8', 'VinFast', 'suv', 5, 'automatic', 'electric', 280000.00, 'Smart D-segment electric SUV, premium ride with advanced ADAS technology.', '/images/vehicles/vf8.jpg'),
    (4, 'Morning', 'Kia', 'hatchback', 4, 'automatic', 'gasoline', 120000.00, 'Compact urban hatchback, agile in city traffic, ultra-economical rental.', '/images/vehicles/morning.jpg'),
    (5, 'Accent', 'Hyundai', 'sedan', 5, 'automatic', 'gasoline', 160000.00, 'Next-generation modern sedan, well-equipped and comfortable on all journeys.', '/images/vehicles/accent.jpg'),
    (6, 'Mazda 3', 'Mazda', 'sedan', 5, 'automatic', 'gasoline', 220000.00, 'Sporty KODO styling, near-luxury interior and exhilarating handling.', '/images/vehicles/mazda3.jpg'),
    (7, 'Ranger', 'Ford', 'pickup', 5, 'automatic', 'diesel', 320000.00, 'Powerful pickup truck with 4WD, ready to conquer any rugged terrain.', '/images/vehicles/ranger.jpg'),
    (8, 'Xpander', 'Mitsubishi', 'minivan', 7, 'automatic', 'gasoline', 240000.00, 'Top-selling 7-seater MPV, high ground clearance, versatile family interior.', '/images/vehicles/xpander.jpg'),
    (9, 'Fortuner', 'Toyota', 'suv', 7, 'automatic', 'diesel', 350000.00, 'Rugged full-size 7-seater SUV with superior off-road capabilities.', '/images/vehicles/fortuner.jpg'),
    (10, 'Santa Fe', 'Hyundai', 'suv', 7, 'automatic', 'diesel', 360000.00, 'Luxurious stylish SUV with premium appointments and Harman Kardon audio.', '/images/vehicles/santafe.jpg'),
    (11, 'Carnival', 'Kia', 'minivan', 8, 'automatic', 'diesel', 450000.00, 'Spacious 8-seater luxury MPV, captain seats designed for long distance comfort.', '/images/vehicles/carnival.jpg'),
    (12, 'VF9', 'VinFast', 'suv', 7, 'automatic', 'electric', 480000.00, 'Flagship full-size electric SUV with premium leather interior and autonomous driving tech.', '/images/vehicles/vf9.jpg'),
    (13, 'Camry', 'Toyota', 'sedan', 5, 'automatic', 'gasoline', 350000.00, 'Refined executive sedan, quiet cabin, executive rear seating.', '/images/vehicles/camry.jpg'),
    (14, 'Civic', 'Honda', 'sedan', 5, 'automatic', 'gasoline', 250000.00, 'Sporty sedan with turbocharged VTEC performance and responsive steering.', '/images/vehicles/civic.jpg'),
    (15, '320i', 'BMW', 'sedan', 5, 'automatic', 'gasoline', 500000.00, 'German luxury sports sedan with M Sport package and perfect 50:50 chassis balance.', '/images/vehicles/bmw320i.jpg'),
    (16, 'C200', 'Mercedes-Benz', 'sedan', 5, 'automatic', 'gasoline', 520000.00, 'Elegant Mercedes-Benz sedan, 11.9-inch portrait touchscreen, 64-color ambient lighting.', '/images/vehicles/c200.jpg'),
    (17, 'Macan', 'Porsche', 'suv', 5, 'automatic', 'gasoline', 850000.00, 'High-performance luxury sports SUV from Stuttgart with pure sports car DNA.', '/images/vehicles/macan.jpg'),
    (18, 'A6', 'Audi', 'sedan', 5, 'automatic', 'gasoline', 600000.00, 'High-tech executive sedan, Matrix LED lighting, dual touchscreen cockpit.', '/images/vehicles/a6.jpg'),
    (19, 'VF e34', 'VinFast', 'suv', 5, 'automatic', 'electric', 180000.00, 'Eco-friendly electric crossover, low operating cost and smart connected features.', '/images/vehicles/vfe34.jpg'),
    (20, 'Corolla Cross', 'Toyota', 'suv', 5, 'automatic', 'hybrid', 270000.00, 'Ultra-quiet and efficient petrol-electric hybrid with Toyota Safety Sense.', '/images/vehicles/corollacross.jpg')
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

-- 10. CONTRACT_TEMPLATES
-- status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED'. Numbers live in typed columns; terms_content holds wording only.
INSERT INTO contract_templates (contract_template_id, version_no, title, terms_content,
                                deposit_rate, late_fee_multiplier, free_cancellation_hours,
                                status, created_by, created_at, activated_at)
VALUES
    (1, 1, 'Hợp đồng thuê xe ô tô tự lái',
     E'Quyền và nghĩa vụ của Bên A\n'
     || E'1. Bàn giao xe đúng thời gian và địa điểm đã thỏa thuận; xe ở tình trạng kỹ thuật tốt, có đủ giấy đăng ký, giấy chứng nhận kiểm định và bảo hiểm trách nhiệm dân sự bắt buộc còn hiệu lực.\n'
     || E'2. Cùng Bên B lập biên bản bàn giao, ghi nhận số km, mức nhiên liệu và tình trạng ngoại thất, nội thất khi giao và khi nhận lại xe.\n'
     || E'3. Chịu chi phí bảo dưỡng định kỳ và các hư hỏng do hao mòn tự nhiên của xe.\n'
     || E'\nQuyền và nghĩa vụ của Bên B\n'
     || E'4. Xuất trình bản gốc căn cước công dân và giấy phép lái xe phù hợp với hạng xe khi nhận xe.\n'
     || E'5. Sử dụng xe đúng mục đích, tuân thủ pháp luật về giao thông đường bộ và chịu trách nhiệm về mọi vi phạm giao thông, kể cả phạt nguội, phát sinh trong thời gian thuê.\n'
     || E'6. Không giao xe cho người khác điều khiển khi chưa được Bên A đồng ý; không cầm cố, thế chấp, cho thuê lại xe; không dùng xe để chở hàng cấm hoặc đua xe.\n'
     || E'7. Bồi thường thiệt hại khi xe bị hư hỏng hoặc mất mát do lỗi của Bên B, căn cứ biên bản bàn giao và chi phí sửa chữa thực tế.\n'
     || E'8. Trả xe đúng thời hạn, đúng địa điểm, với mức nhiên liệu như khi nhận; tự chi trả phí cầu đường, bến bãi phát sinh.',
     0.5000, 2.00, 72,
     'ACTIVE', 1, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days')
ON CONFLICT (contract_template_id) DO NOTHING;

-- 11. CONTRACTS
-- Contract validity follows bookings.status. file_name is NULL until the PDF is issued.
INSERT INTO contracts (contract_id, contract_number, booking_id, contract_template_id,
                       accepted_at, accepted_ip, file_name, issued_at, emailed_at, created_at)
VALUES
    (1, 'CTR-20261018-0001', 1, 1, CURRENT_TIMESTAMP - INTERVAL '4 days', '127.0.0.1', NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (2, 'CTR-20261021-0002', 2, 1, CURRENT_TIMESTAMP - INTERVAL '1 day', '127.0.0.1', NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (3, 'CTR-20261022-0003', 3, 1, CURRENT_TIMESTAMP - INTERVAL '1 hour', '127.0.0.1', NULL, NULL, NULL, CURRENT_TIMESTAMP - INTERVAL '1 hour')
ON CONFLICT (contract_id) DO NOTHING;

-- 12. PAYMENTS
-- payment_type: 'DEPOSIT' | 'FINAL' | 'REFUND'
-- status: 'pending' | 'paid' | 'failed'
INSERT INTO payments (payment_id, booking_id, transaction_code, payment_type, amount, payment_method, status, paid_at)
VALUES 
    (1, 1, 'TXN-DEP-1001', 'DEPOSIT', 5000000.00, 'VIETQR', 'paid', CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (2, 1, 'TXN-FIN-1002', 'FINAL', 1350000.00, 'VIETQR', 'paid', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 1, 'TXN-REF-1003', 'REFUND', 5000000.00, 'BANK_TRANSFER', 'paid', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (4, 2, 'TXN-DEP-2001', 'DEPOSIT', 5000000.00, 'VIETQR', 'paid', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (5, 3, 'TXN-DEP-3001', 'DEPOSIT', 8000000.00, 'VIETQR', 'paid', CURRENT_TIMESTAMP - INTERVAL '1 hour')
ON CONFLICT (payment_id) DO NOTHING;

-- 13. REVIEWS
-- rating: 1..5
INSERT INTO reviews (review_id, booking_id, rating, comment, created_at)
VALUES 
    (1, 1, 5, 'Clean car, smooth handover, very satisfied!', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (review_id) DO NOTHING;

-- 14. VEHICLE_INSPECTIONS
-- status: 'scheduled' | 'passed' | 'failed'
INSERT INTO vehicle_inspections (vehicle_inspection_id, vehicle_id, scheduled_at, completed_at, status, notes)
VALUES 
    (1, 1, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days', 'passed', 'Routine 10,000km periodic inspection, vehicle in optimal condition.'),
    (2, 4, CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'scheduled', 'Scheduled EV battery and ADAS sensor system diagnostic.')
ON CONFLICT (vehicle_inspection_id) DO NOTHING;

-- 15. ISSUES
-- issue_type, status: 'open' | 'in_progress' | 'resolved'
INSERT INTO issues (issue_id, booking_id, slot_id, vehicle_id, employee_id, issue_type, subject, status, evidence_url, reported_at)
VALUES 
    (1, 1, 1, 1, 2, 'SCRATCH', 'Minor scratch on front bumper', 'resolved', '/uploads/issues/issue_1.jpg', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (issue_id) DO NOTHING;

-- 16. BOOKING_FEES
-- fee_type: 'RENTAL' | 'LATE_FEE' | 'DAMAGE_COMPENSATION' | 'CLEANING_FEE' | 'TOLL_FEE'
INSERT INTO booking_fees (booking_fee_id, booking_id, fee_type, amount, description, issue_id, created_at)
VALUES
    (1, 1, 'DAMAGE_COMPENSATION', 200000.00, 'Compensation for front bumper scratch per incident #1', 1, CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (booking_fee_id) DO NOTHING;

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
SELECT setval(pg_get_serial_sequence('contract_templates', 'contract_template_id'), COALESCE(MAX(contract_template_id), 1)) FROM contract_templates;
SELECT setval(pg_get_serial_sequence('contracts', 'contract_id'), COALESCE(MAX(contract_id), 1)) FROM contracts;
SELECT setval(pg_get_serial_sequence('payments', 'payment_id'), COALESCE(MAX(payment_id), 1)) FROM payments;
SELECT setval(pg_get_serial_sequence('reviews', 'review_id'), COALESCE(MAX(review_id), 1)) FROM reviews;
SELECT setval(pg_get_serial_sequence('vehicle_inspections', 'vehicle_inspection_id'), COALESCE(MAX(vehicle_inspection_id), 1)) FROM vehicle_inspections;
SELECT setval(pg_get_serial_sequence('issues', 'issue_id'), COALESCE(MAX(issue_id), 1)) FROM issues;
SELECT setval(pg_get_serial_sequence('booking_fees', 'booking_fee_id'), COALESCE(MAX(booking_fee_id), 1)) FROM booking_fees;

COMMIT;


-- Additional physical vehicles per model for rich inventory

BEGIN;

INSERT INTO license_plates (license_plate_id, plate, province, registration_expiry)
VALUES
(21, '51L-125.45', 'Can Tho', CURRENT_DATE + INTERVAL '13 months'),
(22, '51L-704.64', 'Can Tho', CURRENT_DATE + INTERVAL '7 months'),
(23, '51K-674.35', 'Binh Duong', CURRENT_DATE + INTERVAL '23 months'),
(24, '51K-877.30', 'Da Nang', CURRENT_DATE + INTERVAL '19 months'),
(25, '43A-204.21', 'Hai Phong', CURRENT_DATE + INTERVAL '18 months'),
(26, '51K-847.68', 'Da Nang', CURRENT_DATE + INTERVAL '23 months'),
(27, '43A-691.34', 'Da Nang', CURRENT_DATE + INTERVAL '8 months'),
(28, '30K-987.22', 'Hai Phong', CURRENT_DATE + INTERVAL '18 months'),
(29, '43A-463.36', 'Hanoi', CURRENT_DATE + INTERVAL '14 months'),
(30, '51H-573.58', 'Hanoi', CURRENT_DATE + INTERVAL '14 months'),
(31, '51K-924.50', 'Hai Phong', CURRENT_DATE + INTERVAL '18 months'),
(32, '43A-317.93', 'Can Tho', CURRENT_DATE + INTERVAL '21 months'),
(33, '30K-862.81', 'Hanoi', CURRENT_DATE + INTERVAL '23 months'),
(34, '30K-241.75', 'Da Nang', CURRENT_DATE + INTERVAL '21 months'),
(35, '51H-911.97', 'Can Tho', CURRENT_DATE + INTERVAL '19 months'),
(36, '30L-666.11', 'Dong Nai', CURRENT_DATE + INTERVAL '9 months'),
(37, '51H-564.10', 'Dong Nai', CURRENT_DATE + INTERVAL '14 months'),
(38, '30K-256.57', 'Hai Phong', CURRENT_DATE + INTERVAL '11 months'),
(39, '43A-999.49', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '13 months'),
(40, '61B-935.18', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '23 months'),
(41, '30L-640.87', 'Hanoi', CURRENT_DATE + INTERVAL '19 months'),
(42, '43A-548.76', 'Can Tho', CURRENT_DATE + INTERVAL '20 months'),
(43, '51K-702.80', 'Da Nang', CURRENT_DATE + INTERVAL '13 months'),
(44, '51K-334.18', 'Can Tho', CURRENT_DATE + INTERVAL '7 months'),
(45, '61B-319.79', 'Da Nang', CURRENT_DATE + INTERVAL '10 months'),
(46, '51L-199.94', 'Hanoi', CURRENT_DATE + INTERVAL '19 months'),
(47, '51K-789.93', 'Hai Phong', CURRENT_DATE + INTERVAL '9 months'),
(48, '30K-294.78', 'Hanoi', CURRENT_DATE + INTERVAL '20 months'),
(49, '30K-995.19', 'Dong Nai', CURRENT_DATE + INTERVAL '20 months'),
(50, '30K-270.62', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '21 months'),
(51, '60A-102.59', 'Hanoi', CURRENT_DATE + INTERVAL '14 months'),
(52, '61B-258.34', 'Can Tho', CURRENT_DATE + INTERVAL '15 months'),
(53, '51K-865.50', 'Binh Duong', CURRENT_DATE + INTERVAL '7 months'),
(54, '51K-620.20', 'Hanoi', CURRENT_DATE + INTERVAL '11 months'),
(55, '30K-692.86', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '7 months'),
(56, '43A-367.36', 'Binh Duong', CURRENT_DATE + INTERVAL '16 months'),
(57, '30L-568.50', 'Can Tho', CURRENT_DATE + INTERVAL '8 months'),
(58, '51L-650.37', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '22 months'),
(59, '43A-391.30', 'Hanoi', CURRENT_DATE + INTERVAL '20 months'),
(60, '51L-999.27', 'Can Tho', CURRENT_DATE + INTERVAL '14 months'),
(61, '30L-719.36', 'Da Nang', CURRENT_DATE + INTERVAL '16 months'),
(62, '51K-194.91', 'Da Nang', CURRENT_DATE + INTERVAL '19 months'),
(63, '51H-752.43', 'Hai Phong', CURRENT_DATE + INTERVAL '11 months'),
(64, '51L-807.29', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '23 months'),
(65, '60A-230.15', 'Hanoi', CURRENT_DATE + INTERVAL '15 months'),
(66, '30K-782.23', 'Can Tho', CURRENT_DATE + INTERVAL '17 months'),
(67, '51H-522.13', 'Hai Phong', CURRENT_DATE + INTERVAL '11 months'),
(68, '51L-491.14', 'Hanoi', CURRENT_DATE + INTERVAL '21 months'),
(69, '30K-328.13', 'Da Nang', CURRENT_DATE + INTERVAL '12 months'),
(70, '30L-459.92', 'Hai Phong', CURRENT_DATE + INTERVAL '22 months'),
(71, '51H-694.43', 'Da Nang', CURRENT_DATE + INTERVAL '7 months'),
(72, '43A-546.87', 'Hai Phong', CURRENT_DATE + INTERVAL '22 months'),
(73, '51K-825.65', 'Da Nang', CURRENT_DATE + INTERVAL '6 months'),
(74, '43A-738.50', 'Can Tho', CURRENT_DATE + INTERVAL '9 months'),
(75, '30L-667.26', 'Dong Nai', CURRENT_DATE + INTERVAL '12 months'),
(76, '30L-515.80', 'Binh Duong', CURRENT_DATE + INTERVAL '6 months'),
(77, '43A-576.66', 'Hai Phong', CURRENT_DATE + INTERVAL '20 months'),
(78, '30L-627.94', 'Ho Chi Minh City', CURRENT_DATE + INTERVAL '16 months'),
(79, '30K-250.13', 'Hai Phong', CURRENT_DATE + INTERVAL '7 months'),
(80, '60A-744.83', 'Dong Nai', CURRENT_DATE + INTERVAL '12 months')
ON CONFLICT (license_plate_id) DO NOTHING;

INSERT INTO vehicles (vehicle_id, vehicle_type_id, license_plate_id, color, year, odometer, status)
VALUES
(21, 1, 21, 'Deep Blue', 2023, 9717, 'maintenance'),
(22, 1, 22, 'Pearl White', 2022, 17328, 'available'),
(23, 1, 23, 'Champagne Gold', 2023, 32439, 'available'),
(24, 2, 24, 'Ruby Red', 2024, 13189, 'available'),
(25, 2, 25, 'Metallic Black', 2024, 25541, 'available'),
(26, 2, 26, 'Metallic Black', 2025, 8164, 'available'),
(27, 3, 27, 'Pearl White', 2023, 21965, 'available'),
(28, 3, 28, 'Lunar Gray', 2025, 44660, 'available'),
(29, 3, 29, 'Metallic Black', 2023, 38005, 'maintenance'),
(30, 4, 30, 'Deep Blue', 2024, 6665, 'available'),
(31, 4, 31, 'Lunar Gray', 2022, 16826, 'available'),
(32, 4, 32, 'Champagne Gold', 2025, 12363, 'available'),
(33, 5, 33, 'Lunar Gray', 2025, 41242, 'rented'),
(34, 5, 34, 'Metallic Black', 2022, 10185, 'available'),
(35, 5, 35, 'Metallic Black', 2025, 28009, 'available'),
(36, 6, 36, 'Lunar Gray', 2024, 10310, 'available'),
(37, 6, 37, 'Silver Frost', 2022, 43979, 'available'),
(38, 6, 38, 'Pearl White', 2024, 35021, 'available'),
(39, 7, 39, 'Pearl White', 2023, 40182, 'available'),
(40, 7, 40, 'Silver Frost', 2023, 34148, 'available'),
(41, 7, 41, 'Deep Blue', 2023, 23428, 'rented'),
(42, 8, 42, 'Metallic Black', 2023, 17725, 'available'),
(43, 8, 43, 'Deep Blue', 2022, 7652, 'maintenance'),
(44, 8, 44, 'Ruby Red', 2022, 36695, 'available'),
(45, 9, 45, 'Midnight Black', 2023, 33996, 'rented'),
(46, 9, 46, 'Ruby Red', 2025, 29941, 'rented'),
(47, 9, 47, 'Pearl White', 2025, 25236, 'available'),
(48, 10, 48, 'Silver Frost', 2025, 15025, 'available'),
(49, 10, 49, 'Metallic Black', 2022, 38427, 'available'),
(50, 10, 50, 'Midnight Black', 2023, 29282, 'available'),
(51, 11, 51, 'Midnight Black', 2024, 30722, 'maintenance'),
(52, 11, 52, 'Deep Blue', 2022, 40957, 'maintenance'),
(53, 11, 53, 'Pearl White', 2025, 35954, 'available'),
(54, 12, 54, 'Metallic Black', 2022, 18414, 'rented'),
(55, 12, 55, 'Metallic Black', 2025, 41251, 'available'),
(56, 12, 56, 'Deep Blue', 2024, 28938, 'available'),
(57, 13, 57, 'Pearl White', 2025, 43708, 'available'),
(58, 13, 58, 'Lunar Gray', 2023, 25872, 'available'),
(59, 13, 59, 'Lunar Gray', 2022, 39346, 'available'),
(60, 14, 60, 'Metallic Black', 2022, 39256, 'available'),
(61, 14, 61, 'Deep Blue', 2024, 36122, 'rented'),
(62, 14, 62, 'Lunar Gray', 2022, 3232, 'available'),
(63, 15, 63, 'Midnight Black', 2025, 39759, 'available'),
(64, 15, 64, 'Pearl White', 2024, 41175, 'available'),
(65, 15, 65, 'Ruby Red', 2022, 26449, 'available'),
(66, 16, 66, 'Champagne Gold', 2023, 18514, 'available'),
(67, 16, 67, 'Ruby Red', 2025, 19263, 'available'),
(68, 16, 68, 'Deep Blue', 2023, 33166, 'available'),
(69, 17, 69, 'Champagne Gold', 2024, 21258, 'available'),
(70, 17, 70, 'Champagne Gold', 2024, 4808, 'available'),
(71, 17, 71, 'Metallic Black', 2025, 25654, 'maintenance'),
(72, 18, 72, 'Metallic Black', 2025, 40787, 'available'),
(73, 18, 73, 'Deep Blue', 2024, 31265, 'available'),
(74, 18, 74, 'Lunar Gray', 2024, 29764, 'available'),
(75, 19, 75, 'Champagne Gold', 2025, 14405, 'available'),
(76, 19, 76, 'Lunar Gray', 2024, 16774, 'rented'),
(77, 19, 77, 'Deep Blue', 2025, 14120, 'maintenance'),
(78, 20, 78, 'Metallic Black', 2023, 23343, 'available'),
(79, 20, 79, 'Deep Blue', 2025, 43060, 'available'),
(80, 20, 80, 'Champagne Gold', 2025, 29191, 'available')
ON CONFLICT (vehicle_id) DO NOTHING;

-- Update sequences
SELECT setval('license_plates_license_plate_id_seq', (SELECT MAX(license_plate_id) FROM license_plates));
SELECT setval('vehicles_vehicle_id_seq', (SELECT MAX(vehicle_id) FROM vehicles));

COMMIT;

