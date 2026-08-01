# TripPoint Backend

A modern GraphQL-based backend API for managing trips, expenses, and itineraries. Built with Spring Boot 3.5.4, Kotlin 2.2.0, and PostgreSQL.

## 🚀 Project Overview

TripPoint is a comprehensive travel management platform that enables users to:
- Plan and organize trips
- Track shared expenses
- Create and manage detailed itineraries
- Leverage AI-powered features for trip planning

## 🏗️ Architecture

### Technology Stack

- **Language**: Kotlin 2.2.0
- **Framework**: Spring Boot 3.5.4
- **API**: GraphQL with GraphiQL IDE
- **Authentication**: JWT-based security
- **Database**: PostgreSQL 17
- **Build Tool**: Gradle (Kotlin DSL)
- **Java Version**: 21

### Project Structure

```
src/main/kotlin/com/trippoint/backend/
├── config/              # Spring configuration (Security, etc.)
├── auth/                # Authentication & user management
│   ├── entity/         # User entity
│   ├── service/        # JwtService, AuthService, PasswordService
│   ├── repository/     # UserRepository
│   ├── graphql/        # AuthQuery, AuthMutation
│   └── dto/            # Auth-related DTOs
├── trip/               # Trip management
├── expense/            # Expense tracking & splitting
├── itinerary/          # Trip itineraries
├── ai/                 # AI-powered features
└── common/             # Shared utilities
```

## 📦 Key Dependencies

- **Spring Security**: Secure authentication and authorization
- **Spring Data JPA**: ORM with Hibernate
- **Spring GraphQL**: GraphQL API implementation
- **JJWT**: JWT token generation and validation
- **Jackson**: JSON processing (Kotlin module)
- **Flyway**: Database migrations (currently disabled)
- **PostgreSQL Driver**: Database connectivity
- **Spring Validation**: Input validation

## 🚀 Quick Start

```bash
# 1. Start database
docker-compose up -d

# 2. Run application
./gradlew bootRun

# 3. Open GraphiQL IDE
http://localhost:8081/graphiql

# 4. Test register & me endpoints
# See ANDROID_INTEGRATION.md for examples
```

## 🛠️ Setup & Configuration

### Prerequisites

- Java 21+
- PostgreSQL 17
- Gradle 8.x

### Environment Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd backend
   ```

2. **Start PostgreSQL** (using Docker Compose)
   ```bash
   docker-compose up -d
   ```

   This will start a PostgreSQL container with:
   - Database: `trippoint`
   - Username: `trippoint`
   - Password: `trippoint`
   - Port: `5432`

3. **Configure Application**

   The default configuration is in `application.yml`:
   ```yaml
   server:
     port: 8081
   
   spring:
     application:
       name: trippoint-backend
     datasource:
       url: jdbc:postgresql://localhost:5432/trippoint
       username: trippoint
       password: trippoint
   ```

4. **Build & Run**
   ```bash
   ./gradlew build
   ./gradlew bootRun
   ```

   Or run directly from IDE

## 🔌 API Endpoints

### GraphQL Endpoint
- **URL**: `http://localhost:8081/graphql`
- **GraphiQL IDE**: `http://localhost:8081/graphiql`
- **WebSocket**: `http://localhost:8081/graphql/ws` (for subscriptions)

### Authentication

The API uses JWT tokens for authentication. Implement the following flow:

1. **Register User** - Create new user account
2. **Login** - Authenticate and receive JWT token
3. **Use Token** - Include token in Authorization header for protected queries/mutations

Example GraphQL request header:
```
Authorization: Bearer <jwt-token>
```

## 🔐 Authentication Module

The auth module provides:

- **User Registration**: Create new user accounts
- **User Login**: Authenticate users and generate JWT tokens
- **Token Management**: JWT token creation, validation, and refresh
- **Password Security**: Bcrypt-based password hashing

### Auth Services

- `JwtService`: Handles JWT token operations
- `AuthService`: User authentication and registration logic
- `PasswordService`: Password hashing and validation
- `UserRepository`: Database access for user entities

## 📊 Database Schema

The application uses Hibernate with `ddl-auto: create-drop` (development mode):
- Database schema is automatically created on startup
- Tables are dropped on shutdown

