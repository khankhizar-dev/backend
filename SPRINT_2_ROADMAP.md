# 🚀 Sprint 2: Complete Authentication Features

## Status Overview

```
Sprint 1 (COMPLETE)          Sprint 2 (NEXT)
✅ Register                  ⬜ Refresh Token Mutation
✅ Me (Get User)             ⬜ Logout API  
✅ JWT Infrastructure        ⬜ Email Verification (optional)
✅ DB User Verification      ⬜ Role-Based Authorization
✅ Password Hashing          ⬜ Token Blacklist
                             ⬜ Forgot Password (optional)
```

**🎯 Priority:** Refresh Token → Logout → Role-Based Auth → Token Blacklist

---

## 🔧 Task 1: Add Login Mutation to GraphQL (30 min)

**Status:** 90% Done (AuthService.login() exists, just need GraphQL wrapper)

### Files to Update

**File:** `src/main/kotlin/com/trippoint/backend/auth/graphql/AuthMutation.kt`

```kotlin
@Controller
class AuthMutation(
    private val authService: AuthService
) {

    @MutationMapping
    fun login(email: String, password: String): AuthPayload {
        return authService.login(email, password)
    }

    @MutationMapping
    fun register(input: RegisterInput): AuthPayload {
        return authService.register(
            RegisterRequest(
                email = input.email,
                password = input.password,
                firstName = input.firstName,
                lastName = input.lastName
            )
        )
    }
}
```

**File:** `src/main/resources/graphql/auth.graphqls`

```graphql
type Query {
    me: User!
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

input RegisterInput {
    email: String!
    password: String!
    firstName: String!
    lastName: String!
}

type AuthPayload {
    user: User!
    token: String!
    refreshToken: String!
}
```

**Test:**
```graphql
mutation {
  login(email: "khizar@test.com", password: "Password@123") {
    token
    refreshToken
    user {
      id
      email
    }
  }
}
```

---

## 🔄 Task 2: Refresh Token Mutation (45 min)

### Implementation

**File:** `src/main/kotlin/com/trippoint/backend/auth/service/AuthService.kt` (add method)

```kotlin
fun refreshToken(refreshToken: String): AuthPayload {
    val userId = jwtService.getUserIdFromToken(refreshToken)
        ?: throw IllegalArgumentException("Invalid refresh token")
    
    val user = userRepository.findById(userId)
        .orElseThrow { IllegalArgumentException("User not found") }
    
    require(user.isActive) { "User account is inactive" }
    
    val newToken = jwtService.generateToken(userId)
    val newRefreshToken = jwtService.generateRefreshToken(userId)
    
    return AuthPayload(
        user = UserResponse(
            id = userId,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName
        ),
        token = newToken,
        refreshToken = newRefreshToken
    )
}
```

**File:** `src/main/kotlin/com/trippoint/backend/auth/graphql/AuthMutation.kt` (add method)

```kotlin
@MutationMapping
fun refreshToken(refreshToken: String): AuthPayload {
    return authService.refreshToken(refreshToken)
}
```

**File:** `src/main/resources/graphql/auth.graphqls` (update mutations)

```graphql
type Mutation {
    register(input: RegisterInput!): AuthPayload!
    login(email: String!, password: String!): AuthPayload!
    refreshToken(refreshToken: String!): AuthPayload!
}
```

**Test:**
```graphql
mutation {
  refreshToken(refreshToken: "eyJhbGciOi...") {
    token
    refreshToken
    user {
      id
    }
  }
}
```

---

## 🚪 Task 3: Logout API with Token Blacklist (2 hrs)

### Step 1: Add Token Blacklist Table

**File:** `src/main/resources/db/migration/V003__add_token_blacklist.sql`

```sql
CREATE TABLE token_blacklist (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id VARCHAR(500) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blacklisted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_token_blacklist_user_id ON token_blacklist(user_id);
CREATE INDEX idx_token_blacklist_expires_at ON token_blacklist(expires_at);
```

### Step 2: Create Blacklist Repository

**File:** `src/main/kotlin/com/trippoint/backend/auth/repository/TokenBlacklistRepository.kt`

```kotlin
@Repository
interface TokenBlacklistRepository : JpaRepository<TokenBlacklist, UUID> {
    fun findByTokenId(tokenId: String): TokenBlacklist?
    fun deleteByExpiresAtBefore(expiration: LocalDateTime)
}
```

### Step 3: Create Token Blacklist Entity

**File:** `src/main/kotlin/com/trippoint/backend/auth/entity/TokenBlacklist.kt`

```kotlin
@Entity
@Table(name = "token_blacklist")
data class TokenBlacklist(
    @Id
    @GeneratedValue
    val id: UUID? = null,
    
    @Column(nullable = false, unique = true)
    val tokenId: String,
    
    @Column(nullable = false)
    val userId: UUID,
    
    @Column(nullable = false)
    val blacklistedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val expiresAt: LocalDateTime
)
```

### Step 4: Update JWT Service

**File:** `src/main/kotlin/com/trippoint/backend/auth/service/JwtService.kt` (add methods)

```kotlin
fun getTokenId(token: String): String? {
    return try {
        val claims = jwtParser.parseSignedClaims(token)
        claims.payload.id
    } catch (e: Exception) {
        null
    }
}

fun isTokenBlacklisted(token: String): Boolean {
    val tokenId = getTokenId(token) ?: return false
    return tokenBlacklistRepository.findByTokenId(tokenId) != null
}
```

### Step 5: Update Authentication Filter

**File:** `src/main/kotlin/com/trippoint/backend/config/JwtAuthenticationFilter.kt` (add check)

