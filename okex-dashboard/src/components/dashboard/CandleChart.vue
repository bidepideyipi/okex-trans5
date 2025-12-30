<template>
  <div class="candle-chart-wrapper">
    <div class="chart-header">
      <div>
        <h2>{{ symbol }} <span class="interval">{{ interval }}</span></h2>
        <p class="subtitle">Last {{ candles.length }} candles (chronological)</p>
      </div>
      <button class="back-btn" @click="$router.back()">← Back</button>
    </div>

    <div v-if="loading" class="state">Loading candles...</div>
    <div v-else-if="error" class="state error">{{ error }}</div>
    <div v-else-if="candles.length === 0" class="state">No candle data available.</div>
    <div v-else>
      <div class="chart-container">
        <VueChart :data="chartData" :options="chartOptions" />
      </div>
      <div class="volume-container">
        <VueChart :data="volumeChartData" :options="volumeChartOptions" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Chart as VueChart } from 'vue-chartjs'
import {
  Chart as ChartJS,
  BarElement,
  CategoryScale,
  LinearScale,
  Tooltip,
  Legend
} from 'chart.js'
import type { Candle } from '../../types'

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip, Legend)

interface Props {
  symbol: string
  interval: string
  candles: Candle[]
  loading: boolean
  error: string | null
}

const props = defineProps<Props>()

const chartData = computed(() => {
  const labels = props.candles.map((c) => c.timestamp)

  const wickData = props.candles.map((c) => [c.low, c.high])
  const bodyData = props.candles.map((c) => [Math.min(c.open, c.close), Math.max(c.open, c.close)])
  const colors = props.candles.map((c) => (c.close >= c.open ? '#26a69a' : '#ef5350'))

  return {
    labels,
    datasets: [
      {
        type: 'bar' as const,
        label: 'Wick',
        data: wickData,
        backgroundColor: colors,
        borderColor: colors,
        borderWidth: 1,
        barPercentage: 0.05,
        categoryPercentage: 1.0
      },
      {
        type: 'bar' as const,
        label: 'Body',
        data: bodyData,
        backgroundColor: colors,
        borderColor: colors,
        borderWidth: 1,
        barPercentage: 0.6,
        categoryPercentage: 1.0
      }
    ]
  }
})

const volumeChartData = computed(() => {
  const labels = props.candles.map((c) => c.timestamp)
  const volumes = props.candles.map((c) => c.volume)
  const colors = props.candles.map((c) => (c.close >= c.open ? '#26a69a' : '#ef5350'))

  return {
    labels,
    datasets: [
      {
        type: 'bar' as const,
        label: 'Volume',
        data: volumes,
        backgroundColor: colors,
        borderColor: colors,
        borderWidth: 1,
        barPercentage: 0.8,
        categoryPercentage: 1.0
      }
    ]
  }
})

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  interaction: {
    mode: 'index' as const,
    intersect: false
  },
  scales: {
    x: {
      type: 'category' as const,
      ticks: {
        maxTicksLimit: 10,
        color: '#555',
        callback(value: any, index: number, ticks: any[]) {
          const label = (ticks[index] && (ticks[index] as any).label) || ''
          return String(label).replace('T', ' ').slice(0, 16)
        }
      },
      grid: {
        color: '#e0e0e0'
      }
    },
    y: {
      beginAtZero: false,
      ticks: {
        maxTicksLimit: 8,
        color: '#555'
      },
      grid: {
        color: '#f0f0f0'
      }
    }
  },
  plugins: {
    legend: {
      display: false
    }
  }
}))

const volumeChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  scales: {
    x: {
      type: 'category' as const,
      ticks: {
        maxTicksLimit: 10,
        color: '#555',
        callback(value: any, index: number, ticks: any[]) {
          const label = (ticks[index] && (ticks[index] as any).label) || ''
          return String(label).replace('T', ' ').slice(0, 16)
        }
      },
      grid: {
        color: '#e0e0e0'
      }
    },
    y: {
      beginAtZero: true,
      ticks: {
        maxTicksLimit: 4,
        color: '#555'
      },
      grid: {
        color: '#f0f0f0'
      }
    }
  },
  plugins: {
    legend: {
      display: false
    }
  }
}))
</script>

<style scoped>
.candle-chart-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.chart-header .interval {
  font-size: 14px;
  background: #e3f2fd;
  color: #1976d2;
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: 8px;
}

.subtitle {
  margin: 4px 0 0;
  color: #666;
  font-size: 13px;
}

.back-btn {
  padding: 6px 12px;
  border-radius: 4px;
  border: 1px solid #ccc;
  background: white;
  cursor: pointer;
  font-size: 13px;
  color: #333;
}

.back-btn:hover {
  border-color: #1976d2;
  color: #1976d2;
}

.chart-container {
  height: 340px;
  background: white;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.volume-container {
  margin-top: 12px;
  height: 120px;
  background: white;
  border-radius: 8px;
  padding: 12px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.state {
  padding: 24px;
  text-align: center;
  color: #666;
}

.state.error {
  color: #d32f2f;
}
</style>
