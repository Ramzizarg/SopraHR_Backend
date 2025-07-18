import os
from dotenv import load_dotenv

load_dotenv()

# Database Configurations for each microservice
DATABASES = {
    'analytics': {
        'host': os.getenv('DB_HOST', 'localhost'),
        'port': int(os.getenv('DB_PORT', 3306)),
        'user': os.getenv('DB_USER', 'root'),
        'password': os.getenv('DB_PASSWORD', ''),
        'database': os.getenv('ANALYTICS_DB', 'analytics_db')
    },
    'contact': {
        'host': os.getenv('DB_HOST', 'localhost'),
        'port': int(os.getenv('DB_PORT', 3306)),
        'user': os.getenv('DB_USER', 'root'),
        'password': os.getenv('DB_PASSWORD', ''),
        'database': os.getenv('CONTACT_DB', 'contact_db')
    },
    'notification': {
        'host': os.getenv('DB_HOST', 'localhost'),
        'port': int(os.getenv('DB_PORT', 3306)),
        'user': os.getenv('DB_USER', 'root'),
        'password': os.getenv('DB_PASSWORD', ''),
        'database': os.getenv('NOTIFICATION_DB', 'notification_db')
    },
    'planning': {
        'host': os.getenv('DB_HOST', 'localhost'),
        'port': int(os.getenv('DB_PORT', 3306)),
        'user': os.getenv('DB_USER', 'root'),
        'password': os.getenv('DB_PASSWORD', ''),
        'database': os.getenv('PLANNING_DB', 'planning_db')
    },
    'reservation': {
        'host': os.getenv('DB_HOST', 'localhost'),
        'port': int(os.getenv('DB_PORT', 3306)),
        'user': os.getenv('DB_USER', 'root'),
        'password': os.getenv('DB_PASSWORD', ''),
        'database': os.getenv('RESERVATION_DB', 'reservation_db')
    },
    'teletravail': {
        'host': os.getenv('DB_HOST', 'localhost'),
        'port': int(os.getenv('DB_PORT', 3306)),
        'user': os.getenv('DB_USER', 'root'),
        'password': os.getenv('DB_PASSWORD', ''),
        'database': os.getenv('TELETRAVAIL_DB', 'teletravail_db')
    },
    'user': {
        'host': os.getenv('DB_HOST', 'localhost'),
        'port': int(os.getenv('DB_PORT', 3306)),
        'user': os.getenv('DB_USER', 'root'),
        'password': os.getenv('DB_PASSWORD', ''),
        'database': os.getenv('USER_DB', 'user_db')
    }
}

# AI Decision Thresholds
TELEWORK_THRESHOLDS = {
    'max_percentage': 60,  # Maximum allowed telework percentage
    'warning_threshold': 50,  # Warning level
    'optimal_range': (20, 40)  # Optimal telework range
}

RESERVATION_THRESHOLDS = {
    'critical_high': 90,  # Critical high occupancy (>90%)
    'warning_high': 80,   # Warning high occupancy (>80%)
    'optimal_range': (70, 85),  # Optimal occupancy range
    'warning_low': 60,    # Warning low occupancy (<60%)
    'critical_low': 50    # Critical low occupancy (<50%)
}

# API Endpoints for your microservices
API_ENDPOINTS = {
    'analytics': 'http://localhost:5001/api/analytics',
    'contact': 'http://localhost:4001/api/contacts',
    'notification': 'http://localhost:3001/api/notifications',
    'planning': 'http://localhost:8001/api/planning',
    'reservation': 'http://localhost:6001/api/reservations',
    'teletravail': 'http://localhost:7001/api/teletravail',
    'user': 'http://localhost:9001/api/users'
}

# Model Configuration
MODEL_CONFIG = {
    'prediction_days': 30,  # Days to predict
    'confidence_threshold': 0.8,  # Minimum confidence for decisions
    'retrain_frequency': 7  # Retrain model every 7 days
} 