import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // Bypass Gateway — route directly to each service for debugging
      '/api/user': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false
      },
      '/api/activities': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false
      },
      '/api/recommendations': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
