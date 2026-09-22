import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  build: {
    // 把按需引入的组件样式合并成单个 CSS 文件：HTTP/1.1 下切换页面时
    // 不再需要额外请求 20+ 个小的样式分块（配合 /assets 的强缓存，切换基本零请求）
    cssCodeSplit: false
  },
  plugins: [
    vue(),
    // Element Plus 按需引入：组件、指令与 ElMessage/ElMessageBox 等 API 均自动引入，
    // 样式由解析器按组件注入（importStyle: 'css'），不再全量加载 element-plus 样式表。
    AutoImport({
      imports: ['vue', 'vue-router'],
      resolvers: [ElementPlusResolver({ importStyle: 'css' })]
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
      directives: true
    })
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        // 后端端口可通过 VITE_PROXY_TARGET 覆盖（start.bat 会按 start.env 里的端口设置）
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.spec.js'],
    server: {
      // 按需注入样式后 element-plus 的 css 需要交给 Vite 处理，否则 Node 端会报未知扩展名
      deps: { inline: ['element-plus'] }
    }
  }
})
