import SockJS from 'sockjs-client/dist/sockjs.min.js'
import { Client } from '@stomp/stompjs'

/**
 * WebSocket连接管理类
 */
class WebSocketManager {
  constructor() {
    this.client = null
    this.connected = false
    this.subscriptions = new Map()
    this.messageHandlers = new Map()
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
  }

  /**
   * 连接WebSocket
   */
  connect(onConnected, onError) {
    const socket = new SockJS('http://localhost:8080/api/ws')
    
    this.client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      
      onConnect: () => {
        console.log('✅ WebSocket连接成功')
        this.connected = true
        this.reconnectAttempts = 0
        
        // 重新订阅所有频道
        this.resubscribeAll()
        
        if (onConnected) onConnected()
      },
      
      onStompError: (frame) => {
        console.error('❌ WebSocket错误:', frame)
        this.connected = false
        if (onError) onError(frame)
      },
      
      onDisconnect: () => {
        console.warn('⚠️ WebSocket断开连接')
        this.connected = false
        this.attemptReconnect(onConnected, onError)
      }
    })
    
    this.client.activate()
  }

  /**
   * 尝试重新连接
   */
  attemptReconnect(onConnected, onError) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`🔄 尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
      setTimeout(() => {
        this.connect(onConnected, onError)
      }, 5000)
    } else {
      console.error('❌ 达到最大重连次数，停止重连')
    }
  }

  /**
   * 订阅频道
   */
  subscribe(destination, callback) {
    if (!this.connected || !this.client) {
      console.warn('WebSocket未连接，稍后将自动订阅')
      this.messageHandlers.set(destination, callback)
      return null
    }

    const subscription = this.client.subscribe(destination, (message) => {
      try {
        const data = JSON.parse(message.body)
        callback(data)
      } catch (e) {
        console.error('解析消息失败:', e)
        callback(message.body)
      }
    })

    this.subscriptions.set(destination, subscription)
    this.messageHandlers.set(destination, callback)
    console.log(`📡 已订阅频道: ${destination}`)
    
    return subscription
  }

  /**
   * 取消订阅
   */
  unsubscribe(destination) {
    const subscription = this.subscriptions.get(destination)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(destination)
      this.messageHandlers.delete(destination)
      console.log(`📴 已取消订阅: ${destination}`)
    }
  }

  /**
   * 重新订阅所有频道
   */
  resubscribeAll() {
    this.messageHandlers.forEach((callback, destination) => {
      if (!this.subscriptions.has(destination)) {
        this.subscribe(destination, callback)
      }
    })
  }

  /**
   * 发送消息
   */
  send(destination, body) {
    if (!this.connected || !this.client) {
      console.error('WebSocket未连接')
      return false
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body)
    })
    return true
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.client) {
      this.subscriptions.forEach((subscription) => {
        subscription.unsubscribe()
      })
      this.subscriptions.clear()
      this.messageHandlers.clear()
      
      this.client.deactivate()
      this.connected = false
      console.log('🔌 WebSocket已断开')
    }
  }
}

// 创建全局单例
const wsManager = new WebSocketManager()

export default wsManager
