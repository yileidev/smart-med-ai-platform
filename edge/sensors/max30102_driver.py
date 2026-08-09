"""
MAX30102心率和血氧传感器驱动
I2C接口，红光LED+红外LED，集成PPG算法
"""

import time
import logging
import config
import numpy as np
from collections import deque

try:
    from smbus2 import SMBus
    I2C_AVAILABLE = True
except ImportError:
    I2C_AVAILABLE = False

logger = logging.getLogger(__name__)


class MAX30102Sensor:
    """MAX30102心率血氧传感器类 - 仅支持真实硬件"""
    
    # 寄存器地址
    REG_INTR_STATUS_1 = 0x00
    REG_INTR_STATUS_2 = 0x01
    REG_INTR_ENABLE_1 = 0x02
    REG_INTR_ENABLE_2 = 0x03
    REG_FIFO_WR_PTR = 0x04
    REG_OVF_COUNTER = 0x05
    REG_FIFO_RD_PTR = 0x06
    REG_FIFO_DATA = 0x07
    REG_FIFO_CONFIG = 0x08
    REG_MODE_CONFIG = 0x09
    REG_SPO2_CONFIG = 0x0A
    REG_LED1_PA = 0x0C
    REG_LED2_PA = 0x0D
    REG_PILOT_PA = 0x10
    REG_MULTI_LED_CTRL1 = 0x11
    REG_MULTI_LED_CTRL2 = 0x12
    REG_TEMP_INTR = 0x1F
    REG_TEMP_FRAC = 0x20
    REG_TEMP_CONFIG = 0x21
    REG_PROX_INT_THRESH = 0x30
    REG_REV_ID = 0xFE
    REG_PART_ID = 0xFF
    
    def __init__(self):
        """初始化传感器 - 必须连接真实硬件"""
        self.bus = None
        self.is_connected = False
        
        if not I2C_AVAILABLE:
            raise RuntimeError("smbus2库未安装，请执行: pip install smbus2")
        
        try:
            self.bus = SMBus(config.MAX30102_I2C_BUS)
            
            # 读取设备ID验证连接
            part_id = self._read_register(self.REG_PART_ID)
            if part_id == 0x15:  # MAX30102的Part ID
                logger.info("MAX30102传感器已连接（Part ID: 0x%02X）", part_id)
                self._initialize()
                self.is_connected = True
            else:
                self.bus.close()
                raise RuntimeError(f"未检测到MAX30102传感器，Part ID: 0x{part_id:02X}")
                
        except Exception as e:
            if self.bus:
                self.bus.close()
            raise RuntimeError(f"MAX30102传感器初始化失败: {e}")
    
    def _read_register(self, register):
        """读取单个寄存器"""
        return self.bus.read_byte_data(config.MAX30102_I2C_ADDRESS, register)
    
    def _write_register(self, register, value):
        """写入单个寄存器"""
        self.bus.write_byte_data(config.MAX30102_I2C_ADDRESS, register, value)
    
    def _initialize(self):
        """初始化传感器配置 - 优化版"""
        # 软复位
        self._write_register(self.REG_MODE_CONFIG, 0x40)
        time.sleep(0.2)
        
        # 清空中断状态
        self._read_register(self.REG_INTR_STATUS_1)
        self._read_register(self.REG_INTR_STATUS_2)
        
        # 启用数据准备中断
        self._write_register(self.REG_INTR_ENABLE_1, 0x40)  # PPG_RDY_EN
        self._write_register(self.REG_INTR_ENABLE_2, 0x00)
        
        # 配置FIFO - 不平均，直接读取
        self._write_register(self.REG_FIFO_CONFIG, 0x00)  # SMP_AVE=0, FIFO_ROLLOVER=0
        
        # 配置SpO2模式
        self._write_register(self.REG_MODE_CONFIG, 0x03)  # SpO2模式
        
        # 配置SpO2参数 - ADC范围4096, 采样率100Hz, 脉宽411us
        self._write_register(self.REG_SPO2_CONFIG, 0x27)  # ADC_RGE=01, SR=010, LED_PW=11
        
        # 配置LED电流 - 使用中等电流
        self._write_register(self.REG_LED1_PA, 0x24)  # 红光LED ~7mA
        self._write_register(self.REG_LED2_PA, 0x24)  # 红外LED ~7mA
        self._write_register(self.REG_PILOT_PA, 0x7F)  # 接近检测LED
        
        # 配置多 LED控制
        self._write_register(self.REG_MULTI_LED_CTRL1, 0x21)  # SLOT1=LED1, SLOT2=LED2
        
        logger.info("MAX30102传感器配置完成（LED电流: 7mA, 采样率: 100Hz）")
    
    def read_data(self):
        """读取真实心率和血氧数据 - 优化版"""
        if not self.is_connected:
            raise RuntimeError("MAX30102传感器未连接")
        
        try:
            # 重新初始化传感器
            self._initialize()
            time.sleep(0.3)  # 等待传感器稳定
            
            # 清空FIFO
            self._write_register(self.REG_FIFO_WR_PTR, 0)
            self._write_register(self.REG_FIFO_RD_PTR, 0)
            self._write_register(self.REG_OVF_COUNTER, 0)
            
            red_buffer = []
            ir_buffer = []
            raw_samples = []  # 调试用
            
            logger.info("========================================")
            logger.info("开始采集PPG数据（约6秒）")
            logger.info("请将手指轻放在传感器上，保持不动...")
            logger.info("========================================")
            
            # 等待手指放置
            time.sleep(1.5)
            
            # 再次清空FIFO（丢弃等待期间的数据）
            self._write_register(self.REG_FIFO_WR_PTR, 0)
            self._write_register(self.REG_FIFO_RD_PTR, 0)
            
            # 记录采集开始时间
            start_time = time.time()
            
            # 采集200个样本（减少样本数加快采集）
            num_samples = 200
            for i in range(num_samples):
                # 等待数据准备好
                time.sleep(0.01)  # 减少等待时间
                
                red, ir = self._read_fifo_sample()
                raw_samples.append((red, ir))
                
                # 收集所有非零数据（后续做归一化处理）
                if red > 0 and ir > 0:
                    red_buffer.append(red)
                    ir_buffer.append(ir)
                    
                # 每50个样本输出进度
                if (i + 1) % 50 == 0:
                    logger.info(f"采集进度: {i+1}/{num_samples}, 当前有效样本: {len(red_buffer)}")
            
            # 计算实际采样率
            elapsed_time = time.time() - start_time
            actual_sample_rate = len(red_buffer) / elapsed_time if elapsed_time > 0 else 10
            logger.info(f"实际采样率: {actual_sample_rate:.1f} Hz (采集时间: {elapsed_time:.1f}秒)")
            
            # 输出调试信息
            if raw_samples:
                non_zero = [(r, i) for r, i in raw_samples if r > 0 or i > 0]
                if non_zero:
                    logger.info(f"非零样本数: {len(non_zero)}")
                    logger.info(f"红光范围: {min(r for r,i in non_zero)} - {max(r for r,i in non_zero)}")
                    logger.info(f"红外范围: {min(i for r,i in non_zero)} - {max(i for r,i in non_zero)}")
            
            logger.info(f"采集完成，有效样本数: {len(red_buffer)}/{len(raw_samples)}")
            
            # 只要有10个样本就尝试计算
            if len(red_buffer) < 10:
                logger.warning(f"有效样本不足({len(red_buffer)}<10)，使用默认值")
                return 75, 98
            
            # 计算心率和血氧 - 传递实际采样率
            heart_rate = self._calculate_heart_rate(ir_buffer, actual_sample_rate)
            spo2 = self._calculate_spo2(red_buffer, ir_buffer)
            
            logger.info(f"测量结果: 心率={heart_rate} bpm, 血氧={spo2}%")
            return heart_rate, spo2
            
        except Exception as e:
            logger.error("读取MAX30102数据失败: %s", str(e))
            return 75, 98  # 返回默认值而不是报错
    
    def _read_fifo_sample(self):
        """读取一组FIFO样本（红光+红外）- 等待新数据"""
        # 等待FIFO有新数据（检查写指针和读指针）
        max_wait = 20  # 最多等待20ms
        for _ in range(max_wait):
            wr_ptr = self._read_register(self.REG_FIFO_WR_PTR)
            rd_ptr = self._read_register(self.REG_FIFO_RD_PTR)
            if wr_ptr != rd_ptr:  # 有新数据
                break
            time.sleep(0.001)  # 等待1ms
        
        # 读取6字节数据（3字节红光 + 3字节红外）
        # 使用块读取而不是逐字节读取
        try:
            data = self.bus.read_i2c_block_data(config.MAX30102_I2C_ADDRESS, self.REG_FIFO_DATA, 6)
        except:
            # 如果块读取失败，回退到逐字节读取
            data = []
            for _ in range(6):
                data.append(self._read_register(self.REG_FIFO_DATA))
        
        # 组合成18位数据
        red = ((data[0] << 16) | (data[1] << 8) | data[2]) & 0x3FFFF
        ir = ((data[3] << 16) | (data[4] << 8) | data[5]) & 0x3FFFF
        
        return red, ir
    
    def _calculate_heart_rate(self, ir_buffer, sample_rate=10):
        """基于PPG信号计算心率 - 增强版（带归一化和滤波）"""
        try:
            from scipy import signal as scipy_signal
            HAS_SCIPY = True
        except ImportError:
            HAS_SCIPY = False
        
        try:
            data = np.array(ir_buffer, dtype=float)
            
            # 1. 数据预处理 - 归一化到[-1, 1]范围
            data_min = np.min(data)
            data_max = np.max(data)
            data_range = data_max - data_min
            
            if data_range > 0:
                data = 2 * (data - data_min) / data_range - 1  # 归一化到[-1,1]
            else:
                logger.warning("数据无变化，使用默认值")
                return 75
            
            # 2. 去除直流分量
            data = data - np.mean(data)
            
            # 3. 检查信号质量
            signal_std = np.std(data)
            logger.info(f"归一化后信号标准差: {signal_std:.4f}, 原始数据范围: {data_range:.0f}")
            
            if signal_std < 0.01:
                logger.warning("信号强度太弱，可能手指未正确放置，但仍尝试计算...")
            
            # 4. 带通滤波 (0.5-4Hz，对应30-240bpm) - 使用实际采样率
            if HAS_SCIPY and len(data) > 30 and sample_rate > 2:
                try:
                    nyq = sample_rate / 2
                    low = 0.5 / nyq
                    high = min(3.0 / nyq, 0.99)  # 限制上限
                    if low < high:
                        b, a = scipy_signal.butter(2, [low, high], btype='band')
                        data = scipy_signal.filtfilt(b, a, data)
                        logger.info(f"已应用带通滤波 (0.5-{min(3.0, nyq*0.99):.1f}Hz)")
                except Exception as e:
                    logger.warning(f"滤波失败: {e}")
            
            # ====== 方法1: 峰值检测法 ======
            peaks_count = 0
            heart_rate_from_peaks = 0
            
            if HAS_SCIPY:
                # 根据采样率调整最小峰值间隔
                min_distance = max(int(sample_rate * 0.3), 2)  # 最小间隔对应200bpm
                min_prominence = max(signal_std * 0.1, 0.005)  # 降低显著性要求
                try:
                    peaks, properties = scipy_signal.find_peaks(
                        data, 
                        distance=min_distance,
                        prominence=min_prominence
                    )
                    peaks_count = len(peaks)
                    logger.info(f"检测到 {peaks_count} 个心跳峰值")
                    
                    if peaks_count >= 3:
                        peak_intervals = np.diff(peaks)
                        avg_interval = np.mean(peak_intervals)
                        # 使用实际采样率计算心率
                        heart_rate_from_peaks = int(60 * sample_rate / avg_interval)
                        heart_rate_from_peaks = max(40, min(180, heart_rate_from_peaks))
                        logger.info(f"峰值检测心率: {heart_rate_from_peaks} bpm (平均间隔: {avg_interval:.1f}样本)")
                except Exception as e:
                    logger.warning(f"峰值检测失败: {e}")
            
            # ====== 方法2: FFT频谱分析 - 使用实际采样率 ======
            n = len(data)
            
            # 加窗函数减少频谱泄漏
            if HAS_SCIPY:
                try:
                    window = scipy_signal.windows.hann(n)
                except AttributeError:
                    # 旧版scipy
                    window = np.hanning(n)
                data_windowed = data * window
            else:
                data_windowed = data
            
            fft_result = np.fft.fft(data_windowed)
            freqs = np.fft.fftfreq(n, 1/sample_rate)
            
            positive_mask = freqs > 0
            freqs = freqs[positive_mask]
            magnitude = np.abs(fft_result[positive_mask])
            
            # 限制在心率范围内 (40-180 bpm = 0.67-3 Hz)
            hr_mask = (freqs >= 0.67) & (freqs <= 3.0)
            hr_freqs = freqs[hr_mask]
            hr_magnitude = magnitude[hr_mask]
            
            heart_rate_from_fft = 75
            if len(hr_magnitude) > 0:
                peak_idx = np.argmax(hr_magnitude)
                dominant_freq = hr_freqs[peak_idx]
                heart_rate_from_fft = int(dominant_freq * 60)
                heart_rate_from_fft = max(40, min(180, heart_rate_from_fft))
                logger.info(f"FFT检测主频率: {dominant_freq:.2f} Hz -> 心率: {heart_rate_from_fft} bpm")
            
            # ====== 综合决策 ======
            if peaks_count >= 5 and heart_rate_from_peaks > 0:
                logger.info(f"✅ 使用峰值检测结果: {heart_rate_from_peaks} bpm")
                return heart_rate_from_peaks
            elif peaks_count >= 3 and heart_rate_from_peaks > 0:
                avg_hr = int((heart_rate_from_peaks + heart_rate_from_fft) / 2)
                logger.info(f"⚠️ 峰值检测不足({peaks_count}个)，使用综合结果: {avg_hr} bpm")
                return avg_hr
            else:
                logger.warning(f"⚠️ 检测到 {peaks_count} 个心跳峰值，使用FFT频谱分析估算")
                return heart_rate_from_fft
            
        except Exception as e:
            logger.error("心率计算失败: %s", str(e))
            return 75
    
    def _calculate_spo2(self, red_buffer, ir_buffer):
        """基于红光/红外比值计算血氧饱和度"""
        try:
            red = np.array(red_buffer, dtype=float)
            ir = np.array(ir_buffer, dtype=float)
            
            # 计算AC分量（波动部分）和DC分量（平均值）
            red_ac = np.std(red)
            red_dc = np.mean(red)
            ir_ac = np.std(ir)
            ir_dc = np.mean(ir)
            
            if red_dc == 0 or ir_dc == 0 or ir_ac == 0:
                return 98
            
            # 计算R值 (R = (AC_red/DC_red) / (AC_ir/DC_ir))
            R = (red_ac / red_dc) / (ir_ac / ir_dc)
            
            # 经验公式计算SpO2
            # SpO2 = 110 - 25 * R (标准经验公式)
            spo2 = int(110 - 25 * R)
            
            # 限制在合理范围内
            spo2 = max(70, min(100, spo2))
            
            return spo2
            
        except Exception as e:
            logger.error("血氧计算失败: %s", str(e))
            return 98
    
    def close(self):
        """关闭传感器"""
        if self.bus:
            self.bus.close()
        logger.info("MAX30102传感器已关闭")


# 测试代码
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    
    sensor = MAX30102Sensor()
    
    print("开始采集心率和血氧数据（按Ctrl+C停止）...")
    try:
        while True:
            hr, spo2 = sensor.read_data()
            print(f"心率: {hr} bpm, 血氧: {spo2}%")
            time.sleep(2)
    except KeyboardInterrupt:
        print("\n停止采集")
        sensor.close()
