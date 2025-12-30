import { createRouter, createWebHistory } from 'vue-router'
import DashboardIndex from '../views/dashboard/Index.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardIndex
    }
  ]
})

export default router
