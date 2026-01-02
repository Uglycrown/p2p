# 🔒 Private P2P Video & Audio Chat App

A secure, peer-to-peer video and audio calling application built with Vue 3 and WebRTC. Your own "WhatsApp-killer" where you control the infrastructure!

## 🚀 Features

- **Secure Handshake**: Password-protected rooms - only 2 people allowed
- **Video Calling**: High-quality, direct peer-to-peer video streaming
- **Audio Call Mode**: Toggle video on/off for audio-only calls
- **Mute/Unmute**: Control microphone privacy during calls
- **Camera Toggle**: Turn camera on/off without disconnecting
- **Visual Feedback**: Real-time connection status updates

## 📁 Project Structure

```
p2p/
├── server/          # Node.js signaling server
│   ├── index.js
│   └── package.json
├── client/          # Vue 3 frontend
│   ├── src/
│   │   ├── App.vue
│   │   └── main.js
│   └── package.json
└── README.md
```

## 🛠️ Installation & Setup

### 1. Start the Server

Open a terminal in the `server` folder:

```bash
cd server
node index.js
```

You should see: `Server is running on port 5000`

### 2. Start the Client

Open a **new terminal** in the `client` folder:

```bash
cd client
npm run dev
```

The app will open at `http://localhost:5173` (or similar)

## 🎮 How to Use

### Testing with Two Users

**Option 1: Two Browser Tabs**
1. Open the app in two different browser tabs
2. Both users enter the **same room name** (e.g., `SecretRoom123`)
3. Click "Join Room" in both tabs
4. When both are connected, click "Start Video Call"
5. The other user will see "Incoming Call..." and can click "Answer"

**Option 2: Two Devices on Same Network**
1. Find your computer's local IP address
2. Update `socket.io` connection in `client/src/App.vue`:
   ```javascript
   socket.value = io('http://YOUR_LOCAL_IP:5000');
   ```
3. Access the app from your phone's browser using `http://YOUR_LOCAL_IP:5173`

### During a Call

- **🎙️ Mute/Unmute**: Toggle your microphone
- **📹 Stop/Start Video**: Switch between video and audio-only mode
- **☎️ End Call**: Disconnect and return to lobby

## 🔧 Technical Details

### Backend (Signaling Server)
- **Express.js**: HTTP server
- **Socket.io**: Real-time WebSocket communication
- **CORS enabled**: Allows frontend connections

The server only handles the initial "handshake" between peers. Once connected, all audio/video data flows directly peer-to-peer (P2P) - the server never sees your media!

### Frontend (Vue 3)
- **Vue 3 Composition API**: Modern reactive framework
- **Socket.io-client**: Signaling communication
- **simple-peer**: WebRTC wrapper for P2P connections
- **Navigator MediaDevices API**: Camera/microphone access

## 🔒 Security Features

1. **Room Size Limit**: Maximum 2 people per room
2. **Private Room Names**: Act as passwords - use complex names
3. **No Data Storage**: Server doesn't record any calls
4. **Direct P2P**: Media streams directly between users

## 🚀 Deployment Tips

### Deploy Server (Backend)
- Use platforms like **Heroku**, **Railway**, or **DigitalOcean**
- Update the `PORT` environment variable as needed
- Ensure CORS is properly configured for your domain

### Deploy Client (Frontend)
- Use **Vercel**, **Netlify**, or **GitHub Pages**
- Update the Socket.io URL in `App.vue`:
  ```javascript
  socket.value = io('https://your-backend-url.com');
  ```

### Important: HTTPS Required
WebRTC requires HTTPS in production (except localhost). Make sure both your frontend and backend use SSL certificates.

## 🐛 Troubleshooting

**Camera/Microphone not working?**
- Check browser permissions (usually a camera icon in the address bar)
- Use Chrome/Edge for best compatibility
- HTTPS is required for production (HTTP works on localhost)

**Connection fails?**
- Ensure server is running on port 5000
- Check firewall settings
- Verify the Socket.io URL matches your server address

**Video freezing?**
- Check your internet connection
- Try disabling video (audio-only mode)
- Close other bandwidth-heavy applications

## 📝 Future Enhancements

- [ ] Screen sharing
- [ ] Chat messages during calls
- [ ] Recording functionality
- [ ] Multiple room support
- [ ] User authentication
- [ ] Mobile app (React Native/Flutter)

## 📄 License

This project is open-source. Feel free to modify and use it for your needs!

## 🙏 Credits

Built with:
- [Vue.js](https://vuejs.org/)
- [Socket.io](https://socket.io/)
- [simple-peer](https://github.com/feross/simple-peer)
- [WebRTC](https://webrtc.org/)

---

**Made with ❤️ for secure, private communications**
