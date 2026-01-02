<template>
  <!-- Lobby Screen (Before Call) -->
  <div v-if="!callAccepted && !callEnded" class="lobby-container">
    <div class="lobby-card">
      <h1 class="lobby-title">🔒 Private Video Chat</h1>
      <p class="lobby-subtitle">Connect securely with anyone, anywhere</p>
      
      <div class="lobby-form">
        <input 
          v-model="roomId" 
          placeholder="Enter Room Name" 
          class="lobby-input" 
          @keyup.enter="joinRoom"
        />
        <button @click="joinRoom" class="lobby-btn primary-btn">
          <span v-if="!myId">Join Room</span>
          <span v-else>Joined ✓</span>
        </button>
      </div>
      
      <div v-if="myId" class="status-box">
        <div class="status-indicator"></div>
        <p class="status-text">Connected • Waiting for friend...</p>
        <p v-if="userJoined" class="friend-joined">✨ Friend is here! Ready to call</p>
      </div>
      
      <button 
        v-if="userJoined && !isCalling" 
        @click="callUser" 
        class="start-call-btn"
      >
        <span class="call-icon">📞</span>
        Start Video Call
      </button>
      
      <div v-if="incomingCall && !callAccepted" class="incoming-call">
        <div class="incoming-animation"></div>
        <p class="incoming-text">Incoming Call...</p>
        <button @click="answerCall" class="answer-btn">
          <span class="call-icon">📞</span>
          Answer
        </button>
      </div>
    </div>
  </div>

  <!-- Fullscreen Video Call (During Call) -->
  <div v-if="callAccepted && !callEnded" class="fullscreen-call">
    <!-- Main Video (Friend's video - Large) -->
    <video 
      ref="userVideo" 
      autoplay 
      playsinline 
      class="main-video"
    ></video>
    
    <!-- Small Self Video (Picture-in-Picture) -->
    <div class="pip-video-container">
      <video 
        ref="myVideo" 
        muted 
        autoplay 
        playsinline 
        class="pip-video"
      ></video>
      <span class="pip-label">You</span>
    </div>
    
    <!-- Top Bar (Info) -->
    <div class="top-bar">
      <div class="call-info">
        <div class="call-status-dot"></div>
        <span class="call-duration">Connected</span>
      </div>
      <button @click="toggleFullscreen" class="icon-btn">
        <span v-if="!isFullscreen">⛶</span>
        <span v-else>⛶</span>
      </button>
    </div>
    
    <!-- Bottom Control Bar -->
    <div class="bottom-bar">
      <div class="controls-group">
        <!-- Audio Toggle -->
        <button 
          @click="toggleAudio" 
          :class="['control-btn', !audioEnabled && 'muted']"
          title="Mute/Unmute"
        >
          <span v-if="audioEnabled">🎤</span>
          <span v-else>🔇</span>
        </button>
        
        <!-- Video Toggle -->
        <button 
          @click="toggleVideo" 
          :class="['control-btn', !videoEnabled && 'video-off']"
          title="Camera On/Off"
        >
          <span v-if="videoEnabled">📹</span>
          <span v-else>📷</span>
        </button>
        
        <!-- Settings Toggle -->
        <button 
          @click="showSettings = !showSettings" 
          class="control-btn"
          title="Settings"
        >
          ⚙️
        </button>
        
        <!-- End Call -->
        <button 
          @click="leaveCall" 
          class="control-btn end-call-btn"
          title="End Call"
        >
          📵
        </button>
      </div>
    </div>
    
    <!-- Settings Panel (Slide up) -->
    <transition name="slide-up">
      <div v-if="showSettings" class="settings-panel">
        <div class="settings-header">
          <h3>Call Settings</h3>
          <button @click="showSettings = false" class="close-btn">✕</button>
        </div>
        
        <div class="settings-content">
          <div class="setting-item">
            <label>📊 Video Quality</label>
            <select v-model="selectedQuality" @change="changeVideoQuality" class="settings-select">
              <option value="low">Low (360p)</option>
              <option value="medium">Medium (480p)</option>
              <option value="high">High (720p)</option>
              <option value="hd">HD (1080p)</option>
            </select>
          </div>
          
          <div class="setting-item">
            <label>🎵 Audio Quality</label>
            <select v-model="selectedAudioQuality" @change="changeAudioQuality" class="settings-select">
              <option value="voice">Voice (16kHz)</option>
              <option value="music">Music (44.1kHz)</option>
              <option value="studio">Studio (48kHz)</option>
            </select>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import io from 'socket.io-client';
import SimplePeer from 'simple-peer';

// State
const socket = ref(null);
const myId = ref('');
const roomId = ref('');
const stream = ref(null);
const userJoined = ref(false); // Is the friend in the room?
const isCalling = ref(false);
const incomingCall = ref(false);
const callerSignal = ref(null);
const callerId = ref(null);
const callAccepted = ref(false);
const callEnded = ref(false);

// Video quality control
const selectedQuality = ref('medium');

// Audio quality control
const selectedAudioQuality = ref('studio');

// UI Control
const showSettings = ref(false);
const isFullscreen = ref(false);

// Media Controls (WhatsApp Style)
const audioEnabled = ref(true);
const videoEnabled = ref(true);

// Refs for HTML elements
const myVideo = ref(null);
const userVideo = ref(null);
const connectionRef = ref(null);

// Quality presets
const qualityPresets = {
  low: { width: 640, height: 360, frameRate: 15 },
  medium: { width: 854, height: 480, frameRate: 24 },
  high: { width: 1280, height: 720, frameRate: 30 },
  hd: { width: 1920, height: 1080, frameRate: 30 }
};

// Audio quality presets
const audioQualityPresets = {
  voice: {
    sampleRate: 16000,
    channelCount: 1,
    echoCancellation: true,
    noiseSuppression: true,
    autoGainControl: true
  },
  music: {
    sampleRate: 44100,
    channelCount: 2,
    echoCancellation: true,
    noiseSuppression: false,
    autoGainControl: false
  },
  studio: {
    sampleRate: 48000,
    channelCount: 2,
    echoCancellation: true,
    noiseSuppression: true,
    autoGainControl: true
  }
};

// 1. Initialize System
onMounted(async () => {
  // Get Camera/Mic Permissions immediately with default quality
  try {
    const constraints = {
      video: {
        width: { ideal: qualityPresets.medium.width },
        height: { ideal: qualityPresets.medium.height },
        frameRate: { ideal: qualityPresets.medium.frameRate }
      },
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true,
        sampleRate: 48000,
        channelCount: 2,
        sampleSize: 16,
        latency: 0.01
      }
    };
    const currentStream = await navigator.mediaDevices.getUserMedia(constraints);
    stream.value = currentStream;
    if (myVideo.value) myVideo.value.srcObject = currentStream;
  } catch (err) {
    alert("Please allow camera and microphone access!");
  }

  // Connect to Signaling Server
  socket.value = io('http://localhost:5000'); // CHANGE THIS URL WHEN DEPLOYING

  socket.value.on('me', (id) => myId.value = id);
  
  socket.value.on('userJoined', (id) => {
    console.log("Friend joined!");
    userJoined.value = true;
    callerId.value = id; // Assuming 2 people, the other ID is the friend
  });

  socket.value.on('callUser', (data) => {
    incomingCall.value = true;
    callerSignal.value = data.signal;
    callerId.value = data.from;
  });
  
  socket.value.on('callEnded', () => {
      endCallCleanup();
  });
});

