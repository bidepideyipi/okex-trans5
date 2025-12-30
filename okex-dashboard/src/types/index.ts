// WebSocket connection status types
export const ConnectionStatus = {
  CONNECTED: 'CONNECTED',
  DISCONNECTED: 'DISCONNECTED',
  CONNECTING: 'CONNECTING',
  RECONNECTING: 'RECONNECTING',
  ERROR: 'ERROR'
} as const

export type ConnectionStatus = typeof ConnectionStatus[keyof typeof ConnectionStatus]

// Candle data type (matches backend Candle model)
export interface Candle {
  symbol: string
  timestamp: string
  interval: string
  open: number
  high: number
  low: number
  close: number
  volume: number
  confirm: string
  created_at?: string
}

// WebSocket connection info
export interface ConnectionInfo {
  status: ConnectionStatus
  url: string
  connectedAt?: string
  disconnectedAt?: string
  lastMessageTime?: string
  reconnectAttempts: number
  currentReconnectDelay?: number
}

// Reconnection record
export interface ReconnectionRecord {
  id: string
  timestamp: string
  reason: string
  attempt: number
  success: boolean
  duration?: number
  error?: string
}

// System metrics
export interface SystemMetrics {
  messagesReceived: number
  messagesPerSecond: number
  dataProcessed: number
  cacheHitRate: number
  mongodbConnections: number
  redisConnections: number
  memoryUsage: number
  cpuUsage: number
}

// Subscription info
export interface SubscriptionInfo {
  symbol: string
  interval: string
  subscribedAt: string
  messagesReceived: number
  lastUpdate: string
}

// API Response types
export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: string
  timestamp: string
}
