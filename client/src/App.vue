<template>
  <!-- Lobby Screen (Before Call) -->
  <div v-if="!callAccepted && !callEnded" class="lobby-container">
    <div class="lobby-card">
      <h1 class="lobby-title">🔒 Private Video Chat</h1>
      <p class="lobby-subtitle">Connect securely with anyone, anywhere</p>
      
      <!-- Video Preview -->
      <div class="video-preview-container">
        <video 
          ref="myVideo" 
          muted 
          autoplay 
          playsinline 
          class="video-preview"
        ></video>
        <div v-if="!cameraEnabled" class="camera-off-message">
          <span class="camera-icon">📷</span>
          <p>Camera is off</p>
        </div>
        
        <!-- Camera Toggle Button (Overlay) -->
        <button 
          @click="toggleCameraLobby" 
          class="lobby-camera-toggle"
          :title="cameraEnabled ? 'Turn Camera Off' : 'Turn Camera On'"
        >
          <svg v-if="cameraEnabled" class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M23 7l-7 5 7 5V7z"/>
            <rect x="1" y="5" width="15" height="14" rx="2" ry="2"/>
          </svg>
          <svg v-else class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="1" y1="1" x2="23" y2="23"/>
            <path d="M21 21H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h3m3 0h6l2-3h4a2 2 0 0 1 2 2v9.34m-7.72-2.06a4 4 0 1 1-5.56-5.56"/>
          </svg>
          <span class="toggle-label">{{ cameraEnabled ? 'Camera On' : 'Camera Off' }}</span>
        </button>
      </div>
      
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
  <div 
    v-if="callAccepted && !callEnded" 
    class="fullscreen-call"
    @click="handleScreenTap"
  >
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
    
    <!-- Top Bar (Info) - Auto-hide -->
    <transition name="fade">
      <div v-show="showControls" class="top-bar">
        <div class="call-info">
          <div class="call-status-dot"></div>
          <span class="call-duration">{{ callDuration }}</span>
        </div>
        
        <!-- Settings Button -->
        <button 
          @click.stop="showSettings = !showSettings" 
          class="settings-btn-top"
          title="Settings"
        >
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/>
            <path d="M12 1v6m0 6v6M3.93 3.93l4.24 4.24m5.66 5.66l4.24 4.24M1 12h6m6 0h6M3.93 20.07l4.24-4.24m5.66-5.66l4.24-4.24"/>
          </svg>
        </button>
      </div>
    </transition>
    
    <!-- Bottom Control Bar - Auto-hide -->
    <transition name="slide-up">
      <div v-show="showControls" class="bottom-bar">
        <div class="controls-group">
          <!-- Camera Toggle -->
          <button 
            @click.stop="toggleCamera" 
            :class="['control-btn-modern', !cameraEnabled && 'btn-disabled']"
            :title="cameraEnabled ? 'Turn Camera Off' : 'Turn Camera On'"
          >
            <svg v-if="cameraEnabled" class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 7l-7 5 7 5V7z"/>
              <rect x="1" y="5" width="15" height="14" rx="2" ry="2"/>
            </svg>
            <svg v-else class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="1" y1="1" x2="23" y2="23"/>
              <path d="M21 21H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h3m3 0h6l2-3h4a2 2 0 0 1 2 2v9.34m-7.72-2.06a4 4 0 1 1-5.56-5.56"/>
            </svg>
          </button>
          
          <!-- Switch Camera (Front/Rear) -->
          <button 
            v-if="availableCameras.length > 1 && cameraEnabled"
            @click.stop="switchCamera" 
            class="control-btn-modern"
            :title="facingMode === 'user' ? 'Switch to Rear Camera' : 'Switch to Front Camera'"
          >
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M17 10l5 5-5 5"/>
              <path d="M22 15H11a4 4 0 0 1-4-4V8a4 4 0 0 1 4-4h11"/>
            </svg>
          </button>
          
          <!-- Audio Toggle -->
          <button 
            @click.stop="toggleAudio" 
            :class="['control-btn-modern', !audioEnabled && 'btn-disabled']"
            :title="audioEnabled ? 'Mute' : 'Unmute'"
          >
            <svg v-if="audioEnabled" class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
              <line x1="12" y1="19" x2="12" y2="23"/>
              <line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
            <svg v-else class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="1" y1="1" x2="23" y2="23"/>
              <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6"/>
              <path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2a7 7 0 0 1-.11 1.23"/>
              <line x1="12" y1="19" x2="12" y2="23"/>
              <line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
          </button>
        </div>
      </div>
    </transition>
    
    <!-- Hang Up Button (Always Visible) -->
    <div class="hang-up-container-always">
      <button 
        @click.stop="leaveCall" 
        class="hang-up-btn-always"
        title="End Call"
      >
        <svg class="icon" viewBox="0 0 24 24" fill="currentColor">
          <path d="M22 10.5V11a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-1a2 2 0 0 1 2-2h1a2 2 0 0 1 2 2zM2 11V10.5a2 2 0 0 1 2-2h1a2 2 0 0 1 2 2V11a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2zm12-8C9.79 3 6 5.91 6 9.5V11H4.5c-1.1 0-2 .9-2 2v1c0 1.1.9 2 2 2H6v3c0 .55.45 1 1 1h10c.55 0 1-.45 1-1v-3h1.5c1.1 0 2-.9 2-2v-1c0-1.1-.9-2-2-2H18V9.5C18 5.91 14.21 3 12 3z"/>
        </svg>
      </button>
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
            <label>📹 Camera</label>
            <div class="camera-info">
              <p class="info-text">
                Status: <strong>{{ cameraEnabled ? 'On' : 'Off' }}</strong>
              </p>
              <p class="info-text">
                Mode: <strong>{{ facingMode === 'user' ? 'Front Camera' : 'Rear Camera' }}</strong>
              </p>
              <p class="info-text" v-if="availableCameras.length > 0">
                Available: <strong>{{ availableCameras.length }} camera(s)</strong>
              </p>
            </div>
          </div>
          
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
const showControls = ref(true);
let controlsTimeout = null;
const callStartTime = ref(null);
const callDuration = ref('00:00');