// 2. Logic Functions
const joinRoom = () => {
  if(!roomId.value) return alert("Enter a room name!");
  socket.value.emit('joinRoom', roomId.value);
};

const callUser = () => {
  isCalling.value = true;
  const peer = new SimplePeer({
    initiator: true,
    trickle: false,
    stream: stream.value
  });

  peer.on('signal', (data) => {
    socket.value.emit('callUser', {
      userToCall: callerId.value, // We send this to the person who joined
      signalData: data,
      from: myId.value
    });
  });

  peer.on('stream', (userStream) => {
    userVideo.value.srcObject = userStream;
  });

  socket.value.on('callAccepted', (signal) => {
    callAccepted.value = true;
    peer.signal(signal);
  });

  connectionRef.value = peer;
};

const answerCall = () => {
  callAccepted.value = true;
  const peer = new SimplePeer({
    initiator: false,
    trickle: false,
    stream: stream.value
  });

  peer.on('signal', (data) => {
    socket.value.emit('answerCall', { signal: data, to: callerId.value });
  });

  peer.on('stream', (userStream) => {
    userVideo.value.srcObject = userStream;
  });

  peer.signal(callerSignal.value);
  connectionRef.value = peer;
};

// 3. Feature: Audio/Video Toggles (The "WhatsApp" Features)
const toggleAudio = () => {
  const audioTrack = stream.value.getAudioTracks()[0];
  if(audioTrack) {
    audioTrack.enabled = !audioTrack.enabled;
    audioEnabled.value = audioTrack.enabled;
  }
};

const toggleVideo = () => {
  const videoTrack = stream.value.getVideoTracks()[0];
  if(videoTrack) {
    videoTrack.enabled = !videoTrack.enabled;
    videoEnabled.value = videoTrack.enabled;
  }
};

const leaveCall = () => {
    socket.value.emit('endCall', roomId.value);
    endCallCleanup();
};

