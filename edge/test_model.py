#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
模型测试脚本
"""

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from models.bert_tiny_triage import BERTTinyTriage

def main():
    print("=" * 50)
    print("BERT-Tiny 分诊模型测试")
    print("=" * 50)
    
    # 加载模型
    print("\n[1] 正在加载模型...")
    model = BERTTinyTriage()
    print("[1] 模型加载完成!")
    
    # 测试用例
    test_cases = [
        {
            "name": "危急病例 - 胸痛呼吸困难",
            "symptoms": "胸痛呼吸困难，大汗淋漓",
            "vitals": {"temperature": 38.5, "heartRate": 120, "systolicBP": 160, "diastolicBP": 95, "bloodOxygen": 92}
        },
        {
            "name": "急症病例 - 发热咳嗽",
            "symptoms": "发热，咳嗽，头痛",
            "vitals": {"temperature": 38.8, "heartRate": 95, "systolicBP": 130, "diastolicBP": 85, "bloodOxygen": 96}
        },
        {
            "name": "非急症病例 - 常规体检",
            "symptoms": "常规体检，无明显不适",
            "vitals": {"temperature": 36.5, "heartRate": 72, "systolicBP": 118, "diastolicBP": 75, "bloodOxygen": 98}
        }
    ]
    
    print("\n[2] 开始测试预测...")
    print("-" * 50)
    
    triage_labels = ['濒危', '危急', '急症', '次急症', '非急症']
    
    for i, case in enumerate(test_cases, 1):
        print(f"\n测试 {i}: {case['name']}")
        print(f"  症状: {case['symptoms']}")
        print(f"  体征: 体温={case['vitals']['temperature']}°C, 心率={case['vitals']['heartRate']}bpm, "
              f"血压={case['vitals']['systolicBP']}/{case['vitals']['diastolicBP']}mmHg, "
              f"血氧={case['vitals']['bloodOxygen']}%")
        
        level, confidence = model.predict(case['symptoms'], case['vitals'])
        
        print(f"  结果: {level}级 ({triage_labels[level-1]}), 置信度: {confidence:.2%}")
    
    print("\n" + "=" * 50)
    print("测试完成!")
    print("=" * 50)

if __name__ == "__main__":
    main()
