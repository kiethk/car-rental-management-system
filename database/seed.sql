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
-- role: 'ADMIN' | 'STAFF'
INSERT INTO employees (employee_id, name, phone, email, password_hash, role)
VALUES 
    (1, 'Admin Manager', '0988888888', 'admin@crms.com', '$2a$10$f1dC/wkm8nQT1k3YNvBP3e75TsnTOcT4JaMR6LcS.KsJ5nBC0w.nW', 'ADMIN'),
    (2, 'Support Staff', '0977777777', 'staff@crms.com', '$2a$10$f1dC/wkm8nQT1k3YNvBP3e75TsnTOcT4JaMR6LcS.KsJ5nBC0w.nW', 'STAFF')
ON CONFLICT (employee_id) DO NOTHING;

-- 3. VEHICLE_TYPES (20 diverse vehicle models with high quality online images)
INSERT INTO vehicle_types (vehicle_type_id, name, brand, category, seats, transmission, fuel_type, price_per_hour, description, image_url)
VALUES 
    (1, 'Vios', 'Toyota', 'SEDAN', 5, 'AUTOMATIC', 'GASOLINE', 150000.00, 'Sedan 5 chỗ nhỏ gọn, tiết kiệm nhiên liệu, lựa chọn lý tưởng khi di chuyển trong thành phố.', '/images/vehicles/vios.jpg'),
    (2, 'CR-V', 'Honda', 'SUV', 7, 'AUTOMATIC', 'GASOLINE', 300000.00, 'SUV 7 chỗ rộng rãi, tiện nghi, phù hợp hoàn hảo cho các chuyến du lịch gia đình.', '/images/vehicles/crv.jpg'),
    (3, 'VF8', 'VinFast', 'SUV', 5, 'AUTOMATIC', 'ELECTRIC', 280000.00, 'SUV điện phân khúc D thông minh, trải nghiệm cao cấp với công nghệ trợ lái ADAS tiên tiến.', '/images/vehicles/vf8.jpg'),
    (4, 'Morning', 'Kia', 'HATCHBACK', 4, 'AUTOMATIC', 'GASOLINE', 120000.00, 'Hatchback đô thị nhỏ gọn, linh hoạt trên phố đông, chi phí thuê siêu tiết kiệm.', '/images/vehicles/morning.jpg'),
    (5, 'Accent', 'Hyundai', 'SEDAN', 5, 'AUTOMATIC', 'GASOLINE', 160000.00, 'Sedan thế hệ mới hiện đại, trang bị tiện nghi đầy đủ và êm ái trên mọi hành trình.', '/images/vehicles/accent.jpg'),
    (6, 'Mazda 3', 'Mazda', 'SEDAN', 5, 'AUTOMATIC', 'GASOLINE', 220000.00, 'Thiết kế KODO thể thao, nội thất tiệm cận xe sang cùng cảm giác lái phấn khích.', '/images/vehicles/mazda3.jpg'),
    (7, 'Ranger', 'Ford', 'PICKUP', 5, 'AUTOMATIC', 'DIESEL', 320000.00, 'Bán tải mạnh mẽ dẫn động 4 bánh, sẵn sàng chinh phục mọi cung đường hiểm trở.', '/images/vehicles/ranger.jpg'),
    (8, 'Xpander', 'Mitsubishi', 'MINIVAN', 7, 'AUTOMATIC', 'GASOLINE', 240000.00, 'Mẫu MPV 7 chỗ bán chạy hàng đầu, gầm cao, nội thất đa dụng cho gia đình.', '/images/vehicles/xpander.jpg'),
    (9, 'Fortuner', 'Toyota', 'SUV', 7, 'AUTOMATIC', 'DIESEL', 350000.00, 'SUV 7 chỗ cỡ lớn khỏe khoắn, gầm cao máy thoáng vượt mọi địa hình vượt trội.', '/images/vehicles/fortuner.jpg'),
    (10, 'Santa Fe', 'Hyundai', 'SUV', 7, 'AUTOMATIC', 'DIESEL', 360000.00, 'SUV sang trọng thời thượng với tiện nghi cao cấp và hệ thống âm thanh Harman Kardon.', '/images/vehicles/santafe.jpg'),
    (11, 'Carnival', 'Kia', 'MINIVAN', 8, 'AUTOMATIC', 'DIESEL', 450000.00, 'MPV 8 chỗ đẳng cấp, ghế cơ trưởng sang trọng mang lại sự thoải mái tuyệt đối trên đường dài.', '/images/vehicles/carnival.jpg'),
    (12, 'VF9', 'VinFast', 'SUV', 7, 'AUTOMATIC', 'ELECTRIC', 480000.00, 'SUV điện cỡ lớn đầu bảng, nội thất bọc da cao cấp cùng công nghệ lái thông minh vượt bậc.', '/images/vehicles/vf9.jpg'),
    (13, 'Camry', 'Toyota', 'SEDAN', 5, 'AUTOMATIC', 'GASOLINE', 350000.00, 'Sedan doanh nhân lịch lãm, không gian cách âm hoàn hảo cùng hàng ghế sau chuẩn thương gia.', '/images/vehicles/camry.jpg'),
    (14, 'Civic', 'Honda', 'SEDAN', 5, 'AUTOMATIC', 'GASOLINE', 250000.00, 'Sedan thể thao trang bị động cơ VTEC Turbo mạnh mẽ cùng cảm giác lái nhạy bén.', '/images/vehicles/civic.jpg'),
    (15, '320i', 'BMW', 'SEDAN', 5, 'AUTOMATIC', 'GASOLINE', 500000.00, 'Sedan thể thao hạng sang nước Đức với gói M Sport và tỷ lệ phân bổ trọng lượng 50:50 hoàn hảo.', '/images/vehicles/bmw320i.jpg'),
    (16, 'C200', 'Mercedes-Benz', 'SEDAN', 5, 'AUTOMATIC', 'GASOLINE', 520000.00, 'Sedan Mercedes-Benz lịch lãm, màn hình trung tâm 11.9 inch cùng đèn viền nội thất 64 màu.', '/images/vehicles/c200.jpg'),
    (17, 'Macan', 'Porsche', 'SUV', 5, 'AUTOMATIC', 'GASOLINE', 850000.00, 'SUV thể thao hạng sang hiệu năng cao từ Stuttgart mang trọn vẹn gen tốc độ đỉnh cao.', '/images/vehicles/macan.jpg'),
    (18, 'A6', 'Audi', 'SEDAN', 5, 'AUTOMATIC', 'GASOLINE', 600000.00, 'Sedan doanh nhân công nghệ cao, đèn pha Matrix LED cùng khoang lái màn hình kép hiện đại.', '/images/vehicles/a6.jpg'),
    (19, 'VF e34', 'VinFast', 'SUV', 5, 'AUTOMATIC', 'ELECTRIC', 180000.00, 'Crossover điện thân thiện với môi trường, chi phí vận hành tối ưu và tính năng kết nối thông minh.', '/images/vehicles/vfe34.jpg'),
    (20, 'Corolla Cross', 'Toyota', 'SUV', 5, 'AUTOMATIC', 'HYBRID', 270000.00, 'Xe Hybrid vận hành siêu êm ái và tiết kiệm, trang bị gói an toàn chủ động Toyota Safety Sense.', '/images/vehicles/corollacross.jpg')
