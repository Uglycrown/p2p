# 🚀 Quick Start Guide

## Step 1: Start the Server

Open a terminal and run:

```bash
cd server
npm start
```

You should see:
```
Server is running on port 5000
```

**Keep this terminal open!**

## Step 2: Start the Client

Open a **NEW** terminal and run:

```bash
cd client
npm run dev
```

You should see something like:
```
VITE ready in 500ms
Local: http://localhost:5173/
```

## Step 3: Test the App

### Method 1: Two Browser Tabs (Easiest)

1. Open `http://localhost:5173` in your browser
2. Open another tab with the same URL
3. In **Tab 1**: Enter room name "test123" and click "Join Room"
4. In **Tab 2**: Enter room name "test123" and click "Join Room"
5. Wait for "Friend is here!" message in both tabs
6. In **Tab 1**: Click "📞 Start Video Call"
7. In **Tab 2**: Click "Answer" when you see "Incoming Call..."
8. 🎉 You're connected!

### Method 2: Two Devices (Phone + Computer)

1. Find your computer's IP address:
   - Windows: Open CMD and type `ipconfig` (look for IPv4 Address)
   - Mac/Linux: Open Terminal and type `ifconfig` or `ip addr`
   
2. Edit `client/src/App.vue` line 92:
   ```javascript
   // Change from:
   socket.value = io('http://localhost:5000');
   
   // To (replace with YOUR IP):
   socket.value = io('http://192.168.1.100:5000');
   ```

3. Access from phone: `http://192.168.1.100:5173`
4. Follow the same room joining steps as Method 1

## 🎮 Controls During Call

- **🎙️ Mute Button**: Silence your microphone
- **📹 Video Button**: Turn camera on/off (audio-only mode)
- **☎️ End Call**: Disconnect and return to lobby

## ⚠️ Common Issues

**"Please allow camera and microphone access!"**
- Click the camera icon in your browser's address bar
- Select "Allow" for camera and microphone permissions

**Can't connect to friend?**
- Make sure both users entered the EXACT same room name (case-sensitive!)
- Check that the server terminal is still running
- Try refreshing both browser tabs

**Video is black?**
- Check if another app is using your camera (Zoom, Teams, etc.)
- Try restarting your browser
- Ensure you clicked "Allow" for camera permissions

## 🎯 Pro Tips

1. **Use a unique room name**: Like a password - make it complex!
2. **Test alone first**: Open two tabs to ensure everything works
3. **Good lighting**: Make sure you have light on your face for better video quality
4. **Close other apps**: Free up bandwidth for smoother calls

---

**Enjoy your private, secure video calls! 🎉**