// Media Controls (WhatsApp Style)
const audioEnabled = ref(true);
const videoEnabled = ref(true);

// Camera Controls
const cameraEnabled = ref(false);
const facingMode = ref('user'); // 'user' = front camera, 'environment' = rear camera
const availableCameras = ref([]);

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
  // Get available cameras
  await enumerateCameras();
  
  // Get Camera/Mic Permissions - Start with camera ON
  try {
    const quality = qualityPresets[selectedQuality.value];
    const constraints = {
      video: {
        width: { ideal: quality.width },
        height: { ideal: quality.height },
        frameRate: { ideal: quality.frameRate },
        facingMode: facingMode.value
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
    cameraEnabled.value = true; // Mark camera as enabled
    videoEnabled.value = true;
  } catch (err) {
    console.error("Media access error:", err);
    alert("Please allow camera and microphone access!");
  }

  // Connect to Signaling Server
  // IMPORTANT: Replace with your Render server URL after deploying
  const serverUrl = import.meta.env.VITE_SERVER_URL || 'http://localhost:5000';
  socket.value = io(serverUrl);

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
  
  // Create peer with current stream (may or may not have video)
  const peer = new SimplePeer({
    initiator: true,
    trickle: false,
    stream: stream.value,
    offerOptions: {
      offerToReceiveAudio: true,
      offerToReceiveVideo: true
    }
  });

  peer.on('signal', (data) => {
    socket.value.emit('callUser', {
      userToCall: callerId.value,
      signalData: data,
      from: myId.value
    });
  });

  peer.on('stream', (userStream) => {
    userVideo.value.srcObject = userStream;
  });

  socket.value.on('callAccepted', (signal) => {
    callAccepted.value = true;
    startCallTimer();
    peer.signal(signal);
    resetControlsTimer();
  });

  connectionRef.value = peer;
};

const answerCall = () => {
  callAccepted.value = true;
  startCallTimer();
  
  // Create peer with current stream (may or may not have video)
  const peer = new SimplePeer({
    initiator: false,
    trickle: false,
    stream: stream.value,
    answerOptions: {
      offerToReceiveAudio: true,
      offerToReceiveVideo: true
    }
  });

  peer.on('signal', (data) => {
    socket.value.emit('answerCall', { signal: data, to: callerId.value });
  });

  peer.on('stream', (userStream) => {
    userVideo.value.srcObject = userStream;
  });

  peer.signal(callerSignal.value);
  connectionRef.value = peer;
  
  // Start auto-hide timer
  resetControlsTimer();
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

// Lobby Camera Toggle (Same as toggleCamera but for lobby screen)
const toggleCameraLobby = async () => {
  if (cameraEnabled.value) {
    // Turn camera OFF
    const videoTrack = stream.value?.getVideoTracks()[0];
    if (videoTrack) {
      videoTrack.stop();
      stream.value.removeTrack(videoTrack);
    }
    cameraEnabled.value = false;
    videoEnabled.value = false;
  } else {
    // Turn camera ON
    try {
      const quality = qualityPresets[selectedQuality.value];
      const constraints = {
        video: {
          width: { ideal: quality.width },
          height: { ideal: quality.height },
          frameRate: { ideal: quality.frameRate },
          facingMode: facingMode.value
        },
        audio: false
      };
      
      const newStream = await navigator.mediaDevices.getUserMedia(constraints);
      const newVideoTrack = newStream.getVideoTracks()[0];
      
      stream.value.addTrack(newVideoTrack);
      
      if (myVideo.value) {
        myVideo.value.srcObject = stream.value;
      }
      
      cameraEnabled.value = true;
      videoEnabled.value = true;
    } catch (err) {
      console.error('Failed to turn on camera:', err);
      alert('Could not access camera. Please check permissions.');
    }
  }
};

// Turn Camera On/Off (Complete start/stop)
const toggleCamera = async () => {
  if (cameraEnabled.value) {
    // Turn camera OFF
    const videoTrack = stream.value?.getVideoTracks()[0];
    if (videoTrack) {
      videoTrack.stop();
      stream.value.removeTrack(videoTrack);
    }
    
    // If in a call, remove video track from peer connection
    if (connectionRef.value && connectionRef.value._pc) {
      const senders = connectionRef.value._pc.getSenders();
      const videoSender = senders.find(s => s.track && s.track.kind === 'video');
      if (videoSender) {
        connectionRef.value._pc.removeTrack(videoSender);
      }
    }
    
    cameraEnabled.value = false;
    videoEnabled.value = false;
  } else {
    // Turn camera ON
    try {
      const quality = qualityPresets[selectedQuality.value];
      const constraints = {
        video: {
          width: { ideal: quality.width },
          height: { ideal: quality.height },
          frameRate: { ideal: quality.frameRate },
          facingMode: facingMode.value
        },
        audio: false
      };
      
      const newStream = await navigator.mediaDevices.getUserMedia(constraints);
      const newVideoTrack = newStream.getVideoTracks()[0];
      
      stream.value.addTrack(newVideoTrack);
      
      if (myVideo.value) {
        myVideo.value.srcObject = stream.value;
      }
      
      // Add track to peer connection - FIXED for camera OFF at start
      if (connectionRef.value && connectionRef.value._pc) {
        const senders = connectionRef.value._pc.getSenders();
        const videoSender = senders.find(s => s.track && s.track.kind === 'video');
        
        if (videoSender) {
          // Replace existing video track
          await videoSender.replaceTrack(newVideoTrack);
          console.log('Video track replaced in peer connection');
        } else {
          // Add new video track if no sender exists (camera was OFF at start)
          connectionRef.value._pc.addTrack(newVideoTrack, stream.value);
          console.log('Video track added to peer connection');
        }
      }
      
      cameraEnabled.value = true;
      videoEnabled.value = true;
    } catch (err) {
      console.error('Failed to turn on camera:', err);
      alert('Could not access camera. Please check permissions.');
    }
  }
};

// Switch Camera (Front/Rear)
const switchCamera = async () => {
  if (!cameraEnabled.value) {
    alert('Please turn on the camera first!');
    return;
  }
  
  // Toggle facing mode
  facingMode.value = facingMode.value === 'user' ? 'environment' : 'user';
  
  try {
    // Stop current video track
    const oldVideoTrack = stream.value.getVideoTracks()[0];
    if (oldVideoTrack) {
      oldVideoTrack.stop();
      stream.value.removeTrack(oldVideoTrack);
    }
    
    // Get new stream with opposite camera
    const quality = qualityPresets[selectedQuality.value];
    const constraints = {
      video: {
        width: { ideal: quality.width },
        height: { ideal: quality.height },
        frameRate: { ideal: quality.frameRate },
        facingMode: facingMode.value
      },
      audio: false
    };
    
    const newStream = await navigator.mediaDevices.getUserMedia(constraints);
    const newVideoTrack = newStream.getVideoTracks()[0];
    
    stream.value.addTrack(newVideoTrack);
    
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
    
    console.log(`Camera switched to ${facingMode.value === 'user' ? 'front' : 'rear'}`);
  } catch (err) {
    console.error('Failed to switch camera:', err);
    alert('Could not switch camera. Your device may not have multiple cameras.');
    // Revert facing mode if failed
    facingMode.value = facingMode.value === 'user' ? 'environment' : 'user';
  }
};

// Enumerate available cameras
const enumerateCameras = async () => {
  try {
    const devices = await navigator.mediaDevices.enumerateDevices();
    availableCameras.value = devices.filter(device => device.kind === 'videoinput');
    console.log(`Found ${availableCameras.value.length} cameras`);
  } catch (err) {
    console.error('Failed to enumerate cameras:', err);
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

// 4. Auto-hide Controls
const handleScreenTap = () => {
  showControls.value = true;
  resetControlsTimer();
};

const resetControlsTimer = () => {
  if (controlsTimeout) {
    clearTimeout(controlsTimeout);
  }
  
  // Auto-hide after 3 seconds
  controlsTimeout = setTimeout(() => {
    if (!showSettings.value) {
      showControls.value = false;
    }
  }, 3000);
};

// 5. Call Duration Timer
const startCallTimer = () => {
  callStartTime.value = Date.now();
  
  const updateDuration = () => {
    if (!callAccepted.value || callEnded.value) return;
    
    const elapsed = Math.floor((Date.now() - callStartTime.value) / 1000);
    const minutes = Math.floor(elapsed / 60).toString().padStart(2, '0');
    const seconds = (elapsed % 60).toString().padStart(2, '0');
    callDuration.value = `${minutes}:${seconds}`;
    
    requestAnimationFrame(updateDuration);
  };
  
  updateDuration();
};

// 6. Change Video Quality
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
  /* Support for iPhone X notch and other safe areas */
  padding-left: env(safe-area-inset-left);
  padding-right: env(safe-area-inset-right);
}

/* ==================== LOBBY SCREEN ==================== */
.lobby-container {
  min-height: 100vh;
  min-height: 100dvh; /* Dynamic viewport height for mobile */
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  overflow-y: auto; /* Allow scrolling on mobile */
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
  margin: auto; /* Center vertically */
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
  margin-bottom: 30px;
}

.video-preview-container {
  position: relative;
  width: 100%;
  max-width: 400px;
  margin: 0 auto 30px;
  border-radius: 16px;
  overflow: hidden;
  background: #1a1a1a;
  box-shadow: 0 10px 30px rgba(0,0,0,0.2);
}

.video-preview {
  width: 100%;
  height: 300px;
  object-fit: cover;
  display: block;
}

.camera-off-message {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: #1a1a1a;
  color: white;
}

.camera-icon {
  font-size: 48px;
  margin-bottom: 10px;
}

.camera-off-message p {
  font-size: 16px;
  color: #a0aec0;
}

/* Lobby Camera Toggle Button */
.lobby-camera-toggle {
  position: absolute;
  bottom: 15px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 50px;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  z-index: 10;
}

.lobby-camera-toggle .icon {
  width: 20px;
  height: 20px;
  stroke-width: 2.5;
}

.lobby-camera-toggle:hover {
  background: rgba(0, 0, 0, 0.95);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateX(-50%) translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
}

.lobby-camera-toggle:active {
  transform: translateX(-50%) scale(0.95);
}

.toggle-label {
  user-select: none;
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
  background: rgba(102, 126, 234, 0.08);
  padding: 25px;
  border-radius: 16px;
  border: 2px solid rgba(102, 126, 234, 0.2);
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
  position: relative;
}

.incoming-animation::before {
  content: '📞';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 36px;
}

@keyframes ring {
  0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.7); }
  50% { transform: scale(1.05); box-shadow: 0 0 0 10px rgba(102, 126, 234, 0); }
  100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(102, 126, 234, 0); }
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
  -webkit-tap-highlight-color: transparent;
  touch-action: none;
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
  top: 80px;
  right: 20px;
  width: 120px;
  height: 160px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0,0,0,0.7);
  z-index: 10;
  border: 3px solid rgba(255,255,255,0.3);
  transition: all 0.3s;
  background: #1a1a1a;
}