```kotlin
if (header != null && header.startsWith("Bearer ")) {
    val token = header.removePrefix("Bearer ")
    
    // Check if token is blacklisted
    if (jwtService.isTokenBlacklisted(token)) {
        filterChain.doFilter(request, response)
        return
    }
    
    val userId = jwtService.getUserIdFromToken(token)
    // ... rest of filter
}
```

### Step 6: Add Logout Mutation

**File:** `src/main/kotlin/com/trippoint/backend/auth/service/AuthService.kt` (add method)

```kotlin
fun logout(token: String, userId: UUID): Boolean {
    val tokenId = jwtService.getTokenId(token)
        ?: throw IllegalArgumentException("Invalid token")
    
    val expiresAt = jwtService.getTokenExpiration(token)
        ?: throw IllegalArgumentException("Invalid token")
    
    tokenBlacklistRepository.save(
        TokenBlacklist(
            tokenId = tokenId,
            userId = userId,
            expiresAt = expiresAt
        )
    )
    
    return true
}
```

**File:** `src/main/kotlin/com/trippoint/backend/auth/graphql/AuthMutation.kt` (add method)

```kotlin
@MutationMapping
fun logout(
    @ContextValue("Authorization") authorization: String?,
    authentication: Authentication
): Boolean {
    val token = authorization?.removePrefix("Bearer ")
        ?: throw IllegalArgumentException("No token provided")
    
    val principal = authentication.principal as UserPrincipal
    
    return authService.logout(token, principal.userId)
}
```

**Test:**
```graphql
mutation {
  logout
}

# Response: { "data": { "logout": true } }
# Then try me query with same token → Should fail
```

---

## 👥 Task 4: Role-Based Authorization (4-5 hrs)

### Step 1: Add Role Enum

**File:** `src/main/kotlin/com/trippoint/backend/auth/entity/UserRole.kt`

```kotlin
enum class UserRole {
    USER,
    ADMIN,
    MODERATOR
}
```

### Step 2: Update User Entity

**File:** `src/main/kotlin/com/trippoint/backend/auth/entity/User.kt` (add field)

```kotlin
@Enumerated(EnumType.STRING)
@Column(nullable = false)
val role: UserRole = UserRole.USER
```

### Step 3: Update UserPrincipal

**File:** `src/main/kotlin/com/trippoint/backend/auth/security/UserPrincipal.kt`

```kotlin
class UserPrincipal(
    val userId: UUID,
    val email: String,
    val role: UserRole
) : UserDetails {
    
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return mutableListOf(SimpleGrantedAuthority("ROLE_${role.name}"))
    }
    // ... rest of class
}
```

### Step 4: Add Authorization Annotations

**File:** `src/main/kotlin/com/trippoint/backend/auth/graphql/UserQuery.kt`

```kotlin
@QueryMapping
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
fun me(authentication: Authentication): UserResponse {
    // ... implementation
}
```

**File:** `src/main/kotlin/com/trippoint/backend/auth/graphql/AdminQuery.kt` (new)

```kotlin
@Controller
class AdminQuery(
    private val userRepository: UserRepository
) {
    
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun allUsers(): List<UserResponse> {
        return userRepository.findAll().map { user ->
            UserResponse(
                id = user.id!!,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName
            )
        }
    }
}
```

---

## 📋 Task Breakdown

| Task | Time | Depends On |
|------|------|-----------|
| 1. Login Mutation | 30 min | ✅ Auth Service |
| 2. Refresh Token | 45 min | ✅ JWT Service |
| 3. Logout (Blacklist) | 2 hrs | Complete #1, #2 |
| 4. Role-Based Auth | 4 hrs | Complete #3 |

**Total:** ~7 hours (~1 day of dev work)

---

## ✅ Success Criteria

- [ ] Login mutation returns JWT + refresh token
- [ ] Refresh token generates new JWT without re-entering password
- [ ] Logout adds token to blacklist
- [ ] Blacklisted tokens are rejected immediately
- [ ] Admin queries require ROLE_ADMIN
- [ ] User queries require ROLE_USER or ROLE_ADMIN
- [ ] All tests pass
- [ ] Build succeeds

---

## 🧪 Integration Test Plan

```graphql
# 1. Register
mutation {
  register(input: {
    email: "test@example.com"
    password: "Password@123"
    firstName: "Test"
    lastName: "User"
  }) {
    token
    refreshToken
  }
}
# Save token and refreshToken

# 2. Test me with token
query {
  me { id email }
}
# Headers: Authorization: Bearer {token}
# ✅ Should work

# 3. Test refresh token
mutation {
  refreshToken(refreshToken: "{refreshToken}") {
    token
  }
}
# ✅ Should return new token

# 4. Logout
mutation {
  logout
}
# Headers: Authorization: Bearer {token}
# ✅ Should return true

# 5. Test blacklisted token
query {
  me { id }
}
# Headers: Authorization: Bearer {token}  (the one we logged out)
# ❌ Should fail with "User not authenticated"
```

---

## 📊 After Sprint 2

```
✅ Register
✅ Me (Get Current User)
✅ Login
✅ Refresh Token
✅ Logout (with Blacklist)
✅ Role-Based Authorization
✅ Token Revocation

Ready for → Trips CRUD (Sprint 3)
```

---

## 🎯 Recommendation

**Start with:** Refresh Token Mutation (easy win)  
**Then:** Login Mutation (basically done)  
**Then:** Logout + Blacklist (most complex)  
**Finally:** Role-Based Auth (extends everything)

This order ensures you have complete auth ASAP for Android integration.

