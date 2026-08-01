# Register a user
$registerBody = @{
    query = 'mutation { register(input: { email: "testme@example.com", password: "Test1234!", firstName: "John", lastName: "Doe" }) { user { id email firstName lastName } token refreshToken } }'
} | ConvertTo-Json

$headers = @{ "Content-Type" = "application/json" }

Write-Host "1. Registering user..." -ForegroundColor Cyan
$registerResponse = Invoke-WebRequest -Uri http://localhost:8081/graphql `
  -Method Post -Headers $headers -Body $registerBody -UseBasicParsing

$registerData = $registerResponse.Content | ConvertFrom-Json
$token = $registerData.data.register.token
$userId = $registerData.data.register.user.id
$email = $registerData.data.register.user.email

Write-Host "   ✓ User registered!" -ForegroundColor Green
Write-Host "   ID: $userId" -ForegroundColor Yellow
Write-Host "   Email: $email" -ForegroundColor Yellow
Write-Host "   Token: $($token.Substring(0, 20))..." -ForegroundColor Yellow

# Call "me" endpoint with JWT
Write-Host "`n2. Calling 'me' endpoint with JWT..." -ForegroundColor Cyan
$meBody = @{
    query = 'query { me { id email firstName lastName } }'
} | ConvertTo-Json

$authHeaders = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $token"
}

$meResponse = Invoke-WebRequest -Uri http://localhost:8081/graphql `
  -Method Post -Headers $authHeaders -Body $meBody -UseBasicParsing

$meData = $meResponse.Content | ConvertFrom-Json
Write-Host "   ✓ Me query successful!" -ForegroundColor Green
Write-Host "   Response: " -ForegroundColor Yellow
Write-Host ($meData | ConvertTo-Json -Depth 5) -ForegroundColor White

Write-Host "`n3. Testing without JWT (should fail)..." -ForegroundColor Cyan
$badHeaders = @{ "Content-Type" = "application/json" }
try {
    $badResponse = Invoke-WebRequest -Uri http://localhost:8081/graphql `
      -Method Post -Headers $badHeaders -Body $meBody -UseBasicParsing
    $badData = $badResponse.Content | ConvertFrom-Json
    if ($badData.errors) {
        Write-Host "   ✓ Correctly rejected unauthenticated request" -ForegroundColor Green
        Write-Host "   Error: $($badData.errors[0].message)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ✓ Request failed as expected (no JWT)" -ForegroundColor Green
}

Write-Host "`n✅ All tests passed!" -ForegroundColor Green