.pip-video-container:active {
  transform: scale(0.95);
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
  background: rgba(0,0,0,0.8);
  color: white;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

/* Top Bar */
.top-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 20px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(to bottom, rgba(0,0,0,0.8) 0%, transparent 100%);
  z-index: 5;
}

.call-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.call-status-dot {
  width: 8px;
  height: 8px;
  background: #48bb78;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.call-duration {
  color: white;
  font-size: 15px;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5);
}

.settings-btn-top {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  -webkit-tap-highlight-color: transparent;
  touch-action: manipulation;
}

.settings-btn-top .icon {
  width: 20px;
  height: 20px;
  stroke-width: 2;
}

.settings-btn-top:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: scale(1.05);
}

.settings-btn-top:active {
  transform: scale(0.95);
}

/* Bottom Control Bar */
.bottom-bar {
  position: absolute;
  bottom: 110px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  z-index: 5;
}

.controls-group {
  display: flex;
  gap: 16px;
  align-items: center;
  background: rgba(30, 30, 30, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 12px 20px;
  border-radius: 50px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.control-btn-modern {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: white;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  -webkit-tap-highlight-color: transparent;
  touch-action: manipulation;
  position: relative;
}

.control-btn-modern .icon {
  width: 24px;
  height: 24px;
  stroke-width: 2.5;
}

.control-btn-modern:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-2px);
}

