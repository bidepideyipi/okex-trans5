<template>
  <div class="reconnection-history-card">
    <div class="card-header">
      <h3>Reconnection History</h3>
      <button class="refresh-btn" @click="refresh">
        <span>Refresh</span>
      </button>
    </div>

    <div class="history-content">
      <div v-if="reconnectionHistory.length === 0" class="empty-state">
        <p>No reconnection records found</p>
      </div>

      <div v-else class="history-list">
        <div
          v-for="record in reconnectionHistory"
          :key="record.id"
          class="history-item"
          :class="{ success: record.success, failure: !record.success }"
        >
          <div class="item-header">
            <div class="status-badge" :class="{ success: record.success, failure: !record.success }">
              <span>{{ record.success ? 'Success' : 'Failed' }}</span>
            </div>
            <span class="timestamp">{{ formatTimestamp(record.timestamp) }}</span>
          </div>

          <div class="item-body">
            <div class="info-row">
              <span class="label">Reason:</span>
              <span class="value">{{ record.reason }}</span>
            </div>

            <div class="info-row">
              <span class="label">Attempt:</span>
              <span class="value">#{{ record.attempt }}</span>
            </div>

            <div v-if="record.duration" class="info-row">
              <span class="label">Duration:</span>
              <span class="value">{{ record.duration }}ms</span>
            </div>

            <div v-if="record.error" class="info-row error-row">
              <span class="label">Error:</span>
              <span class="value error-text">{{ record.error }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useWebSocketStore } from '../../stores/websocket'

const store = useWebSocketStore()
const { reconnectionHistory } = storeToRefs(store)
const { fetchReconnectionHistory } = store

function formatTimestamp(timestamp: string): string {
  return new Date(timestamp).toLocaleString()
}

function refresh() {
  fetchReconnectionHistory()
}
</script>

<style scoped>
.reconnection-history-card {
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

.refresh-btn {
  padding: 8px 16px;
  background: #2196F3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.3s;
}

.refresh-btn:hover {
  background: #1976D2;
}

.history-content {
  max-height: 500px;
  overflow-y: auto;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 16px;
  transition: all 0.3s;
}

.history-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.history-item.success {
  border-left: 4px solid #4CAF50;
}

.history-item.failure {
  border-left: 4px solid #EF5350;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.success {
  background: #E8F5E9;
  color: #2E7D32;
}

.status-badge.failure {
  background: #FFEBEE;
  color: #C62828;
}

.timestamp {
  font-size: 13px;
  color: #666;
  font-family: monospace;
}

.item-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  gap: 8px;
  font-size: 14px;
}

.info-row .label {
  font-weight: 500;
  color: #666;
  min-width: 80px;
}

.info-row .value {
  color: #333;
  font-family: monospace;
}

.error-row {
  background: #FFEBEE;
  padding: 8px;
  border-radius: 4px;
}

.error-text {
  color: #C62828;
  word-break: break-word;
}
</style>
