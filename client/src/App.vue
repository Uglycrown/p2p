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
        <div class="room-input-container">
          <input
            v-model="roomId"
            placeholder="Enter Room Name (6+ characters)"
            class="lobby-input"
            @keyup.enter="joinRoom"
          />
          <button 
            v-if="roomId && roomId.length >= 6" 
            @click="copyRoomName" 
            class="copy-btn"
            :title="copySuccess ? 'Copied!' : 'Copy Room Name'"
          >
            <svg v-if="!copySuccess" class="copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
            </svg>
            <svg v-else class="copy-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"/>
            </svg>
          </button>
        </div>

        <!-- Optional Password -->
        <div class="password-section">
          <label class="password-label">
            <input
              type="checkbox"
              v-model="isCreatingRoom"
              class="password-checkbox"
            />
            <span>🔒 Create with password (optional)</span>
          </label>

          <input
            v-if="isCreatingRoom"
            v-model="roomPassword"
            type="password"
            placeholder="Enter password (8+ characters)"
            class="lobby-input password-input"
          />
        </div>

        <button @click="generateSecureRoomName" class="lobby-btn secondary-btn">
          🔒 Generate Secure Room
        </button>
        <button @click="joinRoom" class="lobby-btn primary-btn" :disabled="isJoining">
          <span v-if="isJoining">⏳ Connecting...</span>
          <span v-else-if="!myId">{{ isCreatingRoom ? 'Create Room' : 'Join Room' }}</span>
          <span v-else>Joined ✓</span>
        </button>

        <p v-if="joinError" class="error-message">{{ joinError }}</p>
      </div>

      <div v-if="myId" class="status-box">
        <div class="status-indicator"></div>
        <p class="status-text">Connected • Waiting for friend...</p>
        <p v-if="userJoined" class="friend-joined">✨ Friend is here! Connecting...</p>
        <p v-if="callAccepted" class="friend-joined">🎥 Call in progress!</p>
      </div>
    </div>
  </div>

  <!-- Password Modal -->
  <div v-if="showPasswordModal" class="modal-overlay" @click.self="showPasswordModal = false">
    <div class="modal-content">
      <h2>🔒 Room Password Required</h2>
      <p>This room is password protected. Enter the password to join.</p>

      <input
        v-model="roomPassword"
        type="password"
        placeholder="Enter room password"
        class="modal-input"
        @keyup.enter="joinWithPassword"
      />

      <div class="modal-buttons">
        <button @click="joinWithPassword" class="modal-btn primary">
          Join Room
        </button>
        <button @click="showPasswordModal = false; roomPassword = ''" class="modal-btn secondary">
          Cancel
        </button>
      </div>
    </div>
  </div>

  <!-- Fullscreen Video Call (During Call) -->
  <div
    v-if="callAccepted && !callEnded"
    class="fullscreen-call"
    @click="handleScreenTap"
    @mousemove="handleScreenTap"
    @touchstart="handleScreenTap"
  >
    <!-- Main Video (Friend's video - Large) -->
    <video
      ref="userVideo"
      autoplay
      playsinline
      :class="['main-video', isVideoSwapped && 'swapped']"
      @dblclick="swapVideos"
    ></video>

    <!-- Small Self Video (Picture-in-Picture) -->
    <div 
      class="pip-video-container" 
      :style="pipStyle"
      @dblclick="swapVideos"
      @mousedown="startDrag"
      @touchstart="startDragTouch"
    >
      <video
        ref="myVideo"
        muted
        autoplay
        playsinline
        :class="['pip-video', isVideoSwapped && 'swapped']"
      ></video>
      <span class="pip-label">{{ isVideoSwapped ? 'Friend' : 'You' }}</span>
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
          <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="3"/>
            <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 ația 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
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

          <!-- Screen Share Toggle -->
          <button
            @click.stop="toggleScreenShare"
            :class="['control-btn-modern', isScreenSharing && 'btn-active']"
            title="Share Screen"
          >
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
              <line x1="8" y1="21" x2="16" y2="21"/>
              <line x1="12" y1="17" x2="12" y2="21"/>
              <polyline v-if="isScreenSharing" points="10 8 12 10 15 7" stroke-width="3"/>
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

          <!-- Camera Selector (Multi-camera devices) -->
          <button
            v-if="availableCameras.length > 2 && cameraEnabled"
            @click.stop="showCameraSelector = !showCameraSelector"
            class="control-btn-modern"
            title="Select Camera"
          >
            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="3"/>
              <path d="M12 1v6m0 6v6M1 12h6m6 0h6"/>
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

          <!-- Hang Up Button -->
          <button
            @click.stop="leaveCall"
            class="control-btn-modern hang-up-red"
            title="End Call"
          >
            <svg class="icon" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.56.9-.98.49-1.87 1.12-2.66 1.85-.18.18-.43.28-.7.28-.28 0-.53-.11-.71-.29L.29 13.08c-.18-.17-.29-.42-.29-.7 0-.28.11-.53.29-.71C3.34 8.78 7.46 7 12 7s8.66 1.78 11.71 4.67c.18.18.29.43.29.71 0 .28-.11.53-.29.71l-2.48 2.48c-.18.18-.43.29-.71.29-.27 0-.52-.11-.7-.28-.79-.74-1.69-1.36-2.67-1.85-.33-.16-.56-.5-.56-.9v-3.1C15.15 9.25 13.6 9 12 9z"/>
            </svg>
          </button>
        </div>
      </div>
    </transition>

    <!-- Camera Selector Popup -->
    <transition name="fade">
      <div v-if="showCameraSelector" class="camera-selector-overlay" @click.self="showCameraSelector = false">
        <div class="camera-selector-panel">
          <h3>Select Camera</h3>
          <div class="camera-list">
            <button
              v-for="camera in availableCameras"
              :key="camera.deviceId"
              @click="selectSpecificCamera(camera.deviceId)"
              :class="['camera-option', selectedCameraId === camera.deviceId && 'active']"
            >
              <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M23 7l-7 5 7 5V7z"/>
                <rect x="1" y="5" width="15" height="14" rx="2" ry="2"/>
              </svg>
              <span>{{ getCameraLabel(camera) }}</span>
            </button>
          </div>
          <button @click="showCameraSelector = false" class="close-camera-btn">Close</button>
        </div>
      </div>
    </transition>

    <!-- Settings Panel (Slide up) -->
    <transition name="slide-up">
      <div v-if="showSettings" class="settings-panel">
        <!-- Drag Handle -->
        <div class="settings-handle"></div>

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
              <option value="low">Low (360p) - Save Battery 🔋</option>
              <option value="medium">Medium (480p) - Balanced 🔋</option>
              <option value="high">High (720p) - Clear & Efficient ✨ (Recommended)</option>
              <option value="hd">Full HD (1080p) - Very Clear 🎥</option>
              <option value="ultra">Ultra HD (1080p@30fps) - Maximum Quality 🌟</option>
            </select>
            <p class="quality-hint">720p (High) gives excellent clarity with good battery life</p>
          </div>

          <div class="setting-item">
            <label>🎵 Audio Quality</label>
            <select v-model="selectedAudioQuality" @change="changeAudioQuality" class="settings-select">
              <option value="voice">Voice (16kHz) - Save Battery 🔋</option>
              <option value="music">Music (44.1kHz) - Clear Audio ✨ (Recommended)</option>
              <option value="studio">Studio (48kHz) - Best Quality 🌟</option>
            </select>
            <p class="quality-hint">Music quality provides crystal clear audio for video calls</p>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, watch, nextTick, computed } from 'vue';
import io from 'socket.io-client';
import SimplePeer from 'simple-peer';
import { E2EEncryption } from './encryption.js';

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

// Loading state
const isJoining = ref(false);
const joinError = ref('');

// Video quality control - Default to 'high' for clear video
const selectedQuality = ref('high');

// Audio quality control - Default to 'music' for better clarity
const selectedAudioQuality = ref('music');

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
const selectedCameraId = ref(null);
const showCameraSelector = ref(false);

// Refs for HTML elements
const myVideo = ref(null);
const userVideo = ref(null);
const connectionRef = ref(null);

