import { createRouter, createWebHistory } from 'vue-router'
import DashboardIndex from '../views/dashboard/Index.vue'
import CandleChartPage from '../views/dashboard/CandleChartPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardIndex
    },
    {
      path: '/candles/:symbol/:interval',
      name: 'candles',
      component: CandleChartPage
    }
  ]
})

export default router
