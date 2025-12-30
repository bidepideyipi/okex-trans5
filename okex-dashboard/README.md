# OKEx WebSocket Monitoring Dashboard

A Vue 3 + TypeScript monitoring dashboard for the OKEx WebSocket data processing system. This dashboard provides real-time visibility into WebSocket connection status, system metrics, and subscription management.

## Features

### 🔌 WebSocket Connection Monitoring
- Real-time connection status display (Connected/Disconnected/Reconnecting/Error)
- Connection details (URL, connected time, last message time)
- Reconnection attempts tracking with Fibonacci backoff visualization
- Manual reconnection trigger

### 📊 System Metrics Dashboard
- Messages received and processing rate
- Data processing volume
- Cache hit rate visualization
- MongoDB and Redis connection pools
- System resource usage (Memory/CPU)
- Auto-refreshing metrics with configurable intervals

### 📜 Reconnection History
- Complete reconnection event timeline
- Success/failure status for each attempt
- Detailed error messages and duration tracking
- Filterable and sortable history

### 📡 Subscription Management
- Active subscriptions display (Symbol + Interval)
- Message count per subscription
- Last update time tracking
- Subscription statistics

## Tech Stack

- **Framework**: Vue 3 with Composition API
- **Language**: TypeScript
- **State Management**: Pinia
- **Routing**: Vue Router 4
- **HTTP Client**: Axios
- **Build Tool**: Vite (Rolldown experimental)
- **Charts**: Chart.js + vue-chartjs
- **Utilities**: @vueuse/core

## Project Structure

```
okex-dashboard/
├── src/
│   ├── components/
│   │   └── dashboard/           # Dashboard components
│   │       ├── ConnectionStatus.vue
│   │       ├── ReconnectionHistory.vue
│   │       ├── SystemMetrics.vue
│   │       └── SubscriptionsList.vue
│   ├── views/
│   │   └── dashboard/
│   │       └── Index.vue        # Main dashboard view
│   ├── stores/
│   │   └── websocket.ts         # Pinia store for state management
│   ├── services/
│   │   └── api.ts               # API service layer
│   ├── types/
│   │   └── index.ts             # TypeScript type definitions
│   ├── router/
│   │   └── index.ts             # Vue Router configuration
│   ├── App.vue                  # Root component
│   └── main.ts                  # Application entry point
├── .env                         # Environment configuration
└── package.json
```

## Prerequisites

- Node.js >= 18.x
- npm >= 9.x
- OKEx backend server running on port 8080 (or configured port)

## Installation

1. Navigate to the dashboard directory:
```bash
cd okex-trans-5/okex-dashboard
```

2. Install dependencies:
```bash
npm install
```

3. Configure environment variables:

Edit `.env` file:
```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_AUTO_REFRESH_INTERVAL=5000
```

## Development

Start the development server:
```bash
npm run dev
```

The dashboard will be available at `http://localhost:5173`

## Build for Production

Build the project:
```bash
npm run build
```

Preview production build:
```bash
npm run preview
```

## API Endpoints

The dashboard expects the following REST API endpoints from the backend:

### WebSocket Status
```
GET /api/websocket/status
Response: {
  success: boolean
  data: {
    status: 'CONNECTED' | 'DISCONNECTED' | 'CONNECTING' | 'RECONNECTING' | 'ERROR'
    url: string
    connectedAt?: string
    disconnectedAt?: string
    lastMessageTime?: string
    reconnectAttempts: number
    currentReconnectDelay?: number
  }
}
```

### Reconnection History
```
GET /api/websocket/reconnections?limit=50
Response: {
  success: boolean
  data: Array<{
    id: string
    timestamp: string
    reason: string
    attempt: number
    success: boolean
    duration?: number
    error?: string
  }>
}
```

### System Metrics
```
GET /api/metrics
Response: {
  success: boolean
  data: {
    messagesReceived: number
    messagesPerSecond: number
    dataProcessed: number
    cacheHitRate: number
    mongodbConnections: number
    redisConnections: number
    memoryUsage: number
    cpuUsage: number
  }
}
```

