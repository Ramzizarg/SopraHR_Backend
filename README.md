# Workstation Management Application

A comprehensive workstation management system with microservices architecture, AI decision system, and Angular frontend.

## 🏗️ Architecture

### Microservices (Spring Boot)
- **Eureka Server** (Port 8761) - Service discovery
- **Analytics Service** (Port 5001) - Data analytics
- **Contact Service** (Port 4001) - Contact management
- **Notification Service** (Port 3001) - Notifications
- **Planning Service** (Port 8001) - Planning management
- **Reservation Service** (Port 6001) - Workstation reservations
- **Teletravail Service** (Port 7001) - Telework management
- **User Service** (Port 9001) - User management
- **Workstation Service** (Port 8080) - Workstation management

### AI Decision System (Python)
- **AI API** (Port 8000) - FastAPI backend for AI insights
- **AI Dashboard** (Port 8050) - Dash visualization dashboard

### Frontend
- **Angular Application** (Port 4200) - User interface

## 🗄️ Database Configuration

Each microservice has its own database:

| Service | Database Name |
|---------|---------------|
| Analytics | `analytics_db` |
| Contact | `contactdb` |
| Notification | `notification_db` |
| Planning | `planningdb` |
| Reservation | `reservationdb` |
| Teletravail | `teletravaildb` |
| User | `SopraHR` |

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven
- Python 3.8+
- Node.js 16+
- MySQL 8.0+
- Angular CLI

### 1. Database Setup
```bash
# Run the database creation script
cd workstation-spring/ai-decision-system
create_databases.bat
```

### 2. Start All Services (Windows)
```bash
# Run the complete startup script
cd workstation-spring
start-all-services.bat
```

### 3. Manual Startup (Alternative)

#### Start Backend Services
```bash
# 1. Start Eureka Server first
cd eureka-server
mvnw spring-boot:run

# 2. Start other microservices (in separate terminals)
cd analytics-service && mvnw spring-boot:run
cd contact-service && mvnw spring-boot:run
cd notification-service && mvnw spring-boot:run
cd planning-service && mvnw spring-boot:run
cd reservation-service && mvnw spring-boot:run
cd teletravail-service && mvnw spring-boot:run
cd user-service && mvnw spring-boot:run
cd workstation-service && mvnw spring-boot:run
```

#### Start AI System
```bash
cd ai-decision-system
python dashboard_api.py
```

#### Start Frontend
```bash
cd workstation-angular
npm install
ng serve
```

## 🌐 Access Points

### Backend Services
- **Eureka Dashboard**: http://localhost:8761
- **Analytics Service**: http://localhost:5001
- **Contact Service**: http://localhost:4001
- **Notification Service**: http://localhost:3001
- **Planning Service**: http://localhost:8001
- **Reservation Service**: http://localhost:6001
- **Teletravail Service**: http://localhost:7001
- **User Service**: http://localhost:9001
- **Workstation Service**: http://localhost:8080

### AI System
- **AI API**: http://localhost:8000
- **AI Dashboard**: http://localhost:8050
- **AI Health Check**: http://localhost:8000/api/health

### Frontend
- **Angular App**: http://localhost:4200

## 🔧 Configuration

### Environment Variables
The AI system uses a `.env` file in `ai-decision-system/`:
```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=

ANALYTICS_DB=analytics_db
CONTACT_DB=contactdb
NOTIFICATION_DB=notification_db
PLANNING_DB=planningdb
RESERVATION_DB=reservationdb
TELETRAVAIL_DB=teletravaildb
USER_DB=SopraHR
```

### Python Dependencies
```bash
cd ai-decision-system
pip install fastapi uvicorn pandas numpy scikit-learn matplotlib seaborn plotly dash python-multipart pymysql python-dotenv schedule
```

## 📊 AI Features

The AI decision system provides:
- **Real-time Analytics**: Telework and reservation data analysis
- **Predictive Insights**: Future trends and patterns
- **Anomaly Detection**: Unusual patterns in data
- **Recommendations**: AI-powered suggestions for optimization
- **Alerts**: Automated notifications for critical thresholds

## 🛠️ Troubleshooting

### Common Issues

1. **Database Connection Errors**
   - Ensure MySQL is running
   - Check database names match configuration
   - Verify credentials in `.env` file

2. **Port Conflicts**
   - Check if ports are already in use
   - Stop conflicting services
   - Update port configuration if needed

3. **Python Module Errors**
   - Install missing dependencies: `pip install <module-name>`
   - Check Python version compatibility

4. **Service Registration Issues**
   - Ensure Eureka Server starts first
   - Check service URLs in application.properties
   - Verify network connectivity

### Logs
- Check individual service logs in their respective terminal windows
- Eureka dashboard shows service registration status
- AI system logs are displayed in the terminal

## 📁 Project Structure

```
workstation-spring/
├── eureka-server/           # Service discovery
├── analytics-service/        # Analytics microservice
├── contact-service/          # Contact management
├── notification-service/     # Notifications
├── planning-service/         # Planning management
├── reservation-service/      # Reservations
├── teletravail-service/     # Telework management
├── user-service/            # User management
├── workstation-service/      # Workstation management
├── ai-decision-system/      # AI system (Python)
│   ├── dashboard_api.py     # FastAPI backend
│   ├── ai_dashboard.py      # Dash dashboard
│   ├── data_collector.py    # Data collection
│   ├── ai_analyzer.py       # AI analysis
│   └── config.py           # Configuration
├── start-all-services.bat   # Complete startup script
└── README.md               # This file
```

## 🤝 Contributing

1. Follow the microservices architecture
2. Maintain database separation
3. Update configuration files as needed
4. Test all services before deployment

## 📞 Support

For issues or questions:
1. Check the troubleshooting section
2. Review service logs
3. Verify configuration files
4. Ensure all prerequisites are met 