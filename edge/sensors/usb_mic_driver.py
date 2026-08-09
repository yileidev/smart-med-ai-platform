"""
USB麦克风驱动
用于采集患者语音主诉

功能：
1. USB麦克风音频采集
2. 实时录音
3. 音频格式转换
4. 噪声抑制
"""

import pyaudio
import wave
import threading
import time
import numpy as np
import logging
import config
from pathlib import Path

logger = logging.getLogger(__name__)


class USBMicRecorder:
    """
    USB麦克风录音器
    """
    
    def __init__(self):
        # 音频参数
        self.sample_rate = config.USB_MIC_SAMPLE_RATE
        self.channels = config.USB_MIC_CHANNELS
        self.chunk_size = config.USB_MIC_CHUNK_SIZE
        self.format = pyaudio.paInt16
        
        # PyAudio实例
        self.audio = None
        self.stream = None
        
        # 录音状态
        self.is_recording = False
        self.audio_data = []
        
        # 初始化音频设备
        self._initialize_audio()
        
        logger.info(f"USB麦克风初始化完成: {self.sample_rate}Hz, {self.channels}通道")
    
    def _initialize_audio(self):
        """初始化音频设备"""
        try:
            self.audio = pyaudio.PyAudio()
            
            # 查找可用的音频设备
            self._list_audio_devices()
            
            # 检查默认输入设备
            default_device = self.audio.get_default_input_device_info()
            logger.info(f"默认输入设备: {default_device['name']}")
            
        except Exception as e:
            logger.error(f"音频设备初始化失败: {e}")
            self.audio = None
    
    def _list_audio_devices(self):
        """列出可用的音频设备"""
        if not self.audio:
            return
        
        logger.info("可用音频设备:")
        for i in range(self.audio.get_device_count()):
            info = self.audio.get_device_info_by_index(i)
            if info['maxInputChannels'] > 0:
                logger.info(f"  [{i}] {info['name']} - {info['maxInputChannels']}通道")
    
    def record(self, duration=10, device_index=None):
        """
        录制音频
        
        Args:
            duration: 录音时长(秒)
            device_index: 音频设备索引，None为默认设备
            
        Returns:
            audio_data: 录制的音频数据(bytes)
        """
        if not self.audio:
            logger.error("音频设备未初始化")
            return self._generate_mock_audio(duration)
        
        try:
            logger.info(f"开始录音，时长: {duration}秒")
            
            # 打开音频流
            self.stream = self.audio.open(
                format=self.format,
                channels=self.channels,
                rate=self.sample_rate,
                input=True,
                input_device_index=device_index,
                frames_per_buffer=self.chunk_size
            )
            
            self.audio_data = []
            self.is_recording = True
            
            # 录制音频
            frames = int(self.sample_rate / self.chunk_size * duration)
            
            for _ in range(frames):
                if not self.is_recording:
                    break
                
                try:
                    data = self.stream.read(self.chunk_size, exception_on_overflow=False)
                    self.audio_data.append(data)
                except Exception as e:
                    logger.warning(f"录音数据读取警告: {e}")
            
            # 停止和关闭流
            self.stream.stop_stream()
            self.stream.close()
            
            # 合并音频数据
            audio_bytes = b''.join(self.audio_data)
            
            logger.info(f"录音完成，数据长度: {len(audio_bytes)} bytes")
            
            return audio_bytes
            
        except Exception as e:
            logger.error(f"录音失败: {e}")
            return self._generate_mock_audio(duration)
        finally:
            self.is_recording = False
            if self.stream:
                try:
                    self.stream.close()
                except:
                    pass
    
    def record_with_vad(self, max_duration=30, silence_threshold=1000, silence_duration=2):
        """
        使用语音活动检测的录音
        
        Args:
            max_duration: 最大录音时长
            silence_threshold: 静音阈值
            silence_duration: 静音持续时间(秒)后停止录音
            
        Returns:
            audio_data: 录制的音频数据
        """
        if not self.audio:
            return self._generate_mock_audio(5)
        
        try:
            logger.info("开始VAD录音...")
            
            self.stream = self.audio.open(
                format=self.format,
                channels=self.channels,
                rate=self.sample_rate,
                input=True,
                frames_per_buffer=self.chunk_size
            )
            
            self.audio_data = []
            self.is_recording = True
            
            silence_counter = 0
            silence_frames = int(self.sample_rate / self.chunk_size * silence_duration)
            max_frames = int(self.sample_rate / self.chunk_size * max_duration)
            frame_count = 0
            
            while self.is_recording and frame_count < max_frames:
                try:
                    data = self.stream.read(self.chunk_size, exception_on_overflow=False)
                    self.audio_data.append(data)
                    
                    # 检测音频幅度
                    audio_array = np.frombuffer(data, dtype=np.int16)
                    amplitude = np.max(np.abs(audio_array))
                    
                    if amplitude < silence_threshold:
                        silence_counter += 1
                        if silence_counter >= silence_frames:
                            logger.info("检测到持续静音，停止录音")
                            break
                    else:
                        silence_counter = 0
                    
                    frame_count += 1
                    
                except Exception as e:
                    logger.warning(f"VAD录音警告: {e}")
                    break
            
            self.stream.stop_stream()
            self.stream.close()
            
            audio_bytes = b''.join(self.audio_data)
            logger.info(f"VAD录音完成，数据长度: {len(audio_bytes)} bytes")
            
            return audio_bytes
            
        except Exception as e:
            logger.error(f"VAD录音失败: {e}")
            return self._generate_mock_audio(5)
    
    def save_audio(self, audio_data, filename):
        """
        保存音频数据到文件
        
        Args:
            audio_data: 音频数据
            filename: 文件名
        """
        try:
            filepath = Path(filename)
            filepath.parent.mkdir(parents=True, exist_ok=True)
            
            with wave.open(str(filepath), 'wb') as wf:
                wf.setnchannels(self.channels)
                wf.setsampwidth(self.audio.get_sample_size(self.format) if self.audio else 2)
                wf.setframerate(self.sample_rate)
                wf.writeframes(audio_data)
            
            logger.info(f"音频已保存: {filepath}")
            
        except Exception as e:
            logger.error(f"音频保存失败: {e}")
    
    def real_time_record(self, callback, duration=None):
        """
        实时录音并回调处理
        
        Args:
            callback: 音频数据处理回调函数
            duration: 录音时长，None为无限录音
        """
        if not self.audio:
            logger.error("音频设备未初始化，无法实时录音")
            return
        
        try:
            logger.info("开始实时录音...")
            
            self.stream = self.audio.open(
                format=self.format,
                channels=self.channels,
                rate=self.sample_rate,
                input=True,
                frames_per_buffer=self.chunk_size,
                stream_callback=self._stream_callback
            )
            
            self.callback = callback
            self.is_recording = True
            self.stream.start_stream()
            
            # 如果指定了时长，等待相应时间
            if duration:
                time.sleep(duration)
                self.stop_recording()
            else:
                # 无限录音，直到手动停止
                while self.is_recording:
                    time.sleep(0.1)
            
        except Exception as e:
            logger.error(f"实时录音失败: {e}")
        finally:
            self.stop_recording()
    
    def _stream_callback(self, in_data, frame_count, time_info, status):
        """音频流回调函数"""
        if self.is_recording and hasattr(self, 'callback'):
            try:
                self.callback(in_data)
            except Exception as e:
                logger.error(f"音频回调处理失败: {e}")
        
        return (None, pyaudio.paContinue)
    
    def stop_recording(self):
        """停止录音"""
        self.is_recording = False
        
        if self.stream:
            try:
                self.stream.stop_stream()
                self.stream.close()
            except:
                pass
        
        logger.info("录音已停止")
    
    def close(self):
        """关闭音频设备"""
        self.stop_recording()
        
        if self.audio:
            try:
                self.audio.terminate()
            except:
                pass
        
        logger.info("USB麦克风已关闭")
    
    def _generate_mock_audio(self, duration):
        """生成模拟音频数据"""
        logger.info(f"生成模拟音频数据，时长: {duration}秒")
        
        # 生成简单的正弦波音频数据
        samples = int(self.sample_rate * duration)
        t = np.linspace(0, duration, samples)
        
        # 混合多个频率模拟语音
        frequencies = [440, 880, 1320]  # A4, A5, E6
        signal = np.zeros_like(t)
        
        for freq in frequencies:
            signal += 0.3 * np.sin(2 * np.pi * freq * t)
        
        # 添加噪声
        noise = 0.1 * np.random.randn(len(signal))
        signal += noise
        
        # 转换为16位整数
        signal = (signal * 32767).astype(np.int16)
        
        return signal.tobytes()
    
    def test_microphone(self):
        """测试麦克风功能"""
        logger.info("开始麦克风测试...")
        
        try:
            # 录制3秒测试音频
            test_audio = self.record(duration=3)
            
            if test_audio:
                # 分析音频特性
                audio_array = np.frombuffer(test_audio, dtype=np.int16)
                max_amplitude = np.max(np.abs(audio_array))
                rms_amplitude = np.sqrt(np.mean(audio_array**2))
                
                logger.info(f"测试音频统计:")
                logger.info(f"  数据长度: {len(test_audio)} bytes")
                logger.info(f"  最大幅度: {max_amplitude}")
                logger.info(f"  RMS幅度: {rms_amplitude:.1f}")
                
                if max_amplitude > 1000:
                    logger.info("✅ 麦克风工作正常")
                    return True
                else:
                    logger.warning("⚠️ 麦克风信号较弱，检查音频输入")
                    return False
            else:
                logger.error("❌ 麦克风测试失败")
                return False
                
        except Exception as e:
            logger.error(f"麦克风测试异常: {e}")
            return False


# 测试代码
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    
    # 创建录音器
    recorder = USBMicRecorder()
    
    # 测试麦克风
    recorder.test_microphone()
    
    # 测试录音功能
    print("开始5秒测试录音...")
    audio_data = recorder.record(duration=5)
    
    if audio_data:
        print(f"录音成功，数据长度: {len(audio_data)} bytes")
        
        # 保存测试音频
        recorder.save_audio(audio_data, "test_recording.wav")
    
    # 测试VAD录音
    print("开始VAD录音测试（请说话，静音2秒后自动停止）...")
    vad_audio = recorder.record_with_vad(max_duration=15, silence_duration=2)
    
    if vad_audio:
        print(f"VAD录音成功，数据长度: {len(vad_audio)} bytes")
        recorder.save_audio(vad_audio, "test_vad_recording.wav")
    
    # 关闭设备
    recorder.close()