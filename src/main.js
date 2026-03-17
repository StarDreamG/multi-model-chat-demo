import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { createRouterEngine } from './router'
import App from './App.vue'
import './assets/styles/global.css'

const sandboxApp = createApp(App)
sandboxApp.use(createPinia())
sandboxApp.use(createRouterEngine())
sandboxApp.mount('#app')
