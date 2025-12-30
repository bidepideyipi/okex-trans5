<template>
  <div class="dashboard-container">
    <header class="dashboard-header">
      <h1>OKEx WebSocket Monitoring Dashboard</h1>
      <div class="header-actions">
        <button class="action-btn" :class="{ active: autoRefresh }" @click="toggleAutoRefresh">
          <span>{{ autoRefresh ? '🔄 Auto-Refresh ON' : '⏸️ Auto-Refresh OFF' }}</span>
        </button>
      </div>
    </header>

    <div class="dashboard-content">
      <div class="section">
        <ConnectionStatus />
      </div>

      <div class="section">
        <SystemMetrics />
      </div>

      <div class="section">
        <ReconnectionHistory />
      </div>

      <div v-if="subscriptions.length > 0" class="section">
        <SubscriptionsList />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useWebSocketStore } from '../../stores/websocket'
import ConnectionStatus from '../../components/dashboard/ConnectionStatus.vue'
import SystemMetrics from '../../components/dashboard/SystemMetrics.vue'
import ReconnectionHistory from '../../components/dashboard/ReconnectionHistory.vue'
import SubscriptionsList from '../../components/dashboard/SubscriptionsList.vue'

const store = useWebSocketStore()
const { subscriptions } = storeToRefs(store)
const {
  fetchConnectionStatus,
  fetchSystemMetrics,
  fetchReconnectionHistory,
  fetchSubscriptions,
  startAutoRefresh,
  stopAutoRefresh
} = store

const autoRefresh = ref(true)

function toggleAutoRefresh() {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    startAutoRefresh(5000)
  } else {
    stopAutoRefresh()
  }
}

onMounted(async () => {
  // Initial data fetch
  await Promise.all([
    fetchConnectionStatus(),
    fetchSystemMetrics(),
    fetchReconnectionHistory(),
    fetchSubscriptions()
  ])

  // Start auto-refresh
  if (autoRefresh.value) {
    startAutoRefresh(5000)
  }
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.dashboard-header {
  background: white;
  padding: 24px 32px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 100;
}

.dashboard-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  padding: 10px 20px;
  background: white;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #666;
  transition: all 0.3s;
}

.action-btn:hover {
  border-color: #2196F3;
  color: #2196F3;
}

.action-btn.active {
  background: #2196F3;
  border-color: #2196F3;
  color: white;
}

.dashboard-content {
  padding: 32px;
  max-width: 1400px;
  margin: 0 auto;
}

.section {
  margin-bottom: 24px;
}
</style>