For production, configure Flyway migrations (currently disabled in config).

### Key Entities

- **User**: User accounts with credentials and profile information

## 📝 Features Completed

### ✅ Authentication & Security
- User registration and login
- JWT token generation and validation
- Password hashing with Bcrypt
- Spring Security configuration
- GraphQL mutations for auth endpoints

### ✅ Project Structure
- Multi-module architecture (auth, trip, expense, itinerary, ai)
- Separation of concerns (entities, services, repositories, GraphQL)
- Kotlin DSL Gradle build configuration

### ✅ API Framework
- GraphQL integration with Spring Boot
- GraphiQL IDE for API testing
- WebSocket support for real-time features
- Input validation with Spring Validation

### ✅ Database Layer
- PostgreSQL integration
- Spring Data JPA/Hibernate ORM
- Automatic schema generation (development)

### ✅ Development Tools
- Docker Compose setup for local development
- Spring Boot DevTools for fast reloading
- GraphiQL IDE for GraphQL queries

## 🧪 Testing

Run tests with:
```bash
./gradlew test
```

The project includes:
- JUnit 5 test framework
- Kotlin test extensions
- H2 in-memory database for tests

## 📚 Configuration Details

### Time Zone
- Configured to UTC by default (set in `BackendApplication.kt`)
- Response serialization uses `Asia/Kolkata` timezone

### Jackson Configuration
- `default-property-inclusion: non_null` - Excludes null values from responses
- Proper timezone handling for timestamps

### Hibernate Configuration
- `format_sql: true` - Pretty-prints SQL in logs
- `show-sql: false` - SQL statements not shown (disable for debugging)

## 🚦 Development Workflow

1. **Local Development**
   ```bash
   ./gradlew bootRun
   ```

2. **Debug Mode**
   - Set `debug: true` in `application.yml`
   - View SQL statements by setting `show-sql: true`

3. **Database Inspection**
   - Use PostgreSQL client to connect: `psql -h localhost -U trippoint -d trippoint`

4. **API Testing**
   - Access GraphiQL at `http://localhost:8081/graphiql`
   - Write and test GraphQL queries/mutations interactively

## 🔄 Common Commands

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Clean build
./gradlew clean build

# Check dependencies
./gradlew dependencies

# Start database only
docker-compose up -d postgres

# Stop database
docker-compose down
```

## 📖 Useful Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/3.5.4/reference/html/)
- [Spring GraphQL Guide](https://docs.spring.io/spring-boot/4.1.0/reference/web/spring-graphql.html)
- [Spring Security Reference](https://docs.spring.io/spring-boot/4.1.0/reference/web/spring-security.html)
- [Kotlin Language Reference](https://kotlinlang.org/docs/home.html)
- [GraphQL Documentation](https://graphql.org/)

## 📚 Development Documentation

- **[SPRINT_1_SUMMARY.md](./SPRINT_1_SUMMARY.md)** - Overview of Me endpoint implementation
- **[SPRINT_1_ME_ENDPOINT.md](./SPRINT_1_ME_ENDPOINT.md)** - Detailed technical implementation
- **[ANDROID_INTEGRATION.md](./ANDROID_INTEGRATION.md)** - Android developer guide with code examples

## 📋 Environment Variables

No additional environment variables required for local development. All configuration is in `application.yml`.

For production deployment, consider:
- `DATABASE_URL`: PostgreSQL connection string
- `JWT_SECRET_KEY`: Secret key for JWT signing
- `SERVER_PORT`: Application port
- `SPRING_PROFILES_ACTIVE`: Environment-specific configuration

## 🐛 Troubleshooting

### Database Connection Failed
- Ensure PostgreSQL is running: `docker-compose ps`
- Verify credentials in `application.yml`
- Check PostgreSQL port (default 5432)

### Port Already in Use
- Change port in `application.yml` (default 8081)
- Or kill existing process: `lsof -i :8081`

### Build Failures
- Clear cache: `./gradlew clean`
- Ensure Java 21+ is installed: `java -version`
- Sync Gradle: `./gradlew --refresh-dependencies`

## 📄 License

[Add your license information here]

## 👥 Contributors

[Add contributor information here]

---

**Last Updated**: August 2024
**Version**: 0.0.1-SNAPSHOT
