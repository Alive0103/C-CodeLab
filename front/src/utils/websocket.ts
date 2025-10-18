import { useAuthStore } from '../stores/auth'

export function createExecutionWS(onMessage: (data: any) => void) {
  const auth = useAuthStore()
  let ws: WebSocket | null = null
  let retry = 0

  const connect = () => {
    // 获取token并作为查询参数传递
    const token = localStorage.getItem('token')
    let url = (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws/execution-result'
    
    if (token) {
      // 移除Bearer前缀（如果有的话）
      const cleanToken = token.startsWith('Bearer ') ? token.substring(7) : token
      url += `?token=${encodeURIComponent(cleanToken)}`
    }
    
    ws = new WebSocket(url, [])
    ws.onopen = () => {
      retry = 0
      console.log('WebSocket connected successfully')
    }
    ws.onmessage = (ev) => {
      try { 
        onMessage(JSON.parse(ev.data)) 
      } catch (e) { 
        console.warn('Failed to parse WebSocket message:', e)
      }
    }
    ws.onclose = (event) => {
      console.log('WebSocket closed:', event.code, event.reason)
      if (retry < 5) {
        setTimeout(connect, Math.min(30000, 1000 * Math.pow(2, retry++)))
      }
    }
    ws.onerror = (error) => {
      console.error('WebSocket error:', error)
    }
  }

  connect()
  return {
    close: () => ws?.close()
  }
}


