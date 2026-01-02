# 🚀 Deployment Guide - Host Your P2P Video Chat Live

## Quick Overview

To host this app live, you need to deploy:
1. **Backend (Server)** - Node.js server on Render/Railway/Heroku
2. **Frontend (Client)** - Vue app on Vercel/Netlify

---

## 🎯 Option 1: FREE Hosting (Recommended for Testing)

### Step 1: Deploy Backend to Render.com (Free)

**1.1 Create Render Account**
- Go to https://render.com
- Sign up with GitHub/Email

**1.2 Deploy Server**
1. Click "New +" → "Web Service"
2. Choose "Build and deploy from a Git repository"
3. Connect your GitHub (or upload manually)
4. Settings:
   - **Name**: `p2p-video-server`
   - **Environment**: `Node`
   - **Build Command**: `npm install`
   - **Start Command**: `node index.js`
   - **Instance Type**: `Free`
5. Click "Create Web Service"
6. Wait 2-3 minutes for deployment
7. Copy your server URL (e.g., `https://p2p-video-server.onrender.com`)

---

### Step 2: Update Client Code

**2.1 Edit the Socket.io URL**

Open: `client/src/App.vue` (Line 218 approximately)

Change from:
```javascript
socket.value = io('http://localhost:5000');
```

To:
```javascript
socket.value = io('https://YOUR-RENDER-URL.onrender.com');
```

Example:
```javascript
socket.value = io('https://p2p-video-server.onrender.com');
```

**2.2 Save the file**

---

### Step 3: Deploy Frontend to Vercel (Free)

**3.1 Create Vercel Account**
- Go to https://vercel.com
- Sign up with GitHub/Email

**3.2 Install Vercel CLI (Optional but easier)**
```bash
npm install -g vercel
```

**3.3 Deploy Client**

**Option A: Using Vercel CLI (Easier)**
```bash
cd client
vercel
```
Follow prompts:
- Set up and deploy? `Y`
- Which scope? (Your account)
- Link to existing project? `N`
- What's your project's name? `p2p-video-chat`
- In which directory is your code located? `./`
- Want to override the settings? `N`

**Option B: Using Vercel Website**
1. Go to vercel.com → "Add New" → "Project"
2. Import your `client` folder
3. Framework Preset: `Vite`
4. Click "Deploy"

**3.4 Get Your Live URL**
- Vercel gives you: `https://p2p-video-chat.vercel.app`
- Copy this URL!

---

### Step 4: Test Your Live App! 🎉

1. Open your Vercel URL on your computer
2. Open the same URL on your phone (or another device)
3. Both join the same room name
4. Start video call
5. **It works live on the internet!** 🚀

---

## 🎯 Option 2: Alternative Services

### Backend Alternatives

**Railway.app** (Easy, Free tier)
1. Go to https://railway.app
2. "New Project" → "Deploy from GitHub"
3. Select your repo
4. Add `server` folder
5. Railway auto-detects Node.js
6. Get your URL: `https://your-app.railway.app`

**Heroku** (Popular, Free tier removed but $5/month)
```bash
cd server
heroku create p2p-video-server
git init
git add .
git commit -m "Deploy server"
heroku git:remote -a p2p-video-server
git push heroku master
```

### Frontend Alternatives

**Netlify** (Similar to Vercel)
1. Go to https://netlify.com
2. Drag & drop your `client/dist` folder
3. Get instant URL

**GitHub Pages** (Free)
```bash
cd client
npm run build
# Upload dist/ folder to GitHub Pages
```

---

## 📝 Complete Deployment Checklist

### Before Deploying:

- [ ] Test locally (works with two tabs?)
- [ ] Server dependencies installed (`cd server && npm install`)
- [ ] Client dependencies installed (`cd client && npm install`)
- [ ] Build client works (`cd client && npm run build`)

### Server Deployment:

- [ ] Choose hosting provider (Render/Railway/Heroku)
- [ ] Create account
- [ ] Deploy server
- [ ] Copy server URL
- [ ] Test server URL in browser (should see "Cannot GET /")

### Client Deployment:

