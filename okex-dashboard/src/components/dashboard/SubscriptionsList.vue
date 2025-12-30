<template>
  <div class="subscriptions-card">
    <div class="card-header">
      <h3>Active Subscriptions</h3>
      <span class="count-badge">{{ subscriptions.length }}</span>
    </div>

    <div class="subscriptions-grid">
      <div
        v-for="sub in subscriptions"
        :key="`${sub.symbol}-${sub.interval}`"
        class="subscription-item"
        @click="openCandleChart(sub)"
      >
        <div class="sub-header">
          <div class="sub-name">
            <span class="symbol">{{ sub.symbol }}</span>
            <span class="interval-badge">{{ sub.interval }}</span>
          </div>
        </div>

        <div class="sub-details">
          <div class="detail-row">
            <span class="label">Subscribed:</span>
            <span class="value">{{ formatTimestamp(sub.subscribedAt) }}</span>
          </div>

          <div class="detail-row">
            <span class="label">Messages:</span>
            <span class="value">{{ sub.messagesReceived.toLocaleString() }}</span>
          </div>

          <div class="detail-row">
            <span class="label">Last Update:</span>
            <span class="value">{{ formatRelativeTime(sub.lastUpdate) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useWebSocketStore } from '../../stores/websocket'

const store = useWebSocketStore()
const { subscriptions } = storeToRefs(store)
const router = useRouter()

function formatTimestamp(timestamp: string): string {
  return new Date(timestamp).toLocaleString()
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

function openCandleChart(sub: { symbol: string; interval: string }) {
  router.push({
    name: 'candles',
    params: {
      symbol: sub.symbol,
      interval: sub.interval
    }
  })
}
</script>

<style scoped>
.subscriptions-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.count-badge {
  background: #2196F3;
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
}

.subscriptions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.subscription-item {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 16px;
  transition: all 0.3s;
  cursor: pointer;
}

.subscription-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.sub-header {
  margin-bottom: 12px;
}

.sub-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.symbol {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.interval-badge {
  background: #E3F2FD;
  color: #1976D2;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.sub-details {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.detail-row .label {
  color: #666;
}

.detail-row .value {
  color: #333;
  font-family: monospace;
  font-weight: 500;
}
</style>
