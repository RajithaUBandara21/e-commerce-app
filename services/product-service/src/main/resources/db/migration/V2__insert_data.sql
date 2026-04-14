-- Insert categories and capture IDs using deterministic values
INSERT INTO category (id, name, description)
VALUES
    (1, 'Electronics', 'Electronic devices and gadgets'),
    (2, 'Books', 'All kinds of books'),
    (3, 'Clothing', 'Men and Women clothing');

-- Insert products with correct foreign keys
INSERT INTO product (id, name, description, available_quantity, price, category_id)
VALUES
    (1, 'iPhone 15', 'Apple smartphone', 50, 1200.00, 1),
    (2, 'Samsung Galaxy S24', 'Samsung flagship phone', 40, 999.99, 1),

    (3, 'Spring Boot Guide', 'Java Spring Boot book', 100, 29.99, 2),
    (4, 'Clean Code', 'Robert C. Martin book', 80, 35.50, 2),

    (5, 'T-Shirt', 'Cotton T-shirt', 200, 15.00, 3),
    (6, 'Jeans', 'Blue denim jeans', 120, 40.00, 3);