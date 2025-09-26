import { useAuthStore } from '../stores/auth'

export function createExecutionWS(onMessage: (data: any) => void) {
  const auth = useAuthStore()
  let ws: WebSocket | null = null
  let retry = 0

  const connect = () => {
    const url = (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws/execution-result'
    ws = new WebSocket(url, [])
    ws.onopen = () => {
      retry = 0
      // Some servers only accept headers, but browsers can't set them; rely on proxy to forward Authorization header if needed
    }
    ws.onmessage = (ev) => {
      try { onMessage(JSON.parse(ev.data)) } catch { /* noop */ }
    }
    ws.onclose = () => {
      if (retry < 5) {
        setTimeout(connect, Math.min(30000, 1000 * Math.pow(2, retry++)))
      }
    }
  }

  connect()
  return {
    close: () => ws?.close()
  }
}


