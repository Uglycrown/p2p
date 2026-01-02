# 🔧 Troubleshooting Guide

## Common Issues & Solutions

### 🎥 Camera/Microphone Issues

#### Problem: "Please allow camera and microphone access!" alert
**Solution:**
1. Click the camera icon (🎥) in your browser's address bar
2. Select "Allow" for both camera and microphone
3. Refresh the page
4. If on Chrome: Go to `chrome://settings/content/camera` and check permissions

#### Problem: Black screen / No video showing
**Causes & Solutions:**
- **Another app is using camera**: Close Zoom, Teams, Skype, Discord
- **Camera disabled in OS**: 
  - Windows: Settings → Privacy → Camera → Allow apps
  - Mac: System Preferences → Security & Privacy → Camera
- **Wrong camera selected**: Some laptops have multiple cameras
- **Browser doesn't have permission**: Check browser settings

#### Problem: No audio
**Solutions:**
- Click the Mute/Unmute button to ensure it's not muted
- Check system volume (not muted)
- Try headphones to rule out speaker issues
- Verify microphone permissions in browser

---

### 🔌 Connection Issues

#### Problem: "Cannot connect to server"
**Checklist:**
1. ✅ Is the server running? (Check terminal shows "Server is running on port 5000")
2. ✅ Is the URL correct? (Should be `http://localhost:5000` in App.vue)
3. ✅ Port 5000 not already in use? Try: `netstat -ano | findstr :5000`
4. ✅ Firewall blocking? Temporarily disable to test

**Solution for Port Already in Use:**
```bash
# Windows: Find process using port 5000
netstat -ano | findstr :5000

# Kill the process (replace PID with actual number)
taskkill /PID <PID> /F
```

#### Problem: "Room Full" message
**Cause:** Room already has 2 people
**Solution:** 
- Choose a different room name
- Ask current users to leave first
- This is by design for security (only 2 people allowed)

#### Problem: Friend can't join the same room
**Checklist:**
1. ✅ Both entered EXACT same room name? (case-sensitive!)
2. ✅ Both connected to the same server?
3. ✅ Both see their Socket ID displayed?
4. ✅ Server terminal shows two connections?

---

### 📞 Call Issues

#### Problem: "Start Video Call" button doesn't appear
**Possible Causes:**
- Friend hasn't joined the room yet
- Wait for "Friend is here! Ready to call." message
- Both users must join BEFORE calling

#### Problem: Call connects but can't see/hear friend
**Solutions:**
1. **Check video/audio tracks:**
   - Open browser console (F12)
   - Look for WebRTC errors
   - Verify stream is being sent

2. **Network issues:**
   - Both users on same network? Try different networks
   - Corporate firewall? May block WebRTC
   - VPN active? Try disabling temporarily

3. **Browser compatibility:**
   - Use Chrome/Edge (most reliable)
   - Update browser to latest version
   - Clear browser cache

#### Problem: Video freezes or lags
**Solutions:**
- Close bandwidth-heavy applications (downloads, streaming)
- Reduce video quality expectations on slow connections
- Use audio-only mode (click "Stop Video")
- Check internet speed: Minimum 1 Mbps upload/download recommended
- Move closer to WiFi router
- Use wired ethernet connection instead of WiFi

#### Problem: Echo during call
**Solutions:**
- **Use headphones!** (This fixes 99% of echo issues)
- Don't use speakerphone
- Reduce speaker volume
- Ensure only one device is in the call (not both your phone and laptop)

---

### 🖥️ Server Issues

#### Problem: Server won't start
**Error: "Cannot find module 'express'"**
**Solution:**
```bash
cd server
npm install
```

**Error: "Port 5000 already in use"**
**Solution 1 - Kill the process:**
```bash
# Windows
netstat -ano | findstr :5000
taskkill /PID <PID> /F
```

**Solution 2 - Change port:**
Edit `server/index.js`:
```javascript
const PORT = process.env.PORT || 5001; // Change to 5001
```
Then update client `App.vue`:
```javascript
socket.value = io('http://localhost:5001'); // Match new port
```

#### Problem: Server crashes with CORS error
**Solution:** Already handled in code, but if issues persist:
```javascript
// In server/index.js, update CORS:
cors: {
    origin: "http://localhost:5173", // Specific origin
    methods: ["GET", "POST"]
}
```

---

### 💻 Client Issues

#### Problem: Client won't start
**Error: "npm: command not found"**
**Solution:** Install Node.js from https://nodejs.org/

**Error: "Cannot find module 'socket.io-client'"**
**Solution:**
```bash
cd client
npm install
```

#### Problem: Blank white screen
**Solutions:**
1. Check browser console (F12) for errors
2. Verify Vite dev server is running
3. Clear browser cache
4. Try different browser
5. Check if `App.vue` exists and is valid

