# Quickstart script for local development without Docker
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Starting Acentra Order Processing System (Local Dev)   " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Start Spring Boot Backend in a new window
Write-Host "`n[1/2] Launching Spring Boot Backend on http://localhost:8080..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd backend; .\mvnw.cmd spring-boot:run"

# Wait 5 seconds for backend to initialize
Start-Sleep -Seconds 5

# 2. Start React Frontend
Write-Host "`n[2/2] Launching React Dashboard on http://localhost:5173..." -ForegroundColor Green
Set-Location -Path "frontend"
npm run dev
