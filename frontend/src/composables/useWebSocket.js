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
  const HEARTBEAT_INTERVAL = 60000 // 60초마다 Heartbeat (1분)
  const MAX_IDLE_TIME = 600000 // 10분 (더 긴 idle 시간)

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
        console.log('✅ WebSocket connected!')
        connectionStatus.value = 'connected'
        lastActivityTime = Date.now()
        if (reconnectInterval) {
          clearInterval(reconnectInterval)
          reconnectInterval = null
        }
        
        // socket_gorila 방식: 연결 즉시 초기 구독 메시지 전송
        sendInitialMessages()
        
        startHeartbeat()
      }

      socket.value.onmessage = (event) => {
        lastActivityTime = Date.now()
        try {
          const message = JSON.parse(event.data)
          
          // 재연결 알림 감지 시 구독 메시지 재전송
          if (message.type === 'reconnect') {
            console.log('🔄 WebSocket 재연결 감지 - 구독 메시지 재전송')
            // 잠시 대기 후 구독 메시지 전송 (연결이 완전히 안정화될 때까지)
            setTimeout(() => {
              sendInitialMessages()
            }, 1000)
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
        
        // 1002 (Protocol Error)나 1001 (Idle Timeout) 등은 재연결 시도
        if (event.code === 1002 || event.code === 1001 || event.code !== 1000) {
          console.log(`WebSocket 에러 코드 ${event.code} - 재연결 시도`)
          if (!reconnectInterval) {
            reconnectInterval = setInterval(connect, RECONNECT_DELAY)
          }
        } else {
          console.log(`WebSocket 정상 종료 (코드: ${event.code}) - 재연결하지 않음`)
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

  // socket_gorila 방식: 초기 구독 메시지 전송
  const sendInitialMessages = () => {
    if (!socket.value || socket.value.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket이 연결되지 않았습니다. 초기 메시지를 전송할 수 없습니다.')
      return
    }

    console.log('📤 초기 구독 메시지 전송 시작...')

    // 실시간 업데이트를 받기 위한 필수 메시지들 전송
    const messages = [
      { method: "user.auth_user", type: 0, request_id: 1 },
      { method: "notification.list", type: 0, request_id: 2 },
      { method: "resourcepool.list", type: 0, request_id: 3 },
      // 머신 목록을 구독하여 실시간 업데이트 받기
      { 
        method: "machine.list", 
        type: 0, 
        request_id: 4,
        params: {
          filter: {},
          group_collapsed: [],
          group_key: "status",
          page_number: 1,
          page_size: 50,
          sort_direction: "ascending",
          sort_key: "hostname"
        }
      },
    ]
    
    messages.forEach((msg, index) => {
      console.log(`📤 구독 메시지 ${index + 1} 전송:`, msg.method)
      sendMessage(JSON.stringify(msg))
    })
    
    console.log('✅ WebSocket 구독 완료 (머신, 리소스풀, 알림 실시간 업데이트 활성화)')
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
        // Heartbeat 비활성화 - MAAS 서버가 ping을 지원하지 않아 1002 에러 발생
        // 대신 연결 상태만 확인 (콘솔 로그 제거)
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