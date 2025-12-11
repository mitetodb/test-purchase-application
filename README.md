# Test Purchase Application

A comprehensive system for managing test purchases (Mystery Shopping orders) used by companies that perform Mystery Shopping services for various clients. The application includes a separate Price Calculation microservice that provides REST API for cost calculations.

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.4.0
- **Build Tool**: Maven
- **Database**: MySQL 8
- **Frontend**: Spring MVC + Thymeleaf
- **Security**: Spring Security
- **Caching**: Spring Cache
- **Scheduling**: Spring Scheduling
- **Inter-service Communication**: Spring Cloud OpenFeign
- **Email**: Spring Mail

## Architecture

The solution consists of two independent Spring Boot applications:

1. **Main Application** (Port 8080) - Core backend system with web interface
2. **Price Calculation Microservice** (Port 8081) - REST API for price calculations

## Features

### Domain Entities
- **Customer** - Client companies that order mystery shopping services
- **Shop** - Retail locations where test purchases are made
- **TestPurchase** - Test purchase orders with items and status tracking
- **User** - Application users with different roles
- **Item** - Products purchased in test purchases
- **Attachment** - Documents and files attached to test purchases

### Functionalities

#### Test Purchase Management
1. Create new test purchase orders
2. Edit existing test purchases
3. Delete test purchases
4. Change test purchase status with history tracking
5. View test purchase details with attachments and history

#### Customer Management
6. Create new customers
7. Edit customer information
8. Delete customers
9. View customer details and associated test purchases

#### Shop Management
10. Create new shops
11. Edit shop information
12. Delete shops

#### Attachment Management
13. Upload attachments to test purchases
14. Delete attachments (admin only)

### Security Features

- **Authentication**: Form-based login
- **Authorization**: Role-based access control
- **Roles**: 
  - ADMIN - Full system access
  - ACCOUNT_MANAGER - Manages assigned customers
  - SALES_MANAGER - Manages shops and sales
  - CUSTOMER - Views own test purchases
  - MYSTERY_SHOPPER - Creates and manages assigned test purchases
- **CSRF Protection**: Enabled
- **Password Reset**: Email-based password reset functionality
- **User Management**: Admins can manage roles, block/unblock users

### Technical Features

- **Caching**: Implemented for Shops and Customers using Spring Cache
- **Scheduling**: 
  - Daily cleanup of expired password reset tokens (cron)
  - Periodic statistics update for old test purchases (fixed delay)
- **Error Handling**: Custom error pages for 404, 403, 400, 500 errors
- **Validation**: Comprehensive validation on DTOs, entities, and service layers
- **Logging**: AOP-based action logging and service-level logging

## Integrations

### Price Calculation Microservice
- **Feign Client**: Used for inter-service communication
- **Endpoints Used**:
  - POST `/api/pricing/calculate` - Calculate test purchase fees
  - PUT `/api/pricing/customers/{id}/base-fee` - Update customer base fee
  - DELETE `/api/pricing/customers/{id}/base-fee` - Delete customer base fee
  - GET `/api/pricing/customers/{id}/base-fee` - Get customer base fee

## Setup Instructions

### Prerequisites
- Java 17
- Maven 3.6+
- Docker (for MySQL)
- MySQL 8

### Database Setup

1. Start MySQL with Docker:
```bash
docker run --name mysql8_db -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=testpurchase_app -d mysql:8
```

### Application Setup

1. **Start Price Calculation Microservice** (Port 8081):
```bash
cd price-calculation-microservice
mvn spring-boot:run
```

2. **Start Main Application** (Port 8080):
```bash
cd test-purchase-application
mvn spring-boot:run
```

### Default Admin Account
- Username: `admin`
- Password: `123123`
- Created automatically on first startup if no users exist

## Repository Links

- **Main Application**: https://github.com/mitetodb/test-purchase-application
- **Price Calculation Microservice**: https://github.com/mitetodb/price-calculation-microservice

## Testing

The application includes:
- Unit tests for services
- Integration tests for repositories
- API tests for controllers
- Security tests

Run tests with:
```bash
mvn test
```

## Project Structure

```
src/main/java/app/
├── client/          # Feign clients for microservice communication
├── config/          # Configuration classes
├── controller/      # MVC controllers
├── exception/       # Custom exceptions and handlers
├── model/           # Entities, DTOs, enums
├── repository/      # JPA repositories
├── security/        # Security configuration
└── service/         # Business logic services
```

## License

This project is part of a Spring Advanced course assignment.