ON CONFLICT (vehicle_type_id) DO UPDATE SET 
    name = EXCLUDED.name, brand = EXCLUDED.brand, category = EXCLUDED.category, seats = EXCLUDED.seats, 
    transmission = EXCLUDED.transmission, fuel_type = EXCLUDED.fuel_type, price_per_hour = EXCLUDED.price_per_hour, 
    description = EXCLUDED.description, image_url = EXCLUDED.image_url;

-- 4. VEHICLES
-- status: 'AVAILABLE' | 'RENTED' | 'MAINTENANCE' | 'RETIRED'
-- registration_expiry: inspection sticker of the vehicle (it stays with the car when the plate changes)
INSERT INTO vehicles (vehicle_id, vehicle_type_id, color, year, odometer, registration_expiry, status)
VALUES
    (1, 1, 'Trắng', 2023, 12000, CURRENT_DATE + INTERVAL '1 year', 'AVAILABLE'),
    (2, 1, 'Đen', 2023, 15400, CURRENT_DATE + INTERVAL '1 year', 'RENTED'),
    (3, 2, 'Bạc', 2024, 8500, CURRENT_DATE + INTERVAL '1 year', 'AVAILABLE'),
    (4, 3, 'Xanh dương', 2023, 18000, CURRENT_DATE - INTERVAL '5 days', 'MAINTENANCE'), -- Expired registration
    (5, 4, 'Đỏ', 2023, 21000, CURRENT_DATE + INTERVAL '15 days', 'AVAILABLE'), -- Expiring soon in <= 30 days
    (6, 5, 'Trắng ngọc trai', 2024, 6200, CURRENT_DATE + INTERVAL '6 months', 'AVAILABLE'),
    (7, 6, 'Xám xi măng', 2023, 14500, CURRENT_DATE + INTERVAL '3 days', 'AVAILABLE'), -- Expiring soon
    (8, 7, 'Cam Wildtrak', 2023, 28000, CURRENT_DATE + INTERVAL '1 year', 'AVAILABLE'),
    (9, 8, 'Trắng', 2024, 9800, CURRENT_DATE + INTERVAL '25 days', 'AVAILABLE'), -- Expiring soon
    (10, 9, 'Đen bóng đêm', 2023, 31000, CURRENT_DATE + INTERVAL '10 months', 'AVAILABLE'),
    (11, 10, 'Nâu ánh kim', 2023, 19200, CURRENT_DATE + INTERVAL '2 years', 'AVAILABLE'),
    (12, 11, 'Trắng tuyết', 2024, 4300, CURRENT_DATE + INTERVAL '9 days', 'AVAILABLE'), -- Expiring soon
    (13, 12, 'Xanh rêu', 2024, 5100, CURRENT_DATE + INTERVAL '1 year', 'AVAILABLE'),
    (14, 13, 'Đen', 2023, 17800, CURRENT_DATE - INTERVAL '12 days', 'MAINTENANCE'), -- Expired registration
    (15, 14, 'Đỏ Sonic', 2023, 13200, CURRENT_DATE + INTERVAL '1 year', 'AVAILABLE'),
    (16, 15, 'Xanh Portimao', 2023, 11500, CURRENT_DATE + INTERVAL '1 year', 'AVAILABLE'),
    (17, 16, 'Bạc Iridium', 2023, 9000, CURRENT_DATE - INTERVAL '2 days', 'MAINTENANCE'), -- Expired registration
    (18, 17, 'Đen nhám Volcano', 2023, 8200, CURRENT_DATE + INTERVAL '1 year', 'AVAILABLE'),
    (19, 18, 'Trắng băng', 2023, 14000, CURRENT_DATE + INTERVAL '1 year', 'AVAILABLE'),
    (20, 19, 'Xanh đại dương', 2023, 16500, CURRENT_DATE + INTERVAL '1 year', 'AVAILABLE')
