# 🔧 Quick Fix: ERR_NAME_NOT_RESOLVED

## ❌ The Problem

Your error shows:
```
GET https://https/socket.io/...
```

Notice: `https://https` - **DOUBLE HTTPS!**

This means you entered the wrong value in Vercel environment variables.

---

## ✅ The Solution

### Fix the Environment Variable

1. **Go to:** https://vercel.com/dashboard
2. **Click:** Your project `p2p-blue-one`
3. **Go to:** Settings tab
4. **Click:** Environment Variables
5. **Find:** `VITE_SERVER_URL`
6. **Click:** `...` menu → Edit

### Change the Value

**❌ Current (WRONG):**
```
VITE_SERVER_URL = https
```
or
```
VITE_SERVER_URL = https://https
```

**✅ Correct (COPY THIS EXACTLY):**
```
VITE_SERVER_URL = https://p2p-video-server.onrender.com
```

### Save and Redeploy

7. **Click:** Save
8. **Go to:** Deployments tab
9. **Click:** `...` → Redeploy
10. **Wait:** 1-2 minutes

---

## ✅ Test After Fix

1. Open: https://p2p-blue-one.vercel.app
2. Press F12 (Console)
3. Enter room name
4. Click "Join Room"
5. Should see:
   ```
   ✅ Connected to server!
   ✅ Got my ID: abc123...
   ```

---

## 💡 Key Points

**The URL MUST be:**
- ✅ Complete: `https://p2p-video-server.onrender.com`
- ✅ Include `https://`
- ✅ Include full domain name
- ✅ No spaces
- ✅ No quotes

**NOT:**
- ❌ Just `https`
- ❌ Just `p2p-video-server`
- ❌ `http://` (must be `https://`)
- ❌ With quotes: `"https://..."`

---

## 🎯 Success Checklist

- [ ] Changed VITE_SERVER_URL to correct value
- [ ] Saved changes
- [ ] Redeployed Vercel
- [ ] Waited for deployment to finish
- [ ] Tested app in browser
- [ ] Console shows "Connected to server!"
- [ ] No more ERR_NAME_NOT_RESOLVED

---

## 🚀 You're Almost There!

Once you fix this URL, everything will work perfectly! 🎉
