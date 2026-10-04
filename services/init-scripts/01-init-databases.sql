-- Create databases if they do not exist
SELECT 'CREATE DATABASE "order"'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'order')\gexec

SELECT 'CREATE DATABASE "product"'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'product')\gexec

SELECT 'CREATE DATABASE "payment"'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment')\gexec
