# 📹 Camera Controls Guide

## ✨ New Features Added

### 1. **Camera On/Off Toggle**
- **Button:** 📹 (camera on) / 📷 (camera off)
- **Function:** Completely turns the camera on or off
- **Behavior:** 
  - When OFF: Camera stream stops, saves bandwidth
  - When ON: Camera starts with current quality settings
- **Use Case:** Save bandwidth, privacy control

### 2. **Camera Switch (Front/Rear)**
- **Button:** 🔄 (rotate icon)
- **Function:** Switches between front and rear camera
- **Requirements:** 
  - Camera must be ON
  - Device must have multiple cameras
- **Auto-detection:** Button only appears if device has 2+ cameras
- **Use Case:** Show your surroundings or switch to selfie mode

### 3. **Camera Status in Settings**
- **Location:** Settings panel (⚙️ button)
- **Shows:**
  - Camera Status (On/Off)
  - Current Camera Mode (Front/Rear)
  - Available Cameras count

---

## 🎮 How to Use

### During a Video Call:

1. **Tap the screen** → Controls appear for 3 seconds
2. **Camera Controls:**
   - 🎤 **Mic** - Mute/unmute audio
   - 📹 **Camera** - Turn camera on/off completely
   - 🔄 **Switch** - Change front/rear (only shows if 2+ cameras)
   - ⚙️ **Settings** - Open quality settings

3. **Hang Up Button** (always visible):
   - Red phone icon at bottom center
   - Tap to end call

---

## 🔧 Technical Details

### Camera Initialization
- App starts with **camera OFF** by default
- Only audio is enabled initially
- Camera turns on when you tap the camera button

### Camera Modes
- **Front Camera (user):** Default for video calls
- **Rear Camera (environment):** For showing surroundings

### Quality Settings
When camera is ON, quality applies:
- **Low:** 360p @ 15fps
- **Medium:** 480p @ 24fps (default)
- **High:** 720p @ 30fps
- **HD:** 1080p @ 30fps

### Audio Quality
Crystal clear audio with 3 presets:
- **Voice:** 16kHz, optimized for speech
- **Music:** 44.1kHz, stereo
- **Studio:** 48kHz, stereo, noise suppression (default)

---

## 📱 Mobile Optimization

### Auto-Hide Controls
- Controls show when you tap the screen
- Auto-hide after 3 seconds
- Hang up button **always visible**

### Responsive Design
- ✅ Portrait mode optimized
- ✅ Landscape mode optimized
- ✅ Works on phones, tablets, desktop
- ✅ Touch-friendly buttons (56px+)

### Button Sizes
- **Mobile:** 56px × 56px
- **Small Mobile:** 52px × 52px
- **Landscape:** 48px × 48px
- **Hang Up:** Always 64-70px (easy to hit)

---

## 🚀 Testing Instructions

### Test Camera Switch:
1. Join a room and start a call
2. Tap screen to show controls
3. Tap 📹 to turn camera ON
4. If you have 2+ cameras, tap 🔄 to switch

### Test on Mobile:
1. Open on phone: https://p2p-blue-one.vercel.app
2. Allow camera/microphone permissions
3. Join room and start call
4. Test all buttons (mic, camera, switch, settings)

### Test Quality Changes:
1. During call, tap ⚙️
2. Change video/audio quality
3. Settings apply immediately

---

## 🎯 User Experience Improvements

### Before:
- ❌ Camera always on (privacy concern)
- ❌ No way to switch cameras
- ❌ Controls always visible (distracting)

### After:
- ✅ Camera starts OFF (privacy first)
- ✅ Easy camera switching (front/rear)
- ✅ Auto-hide controls (clean UI)
- ✅ Always-visible hang up button (safety)

---

## 📋 Keyboard Shortcuts (Future)

Planned shortcuts:
- `M` - Toggle microphone
- `V` - Toggle camera
- `F` - Switch camera (flip)
- `Q` - Toggle settings
- `Esc` - End call

---

## 🐛 Troubleshooting

### Camera Switch Not Working?
- Make sure camera is ON first
- Check if device has multiple cameras
- Try in Settings → See "Available: X camera(s)"

### Camera Won't Turn On?
1. Check browser permissions
2. Close other apps using camera
3. Reload page and try again

### No Switch Button?
- Button only shows if 2+ cameras detected
- Some devices only have 1 camera (normal)

---

## 🔐 Privacy Notes

- Camera starts **OFF** by default
- You control when camera turns on
- Other person sees black screen when camera is off
- No video data sent when camera is off

---

## 📊 Performance

### Camera OFF:
- Saves ~50-80% bandwidth
- Reduces CPU usage
- Extends battery life

### Camera ON (Medium Quality):
- ~1-2 Mbps video bandwidth
- ~60-100 Kbps audio bandwidth
- Total: ~1.5-2.5 Mbps

---

## 🎨 UI/UX Details

### Visual Feedback:
- **Blue background** = Camera OFF
- **Red background** = Muted
- **Clear background** = Active
- **Pulsing** = Hang up button (attention)

### Animations:
- Slide-up: Settings panel
- Fade: Top status bar
- Scale: Button press feedback

---

**All features working! Test at:** http://localhost:5174/

**Deployed version:** https://p2p-blue-one.vercel.app

**Need help?** Check console logs or ask for support!
