@echo off
echo ========================================
echo  🚀 Android App Builder - P2P Video Chat
echo ========================================
echo.

cd client

echo 📦 Step 1/7: Installing TypeScript...
call npm install -D typescript
if errorlevel 1 (
    echo ❌ Failed to install TypeScript
    pause
    exit /b 1
)

echo.
echo 📦 Step 2/7: Installing Capacitor...
call npm install @capacitor/core @capacitor/cli @capacitor/android
if errorlevel 1 (
    echo ❌ Failed to install Capacitor
    pause
    exit /b 1
)

echo.
echo ⚙️ Step 3/7: Initializing Capacitor...
call npx cap init "P2P Video Chat" "com.p2pvideo.app" --web-dir=dist
if errorlevel 1 (
    echo ❌ Failed to initialize Capacitor
    pause
    exit /b 1
)

echo.
echo 🔨 Step 4/7: Building web application...
call npm run build
if errorlevel 1 (
    echo ❌ Failed to build web app
    pause
    exit /b 1
)

echo.
echo 📱 Step 5/7: Adding Android platform...
call npx cap add android
if errorlevel 1 (
    echo ❌ Failed to add Android platform
    pause
    exit /b 1
)

echo.
echo 🔄 Step 6/7: Syncing files to Android...
call npx cap sync android
if errorlevel 1 (
    echo ❌ Failed to sync files
    pause
    exit /b 1
)

echo.
echo ✅ Setup Complete!
echo.
echo 📱 Step 7/7: Opening Android Studio...
echo.
echo After Android Studio opens:
echo   1. Wait for Gradle sync to complete
echo   2. Click Build → Build Bundle(s) / APK(s) → Build APK(s)
echo   3. APK will be in: android/app/build/outputs/apk/
echo.
pause

call npx cap open android

echo.
echo 🎉 Done! Your Android app is ready to build!
echo.
pause
