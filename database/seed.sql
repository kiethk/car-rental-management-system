-- Full Seed Data for CRMS (all 13 tables)
-- Demo password for all seed accounts: 123456

BEGIN;

-- 1. CUSTOMERS
INSERT INTO customers (customer_id, name, phone, email, password_hash, created_at)
VALUES 
    (1, 'John Doe', '0901234567', 'customer@example.com', '$2a$10$f1dC/wkm8nQT1k3YNvBP3e75TsnTOcT4JaMR6LcS.KsJ5nBC0w.nW', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    (2, 'Jane Smith', '0912345678', 'customer2@example.com', '$2a$10$f1dC/wkm8nQT1k3YNvBP3e75TsnTOcT4JaMR6LcS.KsJ5nBC0w.nW', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (3, 'David Miller', '0923456789', 'customer3@example.com', '$2a$10$f1dC/wkm8nQT1k3YNvBP3e75TsnTOcT4JaMR6LcS.KsJ5nBC0w.nW', CURRENT_TIMESTAMP - INTERVAL '2 days')
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

-- 4. VEHICLES
-- status: 'available' | 'rented' | 'maintenance'
-- registration_expiry: inspection sticker of the vehicle (it stays with the car when the plate changes)
INSERT INTO vehicles (vehicle_id, vehicle_type_id, color, year, odometer, registration_expiry, status)
VALUES
    (1, 1, 'White', 2023, 12000, CURRENT_DATE + INTERVAL '1 year', 'available'),
    (2, 1, 'Black', 2023, 15400, CURRENT_DATE + INTERVAL '1 year', 'rented'),
    (3, 2, 'Silver', 2024, 8500, CURRENT_DATE + INTERVAL '1 year', 'available'),
    (4, 3, 'Blue', 2023, 18000, CURRENT_DATE - INTERVAL '5 days', 'maintenance'), -- Expired registration
    (5, 4, 'Red', 2023, 21000, CURRENT_DATE + INTERVAL '15 days', 'available'), -- Expiring soon in <= 30 days
    (6, 5, 'Pearl White', 2024, 6200, CURRENT_DATE + INTERVAL '6 months', 'available'),
    (7, 6, 'Cement Gray', 2023, 14500, CURRENT_DATE + INTERVAL '1 year', 'available'),
    (8, 7, 'Wildtrak Orange', 2023, 28000, CURRENT_DATE + INTERVAL '1 year', 'available'),
    (9, 8, 'White', 2024, 9800, CURRENT_DATE + INTERVAL '8 months', 'available'),
    (10, 9, 'Midnight Black', 2023, 31000, CURRENT_DATE + INTERVAL '10 months', 'available'),
    (11, 10, 'Golden Brown', 2023, 19200, CURRENT_DATE + INTERVAL '2 years', 'available'),
    (12, 11, 'Snow White', 2024, 4300, CURRENT_DATE + INTERVAL '18 months', 'available'),
    (13, 12, 'Moss Green', 2024, 5100, CURRENT_DATE + INTERVAL '1 year', 'available'),
    (14, 13, 'Black', 2023, 17800, CURRENT_DATE + INTERVAL '1 year', 'available'),
    (15, 14, 'Sonic Red', 2023, 13200, CURRENT_DATE + INTERVAL '1 year', 'available'),
    (16, 15, 'Portimao Blue', 2023, 11500, CURRENT_DATE + INTERVAL '1 year', 'available'),
    (17, 16, 'Iridium Silver', 2023, 9000, CURRENT_DATE + INTERVAL '2 years', 'available'),
    (18, 17, 'Matte Volcano Black', 2023, 8200, CURRENT_DATE + INTERVAL '1 year', 'available'),
    (19, 18, 'Glacier White', 2023, 14000, CURRENT_DATE + INTERVAL '1 year', 'available'),
    (20, 19, 'Deep Ocean Blue', 2023, 16500, CURRENT_DATE + INTERVAL '1 year', 'available')
ON CONFLICT (vehicle_id) DO NOTHING;

-- 5. LICENSE_PLATES
-- valid_to IS NULL = plate currently on the car. Vehicle 1 shows a car with several plate generations.
INSERT INTO license_plates (license_plate_id, vehicle_id, plate, province, valid_from, valid_to)
VALUES
    (1, 1, '51A-123.45', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (2, 2, '51B-678.90', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (3, 3, '29A-999.88', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (4, 4, '43A-555.66', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (5, 5, '51K-111.22', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (6, 6, '51K-333.44', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (7, 7, '60A-888.99', 'Dong Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (8, 8, '61A-777.66', 'Binh Duong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (9, 9, '51H-246.80', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (10, 10, '30H-135.79', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (11, 11, '51F-999.99', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (12, 12, '51L-888.88', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (13, 13, '29B-456.78', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (14, 14, '43B-123.89', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (15, 15, '51K-902.10', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (16, 16, '51K-777.77', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (17, 17, '51A-899.99', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (18, 18, '30A-666.88', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (19, 19, '51E-234.56', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (20, 20, '60B-345.67', 'Dong Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (81, 1, '51F-100.01', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '4 years', CURRENT_TIMESTAMP - INTERVAL '2 years'),
    (82, 1, '51G-200.02', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '2 years', CURRENT_TIMESTAMP - INTERVAL '1 year')
ON CONFLICT (license_plate_id) DO NOTHING;

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
    price_per_hour, deposit_amount, discount_amount, license_plate
)
VALUES 
    -- Booking 1: Completed
    (
        1, 1, 1, 1, 2, 1,
        CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days',
        0.00, 1350000.00,
        'completed', CURRENT_TIMESTAMP - INTERVAL '5 days',
        150000.00, 5000000.00, 150000.00, '51A-123.45'
    ),
    -- Booking 2: Active rental
    (
        2, 2, 1, 2, 2, NULL,
        CURRENT_TIMESTAMP - INTERVAL '4 hours', CURRENT_TIMESTAMP + INTERVAL '6 hours', NULL,
        0.00, 1500000.00,
        'active', CURRENT_TIMESTAMP - INTERVAL '1 day',
        150000.00, 5000000.00, 0.00, '51B-678.90'
    ),
    -- Booking 3: Confirmed
    (
        3, 1, 3, 3, 2, 2,
        CURRENT_TIMESTAMP + INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '1 day 10 hours', NULL,
        0.00, 2900000.00,
        'confirmed', CURRENT_TIMESTAMP - INTERVAL '2 hours',
        300000.00, 8000000.00, 100000.00, NULL
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

INSERT INTO vehicles (vehicle_id, vehicle_type_id, color, year, odometer, registration_expiry, status)
VALUES
    (21, 1, 'Deep Blue', 2023, 9717, CURRENT_DATE + INTERVAL '13 months', 'maintenance'),
    (22, 1, 'Pearl White', 2022, 17328, CURRENT_DATE + INTERVAL '7 months', 'available'),
    (23, 1, 'Champagne Gold', 2023, 32439, CURRENT_DATE + INTERVAL '23 months', 'available'),
    (24, 2, 'Ruby Red', 2024, 13189, CURRENT_DATE + INTERVAL '19 months', 'available'),
    (25, 2, 'Metallic Black', 2024, 25541, CURRENT_DATE + INTERVAL '18 months', 'available'),
    (26, 2, 'Metallic Black', 2025, 8164, CURRENT_DATE + INTERVAL '23 months', 'available'),
    (27, 3, 'Pearl White', 2023, 21965, CURRENT_DATE + INTERVAL '8 months', 'available'),
    (28, 3, 'Lunar Gray', 2025, 44660, CURRENT_DATE + INTERVAL '18 months', 'available'),
    (29, 3, 'Metallic Black', 2023, 38005, CURRENT_DATE + INTERVAL '14 months', 'maintenance'),
    (30, 4, 'Deep Blue', 2024, 6665, CURRENT_DATE + INTERVAL '14 months', 'available'),
    (31, 4, 'Lunar Gray', 2022, 16826, CURRENT_DATE + INTERVAL '18 months', 'available'),
    (32, 4, 'Champagne Gold', 2025, 12363, CURRENT_DATE + INTERVAL '21 months', 'available'),
    (33, 5, 'Lunar Gray', 2025, 41242, CURRENT_DATE + INTERVAL '23 months', 'rented'),
    (34, 5, 'Metallic Black', 2022, 10185, CURRENT_DATE + INTERVAL '21 months', 'available'),
    (35, 5, 'Metallic Black', 2025, 28009, CURRENT_DATE + INTERVAL '19 months', 'available'),
    (36, 6, 'Lunar Gray', 2024, 10310, CURRENT_DATE + INTERVAL '9 months', 'available'),
    (37, 6, 'Silver Frost', 2022, 43979, CURRENT_DATE + INTERVAL '14 months', 'available'),
    (38, 6, 'Pearl White', 2024, 35021, CURRENT_DATE + INTERVAL '11 months', 'available'),
    (39, 7, 'Pearl White', 2023, 40182, CURRENT_DATE + INTERVAL '13 months', 'available'),
    (40, 7, 'Silver Frost', 2023, 34148, CURRENT_DATE + INTERVAL '23 months', 'available'),
    (41, 7, 'Deep Blue', 2023, 23428, CURRENT_DATE + INTERVAL '19 months', 'rented'),
    (42, 8, 'Metallic Black', 2023, 17725, CURRENT_DATE + INTERVAL '20 months', 'available'),
    (43, 8, 'Deep Blue', 2022, 7652, CURRENT_DATE + INTERVAL '13 months', 'maintenance'),
    (44, 8, 'Ruby Red', 2022, 36695, CURRENT_DATE + INTERVAL '7 months', 'available'),
    (45, 9, 'Midnight Black', 2023, 33996, CURRENT_DATE + INTERVAL '10 months', 'rented'),
    (46, 9, 'Ruby Red', 2025, 29941, CURRENT_DATE + INTERVAL '19 months', 'rented'),
    (47, 9, 'Pearl White', 2025, 25236, CURRENT_DATE + INTERVAL '9 months', 'available'),
    (48, 10, 'Silver Frost', 2025, 15025, CURRENT_DATE + INTERVAL '20 months', 'available'),
    (49, 10, 'Metallic Black', 2022, 38427, CURRENT_DATE + INTERVAL '20 months', 'available'),
    (50, 10, 'Midnight Black', 2023, 29282, CURRENT_DATE + INTERVAL '21 months', 'available'),
    (51, 11, 'Midnight Black', 2024, 30722, CURRENT_DATE + INTERVAL '14 months', 'maintenance'),
    (52, 11, 'Deep Blue', 2022, 40957, CURRENT_DATE + INTERVAL '15 months', 'maintenance'),
    (53, 11, 'Pearl White', 2025, 35954, CURRENT_DATE + INTERVAL '7 months', 'available'),
    (54, 12, 'Metallic Black', 2022, 18414, CURRENT_DATE + INTERVAL '11 months', 'rented'),
    (55, 12, 'Metallic Black', 2025, 41251, CURRENT_DATE + INTERVAL '7 months', 'available'),
    (56, 12, 'Deep Blue', 2024, 28938, CURRENT_DATE + INTERVAL '16 months', 'available'),
    (57, 13, 'Pearl White', 2025, 43708, CURRENT_DATE + INTERVAL '8 months', 'available'),
    (58, 13, 'Lunar Gray', 2023, 25872, CURRENT_DATE + INTERVAL '22 months', 'available'),
    (59, 13, 'Lunar Gray', 2022, 39346, CURRENT_DATE + INTERVAL '20 months', 'available'),
    (60, 14, 'Metallic Black', 2022, 39256, CURRENT_DATE + INTERVAL '14 months', 'available'),
    (61, 14, 'Deep Blue', 2024, 36122, CURRENT_DATE + INTERVAL '16 months', 'rented'),
    (62, 14, 'Lunar Gray', 2022, 3232, CURRENT_DATE + INTERVAL '19 months', 'available'),
    (63, 15, 'Midnight Black', 2025, 39759, CURRENT_DATE + INTERVAL '11 months', 'available'),
    (64, 15, 'Pearl White', 2024, 41175, CURRENT_DATE + INTERVAL '23 months', 'available'),
    (65, 15, 'Ruby Red', 2022, 26449, CURRENT_DATE + INTERVAL '15 months', 'available'),
    (66, 16, 'Champagne Gold', 2023, 18514, CURRENT_DATE + INTERVAL '17 months', 'available'),
    (67, 16, 'Ruby Red', 2025, 19263, CURRENT_DATE + INTERVAL '11 months', 'available'),
    (68, 16, 'Deep Blue', 2023, 33166, CURRENT_DATE + INTERVAL '21 months', 'available'),
    (69, 17, 'Champagne Gold', 2024, 21258, CURRENT_DATE + INTERVAL '12 months', 'available'),
    (70, 17, 'Champagne Gold', 2024, 4808, CURRENT_DATE + INTERVAL '22 months', 'available'),
    (71, 17, 'Metallic Black', 2025, 25654, CURRENT_DATE + INTERVAL '7 months', 'maintenance'),
    (72, 18, 'Metallic Black', 2025, 40787, CURRENT_DATE + INTERVAL '22 months', 'available'),
    (73, 18, 'Deep Blue', 2024, 31265, CURRENT_DATE + INTERVAL '6 months', 'available'),
    (74, 18, 'Lunar Gray', 2024, 29764, CURRENT_DATE + INTERVAL '9 months', 'available'),
    (75, 19, 'Champagne Gold', 2025, 14405, CURRENT_DATE + INTERVAL '12 months', 'available'),
    (76, 19, 'Lunar Gray', 2024, 16774, CURRENT_DATE + INTERVAL '6 months', 'rented'),
    (77, 19, 'Deep Blue', 2025, 14120, CURRENT_DATE + INTERVAL '20 months', 'maintenance'),
    (78, 20, 'Metallic Black', 2023, 23343, CURRENT_DATE + INTERVAL '16 months', 'available'),
    (79, 20, 'Deep Blue', 2025, 43060, CURRENT_DATE + INTERVAL '7 months', 'available'),
    (80, 20, 'Champagne Gold', 2025, 29191, CURRENT_DATE + INTERVAL '12 months', 'available')
ON CONFLICT (vehicle_id) DO NOTHING;

INSERT INTO license_plates (license_plate_id, vehicle_id, plate, province, valid_from, valid_to)
VALUES
    (21, 21, '51L-125.45', 'Can Tho', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (22, 22, '51L-704.64', 'Can Tho', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (23, 23, '51K-674.35', 'Binh Duong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (24, 24, '51K-877.30', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (25, 25, '43A-204.21', 'Hai Phong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (26, 26, '51K-847.68', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (27, 27, '43A-691.34', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (28, 28, '30K-987.22', 'Hai Phong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (29, 29, '43A-463.36', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (30, 30, '51H-573.58', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (31, 31, '51K-924.50', 'Hai Phong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (32, 32, '43A-317.93', 'Can Tho', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (33, 33, '30K-862.81', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (34, 34, '30K-241.75', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (35, 35, '51H-911.97', 'Can Tho', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (36, 36, '30L-666.11', 'Dong Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (37, 37, '51H-564.10', 'Dong Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (38, 38, '30K-256.57', 'Hai Phong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (39, 39, '43A-999.49', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (40, 40, '61B-935.18', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (41, 41, '30L-640.87', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (42, 42, '43A-548.76', 'Can Tho', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (43, 43, '51K-702.80', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (44, 44, '51K-334.18', 'Can Tho', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (45, 45, '61B-319.79', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (46, 46, '51L-199.94', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (47, 47, '51K-789.93', 'Hai Phong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (48, 48, '30K-294.78', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (49, 49, '30K-995.19', 'Dong Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (50, 50, '30K-270.62', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (51, 51, '60A-102.59', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (52, 52, '61B-258.34', 'Can Tho', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (53, 53, '51K-865.50', 'Binh Duong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (54, 54, '51K-620.20', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (55, 55, '30K-692.86', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (56, 56, '43A-367.36', 'Binh Duong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (57, 57, '30L-568.50', 'Can Tho', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (58, 58, '51L-650.37', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (59, 59, '43A-391.30', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (60, 60, '51L-999.27', 'Can Tho', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (61, 61, '30L-719.36', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (62, 62, '51K-194.91', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (63, 63, '51H-752.43', 'Hai Phong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (64, 64, '51L-807.29', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (65, 65, '60A-230.15', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (66, 66, '30K-782.23', 'Can Tho', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (67, 67, '51H-522.13', 'Hai Phong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (68, 68, '51L-491.14', 'Hanoi', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (69, 69, '30K-328.13', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (70, 70, '30L-459.92', 'Hai Phong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (71, 71, '51H-694.43', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (72, 72, '43A-546.87', 'Hai Phong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (73, 73, '51K-825.65', 'Da Nang', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (74, 74, '43A-738.50', 'Can Tho', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (75, 75, '30L-667.26', 'Dong Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (76, 76, '30L-515.80', 'Binh Duong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (77, 77, '43A-576.66', 'Hai Phong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (78, 78, '30L-627.94', 'Ho Chi Minh City', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (79, 79, '30K-250.13', 'Hai Phong', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (80, 80, '60A-744.83', 'Dong Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL)
ON CONFLICT (license_plate_id) DO NOTHING;

-- Update sequences
SELECT setval('license_plates_license_plate_id_seq', (SELECT MAX(license_plate_id) FROM license_plates));
SELECT setval('vehicles_vehicle_id_seq', (SELECT MAX(vehicle_id) FROM vehicles));

COMMIT;

