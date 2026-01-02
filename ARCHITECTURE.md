# 📊 Project Structure Overview

## Visual Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    P2P Video Chat System                     │
└─────────────────────────────────────────────────────────────┘

┌──────────────────┐              ┌──────────────────┐
│   Browser Tab 1  │              │   Browser Tab 2  │
│   (User A)       │              │   (User B)       │
│                  │              │                  │
│  Vue 3 Client    │              │  Vue 3 Client    │
│  + WebRTC        │              │  + WebRTC        │
└────────┬─────────┘              └─────────┬────────┘
         │                                  │
         │ Socket.io (Signaling Only)      │
         │                                  │
         └──────────┬───────────────────────┘
                    │
         ┌──────────▼──────────┐
         │   Node.js Server    │
         │   Port 5000         │
         │                     │
         │  - Room Management  │
         │  - Signal Relay     │
         │  - User Matching    │
         └─────────────────────┘

After Connection Established:
┌──────────────────┐                    ┌──────────────────┐
│   User A         │ ◄═══════════════► │   User B         │
│                  │   Direct P2P       │                  │
│  Video + Audio   │   (No Server!)     │  Video + Audio   │
└──────────────────┘                    └──────────────────┘
```

## File Structure

```
p2p/
│
├── 📄 README.md                 # Complete documentation
├── 📄 QUICKSTART.md             # Quick start guide
├── 📄 SETUP_COMPLETE.md         # This file
├── 🪟 start-server.bat          # Windows server launcher
├── 🪟 start-client.bat          # Windows client launcher
│
├── 📁 server/                   # Backend (Signaling Server)
│   ├── 📄 index.js              # Main server file (~50 lines)
│   ├── 📄 package.json          # Dependencies config
│   └── 📁 node_modules/         # Installed packages
│
└── 📁 client/                   # Frontend (Vue 3 App)
    ├── 📄 index.html            # HTML entry point
    ├── 📄 package.json          # Dependencies config
    ├── 📄 vite.config.js        # Vite configuration
    │
    ├── 📁 src/
    │   ├── 📄 App.vue           # Main app component (~200 lines)
    │   ├── 📄 main.js           # Vue initialization
    │   └── 📄 style.css         # Global styles
    │
    └── 📁 node_modules/         # Installed packages
```

## Technology Flow

### 1. Initial Setup Phase
```
User Opens Browser
       ↓
   Loads Vue App
       ↓
Requests Camera/Mic Permission
       ↓
Connects to Socket.io Server
       ↓
Receives Unique Socket ID
```

### 2. Room Join Phase
```
User Enters Room Name
       ↓
Emits "joinRoom" to Server
       ↓
Server Checks Room Size
       ↓
If < 2 people: Join Success
If = 2 people: Room Full (Rejected)
       ↓
Waits for Second User
       ↓
Both Users Get "userJoined" Event
```

### 3. Call Initiation Phase
```
User A Clicks "Start Call"
       ↓
Creates SimplePeer (Initiator)
       ↓
Generates WebRTC Signal Data
       ↓
Sends Signal to User B via Server
       ↓
User B Receives "callUser" Event
       ↓
User B Clicks "Answer"
       ↓
Creates SimplePeer (Non-Initiator)
       ↓
Sends Answer Signal to User A
       ↓
WebRTC Handshake Complete!
```

### 4. Active Call Phase
```
┌─────────────────────────────────┐
│  Direct P2P Connection Active   │
├─────────────────────────────────┤
│  • Video Stream (User A → B)    │
│  • Video Stream (User B → A)    │
│  • Audio Stream (User A → B)    │
│  • Audio Stream (User B → A)    │
│  • Toggle Controls Available    │
└─────────────────────────────────┘

Server is NOT involved in media transfer!
```

## Data Flow Diagram

```
┌─────────┐  Camera/Mic   ┌──────────────┐
│ Hardware├──────────────►│ MediaStream  │
└─────────┘               └──────┬───────┘
                                 │
                                 ▼
                          ┌──────────────┐
                          │  WebRTC Peer │
                          └──────┬───────┘
                                 │
                    ┌────────────┴────────────┐
                    │                         │
                    ▼                         ▼
          ┌─────────────────┐       ┌─────────────────┐
          │ Local Video Tag │       │ Send to Remote  │
          │  (You see you)  │       │  (Friend sees)  │
          └─────────────────┘       └─────────────────┘
