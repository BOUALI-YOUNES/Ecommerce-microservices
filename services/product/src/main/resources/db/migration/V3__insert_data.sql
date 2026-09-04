INSERT INTO category (id, name, description)
VALUES
    (nextval('category_seq'), 'Electronics', 'Electronic devices and accessories'),
    (nextval('category_seq'), 'Laptops', 'Laptops and portable computers'),
    (nextval('category_seq'), 'Smartphones', 'Smartphones and mobile devices'),
    (nextval('category_seq'), 'Accessories', 'Computer and electronic accessories'),
    (nextval('category_seq'), 'Gaming', 'Gaming devices and peripherals');



INSERT INTO product (
    id,
    name,
    description,
    available_quantity,
    price,
    category_id
)
VALUES
    (
        nextval('product_seq'),
        'Dell Inspiron 15',
        '15-inch laptop with Intel Core i5 processor',
        25,
        749.99,
        (SELECT id FROM category WHERE name = 'Laptops')
    ),
    (
        nextval('product_seq'),
        'Lenovo ThinkPad E14',
        'Business laptop with Intel Core i7 processor',
        15,
        999.99,
        (SELECT id FROM category WHERE name = 'Laptops')
    ),
    (
        nextval('product_seq'),
        'iPhone 15',
        'Apple smartphone with 128GB storage',
        30,
        899.99,
        (SELECT id FROM category WHERE name = 'Smartphones')
    ),
    (
        nextval('product_seq'),
        'Samsung Galaxy S24',
        'Samsung flagship smartphone',
        20,
        849.99,
        (SELECT id FROM category WHERE name = 'Smartphones')
    ),
    (
        nextval('product_seq'),
        'Wireless Mouse',
        'Ergonomic wireless mouse with USB receiver',
        100,
        29.99,
        (SELECT id FROM category WHERE name = 'Accessories')
    ),
    (
        nextval('product_seq'),
        'Mechanical Keyboard',
        'RGB mechanical keyboard',
        50,
        79.99,
        (SELECT id FROM category WHERE name = 'Accessories')
    ),
    (
        nextval('product_seq'),
        'Sony WH-1000XM5',
        'Wireless noise-cancelling headphones',
        35,
        349.99,
        (SELECT id FROM category WHERE name = 'Electronics')
    ),
    (
        nextval('product_seq'),
        'Gaming Monitor 27"',
        '27-inch 144Hz gaming monitor',
        18,
        299.99,
        (SELECT id FROM category WHERE name = 'Gaming')
    ),
    (
        nextval('product_seq'),
        'PlayStation 5',
        'Sony PlayStation 5 gaming console',
        10,
        499.99,
        (SELECT id FROM category WHERE name = 'Gaming')
    ),
    (
        nextval('product_seq'),
        'Xbox Wireless Controller',
        'Wireless controller for Xbox and PC',
        45,
        59.99,
        (SELECT id FROM category WHERE name = 'Gaming')
    );