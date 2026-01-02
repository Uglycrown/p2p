import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { Buffer } from 'buffer'

// Make Buffer available globally for simple-peer
window.Buffer = Buffer

createApp(App).mount('#app')
