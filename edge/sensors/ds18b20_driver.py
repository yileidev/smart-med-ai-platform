"""
DS18B20数字温度传感器驱动
1-Wire协议，测温范围-55°C~+125°C，精度±0.5°C
"""

import time
import glob
import logging
import config

logger = logging.getLogger(__name__)


class DS18B20Sensor:
    """DS18B20体温传感器类"""
    
    def __init__(self):
        """初始化传感器"""
        self.device_path = None
        self.device_file = None
        
        # 查找1-Wire设备
        self._find_device()
        
        if not self.device_file:
            logger.warning("DS18B20传感器未找到，将使用模拟数据")
            self.simulation_mode = True
        else:
            self.simulation_mode = False
            logger.info("DS18B20传感器已连接: %s", self.device_file)
    
    def _find_device(self):
        """查找1-Wire设备"""
        try:
            # 在/sys/bus/w1/devices/目录下查找28-开头的设备
            devices = glob.glob(config.DS18B20_DEVICE_PATH + '28-*')
            if devices:
                self.device_path = devices[0]
                self.device_file = self.device_path + '/w1_slave'
        except Exception as e:
            logger.error("查找DS18B20设备失败: %s", str(e))
    
    def _read_temp_raw(self):
        """读取原始温度数据"""
        try:
            with open(self.device_file, 'r') as f:
                lines = f.readlines()
            return lines
        except Exception as e:
            logger.error("读取DS18B20原始数据失败: %s", str(e))
            return None
    
    def read_temperature(self):
        """读取温度值（摄氏度）"""
        if self.simulation_mode:
            # 模拟模式：返回模拟数据
            import random
            simulated_temp = 36.5 + random.uniform(-1.5, 1.5)
            return round(simulated_temp, 1)
        
        try:
            lines = self._read_temp_raw()
            if not lines:
                return 36.5  # 返回默认值
            
            # 检查CRC校验
            while lines[0].strip()[-3:] != 'YES':
                time.sleep(0.2)
                lines = self._read_temp_raw()
            
            # 提取温度值
            equals_pos = lines[1].find('t=')
            if equals_pos != -1:
                temp_string = lines[1][equals_pos+2:]
                temp_c = float(temp_string) / 1000.0
                return round(temp_c, 1)
            
            return 36.5
            
        except Exception as e:
            logger.error("读取DS18B20温度失败: %s", str(e))
            return 36.5
    
    def close(self):
        """关闭传感器"""
        logger.info("DS18B20传感器已关闭")


# 测试代码
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    
    sensor = DS18B20Sensor()
    
    print("开始采集温度数据（按Ctrl+C停止）...")
    try:
        while True:
            temp = sensor.read_temperature()
            print(f"体温: {temp}°C")
            time.sleep(2)
    except KeyboardInterrupt:
        print("\n停止采集")
        sensor.close()
