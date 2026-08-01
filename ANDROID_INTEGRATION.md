# For Android Developers: TripPoint Backend Ready 🚀

## Current Status
✅ **Authentication infrastructure is production-ready**

Your backend can now handle:
1. ✅ User registration with firstName, lastName
2. ✅ JWT token generation
3. ✅ **Me query** - First authenticated endpoint
4. ⬜ Login endpoint (coming next)
5. ⬜ Refresh token endpoint (coming next)

## API Endpoints Available Now

### Register User
```graphql
mutation {
  register(input: {
    email: "alice@example.com"
    password: "SecurePass123!"
    firstName: "Alice"
    lastName: "Johnson"
  }) {
    user {
      id
      email
      firstName
      lastName
    }
    token          # ← Store this in EncryptedSharedPreferences!
    refreshToken   # ← Also store this for refresh flow
  }
}
```

**Response Example**:
```json
{
  "data": {
    "register": {
      "user": {
        "id": "a1b2c3d4-e5f6-4a5b-8c9d-e0f1a2b3c4d5",
        "email": "alice@example.com",
        "firstName": "Alice",
        "lastName": "Johnson"
      },
      "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhMWIyYzNkNC1lNWY2LTRhNWItOGM5ZC1lMGYxYTJiM2M0ZDUiLCJpYXQiOjE3MjI1MTI0NzcsImV4cCI6MTcyMjU5ODg3N30.oXpf...",
      "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhMWIyYzNkNC1lNWY2LTRhNWItOGM5ZC1lMGYxYTJiM2M0ZDUiLCJ0eXBlIjoicmVmcmVzaCIsImlhdCI6MTcyMjUxMjQ3NywiZXhwIjoxNzIzMTE3Mjc3fQ.mK5l..."
    }
  }
}
```

### Get Current User Profile (with JWT)
```graphql
query {
  me {
    id
    email
    firstName
    lastName
  }
}
```

**Request Headers**:
```
Authorization: Bearer {token_from_register_response}
Content-Type: application/json
```

**Success Response**:
```json
{
  "data": {
    "me": {
      "id": "a1b2c3d4-e5f6-4a5b-8c9d-e0f1a2b3c4d5",
      "email": "alice@example.com",
      "firstName": "Alice",
      "lastName": "Johnson"
    }
  }
}
```

**Error Response (missing/invalid JWT)**:
```json
{
  "errors": [
    {
      "message": "User not authenticated",
      "locations": [{"line": 2, "column": 3}],
      "path": ["me"]
    }
  ]
}
```

## Android Implementation Guide

### Step 1: Define GraphQL Queries

```kotlin
// Register.graphql
mutation Register($email: String!, $password: String!, $firstName: String!, $lastName: String!) {
  register(input: { 
    email: $email
    password: $password
    firstName: $firstName
    lastName: $lastName
  }) {
    user {
      id
      email
      firstName
      lastName
    }
    token
    refreshToken
  }
}

// Me.graphql
query GetMe {
  me {
    id
    email
    firstName
    lastName
  }
}
```

### Step 2: Store JWT Securely

```kotlin
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secret_shared_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// After registration
encryptedPrefs.edit().apply {
    putString("jwt_token", response.token)
    putString("refresh_token", response.refreshToken)
    apply()
}
```

### Step 3: Call Me Endpoint (First Authenticated API)

```kotlin
// Using Apollo Client (or any GraphQL client)

fun fetchUserProfile(apolloClient: ApolloClient) {
    val token = encryptedPrefs.getString("jwt_token", null) ?: return
    
    apolloClient.query(GetMeQuery())
        .addHttpHeader("Authorization", "Bearer $token")
        .toFlow()
        .collect { response ->
            when {
                response.hasErrors() -> {
                    // Handle auth error - likely token expired
                    // Clear stored token, redirect to login
                    encryptedPrefs.edit().clear().apply()
                    navigateToLogin()
                }
                response.data != null -> {
                    // Success - display user profile
                    val user = response.data!!.me
                    displayProfile(user.id, user.email, user.firstName, user.lastName)
                }
            }
        }
}

fun displayProfile(id: String, email: String, firstName: String?, lastName: String?) {
    // Update UI with user profile
    welcomeText.text = "Welcome, ${firstName ?: email}!"
    emailText.text = email
    // ... etc
}
```

