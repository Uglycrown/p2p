# 🚀 Render Deployment Guide

## Problem You Had

**Error:** `CORS policy blocking` + `404 Not Found`

**Cause:** 
- Server CORS not properly configured for production
- Missing health check endpoints
- No logging for debugging

## ✅ What I Fixed

1. **Added proper CORS configuration**
   - Enabled for all origins
   - Added credentials support
   - Both Express and Socket.io CORS

2. **Added health check endpoints**
   - `GET /` - Returns server status
   - `GET /health` - Returns OK status

3. **Added logging**
   - Connection events
   - Join room events
   - Disconnect events

4. **Improved Socket.io config**
   - Added transports (websocket, polling)
   - Better error handling

---

## 🔄 How to Deploy to Render

### Method 1: Manual Upload (Easiest)

1. Go to https://dashboard.render.com
2. Click **"New +"** → **"Web Service"**
3. Select **"Build and deploy from a Git repository"** OR **"Public Git repository"**
4. If no Git, select **"Manual Deploy"**
5. Upload your **`server`** folder
6. Configure:
   ```
   Name: p2p-video-server
   Environment: Node
   Build Command: npm install
   Start Command: node index.js
   Instance Type: Free
   ```
7. Click **"Create Web Service"**
8. Wait 2-3 minutes for deployment

### Method 2: Connect GitHub

1. Push your code to GitHub
2. Go to Render dashboard
3. **"New +"** → **"Web Service"**
4. Connect GitHub repository
5. Select `server` folder as root
6. Same configuration as above
7. Render auto-deploys on every push!

---

## ✅ Verify Server is Working

### Test 1: Open in Browser

Open: `https://your-server.onrender.com`

**Expected Response:**
```json
{
  "status": "Server is running",
  "message": "P2P Video Chat Server",
  "timestamp": "2026-01-02T16:10:00.000Z"
}
```

### Test 2: Check Logs

In Render dashboard:
1. Click your service
2. Go to **"Logs"** tab
3. Should see:
   ```
   ═══════════════════════════════════════════════
     P2P Video Chat Server
     Running on port: 10000
     Time: ...
   ═══════════════════════════════════════════════
   ```

---

## 🔗 Connect Vercel to Render

### Step 1: Get Render URL

From Render dashboard, copy your URL:
```
https://p2p-video-server.onrender.com
```

### Step 2: Add to Vercel

1. Go to https://vercel.com/dashboard
2. Click your project: **p2p-blue-one**
3. Go to **Settings** → **Environment Variables**
4. Add new variable:
   - **Name:** `VITE_SERVER_URL`
   - **Value:** `https://p2p-video-server.onrender.com`
5. Click **Save**

### Step 3: Redeploy Vercel

1. Go to **Deployments** tab
2. Click **"..."** on latest deployment
3. Click **"Redeploy"**
4. Wait 1-2 minutes

---

## 🐛 Troubleshooting

### Issue: Still getting CORS error

**Check:**
- Is Render deployment finished?
- Did you add VITE_SERVER_URL to Vercel?
- Did you redeploy Vercel after adding variable?

**Fix:**
```bash
# Verify server is running
curl https://your-server.onrender.com

# Should return JSON, not error
```

### Issue: 404 Not Found

**Check:**
- Server logs in Render dashboard
- Look for "Server is running on port"

**Fix:**
- Check Start Command is: `node index.js`
- Verify package.json has correct dependencies

### Issue: Server sleeps after 15 minutes

**Render Free Tier:**
- Server sleeps after 15 min of inactivity
- First request takes 30-60 seconds to wake up
- This is normal for free tier

**Solution:**
- Upgrade to Render Starter ($7/month) for always-on
- OR use a ping service to keep it awake

---

## 📊 Server Configuration

### Current Setup

```javascript
PORT: process.env.PORT || 5000
CORS: Enabled for all origins (*)
Transports: WebSocket, Polling
Health Check: / and /health endpoints
```

### For Production (Optional)

Update CORS to specific origin:

```javascript
cors: {
  origin: "https://p2p-blue-one.vercel.app",
  methods: ["GET", "POST"],
  credentials: true
}
```

---

## ✅ Success Checklist

- [ ] Server deployed to Render
- [ ] Server URL accessible in browser
- [ ] JSON response visible at root URL
- [ ] VITE_SERVER_URL added to Vercel
- [ ] Vercel redeployed
- [ ] No CORS errors in browser console
- [ ] "Join Room" button works
- [ ] Video call connects successfully

---

## 🎉 You're Done!

Once all checkboxes are checked, your app is live and working!

**Live URLs:**
- Frontend: https://p2p-blue-one.vercel.app
- Backend: https://p2p-video-server.onrender.com

**Cost:** $0/month (100% FREE!)

---

## 📞 Support

If still having issues:
1. Check Render logs for errors
2. Check browser console for errors
3. Verify both URLs are accessible
4. Make sure you redeployed after each change

**Common mistake:** Forgetting to redeploy after making changes!
