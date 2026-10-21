-- Full Seed Data for CRMS (all 13 tables)
-- Password mặc định cho tất cả tài khoản test: 123456 (plain text khớp với AuthServiceImpl hiện tại)

BEGIN;

-- 1. CUSTOMERS
INSERT INTO customers (customer_id, name, phone, email, password_hash, created_at)
VALUES 
    (1, 'Nguyễn Văn Khách', '0901234567', 'customer@gmail.com', '123456', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    (2, 'Trần Thị Thuê', '0912345678', 'customer2@gmail.com', '123456', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (3, 'Lê Văn Lái', '0923456789', 'customer3@gmail.com', '123456', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (customer_id) DO NOTHING;

-- 2. EMPLOYEES
-- role: 'admin' | 'staff'
INSERT INTO employees (employee_id, name, phone, email, password_hash, role)
VALUES 
    (1, 'Admin Quản Trị', '0988888888', 'admin@crms.com', '123456', 'admin'),
    (2, 'Nhân Viên Hỗ Trợ', '0977777777', 'staff@crms.com', '123456', 'staff')
ON CONFLICT (employee_id) DO NOTHING;

-- 3. VEHICLE_TYPES
INSERT INTO vehicle_types (vehicle_type_id, name, brand, category, seats, transmission, fuel_type, price_per_hour, description, image_url)
VALUES 
    (1, 'Toyota Vios 2023', 'Toyota', 'sedan', 5, 'automatic', 'gasoline', 150000.00, 'Xe sedan 5 chỗ nhỏ gọn, tiết kiệm nhiên liệu, tiện di chuyển đô thị.', '/images/vios.jpg'),
    (2, 'Honda CR-V 2024', 'Honda', 'suv', 7, 'automatic', 'gasoline', 300000.00, 'SUV 7 chỗ rộng rãi, phù hợp gia đình du lịch.', '/images/crv.jpg'),
    (3, 'VinFast VF8 2023', 'VinFast', 'suv', 5, 'automatic', 'electric', 280000.00, 'Xe điện thông minh hạng D, trải nghiệm lái cao cấp.', '/images/vf8.jpg')
ON CONFLICT (vehicle_type_id) DO NOTHING;

-- 4. LICENSE_PLATES
INSERT INTO license_plates (license_plate_id, plate, province, registration_expiry)
VALUES 
    (1, '51A-123.45', 'TP.HCM', CURRENT_DATE + INTERVAL '1 year'),
    (2, '51B-678.90', 'TP.HCM', CURRENT_DATE + INTERVAL '1 year'),
    (3, '29A-999.88', 'Hà Nội', CURRENT_DATE + INTERVAL '1 year'),
    (4, '43A-555.66', 'Đà Nẵng', CURRENT_DATE + INTERVAL '1 year')
ON CONFLICT (license_plate_id) DO NOTHING;

-- 5. VEHICLES
-- status: 'available' | 'rented' | 'maintenance'
INSERT INTO vehicles (vehicle_id, vehicle_type_id, license_plate_id, color, year, odometer, status)
VALUES 
    (1, 1, 1, 'White', 2023, 12000, 'available'),
    (2, 1, 2, 'Black', 2023, 15400, 'rented'),
    (3, 2, 3, 'Silver', 2024, 8500, 'available'),
    (4, 3, 4, 'Blue', 2023, 18000, 'maintenance')
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
    -- Booking 1: Đã hoàn tất (completed)
    (
        1, 1, 1, 1, 2, 1,
        CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days',
        0.00, 1350000.00,
        'completed', CURRENT_TIMESTAMP - INTERVAL '5 days',
        150000.00, 5000000.00, 150000.00
    ),
    -- Booking 2: Đang thuê (active)
    (
        2, 2, 1, 2, 2, NULL,
        CURRENT_TIMESTAMP - INTERVAL '4 hours', CURRENT_TIMESTAMP + INTERVAL '6 hours', NULL,
        0.00, 1500000.00,
        'active', CURRENT_TIMESTAMP - INTERVAL '1 day',
        150000.00, 5000000.00, 0.00
    ),
    -- Booking 3: Đã xác nhận, chuẩn bị nhận xe (confirmed)
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
    (1, 1, 5, 'Xe sạch sẽ, nhân viên bàn giao xe đúng giờ, rất hài lòng!', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (review_id) DO NOTHING;

-- 12. VEHICLE_INSPECTIONS
-- status: 'scheduled' | 'passed' | 'failed'
INSERT INTO vehicle_inspections (vehicle_inspection_id, vehicle_id, scheduled_at, completed_at, status, notes)
VALUES 
    (1, 1, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days', 'passed', 'Kiểm tra định kỳ 10.000km, xe vận hành tốt.'),
    (2, 4, CURRENT_TIMESTAMP - INTERVAL '1 day', NULL, 'scheduled', 'Kiểm tra hệ thống pin và cảm biến cảnh báo.')
ON CONFLICT (vehicle_inspection_id) DO NOTHING;

-- 13. ISSUES
-- issue_type, status: 'open' | 'in_progress' | 'resolved'
INSERT INTO issues (issue_id, booking_id, slot_id, vehicle_id, employee_id, issue_type, subject, status, fine_amount, evidence_url, reported_at)
VALUES 
    (1, 1, 1, 1, 2, 'SCRATCH', 'Vết xước nhẹ ở cản trước', 'resolved', 200000.00, '/uploads/issues/issue_1.jpg', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (issue_id) DO NOTHING;

-- Cập nhật chuỗi sequence của PostgreSQL cho các bảng IDENTITY
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