### Step 4: Handle Auth Errors

```kotlin
sealed class AuthError {
    object InvalidCredentials : AuthError()
    object TokenExpired : AuthError()
    object NetworkError : AuthError()
    data class UnknownError(val message: String) : AuthError()
}

fun handleMeQueryError(error: ApolloException): AuthError {
    return when {
        error.message?.contains("User not authenticated") == true -> {
            AuthError.TokenExpired
        }
        error.message?.contains("401") == true -> {
            AuthError.InvalidCredentials
        }
        error is java.io.IOException -> {
            AuthError.NetworkError
        }
        else -> AuthError.UnknownError(error.message ?: "Unknown error")
    }
}

// Usage
when (val error = handleMeQueryError(exception)) {
    is AuthError.TokenExpired -> clearAuthAndNavigateToLogin()
    is AuthError.InvalidCredentials -> showAuthError("Invalid credentials")
    is AuthError.NetworkError -> showNetworkError()
    is AuthError.UnknownError -> showError(error.message)
}
```

## GraphQL Client Setup (Recommended)

### Apollo Client
```gradle
dependencies {
    implementation("com.apollographql.apollo3:apollo-runtime:3.8.1")
}
```

```kotlin
val apolloClient = ApolloClient.Builder()
    .serverUrl("http://localhost:8081/graphql") // Change for production
    .build()
```

### Ktor Client (Alternative)
```kotlin
val httpClient = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}

suspend fun callGraphQL(query: String, variables: Map<String, Any>?) {
    httpClient.post<String>("http://localhost:8081/graphql") {
        contentType(ContentType.Application.Json)
        header("Authorization", "Bearer $token")
        setBody(mapOf("query" to query, "variables" to variables))
    }
}
```

## Testing Checklist

- [ ] Register new user → Get JWT token
- [ ] Store JWT in EncryptedSharedPreferences
- [ ] Call `me` query with JWT header
- [ ] Verify user profile displays correctly
- [ ] Call `me` query WITHOUT JWT header → See error
- [ ] Call `me` query with INVALID JWT → See error
- [ ] Manually test in GraphiQL at http://localhost:8081/graphiql

## Server Details

**URL**: `http://localhost:8081/graphql` (development)  
**GraphiQL IDE**: `http://localhost:8081/graphiql` (testing)  
**Database**: PostgreSQL running in Docker

## What's Coming Next

| Sprint | Feature | Status | ETA |
|--------|---------|--------|-----|
| 1 | Me (Current User) | ✅ Done | Now |
| 2 | Login Endpoint | ⏳ Next | Days |
| 2 | Refresh Token | ⏳ Next | Days |
| 3 | Create Trip | ⏳ Soon | Week |
| 3 | Update Trip | ⏳ Soon | Week |
| 4 | Add Expense | ⏳ Later | 2 weeks |
| 4 | Split Expense | ⏳ Later | 2 weeks |

## Documentation Links

- [Backend README](./README.md) - Full setup instructions
- [Sprint 1 Details](./SPRINT_1_ME_ENDPOINT.md) - Technical implementation
- [GraphQL Schema](./src/main/resources/graphql/auth.graphqls) - Available types

## Quick Questions?

**Q: Where do I add the JWT token?**  
A: In the HTTP request header: `Authorization: Bearer {token}`

**Q: How long does the JWT last?**  
A: Default is 24 hours (86400000ms in application.yml)

**Q: What if the token expires?**  
A: The `refreshToken` can be used to get a new JWT (endpoint coming in Sprint 2)

**Q: Can I test this in GraphiQL?**  
A: Yes! Register first, then click "HTTP Headers" and add the Authorization header

---

**Backend is ready for Android integration! 🎉**

Start by implementing the register flow and me query in your app.

