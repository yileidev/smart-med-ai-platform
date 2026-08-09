"""
MQTT消息发布器
用于边缘端与云端通信

功能：
1. MQTT连接管理
2. 消息发布
3. 连接重试
4. 心跳维持
"""

import paho.mqtt.client as mqtt
import json
import time
import threading
import logging
import config
from typing import Dict, Any

logger = logging.getLogger(__name__)


class MQTTPublisher:
    """
    MQTT消息发布器
    负责边缘端数据上报到云端
    """
    
    def __init__(self):
        # MQTT配置
        self.broker = config.MQTT_BROKER
        self.port = config.MQTT_PORT
        self.client_id = config.MQTT_CLIENT_ID
        self.username = config.MQTT_USERNAME
        self.password = config.MQTT_PASSWORD
        
        # 主题配置
        self.triage_topic = config.MQTT_TOPIC_TRIAGE
        self.heartbeat_topic = config.MQTT_TOPIC_HEARTBEAT
        self.command_topic = f"cloud/{config.DEVICE_ID}/#"  # 订阅云端命令
        
        # 客户端实例
        self.client = None
        self.is_connected = False
        
        # 消息回调函数
        self.message_callbacks = []
        
        # 重连配置
        self.retry_count = 0
        self.max_retries = 5
        self.retry_interval = 5
        
        # 消息队列（离线缓存）
        self.message_queue = []
        self.max_queue_size = 100
        
        # 初始化MQTT客户端
        self._initialize_client()
        
        logger.info(f"MQTT发布器初始化完成: {self.broker}:{self.port}")
    
    def _initialize_client(self):
        """初始化MQTT客户端"""
        try:
            # 创建客户端实例（兼容paho-mqtt 2.x和1.x）
            try:
                # paho-mqtt 2.x 新版API
                from paho.mqtt.client import CallbackAPIVersion
                self.client = mqtt.Client(callback_api_version=CallbackAPIVersion.VERSION1, client_id=self.client_id)
                logger.info("使用paho-mqtt 2.x API")
            except (ImportError, TypeError, AttributeError):
                # paho-mqtt 1.x 旧版API
                self.client = mqtt.Client(self.client_id)
                logger.info("使用paho-mqtt 1.x API")
                
            # 设置用户名密码（如果配置了）
            if self.username and self.password:
                self.client.username_pw_set(self.username, self.password)
                
            # 设置回调函数
            self.client.on_connect = self._on_connect
            self.client.on_disconnect = self._on_disconnect
            self.client.on_publish = self._on_publish
            self.client.on_message = self._on_message  # 添加消息接收回调
                
            # 设置连接参数
            self.client.reconnect_delay_set(min_delay=1, max_delay=120)
                
        except Exception as e:
            logger.error(f"MQTT客户端初始化失败: {e}")
            import traceback
            traceback.print_exc()
            self.client = None
    
    def _on_connect(self, client, userdata, flags, rc):
        """连接回调"""
        if rc == 0:
            self.is_connected = True
            self.retry_count = 0
            logger.info(f"MQTT连接成功: {self.broker}")
            
            # 订阅云端命令主题
            self.client.subscribe(self.command_topic)
            logger.info(f"已订阅云端命令主题: {self.command_topic}")
            
            # 发送离线缓存的消息
            self._send_queued_messages()
            
        else:
            self.is_connected = False
            error_messages = {
                1: "协议版本不正确",
                2: "客户端ID无效",
                3: "服务器不可用",
                4: "用户名或密码错误",
                5: "未授权"
            }
            error_msg = error_messages.get(rc, f"未知错误: {rc}")
            logger.error(f"MQTT连接失败: {error_msg}")
    
    def _on_disconnect(self, client, userdata, rc):
        """断连回调"""
        self.is_connected = False
        if rc != 0:
            logger.warning(f"MQTT意外断连: {rc}")
            # 启动重连线程
            threading.Thread(target=self._reconnect, daemon=True).start()
        else:
            logger.info("MQTT正常断连")
    
    def _on_publish(self, client, userdata, mid):
        """发布回调"""
        logger.debug(f"消息发布成功: {mid}")
    
    def _on_message(self, client, userdata, msg):
        """消息接收回调 - 处理云端命令"""
        try:
            topic = msg.topic
            payload = msg.payload.decode('utf-8')
            logger.info(f"收到云端消息: {topic}")
            
            # 解析JSON
            try:
                data = json.loads(payload)
            except:
                data = {'raw': payload}
            
            # 调用所有注册的回调函数
            for callback in self.message_callbacks:
                try:
                    callback(topic, data)
                except Exception as e:
                    logger.error(f"消息回调执行失败: {e}")
                    
        except Exception as e:
            logger.error(f"消息处理异常: {e}")
    
    def register_callback(self, callback):
        """注册消息回调函数"""
        if callback not in self.message_callbacks:
            self.message_callbacks.append(callback)
            logger.info("已注册消息回调函数")
    
    def connect(self):
        """连接到MQTT代理"""
        if not self.client:
            logger.error("MQTT客户端未初始化")
            return False
        
        try:
            logger.info(f"正在连接MQTT代理: {self.broker}:{self.port}")
            self.client.connect(self.broker, self.port, keepalive=60)
            self.client.loop_start()
            
            # 等待连接建立
            timeout = 10
            start_time = time.time()
            while not self.is_connected and time.time() - start_time < timeout:
                time.sleep(0.1)
            
            if self.is_connected:
                logger.info("MQTT连接建立成功")
                return True
            else:
                logger.error("MQTT连接超时")
                return False
                
        except Exception as e:
            logger.error(f"MQTT连接异常: {e}")
            return False
    
    def _reconnect(self):
        """重连逻辑"""
        while self.retry_count < self.max_retries and not self.is_connected:
            self.retry_count += 1
            logger.info(f"MQTT重连尝试 {self.retry_count}/{self.max_retries}")
            
            try:
                if self.client:
                    self.client.reconnect()
                time.sleep(self.retry_interval)
                
            except Exception as e:
                logger.error(f"MQTT重连失败: {e}")
                time.sleep(self.retry_interval)
        
        if not self.is_connected:
            logger.error("MQTT重连次数耗尽，连接失败")
    
    def publish(self, topic: str, message: str, qos: int = 1, retain: bool = False):
        """
        发布消息
        
        Args:
            topic: 主题
            message: 消息内容
            qos: 服务质量等级
            retain: 是否保留消息
            
        Returns:
            success: 是否发布成功
        """
        if not self.client:
            logger.error("MQTT客户端未初始化")
            return False
        
        try:
            # 检查客户端是否连接
            logger.info(f"MQTT发布检查 - is_connected: {self.is_connected}, client.is_connected(): {self.client.is_connected() if hasattr(self.client, 'is_connected') else 'N/A'}")
            
            if self.is_connected:
                # 直接发布
                result = self.client.publish(topic, message, qos, retain)
                
                if result.rc == mqtt.MQTT_ERR_SUCCESS:
                    logger.info(f"消息发布成功: {topic}")
                    return True
                else:
                    logger.error(f"消息发布失败: {result.rc}")
                    return False
            else:
                # 连接断开，加入队列
                self._queue_message(topic, message, qos, retain)
                logger.info("MQTT未连接，消息已加入队列")
                
                # 尝试重连
                if self._attempt_reconnect():
                    logger.info("重连成功，消息已从队列发送")
                    return True
                else:
                    logger.error("重连失败")
                    return False
                
        except Exception as e:
            logger.error(f"消息发布异常: {e}")
            return False
    
    def _queue_message(self, topic: str, message: str, qos: int, retain: bool):
        """将消息加入离线队列"""
        if len(self.message_queue) >= self.max_queue_size:
            # 队列满了，移除最旧的消息
            removed_msg = self.message_queue.pop(0)
            logger.warning("消息队列已满，移除最旧消息")
        
        self.message_queue.append({
            'topic': topic,
            'message': message,
            'qos': qos,
            'retain': retain,
            'timestamp': time.time()
        })
        
        logger.debug(f"消息已入队，队列长度: {len(self.message_queue)}")
    
    def _send_queued_messages(self):
        """发送队列中的消息"""
        if not self.message_queue:
            return
        
        logger.info(f"发送队列中的 {len(self.message_queue)} 条消息")
        
        success_count = 0
        failed_messages = []
        
        for msg in self.message_queue:
            try:
                result = self.client.publish(
                    msg['topic'], 
                    msg['message'], 
                    msg['qos'], 
                    msg['retain']
                )
                
                if result.rc == mqtt.MQTT_ERR_SUCCESS:
                    success_count += 1
                else:
                    failed_messages.append(msg)
                    
            except Exception as e:
                logger.error(f"队列消息发送失败: {e}")
                failed_messages.append(msg)
        
        # 更新队列（保留失败的消息）
        self.message_queue = failed_messages
        
        logger.info(f"队列消息发送完成: 成功 {success_count}, 失败 {len(failed_messages)}")
    
    def _attempt_reconnect(self):
        """尝试重连"""
        if self.is_connected:
            return True
        
        try:
            # 使用connect而不是reconnect，更可靠
            self.client.connect(self.broker, self.port, keepalive=60)
            
            # 短暂等待连接建立
            timeout = 5
            start_time = time.time()
            while not self.is_connected and time.time() - start_time < timeout:
                time.sleep(0.1)
            
            # 重连成功后发送队列中的消息
            if self.is_connected:
                logger.info("MQTT重连成功")
                self._send_queued_messages()
            
            return self.is_connected
            
        except Exception as e:
            logger.error(f"重连尝试失败: {e}")
            return False
    
    def publish_triage_data(self, device_id: str, patient_data: Dict[str, Any]):
        """
        发布分诊数据
        
        Args:
            device_id: 设备ID
            patient_data: 患者数据
        """
        try:
            message = {
                'device_id': device_id,
                'timestamp': int(time.time() * 1000),
                'data_type': 'triage',
                'patient_data': patient_data
            }
            
            message_json = json.dumps(message, ensure_ascii=False)
            
            success = self.publish(self.triage_topic, message_json)
            
            if success:
                logger.info(f"分诊数据发布成功: {device_id}")
            else:
                logger.error(f"分诊数据发布失败: {device_id}")
            
            return success
            
        except Exception as e:
            logger.error(f"分诊数据发布异常: {e}")
            return False
    
    def publish_heartbeat(self, device_id: str, status: str = "online", metadata: Dict = None):
        """
        发布设备心跳
        
        Args:
            device_id: 设备ID
            status: 设备状态
            metadata: 额外元数据
        """
        try:
            heartbeat = {
                'device_id': device_id,
                'timestamp': int(time.time() * 1000),
                'status': status,
                'metadata': metadata or {}
            }
            
            heartbeat_json = json.dumps(heartbeat, ensure_ascii=False)
            
            # 心跳消息使用QoS 0（最多一次传递）
            success = self.publish(self.heartbeat_topic, heartbeat_json, qos=0)
            
            logger.debug(f"心跳发布: {device_id} - {status}")
            
            return success
            
        except Exception as e:
            logger.error(f"心跳发布异常: {e}")
            return False
    
    def get_connection_status(self):
        """获取连接状态信息"""
        return {
            'is_connected': self.is_connected,
            'broker': self.broker,
            'port': self.port,
            'client_id': self.client_id,
            'retry_count': self.retry_count,
            'queue_size': len(self.message_queue)
        }
    
    def disconnect(self):
        """断开MQTT连接"""
        if self.client:
            try:
                self.client.loop_stop()
                self.client.disconnect()
                self.is_connected = False
                logger.info("MQTT连接已断开")
            except Exception as e:
                logger.error(f"MQTT断连异常: {e}")


# 测试代码
if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG)
    
    # 创建MQTT发布器
    publisher = MQTTPublisher()
    
    # 连接测试
    if publisher.connect():
        print("MQTT连接成功")
        
        # 测试发布分诊数据
        test_patient_data = {
            'vital_signs': {
                'temperature': 38.2,
                'heart_rate': 110,
                'blood_pressure': '150/90',
                'oxygen_saturation': 96
            },
            'symptoms': '患者主诉胸痛，呼吸困难',
            'triage_level': 2,
            'confidence': 0.85
        }
        
        publisher.publish_triage_data('edge-device-001', test_patient_data)
        
        # 测试心跳
        publisher.publish_heartbeat('edge-device-001', 'online', {
            'cpu_usage': 45.2,
            'memory_usage': 67.8,
            'temperature': 42.5
        })
        
        # 等待消息发送
        time.sleep(2)
        
        # 获取连接状态
        status = publisher.get_connection_status()
        print(f"连接状态: {status}")
        
        # 断开连接
        publisher.disconnect()
        
    else:
        print("MQTT连接失败")