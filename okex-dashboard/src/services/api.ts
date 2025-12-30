import axios, { type AxiosInstance } from 'axios'
import type {
  ApiResponse,
  ConnectionInfo,
  ReconnectionRecord,
  SystemMetrics,
  SubscriptionInfo,
  Candle
} from '../types'

class ApiService {
  private api: AxiosInstance

  constructor() {
    this.api = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    // Request interceptor
    this.api.interceptors.request.use(
      (config) => {
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )

    // Response interceptor
    this.api.interceptors.response.use(
      (response) => response.data,
      (error) => {
        console.error('API Error:', error)
        return Promise.reject(error)
      }
    )
  }

  // WebSocket connection status
  async getConnectionStatus(): Promise<ApiResponse<ConnectionInfo>> {
    return this.api.get('/websocket/status')
  }

  // Get reconnection history
  async getReconnectionHistory(limit = 50): Promise<ApiResponse<ReconnectionRecord[]>> {
    return this.api.get('/websocket/reconnect-history', {
      params: { limit }
    })
  }

  // Get system metrics
  async getSystemMetrics(): Promise<ApiResponse<SystemMetrics>> {
    return this.api.get('/metrics')
  }

  // Get subscriptions
  async getSubscriptions(): Promise<ApiResponse<SubscriptionInfo[]>> {
    return this.api.get('/subscriptions')
  }

  // Manually trigger reconnection
  async reconnect(): Promise<ApiResponse<void>> {
    return this.api.post('/websocket/reconnect')
  }

  // Add subscription
  async addSubscription(symbol: string, interval: string): Promise<ApiResponse<void>> {
    return this.api.post('/websocket/subscriptions', { symbol, interval })
  }

  // Remove subscription
  async removeSubscription(symbol: string, interval: string): Promise<ApiResponse<void>> {
    return this.api.delete('/websocket/subscriptions', {
      data: { symbol, interval }
    })
  }

  // Get recent candles for a symbol and interval (default last 300)
  async getCandles(symbol: string, interval: string, limit = 300): Promise<ApiResponse<Candle[]>> {
    return this.api.get('/candles', {
      params: { symbol, interval, limit }
    })
  }
}
export const apiService = new ApiService()