// Phase 2: Authentication & Encryption
const authToken = ref(null);
const roomPassword = ref('');
const showPasswordModal = ref(false);
const roomHasPassword = ref(false);
const isCreatingRoom = ref(false);
const e2eEncryption = ref(null);

// Phase 3: Reconnection & Advanced Features
const isReconnecting = ref(false);
const reconnectAttempts = ref(0);
const maxReconnectAttempts = 5;
const connectionStatus = ref('disconnected'); // 'connected', 'connecting', 'disconnected'
const isScreenSharing = ref(false);
const screenStream = ref(null);

// Copy room name functionality
const copySuccess = ref(false);

// Video swap functionality
const isVideoSwapped = ref(false);

// PiP video dragging functionality
const pipPosition = ref({ x: 20, y: 80 }); // Default position (right: 20px, top: 80px)
const isDragging = ref(false);
const dragStart = ref({ x: 0, y: 0 });
const dragOffset = ref({ x: 0, y: 0 });

// Quality presets with bitrate - Optimized for BOTH quality AND battery
const qualityPresets = {
  low: { width: 640, height: 360, frameRate: 15, bitrate: 300000 }, // 300 Kbps
  medium: { width: 854, height: 480, frameRate: 24, bitrate: 800000 }, // 800 Kbps
  high: { width: 1280, height: 720, frameRate: 30, bitrate: 2000000 }, // 2 Mbps - Clear & efficient
  hd: { width: 1920, height: 1080, frameRate: 30, bitrate: 3500000 }, // 3.5 Mbps - Very clear
  ultra: { width: 1920, height: 1080, frameRate: 30, bitrate: 5000000 } // 5 Mbps - Maximum quality
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
  // Detect device type and set quality accordingly
  const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  if (isMobile) {
    selectedQuality.value = 'high';
    selectedAudioQuality.value = 'music';
    // Set initial PiP position for mobile
    pipPosition.value = { x: 15, y: 70 };
  } else {
    selectedQuality.value = 'hd';
    selectedAudioQuality.value = 'studio';
    // Set initial PiP position for desktop
    pipPosition.value = { x: 20, y: 80 };
  }

  // Get available cameras
  await enumerateCameras();

  // Get Camera/Mic Permissions - Start with camera ON
  try {
    const quality = qualityPresets[selectedQuality.value];
    const audioQuality = audioQualityPresets[selectedAudioQuality.value];
    const constraints = {
      video: {
        width: { ideal: quality.width },
        height: { ideal: quality.height },
        frameRate: { ideal: quality.frameRate },
        facingMode: facingMode.value
      },
      audio: {
        echoCancellation: audioQuality.echoCancellation,
        noiseSuppression: audioQuality.noiseSuppression,
        autoGainControl: audioQuality.autoGainControl,
        sampleRate: audioQuality.sampleRate,
        channelCount: audioQuality.channelCount
      }
    };
    const currentStream = await navigator.mediaDevices.getUserMedia(constraints);
    stream.value = currentStream;
    if (myVideo.value) myVideo.value.srcObject = currentStream;
    cameraEnabled.value = true;
    videoEnabled.value = true;
  } catch (err) {
    alert("Please allow camera and microphone access!");
  }

  // Connect to Signaling Server
  // IMPORTANT: Replace with your Render server URL after deploying
  const serverUrl = import.meta.env.VITE_SERVER_URL || 'http://localhost:5000';
  socket.value = io(serverUrl, {
    auth: {
      token: authToken.value // Send JWT token if available
    }
  });

  // Setup socket listeners
  setupSocketListeners();
});

// Copy room name to clipboard
const copyRoomName = async () => {
  if (!roomId.value) return;
  
  try {
    await navigator.clipboard.writeText(roomId.value);
    copySuccess.value = true;
    
    // Reset after 2 seconds
    setTimeout(() => {
      copySuccess.value = false;
    }, 2000);
  } catch (err) {
    // Fallback for older browsers
    const textArea = document.createElement('textarea');
    textArea.value = roomId.value;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    document.body.appendChild(textArea);
    textArea.select();
    try {
      document.execCommand('copy');
      copySuccess.value = true;
      setTimeout(() => {
        copySuccess.value = false;
      }, 2000);
    } catch (e) {
      alert('Failed to copy room name');
    }
    document.body.removeChild(textArea);
  }
};

// Security: Generate secure room name
const generateSecureRoomName = () => {
  // Generate cryptographically secure random room name
  const array = new Uint8Array(12);
  crypto.getRandomValues(array);

  // Convert to base36 (0-9, a-z) and format nicely
  const roomName = Array.from(array, byte =>
    byte.toString(36).padStart(2, '0')
  ).join('').substring(0, 16);

  // Format with dashes for readability
  const formatted = roomName.match(/.{1,4}/g).join('-');
  roomId.value = formatted;
};

// Security: Validate room name before joining
const validateRoomName = (name) => {
  if (!name || name.length < 6) {
    return "Room name must be at least 6 characters";
  }

  if (!/^[a-zA-Z0-9_-]+$/.test(name)) {
    return "Room name can only contain letters, numbers, dashes, and underscores";
  }

  return null;
};

// 2. Logic Functions
const joinRoom = async () => {
  if(!roomId.value) return alert("Enter a room name!");

  // Security: Validate room name
  const validationError = validateRoomName(roomId.value);
  if (validationError) {
    alert(validationError);
    return;
  }

  isJoining.value = true;
  joinError.value = '';

  try {
    // Phase 2: Check if room requires password or create with password
    const serverUrl = import.meta.env.VITE_SERVER_URL || 'http://localhost:5000';

    // Check room info
    const roomInfoResponse = await fetch(`${serverUrl}/api/room-info/${roomId.value}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }
    });

    if (!roomInfoResponse.ok) {
      const errorText = await roomInfoResponse.text();
      alert(`Server error: ${roomInfoResponse.status} - ${errorText}`);
      return;
    }

    const roomInfo = await roomInfoResponse.json();

    if (roomInfo.isFull) {
      alert("Room is full! Only 2 people allowed.");
      return;
    }

    // If room has password and we're joining (not creating)
    if (roomInfo.hasPassword && !isCreatingRoom.value) {
      roomHasPassword.value = true;
      showPasswordModal.value = true;
      return; // Wait for password input
    }

    // If creating room with password or joining without password
    const endpoint = isCreatingRoom.value ? '/api/generate-room-token' : '/api/generate-room-token';
    const body = {
      roomID: roomId.value,
      password: roomPassword.value || undefined
    };


    const response = await fetch(`${serverUrl}${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(body)
    });


    if (!response.ok) {
      const error = await response.json();
        alert(error.error || 'Failed to join room');
      return;
    }

    const data = await response.json();
    authToken.value = data.token;

    // Initialize E2E encryption
    e2eEncryption.value = new E2EEncryption(roomId.value, roomPassword.value);


    // Reconnect socket with new token
    if (socket.value) {
      socket.value.disconnect();
    }

    socket.value = io(serverUrl, {
      auth: { token: authToken.value },
      transports: ['websocket', 'polling'],
      reconnection: true,
      reconnectionAttempts: 5,
      reconnectionDelay: 1000
    });

    // Re-setup socket listeners
    setupSocketListeners();

    // Join the room
    socket.value.emit('joinRoom', roomId.value);


  } catch (error) {
    joinError.value = `Failed: ${error.message}`;
    alert(`Failed to join room: ${error.message}. Check console for details.`);
  } finally {
    isJoining.value = false;
  }
};

// Setup socket event listeners
const setupSocketListeners = () => {
  socket.value.on('me', (id) => myId.value = id);

  socket.value.on('userJoined', (id) => {
    userJoined.value = true;
    callerId.value = id;

    // Auto-start call immediately when friend joins
    setTimeout(() => {
      if (!callAccepted.value && !isCalling.value) {
        callUser();
      }
    }, 500);
  });

  socket.value.on('roomFull', () => {
    alert("Room is full! Only 2 people allowed.");
  });

  socket.value.on('error', (data) => {
    alert(data.message || "An error occurred");
  });

  socket.value.on('callUser', (data) => {
    // Phase 2: Decrypt signal if E2E encryption is enabled
    try {
      const signal = e2eEncryption.value
        ? e2eEncryption.value.decryptSignal(data.signal)
        : data.signal;

      // If we haven't started the call yet, save caller info and signal
      if (!incomingCall.value) {
        incomingCall.value = true;
        callerSignal.value = signal;
        callerId.value = data.from;

        // Auto-answer call immediately
            setTimeout(() => {
          if (!callAccepted.value) {
            answerCall();
          }
        }, 100);
      } else if (connectionRef.value && signal.candidate) {
        // Handle trickled ICE candidates
            connectionRef.value.signal(signal);
      }
    } catch (error) {
        alert('Failed to establish secure connection');
    }
  });

  socket.value.on('callEnded', () => {
      endCallCleanup();
  });
};

