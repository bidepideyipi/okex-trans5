<template>
  <div class="dashboard-container">
    <header class="dashboard-header">
      <h1>Candles - {{ symbol }} <span class="interval">{{ interval }}</span></h1>
    </header>

    <main class="dashboard-content">
      <CandleChart
        :symbol="symbol"
        :interval="interval"
        :candles="candles"
        :loading="loading"
        :error="error"
      />
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import CandleChart from '../../components/dashboard/CandleChart.vue'
import type { Candle } from '../../types'
import { apiService } from '../../services/api'

const route = useRoute()
const symbol = route.params.symbol as string
const interval = route.params.interval as string

const candles = ref<Candle[]>([])
const loading = ref<boolean>(false)
const error = ref<string | null>(null)

onMounted(async () => {
  loading.value = true
  error.value = null

  try {
    const response = await apiService.getCandles(symbol, interval, 100)
    if (response.success && response.data) {
      candles.value = response.data
    } else {
      error.value = response.error || 'Failed to load candles'
    }
  } catch (e: any) {
    error.value = e?.message || 'Unexpected error while loading candles'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.dashboard-header {
  background: white;
  padding: 16px 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.dashboard-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.dashboard-header .interval {
  font-size: 14px;
  background: #e3f2fd;
  color: #1976d2;
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: 8px;
}

.dashboard-content {
  padding: 24px;
}
</style>
