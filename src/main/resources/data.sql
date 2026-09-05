INSERT INTO products (name, price, stock_quantity, description, category, brand, tags)
SELECT '무선 마우스', 15000, 100, '저소음 클릭과 인체공학 디자인을 적용한 사무용 무선 마우스', '주변기기', 'AI SHOPPING', '재택근무,사무용,저소음,무선'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = '무선 마우스');

INSERT INTO products (name, price, stock_quantity, description, category, brand, tags)
SELECT '블루투스 키보드', 29000, 100, '노트북과 태블릿을 빠르게 전환할 수 있는 휴대용 키보드', '주변기기', 'AI SHOPPING', '재택근무,휴대용,블루투스,키보드'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = '블루투스 키보드');

INSERT INTO products (name, price, stock_quantity, description, category, brand, tags)
SELECT 'USB 허브', 39000, 100, 'HDMI와 USB 3.0 포트를 제공하는 USB-C 멀티 허브', '액세서리', 'AI SHOPPING', 'USB-C,멀티포트,노트북,재택근무'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name = 'USB 허브');

UPDATE products
SET brand = 'AI SHOPPING'
WHERE brand = 'SKALA'
  AND name IN ('무선 마우스', '블루투스 키보드', 'USB 허브');
