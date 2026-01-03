# Video Mirror & Autoplay Fix

## Issues Fixed

### 1. Self-View Not Mirrored
**Problem:** The "You" video (self-view) was not mirrored, making it feel unnatural (like looking at a photo instead of a mirror).

**Solution:** Added CSS transform to flip the video horizontally:
```css
.pip-video {
  transform: scaleX(-1);  /* Mirror effect */
}

.video-preview {
  transform: scaleX(-1);  /* Mirror in lobby too */
}
```

**Affected Elements:**
- Small PIP video during call (bottom-right corner)
- Video preview in lobby (before joining)

### 2. Friend's Video Not Playing After Quality Change
**Problem:** When changing video quality settings, the friend's video (large main video) would freeze and not resume playing automatically.

**Solution:** Implemented multiple layers of autoplay enforcement:

#### A. Explicit play() calls when receiving stream
```javascript
peer.on('stream', (userStream) => {
  userVideo.value.srcObject = userStream;
  // Ensure video plays
  userVideo.value.play().catch(err => console.log('Autoplay prevented:', err));
});
```

#### B. Watch for srcObject changes
```javascript
watch(() => userVideo.value?.srcObject, async (newSrcObject) => {
  if (newSrcObject && userVideo.value) {
    await nextTick();
    userVideo.value.play().catch(err => {
      console.log('Video autoplay prevented:', err);
      // Retry after short delay
      setTimeout(() => {
        userVideo.value?.play().catch(e => console.log('Retry failed:', e));
      }, 100);
    });
  }
});
```

## Technical Details

### Mirror Effect
- Uses CSS `transform: scaleX(-1)` to horizontally flip video
- Applied to both lobby preview and in-call self-view
- Does NOT mirror friend's video (only your own)
- Native CSS, no performance impact

### Autoplay Strategy
1. **HTML autoplay attribute** - First line of defense
2. **Explicit play() on stream received** - Ensures play when peer connects
3. **Vue watcher on srcObject** - Catches any stream changes (quality, reconnect, etc.)
4. **Retry mechanism** - If autoplay blocked, retry after 100ms
5. **nextTick() usage** - Waits for DOM update before playing

### Browser Autoplay Policies
Modern browsers block autoplay with sound, but our implementation:
- Friend's video is NOT muted (has sound) ✅
- Explicit user interaction (clicking join/quality) enables autoplay ✅
- Retry mechanism handles edge cases ✅

## Testing Checklist

### Mirror Effect
- [ ] Self-view mirrored in lobby
- [ ] Self-view mirrored during call (PIP)
- [ ] Friend's video NOT mirrored
- [ ] Mirror works after camera switch
- [ ] Mirror works after screen share

### Autoplay
- [ ] Friend's video plays on initial connection
- [ ] Friend's video continues after changing quality (low/medium/high/HD)
- [ ] Friend's video continues after changing audio quality
- [ ] Friend's video resumes after network reconnection
- [ ] Friend's video resumes after they switch cameras
- [ ] Works on mobile (iOS Safari, Chrome, Firefox)
- [ ] Works on desktop (Chrome, Firefox, Safari, Edge)

## Files Modified
- `client/src/App.vue`:
  - Added `watch` and `nextTick` to Vue imports
  - Added CSS transforms for mirroring
  - Added explicit `.play()` calls in peer stream handlers
  - Added Vue watcher for automatic video playback

## Browser Compatibility
- **Mirror:** All modern browsers (CSS transforms)
- **Autoplay:** Chrome 66+, Firefox 66+, Safari 11+, Edge 79+
- **Mobile:** iOS 10+, Android Chrome 53+

## Known Limitations
- Autoplay may be blocked on first load without user interaction
- Some browsers require HTTPS for autoplay with audio
- Retry mechanism has 100ms delay (imperceptible to users)

## Deployment Notes
1. Build completed successfully ✅
2. No breaking changes
3. Fully backward compatible
4. Works with existing video quality/camera switching features
