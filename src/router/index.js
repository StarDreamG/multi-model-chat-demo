import { createRouter, createWebHistory } from 'vue-router'
import LandingView from '../views/LandingView.vue'
import LabView from '../views/LabView.vue'
import DocsView from '../views/DocsView.vue'

export const createRouterEngine = () => createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'landing', component: LandingView },
    { path: '/lab', name: 'lab', component: LabView },
    { path: '/docs', name: 'docs', component: DocsView }
  ]
})
