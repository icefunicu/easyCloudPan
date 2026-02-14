import { fileURLToPath, URL } from 'node:url'

import { defineConfig, loadEnv, type UserConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import viteCompression from 'vite-plugin-compression'
import { VitePWA } from 'vite-plugin-pwa'
import viteImagemin from 'vite-plugin-imagemin'

// https://vitejs.dev/config/
export default defineConfig(({ mode }): UserConfig => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    base: env.VITE_CDN_URL || '/',
    plugins: [
      vue(),
      viteCompression({
        verbose: true,
        disable: false,
        threshold: 10240,
        algorithm: 'gzip',
        ext: '.gz',
      }),
      viteCompression({
        verbose: true,
        disable: false,
        threshold: 10240,
        algorithm: 'brotliCompress',
        ext: '.br',
      }),
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: ['favicon.ico', 'apple-touch-icon.png', 'masked-icon.svg'],
        manifest: {
          name: 'EasyCloudPan',
          short_name: 'EasyPan',
          description: 'A simple and easy-to-use cloud storage system.',
          theme_color: '#ffffff',
          icons: [
            {
              src: 'pwa-192x192.png',
              sizes: '192x192',
              type: 'image/png'
            },
            {
              src: 'pwa-512x512.png',
              sizes: '512x512',
              type: 'image/png'
            }
          ]
        }
      }),
      viteImagemin({
        gifsicle: {
          optimizationLevel: 7,
          interlaced: false,
        },
        optipng: {
          optimizationLevel: 7,
        },
        mozjpeg: {
          quality: 80,
        },
        pngquant: {
          quality: [0.8, 0.9],
          speed: 4,
        },
        svgo: {
          plugins: [
            {
              name: 'removeViewBox',
              active: false,
            },
            {
              name: 'removeEmptyAttrs',
              active: false,
            },
          ],
        },
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
          target: env.VITE_API_TARGET || "http://localhost:7090",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '/api')
        }
      }
    },
    css: {
      preprocessorOptions: {
        scss: {
          silenceDeprecations: ['import', 'global-builtin'],
        },
      },
    },
    build: {
      chunkSizeWarningLimit: 1500,
      cssCodeSplit: true,
      sourcemap: mode !== 'production',
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
              return 'preview-pdf' // PDF preview related
            }

            if (
              normalizedId.includes('/docx-preview/') ||
              normalizedId.includes('/jszip/')
            ) {
              return 'preview-doc' // DOCX preview related
            }

            if (normalizedId.includes('/xlsx/')) {
              return 'preview-sheet' // Excel preview related
            }

            if (
              normalizedId.includes('/dplayer/') ||
              normalizedId.includes('/aplayer/') ||
              normalizedId.includes('/hls.js/')
            ) {
              return 'media-vendor' // Media player related
            }

            if (
              normalizedId.includes('/element-plus/') ||
              normalizedId.includes('/@element-plus/')
            ) {
              return 'element-plus' // UI framework
            }

            if (
              normalizedId.includes('/vue-router/') ||
              normalizedId.includes('/pinia/') ||
              normalizedId.includes('/vue/') ||
              normalizedId.includes('/vue-legacy/')
            ) {
              return 'framework-vendor' // Core framework
            }

            if (
              normalizedId.includes('/highlight.js/') ||
              normalizedId.includes('/@highlightjs/')
            ) {
              return 'highlight-js' // Code highlighting
            }

            if (
              normalizedId.includes('/axios/') ||
              normalizedId.includes('/vue-clipboard3/')
            ) {
              return 'utils-vendor' // Utility libraries
            }

            return 'vendor' // Other dependencies
          }
        }
      },
      minify: 'terser',
      terserOptions: {
        compress: {
          drop_console: mode === 'production',
          drop_debugger: mode === 'production',
          pure_funcs: mode === 'production' ? ['console.log', 'console.info'] : [],
        },
      },
    }
  }
})
