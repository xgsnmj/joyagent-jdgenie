@echo off
setlocal enabledelayedexpansion

REM Check if Node.js is installed
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo Node.js is not installed. Please install Node.js 18 or higher.
    exit /b 1
)

REM Get Node.js version
for /f "tokens=1 delims=v" %%i in ('node -v') do set NODE_VERSION=%%i
for /f "tokens=1,2 delims=." %%a in ("%NODE_VERSION%") do (
    set NODE_MAJOR=%%a
)

REM Check if Node.js version is 18 or higher
if %NODE_MAJOR% lss 18 (
    echo Node.js version 18 is required. Current version: v%NODE_VERSION%
    exit /b 1
)

REM Check if pnpm is installed
where pnpm >nul 2>&1
if %errorlevel% neq 0 (
    echo pnpm is not installed. installing pnpm@7.33.1 now
    echo RUN 'npm install -g pnpm@7.33.1' Installing pnpm...
    call npm install pnpm@7.33.1 -g
)

REM Get pnpm version
for /f "tokens=1,2 delims=." %%a in ('pnpm -v') do (
    set PNPM_MAJOR=%%a
)

REM Check if pnpm version is 7 or higher
if %PNPM_MAJOR% lss 7 (
    echo pnpm version 7 is required. Current version: !PNPM_VERSION!
    exit /b 1
)

REM Install dependencies
call pnpm i --registry=https://registry.npmmirror.com

REM Run development server
call pnpm run dev

echo ✅front end code start success!
