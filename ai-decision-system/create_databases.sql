-- Create databases for microservices architecture
-- Run this script in MySQL to create all necessary databases

-- Analytics Service Database
CREATE DATABASE IF NOT EXISTS analytics_db;
USE analytics_db;

-- Contact Service Database
CREATE DATABASE IF NOT EXISTS contactdb;
USE contactdb;

-- Notification Service Database
CREATE DATABASE IF NOT EXISTS notification_db;
USE notification_db;

-- Planning Service Database
CREATE DATABASE IF NOT EXISTS planningdb;
USE planningdb;

-- Reservation Service Database
CREATE DATABASE IF NOT EXISTS reservationdb;
USE reservationdb;

-- Teletravail Service Database
CREATE DATABASE IF NOT EXISTS teletravaildb;
USE teletravaildb;

-- User Service Database
CREATE DATABASE IF NOT EXISTS SopraHR;
USE SopraHR;

-- Show all created databases
SHOW DATABASES; 