# TripPoint Backend Architecture

## Authentication Architecture (Sprint 1 - Implemented)

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Application                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 1. Registration Flow                                 │  │
│  │    ├─ User enters email, password, name              │  │
│  │    └─ Sends: POST /graphql (register mutation)       │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 2. Receive JWT                                        │  │
│  │    └─ Response: { token, refreshToken, user }        │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 3. Store Securely                                    │  │
│  │    └─ Save token in EncryptedSharedPreferences       │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 4. First Auth API Call                               │  │
│  │    ├─ Retrieve token from storage                    │  │
│  │    ├─ Header: Authorization: Bearer {token}          │  │
│  │    └─ POST /graphql (me query) ◄────┐                │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ 5. Display Profile                                   │  │
│  │    └─ Response: { id, email, firstName, lastName }   │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                                │
                                │
                                │
                    HTTP Request/Response
                                │
                                ▼
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Backend                         │
├─────────────────────────────────────────────────────────────┤
│  ┌────────────────────────────────────────────────────────┐ │
│  │  HTTP Layer (Tomcat)                                   │ │
│  │  └─ Receives all GraphQL requests                      │ │
│  └────────────────────────────────────────────────────────┘ │
│                         │                                    │
│                         ▼                                    │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  JwtAuthenticationFilter (NEW - Sprint 1)              │ │
│  │  ├─ Extract Authorization header                       │ │
│  │  ├─ Parse "Bearer {token}"                            │ │
│  │  ├─ Call JwtService.validateToken(token)              │ │
│  │  ├─ Call JwtService.getUserIdFromToken(token)          │ │
│  │  ├─ Create UsernamePasswordAuthenticationToken         │ │
│  │  ├─ Set SecurityContext.authentication                 │ │
│  │  └─ Continue filter chain                              │ │
│  └────────────────────────────────────────────────────────┘ │
│                         │                                    │
│                         ▼                                    │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  GraphQL Dispatcher                                    │ │
│  │  └─ Routes to appropriate resolver                     │ │
│  └────────────────────────────────────────────────────────┘ │
│                         │                                    │
│                    Mutation?   Query?                        │
│                    │            │                            │
│                    ▼            ▼                            │
│  ┌──────────────────┐  ┌─────────────────────────────────┐ │
│  │ AuthMutation     │  │ AuthQuery                       │ │
│  │  register(...)   │  │  me(): UserResponse             │ │
│  │  login(...)      │  │  ├─ Get Authentication          │ │
│  │  logout(...)     │  │  ├─ Extract userId              │ │
│  │                  │  │  ├─ Call AuthService.me(userId) │ │
│  └──────────────────┘  │  └─ Return UserResponse         │ │
│                        └─────────────────────────────────┘ │
│                                   │                         │
│                                   ▼                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  AuthService                                            │ │
│  │  ├─ register(request): AuthPayload                     │ │
│  │  ├─ login(email, password): AuthPayload                │ │
│  │  └─ me(userId): UserResponse ◄──────┐                  │ │
│  └────────────────────────────────────────────────────────┘ │
│                                   │                         │
│                                   ▼                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  UserRepository (Spring Data JPA)                       │ │
│  │  ├─ findById(userId)                                    │ │
│  │  ├─ findByEmail(email)                                  │ │
│  │  ├─ existsByEmail(email)                                │ │
│  │  └─ save(user)                                          │ │
│  └────────────────────────────────────────────────────────┘ │
│                                   │                         │
│                                   ▼                         │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  PostgreSQL Database                                    │ │
│  │  └─ Users table (id, email, passwordHash, firstName, ...) │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow: Register → Me Query

```
1. REGISTRATION
   ┌─────────────────────────────┐
   │ Mutation: register(input)   │
   └─────────────────────────────┘
            │
            ▼
   ┌─────────────────────────────┐
   │ AuthMutation.register()     │
   │  ├─ Validate input          │
   │  └─ Call AuthService        │
   └─────────────────────────────┘
            │
            ▼
   ┌─────────────────────────────┐
   │ AuthService.register()      │
   │  ├─ Hash password           │
   │  ├─ Save user               │
   │  ├─ Generate JWT            │
   │  └─ Generate RefreshToken   │
   └─────────────────────────────┘
            │
            ▼
   ┌─────────────────────────────┐
   │ Response: AuthPayload       │
   │  ├─ user { id, email, ... } │
   │  ├─ token                   │
   │  └─ refreshToken            │
   └─────────────────────────────┘
            │
            ▼
   ┌─────────────────────────────┐
   │ Android Stores:             │
   │  ├─ token (in SharedPrefs)   │
   │  └─ User profile            │
   └─────────────────────────────┘


2. ME QUERY (with JWT)
   ┌─────────────────────────────┐
   │ Request:                    │
   │  query { me { ... } }       │
   │ Header:                     │
   │  Authorization:             │
   │    Bearer {token}           │
   └─────────────────────────────┘
            │
            ▼
   ┌─────────────────────────────┐
   │ JwtAuthenticationFilter      │
   │  ├─ Extract header           │
   │  ├─ Parse Bearer token       │
   │  ├─ Validate JWT             │
   │  ├─ Extract userId           │
   │  └─ Set SecurityContext      │
   └─────────────────────────────┘
            │
            ▼
   ┌─────────────────────────────┐
   │ AuthQuery.me()              │
   │  ├─ Get from SecurityContext │
   │  ├─ Extract userId           │
   │  └─ Call AuthService.me()    │
   └─────────────────────────────┘
            │
            ▼
   ┌─────────────────────────────┐
   │ AuthService.me(userId)      │
   │  └─ Load user from DB        │
   └─────────────────────────────┘
            │
            ▼
   ┌─────────────────────────────┐
   │ Response: UserResponse      │
   │  ├─ id                       │
   │  ├─ email                    │
   │  ├─ firstName                │
   │  └─ lastName                 │
   └─────────────────────────────┘
            │
            ▼
   ┌─────────────────────────────┐
   │ Android Displays Profile    │
   └─────────────────────────────┘
```

