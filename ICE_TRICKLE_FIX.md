# ICE Trickle Fix - Call Transition Issue

## Issue
✅ App connecting but not transitioning to video call screen
✅ Stuck on "Connecting..." page

## Root Cause
With `trickle: true`, WebRTC sends multiple signal events:
1. Initial SDP offer/answer
2. Then multiple ICE candidates as they're discovered

The previous code only handled the first signal and ignored subsequent ICE candidates.

## Solution Implemented

### 1. Handle Trickled ICE Candidates in `callUser` Event
```javascript
socket.value.on('callUser', (data) => {
  const signal = e2eEncryption.value 
    ? e2eEncryption.value.decryptSignal(data.signal)
    : data.signal;
    
  // First signal: setup call
  if (!incomingCall.value) {
    incomingCall.value = true;
    callerSignal.value = signal;
    callerId.value = data.from;
    answerCall();
  } 
  // Subsequent signals: ICE candidates
  else if (connectionRef.value && signal.candidate) {
    console.log('📡 Received trickled ICE candidate');
    connectionRef.value.signal(signal);
  }
});
```

### 2. Handle Trickled ICE Candidates in `callAccepted` Event
```javascript
socket.value.on('callAccepted', (signal) => {
  const decryptedSignal = e2eEncryption.value 
    ? e2eEncryption.value.decryptSignal(signal)
    : signal;
    
  // First signal: start call
  if (!callAccepted.value) {
    callAccepted.value = true;
    startCallTimer();
    peer.signal(decryptedSignal);
    resetControlsTimer();
  } 
  // Subsequent signals: ICE candidates
  else if (decryptedSignal.candidate && connectionRef.value) {
    console.log('📡 Received trickled ICE candidate');
    connectionRef.value.signal(decryptedSignal);
  }
});
```

## How ICE Trickling Works

### Without Trickling (trickle: false)
```
Peer A → [collects ALL ICE candidates] → sends ONE signal → Peer B
```
❌ Slow: Must wait for all candidates
❌ Can timeout on mobile networks

### With Trickling (trickle: true)
```
Peer A → [initial offer] → Peer B
Peer A → [ICE candidate 1] → Peer B
Peer A → [ICE candidate 2] → Peer B
Peer A → [ICE candidate 3] → Peer B
```
✅ Fast: Connection starts immediately
✅ Works better on mobile

## Testing

1. **Open app on phone** (already installed)
2. **Open browser** (do hard refresh: Ctrl+Shift+R)
3. **Both join same room**
4. **Should now transition to video call screen** ✅

## Expected Behavior

1. Enter room name → Join Room
2. "Connecting..." appears
3. Friend joins
4. **Automatic transition to video call** 🎥
5. Both can see/hear each other

## Debug Console Messages

You should see:
```
📞 Answering call...
📡 Received trickled ICE candidate
📡 Received trickled ICE candidate
Connected to peer!
```

---

**Updated**: January 4, 2026 10:30 AM
**Status**: ✅ Installed on device RZCW92Y7DVP
**Next**: Test the video call!
