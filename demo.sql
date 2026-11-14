-- ============================================================
-- Скрипт демонстрации возможностей MiniDB
-- ============================================================

-- 1. Просмотр структуры таблиц
SHOW TABLES;

-- 2. Выборка всех владельцев (10 записей)
SELECT * FROM owners;

-- 3. Выборка всех автомобилей (20 записей)
TABLE cars;

-- 4. Выборка всех записей сервисного обслуживания (15 записей)
SELECT * FROM services;

-- 5. Просмотр схемы таблицы (DESCRIBE)
DESCRIBE cars;

-- 6. Фильтрация по числовому значению (автомобили дороже $40,000)
SELECT brand, price, reg_date FROM cars WHERE price > 40000;

-- 7. Фильтрация по логическому флагу (только активные авто)
SELECT id, brand, price FROM cars WHERE active = true;

-- 8. Фильтрация по внешнему ключу (автомобили владельца id = 5)
SELECT id, brand, price FROM cars WHERE owner_id = 5;

-- 9. Сервисные работы дороже $500
SELECT id, car_id, service_type, cost FROM services WHERE cost >= 500;

-- 10. Фильтрация по дате регистрации (автомобили с 2023 года)
SELECT brand, price, reg_date FROM cars WHERE reg_date >= '2023-01-01';

-- 11. Точный поиск по марке (использует индекс idx_cars_brand)
SELECT * FROM cars WHERE brand = 'Porsche 911';

-- 12. Вставка новой записи с проверкой первичного ключа
INSERT INTO cars VALUES (21, 'Subaru Outback', 34000.0, 1, TRUE, '2023-07-15');

-- 13. Попытка вставки дублирующего PRIMARY KEY (вызовет корректную ошибку)
INSERT INTO cars (id, brand) VALUES (1, 'Fake Car');

-- 14. Обновление записи (UPDATE)
UPDATE cars SET price = 36000.0 WHERE id = 21;

-- 15. Удаление записи (DELETE)
DELETE FROM cars WHERE id = 21;

-- 16. Итоговая проверка
SELECT * FROM cars WHERE id = 21;