#### Problem: "Failed to fetch" in console
**Cause:** Server not running or wrong URL
**Solution:**
1. Start server first: `cd server && npm start`
2. Verify server URL in `App.vue` matches where server is running
3. Check for typos in URL

---

### 📱 Mobile/Cross-Device Issues

#### Problem: Can't access from phone
**Checklist:**
1. ✅ Phone and computer on SAME WiFi network
2. ✅ Found computer's IP address correctly
3. ✅ Updated Socket.io URL in App.vue to computer's IP
4. ✅ Firewall allows connections on port 5000 and 5173

**Find Computer IP:**
```bash
# Windows
ipconfig

# Look for "IPv4 Address" under your active network adapter
# Example: 192.168.1.100
```

**Update App.vue (line 92):**
```javascript
socket.value = io('http://192.168.1.100:5000'); // Use your actual IP
```

**Access from phone:**
```
http://192.168.1.100:5173
```

#### Problem: HTTPS required error on mobile
**Cause:** Some mobile browsers require HTTPS for camera/mic
**Solution:** 
- For testing: Use Chrome on Android (works with HTTP localhost)
- For production: Deploy with SSL certificates

---

### 🌐 Browser-Specific Issues

#### Chrome/Edge
- Most compatible, use these for testing
- Allow permissions when prompted
- Check `chrome://settings/content` for media permissions

#### Firefox
- May ask for permissions differently
- Go to `about:preferences#privacy` → Permissions → Camera/Microphone
- Sometimes needs explicit permission grant

#### Safari (Desktop)
- Works well on macOS
- Check System Preferences → Security & Privacy
- May need to enable "Auto-Play" for videos

#### Safari (iOS)
- Requires `playsinline` attribute (already added in code)
- May require user gesture to start video
- HTTPS strongly preferred

---

### 🔍 Debugging Tips

#### Check Server Logs
Terminal running server should show:
```
Server is running on port 5000
```
When users connect, you won't see much (Socket.io is quiet by default)

#### Check Browser Console (F12)
Look for:
- Red errors
- WebRTC warnings
- Network failures
- Permission denials

#### Test WebRTC Compatibility
Visit: https://test.webrtc.org/
- Tests camera/microphone
- Tests peer connection
- Shows network information

#### Verify Socket.io Connection
Add debug code to `App.vue`:
```javascript
socket.value.on('connect', () => {
  console.log('✅ Connected to server!');
});

socket.value.on('disconnect', () => {
  console.log('❌ Disconnected from server!');
});
```

---

### 🚫 What NOT to Do

❌ Don't use HTTP in production (use HTTPS)
❌ Don't share room names publicly (they're like passwords)
❌ Don't run server and client on different ports without updating URLs
❌ Don't block browser permissions (camera/mic required)
❌ Don't expect it to work on Internet Explorer (not supported)
❌ Don't use 3+ users in same room (designed for 2 only)

---

### 📊 Performance Optimization

#### Slow Video Loading?
- Reduce video element size in CSS
- Check internet speed (need 1+ Mbps)
- Close other tabs/applications
- Restart router

#### High CPU Usage?
- Normal for video processing
- Close other applications
- Lower screen resolution
- Use audio-only mode

#### Memory Leaks?
- Refresh page after long calls
- Clear browser cache periodically
- Update browser to latest version

---

### 🆘 Still Stuck?

1. **Read error messages carefully** - They usually tell you what's wrong
2. **Check browser console** (F12) - Look for red errors
3. **Verify both server and client are running**
4. **Test with two tabs first** before trying two devices
5. **Try different browser** (Chrome recommended)
6. **Restart everything**: Server, client, browser, router

### Common Error Messages Decoded

| Error | Meaning | Solution |
|-------|---------|----------|
| `getUserMedia failed` | Camera/mic denied | Grant permissions |
| `Connection refused` | Server not running | Start server |
| `CORS error` | Security policy | Already fixed in code |
| `Port in use` | Port already taken | Kill process or change port |
| `Module not found` | Missing package | Run `npm install` |
| `HTTPS required` | Browser security | Deploy with SSL or use Chrome |

---

### ✅ Verification Checklist

Before asking for help, verify:

- [ ] Node.js installed (`node --version`)
- [ ] NPM installed (`npm --version`)
- [ ] Server dependencies installed (`cd server && npm install`)
- [ ] Client dependencies installed (`cd client && npm install`)
- [ ] Server running on port 5000
- [ ] Client running on port 5173
- [ ] Browser has camera/mic permissions
- [ ] No firewall blocking connections
- [ ] Using Chrome/Edge browser
- [ ] Same room name entered in both tabs

---

**Still having issues? Drop a comment with:**
1. What you're trying to do
2. What error you're seeing (screenshot helps!)
3. What you've already tried
4. Your operating system and browser

Good luck! 🚀