const endCallCleanup = () => {
    callEnded.value = true;
    if(connectionRef.value) connectionRef.value.destroy();
    window.location.reload(); // Simple reload to reset state
};

// 4. Change Video Quality
const changeVideoQuality = async () => {
  if (!stream.value) return;
  
  const quality = qualityPresets[selectedQuality.value];
  
  try {
    // Stop current video track
    const oldVideoTrack = stream.value.getVideoTracks()[0];
    if (oldVideoTrack) {
      oldVideoTrack.stop();
    }
    
    // Get new stream with updated quality
    const constraints = {
      video: {
        width: { ideal: quality.width },
        height: { ideal: quality.height },
        frameRate: { ideal: quality.frameRate }
      },
      audio: false // Keep existing audio track
    };
    
    const newStream = await navigator.mediaDevices.getUserMedia(constraints);
    const newVideoTrack = newStream.getVideoTracks()[0];
    
    // Replace video track in current stream
    stream.value.removeTrack(oldVideoTrack);
    stream.value.addTrack(newVideoTrack);
    
    // Update local video display
    if (myVideo.value) {
      myVideo.value.srcObject = stream.value;
    }
    
    // Replace track in peer connection
    if (connectionRef.value && connectionRef.value._pc) {
      const sender = connectionRef.value._pc.getSenders().find(s => s.track && s.track.kind === 'video');
      if (sender) {
        await sender.replaceTrack(newVideoTrack);
      }
    }
    
    console.log(`Video quality changed to ${selectedQuality.value}: ${quality.width}x${quality.height}@${quality.frameRate}fps`);
  } catch (err) {
    console.error('Failed to change video quality:', err);
    alert('Could not change video quality. Try again.');
  }
};

// 5. Change Audio Quality
const changeAudioQuality = async () => {
  if (!stream.value) return;
  
  const audioQuality = audioQualityPresets[selectedAudioQuality.value];
  
  try {
    // Stop current audio track
    const oldAudioTrack = stream.value.getAudioTracks()[0];
    if (oldAudioTrack) {
      oldAudioTrack.stop();
    }
    
    // Get new audio stream with updated quality
    const constraints = {
      audio: {
        sampleRate: audioQuality.sampleRate,
        channelCount: audioQuality.channelCount,
        echoCancellation: audioQuality.echoCancellation,
        noiseSuppression: audioQuality.noiseSuppression,
        autoGainControl: audioQuality.autoGainControl,
        sampleSize: 16,
        latency: 0.01
      },
      video: false // Keep existing video track
    };
    
    const newStream = await navigator.mediaDevices.getUserMedia(constraints);
    const newAudioTrack = newStream.getAudioTracks()[0];
    
    // Replace audio track in current stream
    stream.value.removeTrack(oldAudioTrack);
    stream.value.addTrack(newAudioTrack);
    
    // Update local audio
    if (myVideo.value) {
      myVideo.value.srcObject = stream.value;
    }
    
    // Replace track in peer connection
    if (connectionRef.value && connectionRef.value._pc) {
      const sender = connectionRef.value._pc.getSenders().find(s => s.track && s.track.kind === 'audio');
      if (sender) {
        await sender.replaceTrack(newAudioTrack);
      }
    }
    
    console.log(`Audio quality changed to ${selectedAudioQuality.value}: ${audioQuality.sampleRate}Hz, ${audioQuality.channelCount} channel(s)`);
  } catch (err) {
    console.error('Failed to change audio quality:', err);
    alert('Could not change audio quality. Try again.');
  }
};

// 6. Toggle Fullscreen
const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
    isFullscreen.value = true;
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen();
      isFullscreen.value = false;
    }
  }
};

</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  overflow: hidden;
}

/* ==================== LOBBY SCREEN ==================== */
.lobby-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.lobby-card {
  background: white;
  border-radius: 24px;
  padding: 60px 40px;
  max-width: 480px;
  width: 100%;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  text-align: center;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.lobby-title {
  font-size: 32px;
  color: #2d3748;
  margin-bottom: 12px;
  font-weight: 700;
}

.lobby-subtitle {
  color: #718096;
  font-size: 16px;
  margin-bottom: 40px;
}

.lobby-form {
  margin-bottom: 30px;
}

.lobby-input {
  width: 100%;
  padding: 16px 20px;
  font-size: 16px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  margin-bottom: 16px;
  transition: all 0.3s;
  font-family: inherit;
}

.lobby-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.lobby-btn {
  width: 100%;
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
}

.primary-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

.status-box {
  background: #f7fafc;
  padding: 20px;
  border-radius: 12px;
  margin: 20px 0;
}

.status-indicator {
  width: 8px;
  height: 8px;
  background: #48bb78;
  border-radius: 50%;
  display: inline-block;
  margin-right: 8px;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  color: #4a5568;
  font-size: 14px;
  margin-bottom: 10px;
}

.friend-joined {
  color: #48bb78;
  font-weight: 600;
  font-size: 16px;
  animation: slideIn 0.5s ease;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateX(-10px); }
  to { opacity: 1; transform: translateX(0); }
}

