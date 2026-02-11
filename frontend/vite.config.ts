import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import viteCompression from 'vite-plugin-compression'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    viteCompression({
      verbose: true,
      disable: false,
      threshold: 10240,
      algorithm: 'gzip',
      ext: '.gz',
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 8080,
    hmr: true,
    proxy: {
      "/api": {
        target: process.env.VITE_API_TARGET || "http://localhost:7090",
        changeOrigin: true,
        pathRewrite: {
          "^api": "/api"
        }
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = id.replace(/\\/g, '/')
          if (!normalizedId.includes('/node_modules/')) {
            return
          }

          if (
            normalizedId.includes('/pdfjs-dist/') ||
            normalizedId.includes('/vue-pdf-embed/')
          ) {
            return 'preview-pdf'
          }

          if (
            normalizedId.includes('/docx-preview/') ||
            normalizedId.includes('/jszip/')
          ) {
            return 'preview-doc'
          }

          if (normalizedId.includes('/xlsx/')) {
            return 'preview-sheet'
          }

          if (
            normalizedId.includes('/dplayer/') ||
            normalizedId.includes('/aplayer/') ||
            normalizedId.includes('/hls.js/')
          ) {
            return 'media-vendor'
          }

          if (
            normalizedId.includes('/element-plus/') ||
            normalizedId.includes('/@element-plus/')
          ) {
            return 'element-plus'
          }

          if (
            normalizedId.includes('/vue-router/') ||
            normalizedId.includes('/pinia/') ||
            normalizedId.includes('/vue/')
          ) {
            return 'framework-vendor'
          }

          return 'vendor'
        }
      }
    }
  }
})
