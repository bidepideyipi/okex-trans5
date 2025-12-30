import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { apiService } from '../services/api'
import type {
  ConnectionInfo,
  ReconnectionRecord,
  SystemMetrics,
  SubscriptionInfo
} from '../types'
import { ConnectionStatus } from '../types'

export const useWebSocketStore = defineStore('websocket', () => {
  // State
  const connectionInfo = ref<ConnectionInfo>({
    status: ConnectionStatus.DISCONNECTED,
    url: '',
    reconnectAttempts: 0
  })
  const reconnectionHistory = ref<ReconnectionRecord[]>([])
  const systemMetrics = ref<SystemMetrics>({
    messagesReceived: 0,
    messagesPerSecond: 0,
    dataProcessed: 0,
    cacheHitRate: 0,
    mongodbConnections: 0,
    redisConnections: 0,
    memoryUsage: 0,
    cpuUsage: 0
  })
  const subscriptions = ref<SubscriptionInfo[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Computed
  const isConnected = computed(() => connectionInfo.value.status === ConnectionStatus.CONNECTED)
  const isReconnecting = computed(
    () => connectionInfo.value.status === ConnectionStatus.RECONNECTING
  )

  // Actions
  async function fetchConnectionStatus() {
    try {
      loading.value = true
      error.value = null
      const response = await apiService.getConnectionStatus()
      if (response.success && response.data) {
        connectionInfo.value = response.data
      }
    } catch (err) {
      error.value = 'Failed to fetch connection status'
      console.error(err)
    } finally {
      loading.value = false
    }
  }

  async function fetchReconnectionHistory() {
    try {
      const response = await apiService.getReconnectionHistory()
      if (response.success && response.data) {
        reconnectionHistory.value = response.data
      }
    } catch (err) {
      console.error('Failed to fetch reconnection history:', err)
    }
  }

  async function fetchSystemMetrics() {
    try {
      const response = await apiService.getSystemMetrics()
      if (response.success && response.data) {
        systemMetrics.value = response.data
      }
    } catch (err) {
      console.error('Failed to fetch system metrics:', err)
    }
  }

  async function fetchSubscriptions() {
    try {
      const response = await apiService.getSubscriptions()
      if (response.success && response.data) {
        subscriptions.value = response.data
      }
    } catch (err) {
      console.error('Failed to fetch subscriptions:', err)
    }
  }

  async function triggerReconnect() {
    try {
      loading.value = true
      await apiService.reconnect()
      await fetchConnectionStatus()
    } catch (err) {
      error.value = 'Failed to trigger reconnection'
      console.error(err)
    } finally {
      loading.value = false
    }
  }

  async function addSubscription(symbol: string, interval: string) {
    try {
      await apiService.addSubscription(symbol, interval)
      await fetchSubscriptions()
    } catch (err) {
      console.error('Failed to add subscription:', err)
      throw err
    }
  }

  async function removeSubscription(symbol: string, interval: string) {
    try {
      await apiService.removeSubscription(symbol, interval)
      await fetchSubscriptions()
    } catch (err) {
      console.error('Failed to remove subscription:', err)
      throw err
    }
  }

  // Auto-refresh data
  let refreshInterval: number | null = null

  function startAutoRefresh(intervalMs = 5000) {
    stopAutoRefresh()
    refreshInterval = window.setInterval(() => {
      fetchConnectionStatus()
      fetchSystemMetrics()
      fetchSubscriptions()
    }, intervalMs)
  }

  function stopAutoRefresh() {
    if (refreshInterval) {
      clearInterval(refreshInterval)
      refreshInterval = null
    }
  }

  return {
    // State
    connectionInfo,
    reconnectionHistory,
    systemMetrics,
    subscriptions,
    loading,
    error,
    // Computed
    isConnected,
    isReconnecting,
    // Actions
    fetchConnectionStatus,
    fetchReconnectionHistory,
    fetchSystemMetrics,
    fetchSubscriptions,
    triggerReconnect,
    addSubscription,
    removeSubscription,
    startAutoRefresh,
    stopAutoRefresh
  }
})
