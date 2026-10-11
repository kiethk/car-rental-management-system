-- Demo fleet only. No customer passwords or employee accounts are seeded.
BEGIN;
INSERT INTO vehicle_types (name,brand,category,seats,transmission,fuel_type,price_per_hour,deposit_amount,description)
VALUES ('Vios','Toyota','sedan',5,'automatic','gasoline',180000,5000000,'Xe sedan 5 chỗ'),
       ('CR-V','Honda','suv',7,'automatic','gasoline',320000,8000000,'Xe SUV 7 chỗ');
INSERT INTO license_plates (plate,province,registration_expiry)
VALUES ('51A-123.45','TP.HCM',CURRENT_DATE + INTERVAL '1 year'),
       ('51A-234.56','TP.HCM',CURRENT_DATE + INTERVAL '1 year'),
       ('51A-345.67','TP.HCM',CURRENT_DATE + INTERVAL '1 year');
INSERT INTO vehicles (vehicle_type_id,license_plate_id,color,year,odometer,status)
SELECT t.vehicle_type_id,p.license_plate_id,v.color,2025,10000,'available'
FROM (VALUES ('Vios','51A-123.45','White'),('Vios','51A-234.56','Black'),('CR-V','51A-345.67','White')) AS v(name,plate,color)
JOIN vehicle_types t ON t.name=v.name
JOIN license_plates p ON p.plate=v.plate;
INSERT INTO slots (vehicle_type_id,start_time,end_time,capacity,status)
SELECT t.vehicle_type_id,
       (CURRENT_DATE + make_interval(days => d.day_offset, hours => 9)) AT TIME ZONE 'Asia/Ho_Chi_Minh',
       (CURRENT_DATE + make_interval(days => d.day_offset, hours => 18)) AT TIME ZONE 'Asia/Ho_Chi_Minh',
       (SELECT COUNT(*) FROM vehicles v WHERE v.vehicle_type_id=t.vehicle_type_id), 'open'
FROM vehicle_types t CROSS JOIN (VALUES (2),(3)) AS d(day_offset);
COMMIT;
