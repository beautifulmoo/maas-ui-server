// WebSocket 연결을 관리하는 컴포저블 함수
import { ref, onMounted, onUnmounted } from 'vue'

export function useWebSocket() {
  const socket = ref(null)
  const connectionStatus = ref('disconnected') // disconnected, connecting, connected, error
  const lastMessage = ref(null)
  let reconnectInterval = null
  let heartbeatInterval = null
  let lastActivityTime = Date.now()

  const WS_URL = 'ws://localhost:8081/ws' // 백엔드 WebSocket 엔드포인트
  const RECONNECT_DELAY = 5000 // 5초 후 재연결
  const HEARTBEAT_INTERVAL = 30000 // 30초마다 Heartbeat
  const MAX_IDLE_TIME = 300000 // 5분 (300초)

  const connect = () => {
    if (socket.value && (socket.value.readyState === WebSocket.OPEN || socket.value.readyState === WebSocket.CONNECTING)) {
      console.log('WebSocket already connected or connecting.')
      return
    }

    connectionStatus.value = 'connecting'
    console.log(`Attempting to connect to WebSocket: ${WS_URL}`)

    try {
      socket.value = new WebSocket(WS_URL)

      socket.value.onopen = () => {
        console.log('WebSocket connected!')
        connectionStatus.value = 'connected'
        lastActivityTime = Date.now()
        if (reconnectInterval) {
          clearInterval(reconnectInterval)
          reconnectInterval = null
        }
        startHeartbeat()
      }

      socket.value.onmessage = (event) => {
        lastActivityTime = Date.now()
        try {
          const message = JSON.parse(event.data)
          
          // pong 메시지는 lastMessage에 저장하지 않음 (heartbeat 응답)
          if (message.method === 'pong') {
            console.log('💓 [WebSocket] Pong received at', new Date().toLocaleTimeString())
            return
          }
          
          lastMessage.value = message
        } catch (e) {
          console.error('Failed to parse WebSocket message:', e, event.data)
          lastMessage.value = event.data // JSON 파싱 실패 시 원본 데이터 저장
        }
      }

      socket.value.onerror = (event) => {
        console.error('WebSocket error:', event)
        connectionStatus.value = 'error'
        if (!reconnectInterval) {
          reconnectInterval = setInterval(connect, RECONNECT_DELAY)
        }
      }

      socket.value.onclose = (event) => {
        console.warn('WebSocket disconnected:', event.code, event.reason)
        connectionStatus.value = 'disconnected'
        stopHeartbeat()
        if (!reconnectInterval) {
          reconnectInterval = setInterval(connect, RECONNECT_DELAY)
        }
      }
    } catch (e) {
      console.error('Failed to create WebSocket:', e)
      connectionStatus.value = 'error'
      if (!reconnectInterval) {
        reconnectInterval = setInterval(connect, RECONNECT_DELAY)
      }
    }
  }

  const sendMessage = (message) => {
    if (socket.value && socket.value.readyState === WebSocket.OPEN) {
      socket.value.send(message)
    } else {
      console.warn('WebSocket is not open. Message not sent:', message)
    }
  }

  const startHeartbeat = () => {
    stopHeartbeat() // 기존 Heartbeat 중지
    heartbeatInterval = setInterval(() => {
      const currentTime = Date.now()
      const idleTime = currentTime - lastActivityTime

      if (idleTime > MAX_IDLE_TIME) {
        console.warn(`WebSocket idle for ${idleTime}ms. Reconnecting...`)
        socket.value.close(1000, 'Idle timeout')
        return
      }

      if (socket.value && socket.value.readyState === WebSocket.OPEN) {
        // 백엔드에 Heartbeat 메시지 전송
        sendMessage(JSON.stringify({ method: "ping", type: 0, request_id: Date.now() }))
      }
    }, HEARTBEAT_INTERVAL)
  }

  const stopHeartbeat = () => {
    if (heartbeatInterval) {
      clearInterval(heartbeatInterval)
      heartbeatInterval = null
    }
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    if (socket.value) {
      socket.value.close()
    }
    if (reconnectInterval) {
      clearInterval(reconnectInterval)
    }
    stopHeartbeat()
  })

  return {
    socket,
    connectionStatus,
    lastMessage,
    sendMessage
  }
}