```

## Dependencies

### Server Dependencies
```json
{
  "express": "^5.2.1",        // Web server framework
  "socket.io": "^4.8.3",      // WebSocket communication
  "cors": "^2.8.5"            // Cross-origin requests
}
```

### Client Dependencies
```json
{
  "vue": "^3.5.13",           // Frontend framework
  "socket.io-client": "^4.x", // WebSocket client
  "simple-peer": "^9.x"       // WebRTC wrapper
}
```

## Port Usage

```
Port 5000 → Backend Signaling Server (WebSocket)
Port 5173 → Frontend Development Server (HTTP)
Random Ports → WebRTC P2P Connections (UDP/TCP)
```

## Security Layers

```
┌────────────────────────────────────┐
│ Layer 1: Room Name (Secret Key)   │
├────────────────────────────────────┤
│ Layer 2: Max 2 Users Per Room     │
├────────────────────────────────────┤
│ Layer 3: WebRTC Encryption (DTLS) │
├────────────────────────────────────┤
│ Layer 4: No Server Data Storage   │
└────────────────────────────────────┘
```

## Performance Characteristics

```
Connection Time:  1-3 seconds
Video Quality:    Depends on bandwidth (auto-adaptive)
Audio Quality:    48kHz stereo (if supported)
Latency:          20-100ms (P2P direct)
Max Users:        2 (one-on-one only)
Data Transfer:    ~500KB/s per direction (video call)
```

## Browser Compatibility

```
✅ Chrome/Edge (Recommended)
✅ Firefox
✅ Safari (Desktop & iOS)
✅ Opera
❌ Internet Explorer (Not supported)
```

## WebRTC Connection States

```
1. "new"         → Peer created, not connected
2. "connecting"  → Attempting connection
3. "connected"   → Successfully connected!
4. "disconnected"→ Connection lost (temporary)
5. "failed"      → Connection failed (permanent)
6. "closed"      → Peer destroyed
```

## Event Flow Cheat Sheet

### Server Events (Socket.io)
```javascript
// Incoming (from client)
"joinRoom"     → User wants to join a room
"callUser"     → Initiate call with signal data
"answerCall"   → Accept call with signal data
"endCall"      → Terminate the call

// Outgoing (to client)
"me"           → Send user their socket ID
"userJoined"   → Notify that friend joined
"roomFull"     → Reject join (already 2 users)
"callUser"     → Forward call request
"callAccepted" → Forward call acceptance
"callEnded"    → Notify call termination
```

### WebRTC Events (SimplePeer)
```javascript
// Peer Events
"signal"   → ICE candidate / SDP offer/answer ready
"stream"   → Remote media stream received
"connect"  → P2P connection established
"close"    → Connection closed
"error"    → Something went wrong
```

## Common Use Cases

### 1. Testing Locally (Same Computer)
```
Open Tab 1 → Join "room123"
Open Tab 2 → Join "room123"
Tab 1 → Start Call
Tab 2 → Answer
✅ Works!
```

### 2. Testing on LAN (Two Devices)
```
Computer → Find IP (192.168.1.100)
Computer → Update App.vue socket URL
Computer → Start both servers
Phone → Open http://192.168.1.100:5173
Both → Join same room name
✅ Works!
```

### 3. Production Deployment
```
Server → Deploy to Railway/Heroku
Client → Deploy to Vercel/Netlify
Update → Client socket URL to server URL
Add → SSL certificates (HTTPS)
✅ Works globally!
```

---

## 🎯 Key Takeaways

1. **Server Only Signals** - No media goes through server
2. **P2P is Direct** - Video/audio flows between users only
3. **Room Name = Password** - Keep it secret
4. **HTTPS Required** - For production (not localhost)
5. **2 Users Max** - Architecture designed for 1-on-1

---

**You're all set! Time to start the servers and test! 🚀**