.start-call-btn {
  width: 100%;
  padding: 18px;
  background: #48bb78;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 20px;
}

.start-call-btn:hover {
  background: #38a169;
  transform: scale(1.02);
  box-shadow: 0 8px 20px rgba(72, 187, 120, 0.4);
}

.call-icon {
  font-size: 24px;
}

.incoming-call {
  margin-top: 30px;
  animation: bounceIn 0.6s ease;
}

@keyframes bounceIn {
  0% { transform: scale(0.8); opacity: 0; }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); opacity: 1; }
}

.incoming-animation {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  border: 4px solid #667eea;
  border-radius: 50%;
  animation: ring 1s infinite;
}

@keyframes ring {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.incoming-text {
  font-size: 20px;
  color: #2d3748;
  margin-bottom: 20px;
  font-weight: 600;
}

.answer-btn {
  padding: 16px 40px;
  background: #48bb78;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.answer-btn:hover {
  background: #38a169;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(72, 187, 120, 0.4);
}

/* ==================== FULLSCREEN CALL ==================== */
.fullscreen-call {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #000;
  overflow: hidden;
}

.main-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  position: absolute;
  top: 0;
  left: 0;
}

.pip-video-container {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 200px;
  height: 150px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
  z-index: 10;
  border: 3px solid rgba(255,255,255,0.2);
  transition: all 0.3s;
}

.pip-video-container:hover {
  transform: scale(1.05);
  border-color: rgba(255,255,255,0.4);
}

.pip-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pip-label {
  position: absolute;
  bottom: 8px;
  left: 8px;
  background: rgba(0,0,0,0.7);
  color: white;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

/* Top Bar */
.top-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 20px 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to bottom, rgba(0,0,0,0.6) 0%, transparent 100%);
  z-index: 5;
}

.call-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.call-status-dot {
  width: 10px;
  height: 10px;
  background: #48bb78;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.call-duration {
  color: white;
  font-size: 16px;
  font-weight: 500;
}

.icon-btn {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.3s;
  backdrop-filter: blur(10px);
}

.icon-btn:hover {
  background: rgba(255,255,255,0.3);
  transform: scale(1.1);
}

/* Bottom Control Bar */
.bottom-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 30px;
  display: flex;
  justify-content: center;
  background: linear-gradient(to top, rgba(0,0,0,0.8) 0%, transparent 100%);
  z-index: 5;
}

.controls-group {
  display: flex;
  gap: 20px;
  align-items: center;
}

.control-btn {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: none;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(10px);
  color: white;
  font-size: 28px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.control-btn:hover {
  background: rgba(255,255,255,0.3);
  transform: scale(1.1);
}

.control-btn.muted {
  background: rgba(239, 68, 68, 0.9);
}

.control-btn.video-off {
  background: rgba(59, 130, 246, 0.9);
}

.control-btn.end-call-btn {
  background: rgba(239, 68, 68, 0.9);
  width: 70px;
  height: 70px;
}

.control-btn.end-call-btn:hover {
  background: rgba(220, 38, 38, 1);
  transform: scale(1.15);
}

/* Settings Panel */
.settings-panel {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(255,255,255,0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px 24px 0 0;
  padding: 30px;
  z-index: 20;
  max-height: 50vh;
  overflow-y: auto;
}

.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.settings-header h3 {
  font-size: 24px;
  color: #2d3748;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  color: #718096;
  cursor: pointer;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s;
}

.close-btn:hover {
  background: #f7fafc;
  color: #2d3748;
}

.settings-content {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

.setting-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.setting-item label {
  font-size: 16px;
  font-weight: 600;
  color: #2d3748;
}

.settings-select {
  padding: 14px 16px;
  font-size: 15px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background: white;
  cursor: pointer;
  transition: all 0.3s;
  font-family: inherit;
}

.settings-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.settings-select:hover {
  border-color: #cbd5e0;
}

/* Slide Up Animation */
.slide-up-enter-active, .slide-up-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-up-enter-from, .slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .pip-video-container {
    width: 120px;
    height: 90px;
    top: 15px;
    right: 15px;
  }
  
  .control-btn {
    width: 50px;
    height: 50px;
    font-size: 24px;
  }
  
  .control-btn.end-call-btn {
    width: 60px;
    height: 60px;
  }
  
  .controls-group {
    gap: 15px;
  }
  
  .lobby-card {
    padding: 40px 30px;
  }
}

/* Hide default elements (for compatibility) */
.hidden {
  display: none !important;
}
</style>
