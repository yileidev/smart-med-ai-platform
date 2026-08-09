"""
本地离线语音识别模块
基于Vosk实现完全离线的语音转文字功能

适用于Jetson Orin Nano边缘部署
无需网络，完全本地运行
"""

import os
import json
import queue
import logging
import threading
import urllib.request
import zipfile
from pathlib import Path

logger = logging.getLogger(__name__)

# Vosk模型配置（小模型，约42MB，Jetson内存可用）
VOSK_MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip"
VOSK_MODEL_NAME = "vosk-model-small-cn-0.22"

# 跨平台模型目录：根据操作系统自动选择路径
import platform
if platform.system() == "Windows":
    VOSK_MODEL_DIR = Path("c:/vosk_model")
else:
    # Linux/Jetson: 使用项目内的 preprocessing/models/vosk 目录
    VOSK_MODEL_DIR = Path(__file__).parent / "models" / "vosk"

# 尝试导入vosk
try:
    import vosk
    import sounddevice as sd
    VOSK_AVAILABLE = True
except ImportError:
    VOSK_AVAILABLE = False
    logger.warning("Vosk未安装，请运行: pip install vosk sounddevice")


class LocalVoiceRecognizer:
    """
    本地离线语音识别器
    基于Vosk引擎，完全离线运行
    """
    
    def __init__(self):
        self.model = None
        self.recognizer = None
        self.vosk_sample_rate = 16000  # Vosk需要的16000Hz
        self.device_sample_rate = 48000  # 麦克风实际采样率（会自动检测）
        self.is_initialized = False
        self.audio_queue = queue.Queue()
        
        if VOSK_AVAILABLE:
            self._init_model()
    
    def _init_model(self):
        """初始化Vosk模型"""
        try:
            model_path = VOSK_MODEL_DIR / VOSK_MODEL_NAME
            
            # 检查模型是否存在
            if not model_path.exists():
                logger.info("Vosk中文模型不存在，正在下载...")
                self._download_model()
            
            # 加载模型
            logger.info(f"加载Vosk模型: {model_path}")
            vosk.SetLogLevel(-1)  # 关闭Vosk日志
            self.model = vosk.Model(str(model_path))
            self.recognizer = vosk.KaldiRecognizer(self.model, self.vosk_sample_rate)
            self.recognizer.SetWords(True)
            self.is_initialized = True
            logger.info("✓ Vosk离线语音识别模型加载成功")
            
        except Exception as e:
            logger.error(f"Vosk模型初始化失败: {e}")
            self.is_initialized = False
    
    def _download_model(self):
        """下载Vosk中文模型"""
        try:
            # 创建目录
            VOSK_MODEL_DIR.mkdir(parents=True, exist_ok=True)
            
            zip_path = VOSK_MODEL_DIR / "model.zip"
            
            logger.info(f"正在下载Vosk中文模型...")
            logger.info(f"下载地址: {VOSK_MODEL_URL}")
            logger.info("模型大小约42MB，请耐心等待...")
            
            # 下载模型
            urllib.request.urlretrieve(VOSK_MODEL_URL, zip_path, self._download_progress)
            
            # 解压模型
            logger.info("正在解压模型...")
            with zipfile.ZipFile(zip_path, 'r') as zip_ref:
                zip_ref.extractall(VOSK_MODEL_DIR)
            
            # 删除zip文件
            zip_path.unlink()
            
            logger.info("✓ Vosk模型下载完成")
            
        except Exception as e:
            logger.error(f"模型下载失败: {e}")
            raise
    
    def _download_progress(self, block_num, block_size, total_size):
        """下载进度回调"""
        downloaded = block_num * block_size
        percent = min(100, downloaded * 100 // total_size)
        if block_num % 100 == 0:
            logger.info(f"下载进度: {percent}%")
    
    def _audio_callback(self, indata, frames, time_info, status):
        """音频回调函数"""
        if status:
            logger.warning(f"音频状态: {status}")
        self.audio_queue.put(bytes(indata))
    
    def _find_working_sample_rate(self, device_index):
        """查找麦克风支持的采样率"""
        # 常见的采样率，优先尝试高采样率
        test_rates = [48000, 44100, 32000, 22050, 16000]
        
        for rate in test_rates:
            try:
                with sd.RawInputStream(
                    device=device_index,
                    samplerate=rate,
                    blocksize=1024,
                    dtype='int16',
                    channels=1
                ):
                    pass
                logger.info(f"麦克风支持采样率: {rate}Hz")
                return rate
            except:
                continue
        return None
    
    def _resample_audio(self, audio_data, from_rate, to_rate):
        """重采样音频数据"""
        import numpy as np
        
        if from_rate == to_rate:
            return audio_data
        
        # 计算重采样比例
        ratio = to_rate / from_rate
        
        # 转换为numpy数组
        audio_array = np.frombuffer(audio_data, dtype=np.int16)
        
        # 简单的线性重采样
        new_length = int(len(audio_array) * ratio)
        indices = np.linspace(0, len(audio_array) - 1, new_length).astype(int)
        resampled = audio_array[indices]
        
        return resampled.tobytes()
    
    def recognize_from_mic(self, duration=5, callback=None):
        """
        从麦克风录音并识别
        
        Args:
            duration: 录音时长（秒）
            callback: 状态回调函数
            
        Returns:
            识别的文本
        """
        if not self.is_initialized:
            return "语音识别模型未初始化"
        
        if not VOSK_AVAILABLE:
            return "Vosk库未安装"
        
        try:
            # 清空队列
            while not self.audio_queue.empty():
                self.audio_queue.get()
            
            # 重置识别器
            self.recognizer = vosk.KaldiRecognizer(self.model, self.vosk_sample_rate)
            
            # === 刷新音频设备列表并检测采样率 ===
            input_device = None
            try:
                if callback:
                    callback("初始化麦克风...")
                
                # 重新扫描音频设备
                sd._terminate()
                sd._initialize()
                
                # 查找可用的输入设备
                devices = sd.query_devices()
                for i, d in enumerate(devices):
                    if d['max_input_channels'] > 0:
                        input_device = i
                        logger.info(f"找到输入设备: {d['name']}")
                        break
                
                if input_device is None:
                    return "未找到麦克风设备"
                
                # 检测麦克风支持的采样率
                self.device_sample_rate = self._find_working_sample_rate(input_device)
                if self.device_sample_rate is None:
                    return "无法找到麦克风支持的采样率"
                
                logger.info(f"使用采样率: {self.device_sample_rate}Hz")
                
            except Exception as e:
                logger.warning(f"音频设备初始化失败: {e}")
                return f"麦克风初始化失败: {e}"
            
            if callback:
                callback("正在录音...")
            
            # 开始录音（使用麦克风支持的采样率）
            with sd.RawInputStream(
                device=input_device,
                samplerate=self.device_sample_rate,
                blocksize=int(self.device_sample_rate * 0.5),  # 0.5秒的块大小
                dtype='int16',
                channels=1,
                callback=self._audio_callback
            ):
                import time
                start_time = time.time()
                
                while time.time() - start_time < duration:
                    try:
                        data = self.audio_queue.get(timeout=0.5)
                        
                        # 如果采样率不是16000Hz，需要重采样
                        if self.device_sample_rate != self.vosk_sample_rate:
                            data = self._resample_audio(data, self.device_sample_rate, self.vosk_sample_rate)
                        
                        if self.recognizer.AcceptWaveform(data):
                            # 获取完整识别结果
                            result = json.loads(self.recognizer.Result())
                            if result.get('text'):
                                if callback:
                                    callback("识别完成")
                                return result['text']
                    except queue.Empty:
                        continue
                
                # 获取最终结果
                if callback:
                    callback("处理中...")
                    
                final_result = json.loads(self.recognizer.FinalResult())
                text = final_result.get('text', '')
                
                if callback:
                    callback("识别完成" if text else "未检测到语音")
                
                return text if text else "未检测到语音内容"
                
        except Exception as e:
            logger.error(f"语音识别失败: {e}")
            if callback:
                callback("识别失败")
            return f"识别失败: {str(e)}"
    
    def recognize_from_file(self, audio_file):
        """
        从音频文件识别
        
        Args:
            audio_file: 音频文件路径
            
        Returns:
            识别的文本
        """
        if not self.is_initialized:
            return "语音识别模型未初始化"
        
        try:
            import wave
            
            wf = wave.open(audio_file, "rb")
            
            if wf.getnchannels() != 1 or wf.getsampwidth() != 2:
                return "音频格式不支持，需要16bit单声道WAV"
            
            rec = vosk.KaldiRecognizer(self.model, wf.getframerate())
            rec.SetWords(True)
            
            results = []
            while True:
                data = wf.readframes(4000)
                if len(data) == 0:
                    break
                if rec.AcceptWaveform(data):
                    result = json.loads(rec.Result())
                    if result.get('text'):
                        results.append(result['text'])
            
            # 最终结果
            final = json.loads(rec.FinalResult())
            if final.get('text'):
                results.append(final['text'])
            
            return ' '.join(results) if results else "未识别到内容"
            
        except Exception as e:
            logger.error(f"文件识别失败: {e}")
            return f"识别失败: {str(e)}"
    
    def is_available(self):
        """检查语音识别是否可用"""
        return VOSK_AVAILABLE and self.is_initialized


# 全局实例
_recognizer = None

def get_recognizer():
    """获取语音识别器单例"""
    global _recognizer
    if _recognizer is None:
        _recognizer = LocalVoiceRecognizer()
    return _recognizer


# 测试代码
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    
    print("=" * 50)
    print("本地离线语音识别测试")
    print("=" * 50)
    
    recognizer = LocalVoiceRecognizer()
    
    if recognizer.is_available():
        print("\n模型已就绪，开始录音测试（5秒）...")
        print("请说话...")
        
        def status_callback(status):
            print(f"状态: {status}")
        
        result = recognizer.recognize_from_mic(duration=5, callback=status_callback)
        print(f"\n识别结果: {result}")
    else:
        print("语音识别不可用，请检查Vosk安装")
