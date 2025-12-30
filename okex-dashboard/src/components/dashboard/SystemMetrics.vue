<template>
  <div class="system-metrics-card">
    <div class="card-header">
      <h3>System Metrics</h3>
      <span class="last-updated">Last updated: {{ formatRelativeTime(lastUpdated) }}</span>
    </div>

    <div class="metrics-grid">
      <div class="metric-item">
        <div class="metric-icon messages"></div>
        <div class="metric-content">
          <span class="metric-label">Messages Received</span>
          <span class="metric-value">{{ formatNumber(systemMetrics.messagesReceived) }}</span>
          <span class="metric-sub">{{ systemMetrics.messagesPerSecond }}/sec</span>
        </div>
      </div>

      <div class="metric-item">
        <div class="metric-icon data"></div>
        <div class="metric-content">
          <span class="metric-label">Data Processed</span>
          <span class="metric-value">{{ formatBytes(systemMetrics.dataProcessed) }}</span>
        </div>
      </div>

      <div class="metric-item">
        <div class="metric-icon cache"></div>
        <div class="metric-content">
          <span class="metric-label">Cache Hit Rate</span>
          <span class="metric-value">{{ (systemMetrics.cacheHitRate * 100).toFixed(1) }}%</span>
          <div class="progress-bar">
            <div
              class="progress-fill"
              :style="{ width: `${systemMetrics.cacheHitRate * 100}%` }"
            ></div>
          </div>
        </div>
      </div>

      <div class="metric-item">
        <div class="metric-icon database"></div>
        <div class="metric-content">
          <span class="metric-label">MongoDB Connections</span>
          <span class="metric-value">{{ systemMetrics.mongodbConnections }}</span>
        </div>
      </div>

      <div class="metric-item">
        <div class="metric-icon redis"></div>
        <div class="metric-content">
          <span class="metric-label">Redis Connections</span>
          <span class="metric-value">{{ systemMetrics.redisConnections }}</span>
        </div>
      </div>

      <div class="metric-item">
        <div class="metric-icon memory"></div>
        <div class="metric-content">
          <span class="metric-label">Memory Usage</span>
          <span class="metric-value">{{ (systemMetrics.memoryUsage * 100).toFixed(1) }}%</span>
          <div class="progress-bar" :class="{ warning: systemMetrics.memoryUsage > 0.7 }">
            <div
              class="progress-fill"
              :style="{ width: `${systemMetrics.memoryUsage * 100}%` }"
            ></div>
          </div>
        </div>
      </div>

      <div class="metric-item">
        <div class="metric-icon cpu"></div>
        <div class="metric-content">
          <span class="metric-label">CPU Usage</span>
          <span class="metric-value">{{ (systemMetrics.cpuUsage * 100).toFixed(1) }}%</span>
          <div class="progress-bar" :class="{ warning: systemMetrics.cpuUsage > 0.7 }">
            <div
              class="progress-fill"
              :style="{ width: `${systemMetrics.cpuUsage * 100}%` }"
            ></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWebSocketStore } from '../../stores/websocket'

const store = useWebSocketStore()
const { systemMetrics } = storeToRefs(store)

const lastUpdated = ref(new Date().toISOString())

function formatNumber(num: number): string {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  } else if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}

function formatBytes(bytes: number): string {
  if (bytes >= 1073741824) {
    return (bytes / 1073741824).toFixed(2) + ' GB'
  } else if (bytes >= 1048576) {
    return (bytes / 1048576).toFixed(2) + ' MB'
  } else if (bytes >= 1024) {
    return (bytes / 1024).toFixed(2) + ' KB'
  }
  return bytes + ' B'
}

function formatRelativeTime(timestamp: string): string {
  const now = new Date().getTime()
  const time = new Date(timestamp).getTime()
  const diff = now - time

  if (diff < 1000) return 'Just now'
  if (diff < 60000) return `${Math.floor(diff / 1000)}s ago`
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`
  return `${Math.floor(diff / 3600000)}h ago`
}

let interval: number
onMounted(() => {
  interval = window.setInterval(() => {
    lastUpdated.value = new Date().toISOString()
  }, 1000)
})

onUnmounted(() => {
  if (interval) {
    clearInterval(interval)
  }
})
</script>

<style scoped>
.system-metrics-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.last-updated {
  font-size: 12px;
  color: #999;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.metric-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  background: #f9f9f9;
  border-radius: 8px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.metric-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 24px;
}

.metric-icon.messages {
  background: #E3F2FD;
}

.metric-icon.messages::before {
  content: '📨';
}

.metric-icon.data {
  background: #F3E5F5;
}

.metric-icon.data::before {
  content: '💾';
}

.metric-icon.cache {
  background: #E8F5E9;
}

.metric-icon.cache::before {
  content: '⚡';
}

.metric-icon.database {
  background: #FFF3E0;
}

.metric-icon.database::before {
  content: '🗄️';
}

.metric-icon.redis {
  background: #FFEBEE;
}

.metric-icon.redis::before {
  content: '🔴';
}

.metric-icon.memory {
  background: #E0F2F1;
}

.metric-icon.memory::before {
  content: '💻';
}

.metric-icon.cpu {
  background: #FCE4EC;
}

.metric-icon.cpu::before {
  content: '⚙️';
}

.metric-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.metric-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.metric-value {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  font-family: monospace;
}

.metric-sub {
  font-size: 12px;
  color: #999;
}

.progress-bar {
  width: 100%;
  height: 6px;
  background: #e0e0e0;
  border-radius: 3px;
  overflow: hidden;
  margin-top: 4px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4CAF50, #8BC34A);
  transition: width 0.3s ease;
}

.progress-bar.warning .progress-fill {
  background: linear-gradient(90deg, #FFA726, #FF5722);
}
</style>
