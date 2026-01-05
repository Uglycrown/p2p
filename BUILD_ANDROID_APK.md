# Build Android APK - Quick Guide

## Current Status
✅ Web app rebuilt with fixes  
✅ Android project synced  
⏳ Need to build APK  

## Option 1: Using Gradle (Command Line) - FASTEST

### Windows:
```bash
cd C:\Users\Goura\OneDrive\Desktop\p2p\client\android
.\gradlew assembleDebug
```

### If gradlew not found:
```bash
cd C:\Users\Goura\OneDrive\Desktop\p2p\client\android
gradlew assembleDebug
```

**APK Location:**
```
client\android\app\build\outputs\apk\debug\app-debug.apk
```

**Time:** ~2-5 minutes

## Option 2: Using Android Studio - MORE RELIABLE

### Steps:
1. **Open Android Studio**

2. **Open Project:**
   - File → Open
   - Navigate to: `C:\Users\Goura\OneDrive\Desktop\p2p\client\android`
   - Click OK

3. **Wait for Gradle Sync:**
   - Bottom right: "Gradle sync in progress..."
   - Wait until it says "Gradle sync finished"
   - Can take 2-5 minutes first time

4. **Build APK:**
   - Menu: Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Or press: `Ctrl + Shift + B`
   - Wait for "APK(s) generated successfully" notification

5. **Find APK:**
   - Click "locate" in the notification
   - Or navigate to: `client\android\app\build\outputs\apk\debug\`
   - File: `app-debug.apk`

**Time:** ~5-10 minutes

## Option 3: Using Our Build Script

```bash
cd C:\Users\Goura\OneDrive\Desktop\p2p
.\build-android-app.bat
```

**Note:** This will:
- Rebuild everything from scratch
- Open Android Studio
- You still need to click "Build APK" in Android Studio

**Time:** ~10-15 minutes

## Installing APK on Android Device

### Method 1: USB Cable (RECOMMENDED)

1. **Enable USB Debugging on Phone:**
   - Settings → About Phone
   - Tap "Build Number" 7 times (enables Developer Options)
   - Settings → Developer Options
   - Enable "USB Debugging"

2. **Connect Phone to Computer:**
   - Use USB cable
   - Select "File Transfer" mode on phone
   - Allow USB debugging when prompted

3. **Install via ADB:**
   ```bash
   cd C:\Users\Goura\OneDrive\Desktop\p2p\client\android\app\build\outputs\apk\debug
   adb install app-debug.apk
   ```

4. **Or Copy Manually:**
   - Copy `app-debug.apk` to phone's Download folder
   - Open file manager on phone
   - Tap `app-debug.apk`
   - Tap "Install"
   - Allow "Install from Unknown Sources" if prompted

### Method 2: Upload and Download

1. **Upload APK:**
   - Upload `app-debug.apk` to Google Drive, Dropbox, etc.

2. **Download on Phone:**
   - Open link on phone
   - Download APK
   - Tap downloaded file
   - Install

### Method 3: Direct from Android Studio (If Connected)

1. **Connect phone via USB**
2. **In Android Studio:**
   - Run → Run 'app'
   - Or click green play button (▶)
3. **Select your device from list**
4. **App installs automatically**

## Verifying Installation

1. **Look for app icon:** "P2P Video Chat"
2. **Open app**
3. **Grant camera/microphone permissions**
4. **Should see camera preview in lobby**

## Testing the Fix

### Test 1: Android ↔ Browser
1. **Phone:** Open P2P Video Chat app
2. **Computer:** Open browser to your app URL
3. **Both:** Enter same room name (e.g., "test123")
4. **Expected:** Both should see each other's video immediately
5. **No errors** in browser console

### Test 2: Camera Toggle
1. **During active call**
2. **Phone:** Tap screen → tap camera button
3. **Expected:** Video disappears on both sides
4. **Tap again:** Video reappears
5. **No "Connection failed" error**

### Test 3: Camera Switch
1. **During active call**
2. **Phone:** Tap screen → tap switch camera button (🔄)
3. **Expected:** Camera switches from front to rear
4. **Browser user** sees the switch in real-time
5. **Connection stays stable**

## Troubleshooting Build Issues

### "gradlew: command not found"
**Solution:** Use Android Studio instead (Option 2)

### "SDK not found"
**Solution:** 
1. Open Android Studio
2. Tools → SDK Manager
3. Install Android SDK
4. Try building again

### Gradle sync fails
**Solution:**
1. In Android Studio: File → Invalidate Caches → Restart
2. Tools → SDK Manager → Update SDK components
3. File → Sync Project with Gradle Files

### "JAVA_HOME not set"
**Solution:**
1. Download and install JDK 17+
2. Set environment variable:
   ```
   JAVA_HOME=C:\Program Files\Java\jdk-17
   ```
3. Restart Android Studio

### Build succeeds but APK not found
**Solution:**
Check alternative locations:
```
client\android\app\build\outputs\apk\debug\app-debug.apk
client\android\build\outputs\apk\debug\app-debug.apk
```

## Quick Build Commands Cheat Sheet

```bash
# Navigate to project
cd C:\Users\Goura\OneDrive\Desktop\p2p

# Rebuild web app (if needed)
cd client
npm run build

# Sync to Android
npx cap sync android

# Build APK (Option A - if gradlew works)
cd android
.\gradlew assembleDebug

# Build APK (Option B - use Android Studio)
# Open Android Studio → Open android folder → Build APK

# Install APK (if phone connected)
cd app\build\outputs\apk\debug
adb install app-debug.apk
```

## Expected File Sizes
- `app-debug.apk`: ~10-30 MB
- Build time: 2-10 minutes depending on machine

## Success Indicators
✅ APK file exists in outputs folder  
✅ File size is reasonable (10-30 MB)  
✅ Installation succeeds on phone  
✅ App opens and shows camera preview  
✅ Can join room and connect with browser  
✅ No "Connection failed" errors  

---

**You're all set! The fixes are applied and ready to test.**

After building the APK and installing on your device, test the connection between Android and browser. The camera toggle and switch features should now work smoothly without any connection errors.
