# ✅ Feature Branch Checklist - Development Process

## Current Status
- 📍 **Branch:** `feature/auth-login`
- 📍 **Base:** `develop`
- 📍 **Status:** 🚀 READY TO START

---

## 📋 Feature: Login Mutation (auth-login)

### Pre-Development
- [x] Branch created: `feature/auth-login`
- [x] Branched from: `develop`
- [x] Latest from develop pulled
- [ ] Implementation plan reviewed
- [ ] Dependencies checked

### Implementation Tasks
- [ ] **AuthMutation.kt** - Create GraphQL mutation resolver
  - [ ] login(email, password) method
  - [ ] Delegates to authService.login()
  - [ ] Returns AuthPayload
  
- [ ] **auth.graphqls** - Update schema
  - [ ] Add login mutation
  - [ ] Update Mutation type
  - [ ] Ensure proper types
  
- [ ] **Error Handling**
  - [ ] User not found error
  - [ ] Invalid password error
  - [ ] Account inactive error
  
- [ ] **Testing**
  - [ ] Valid credentials → Returns JWT
  - [ ] Invalid email → Error
  - [ ] Wrong password → Error
  - [ ] Build succeeds

### Code Review Checklist
- [ ] Code follows Kotlin style guide
- [ ] No hardcoded values
- [ ] Error messages are clear
- [ ] No security issues
- [ ] Comments where needed
- [ ] Build passes: `./gradlew build`
- [ ] Tests pass: `./gradlew test`

### Pre-Push
- [ ] All changes committed
- [ ] Commit messages are clear
- [ ] No debug code left
- [ ] No untracked files

### Push & PR
- [ ] Push to origin: `git push -u origin feature/auth-login`
- [ ] Create PR on GitHub
  - Title: `feat(auth): implement login mutation`
  - Base: `develop` ← Compare: `feature/auth-login`
  - Description: Include changes, testing steps
- [ ] Request code review
- [ ] Address feedback

### Post-Merge
- [ ] PR approved and merged
- [ ] Local branch deleted: `git branch -d feature/auth-login`
- [ ] Remote branch deleted: `git push origin --delete feature/auth-login`
- [ ] Verify develop branch: `git checkout develop; git pull origin develop`

---

## 🛠️ Implementation Details

### What's Already Done
- ✅ AuthService.login() exists
  - Takes email, password
  - Validates credentials
  - Returns AuthPayload with JWT
  
### What We're Adding
- ⬜ AuthMutation.kt - GraphQL resolver wrapper
- ⬜ GraphQL schema mutation

### Code Template

**File:** `src/main/kotlin/com/trippoint/backend/auth/graphql/AuthMutation.kt`

```kotlin
package com.trippoint.backend.auth.graphql

import com.trippoint.backend.auth.dto.RegisterInput
import com.trippoint.backend.auth.dto.AuthPayload
import com.trippoint.backend.auth.dto.RegisterRequest
import com.trippoint.backend.auth.service.AuthService
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

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

---

## 🧪 Testing Commands

```bash
# Build
./gradlew build

# Test specific class
./gradlew test --tests AuthServiceTest

# View build output
./gradlew build --info

# Clean and rebuild
./gradlew clean build
```

### GraphQL Test Query

```graphql
mutation {
  login(email: "khizar@test.com", password: "Password@123") {
    token
    refreshToken
    user {
      id
      email
      firstName
      lastName
    }
  }
}
```

**Expected Response:**
```json
{
  "data": {
    "login": {
      "token": "eyJhbGciOi...",
      "refreshToken": "eyJhbGciOi...",
      "user": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "email": "khizar@test.com",
        "firstName": "Khizar",
        "lastName": "Khan"
      }
    }
  }
}
```

---

## 📝 Commit Message Template

```
feat(auth): implement login mutation

- Add login method to AuthMutation resolver
- Update auth.graphqls with login mutation
- Login returns user profile + JWT + refresh token
- Validates email and password through AuthService
- Handles invalid credentials error

The login mutation completes the authentication
flow alongside register, enabling users to obtain
JWT tokens for authenticated API access.

Testing:
- Valid credentials return JWT ✓
- Invalid email throws error ✓
- Wrong password throws error ✓
- Build succeeds ✓

Related: Sprint 2 (auth-login)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
```

---

## 🚀 Development Steps

### Step 1: Create AuthMutation.kt
```bash
# File: src/main/kotlin/com/trippoint/backend/auth/graphql/AuthMutation.kt
# Copy template above
```

### Step 2: Update auth.graphqls
```bash
# File: src/main/resources/graphql/auth.graphqls
# Update schema with login mutation
```

### Step 3: Build & Test
```bash
./gradlew clean build
```

### Step 4: Commit
```bash
git add src/main/kotlin/com/trippoint/backend/auth/graphql/AuthMutation.kt
git add src/main/resources/graphql/auth.graphqls
git commit -m "feat(auth): implement login mutation..."
```

### Step 5: Push
```bash
git push -u origin feature/auth-login
```

### Step 6: Create PR on GitHub
- Navigate to: https://github.com/khankhizar-dev/backend/pull/new/feature/auth-login
- Set base to: `develop`
- Add description and testing steps

### Step 7: Merge After Approval
```bash
# Option A: Merge via GitHub UI (Recommended)
# Click "Merge Pull Request"

# Option B: Via CLI
git checkout develop
git pull origin develop
git merge feature/auth-login
git push origin develop
git branch -d feature/auth-login
git push origin --delete feature/auth-login
```

---

## ✅ Success Criteria

- [x] Branch created from develop
- [ ] AuthMutation.kt created
- [ ] auth.graphqls updated
- [ ] Build passes without errors
- [ ] No compilation warnings
- [ ] Login mutation tested and works
- [ ] Code reviewed
- [ ] PR merged to develop
- [ ] Feature branch deleted

---

## 📊 Metrics

| Item | Value |
|------|-------|
| Branch | `feature/auth-login` |
| Estimated Time | 30-45 min |
| Files to Create | 1 (AuthMutation.kt) |
| Files to Update | 1 (auth.graphqls) |
| Complexity | Low |
| Dependencies | ✅ All met |

---

## 🎯 Next Feature After This
- ⬜ `feature/auth-refresh-token` (Refresh token mutation)
- ⬜ `feature/auth-logout` (Logout with blacklist)
- ⬜ `feature/auth-roles` (Role-based authorization)

**Ready to start development! 💪**

