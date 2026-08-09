import { ref, onUnmounted } from 'vue'
import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'

/**
 * WebSocket连接管理 Composable
 */
export function useWebSocket() {
  const stompClient = ref(null)
  const connected = ref(false)
  const subscriptions = ref(new Map())

  /**
   * 连接到WebSocket服务器
   */
  const connect = () => {
    try {
      // 创建SockJS连接
      const socket = new SockJS('/api/websocket')
      stompClient.value = Stomp.over(socket)

      // 禁用控制台日志（可选）
      stompClient.value.debug = (str) => {
        console.log('STOMP: ' + str)
      }

      // 连接到服务器
      stompClient.value.connect(
        {},
        (frame) => {
          console.log('WebSocket连接成功:', frame)
          connected.value = true
        },
        (error) => {
          console.error('WebSocket连接失败:', error)
          connected.value = false
          
          // 重连逻辑
          setTimeout(() => {
            if (!connected.value) {
              console.log('尝试重新连接WebSocket...')
              connect()
            }
          }, 5000)
        }
      )
    } catch (error) {
      console.error('WebSocket初始化失败:', error)
    }
  }

  /**
   * 断开WebSocket连接
   */
  const disconnect = () => {
    if (stompClient.value && connected.value) {
      // 取消所有订阅
      subscriptions.value.forEach((subscription) => {
        subscription.unsubscribe()
      })
      subscriptions.value.clear()

      // 断开连接
      stompClient.value.disconnect(() => {
        console.log('WebSocket连接已断开')
        connected.value = false
      })
    }
  }

  /**
   * 订阅消息主题
   * @param {string} destination - 主题路径
   * @param {function} callback - 消息处理回调
   */
  const subscribe = (destination, callback) => {
    if (!stompClient.value || !connected.value) {
      console.warn('WebSocket未连接，无法订阅主题:', destination)
      return null
    }

    try {
      const subscription = stompClient.value.subscribe(destination, (message) => {
        try {
          const data = JSON.parse(message.body)
          callback(data)
        } catch (error) {
          console.error('解析WebSocket消息失败:', error)
          callback(message.body)
        }
      })

      subscriptions.value.set(destination, subscription)
      console.log('已订阅主题:', destination)
      return subscription
    } catch (error) {
      console.error('订阅主题失败:', destination, error)
      return null
    }
  }

  /**
   * 取消订阅
   * @param {string} destination - 主题路径
   */
  const unsubscribe = (destination) => {
    const subscription = subscriptions.value.get(destination)
    if (subscription) {
      subscription.unsubscribe()
      subscriptions.value.delete(destination)
      console.log('已取消订阅主题:', destination)
    }
  }

  /**
   * 发送消息到服务器
   * @param {string} destination - 目标路径
   * @param {object} message - 消息内容
   */
  const sendMessage = (destination, message) => {
    if (!stompClient.value || !connected.value) {
      console.warn('WebSocket未连接，无法发送消息')
      return false
    }

    try {
      stompClient.value.send(destination, {}, JSON.stringify(message))
      console.log('消息已发送到:', destination)
      return true
    } catch (error) {
      console.error('发送消息失败:', error)
      return false
    }
  }

  /**
   * 等待连接建立
   * @param {number} timeout - 超时时间（毫秒）
   */
  const waitForConnection = (timeout = 5000) => {
    return new Promise((resolve, reject) => {
      if (connected.value) {
        resolve(true)
        return
      }

      const checkConnection = () => {
        if (connected.value) {
          resolve(true)
        } else {
          setTimeout(checkConnection, 100)
        }
      }

      setTimeout(() => {
        if (!connected.value) {
          reject(new Error('WebSocket连接超时'))
        }
      }, timeout)

      checkConnection()
    })
  }

  // 组件卸载时自动断开连接
  onUnmounted(() => {
    disconnect()
  })

  return {
    stompClient,
    connected,
    connect,
    disconnect,
    subscribe,
    unsubscribe,
    sendMessage,
    waitForConnection
  }
}