## Security Filter Chain

```
HTTP Request
    │
    ├─► CORS Filter (permissive)
    │
    ├─► JwtAuthenticationFilter ◄── NEW in Sprint 1
    │    ├─ No Authorization header? → Continue (filter transparent)
    │    ├─ Invalid JWT? → Continue (no auth, resolver will reject)
    │    ├─ Valid JWT? → Set SecurityContext ✓
    │    └─ Expired JWT? → Continue (resolver will reject)
    │
    ├─► UsernamePasswordAuthenticationFilter (skipped, disabled)
    │
    ├─► GraphQL Endpoint
    │    ├─ Public queries? → Allowed
    │    ├─ Protected queries (me)?
    │    │   ├─ SecurityContext has auth? → Allowed
    │    │   └─ No auth? → Error 401
    │    │
    │    └─ Public mutations (register)? → Allowed
    │        Protected mutations? → Check auth
    │
    ▼
Response
```

## JWT Token Structure

```
Token = Header.Payload.Signature

HEADER:
{
  "alg": "HS256",
  "typ": "JWT"
}

PAYLOAD (Access Token):
{
  "sub": "a1b2c3d4-e5f6-4a5b-8c9d-e0f1a2b3c4d5",  ◄── userId
  "iat": 1722512477,                               ◄── issued at
  "exp": 1722598877                                ◄── expires at (24h later)
}

PAYLOAD (Refresh Token):
{
  "sub": "a1b2c3d4-e5f6-4a5b-8c9d-e0f1a2b3c4d5",
  "type": "refresh",                               ◄── refresh token marker
  "iat": 1722512477,
  "exp": 1723117277                                ◄── expires at (7 days later)
}

SIGNATURE = HMAC-SHA256(
  base64(header) + "." + base64(payload),
  secret_key
)
```

## GraphQL Schema (Current)

```graphql
type Query {
  me: User!                          # ◄── NEW in Sprint 1
}

type Mutation {
  register(input: RegisterInput!): AuthPayload!
  login(email: String!, password: String!): AuthPayload!
}

type User {
  id: ID!
  email: String!
  firstName: String
  lastName: String
}

type AuthPayload {
  user: User!
  token: String!           # JWT access token
  refreshToken: String!    # JWT refresh token
}

input RegisterInput {
  email: String!
  password: String!
  firstName: String!
  lastName: String!
}
```

## Layer Responsibilities

| Layer | Responsibility | Example |
|-------|-----------------|---------|
| **HTTP/Servlet** | Receive request, send response | Tomcat receives POST /graphql |
| **Filter** | Cross-cutting concerns | JwtAuthenticationFilter validates JWT |
| **GraphQL** | Query parsing & routing | Route to AuthQuery.me() |
| **Resolver** | Extract context, call service | Get userId from SecurityContext |
| **Service** | Business logic | Fetch user from database |
| **Repository** | Database access | UserRepository.findById() |
| **Database** | Persist data | PostgreSQL stores user records |

## Error Scenarios

```
Scenario: Me query without JWT
  Request: query { me { id email } }
  Header: (no Authorization)
  ↓
  JwtAuthenticationFilter: No auth header, continue
  SecurityContext: Empty (no auth)
  ↓
  AuthQuery.me():
    authentication = null
    throw IllegalArgumentException("User not authenticated")
  ↓
  Response: { "errors": [{ "message": "User not authenticated" }] }

Scenario: Me query with expired JWT
  Request: query { me { id email } }
  Header: Authorization: Bearer {expired_token}
  ↓
  JwtAuthenticationFilter:
    validateToken() returns false
    Continue without setting auth
  ↓
  SecurityContext: Empty
  ↓
  AuthQuery.me(): Same as above
  ↓
  Response: { "errors": [{ "message": "User not authenticated" }] }

Scenario: Me query with valid JWT
  Request: query { me { id email } }
  Header: Authorization: Bearer {valid_token}
  ↓
  JwtAuthenticationFilter:
    validateToken() returns true
    getUserIdFromToken() returns UUID
    Create Authentication(userId)
    Set SecurityContext.authentication
  ↓
  AuthQuery.me():
    authentication = Authentication(userId)
    userId = UUID.fromString(authentication.name)
    Call authService.me(userId)
  ↓
  AuthService.me(userId):
    Find user in database
    Return UserResponse
  ↓
  Response: { "data": { "me": { "id": "...", "email": "..." } } }
```

## Database Schema (Current)

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR UNIQUE NOT NULL,
    password_hash VARCHAR NOT NULL,
    first_name VARCHAR,
    last_name VARCHAR,
    profile_image_url VARCHAR,
    is_email_verified BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    deleted_at TIMESTAMP
);
```

---

**This architecture is production-ready for:**
- Android integration
- Scaling to multiple services
- Adding more domain features (trips, expenses, etc.)
- JWT refresh and revocation
- Role-based access control (future enhancement)

