import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// En développement, Vite relaie /api vers le backend Spring Boot (port 8090) :
// le navigateur ne voit qu'une seule origine (http://localhost:5173).
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:8090',
    },
  },
})
