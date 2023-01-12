
-- Make sure the tests are always running on a clean h2 database
DROP ALL OBJECTS;
DROP TABLE IF EXISTS SPRING_SESSION_ATTRIBUTES;
DROP TABLE IF EXISTS SPRING_SESSION;
CREATE SCHEMA IF NOT EXISTS test_general;
SET SCHEMA test_general;
