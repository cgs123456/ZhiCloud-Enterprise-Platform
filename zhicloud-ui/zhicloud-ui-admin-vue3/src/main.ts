// 引入unocss css
import '@/plugins/unocss'

// 导入全局的svg图标
import '@/plugins/svgIcon'

// 初始化多语言
import { setupI18n } from '@/plugins/vueI18n'

// 引入状态管理
import { setupStore } from '@/store'

// 全局组件
import { setupGlobCom } from '@/components'

// 引入 element-plus
import { setupElementPlus } from '@/plugins/elementPlus'

// 引入 form-create
import { setupFormCreate } from '@/plugins/formCreate'

// 引入全局样式
import '@/styles/index.scss'

// 引入动画
import '@/plugins/animate.css'

// 路由
import router, { setupRouter } from '@/router'

// 指令
import { setupAuth, setupMountedFocus } from '@/directives'

import { createApp } from 'vue'

import App from './App.vue'

import './permission'

import '@/plugins/tongji' // 百度统计
import Logger from '@/utils/Logger'

import VueDOMPurifyHTML from 'vue-dompurify-html' // 解决v-html 的安全隐患

// wangEditor 插件注册（改为动态导入，避免 wangEditor 进入首屏包）
import print from 'vue3-print-nb' // 打印插件

// 处理 Vite 预加载模块失败（如重新构建后 chunk 哈希变化），自动刷新页面
window.addEventListener('vite:preloadError', (event) => {
  event.preventDefault()
  window.location.reload()
})

// 创建实例
const setupAll = async () => {
  const app = createApp(App)

  // wangEditor 插件注册：动态导入使其进入异步 chunk，下载与 i18n/store 初始化并行，挂载前确保注册完成
  const wangEditorReady = import('@/views/bpm/model/form/PrintTemplate').then((m) =>
    m.setupWangEditorPlugin()
  )

  await setupI18n(app)

  setupStore(app)

  setupGlobCom(app)

  setupElementPlus(app)

  setupFormCreate(app)

  setupRouter(app)

  // directives 指令
  setupAuth(app)
  setupMountedFocus(app)

  // 全局错误兜底：仅打印，不弹 UI
  app.config.errorHandler = (err, _instance, info) => {
    console.error('[GlobalError]', err, info)
  }

  await router.isReady()

  // 确保 wangEditor 插件在应用挂载前注册完成（Boot.registerModule 只能执行一次）
  await wangEditorReady

  app.use(VueDOMPurifyHTML)

  // 打印
  app.use(print)

  app.mount('#app')
}

setupAll()

Logger.prettyPrimary(`欢迎使用`, import.meta.env.VITE_APP_TITLE)
