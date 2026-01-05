import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  define: {
    'global': 'globalThis',
    'process.env': {},
  },
  resolve: {
    alias: {
      events: 'events',
      buffer: 'buffer',
      util: 'util',
      'process/browser.js': 'process/browser.js',
    }
  },
  optimizeDeps: {
    esbuildOptions: {
      define: {
        global: 'globalThis'
      }
    }
  },
  build: {
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true,
        pure_funcs: ['console.log', 'console.info', 'console.debug', 'console.warn']
      }
    },
    rollupOptions: {
      output: {
        manualChunks: {
          'vue': ['vue'],
          'socket': ['socket.io-client'],
          'webrtc': ['simple-peer'],
          'crypto': ['crypto-js']
        }
      }
    },
    chunkSizeWarningLimit: 1000
  }
})
