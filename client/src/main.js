import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { Buffer } from 'buffer'

// Make Buffer and process available globally for simple-peer
window.Buffer = Buffer
window.process = { env: {}, browser: true }

createApp(App).mount('#app')
