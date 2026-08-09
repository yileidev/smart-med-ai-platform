"""
卡尔曼滤波器
用于边缘端传感器数据去噪

功能：
1. 传感器信号实时滤波
2. 状态估计和预测
3. 噪声抑制
4. 异常值检测
"""

import numpy as np
import logging
import config

logger = logging.getLogger(__name__)


class KalmanFilter:
    """
    一维卡尔曼滤波器
    适用于生理参数信号滤波
    """
    
    def __init__(self, process_noise=None, measurement_noise=None, initial_estimate=None):
        # 从配置文件读取参数
        self.Q = process_noise or config.KALMAN_PROCESS_NOISE        # 过程噪声
        self.R = measurement_noise or config.KALMAN_MEASUREMENT_NOISE # 测量噪声
        
        # 状态变量
        self.x = initial_estimate or config.KALMAN_INITIAL_ESTIMATE  # 状态估计
        self.P = 1.0  # 估计误差协方差
        
        # 历史数据用于异常检测
        self.history = []
        self.max_history = 10
        
        logger.debug(f"卡尔曼滤波器初始化: Q={self.Q}, R={self.R}, x={self.x}")
    
    def filter(self, measurement):
        """
        执行卡尔曼滤波
        
        Args:
            measurement: 传感器测量值
            
        Returns:
            filtered_value: 滤波后的值
        """
        try:
            # 预测步骤
            # 状态预测 (假设状态转移模型为恒定)
            x_pred = self.x
            
            # 误差协方差预测
            P_pred = self.P + self.Q
            
            # 更新步骤
            # 卡尔曼增益
            K = P_pred / (P_pred + self.R)
            
            # 状态更新
            self.x = x_pred + K * (measurement - x_pred)
            
            # 误差协方差更新
            self.P = (1 - K) * P_pred
            
            # 异常值检测
            if self._is_outlier(measurement):
                logger.warning(f"检测到异常值: {measurement}, 使用预测值: {x_pred}")
                return x_pred
            
            # 更新历史记录
            self.history.append(self.x)
            if len(self.history) > self.max_history:
                self.history.pop(0)
            
            return self.x
            
        except Exception as e:
            logger.error(f"卡尔曼滤波失败: {e}")
            return measurement  # 返回原始值
    
    def _is_outlier(self, measurement):
        """检测异常值"""
        if len(self.history) < 3:
            return False
        
        # 基于历史数据的标准差检测
        hist_mean = np.mean(self.history)
        hist_std = np.std(self.history)
        
        # 3-sigma规则
        threshold = 3 * hist_std
        return abs(measurement - hist_mean) > threshold
    
    def reset(self, initial_estimate=None):
        """重置滤波器状态"""
        self.x = initial_estimate or config.KALMAN_INITIAL_ESTIMATE
        self.P = 1.0
        self.history.clear()
        logger.debug(f"卡尔曼滤波器已重置: x={self.x}")


class MultiChannelKalmanFilter:
    """
    多通道卡尔曼滤波器
    同时处理多个传感器信号
    """
    
    def __init__(self, channels=['temperature', 'heart_rate', 'blood_oxygen']):
        self.channels = channels
        self.filters = {}
        
        # 为每个通道创建专用滤波器
        channel_configs = {
            'temperature': {
                'process_noise': 0.005,
                'measurement_noise': 0.05,
                'initial_estimate': 36.5
            },
            'heart_rate': {
                'process_noise': 0.1,
                'measurement_noise': 1.0,
                'initial_estimate': 75
            },
            'blood_oxygen': {
                'process_noise': 0.05,
                'measurement_noise': 0.5,
                'initial_estimate': 98
            },
            'systolic_bp': {
                'process_noise': 0.2,
                'measurement_noise': 2.0,
                'initial_estimate': 120
            },
            'diastolic_bp': {
                'process_noise': 0.2,
                'measurement_noise': 1.5,
                'initial_estimate': 80
            }
        }
        
        for channel in channels:
            config_params = channel_configs.get(channel, {
                'process_noise': 0.01,
                'measurement_noise': 0.1,
                'initial_estimate': 0
            })
            
            self.filters[channel] = KalmanFilter(
                process_noise=config_params['process_noise'],
                measurement_noise=config_params['measurement_noise'],
                initial_estimate=config_params['initial_estimate']
            )
        
        logger.info(f"多通道卡尔曼滤波器初始化完成: {channels}")
    
    def filter_signals(self, measurements):
        """
        同时滤波多个信号
        
        Args:
            measurements: 字典格式的测量值 {channel: value}
            
        Returns:
            filtered_signals: 滤波后的信号字典
        """
        filtered_signals = {}
        
        for channel, value in measurements.items():
            if channel in self.filters:
                filtered_signals[channel] = self.filters[channel].filter(value)
            else:
                # 未配置的通道直接返回原值
                filtered_signals[channel] = value
                logger.warning(f"通道 {channel} 未配置滤波器")
        
        return filtered_signals
    
    def reset_all(self):
        """重置所有滤波器"""
        for filter_obj in self.filters.values():
            filter_obj.reset()
        logger.info("所有卡尔曼滤波器已重置")


# 测试代码
if __name__ == "__main__":
    import matplotlib.pyplot as plt
    import random
    
    logging.basicConfig(level=logging.INFO)
    
    # 测试单通道滤波器
    kf = KalmanFilter(process_noise=0.01, measurement_noise=0.1, initial_estimate=36.5)
    
    # 生成模拟的带噪声体温数据
    true_temp = 37.2
    measurements = []
    filtered_values = []
    
    for i in range(100):
        # 添加高斯噪声
        noise = random.gauss(0, 0.3)
        measurement = true_temp + noise
        
        # 偶尔添加异常值
        if random.random() < 0.05:
            measurement += random.choice([-2, 2])
        
        measurements.append(measurement)
        filtered_value = kf.filter(measurement)
        filtered_values.append(filtered_value)
    
    # 绘制结果
    plt.figure(figsize=(12, 6))
    plt.plot(measurements, 'r-', alpha=0.7, label='原始测量值')
    plt.plot(filtered_values, 'b-', linewidth=2, label='卡尔曼滤波')
    plt.axhline(y=true_temp, color='g', linestyle='--', label='真实值')
    plt.xlabel('时间步')
    plt.ylabel('体温 (°C)')
    plt.title('卡尔曼滤波器性能测试')
    plt.legend()
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.show()
    
    # 计算性能指标
    mse_original = np.mean([(m - true_temp)**2 for m in measurements])
    mse_filtered = np.mean([(f - true_temp)**2 for f in filtered_values])
    
    print(f"原始数据MSE: {mse_original:.4f}")
    print(f"滤波后MSE: {mse_filtered:.4f}")
    print(f"改善率: {(mse_original - mse_filtered) / mse_original * 100:.1f}%")
    
    # 测试多通道滤波器
    print("\n测试多通道滤波器...")
    multi_kf = MultiChannelKalmanFilter(['temperature', 'heart_rate', 'blood_oxygen'])
    
    test_measurements = {
        'temperature': 37.5,
        'heart_rate': 85,
        'blood_oxygen': 97
    }
    
    filtered_results = multi_kf.filter_signals(test_measurements)
    print(f"原始测量: {test_measurements}")
    print(f"滤波结果: {filtered_results}")