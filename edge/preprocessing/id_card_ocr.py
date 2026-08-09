"""
身份证OCR识别模块
基于PaddleOCR实现本地离线身份证信息提取

功能：
1. 身份证正面识别（姓名、性别、民族、出生日期、地址、身份证号）
2. 从摄像头拍照或选择图片文件
3. 完全离线运行，适合边缘部署
"""

import os
import re
import logging
from pathlib import Path
from datetime import datetime

logger = logging.getLogger(__name__)

# 尝试导入PaddleOCR
try:
    from paddleocr import PaddleOCR
    PADDLE_AVAILABLE = True
except ImportError:
    PADDLE_AVAILABLE = False
    logger.warning("PaddleOCR未安装，请运行: pip install paddlepaddle paddleocr")


class IDCardRecognizer:
    """
    身份证OCR识别器
    基于PaddleOCR实现本地离线识别
    """
    
    def __init__(self):
        self.ocr = None
        self.is_initialized = False
        
        if PADDLE_AVAILABLE:
            self._init_ocr()
    
    def _init_ocr(self):
        """初始化PaddleOCR"""
        try:
            logger.info("正在初始化PaddleOCR身份证识别...")
            # 使用中文模型，启用GPU（如果可用）
            self.ocr = PaddleOCR(
                use_angle_cls=True,  # 文字方向分类
                lang='ch',           # 中文
                use_gpu=False,       # Jetson上改为True
                show_log=False       # 关闭日志
            )
            self.is_initialized = True
            logger.info("✓ PaddleOCR初始化成功")
        except Exception as e:
            logger.error(f"PaddleOCR初始化失败: {e}")
            self.is_initialized = False
    
    def recognize_id_card(self, image_path):
        """
        识别身份证图片
        
        Args:
            image_path: 身份证图片路径
            
        Returns:
            dict: 包含姓名、性别、民族、出生日期、地址、身份证号等信息
        """
        if not self.is_initialized:
            return {"error": "OCR引擎未初始化"}
        
        if not os.path.exists(image_path):
            return {"error": f"图片文件不存在: {image_path}"}
        
        try:
            # 执行OCR识别
            result = self.ocr.ocr(image_path, cls=True)
            
            if not result or not result[0]:
                return {"error": "未能识别到文字"}
            
            # 提取所有识别的文本
            texts = []
            for line in result[0]:
                text = line[1][0]  # 获取识别的文字
                texts.append(text)
            
            # 解析身份证信息
            id_info = self._parse_id_card_text(texts)
            
            return id_info
            
        except Exception as e:
            logger.error(f"身份证识别失败: {e}")
            return {"error": str(e)}
    
    def _parse_id_card_text(self, texts):
        """
        解析身份证OCR结果
        
        Args:
            texts: OCR识别的文本列表
            
        Returns:
            dict: 解析后的身份证信息
        """
        info = {
            "name": "",
            "gender": "",
            "nation": "",
            "birth_year": "",
            "birth_month": "",
            "birth_day": "",
            "address": "",
            "id_number": "",
            "age": 0,
            "raw_texts": texts
        }
        
        full_text = " ".join(texts)
        
        # 1. 提取身份证号（18位数字+X）
        id_pattern = r'[1-9]\d{5}(?:19|20)\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\d|3[01])\d{3}[\dXx]'
        id_match = re.search(id_pattern, full_text.replace(" ", ""))
        if id_match:
            info["id_number"] = id_match.group().upper()
            # 从身份证号提取出生日期
            birth_str = info["id_number"][6:14]
            info["birth_year"] = birth_str[:4]
            info["birth_month"] = birth_str[4:6]
            info["birth_day"] = birth_str[6:8]
            # 计算年龄
            try:
                birth_date = datetime(int(info["birth_year"]), 
                                     int(info["birth_month"]), 
                                     int(info["birth_day"]))
                today = datetime.now()
                info["age"] = today.year - birth_date.year
                if (today.month, today.day) < (birth_date.month, birth_date.day):
                    info["age"] -= 1
            except:
                pass
            # 从身份证号判断性别
            gender_code = int(info["id_number"][16])
            info["gender"] = "MALE" if gender_code % 2 == 1 else "FEMALE"
        
        # 2. 提取姓名（通常在"姓名"后面）
        for i, text in enumerate(texts):
            if "姓名" in text:
                # 姓名可能在同一行或下一行
                name_text = text.replace("姓名", "").strip()
                if not name_text and i + 1 < len(texts):
                    name_text = texts[i + 1].strip()
                if name_text and len(name_text) <= 10:
                    info["name"] = name_text
                break
        
        # 如果没找到，尝试其他方式
        if not info["name"]:
            for text in texts:
                # 中文姓名通常2-4个字
                if re.match(r'^[\u4e00-\u9fa5]{2,4}$', text.strip()):
                    if text not in ["男", "女", "汉族", "中国", "公民"]:
                        info["name"] = text.strip()
                        break
        
        # 3. 提取民族
        for text in texts:
            if "民族" in text:
                nation = text.replace("民族", "").strip()
                if nation:
                    info["nation"] = nation
                break
            # 常见民族
            if text.strip() in ["汉", "汉族", "蒙古", "回", "藏", "维吾尔", "苗", "彝", "壮", "布依", "满", "侗", "瑶", "白", "土家", "哈尼", "傣", "黎", "傈僳", "佤", "畲", "高山", "拉祜", "水", "东乡", "纳西", "景颇", "柯尔克孜", "土", "达斡尔", "仫佬", "羌", "布朗", "撒拉", "毛南", "仡佬", "锡伯", "阿昌", "普米", "朝鲜", "塔吉克", "怒", "乌孜别克", "俄罗斯", "鄂温克", "德昂", "保安", "裕固", "京", "塔塔尔", "独龙", "鄂伦春", "赫哲", "门巴", "珞巴", "基诺"]:
                info["nation"] = text.strip()
                if not text.endswith("族"):
                    info["nation"] += "族"
        
        # 4. 提取地址
        for i, text in enumerate(texts):
            if "住址" in text or "地址" in text:
                addr_parts = [text.replace("住址", "").replace("地址", "").strip()]
                # 地址可能跨多行
                for j in range(i + 1, min(i + 4, len(texts))):
                    next_text = texts[j].strip()
                    if any(kw in next_text for kw in ["公民", "身份", "号码", "签发"]):
                        break
                    if re.match(r'^[\u4e00-\u9fa5\d]+', next_text):
                        addr_parts.append(next_text)
                info["address"] = "".join(addr_parts)
                break
        
        return info
    
    def is_available(self):
        """检查OCR是否可用"""
        return PADDLE_AVAILABLE and self.is_initialized


# 全局实例
_recognizer = None

def get_id_recognizer():
    """获取身份证识别器单例"""
    global _recognizer
    if _recognizer is None:
        _recognizer = IDCardRecognizer()
    return _recognizer


# 测试代码
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    
    print("=" * 50)
    print("身份证OCR识别测试")
    print("=" * 50)
    
    recognizer = IDCardRecognizer()
    
    if recognizer.is_available():
        print("\nPaddleOCR已就绪")
        print("请提供身份证图片路径进行测试:")
        
        # 测试示例
        test_image = input("输入图片路径: ").strip()
        if test_image:
            result = recognizer.recognize_id_card(test_image)
            print("\n识别结果:")
            for key, value in result.items():
                if key != "raw_texts":
                    print(f"  {key}: {value}")
    else:
        print("OCR不可用，请安装PaddleOCR")
