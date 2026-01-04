import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import { Buffer } from 'buffer'

// Make Buffer and process available globally for simple-peer
window.Buffer = Buffer
window.process = { 
  env: {}, 
  browser: true,
  nextTick: function(fn, ...args) {
    Promise.resolve().then(() => fn(...args))
  }
}

createApp(App).mount('#app')
