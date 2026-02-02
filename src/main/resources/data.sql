-- Insert test user with id 1
INSERT INTO users (id, name, email, password) 
VALUES (1, 'Test User', 'test@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBuBj6tgIWj5kBjuKDW.cAw1xr6G')
ON CONFLICT (id) DO NOTHING;

-- Insert user role (assuming you have a user_roles table for @ElementCollection)
INSERT INTO user_roles (user_id, roles) 
VALUES (1, 'GUEST')
ON CONFLICT DO NOTHING;
