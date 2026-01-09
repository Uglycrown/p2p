@echo off
echo ========================================
echo    P2P Video Chat - Android Build
echo           All Issues Fixed!
echo ========================================
echo.

cd /d "%~dp0client"

echo [1/3] Building web application...
call npm run build
if errorlevel 1 (
    echo ERROR: Web build failed!
    pause
    exit /b 1
)
echo.

echo [2/3] Syncing with Android...
call npx cap sync android
if errorlevel 1 (
    echo ERROR: Capacitor sync failed!
    pause
    exit /b 1
)
echo.

echo [3/3] Building Android APK...
echo.
echo NOTE: You need Java JDK installed!
echo.
echo Choose build method:
echo   1. Build with Gradle (requires Java JDK)
echo   2. Open in Android Studio (recommended)
echo.
choice /c 12 /n /m "Select option (1 or 2): "

if errorlevel 2 goto androidstudio
if errorlevel 1 goto gradle

:gradle
echo.
echo Building APK with Gradle...
cd android
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo.
    echo ERROR: Gradle build failed!
    echo.
    echo Possible reasons:
    echo   - Java JDK is not installed
    echo   - JAVA_HOME is not set
    echo.
    echo Solutions:
    echo   1. Install Java JDK 17 from https://adoptium.net/
    echo   2. Set JAVA_HOME environment variable
    echo   3. Or use Android Studio (option 2)
    echo.
    pause
    exit /b 1
)
echo.
echo ========================================
echo    BUILD SUCCESSFUL!
echo ========================================
echo.
echo APK Location:
echo   %~dp0client\android\app\build\outputs\apk\debug\app-debug.apk
echo.
echo Install on phone:
echo   adb install -r app-debug.apk
echo.
pause
exit /b 0

:androidstudio
echo.
echo Opening Android project in Android Studio...
echo.
echo Steps in Android Studio:
echo   1. Wait for Gradle sync to complete
echo   2. Click: Build -^> Build Bundle(s) / APK(s) -^> Build APK(s)
echo   3. Find APK at: android\app\build\outputs\apk\debug\app-debug.apk
echo.
start "" "android"
echo.
echo Android project folder opened!
echo Please follow the steps above in Android Studio.
echo.
pause
exit /b 0