ON CONFLICT (vehicle_id) DO UPDATE SET color = EXCLUDED.color, year = EXCLUDED.year, odometer = EXCLUDED.odometer,
    registration_expiry = EXCLUDED.registration_expiry, status = EXCLUDED.status;

-- 5. LICENSE_PLATES
-- valid_to IS NULL = plate currently on the car. Vehicle 1 shows a car with several plate generations.
INSERT INTO license_plates (license_plate_id, vehicle_id, plate, province, valid_from, valid_to)
VALUES
    (1, 1, '51A-123.45', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (2, 2, '51B-678.90', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (3, 3, '29A-999.88', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (4, 4, '43A-555.66', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (5, 5, '51K-111.22', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (6, 6, '51K-333.44', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (7, 7, '60A-888.99', 'Đồng Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (8, 8, '61A-777.66', 'Bình Dương', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (9, 9, '51H-246.80', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (10, 10, '30H-135.79', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (11, 11, '51F-999.99', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (12, 12, '51L-888.88', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (13, 13, '29B-456.78', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (14, 14, '43B-123.89', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (15, 15, '51K-902.10', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (16, 16, '51K-777.77', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (17, 17, '51A-899.99', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (18, 18, '30A-666.88', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (19, 19, '51E-234.56', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (20, 20, '60B-345.67', 'Đồng Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (81, 1, '51F-100.01', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '4 years', CURRENT_TIMESTAMP - INTERVAL '2 years'),
    (82, 1, '51G-200.02', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '2 years', CURRENT_TIMESTAMP - INTERVAL '1 year')
ON CONFLICT (license_plate_id) DO UPDATE SET plate = EXCLUDED.plate, province = EXCLUDED.province;

-- 6. PROMOTIONS
-- discount_type: 'PERCENT' | 'FIXED'
INSERT INTO promotions (promotion_id, code, discount_type, discount_value, start_date, end_date, quantity)
VALUES 
    (1, 'WELCOME10', 'PERCENT', 10.00, CURRENT_DATE - INTERVAL '10 days', CURRENT_DATE + INTERVAL '30 days', 100),
    (2, 'GIAM100K', 'FIXED', 100000.00, CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE + INTERVAL '20 days', 50)
ON CONFLICT (promotion_id) DO NOTHING;

-- 7. LICENSE_DOCUMENTS
-- status: 'pending' | 'approved' | 'rejected'
INSERT INTO license_documents (license_document_id, customer_id, license_number, license_class, license_expiry, license_image_url, submitted_at)
VALUES 
    (1, 1, 'B2-79123456', 'B2', CURRENT_DATE + INTERVAL '5 years', '/uploads/licenses/cust1_license.jpg', CURRENT_TIMESTAMP - INTERVAL '8 days'),
    (2, 2, 'B1-88123999', 'B1', CURRENT_DATE + INTERVAL '3 years', '/uploads/licenses/cust2_license.jpg', CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (3, 3, 'B2-99887766', 'B2', CURRENT_DATE + INTERVAL '8 years', '/uploads/licenses/cust3_license.jpg', CURRENT_TIMESTAMP - INTERVAL '1 days')
ON CONFLICT (license_document_id) DO NOTHING;

-- 8. SLOTS
-- status: 'OPEN' | 'CLOSED'
INSERT INTO slots (slot_id, vehicle_type_id, start_time, end_time, status)
VALUES 
    (1, 1, (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '8 hours'), (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '18 hours'), 'OPEN'),
    (2, 1, (CURRENT_DATE + INTERVAL '2 days' + INTERVAL '8 hours'), (CURRENT_DATE + INTERVAL '2 days' + INTERVAL '18 hours'), 'OPEN'),
    (3, 2, (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '8 hours'), (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '20 hours'), 'OPEN'),
    (4, 3, (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '8 hours'), (CURRENT_DATE + INTERVAL '1 day' + INTERVAL '18 hours'), 'CLOSED')
ON CONFLICT (slot_id) DO NOTHING;

-- 9. BOOKINGS
-- pickup_time and scheduled_end_time are whole hours (minute :00), the same rule the booking form enforces.
-- Past and future bookings sit at fixed daytime hours (CURRENT_DATE + N hours) so they stay inside opening hours
-- whatever time the seed runs; only the active rental is relative to now, because it must cover the current hour.
-- status: 'PENDING_PAYMENT' | 'CONFIRMED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED'
INSERT INTO bookings (
    booking_id, customer_id, slot_id, vehicle_id, employee_id, promotion_id,
    pickup_time, scheduled_end_time, actual_return_time,
    total_price,
    status, created_at,
    price_per_hour, deposit_amount, discount_amount
)
VALUES 
    -- Booking 1: Completed
    (
        1, 1, 1, 1, 2, 1,
        CURRENT_DATE - INTERVAL '3 days' + INTERVAL '9 hours', CURRENT_DATE - INTERVAL '2 days' + INTERVAL '9 hours', CURRENT_DATE - INTERVAL '2 days' + INTERVAL '9 hours',
        1350000.00,
        'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '5 days',
        150000.00, 675000.00, 150000.00
    ),
    -- Booking 2: Active rental
    (
        2, 2, 1, 2, 2, NULL,
        date_trunc('hour', CURRENT_TIMESTAMP) - INTERVAL '4 hours', date_trunc('hour', CURRENT_TIMESTAMP) + INTERVAL '6 hours', NULL,
        1500000.00,
        'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '1 day',
        150000.00, 750000.00, 0.00
    ),
    -- Booking 3: Confirmed
    (
        3, 1, 3, 3, 2, 2,
        CURRENT_DATE + INTERVAL '1 day' + INTERVAL '8 hours', CURRENT_DATE + INTERVAL '1 day' + INTERVAL '18 hours', NULL,
        2900000.00,
        'CONFIRMED', CURRENT_TIMESTAMP - INTERVAL '2 hours',
        300000.00, 1450000.00, 100000.00
    )
ON CONFLICT (booking_id) DO NOTHING;

-- 10. CONTRACT_TEMPLATES
-- status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED'. Numbers live in typed columns; terms_content holds wording only.
-- Version 1 is archived: contracts 1 and 2 were signed on it. Version 2 adds the fees and traffic fine, total-loss and car-swap clauses.
INSERT INTO contract_templates (contract_template_id, version_no, title, terms_content,
                                deposit_rate, late_fee_multiplier, free_cancellation_hours,
                                free_cancel_after_deposit_hours, pickup_wait_hours,
                                status, created_by, created_at, activated_at)
VALUES
    (1, 1, 'Hợp đồng thuê xe ô tô tự lái',
     E'Quyền và nghĩa vụ của Bên A\n'
     || E'1. Bàn giao xe đúng thời gian và địa điểm đã thỏa thuận; xe ở tình trạng kỹ thuật tốt, có đủ giấy đăng ký, giấy chứng nhận đăng kiểm và bảo hiểm trách nhiệm dân sự bắt buộc còn hiệu lực.\n'
     || E'2. Cùng Bên B lập biên bản bàn giao, ghi nhận số km, mức nhiên liệu và tình trạng ngoại thất, nội thất khi giao và khi nhận lại xe.\n'
     || E'3. Chịu chi phí bảo dưỡng định kỳ và các hư hỏng do hao mòn tự nhiên của xe.\n'
     || E'\nQuyền và nghĩa vụ của Bên B\n'
     || E'4. Xuất trình bản gốc căn cước công dân và giấy phép lái xe phù hợp với hạng xe khi nhận xe.\n'
     || E'5. Sử dụng xe đúng mục đích, tuân thủ pháp luật về giao thông đường bộ và chịu trách nhiệm về mọi vi phạm giao thông, kể cả phạt nguội, phát sinh trong thời gian thuê.\n'
     || E'6. Không giao xe cho người khác điều khiển khi chưa được Bên A đồng ý; không cầm cố, thế chấp, cho thuê lại xe; không dùng xe để chở hàng cấm hoặc đua xe.\n'
     || E'7. Bồi thường thiệt hại khi xe bị hư hỏng hoặc mất mát do lỗi của Bên B, căn cứ biên bản bàn giao và chi phí sửa chữa thực tế.\n'
     || E'8. Trả xe đúng thời hạn, đúng địa điểm; tự chi trả phí cầu đường, bến bãi phát sinh.',
     0.5000, 2.00, 72, 1, 2,
     'ARCHIVED', 1, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP - INTERVAL '30 days'),
    (2, 2, 'Hợp đồng thuê xe ô tô tự lái',
     E'Quyền và nghĩa vụ của Bên A\n'
     || E'1. Bàn giao xe đúng thời gian và địa điểm đã thỏa thuận; xe ở tình trạng kỹ thuật tốt, có đủ giấy đăng ký, giấy chứng nhận đăng kiểm và bảo hiểm trách nhiệm dân sự bắt buộc còn hiệu lực.\n'
     || E'2. Cùng Bên B lập biên bản bàn giao, ghi nhận số km, mức nhiên liệu và tình trạng ngoại thất, nội thất khi giao và khi nhận lại xe.\n'
     || E'3. Chịu chi phí bảo dưỡng định kỳ và các hư hỏng do hao mòn tự nhiên của xe.\n'
     || E'\nQuyền và nghĩa vụ của Bên B\n'
     || E'4. Xuất trình bản gốc căn cước công dân và giấy phép lái xe phù hợp với hạng xe khi nhận xe.\n'
     || E'5. Sử dụng xe đúng mục đích, tuân thủ pháp luật về giao thông đường bộ và chịu trách nhiệm về mọi vi phạm giao thông, kể cả phạt nguội, phát sinh trong thời gian thuê.\n'
     || E'6. Không giao xe cho người khác điều khiển khi chưa được Bên A đồng ý; không cầm cố, thế chấp, cho thuê lại xe; không dùng xe để chở hàng cấm hoặc đua xe.\n'
     || E'7. Bồi thường thiệt hại khi xe bị hư hỏng hoặc mất mát do lỗi của Bên B, căn cứ biên bản bàn giao và chi phí sửa chữa thực tế.\n'
     || E'8. Trả xe đúng thời hạn, đúng địa điểm; tự chi trả phí cầu đường, bến bãi phát sinh.\n'
     || E'\nPhí phát sinh và phạt nguội\n'
     || E'9. Phí trả xe trễ và các khoản bồi thường (hư hỏng, vệ sinh, phát sinh khác) được lập biên bản và thanh toán khi quyết toán lúc trả xe; quyết toán xong thì hợp đồng được đóng. Phạt nguội do cơ quan công an giải quyết trực tiếp với Bên B; Bên A chỉ thông báo qua email đơn thuê liên quan và không thu khoản tiền này.\n'
     || E'\nXe hư hỏng không thể sửa chữa hoặc không thể thu hồi\n'
     || E'10. Áp dụng khi xe bị tổn thất toàn bộ (đơn vị giám định hoặc gara chính hãng kết luận không thể sửa chữa, hoặc chi phí sửa chữa vượt 70% giá trị xe), hoặc khi xe không thể thu hồi (mất cắp, bị chiếm giữ, Bên B không hoàn trả) do lỗi của Bên B. Khi đó Bên B bồi thường:\n'
     || E'a) Giá trị thị trường của xe tại thời điểm xảy ra sự việc, theo định giá của đơn vị thẩm định độc lập, trừ đi số tiền bảo hiểm Bên A thực nhận (nếu có);\n'
     || E'b) Chi phí cứu hộ, cẩu kéo, lưu bãi và giám định thực tế phát sinh;\n'
     || E'c) Thiệt hại do xe ngừng khai thác: đơn giá thuê theo ngày nhân với số ngày từ khi xảy ra sự việc đến khi hoàn tất bồi thường, tối đa 30 ngày.\n'
     || E'Khoản bồi thường được thanh toán riêng, ngoài tiền thuê.\n'
     || E'\nĐổi xe khi bàn giao\n'
     || E'11. Biển số ghi trong hợp đồng là xe dự kiến. Nếu đến giờ nhận xe mà xe dự kiến chưa sẵn sàng, Bên A giao cho Bên B xe khác cùng loại xe đã đặt, giá thuê giữ nguyên; việc đổi xe này không làm thay đổi hợp đồng. Bên B muốn hủy thì áp dụng điều khoản hủy đặt xe như thông thường.\n'
     || E'12. Nếu không còn xe cùng loại để giao, việc đặt xe được hủy và Bên A hoàn lại cho Bên B 100% tiền cọc ngay tại điểm giao xe, kể cả khi đã quá thời hạn hủy miễn phí.\n'
     || E'13. Xe thực tế giao cho Bên B là xe ghi trong biên bản bàn giao; mọi quyền và nghĩa vụ theo hợp đồng này áp dụng cho xe đó.',
     0.5000, 2.00, 72, 1, 2,
     'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
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
-- payment_type: 'DEPOSIT' | 'FINAL' | 'REFUND' | 'ISSUE_COMPENSATION'
-- The deposit is 50% of the rent (contract terms), so settlement never refunds: REFUND only follows a timely cancellation.
-- Booking 1 settlement: rent 1,350,000 - deposit 675,000 + issue fee 200,000 = 875,000.
-- status: 'PENDING' | 'PAID' | 'FAILED'
INSERT INTO payments (payment_id, booking_id, transaction_code, payment_type, amount, payment_method, status, paid_at)
VALUES 
    (1, 1, 'TXN-DEP-1001', 'DEPOSIT', 675000.00, 'VNPAY', 'PAID', CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (2, 1, 'FINAL-1-1002', 'FINAL', 875000.00, 'CASH', 'PAID', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (4, 2, 'TXN-DEP-2001', 'DEPOSIT', 750000.00, 'VNPAY', 'PAID', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (5, 3, 'TXN-DEP-3001', 'DEPOSIT', 1450000.00, 'VNPAY', 'PAID', CURRENT_TIMESTAMP - INTERVAL '1 hour')
ON CONFLICT (payment_id) DO NOTHING;

-- 13. REVIEWS
-- rating: 1..5
INSERT INTO reviews (review_id, booking_id, rating, comment, created_at)
VALUES 
    (1, 1, 5, 'Xe sạch sẽ, bàn giao nhanh chóng, rất hài lòng!', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (review_id) DO UPDATE SET comment = EXCLUDED.comment;

-- 14. VEHICLE_INSPECTIONS
-- status: 'SCHEDULED' | 'PASSED' | 'FAILED'
INSERT INTO vehicle_inspections (vehicle_inspection_id, vehicle_id, scheduled_date, completed_at, status, notes)
VALUES 
    (1, 1, CURRENT_DATE - 10, CURRENT_TIMESTAMP - INTERVAL '10 days', 'PASSED', 'Bảo dưỡng định kỳ 10.000km, xe vận hành hoàn hảo.'),
    (2, 4, CURRENT_DATE - 1, NULL, 'SCHEDULED', 'Lịch chẩn đoán pin xe điện và hệ thống cảm biến ADAS.')
ON CONFLICT (vehicle_inspection_id) DO UPDATE SET notes = EXCLUDED.notes;

-- 15. ISSUES
-- issue_type: 'COLLISION' | 'DAMAGE' | 'OTHER'; status: 'OPEN' | 'RESOLVED' | 'CANCELLED'
-- The amount owed is a booking_fees row (fee_type 'ISSUE_COMPENSATION'), not a column here.
INSERT INTO issues (issue_id, booking_id, employee_id, issue_type, subject, description, status,
                    occurred_at, reported_at, resolved_at, resolution_note, notified_at)
VALUES
    (1, 1, 2, 'DAMAGE', 'Vết trầy xước nhẹ ở cản trước', 'Phát hiện vết trầy dài khoảng 15 cm ở cản trước khi trả xe.', 'RESOLVED',
     CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days',
     'Khách hàng đồng ý bồi thường mức cố định 200.000 VNĐ.', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (issue_id) DO UPDATE SET subject = EXCLUDED.subject, description = EXCLUDED.description, resolution_note = EXCLUDED.resolution_note;

-- 15b. ISSUE_EVIDENCES (files live in the private issue upload folder)
INSERT INTO issue_evidences (issue_evidence_id, issue_id, file_name, uploaded_at)
VALUES
    (1, 1, 'issue_1.jpg', CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (issue_evidence_id) DO NOTHING;

-- 16. BOOKING_FEES
-- fee_type: 'RENTAL' | 'LATE_FEE' | 'ISSUE_COMPENSATION'
-- payment_id: the payment that settled the fee (booking 1's settlement is payment 2); NULL means still owed.
INSERT INTO booking_fees (booking_fee_id, booking_id, fee_type, amount, description, issue_id, payment_id, created_at)
VALUES
    (1, 1, 'ISSUE_COMPENSATION', 200000.00, 'Khoản bồi thường vết xước cản trước theo sự cố #1', 1, 2, CURRENT_TIMESTAMP - INTERVAL '2 days')
ON CONFLICT (booking_fee_id) DO UPDATE SET description = EXCLUDED.description;

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
SELECT setval(pg_get_serial_sequence('handovers', 'handover_id'), COALESCE(MAX(handover_id), 1)) FROM handovers;
SELECT setval(pg_get_serial_sequence('issue_evidences', 'issue_evidence_id'), COALESCE(MAX(issue_evidence_id), 1)) FROM issue_evidences;
SELECT setval(pg_get_serial_sequence('booking_fees', 'booking_fee_id'), COALESCE(MAX(booking_fee_id), 1)) FROM booking_fees;

COMMIT;


-- Additional physical vehicles per model for rich inventory

BEGIN;

INSERT INTO vehicles (vehicle_id, vehicle_type_id, color, year, odometer, registration_expiry, status)
VALUES
    (21, 1, 'Xanh thẫm', 2023, 9717, CURRENT_DATE + INTERVAL '13 months', 'MAINTENANCE'),
    (22, 1, 'Trắng ngọc trai', 2022, 17328, CURRENT_DATE + INTERVAL '7 months', 'AVAILABLE'),
    (23, 1, 'Vàng Champagne', 2023, 32439, CURRENT_DATE + INTERVAL '23 months', 'AVAILABLE'),
    (24, 2, 'Đỏ Ruby', 2024, 13189, CURRENT_DATE + INTERVAL '19 months', 'AVAILABLE'),
    (25, 2, 'Đen ánh kim', 2024, 25541, CURRENT_DATE + INTERVAL '18 months', 'AVAILABLE'),
    (26, 2, 'Đen ánh kim', 2025, 8164, CURRENT_DATE + INTERVAL '23 months', 'AVAILABLE'),
    (27, 3, 'Trắng ngọc trai', 2023, 21965, CURRENT_DATE + INTERVAL '8 months', 'AVAILABLE'),
    (28, 3, 'Xám mặt trăng', 2025, 44660, CURRENT_DATE + INTERVAL '18 months', 'AVAILABLE'),
    (29, 3, 'Đen ánh kim', 2023, 38005, CURRENT_DATE + INTERVAL '14 months', 'MAINTENANCE'),
    (30, 4, 'Xanh thẫm', 2024, 6665, CURRENT_DATE + INTERVAL '14 months', 'AVAILABLE'),
    (31, 4, 'Xám mặt trăng', 2022, 16826, CURRENT_DATE + INTERVAL '18 months', 'AVAILABLE'),
    (32, 4, 'Vàng Champagne', 2025, 12363, CURRENT_DATE + INTERVAL '21 months', 'AVAILABLE'),
    (33, 5, 'Xám mặt trăng', 2025, 41242, CURRENT_DATE + INTERVAL '23 months', 'RENTED'),
    (34, 5, 'Đen ánh kim', 2022, 10185, CURRENT_DATE + INTERVAL '21 months', 'AVAILABLE'),
    (35, 5, 'Đen ánh kim', 2025, 28009, CURRENT_DATE + INTERVAL '19 months', 'AVAILABLE'),
    (36, 6, 'Xám mặt trăng', 2024, 10310, CURRENT_DATE + INTERVAL '9 months', 'AVAILABLE'),
    (37, 6, 'Bạc ánh kim', 2022, 43979, CURRENT_DATE + INTERVAL '14 months', 'AVAILABLE'),
    (38, 6, 'Trắng ngọc trai', 2024, 35021, CURRENT_DATE + INTERVAL '11 months', 'AVAILABLE'),
    (39, 7, 'Trắng ngọc trai', 2023, 40182, CURRENT_DATE + INTERVAL '13 months', 'AVAILABLE'),
    (40, 7, 'Bạc ánh kim', 2023, 34148, CURRENT_DATE + INTERVAL '23 months', 'AVAILABLE'),
    (41, 7, 'Xanh thẫm', 2023, 23428, CURRENT_DATE + INTERVAL '19 months', 'RENTED'),
    (42, 8, 'Đen ánh kim', 2023, 17725, CURRENT_DATE + INTERVAL '20 months', 'AVAILABLE'),
    (43, 8, 'Xanh thẫm', 2022, 7652, CURRENT_DATE + INTERVAL '13 months', 'MAINTENANCE'),
    (44, 8, 'Đỏ Ruby', 2022, 36695, CURRENT_DATE + INTERVAL '7 months', 'AVAILABLE'),
    (45, 9, 'Đen bóng đêm', 2023, 33996, CURRENT_DATE + INTERVAL '10 months', 'RENTED'),
    (46, 9, 'Đỏ Ruby', 2025, 29941, CURRENT_DATE + INTERVAL '19 months', 'RENTED'),
    (47, 9, 'Trắng ngọc trai', 2025, 25236, CURRENT_DATE + INTERVAL '9 months', 'AVAILABLE'),
    (48, 10, 'Bạc ánh kim', 2025, 15025, CURRENT_DATE + INTERVAL '20 months', 'AVAILABLE'),
    (49, 10, 'Đen ánh kim', 2022, 38427, CURRENT_DATE + INTERVAL '20 months', 'AVAILABLE'),
    (50, 10, 'Đen bóng đêm', 2023, 29282, CURRENT_DATE + INTERVAL '21 months', 'AVAILABLE'),
    (51, 11, 'Đen bóng đêm', 2024, 30722, CURRENT_DATE + INTERVAL '14 months', 'MAINTENANCE'),
    (52, 11, 'Xanh thẫm', 2022, 40957, CURRENT_DATE + INTERVAL '15 months', 'MAINTENANCE'),
    (53, 11, 'Trắng ngọc trai', 2025, 35954, CURRENT_DATE + INTERVAL '7 months', 'AVAILABLE'),
    (54, 12, 'Đen ánh kim', 2022, 18414, CURRENT_DATE + INTERVAL '11 months', 'RENTED'),
    (55, 12, 'Đen ánh kim', 2025, 41251, CURRENT_DATE + INTERVAL '7 months', 'AVAILABLE'),
    (56, 12, 'Xanh thẫm', 2024, 28938, CURRENT_DATE + INTERVAL '16 months', 'AVAILABLE'),
    (57, 13, 'Trắng ngọc trai', 2025, 43708, CURRENT_DATE + INTERVAL '8 months', 'AVAILABLE'),
    (58, 13, 'Xám mặt trăng', 2023, 25872, CURRENT_DATE + INTERVAL '22 months', 'AVAILABLE'),
    (59, 13, 'Xám mặt trăng', 2022, 39346, CURRENT_DATE + INTERVAL '20 months', 'AVAILABLE'),
    (60, 14, 'Đen ánh kim', 2022, 39256, CURRENT_DATE + INTERVAL '14 months', 'AVAILABLE'),
    (61, 14, 'Xanh thẫm', 2024, 36122, CURRENT_DATE + INTERVAL '16 months', 'RENTED'),
    (62, 14, 'Xám mặt trăng', 2022, 3232, CURRENT_DATE + INTERVAL '19 months', 'AVAILABLE'),
    (63, 15, 'Đen bóng đêm', 2025, 39759, CURRENT_DATE + INTERVAL '11 months', 'AVAILABLE'),
    (64, 15, 'Trắng ngọc trai', 2024, 41175, CURRENT_DATE + INTERVAL '23 months', 'AVAILABLE'),
    (65, 15, 'Đỏ Ruby', 2022, 26449, CURRENT_DATE + INTERVAL '15 months', 'AVAILABLE'),
    (66, 16, 'Vàng Champagne', 2023, 18514, CURRENT_DATE + INTERVAL '17 months', 'AVAILABLE'),
    (67, 16, 'Đỏ Ruby', 2025, 19263, CURRENT_DATE + INTERVAL '11 months', 'AVAILABLE'),
    (68, 16, 'Xanh thẫm', 2023, 33166, CURRENT_DATE + INTERVAL '21 months', 'AVAILABLE'),
    (69, 17, 'Vàng Champagne', 2024, 21258, CURRENT_DATE + INTERVAL '12 months', 'AVAILABLE'),
    (70, 17, 'Vàng Champagne', 2024, 4808, CURRENT_DATE + INTERVAL '22 months', 'AVAILABLE'),
    (71, 17, 'Đen ánh kim', 2025, 25654, CURRENT_DATE + INTERVAL '7 months', 'MAINTENANCE'),
    (72, 18, 'Đen ánh kim', 2025, 40787, CURRENT_DATE + INTERVAL '22 months', 'AVAILABLE'),
    (73, 18, 'Xanh thẫm', 2024, 31265, CURRENT_DATE + INTERVAL '6 months', 'AVAILABLE'),
    (74, 18, 'Xám mặt trăng', 2024, 29764, CURRENT_DATE + INTERVAL '9 months', 'AVAILABLE'),
    (75, 19, 'Vàng Champagne', 2025, 14405, CURRENT_DATE + INTERVAL '12 months', 'AVAILABLE'),
    (76, 19, 'Xám mặt trăng', 2024, 16774, CURRENT_DATE + INTERVAL '6 months', 'RENTED'),
    (77, 19, 'Xanh thẫm', 2025, 14120, CURRENT_DATE + INTERVAL '20 months', 'MAINTENANCE'),
    (78, 20, 'Đen ánh kim', 2023, 23343, CURRENT_DATE + INTERVAL '16 months', 'AVAILABLE'),
    (79, 20, 'Xanh thẫm', 2025, 43060, CURRENT_DATE + INTERVAL '7 months', 'AVAILABLE'),
    (80, 20, 'Vàng Champagne', 2025, 29191, CURRENT_DATE + INTERVAL '12 months', 'AVAILABLE')
ON CONFLICT (vehicle_id) DO UPDATE SET color = EXCLUDED.color, year = EXCLUDED.year, odometer = EXCLUDED.odometer, status = EXCLUDED.status;

INSERT INTO license_plates (license_plate_id, vehicle_id, plate, province, valid_from, valid_to)
VALUES
    (21, 21, '51L-125.45', 'Cần Thơ', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (22, 22, '51L-704.64', 'Cần Thơ', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (23, 23, '51K-674.35', 'Bình Dương', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (24, 24, '51K-877.30', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (25, 25, '43A-204.21', 'Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (26, 26, '51K-847.68', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (27, 27, '43A-691.34', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (28, 28, '30K-987.22', 'Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (29, 29, '43A-463.36', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (30, 30, '51H-573.58', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (31, 31, '51K-924.50', 'Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (32, 32, '43A-317.93', 'Cần Thơ', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (33, 33, '30K-862.81', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (34, 34, '30K-241.75', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (35, 35, '51H-911.97', 'Cần Thơ', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (36, 36, '30L-666.11', 'Đồng Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (37, 37, '51H-564.10', 'Đồng Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (38, 38, '30K-256.57', 'Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (39, 39, '43A-999.49', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (40, 40, '61B-935.18', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (41, 41, '30L-640.87', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (42, 42, '43A-548.76', 'Cần Thơ', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (43, 43, '51K-702.80', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (44, 44, '51K-334.18', 'Cần Thơ', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (45, 45, '61B-319.79', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (46, 46, '51L-199.94', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (47, 47, '51K-789.93', 'Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (48, 48, '30K-294.78', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (49, 49, '30K-995.19', 'Đồng Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (50, 50, '30K-270.62', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (51, 51, '60A-102.59', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (52, 52, '61B-258.34', 'Cần Thơ', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (53, 53, '51K-865.50', 'Bình Dương', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (54, 54, '51K-620.20', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (55, 55, '30K-692.86', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (56, 56, '43A-367.36', 'Bình Dương', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (57, 57, '30L-568.50', 'Cần Thơ', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (58, 58, '51L-650.37', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (59, 59, '43A-391.30', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (60, 60, '51L-999.27', 'Cần Thơ', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (61, 61, '30L-719.36', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (62, 62, '51K-194.91', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (63, 63, '51H-752.43', 'Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (64, 64, '51L-807.29', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (65, 65, '60A-230.15', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (66, 66, '30K-782.23', 'Cần Thơ', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (67, 67, '51H-522.13', 'Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (68, 68, '51L-491.14', 'Hà Nội', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (69, 69, '30K-328.13', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (70, 70, '30L-459.92', 'Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (71, 71, '51H-694.43', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (72, 72, '43A-546.87', 'Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (73, 73, '51K-825.65', 'Đà Nẵng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (74, 74, '43A-738.50', 'Cần Thơ', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (75, 75, '30L-667.26', 'Đồng Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (76, 76, '30L-515.80', 'Bình Dương', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (77, 77, '43A-576.66', 'Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (78, 78, '30L-627.94', 'TP. Hồ Chí Minh', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (79, 79, '30K-250.13', 'Hải Phòng', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL),
    (80, 80, '60A-744.83', 'Đồng Nai', CURRENT_TIMESTAMP - INTERVAL '1 year', NULL)
ON CONFLICT (license_plate_id) DO UPDATE SET plate = EXCLUDED.plate, province = EXCLUDED.province;

-- Update sequences
SELECT setval(pg_get_serial_sequence('license_plates', 'license_plate_id'), (SELECT MAX(license_plate_id) FROM license_plates));
SELECT setval(pg_get_serial_sequence('vehicles', 'vehicle_id'), (SELECT MAX(vehicle_id) FROM vehicles));

COMMIT;