// Join room with password
const joinWithPassword = async () => {
  if (!roomPassword.value || roomPassword.value.length < 8) {
    alert('Password must be at least 8 characters');
    return;
  }

  try {
    const serverUrl = import.meta.env.VITE_SERVER_URL || 'http://localhost:5000';

    const response = await fetch(`${serverUrl}/api/verify-room-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        roomID: roomId.value,
        password: roomPassword.value
      })
    });

    if (!response.ok) {
      const error = await response.json();
      alert(error.error || 'Incorrect password');
      return;
    }

    const data = await response.json();
    authToken.value = data.token;

    // Initialize E2E encryption with password
    e2eEncryption.value = new E2EEncryption(roomId.value, roomPassword.value);

    // Reconnect with token
    if (socket.value) {
      socket.value.disconnect();
    }

    socket.value = io(serverUrl, {
      auth: { token: authToken.value }
    });

    setupSocketListeners();
    socket.value.emit('joinRoom', roomId.value);

    showPasswordModal.value = false;

  } catch (error) {
    alert('Failed to join room');
  }
};

const callUser = () => {
  isCalling.value = true;


  // Ensure we have media stream with ACTIVE tracks
  if (!stream.value) {
    alert('No camera/microphone access. Please restart the app and grant permissions.');
    isCalling.value = false;
    return;
  }

  const tracks = stream.value.getTracks();
  const activeTracks = tracks.filter(track => track.readyState === 'live');


  if (activeTracks.length === 0) {
    alert('Camera/Microphone not active. Please enable camera and try again.');
    isCalling.value = false;
    return;
  }

  // Log each track
  tracks.forEach(track => {
  });

  // IMPORTANT: Set myVideo srcObject immediately when call starts
  nextTick(() => {
    if (myVideo.value && stream.value) {
      myVideo.value.srcObject = stream.value;
      myVideo.value.play().catch(err => {});
    }
  });

  // Create peer with current stream (may or may not have video)
  const peer = new SimplePeer({
    initiator: true,
    trickle: true,
    stream: stream.value,
    config: {
      iceServers: [
        // STUN servers for NAT traversal
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' },
        { urls: 'stun:stun2.l.google.com:19302' },
        { urls: 'stun:stun3.l.google.com:19302' },
        { urls: 'stun:stun4.l.google.com:19302' },
        // TURN servers for difficult networks (relay)
        {
          urls: 'turn:openrelay.metered.ca:80',
          username: 'openrelayproject',
          credential: 'openrelayproject'
        },
        {
          urls: 'turn:openrelay.metered.ca:443',
          username: 'openrelayproject',
          credential: 'openrelayproject'
        },
        {
          urls: 'turn:openrelay.metered.ca:443?transport=tcp',
          username: 'openrelayproject',
          credential: 'openrelayproject'
        }
      ],
      iceCandidatePoolSize: 10,
      iceTransportPolicy: 'all',
      // Enable for dynamic track changes
      sdpSemantics: 'unified-plan'
    },
    offerOptions: {
      offerToReceiveAudio: true,
      offerToReceiveVideo: true
    },
    // Optimize SDP for better quality and battery
    sdpTransform: (sdp) => {
      sdp = preferVP9Codec(sdp); // VP9 = 30% better compression
      sdp = enableHardwareAcceleration(sdp); // Use GPU encoding
      return sdp;
    }
  });

  peer.on('signal', (data) => {
    // Phase 2: Encrypt signal if E2E encryption is enabled
    const signalData = e2eEncryption.value
      ? e2eEncryption.value.encryptSignal(data)
      : data;

    socket.value.emit('callUser', {
      userToCall: callerId.value,
      signalData: signalData,
      from: myId.value
    });
  });

  peer.on('stream', (userStream) => {

    // Log each track
    userStream.getTracks().forEach(track => {
      });

    userVideo.value.srcObject = userStream;
    // Ensure video plays
    userVideo.value.play().catch(err => {});

    // Listen for track changes (when remote peer adds/removes tracks)
    userStream.onaddtrack = (event) => {
        // Refresh video element
      if (userVideo.value) {
        userVideo.value.srcObject = userStream;
        userVideo.value.play().catch(err => {});
      }
    };

    userStream.onremovetrack = (event) => {
      };
  });

  // Apply bitrate constraints after connection
  peer.on('connect', () => {
    applyBitrateConstraints(peer);
  });

  // Monitor ICE connection state changes
  if (peer._pc) {
    peer._pc.oniceconnectionstatechange = () => {

      // If connection is failing, log why
      if (peer._pc.iceConnectionState === 'failed' || peer._pc.connectionState === 'failed') {
            peer._pc.getStats().then(stats => {
          stats.forEach(report => {
            if (report.type === 'candidate-pair' && report.state === 'succeeded') {
                      }
          });
        });
      }
    };

    peer._pc.onicegatheringstatechange = () => {
      };

    peer._pc.onconnectionstatechange = () => {
      };
  }

  // Handle errors
  peer.on('error', (err) => {

    // Log connection states for debugging
    if (peer._pc) {
      }

    // Don't alert on normal errors like renegotiation
    if (err.message && !err.message.includes('negotiation')) {
      }
  });

  // Handle connection close
  peer.on('close', () => {
    if (peer._pc) {
      }
  });

  socket.value.on('callAccepted', (signal) => {
    // Phase 2: Decrypt signal if E2E encryption is enabled
    try {
      const decryptedSignal = e2eEncryption.value
        ? e2eEncryption.value.decryptSignal(signal)
        : signal;

      // First callAccepted with offer/answer
      if (!callAccepted.value) {
        callAccepted.value = true;
        startCallTimer();
        peer.signal(decryptedSignal);
        resetControlsTimer();
      } else if (decryptedSignal.candidate && connectionRef.value) {
        // Handle trickled ICE candidates
            connectionRef.value.signal(decryptedSignal);
      }
    } catch (error) {
        alert('Failed to establish secure connection');
    }
  });

  connectionRef.value = peer;
};

const answerCall = () => {
  callAccepted.value = true;
  startCallTimer();


  // Ensure we have media stream with ACTIVE tracks
  if (!stream.value) {
    alert('No camera/microphone access. Please restart the app.');
    return;
  }

  const tracks = stream.value.getTracks();
  const activeTracks = tracks.filter(track => track.readyState === 'live');


  if (activeTracks.length === 0) {
    alert('Camera/Microphone not active. Please enable camera.');
    return;
  }

  // Log each track
  tracks.forEach(track => {
  });

  // IMPORTANT: Set myVideo srcObject immediately when call starts
  nextTick(() => {
    if (myVideo.value && stream.value) {
      myVideo.value.srcObject = stream.value;
      myVideo.value.play().catch(err => {});
    }
  });

  // Create peer with current stream (may or may not have video)
  const peer = new SimplePeer({
    initiator: false,
    trickle: true,
    stream: stream.value,
    config: {
      iceServers: [
        // STUN servers for NAT traversal
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' },
        { urls: 'stun:stun2.l.google.com:19302' },
        { urls: 'stun:stun3.l.google.com:19302' },
        { urls: 'stun:stun4.l.google.com:19302' },
        // TURN servers for difficult networks (relay)
        {
          urls: 'turn:openrelay.metered.ca:80',
          username: 'openrelayproject',
          credential: 'openrelayproject'
        },
        {
          urls: 'turn:openrelay.metered.ca:443',
          username: 'openrelayproject',
          credential: 'openrelayproject'
        },
        {
          urls: 'turn:openrelay.metered.ca:443?transport=tcp',
          username: 'openrelayproject',
          credential: 'openrelayproject'
        }
      ],
      iceCandidatePoolSize: 10,
      iceTransportPolicy: 'all',
      // Enable for dynamic track changes
      sdpSemantics: 'unified-plan'
    },
    answerOptions: {
      offerToReceiveAudio: true,
      offerToReceiveVideo: true
    },
    // Optimize SDP for better quality and battery
    sdpTransform: (sdp) => {
      sdp = preferVP9Codec(sdp); // VP9 = 30% better compression
      sdp = enableHardwareAcceleration(sdp); // Use GPU encoding
      return sdp;
    }
  });

  peer.on('signal', (data) => {
    // Phase 2: Encrypt signal if E2E encryption is enabled
    const signalData = e2eEncryption.value
      ? e2eEncryption.value.encryptSignal(data)
      : data;

    socket.value.emit('answerCall', { signal: signalData, to: callerId.value });
  });

  peer.on('stream', (userStream) => {

    // Log each track
    userStream.getTracks().forEach(track => {
      });

    userVideo.value.srcObject = userStream;
    // Ensure video plays
    userVideo.value.play().catch(err => {});

    // Listen for track changes (when remote peer adds/removes tracks)
    userStream.onaddtrack = (event) => {
        // Refresh video element
      if (userVideo.value) {
        userVideo.value.srcObject = userStream;
        userVideo.value.play().catch(err => {});
      }
    };

    userStream.onremovetrack = (event) => {
      };
  });

  // Apply bitrate constraints after connection
  peer.on('connect', () => {
    applyBitrateConstraints(peer);
  });

  // Monitor ICE connection state changes
  if (peer._pc) {
    peer._pc.oniceconnectionstatechange = () => {

      // If connection is failing, log why
      if (peer._pc.iceConnectionState === 'failed' || peer._pc.connectionState === 'failed') {
            peer._pc.getStats().then(stats => {
          stats.forEach(report => {
            if (report.type === 'candidate-pair' && report.state === 'succeeded') {
                      }
          });
        });
      }
    };

    peer._pc.onicegatheringstatechange = () => {
      };

    peer._pc.onconnectionstatechange = () => {
      };
  }

  // Handle errors
  peer.on('error', (err) => {

    // Log connection states for debugging
    if (peer._pc) {
      }

    // Don't alert on normal errors like renegotiation
    if (err.message && !err.message.includes('negotiation')) {
      }
  });

  // Handle connection close
  peer.on('close', () => {
    if (peer._pc) {
      }
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
        const videoSender = senders.find(s => s.track?.kind === 'video');

        if (videoSender) {
          // Replace existing video track
          await videoSender.replaceTrack(newVideoTrack);
              } else {
          // Add new video track if no sender exists (camera was OFF at start)
          connectionRef.value._pc.addTrack(newVideoTrack, stream.value);

          // Simple-peer will automatically renegotiate when track is added
          // The 'negotiationneeded' event is handled internally by simple-peer
              }
      }

      cameraEnabled.value = true;
      videoEnabled.value = true;
    } catch (err) {
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

  // Find next camera based on facing mode
  let targetCamera = null;
  const currentFacing = facingMode.value;

  // Toggle facing mode
  facingMode.value = currentFacing === 'user' ? 'environment' : 'user';

  // Try to find the main camera (not ultra-wide)
  if (facingMode.value === 'environment') {
    // Prefer main rear camera over ultra-wide
    // Labels usually contain keywords like "main", "back", or just "camera 0"
    const rearCameras = availableCameras.value.filter(cam => {
      const label = cam.label.toLowerCase();
      return !label.includes('front') && !label.includes('user') &&
             (label.includes('back') || label.includes('rear') || label.includes('environment'));
    });

    // Prefer cameras without "ultra" or "wide" in the name
    targetCamera = rearCameras.find(cam => {
      const label = cam.label.toLowerCase();
      return !label.includes('ultra') && !label.includes('wide');
    }) || rearCameras[0];
  } else {
    // Front camera
    targetCamera = availableCameras.value.find(cam => {
      const label = cam.label.toLowerCase();
      return label.includes('front') || label.includes('user');
    });
  }

  try {
    // Stop current video track
    const oldVideoTrack = stream.value.getVideoTracks()[0];
    if (oldVideoTrack) {
      oldVideoTrack.stop();
      stream.value.removeTrack(oldVideoTrack);
    }

    // Build constraints
    const quality = qualityPresets[selectedQuality.value];
    const constraints = {
      video: targetCamera ? {
        deviceId: { exact: targetCamera.deviceId },
        width: { ideal: quality.width },
        height: { ideal: quality.height },
        frameRate: { ideal: quality.frameRate }
      } : {
        width: { ideal: quality.width },
        height: { ideal: quality.height },
        frameRate: { ideal: quality.frameRate },
        facingMode: facingMode.value
      },
      audio: false
    };

    const newStream = await navigator.mediaDevices.getUserMedia(constraints);
    const newVideoTrack = newStream.getVideoTracks()[0];

    selectedCameraId.value = newVideoTrack.getSettings().deviceId;

    stream.value.addTrack(newVideoTrack);

    if (myVideo.value) {
      myVideo.value.srcObject = stream.value;
    }

    // Replace track in peer connection
    if (connectionRef.value && connectionRef.value._pc) {
      const sender = connectionRef.value._pc.getSenders().find(s => s.track?.kind === 'video');
      if (sender) {
        await sender.replaceTrack(newVideoTrack);
          }
    }
  } catch (err) {
    alert('Could not switch camera. Your device may not have multiple cameras.');
    // Revert facing mode if failed
    facingMode.value = currentFacing;
  }
};

// Select specific camera by deviceId
const selectSpecificCamera = async (deviceId) => {
  if (!cameraEnabled.value) {
    alert('Please turn on the camera first!');
    return;
  }

  try {
    // Stop current video track
    const oldVideoTrack = stream.value.getVideoTracks()[0];
    if (oldVideoTrack) {
      oldVideoTrack.stop();
      stream.value.removeTrack(oldVideoTrack);
    }

    // Get new stream with selected camera
    const quality = qualityPresets[selectedQuality.value];
    const constraints = {
      video: {
        deviceId: { exact: deviceId },
        width: { ideal: quality.width },
        height: { ideal: quality.height },
        frameRate: { ideal: quality.frameRate }
      },
      audio: false
    };

    const newStream = await navigator.mediaDevices.getUserMedia(constraints);
    const newVideoTrack = newStream.getVideoTracks()[0];

    selectedCameraId.value = deviceId;

    // Update facing mode based on camera label
    const camera = availableCameras.value.find(c => c.deviceId === deviceId);
    if (camera) {
      const label = camera.label.toLowerCase();
      facingMode.value = (label.includes('front') || label.includes('user')) ? 'user' : 'environment';
    }

    stream.value.addTrack(newVideoTrack);

    if (myVideo.value) {
      myVideo.value.srcObject = stream.value;
    }

    // Replace track in peer connection
    if (connectionRef.value && connectionRef.value._pc) {
      const sender = connectionRef.value._pc.getSenders().find(s => s.track?.kind === 'video');
      if (sender) {
        await sender.replaceTrack(newVideoTrack);
          }
    }

    showCameraSelector.value = false;
  } catch (err) {
    alert('Could not switch to selected camera.');
  }
};

// Get friendly camera label
const getCameraLabel = (camera) => {
  if (!camera.label || camera.label === '') {
    return `Camera ${availableCameras.value.indexOf(camera) + 1}`;
  }

  const label = camera.label;

  // Simplify common labels
  if (label.toLowerCase().includes('front')) return '📱 Front Camera';
  if (label.toLowerCase().includes('back') || label.toLowerCase().includes('rear')) {
    if (label.toLowerCase().includes('ultra') || label.toLowerCase().includes('wide')) {
      return '📷 Ultra Wide Camera';
    }
    if (label.toLowerCase().includes('tele') || label.toLowerCase().includes('zoom')) {
      return '🔭 Telephoto Camera';
    }
    return '📷 Main Camera';
  }

  return camera.label;
};

// Enumerate available cameras
const enumerateCameras = async () => {
  try {
    const devices = await navigator.mediaDevices.enumerateDevices();
    availableCameras.value = devices.filter(device => device.kind === 'videoinput');
  } catch (err) {
  }
};

// Watch userVideo srcObject and ensure it plays
watch(() => userVideo.value?.srcObject, async (newSrcObject) => {
  if (newSrcObject && userVideo.value) {
    await nextTick();
    userVideo.value.play().catch(err => {
        // Try again after a short delay
      setTimeout(() => {
        userVideo.value?.play().catch(e => {});
      }, 100);
    });
  }
});

// Watch settings and camera selector - keep controls visible when they're open
watch([() => showSettings.value, () => showCameraSelector.value], ([settings, camera]) => {
  if (settings || camera) {
    showControls.value = true;
    if (controlsTimeout) {
      clearTimeout(controlsTimeout);
    }
  } else {
    // When closed, restart auto-hide timer
    resetControlsTimer();
  }
});

// Watch callAccepted - ensure myVideo is set when call starts
watch(() => callAccepted.value, async (accepted) => {
  if (accepted) {
    await nextTick();
    if (myVideo.value && stream.value) {
      myVideo.value.srcObject = stream.value;
      myVideo.value.play().catch(err => {
        // Retry after a short delay
        setTimeout(() => {
          if (myVideo.value && stream.value) {
            myVideo.value.srcObject = stream.value;
            myVideo.value.play().catch(e => {});
          }
        }, 100);
      });
    }
  }
});

// Force VP9 codec for better compression (30-50% bandwidth savings)
const preferVP9Codec = (sdp) => {

  // Prioritize VP9 codec
  sdp = sdp.replace(/m=video (\d+) RTP\/SAVPF (.+)\r\n/g, (match, port, codecs) => {
    const codecArray = codecs.split(' ');

    // Find VP9 codecs
    const vp9Codecs = codecArray.filter(c => {
      const rtpmap = sdp.match(new RegExp(`a=rtpmap:${c} VP9`, 'i'));
      return rtpmap !== null;
    });

    // Other codecs as fallback
    const otherCodecs = codecArray.filter(c => {
      const rtpmap = sdp.match(new RegExp(`a=rtpmap:${c} VP9`, 'i'));
      return rtpmap === null;
    });

    // VP9 first, then fallbacks
    const reordered = [...vp9Codecs, ...otherCodecs].join(' ');

    return `m=video ${port} RTP/SAVPF ${reordered}\r\n`;
  });

  return sdp;
};

// Enable hardware acceleration for battery efficiency
const enableHardwareAcceleration = (sdp) => {

  // Add hardware acceleration hints to SDP
  // This tells the browser to use GPU encoding (much more battery efficient)
  if (sdp.includes('a=mid:video')) {
    sdp = sdp.replace(
      /a=mid:video\r\n/g,
      'a=mid:video\r\na=content:main\r\n'
    );
  }

  return sdp;
};

// Apply bitrate constraints for better quality with adaptive bitrate
const applyBitrateConstraints = async (peer) => {
  if (!peer || !peer._pc) return;

  try {
    const quality = qualityPresets[selectedQuality.value];
    const targetBitrate = quality.bitrate;


    const sender = peer._pc.getSenders().find(s => s.track?.kind === 'video');

    if (sender) {
      const parameters = sender.getParameters();

      if (!parameters.encodings || parameters.encodings.length === 0) {
        parameters.encodings = [{}];
      }

      // Adaptive bitrate: Set min, max, and target for smooth quality
      parameters.encodings[0].maxBitrate = targetBitrate;
      parameters.encodings[0].minBitrate = Math.floor(targetBitrate * 0.5); // 50% min for bad networks
      parameters.encodings[0].maxFramerate = quality.frameRate;

      // Enable adaptive bitrate scaling
      parameters.encodings[0].scaleResolutionDownBy = 1; // No downscaling unless needed
      parameters.encodings[0].scalabilityMode = 'L1T1'; // Temporal scalability

      // High priority for smooth video
      parameters.encodings[0].priority = 'high';
      parameters.encodings[0].networkPriority = 'high';

      await sender.setParameters(parameters);
    }
  } catch (err) {
  }
};

// Screen Share Toggle
const toggleScreenShare = async () => {
  if (!callAccepted.value) {
    alert('Please start a call first!');
    return;
  }

  if (!isScreenSharing.value) {
    // Start screen sharing
    try {
      const screenShareStream = await navigator.mediaDevices.getDisplayMedia({
        video: {
          cursor: 'always',
          displaySurface: 'monitor'
        },
        audio: false
      });

      screenStream.value = screenShareStream;
      isScreenSharing.value = true;

      // Replace video track in peer connection
      const screenTrack = screenShareStream.getVideoTracks()[0];
      const videoSender = connectionRef.value._pc
        .getSenders()
        .find(sender => sender.track?.kind === 'video');

      if (videoSender) {
        await videoSender.replaceTrack(screenTrack);
      }

      // Update local video
      myVideo.value.srcObject = screenShareStream;

      // Handle screen share stop (user clicks browser "Stop sharing" button)
      screenTrack.onended = () => {
        stopScreenShare();
      };

      } catch (err) {
        alert('Could not share screen. Please try again.');
    }
  } else {
    // Stop screen sharing
    stopScreenShare();
  }
};

const stopScreenShare = async () => {
  if (screenStream.value) {
    screenStream.value.getTracks().forEach(track => track.stop());
    screenStream.value = null;
  }

  isScreenSharing.value = false;

  // Switch back to camera if enabled
  if (cameraEnabled.value && stream.value) {
    const videoTrack = stream.value.getVideoTracks()[0];
    if (videoTrack) {
      const videoSender = connectionRef.value._pc
        .getSenders()
        .find(sender => sender.track?.kind === 'video');

      if (videoSender) {
        await videoSender.replaceTrack(videoTrack);
      }

      myVideo.value.srcObject = stream.value;
    }
  }

};

const leaveCall = () => {
    socket.value.emit('endCall', roomId.value);
    endCallCleanup();
};

const endCallCleanup = () => {
    callEnded.value = true;

    // Clear duration timer
    if (durationInterval) {
      clearInterval(durationInterval);
      durationInterval = null;
    }

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

  // Auto-hide after 5 seconds (longer for better UX)
  controlsTimeout = setTimeout(() => {
    if (!showSettings.value && !showCameraSelector.value) {
      showControls.value = false;
    }
  }, 5000);
};

// 5. Call Duration Timer - Optimized to reduce CPU usage
let durationInterval = null;

const startCallTimer = () => {
  callStartTime.value = Date.now();

  // Use setInterval instead of requestAnimationFrame (much more efficient)
  durationInterval = setInterval(() => {
    if (!callAccepted.value || callEnded.value) {
      if (durationInterval) {
        clearInterval(durationInterval);
        durationInterval = null;
      }
      return;
    }

    const elapsed = Math.floor((Date.now() - callStartTime.value) / 1000);
    const minutes = Math.floor(elapsed / 60).toString().padStart(2, '0');
    const seconds = (elapsed % 60).toString().padStart(2, '0');
    callDuration.value = `${minutes}:${seconds}`;
  }, 1000); // Update every second instead of every frame
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

        // Re-apply bitrate constraints with new quality settings
        await applyBitrateConstraints(connectionRef.value);
      }
    }

  } catch (err) {
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

  } catch (err) {
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

// Swap videos (WhatsApp-like feature)
const swapVideos = () => {
  isVideoSwapped.value = !isVideoSwapped.value;
  
  if (isVideoSwapped.value) {
    // Swap: Show my video on main, friend's on PiP
    if (myVideo.value && userVideo.value) {
      const myStream = myVideo.value.srcObject;
      const userStream = userVideo.value.srcObject;
      
      userVideo.value.srcObject = myStream;
      myVideo.value.srcObject = userStream;
      
      // Unmute the previously muted video when swapped
      if (userVideo.value.srcObject) {
        userVideo.value.muted = true; // My video should stay muted
      }
      if (myVideo.value.srcObject) {
        myVideo.value.muted = false; // Friend's video should be audible
      }
    }
  } else {
    // Restore: Friend on main, my video on PiP
    if (myVideo.value && userVideo.value) {
      const myStream = myVideo.value.srcObject;
      const userStream = userVideo.value.srcObject;
      
      userVideo.value.srcObject = userStream;
      myVideo.value.srcObject = myStream;
      
      // Restore original mute states
      if (myVideo.value.srcObject) {
        myVideo.value.muted = true; // My video should be muted
      }
      if (userVideo.value.srcObject) {
        userVideo.value.muted = false; // Friend's video should be audible
      }
    }
  }
};

// Computed style for PiP positioning
const pipStyle = computed(() => ({
  right: `${pipPosition.value.x}px`,
  top: `${pipPosition.value.y}px`,
  cursor: isDragging.value ? 'grabbing' : 'grab'
}));

// Start drag with mouse
const startDrag = (e) => {
  if (e.target.tagName === 'VIDEO') return; // Don't drag if clicking video directly
  
  isDragging.value = true;
  dragStart.value = {
    x: e.clientX,
    y: e.clientY
  };
  dragOffset.value = {
    x: pipPosition.value.x,
    y: pipPosition.value.y
  };
  
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
  e.preventDefault();
};

// Start drag with touch
const startDragTouch = (e) => {
  if (e.touches.length !== 1) return;
  
  const touch = e.touches[0];
  isDragging.value = true;
  dragStart.value = {
    x: touch.clientX,
    y: touch.clientY
  };
  dragOffset.value = {
    x: pipPosition.value.x,
    y: pipPosition.value.y
  };
  
  document.addEventListener('touchmove', onDragTouch, { passive: false });
  document.addEventListener('touchend', stopDragTouch);
  e.preventDefault();
};

// Handle drag movement
const onDrag = (e) => {
  if (!isDragging.value) return;
  
  const deltaX = dragStart.value.x - e.clientX;
  const deltaY = e.clientY - dragStart.value.y;
  
  const newX = dragOffset.value.x + deltaX;
  const newY = dragOffset.value.y + deltaY;
  
  // Get screen dimensions
  const screenWidth = window.innerWidth;
  const screenHeight = window.innerHeight;
  
  // PiP dimensions (adjust based on screen size)
  const pipWidth = 120;
  const pipHeight = 160;
  
  // Constrain to screen boundaries
  const minX = 10;
  const maxX = screenWidth - pipWidth - 10;
  const minY = 60; // Below top bar
  const maxY = screenHeight - pipHeight - 100; // Above bottom controls
  
  pipPosition.value = {
    x: Math.max(minX, Math.min(maxX, screenWidth - newX - pipWidth)),
    y: Math.max(minY, Math.min(maxY, newY))
  };
  
  e.preventDefault();
};

// Handle touch drag movement
const onDragTouch = (e) => {
  if (!isDragging.value || e.touches.length !== 1) return;
  
  const touch = e.touches[0];
  const deltaX = dragStart.value.x - touch.clientX;
  const deltaY = touch.clientY - dragStart.value.y;
  
  const newX = dragOffset.value.x + deltaX;
  const newY = dragOffset.value.y + deltaY;
  
  // Get screen dimensions
  const screenWidth = window.innerWidth;
  const screenHeight = window.innerHeight;
  
  // PiP dimensions (adjust based on screen size)
  const isMobile = screenWidth < 768;
  const pipWidth = isMobile ? 85 : 120;
  const pipHeight = isMobile ? 115 : 160;
  
  // Constrain to screen boundaries
  const minX = 10;
  const maxX = screenWidth - pipWidth - 10;
  const minY = 60; // Below top bar
  const maxY = screenHeight - pipHeight - 100; // Above bottom controls
  
  pipPosition.value = {
    x: Math.max(minX, Math.min(maxX, screenWidth - newX - pipWidth)),
    y: Math.max(minY, Math.min(maxY, newY))
  };
  
  e.preventDefault();
};

// Stop drag
const stopDrag = () => {
  isDragging.value = false;
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
};

// Stop touch drag
const stopDragTouch = () => {
  isDragging.value = false;
  document.removeEventListener('touchmove', onDragTouch);
  document.removeEventListener('touchend', stopDragTouch);
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
  margin-bottom: 10px;
}

/* ==================== LOBBY SCREEN ==================== */
.lobby-container {
  min-height: 100vh;
  min-height: 100dvh; /* Dynamic viewport height for mobile */
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #0f0f0f 0%, #1a1a2e 50%, #16213e 100%);
  padding: 20px;
  overflow-y: auto; /* Allow scrolling on mobile */
  position: relative;
}

.lobby-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 50%, rgba(138, 43, 226, 0.15) 0%, transparent 50%),
              radial-gradient(circle at 80% 80%, rgba(0, 191, 255, 0.15) 0%, transparent 50%);
  pointer-events: none;
}

.lobby-card {
  background: linear-gradient(145deg, rgba(26, 26, 46, 0.95), rgba(15, 15, 15, 0.95));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(138, 43, 226, 0.3);
  border-radius: 24px;
  padding: 60px 40px;
  max-width: 480px;
  width: 100%;
  box-shadow: 0 20px 60px rgba(0,0,0,0.6), 0 0 100px rgba(138, 43, 226, 0.2);
  text-align: center;
  animation: fadeIn 0.5s ease;
  margin: auto; /* Center vertically */
  position: relative;
  z-index: 1;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.lobby-title {
  font-size: 32px;
  color: #ffffff;
  margin-bottom: 12px;
  font-weight: 700;
  text-shadow: 0 0 30px rgba(138, 43, 226, 0.5);
  background: linear-gradient(135deg, #ffffff 0%, #a78bfa 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.lobby-subtitle {
  color: #a0aec0;
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
  background: #0a0a0a;
  box-shadow: 0 10px 30px rgba(0,0,0,0.8), 0 0 40px rgba(138, 43, 226, 0.3);
  border: 1px solid rgba(138, 43, 226, 0.2);
}

.video-preview {
  width: 100%;
  height: 300px;
  object-fit: cover;
  display: block;
  transform: scaleX(-1);
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
  background: linear-gradient(135deg, #0a0a0a 0%, #1a1a2e 100%);
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
  background: rgba(138, 43, 226, 0.3);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 2px solid rgba(138, 43, 226, 0.5);
  border-radius: 50px;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(138, 43, 226, 0.4);
  z-index: 10;
}

.lobby-camera-toggle .icon {
  width: 20px;
  height: 20px;
  stroke-width: 2.5;
}

.lobby-camera-toggle:hover {
  background: rgba(138, 43, 226, 0.5);
  border-color: rgba(138, 43, 226, 0.8);
  transform: translateX(-50%) translateY(-2px);
  box-shadow: 0 6px 24px rgba(138, 43, 226, 0.6);
}

.lobby-camera-toggle:active {
  transform: translateX(-50%) scale(0.95);
}

.toggle-label {
  user-select: none;
}

.lobby-form {
  margin-bottom: 30px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Room Input Container with Copy Button */
.room-input-container {
  position: relative;
  width: 100%;
}

.room-input-container .lobby-input {
  padding-right: 55px;
}

.copy-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border: none;
  background: rgba(138, 43, 226, 0.2);
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  padding: 0;
}

.copy-btn:hover {
  background: rgba(138, 43, 226, 0.4);
  transform: translateY(-50%) scale(1.05);
  box-shadow: 0 0 20px rgba(138, 43, 226, 0.4);
}

.copy-btn:active {
  transform: translateY(-50%) scale(0.95);
}

.copy-icon {
  width: 20px;
  height: 20px;
  stroke: #a78bfa;
  transition: all 0.3s ease;
}

.copy-btn:hover .copy-icon {
  stroke: #c4b5fd;
}

.lobby-input {
  width: 100%;
  padding: 16px 20px;
  font-size: 16px;
  border: 2px solid rgba(138, 43, 226, 0.3);
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  border-radius: 12px;
  transition: all 0.3s;
  font-family: inherit;
}

.lobby-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.lobby-input:focus {
  outline: none;
  border-color: rgba(138, 43, 226, 0.8);
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 3px rgba(138, 43, 226, 0.2), 0 0 20px rgba(138, 43, 226, 0.3);
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
  background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
  color: white;
  box-shadow: 0 4px 20px rgba(139, 92, 246, 0.5), 0 0 40px rgba(139, 92, 246, 0.2);
  border: 1px solid rgba(139, 92, 246, 0.5);
}

.primary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(139, 92, 246, 0.7), 0 0 60px rgba(139, 92, 246, 0.3);
  background: linear-gradient(135deg, #9f7aea 0%, #7c3aed 100%);
}

.primary-btn:active {
  transform: translateY(0);
}

.secondary-btn {
  background: linear-gradient(135deg, rgba(138, 43, 226, 0.2) 0%, rgba(99, 102, 241, 0.2) 100%);
  color: white;
  box-shadow: 0 4px 20px rgba(138, 43, 226, 0.3);
  border: 2px solid rgba(138, 43, 226, 0.5);
}

.secondary-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 30px rgba(138, 43, 226, 0.5);
  background: linear-gradient(135deg, rgba(138, 43, 226, 0.3) 0%, rgba(99, 102, 241, 0.3) 100%);
}

.secondary-btn:active {
  transform: translateY(0);
}

/* Password Section */
.password-section {
  margin: 12px 0;
  text-align: left;
}

.password-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #cbd5e0;
  margin-bottom: 8px;
  user-select: none;
}

.password-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.password-input {
  margin-top: 8px;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from { opacity: 0; max-height: 0; }
  to { opacity: 1; max-height: 100px; }
}

/* Password Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease;
}

.modal-content {
  background: linear-gradient(145deg, rgba(26, 26, 46, 0.98), rgba(15, 15, 15, 0.98));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(138, 43, 226, 0.3);
  border-radius: 20px;
  padding: 40px;
  max-width: 400px;
  width: 90%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.8), 0 0 100px rgba(138, 43, 226, 0.3);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { transform: translateY(50px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.modal-content h2 {
  color: #ffffff;
  margin-bottom: 12px;
  font-size: 24px;
  text-shadow: 0 0 20px rgba(138, 43, 226, 0.4);
}

.modal-content p {
  color: #a0aec0;
  margin-bottom: 24px;
  font-size: 14px;
}

.modal-input {
  width: 100%;
  padding: 14px 18px;
  font-size: 15px;
  border: 2px solid rgba(138, 43, 226, 0.3);
  background: rgba(255, 255, 255, 0.05);
  color: #ffffff;
  border-radius: 10px;
  margin-bottom: 20px;
  transition: all 0.3s;
  font-family: inherit;
}

.modal-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

.modal-input:focus {
  outline: none;
  border-color: rgba(138, 43, 226, 0.8);
  background: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 3px rgba(138, 43, 226, 0.2), 0 0 20px rgba(138, 43, 226, 0.3);
}

.modal-buttons {
  display: flex;
  gap: 12px;
}

.modal-btn {
  flex: 1;
  padding: 14px;
  border: none;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.modal-btn.primary {
  background: linear-gradient(135deg, #8b5cf6 0%, #6366f1 100%);
  color: white;
  border: 1px solid rgba(139, 92, 246, 0.5);
}

.modal-btn.primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(139, 92, 246, 0.6);
  background: linear-gradient(135deg, #9f7aea 0%, #7c3aed 100%);
}

.modal-btn.secondary {
  background: rgba(255, 255, 255, 0.05);
  color: #cbd5e0;
  border: 2px solid rgba(138, 43, 226, 0.3);
}

.modal-btn.secondary:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(138, 43, 226, 0.5);
}

.status-box {
  background: linear-gradient(135deg, rgba(138, 43, 226, 0.1) 0%, rgba(99, 102, 241, 0.1) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(138, 43, 226, 0.3);
  padding: 20px;
  border-radius: 12px;
  margin: 20px 0;
  box-shadow: 0 4px 20px rgba(138, 43, 226, 0.2);
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
  color: #cbd5e0;
  font-size: 14px;
  margin-bottom: 10px;
}

.friend-joined {
  color: #a78bfa;
  font-weight: 600;
  font-size: 16px;
  animation: slideIn 0.5s ease;
  text-shadow: 0 0 20px rgba(138, 43, 226, 0.5);
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
  background: linear-gradient(135deg, rgba(138, 43, 226, 0.2) 0%, rgba(99, 102, 241, 0.2) 100%);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  padding: 25px;
  border-radius: 16px;
  border: 2px solid rgba(138, 43, 226, 0.4);
  box-shadow: 0 8px 30px rgba(138, 43, 226, 0.3);
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
  border: 4px solid #8b5cf6;
  border-radius: 50%;
  animation: ring 1s infinite;
  position: relative;
  box-shadow: 0 0 30px rgba(138, 43, 226, 0.5);
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
  0% { transform: scale(1); box-shadow: 0 0 0 0 rgba(138, 43, 226, 0.7); }
  50% { transform: scale(1.05); box-shadow: 0 0 0 10px rgba(138, 43, 226, 0); }
  100% { transform: scale(1); box-shadow: 0 0 0 0 rgba(138, 43, 226, 0); }
}

.incoming-text {
  font-size: 20px;
  color: #ffffff;
  margin-bottom: 20px;
  font-weight: 600;
  text-shadow: 0 0 20px rgba(138, 43, 226, 0.4);
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
  transition: all 0.3s ease;
}

.main-video.swapped {
  transform: scaleX(-1);
}

.pip-video-container {
  position: absolute;
  width: 120px;
  height: 160px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0,0,0,0.7);
  z-index: 10;
  border: 3px solid rgba(255,255,255,0.3);
  transition: transform 0.2s ease, border-color 0.3s ease, box-shadow 0.3s ease;
  background: #1a1a1a;
  cursor: grab;
  user-select: none;
  -webkit-user-select: none;
  touch-action: none;
}

.pip-video-container:active {
  cursor: grabbing;
}

.pip-video-container:hover {
  transform: scale(1.05);
  border-color: rgba(138, 43, 226, 0.6);
  box-shadow: 0 10px 40px rgba(138, 43, 226, 0.5);
}

.pip-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
  transition: all 0.3s ease;
}

.pip-video.swapped {
  transform: scaleX(1);
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
  pointer-events: none;
  transition: all 0.3s ease;
}

.pip-video-container:hover .pip-label {
  background: rgba(138, 43, 226, 0.9);
  box-shadow: 0 0 10px rgba(138, 43, 226, 0.5);
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
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  justify-content: center;
  z-index: 5;
  max-width: 95vw;
  width: auto;
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
  flex-wrap: nowrap;
  overflow: visible;
  width: auto;
  min-width: fit-content;
}

.control-btn-modern {
  width: 52px;
  height: 52px;
  min-width: 52px;
  min-height: 52px;
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
  flex-shrink: 0;
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

.control-btn-modern.btn-active {
  background: rgba(34, 197, 94, 0.9);
}

.control-btn-modern.btn-active:hover {
  background: rgba(34, 197, 94, 1);
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

/* Settings Panel */
.settings-panel {
  position: absolute;
  bottom: 10px;
  left: 0;
  right: 0;
  background: linear-gradient(to bottom, rgba(26, 26, 46, 0.98) 0%, rgba(15, 15, 15, 0.98) 100%);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border-radius: 20px 20px 0 0;
  padding: 0;
  z-index: 20;
  max-height: 70vh;
  overflow: hidden;
  box-shadow: 0 -10px 40px rgba(0, 0, 0, 0.8), 0 0 60px rgba(138, 43, 226, 0.2);
  border-top: 1px solid rgba(138, 43, 226, 0.3);
}

.settings-handle {
  width: 40px;
  height: 5px;
  background: rgba(138, 43, 226, 0.5);
  border-radius: 3px;
  margin: 12px auto 8px auto;
  cursor: grab;
  box-shadow: 0 0 15px rgba(138, 43, 226, 0.3);
}

.settings-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px 15px 20px;
  background: rgba(26, 26, 46, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 0.5px solid rgba(138, 43, 226, 0.3);
  position: sticky;
  top: 0;
  z-index: 10;
}

.settings-header h3 {
  font-size: 20px;
  color: #ffffff;
  font-weight: 700;
  letter-spacing: -0.3px;
  margin: 0;
  text-shadow: 0 0 20px rgba(138, 43, 226, 0.4);
}

.close-btn {
  background: rgba(138, 43, 226, 0.2);
  border: 1px solid rgba(138, 43, 226, 0.3);
  font-size: 20px;
  color: #ffffff;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
  font-weight: 600;
}

.close-btn:hover {
  background: rgba(138, 43, 226, 0.4);
  box-shadow: 0 0 15px rgba(138, 43, 226, 0.4);
}

.close-btn:active {
  transform: scale(0.92);
}

.settings-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 16px;
  padding-bottom: 30px;
  overflow-y: auto;
  max-height: calc(70vh - 85px);
  background: linear-gradient(to bottom, rgba(15, 15, 15, 0.5) 0%, rgba(26, 26, 46, 0.5) 100%);
}

.setting-item {
  background: rgba(255, 255, 255, 0.03);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(138, 43, 226, 0.2);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
  transition: all 0.2s;
}

.setting-item:active {
  transform: scale(0.99);
  border-color: rgba(138, 43, 226, 0.4);
}

.setting-item label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #a78bfa;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 12px 16px 8px 16px;
  background: transparent;
}

.settings-select {
  width: 100%;
  padding: 16px 16px;
  font-size: 17px;
  border: none;
  background: rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: all 0.2s;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif;
  color: #ffffff;
  font-weight: 400;
  -webkit-appearance: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg width='12' height='8' viewBox='0 0 12 8' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1L6 6L11 1' stroke='%23a78bfa' stroke-width='2' stroke-linecap='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 16px center;
  padding-right: 40px;
}

.settings-select:focus {
  outline: none;
  background-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 15px rgba(138, 43, 226, 0.3);
}

.settings-select:active {
  background-color: rgba(255, 255, 255, 0.1);
}

.settings-select option {
  padding: 12px;
  background: #1a1a2e;
  color: #ffffff;
}

.camera-info {
  background: transparent;
  padding: 0;
  border-radius: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.info-text {
  font-size: 17px;
  color: #ffffff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 0.5px solid rgba(138, 43, 226, 0.2);
  margin: 0;
}

.info-text:last-child {
  border-bottom: none;
}

.info-text strong {
  color: #a78bfa;
  font-weight: 400;
  font-size: 17px;
}

.quality-hint {
  margin: 8px 0 0 0;
  padding: 0 16px 12px 16px;
  font-size: 13px;
  color: #9ca3af;
  font-style: italic;
}

/* Slide Up Animation for Bottom Bar */
.slide-up-enter-active {
  transition: all 0.4s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.slide-up-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 1, 1);
}

.slide-up-enter-from {
  transform: translateX(-50%) translateY(150px);
  opacity: 0;
}

.slide-up-leave-to {
  transform: translateX(-50%) translateY(150px);
  opacity: 0;
}

.slide-up-enter-to {
  transform: translateX(-50%) translateY(0);
  opacity: 1;
}

/* Custom scrollbar for settings */
.settings-content::-webkit-scrollbar {
  width: 0px;
}

.settings-content {
  -ms-overflow-style: none;
  scrollbar-width: none;
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
    width: 85px;
    height: 115px;
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

  .controls-group {
    gap: 12px;
    padding: 10px 16px;
    padding-bottom: 20px;
    flex-wrap: nowrap;
    overflow: visible;
    max-width: calc(100vw - 30px);
  }

  .bottom-bar {
    bottom: max(50px, env(safe-area-inset-bottom));
    max-width: 100vw;
    padding: 0 10px;
  }

  .settings-panel {
    max-height: 75vh;
  }

  .settings-content {
    padding: 16px 14px 25px 14px;
    max-height: calc(75vh - 85px);
  }

  .setting-item label {
    font-size: 12px;
    padding: 10px 14px 6px 14px;
  }

  .settings-select {
    font-size: 16px;
    padding: 14px 14px;
    padding-right: 36px;
  }

  .info-text {
    font-size: 16px;
    padding: 12px 14px;
  }

  .info-text strong {
    font-size: 16px;
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
    width: 75px;
    height: 100px;
  }

  .control-btn-modern {
    width: 44px;
    height: 44px;
    min-width: 44px;
    min-height: 44px;
    flex-shrink: 0;
  }

  .control-btn-modern .icon {
    width: 20px;
    height: 20px;
  }

  .hang-up-red {
    width: 48px !important;
    height: 48px !important;
    min-width: 48px !important;
    min-height: 48px !important;
  }

  .controls-group {
    gap: 10px;
    padding: 8px 14px;
    padding-bottom: 18px;
    flex-wrap: nowrap;
    overflow-x: auto;
    overflow-y: visible;
    max-width: calc(100vw - 20px);
    -webkit-overflow-scrolling: touch;
  }

  .controls-group::-webkit-scrollbar {
    display: none;
  }

  .bottom-bar {
    bottom: max(45px, env(safe-area-inset-bottom));
    max-width: 100vw;
    padding: 0 5px;
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
    width: 70px;
    height: 90px;
  }

  .bottom-bar {
    bottom: max(35px, env(safe-area-inset-bottom));
    max-width: 100vw;
    padding: 0 5px;
  }

  .control-btn-modern {
    width: 44px;
    height: 44px;
    min-width: 44px;
    min-height: 44px;
    flex-shrink: 0;
  }

  .control-btn-modern .icon {
    width: 20px;
    height: 20px;
  }

  .hang-up-red {
    width: 48px !important;
    height: 48px !important;
    min-width: 48px !important;
    min-height: 48px !important;
  }

  .controls-group {
    padding: 8px 14px;
    padding-bottom: 16px;
    flex-wrap: nowrap;
    overflow-x: auto;
    overflow-y: visible;
    max-width: calc(100vw - 20px);
    -webkit-overflow-scrolling: touch;
  }

  .controls-group::-webkit-scrollbar {
    display: none;
  }
}

/* Hide default elements (for compatibility) */
.hidden {
  display: none !important;
}

/* Camera Selector Overlay */
.camera-selector-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(15px);
  -webkit-backdrop-filter: blur(15px);
  display: flex;
  align-items: flex-end;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

.camera-selector-panel {
  background: linear-gradient(145deg, rgba(26, 26, 46, 0.98), rgba(15, 15, 15, 0.98));
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  border-radius: 24px 24px 0 0;
  padding: 24px;
  width: 100%;
  max-width: 500px;
  box-shadow: 0 -8px 32px rgba(0, 0, 0, 0.8), 0 0 60px rgba(138, 43, 226, 0.3);
  border-top: 1px solid rgba(138, 43, 226, 0.3);
  animation: slideUpIn 0.3s ease;
}

.camera-selector-panel h3 {
  color: white;
  margin: 0 0 20px 0;
  font-size: 20px;
  font-weight: 600;
  text-shadow: 0 0 20px rgba(138, 43, 226, 0.4);
}

.camera-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
  max-height: 50vh;
  overflow-y: auto;
}

.camera-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: rgba(138, 43, 226, 0.1);
  border: 2px solid rgba(138, 43, 226, 0.2);
  border-radius: 12px;
  color: white;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  -webkit-tap-highlight-color: transparent;
}

.camera-option .icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  stroke: #a78bfa;
}

.camera-option:hover {
  background: rgba(138, 43, 226, 0.2);
  border-color: rgba(138, 43, 226, 0.4);
  transform: translateX(4px);
  box-shadow: 0 4px 20px rgba(138, 43, 226, 0.3);
}

.camera-option.active {
  background: rgba(138, 43, 226, 0.3);
  border-color: rgba(138, 43, 226, 0.6);
  box-shadow: 0 0 30px rgba(138, 43, 226, 0.4);
}

.close-camera-btn {
  width: 100%;
  padding: 16px;
  background: rgba(138, 43, 226, 0.2);
  border: 2px solid rgba(138, 43, 226, 0.3);
  border-radius: 12px;
  color: white;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  -webkit-tap-highlight-color: transparent;
}

.close-camera-btn:hover {
  background: rgba(138, 43, 226, 0.3);
  border-color: rgba(138, 43, 226, 0.5);
  box-shadow: 0 4px 20px rgba(138, 43, 226, 0.3);
}

@keyframes slideUpIn {
  from {
    transform: translateY(100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

</style>
