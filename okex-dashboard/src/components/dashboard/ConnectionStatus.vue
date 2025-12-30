<template>
  <div class="connection-status-card">
    <div class="card-header">
      <h3>WebSocket Connection Status</h3>
      <button class="reconnect-btn" :disabled="loading || isConnected" @click="handleReconnect">
        <span v-if="loading">Reconnecting...</span>
        <span v-else>Manual Reconnect</span>
      </button>
    </div>

    <div class="status-content">
      <div class="status-indicator" :class="statusClass">
        <div class="status-dot"></div>
        <span class="status-text">{{ statusText }}</span>
      </div>

      <div class="status-details">
        <div class="detail-item">
          <span class="label">URL:</span>
          <span class="value">{{ connectionInfo.url || 'N/A' }}</span>
        </div>

        <div class="detail-item">
          <span class="label">Connected At:</span>
          <span class="value">{{
            connectionInfo.connectedAt
              ? new Date(connectionInfo.connectedAt).toLocaleString()
              : 'N/A'
          }}</span>
        </div>

        <div class="detail-item">
          <span class="label">Last Message:</span>
          <span class="value">{{
            connectionInfo.lastMessageTime
              ? formatRelativeTime(connectionInfo.lastMessageTime)
              : 'N/A'
          }}</span>
        </div>

        <div class="detail-item">
          <span class="label">Reconnect Attempts:</span>
          <span class="value attempts-badge" :class="{ warning: connectionInfo.reconnectAttempts > 3 }">
            {{ connectionInfo.reconnectAttempts }}
          </span>
        </div>

        <div
          v-if="connectionInfo.currentReconnectDelay"
          class="detail-item"
        >
          <span class="label">Current Reconnect Delay:</span>
          <span class="value">{{ connectionInfo.currentReconnectDelay }}ms</span>
        </div>
      </div>
    </div>

    <div v-if="error" class="error-message">
      {{ error }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useWebSocketStore } from '../../stores/websocket'
import { ConnectionStatus } from '../../types'

const store = useWebSocketStore()
const { connectionInfo, loading, error } = storeToRefs(store)
const { isConnected } = storeToRefs(store)
const { triggerReconnect } = store

const statusText = computed(() => {
  switch (connectionInfo.value.status) {
    case ConnectionStatus.CONNECTED:
      return 'Connected'
    case ConnectionStatus.CONNECTING:
      return 'Connecting...'
    case ConnectionStatus.RECONNECTING:
      return 'Reconnecting...'
    case ConnectionStatus.DISCONNECTED:
      return 'Disconnected'
    case ConnectionStatus.ERROR:
      return 'Error'
    default:
      return 'Unknown'
  }
})

const statusClass = computed(() => {
  return connectionInfo.value.status.toLowerCase()
})

function formatRelativeTime(timestamp: string): string {
  const now = new Date().getTime()
  const time = new Date(timestamp).getTime()
  const diff = now - time

  if (diff < 1000) return 'Just now'
  if (diff < 60000) return `${Math.floor(diff / 1000)}s ago`
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`
  return `${Math.floor(diff / 3600000)}h ago`
}

function handleReconnect() {
  triggerReconnect()
}
</script>

<style scoped>
.connection-status-card {
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

.reconnect-btn {
  padding: 8px 16px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.reconnect-btn:hover:not(:disabled) {
  background: #45a049;
}

.reconnect-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.status-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  background: #f5f5f5;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

.connected .status-dot {
  background: #4CAF50;
}

.connecting .status-dot,
.reconnecting .status-dot {
  background: #FFA726;
}

.disconnected .status-dot,
.error .status-dot {
  background: #EF5350;
  animation: none;
}

.status-text {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.status-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-item .label {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.detail-item .value {
  font-size: 14px;
  color: #333;
  font-family: monospace;
}

.attempts-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  background: #E3F2FD;
  color: #1976D2;
  font-size: 12px;
  font-weight: 600;
}

.attempts-badge.warning {
  background: #FFF3E0;
  color: #F57C00;
}

.error-message {
  margin-top: 16px;
  padding: 12px;
  background: #FFEBEE;
  color: #C62828;
  border-radius: 4px;
  font-size: 14px;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}
</style>
