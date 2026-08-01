# 🌿 Git Branching Strategy for TripPoint Backend

## Branching Model: Git Flow

We use **Git Flow** - industry standard for feature development.

```
main (production-ready)
  ↑
  └─ develop (integration branch)
       ↑
       ├─ feature/auth-login
       ├─ feature/auth-refresh-token
       ├─ feature/auth-logout
       ├─ feature/trips-create
       └─ feature/expenses-add
```

---

## Branch Naming Convention

### Feature Branches
```
feature/{module}-{feature-name}

Examples:
  feature/auth-login              (Authentication module)
  feature/auth-refresh-token      (Refresh token implementation)
  feature/auth-role-based-auth    (Role-based access control)
  feature/trips-create            (Trips module)
  feature/trips-update            (Update trips)
  feature/expenses-add            (Add expenses)
  feature/expenses-split          (Split expenses)
  feature/itinerary-day-planner   (Day planner)
```

### Bug Fix Branches
```
bugfix/{module}-{bug-name}

Examples:
  bugfix/auth-token-expiry
  bugfix/trips-null-pointer
```

### Release Branches
```
release/v{version}

Examples:
  release/v1.0.0
  release/v1.1.0
```

### Hotfix Branches
```
hotfix/{issue-name}

Examples:
  hotfix/security-token-leak
```

---

## Workflow: Feature Development

### 1️⃣ Start New Feature

```bash
# Ensure you're on develop and up-to-date
git checkout develop
git pull origin develop

# Create feature branch from develop
git checkout -b feature/auth-login

# Now on: feature/auth-login
```

### 2️⃣ Develop & Commit

```bash
# Make changes
# Edit files, add code, etc.

# Stage changes
git add .

# Commit with descriptive message
git commit -m "feat(auth): implement login mutation

- Add login method to AuthService
- Create AuthMutation resolver
- Add login to GraphQL schema
- Login returns user + JWT + refresh token

Related: #123"
```

**Commit Message Format:**
```
<type>(<module>): <short-description>

<detailed-explanation>

Related: #<issue-number>
```

**Types:** feat, fix, refactor, test, docs, chore, perf

### 3️⃣ Push to Remote

```bash
# Push feature branch
git push origin feature/auth-login

# Set upstream if first time
git push -u origin feature/auth-login
```

### 4️⃣ Create Pull Request

**On GitHub:**
1. Go to Repository → Pull Requests
2. Click "New Pull Request"
3. Base: `develop` ← Compare: `feature/auth-login`
4. Title: `feat(auth): implement login mutation`
5. Description: Include what was changed, testing notes, any blockers
6. Request reviewers
7. Link related issues

**PR Template Example:**
```markdown
## Description
Implements the login mutation to authenticate users.

## Changes
- [x] AuthService.login() method
- [x] AuthMutation resolver
- [x] GraphQL schema updates
- [x] Error handling for invalid credentials

## Related Issues
Closes #123

## Testing
```bash
mutation {
  login(email: "test@example.com", password: "Password@123") {
    token
    user { id }
  }
}
```

## Checklist
- [x] Code follows style guide
- [x] Tests pass: `./gradlew test`
- [x] Build passes: `./gradlew build`
- [x] No breaking changes
```

### 5️⃣ Code Review & Merge

**After approval:**

```bash
# Option A: Merge via GitHub UI (Recommended)
# - Click "Merge Pull Request"
# - Choose "Squash and merge" or "Create merge commit"

# Option B: Merge via CLI
git checkout develop
git pull origin develop
git merge feature/auth-login
git push origin develop

# Delete feature branch
git branch -d feature/auth-login
git push origin --delete feature/auth-login
```

---

## Branch Lifecycle

```
1. CREATE
   git checkout -b feature/auth-login
   
2. DEVELOP
   ├─ Make commits
   ├─ Push to origin
   └─ Regular pulls from develop to stay synced
   
3. REVIEW
   ├─ Create Pull Request
   ├─ Request reviewers
   └─ Address feedback
   
4. MERGE
   ├─ Merge to develop
   └─ Delete feature branch
   
5. RELEASE
   ├─ Merge develop → main (periodic)
   └─ Tag version (v1.0.0)
```

---

## Key Rules

✅ **DO:**
- Create new branch for every feature
- Keep branches focused (one feature per branch)
- Push regularly (daily)
- Write clear commit messages
- Update from develop frequently (`git pull origin develop`)
- Run tests before push

❌ **DON'T:**
- Commit directly to `main` or `develop`
- Mix multiple features in one branch
- Forget to push before EOD
- Merge without PR review
- Leave stale branches (>2 weeks old)

---

## Common Commands Reference

```bash
# List all branches
git branch -a

# Create and switch to feature branch
git checkout -b feature/auth-login

# Switch between branches
git checkout develop
git checkout feature/auth-login

# Sync with develop (pull latest changes)
git pull origin develop

# View commits on current branch
git log --oneline

# View changes before committing
git diff

# Undo last commit (keep changes)
git reset --soft HEAD~1

# Discard all changes (be careful!)
git reset --hard HEAD

# Delete local branch
git branch -d feature/auth-login

# Delete remote branch
git push origin --delete feature/auth-login

# Rename current branch
git branch -m feature/auth-login feature/auth-login-v2
```

---

## Current Status

**Branches:**
- `main` - Production-ready code
- `develop` - Integration/staging branch

**Next Steps:**
1. ✅ Ensure `develop` has all Sprint 1 code
2. ✅ Create branch: `feature/auth-login`
3. ✅ Implement Login mutation
4. ✅ Create PR to `develop`
5. ✅ Merge & delete feature branch
6. ✅ Repeat for each feature

---

## Release Process (When Ready for Production)

```bash
# 1. Create release branch from develop
git checkout -b release/v1.0.0 develop

# 2. Final version bumps, documentation
# Update version in build.gradle.kts
# Update CHANGELOG

git commit -am "chore: bump version to 1.0.0"

# 3. Merge release into main
git checkout main
git merge --no-ff release/v1.0.0
git tag -a v1.0.0 -m "Release version 1.0.0"

# 4. Merge back to develop
git checkout develop
git merge --no-ff release/v1.0.0

# 5. Delete release branch
git branch -d release/v1.0.0
git push origin --delete release/v1.0.0

# 6. Push all branches and tags
git push origin main develop
git push origin --tags
```

---

## Team Communication

For each feature, update the team:

**Sprint Planning:**
```
🎯 Sprint 2: Auth Features
├─ feature/auth-login (Dev: Khizar, 30 min)
├─ feature/auth-refresh (Dev: ?, 45 min)  
├─ feature/auth-logout (Dev: ?, 2 hrs)
└─ feature/auth-roles (Dev: ?, 4 hrs)

Status: On track | ⚠️ Blocked | ✅ Done
```

**Daily Standup:**
```
Yesterday: Completed feature/auth-login, created PR
Today: Reviewing PR feedback, starting feature/auth-refresh
Blocker: Need PostgreSQL access for local testing
```

---

## Summary

✅ Each feature gets its own branch: `feature/{module}-{name}`  
✅ All branches created from `develop`  
✅ All merges via Pull Request with review  
✅ Branches deleted after merge  
✅ `main` only for production releases  

**Ready to start feature development! 🚀**