### Subscriptions
```
GET /api/websocket/subscriptions
Response: {
  success: boolean
  data: Array<{
    symbol: string
    interval: string
    subscribedAt: string
    messagesReceived: number
    lastUpdate: string
  }>
}
```

### Manual Reconnection
```
POST /api/websocket/reconnect
Response: {
  success: boolean
}
```

### Add Subscription
```
POST /api/websocket/subscriptions
Body: {
  symbol: string
  interval: string
}
Response: {
  success: boolean
}
```

### Remove Subscription
```
DELETE /api/websocket/subscriptions
Body: {
  symbol: string
  interval: string
}
Response: {
  success: boolean
}
```

## Features in Detail

### Auto-Refresh

The dashboard automatically refreshes connection status and system metrics every 5 seconds (configurable). You can toggle auto-refresh on/off using the button in the header.

### Connection Status Indicators

- 🟢 **Green**: Connected and healthy
- 🟠 **Orange**: Connecting or reconnecting
- 🔴 **Red**: Disconnected or error state

The status indicator includes a pulsing animation for active connections.

### Reconnection History

Shows detailed history of all reconnection attempts with:
- Success/failure badge
- Timestamp
- Reason for reconnection
- Attempt number
- Duration (for successful reconnections)
- Error message (for failed attempts)

### System Metrics

Real-time visualization of:
- **Messages**: Total received and rate per second
- **Data**: Total bytes processed with human-readable formatting
- **Cache**: Hit rate percentage with progress bar
- **Databases**: Active connection counts for MongoDB and Redis
- **Resources**: Memory and CPU usage with warning indicators (>70%)

## Customization

### Changing Refresh Interval

Modify `.env`:
```env
VITE_AUTO_REFRESH_INTERVAL=10000  # 10 seconds
```

Or programmatically in code:
```typescript
import { useWebSocketStore } from '@/stores/websocket'

const store = useWebSocketStore()
store.startAutoRefresh(10000) // 10 seconds
```

### Styling

All components use scoped styles. Global styles can be modified in:
- `src/App.vue` - Global app styles
- `src/style.css` - Base CSS variables and utilities

### Adding New Metrics

1. Update type definition in `src/types/index.ts`:
```typescript
export interface SystemMetrics {
  // ... existing fields
  newMetric: number
}
```

2. Add metric display in `src/components/dashboard/SystemMetrics.vue`

3. Backend should include the new metric in `/api/metrics` response

## Troubleshooting

### Dashboard shows "Failed to fetch connection status"

- Ensure backend server is running
- Check `VITE_API_BASE_URL` in `.env` matches your backend URL
- Verify CORS is enabled on backend for dashboard origin

### Auto-refresh not working

- Check browser console for errors
- Verify auto-refresh toggle is ON (button should be blue)
- Check network tab for API requests

### Build errors

- Clear node_modules and reinstall: `rm -rf node_modules && npm install`
- Update dependencies: `npm update`
- Check Node.js version: `node --version` (should be >= 18)

## Development Tips

### Hot Module Replacement (HMR)

Vite provides instant HMR. Changes to Vue components will reflect immediately without full page reload.

### Type Checking

Run TypeScript type checking:
```bash
npx vue-tsc --noEmit
```

### Linting

Format code:
```bash
npm run format
```

Lint code:
```bash
npm run lint
```

## Contributing

When adding new features:

1. Add types to `src/types/index.ts`
2. Update API service in `src/services/api.ts`
3. Update Pinia store in `src/stores/websocket.ts`
4. Create/update Vue components
5. Update this README if adding new API endpoints

## License

Internal project - All rights reserved

## Related Documentation

- [ARCHITECTURE.md](../doc/ARCHITECTURE.md) - Backend architecture
- [PRD.md](../PRD.md) - Product requirements
- [Mock.md](../doc/Mock.md) - Testing documentation