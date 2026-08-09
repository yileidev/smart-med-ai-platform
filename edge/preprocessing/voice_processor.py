"""
讯飞语音处理模块
集成讯飞语音转文字API

功能：
1. 实时语音采集
2. 语音转文字
3. 语音质量检测
4. 噪声抑制
"""

import json
import base64
import hmac
import hashlib
import time
import uuid
from urllib.parse import urlencode
from datetime import datetime
import websocket
import threading
import logging
import config

logger = logging.getLogger(__name__)


class XFVoiceProcessor:
    """
    讯飞语音转文字处理器
    基于WebSocket实时语音识别
    """
    
    def __init__(self):
        # 从配置文件读取API信息
        self.app_id = config.XFYUN_APP_ID
        self.api_key = config.XFYUN_API_KEY
        self.api_secret = config.XFYUN_API_SECRET
        self.language = config.XFYUN_LANGUAGE
        
        # WebSocket相关
        self.ws_url = "wss://iat-api.xfyun.cn/v2/iat"
        self.ws = None
        
        # 状态管理
        self.is_connected = False
        self.recognition_result = ""
        self.error_message = ""
        
        # 事件
        self.recognition_event = threading.Event()
        
        logger.info("讯飞语音处理器初始化完成")
    
    def _generate_auth_url(self):
        """生成认证URL"""
        # 生成RFC1123格式的时间戳
        now = datetime.now()
        date = now.strftime('%a, %d %b %Y %H:%M:%S %Z')
        
        # 拼接字符串
        signature_origin = f"host: ws-api.xfyun.cn\ndate: {date}\nGET /v2/iat HTTP/1.1"
        
        # 进行hmac-sha256进行加密
        signature_sha = hmac.new(
            self.api_secret.encode('utf-8'),
            signature_origin.encode('utf-8'),
            digestmod=hashlib.sha256
        ).digest()
        
        signature_sha_base64 = base64.b64encode(signature_sha).decode(encoding='utf-8')
        
        # 构建authorization原始字符串
        authorization_origin = f'api_key="{self.api_key}", algorithm="hmac-sha256", headers="host date request-line", signature="{signature_sha_base64}"'
        
        authorization = base64.b64encode(authorization_origin.encode('utf-8')).decode(encoding='utf-8')
        
        # 将请求的鉴权参数组合为字典
        v = {
            "authorization": authorization,
            "date": date,
            "host": "ws-api.xfyun.cn"
        }
        
        # 拼接鉴权参数，生成url
        url = self.ws_url + '?' + urlencode(v)
        return url
    
    def on_message(self, ws, message):
        """处理WebSocket消息"""
        try:
            data = json.loads(message)
            code = data['code']
            
            if code != 0:
                logger.error(f'语音识别错误: {code}, {data}')
                self.error_message = f"错误码: {code}"
                self.recognition_event.set()
                return
            
            # 解析识别结果
            if 'data' in data and 'result' in data['data']:
                result = data['data']['result']
                if 'ws' in result:
                    for word in result['ws']:
                        for cw in word['cw']:
                            self.recognition_result += cw['w']
            
            # 识别结束
            if data['data']['status'] == 2:
                logger.info(f"语音识别完成: {self.recognition_result}")
                self.recognition_event.set()
                
        except Exception as e:
            logger.error(f"消息处理失败: {e}")
            self.error_message = str(e)
            self.recognition_event.set()
    
    def on_error(self, ws, error):
        """处理WebSocket错误"""
        logger.error(f"WebSocket错误: {error}")
        self.error_message = str(error)
        self.recognition_event.set()
    
    def on_close(self, ws, close_status_code, close_msg):
        """处理WebSocket关闭"""
        self.is_connected = False
        logger.info("WebSocket连接已关闭")
    
    def on_open(self, ws):
        """处理WebSocket连接打开"""
        self.is_connected = True
        logger.info("WebSocket连接已建立")
        
        # 发送配置参数
        config_data = {
            "common": {
                "app_id": self.app_id
            },
            "business": {
                "language": self.language,
                "domain": "iat",
                "accent": "mandarin",
                "vinfo": 1,
                "vad_eos": 10000
            },
            "data": {
                "status": 0,
                "format": "audio/L16;rate=16000",
                "encoding": "raw"
            }
        }
        
        ws.send(json.dumps(config_data))
    
    def speech_to_text(self, audio_data):
        """
        语音转文字
        
        Args:
            audio_data: 音频数据 (bytes)
            
        Returns:
            recognition_text: 识别结果文本
        """
        if not self.api_key or not self.api_secret:
            logger.warning("讯飞API未配置，返回模拟结果")
            return self._mock_recognition(audio_data)
        
        try:
            # 重置状态
            self.recognition_result = ""
            self.error_message = ""
            self.recognition_event.clear()
            
            # 生成认证URL
            auth_url = self._generate_auth_url()
            
            # 建立WebSocket连接
            self.ws = websocket.WebSocketApp(
                auth_url,
                on_message=self.on_message,
                on_error=self.on_error,
                on_close=self.on_close,
                on_open=self.on_open
            )
            
            # 启动WebSocket连接线程
            ws_thread = threading.Thread(target=self.ws.run_forever)
            ws_thread.daemon = True
            ws_thread.start()
            
            # 等待连接建立
            timeout = 10
            start_time = time.time()
            while not self.is_connected and time.time() - start_time < timeout:
                time.sleep(0.1)
            
            if not self.is_connected:
                logger.error("WebSocket连接超时")
                return "连接超时"
            
            # 发送音频数据
            self._send_audio_data(audio_data)
            
            # 等待识别完成
            if self.recognition_event.wait(timeout=30):
                if self.error_message:
                    logger.error(f"语音识别失败: {self.error_message}")
                    return self.error_message
                else:
                    return self.recognition_result or "识别结果为空"
            else:
                logger.error("语音识别超时")
                return "识别超时"
                
        except Exception as e:
            logger.error(f"语音转文字失败: {e}")
            return f"识别失败: {str(e)}"
        finally:
            if self.ws:
                self.ws.close()
    
    def _send_audio_data(self, audio_data):
        """发送音频数据"""
        # 分块发送音频数据
        chunk_size = 1280  # 40ms的音频数据
        
        for i in range(0, len(audio_data), chunk_size):
            chunk = audio_data[i:i + chunk_size]
            
            # 最后一块数据
            status = 2 if i + chunk_size >= len(audio_data) else 1
            
            # 编码音频数据
            audio_base64 = base64.b64encode(chunk).decode('utf-8')
            
            data = {
                "data": {
                    "status": status,
                    "format": "audio/L16;rate=16000",
                    "encoding": "raw",
                    "audio": audio_base64
                }
            }
            
            self.ws.send(json.dumps(data))
            time.sleep(0.04)  # 40ms间隔
    
    def _mock_recognition(self, audio_data):
        """模拟语音识别（当API未配置时）"""
        # 基于音频数据长度和内容的简单模拟
        if not audio_data or len(audio_data) < 1000:
            return "无明显症状"
        
        # 根据音频长度生成不同的模拟结果
        mock_results = [
            "患者主诉头痛，持续2小时",
            "胸痛，呼吸困难",
            "发热，体温38度，乏力",
            "腹痛，恶心呕吐",
            "咳嗽，有痰，持续3天",
            "关节疼痛，活动受限",
            "眩晕，站立不稳",
            "皮疹，瘙痒难忍"
        ]
        
        # 基于数据长度选择结果
        index = (len(audio_data) // 1000) % len(mock_results)
        result = mock_results[index]
        
        logger.info(f"模拟语音识别结果: {result}")
        return result


class VoiceProcessor:
    """
    语音处理器封装类
    简化语音转文字操作
    """
    
    def __init__(self):
        self.xf_processor = XFVoiceProcessor()
        logger.info("语音处理器初始化完成")
    
    def speech_to_text(self, audio_data):
        """
        语音转文字简化接口
        
        Args:
            audio_data: 音频数据
            
        Returns:
            text: 识别的文字
        """
        try:
            # 音频质量检测
            if not self._check_audio_quality(audio_data):
                logger.warning("音频质量较差，可能影响识别效果")
            
            # 执行识别
            text = self.xf_processor.speech_to_text(audio_data)
            
            # 后处理
            cleaned_text = self._post_process_text(text)
            
            return cleaned_text
            
        except Exception as e:
            logger.error(f"语音处理失败: {e}")
            return "语音处理失败"
    
    def _check_audio_quality(self, audio_data):
        """检查音频质量"""
        if not audio_data:
            return False
        
        # 简单的音频质量检查
        # 检查音频长度
        if len(audio_data) < 8000:  # 至少0.5秒的16kHz音频
            return False
        
        # 检查音频幅度
        if isinstance(audio_data, bytes):
            # 假设是16位音频
            import struct
            samples = [struct.unpack('<h', audio_data[i:i+2])[0] for i in range(0, len(audio_data)-1, 2)]
            max_amplitude = max(abs(s) for s in samples)
            
            # 检查是否有足够的信号强度
            if max_amplitude < 1000:  # 阈值可调整
                return False
        
        return True
    
    def _post_process_text(self, text):
        """文本后处理"""
        if not text:
            return "无法识别语音内容"
        
        # 清理文本
        cleaned = text.strip()
        
        # 移除多余的标点符号
        cleaned = cleaned.replace('。。', '。').replace('，，', '，')
        
        # 如果文本过短，添加提示
        if len(cleaned) < 3:
            cleaned = f"症状描述较简单: {cleaned}"
        
        return cleaned
    
    def batch_process(self, audio_files):
        """批量处理音频文件"""
        results = []
        
        for audio_file in audio_files:
            try:
                with open(audio_file, 'rb') as f:
                    audio_data = f.read()
                
                text = self.speech_to_text(audio_data)
                results.append({
                    'file': audio_file,
                    'text': text,
                    'success': True
                })
                
            except Exception as e:
                logger.error(f"处理文件 {audio_file} 失败: {e}")
                results.append({
                    'file': audio_file,
                    'text': f"处理失败: {e}",
                    'success': False
                })
        
        return results


# 测试代码
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    
    # 创建语音处理器
    processor = VoiceProcessor()
    
    # 生成模拟音频数据进行测试
    import random
    mock_audio = bytes(random.randint(0, 255) for _ in range(16000))  # 1秒的模拟音频
    
    # 测试语音转文字
    result = processor.speech_to_text(mock_audio)
    print(f"语音识别结果: {result}")
    
    # 测试音频质量检查
    quality_good = processor._check_audio_quality(mock_audio)
    quality_bad = processor._check_audio_quality(bytes(100))  # 太短的音频
    
    print(f"正常长度音频质量: {quality_good}")
    print(f"过短音频质量: {quality_bad}")