# Notification System for Telework Requests

## Overview
This notification system automatically sends notifications when telework requests are created, approved, or rejected. It eliminates the need for email notifications by providing real-time in-app notifications.

## Architecture

### Services
1. **Eureka Server** (Port 8761) - Service discovery
2. **User Service** (Port 9001) - User management and team information
3. **Notification Service** (Port 3001) - Notification storage and management
4. **Teletravail Service** (Port 7001) - Telework request management

### Database
- **notification_db** - Stores all notifications
- **teletravaildb** - Stores telework requests
- **userdb** - Stores user information

## How It Works

### 1. When a Telework Request is Created
- Employee submits a telework request
- System automatically:
  - Saves the request to database
  - Fetches team leaders and managers for the employee's team
  - Creates notifications for all team leaders and managers
  - Notification type: `TELEWORK_REQUEST_CREATED`

### 2. When a Request is Approved/Rejected
- Team leader or manager approves/rejects the request
- System automatically:
  - Updates the request status
  - Creates a notification for the employee
  - Notification types: `TELEWORK_REQUEST_APPROVED` or `TELEWORK_REQUEST_REJECTED`

### 3. Frontend Display
- Users can view notifications at `/notifications`
- Unread count is displayed
- Notifications can be marked as read or deleted

## API Endpoints

### Notification Service (Port 3001)
- `POST /api/notifications` - Create a new notification
- `GET /api/notifications/user/{userId}` - Get user's notifications
- `PUT /api/notifications/{id}/read` - Mark notification as read
- `DELETE /api/notifications/{id}` - Delete notification

### User Service (Port 9001)
- `GET /api/users/public/team/{teamName}/leaders-managers` - Get team leaders and managers

### Teletravail Service (Port 7001)
- `POST /api/teletravail/requests` - Create telework request (triggers notifications)
- `PUT /api/teletravail/requests/{id}/status` - Update request status (triggers notifications)

## Configuration

### Notification Service
```properties
server.port=3001
spring.application.name=notification-service
spring.datasource.url=jdbc:mysql://localhost:3306/notification_db
```

### Teletravail Service
```properties
notification.service.url=http://localhost:3001
user.service.url=http://localhost:9001/api/users
```

## Starting the System

### Option 1: Use the batch script
```bash
./start-notification-system.bat
```

### Option 2: Start manually
1. Start Eureka Server: `cd eureka-server && mvn spring-boot:run`
2. Start User Service: `cd user-service && mvn spring-boot:run`
3. Start Notification Service: `cd notification-service && mvn spring-boot:run`
4. Start Teletravail Service: `cd teletravail-service && mvn spring-boot:run`

## Testing the System

1. **Create a telework request** through the frontend or API
2. **Check notifications** for team leaders/managers at `http://localhost:3001/api/notifications/user/{userId}`
3. **Approve/reject the request** through the frontend or API
4. **Check notifications** for the employee

## Error Handling

- If notification service is down, telework requests still work
- Failed notifications are logged but don't break the main functionality
- Circuit breakers prevent cascading failures

## Frontend Integration

The Angular frontend includes:
- Notification service for API calls
- Notifications component for display
- Route protection with AuthGuard
- Real-time notification updates

## Database Schema

### Notification Table
```sql
CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'UNREAD',
    related_entity_id BIGINT,
    related_entity_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## Troubleshooting

1. **Notifications not being created**: Check if notification service is running on port 3001
2. **Team leaders not found**: Verify user service is running and team data is correct
3. **Database connection issues**: Ensure MySQL is running and credentials are correct
4. **Service discovery issues**: Check if Eureka Server is running on port 8761 