.control-btn-modern:active {
  transform: scale(0.95);
}

.control-btn-modern.btn-disabled {
  background: rgba(220, 38, 38, 0.9);
}

.control-btn-modern.btn-disabled:hover {
  background: rgba(220, 38, 38, 1);
}

/* Hang Up Button (Red) */
.hang-up-red {
  background: rgba(220, 38, 38, 0.95) !important;
  width: 56px !important;
  height: 56px !important;
}

.hang-up-red:hover {
  background: rgba(220, 38, 38, 1) !important;
  box-shadow: 0 4px 16px rgba(220, 38, 38, 0.5);
}

.hang-up-red .icon {
  transform: rotate(135deg);
}

/* Hang Up Button (Always Visible) */
.hang-up-container-always {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
}

.hang-up-btn-always {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: none;
  background: rgba(220, 38, 38, 0.95);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(220, 38, 38, 0.6);
  transition: all 0.2s ease;
  -webkit-tap-highlight-color: transparent;
  touch-action: manipulation;
  animation: pulse-hang-up 2.5s ease-in-out infinite;
}

.hang-up-btn-always .icon {
  width: 28px;
  height: 28px;
  transform: rotate(135deg);
}

.hang-up-btn-always:hover {
  background: rgba(220, 38, 38, 1);
  transform: translateX(-50%) scale(1.05);
  box-shadow: 0 10px 32px rgba(220, 38, 38, 0.8);
}

