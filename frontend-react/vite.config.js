import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';
export default defineConfig({
    plugins: [react()],
    build: {
        chunkSizeWarningLimit: 1200,
        rollupOptions: {
            output: {
                manualChunks: function (id) {
                    var moduleId = id.replace(/\\/g, '/');
                    if (!moduleId.includes('node_modules')) {
                        return;
                    }
                    if (moduleId.includes('/react/') ||
                        moduleId.includes('/react-dom/') ||
                        moduleId.includes('/react-router-dom/')) {
                        return 'react-core';
                    }
                    if (moduleId.includes('/antd/') || moduleId.includes('/@ant-design/')) {
                        return 'antd-core';
                    }
                    if (moduleId.includes('/@tanstack/') || moduleId.includes('/zustand/') || moduleId.includes('/axios/')) {
                        return 'data-core';
                    }
                    if (moduleId.includes('/xlsx/') || moduleId.includes('/docx-preview/') || moduleId.includes('/hls.js/')) {
                        return 'preview-core';
                    }
                    if (moduleId.includes('/dayjs/')) {
                        return 'date-core';
                    }
                    if (moduleId.includes('/framer-motion/')) {
                        return 'motion-core';
                    }
                    return;
                },
            },
        },
    },
    resolve: {
        alias: {
            '@': '/src',
        },
    },
    server: {
        host: '0.0.0.0',
        port: 5176,
        proxy: {
            '/api': {
                target: 'http://localhost:7090',
                changeOrigin: true,
            },
        },
    },
});