- [ ] Update `App.vue` with server URL
- [ ] Test client build locally (`npm run build`)
- [ ] Choose hosting provider (Vercel/Netlify)
- [ ] Deploy client
- [ ] Copy client URL

### Testing Live:

- [ ] Open client URL in browser
- [ ] Allow camera/microphone permissions
- [ ] Open same URL on phone/another device
- [ ] Join same room on both devices
- [ ] Start video call
- [ ] Test all features (mute, video toggle, quality settings)

---

## 🔒 Important Production Settings

### Update CORS in Server

Edit `server/index.js`:

```javascript
const io = require("socket.io")(server, {
  cors: {
    origin: "https://your-vercel-url.vercel.app", // Your actual Vercel URL
    methods: ["GET", "POST"]
  }
});
```

### Add Environment Variables (Optional)

**Server:**
- `PORT`: Auto-set by hosting provider
- `CORS_ORIGIN`: Your frontend URL

**Client:**
- `VITE_SERVER_URL`: Your backend URL

Create `.env` in client folder:
```
VITE_SERVER_URL=https://your-server.onrender.com
```

Use in code:
```javascript
socket.value = io(import.meta.env.VITE_SERVER_URL);
```

---

## 🌍 Custom Domain (Optional)

### Add Custom Domain to Vercel
1. Go to Vercel Dashboard → Your Project → Settings → Domains
2. Add your domain (e.g., `videochat.yourdomain.com`)
3. Update DNS records as instructed
4. SSL certificate auto-generated!

### Add Custom Domain to Render
1. Go to Render Dashboard → Your Service → Settings
2. Add custom domain
3. Update DNS records
4. SSL certificate auto-generated!

---

## 🐛 Common Deployment Issues

### Issue: "CORS Error"
**Solution:** Update server CORS origin to match client URL

### Issue: "Cannot connect to server"
**Solution:** 
- Check server is running (visit server URL)
- Verify Socket.io URL in `App.vue` is correct
- Ensure server URL uses `https://` not `http://`

### Issue: "Camera not working on mobile"
**Solution:** Mobile browsers require HTTPS for camera access
- Make sure you're using Vercel/Netlify (auto HTTPS)
- Don't use HTTP URLs in production

### Issue: "Page not loading"
**Solution:**
- Clear browser cache
- Check Vercel deployment logs
- Verify build succeeded

---

## 💰 Cost Breakdown

### FREE Tier (Recommended for Testing):
- **Render Free**: 750 hours/month (server sleeps after 15 min inactivity)
- **Vercel Free**: Unlimited bandwidth, 100GB bandwidth
- **Total Cost**: $0/month ✅

### Paid Tier (For Production):
- **Render Starter**: $7/month (always-on server)
- **Vercel Pro**: $20/month (better performance)
- **Total Cost**: $7-27/month

### Enterprise:
- Custom pricing for high traffic
- Dedicated servers
- 99.9% uptime guarantee

---

## 🚀 Deploy Right Now - Quick Commands

```bash
# 1. Install Vercel CLI
npm install -g vercel

# 2. Deploy Server to Render (use website, easier)
# Go to render.com → Deploy

# 3. Update client code with server URL
# Edit client/src/App.vue line 218

# 4. Deploy Client
cd client
vercel

# 5. Done! Get your URL and test!
```

---

## 📊 Post-Deployment Testing

### Test Checklist:
1. ✅ Open client URL on computer
2. ✅ Open client URL on phone
3. ✅ Both join same room
4. ✅ Camera/microphone permissions work
5. ✅ Video call connects
6. ✅ Audio quality is clear
7. ✅ Video quality is good
8. ✅ Mute button works
9. ✅ Video toggle works
10. ✅ Settings panel opens
11. ✅ Quality changes work
12. ✅ End call works
13. ✅ Can reconnect after ending

---

## 🎉 You're Live!

Once deployed, share your link:
- `https://p2p-video-chat.vercel.app`

**Your friends can use it from anywhere in the world!** 🌍

---

## 📞 Need Help?

Common commands:
```bash
# Check if server is running
curl https://your-server.onrender.com

# Rebuild client
cd client
npm run build

# Redeploy to Vercel
cd client
vercel --prod
```

---

**Made with ❤️ - Now go live and show it to your friends!** 🚀