.hang-up-btn-always:active {
  transform: translateX(-50%) scale(0.95);
}

@keyframes pulse-hang-up {
  0%, 100% {
    box-shadow: 0 8px 24px rgba(220, 38, 38, 0.6);
  }
  50% {
    box-shadow: 0 10px 36px rgba(220, 38, 38, 0.9);
  }
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

.camera-info {
  background: #f7fafc;
  padding: 15px;
  border-radius: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-text {
  font-size: 14px;
  color: #4a5568;
  display: flex;
  justify-content: space-between;
}

.info-text strong {
  color: #2d3748;
  font-weight: 600;
}

/* Slide Up Animation */
.slide-up-enter-active, .slide-up-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-up-enter-from, .slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

/* Fade Animation */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

/* Mobile Responsive */
@media (max-width: 768px) {
  .lobby-container {
    padding: 15px;
    align-items: flex-start;
    padding-top: 40px;
  }

  .lobby-card {
    padding: 40px 25px;
    border-radius: 20px;
    max-height: calc(100vh - 80px);
    max-height: calc(100dvh - 80px);
    overflow-y: auto;
  }

  .lobby-title {
    font-size: 26px;
    margin-bottom: 8px;
  }

  .lobby-subtitle {
    font-size: 14px;
    margin-bottom: 20px;
  }

  .video-preview-container {
    max-width: 100%;
    margin-bottom: 20px;
  }

  .video-preview {
    height: 200px;
  }

  .lobby-camera-toggle {
    padding: 10px 16px;
    font-size: 13px;
    bottom: 12px;
  }

  .lobby-camera-toggle .icon {
    width: 18px;
    height: 18px;
  }

  .incoming-call {
    margin-top: 20px;
    padding: 20px;
    background: rgba(102, 126, 234, 0.05);
    border-radius: 16px;
  }

  .incoming-animation {
    width: 60px;
    height: 60px;
    margin: 0 auto 15px;
  }

  .incoming-text {
    font-size: 18px;
    margin-bottom: 15px;
  }

  .answer-btn {
    width: 100%;
    padding: 14px 20px;
    font-size: 16px;
  }

  .pip-video-container {
    width: 100px;
    height: 140px;
    top: 70px;
    right: 15px;
    border-width: 2px;
    border-radius: 12px;
  }

  .control-btn-modern {
    width: 48px;
    height: 48px;
  }

  .control-btn-modern .icon {
    width: 22px;
    height: 22px;
  }

  .hang-up-red {
    width: 52px !important;
    height: 52px !important;
  }

  .hang-up-btn-always {
    width: 60px;
    height: 60px;
    bottom: 25px;
  }

  .hang-up-btn-always .icon {
    width: 26px;
    height: 26px;
  }

  .controls-group {
    gap: 12px;
    padding: 10px 16px;
  }

  .bottom-bar {
    bottom: 100px;
  }

  .settings-panel {
    padding: 25px 20px;
  }
}

@media (max-width: 480px) {
  .lobby-container {
    padding: 10px;
    padding-top: 30px;
  }

  .lobby-card {
    padding: 30px 20px;
    border-radius: 16px;
    max-height: calc(100vh - 60px);
    max-height: calc(100dvh - 60px);
  }

  .lobby-title {
    font-size: 22px;
  }

  .lobby-subtitle {
    font-size: 13px;
    margin-bottom: 15px;
  }

  .video-preview {
    height: 180px;
  }

  .video-preview-container {
    margin-bottom: 15px;
  }

  .lobby-camera-toggle {
    padding: 8px 14px;
    font-size: 12px;
    bottom: 10px;
  }

  .lobby-camera-toggle .icon {
    width: 16px;
    height: 16px;
  }

  .lobby-input {
    padding: 14px 16px;
    font-size: 15px;
    margin-bottom: 12px;
  }

  .lobby-btn {
    padding: 14px;
    font-size: 15px;
  }

  .status-box {
    padding: 15px;
    margin: 15px 0;
  }

  .status-text {
    font-size: 13px;
  }

  .friend-joined {
    font-size: 14px;
  }

  .start-call-btn {
    padding: 16px;
    font-size: 16px;
  }

  .incoming-call {
    margin-top: 15px;
    padding: 15px;
  }

  .incoming-animation {
    width: 50px;
    height: 50px;
    margin-bottom: 12px;
  }

  .incoming-text {
    font-size: 16px;
    margin-bottom: 12px;
  }

  .answer-btn {
    padding: 12px 20px;
    font-size: 15px;
  }
  
  .pip-video-container {
    width: 90px;
    height: 120px;
    top: 60px;
    right: 12px;
  }
  
  .control-btn-modern {
    width: 44px;
    height: 44px;
  }
  
  .control-btn-modern .icon {
    width: 20px;
    height: 20px;
  }

  .hang-up-red {
    width: 48px !important;
    height: 48px !important;
  }

  .hang-up-btn-always {
    width: 56px;
    height: 56px;
    bottom: 20px;
  }

  .hang-up-btn-always .icon {
    width: 24px;
    height: 24px;
  }
  
  .controls-group {
    gap: 10px;
    padding: 8px 14px;
  }
  
  .bottom-bar {
    bottom: 95px;
  }
  
  .call-duration {
    font-size: 14px;
  }
  
  .pip-label {
    font-size: 11px;
    padding: 3px 8px;
  }
}

/* Landscape mode adjustments */
@media (max-width: 768px) and (orientation: landscape) {
  .lobby-container {
    padding: 10px;
  }

  .lobby-card {
    padding: 20px;
    max-height: calc(100vh - 40px);
    max-height: calc(100dvh - 40px);
    display: flex;
    flex-direction: column;
  }

  .lobby-title {
    font-size: 20px;
    margin-bottom: 6px;
  }

  .lobby-subtitle {
    font-size: 12px;
    margin-bottom: 12px;
  }

  .video-preview {
    height: 150px;
  }

  .video-preview-container {
    margin-bottom: 12px;
  }

  .lobby-form {
    margin-bottom: 15px;
  }

  .status-box {
    padding: 10px;
    margin: 10px 0;
  }

  .incoming-call {
    padding: 12px;
    margin-top: 12px;
  }

  .pip-video-container {
    width: 80px;
    height: 100px;
    top: 15px;
    right: 15px;
  }
  
  .bottom-bar {
    bottom: 85px;
  }

  .hang-up-btn-always {
    width: 52px;
    height: 52px;
    bottom: 15px;
  }

  .hang-up-btn-always .icon {
    width: 22px;
    height: 22px;
  }
  
  .control-btn-modern {
    width: 44px;
    height: 44px;
  }
  
  .control-btn-modern .icon {
    width: 20px;
    height: 20px;
  }

  .hang-up-red {
    width: 48px !important;
    height: 48px !important;
  }

  .controls-group {
    padding: 8px 14px;
  }
}

/* Hide default elements (for compatibility) */
.hidden {
  display: none !important;
}
</style>
