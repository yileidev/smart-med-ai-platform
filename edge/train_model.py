"""
模型训练脚本
用于训练BERT-Tiny边缘分诊模型

功能：
1. 数据加载和预处理
2. 模型训练和验证
3. 性能评估和优化
4. TensorRT模型转换
5. 部署准备
"""

import os
import json
import torch
import pandas as pd
import numpy as np
from pathlib import Path
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
from torch.utils.data import Dataset, DataLoader
import torch.nn as nn
import torch.optim as optim
from transformers import BertTokenizer
import logging
import pickle
from typing import Dict, List, Tuple, Any
from tqdm import tqdm

# 设置Hugging Face镜像源
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'

# 导入模型
import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), 'models'))
from bert_tiny_triage import MultiModalTriageModel, BERTTinyTriage
import config

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class TriageDataset(Dataset):
    """
    分诊数据集类
    支持多模态输入：症状文本 + 生理参数
    """
    
    def __init__(self, data: List[Dict], tokenizer, max_length: int = 128):
        self.data = data
        self.tokenizer = tokenizer
        self.max_length = max_length
        
        # 提取数据
        self.texts = [item['symptoms'] for item in data]
        self.vital_signs = [item['vital_signs'] for item in data]
        self.labels = [item['triage_level'] - 1 for item in data]  # 转换为0-4
        
        logger.info(f"数据集初始化完成: {len(self.data)} 样本")
    
    def __len__(self):
        return len(self.data)
    
    def __getitem__(self, idx):
        # 文本编码
        text = str(self.texts[idx])
        encoding = self.tokenizer(
            text,
            add_special_tokens=True,
            max_length=self.max_length,
            padding='max_length',
            truncation=True,
            return_tensors='pt'
        )
        
        # 生理参数
        vital_signs = torch.FloatTensor(self.vital_signs[idx])
        
        # 标签
        label = torch.LongTensor([self.labels[idx]])
        
        return {
            'input_ids': encoding['input_ids'].squeeze(0),
            'attention_mask': encoding['attention_mask'].squeeze(0),
            'vital_signs': vital_signs,
            'labels': label.squeeze(0)
        }


class MedicalTriageTrainer:
    """
    医疗分诊模型训练器
    """
    
    def __init__(self, model_save_dir: str = "models"):
        self.model_save_dir = Path(model_save_dir)
        self.model_save_dir.mkdir(parents=True, exist_ok=True)
        
        self.device = self._get_device()
        self.tokenizer = BertTokenizer.from_pretrained('bert-base-chinese')
        self.scaler = StandardScaler()
        
        # 训练配置 - CPU优化版本
        self.config = {
            'batch_size': 16,  # 增加批次大小以加速训练
            'learning_rate': 2e-5,  # 学习率
            'epochs': 10,  # 减少轮数以适应CPU训练
            'max_length': 64,  # 减少序列长度以加速
            'weight_decay': 0.01,
            'warmup_steps': 100,
            'early_stopping_patience': 5  # 早停耐心值
        }
        
        logger.info(f"训练器初始化完成，使用设备: {self.device}")
    
    def _get_device(self):
        if torch.cuda.is_available():
            device = torch.device('cuda')
            logger.info(f"使用GPU: {torch.cuda.get_device_name()}")
        else:
            device = torch.device('cpu')
            logger.info("使用CPU训练")
        return device
    
    def generate_synthetic_data(self, num_samples: int = 10000) -> List[Dict]:
        """
        生成合成训练数据
        基于医疗分诊标准创建多样化的训练样本
        """
        logger.info(f"开始生成 {num_samples} 个合成训练样本...")
        
        data = []
        
        # 分诊等级模板
        triage_templates = {
            1: {  # 濒危
                'symptoms': [
                    '患者意识不清，呼吸微弱',
                    '严重胸痛，大汗淋漓，面色苍白',
                    '心脏骤停，无脉搏',
                    '大出血，血压下降',
                    '严重呼吸困难，口唇发绀',
                    '昏迷，对疼痛无反应',
                    '严重过敏性休克',
                    '体温过低，意识模糊'
                ],
                'vital_ranges': {
                    'temperature': [(41.0, 43.0), (32.0, 35.0)],
                    'heart_rate': [(150, 200), (30, 45)],
                    'systolic_bp': [(200, 250), (50, 70)],
                    'diastolic_bp': [(110, 150), (30, 50)],
                    'blood_oxygen': (70, 85)
                }
            },
            2: {  # 危急
                'symptoms': [
                    '胸痛，呼吸困难',
                    '严重头痛，视物模糊',
                    '高热，寒战',
                    '剧烈腹痛，呕吐',
                    '心悸，胸闷',
                    '突发言语不清',
                    '哮喘急性发作',
                    '严重外伤，疼痛剧烈'
                ],
                'vital_ranges': {
                    'temperature': (39.0, 41.0),
                    'heart_rate': [(120, 150), (45, 55)],
                    'systolic_bp': [(160, 200), (70, 90)],
                    'diastolic_bp': [(95, 110), (45, 60)],
                    'blood_oxygen': (85, 92)
                }
            },
            3: {  # 急症
                'symptoms': [
                    '发热，咳嗽',
                    '腹痛，恶心',
                    '头痛，眩晕',
                    '关节疼痛，活动受限',
                    '呼吸急促，胸闷',
                    '恶心呕吐，腹泻',
                    '皮疹，瘙痒',
                    '外伤，中等疼痛'
                ],
                'vital_ranges': {
                    'temperature': (38.0, 39.0),
                    'heart_rate': (90, 120),
                    'systolic_bp': (140, 160),
                    'diastolic_bp': (85, 95),
                    'blood_oxygen': (92, 96)
                }
            },
            4: {  # 次急症
                'symptoms': [
                    '轻度头痛',
                    '轻微咳嗽，鼻塞',
                    '轻度腹痛',
                    '关节酸痛',
                    '轻度发热',
                    '消化不良',
                    '轻微外伤',
                    '慢性疼痛加重'
                ],
                'vital_ranges': {
                    'temperature': (37.5, 38.0),
                    'heart_rate': (80, 100),
                    'systolic_bp': (130, 140),
                    'diastolic_bp': (80, 85),
                    'blood_oxygen': (96, 98)
                }
            },
            5: {  # 非急症
                'symptoms': [
                    '常规体检',
                    '慢病复查',
                    '轻微不适',
                    '预防接种咨询',
                    '健康咨询',
                    '药物调整',
                    '报告解读',
                    '无明显症状'
                ],
                'vital_ranges': {
                    'temperature': (36.2, 37.5),
                    'heart_rate': (65, 85),
                    'systolic_bp': (110, 130),
                    'diastolic_bp': (70, 80),
                    'blood_oxygen': (97, 100)
                }
            }
        }
        
        # 生成数据
        for i in range(num_samples):
            # 随机选择分诊等级（不均匀分布，模拟真实情况）
            triage_weights = [0.05, 0.15, 0.35, 0.30, 0.15]  # 1-5级的权重
            triage_level = np.random.choice([1, 2, 3, 4, 5], p=triage_weights)
            
            template = triage_templates[triage_level]
            
            # 选择症状
            symptom = np.random.choice(template['symptoms'])
            
            # 生成生理参数
            vital_signs = []
            
            # 体温
            temp_ranges = template['vital_ranges']['temperature']
            if isinstance(temp_ranges, tuple):
                temp = np.random.uniform(temp_ranges[0], temp_ranges[1])
            else:
                # temp_ranges 是一个包含多个范围的列表，如 [(41.0, 43.0), (32.0, 35.0)]
                # 首先随机选择一个范围
                selected_range = np.random.choice(len(temp_ranges))
                temp_range = temp_ranges[selected_range]
                temp = np.random.uniform(temp_range[0], temp_range[1])
            vital_signs.append(round(temp, 1))
            
            # 心率
            hr_ranges = template['vital_ranges']['heart_rate']
            if isinstance(hr_ranges, tuple):
                hr = int(np.random.uniform(hr_ranges[0], hr_ranges[1]))
            else:
                # hr_ranges 是一个包含多个范围的列表，如 [(150, 200), (30, 45)]
                # 首先随机选择一个范围
                selected_range = np.random.choice(len(hr_ranges))
                hr_range = hr_ranges[selected_range]
                hr = int(np.random.uniform(hr_range[0], hr_range[1]))
            vital_signs.append(hr)
            
            # 收缩压
            sbp_ranges = template['vital_ranges']['systolic_bp']
            if isinstance(sbp_ranges, tuple):
                sbp = int(np.random.uniform(sbp_ranges[0], sbp_ranges[1]))
            else:
                # sbp_ranges 是一个包含多个范围的列表，如 [(200, 250), (50, 70)]
                # 首先随机选择一个范围
                selected_range = np.random.choice(len(sbp_ranges))
                sbp_range = sbp_ranges[selected_range]
                sbp = int(np.random.uniform(sbp_range[0], sbp_range[1]))
            vital_signs.append(sbp)
            
            # 舒张压
            dbp_ranges = template['vital_ranges']['diastolic_bp']
            if isinstance(dbp_ranges, tuple):
                dbp = int(np.random.uniform(dbp_ranges[0], dbp_ranges[1]))
            else:
                # dbp_ranges 是一个包含多个范围的列表，如 [(110, 150), (30, 50)]
                # 首先随机选择一个范围
                selected_range = np.random.choice(len(dbp_ranges))
                dbp_range = dbp_ranges[selected_range]
                dbp = int(np.random.uniform(dbp_range[0], dbp_range[1]))
            vital_signs.append(dbp)
            
            # 血氧
            spo2_range = template['vital_ranges']['blood_oxygen']
            if isinstance(spo2_range, tuple):
                spo2 = int(np.random.uniform(spo2_range[0], spo2_range[1]))
            else:
                spo2 = int(np.random.uniform(spo2_range[0], spo2_range[1]))
            vital_signs.append(spo2)
            
            # 添加噪声
            if np.random.random() < 0.1:  # 10%的噪声数据
                vital_signs = self._add_noise_to_vitals(vital_signs)
            
            data.append({
                'symptoms': symptom,
                'vital_signs': [float(v) if isinstance(v, (np.floating, float)) else int(v) for v in vital_signs],
                'triage_level': int(triage_level)
            })
        
        logger.info(f"合成数据生成完成: {len(data)} 样本")
        
        # 保存数据
        data_file = self.model_save_dir / 'synthetic_training_data.json'
        with open(data_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        
        return data
    
    def _add_noise_to_vitals(self, vital_signs: List[float]) -> List[float]:
        """为生理参数添加噪声"""
        noisy_vitals = vital_signs.copy()
        
        # 随机调整1-2个参数
        indices_to_modify = np.random.choice(len(vital_signs), 
                                          size=np.random.randint(1, 3), 
                                          replace=False)
        
        for idx in indices_to_modify:
            # 添加5-10%的随机噪声
            noise_factor = np.random.uniform(0.95, 1.05)
            noisy_vitals[idx] *= noise_factor
            
            # 确保在合理范围内
            if idx == 0:  # 体温
                noisy_vitals[idx] = max(35.0, min(42.0, noisy_vitals[idx]))
            elif idx == 1:  # 心率
                noisy_vitals[idx] = max(40, min(200, int(noisy_vitals[idx])))
            elif idx in [2, 3]:  # 血压
                noisy_vitals[idx] = max(40, min(250, int(noisy_vitals[idx])))
            elif idx == 4:  # 血氧
                noisy_vitals[idx] = max(70, min(100, int(noisy_vitals[idx])))
        
        return noisy_vitals
    
    def prepare_data(self, data: List[Dict], test_size: float = 0.2, val_size: float = 0.1):
        """
        准备训练数据
        """
        logger.info("开始数据预处理...")
        
        # 提取生理参数进行标准化
        vital_signs_array = np.array([item['vital_signs'] for item in data])
        vital_signs_normalized = self.scaler.fit_transform(vital_signs_array)
        
        # 创建标准化后的数据副本，避免修改原始数据
        normalized_data = []
        for i, item in enumerate(data):
            normalized_item = item.copy()
            normalized_item['vital_signs'] = vital_signs_normalized[i].tolist()
            normalized_data.append(normalized_item)
        
        # 数据分割
        train_data, test_data = train_test_split(normalized_data, test_size=test_size, random_state=42, stratify=[item['triage_level'] for item in normalized_data])
        train_data, val_data = train_test_split(train_data, test_size=val_size/(1-test_size), random_state=42, stratify=[item['triage_level'] for item in train_data])
        
        logger.info(f"数据分割完成: 训练集 {len(train_data)}, 验证集 {len(val_data)}, 测试集 {len(test_data)}")
        
        # 保存scaler
        scaler_path = self.model_save_dir / 'scaler.pkl'
        with open(scaler_path, 'wb') as f:
            pickle.dump(self.scaler, f)
        
        return train_data, val_data, test_data
    
    def create_data_loaders(self, train_data, val_data, test_data):
        """创建数据加载器"""
        train_dataset = TriageDataset(train_data, self.tokenizer, self.config['max_length'])
        val_dataset = TriageDataset(val_data, self.tokenizer, self.config['max_length'])
        test_dataset = TriageDataset(test_data, self.tokenizer, self.config['max_length'])
        
        train_loader = DataLoader(train_dataset, batch_size=self.config['batch_size'], shuffle=True)
        val_loader = DataLoader(val_dataset, batch_size=self.config['batch_size'], shuffle=False)
        test_loader = DataLoader(test_dataset, batch_size=self.config['batch_size'], shuffle=False)
        
        return train_loader, val_loader, test_loader
    
    def train_model(self, train_loader, val_loader):
        """训练模型"""
        logger.info("开始训练模型...")
        
        # 创建模型
        model = MultiModalTriageModel(
            bert_model_name='bert-base-chinese',
            vital_signs_dim=5,
            hidden_dim=256,
            num_classes=5
        )
        model.to(self.device)
        
        # 优化器和学习率调度器
        optimizer = optim.AdamW(model.parameters(), 
                               lr=self.config['learning_rate'], 
                               weight_decay=self.config['weight_decay'])
        
        scheduler = optim.lr_scheduler.LinearLR(optimizer, 
                                               start_factor=1.0, 
                                               end_factor=0.1, 
                                               total_iters=self.config['epochs'])
        
        # 损失函数（处理类别不平衡）
        class_weights = torch.FloatTensor([2.0, 1.5, 1.0, 1.2, 1.8]).to(self.device)  # 根据实际分布调整
        criterion = nn.CrossEntropyLoss(weight=class_weights)
        
        # 训练循环
        best_val_acc = 0.0
        best_model_path = None
        train_losses = []
        val_accuracies = []
        
        # 早停相关变量
        patience_counter = 0
        best_val_loss = float('inf')
        
        for epoch in range(self.config['epochs']):
            # 训练阶段
            model.train()
            total_train_loss = 0
            
            # 添加进度条
            progress_bar = tqdm(train_loader, desc=f"Epoch {epoch+1}/{self.config['epochs']}")
            for batch in progress_bar:
                input_ids = batch['input_ids'].to(self.device)
                attention_mask = batch['attention_mask'].to(self.device)
                vital_signs = batch['vital_signs'].to(self.device)
                labels = batch['labels'].to(self.device)
                
                optimizer.zero_grad()
                
                triage_logits, confidence = model(input_ids, attention_mask, vital_signs)
                loss = criterion(triage_logits, labels)
                
                loss.backward()
                optimizer.step()
                
                total_train_loss += loss.item()
                
                # 更新进度条显示
                progress_bar.set_postfix({'loss': f'{loss.item():.4f}'})
            
            avg_train_loss = total_train_loss / len(train_loader)
            train_losses.append(avg_train_loss)
            
            # 验证阶段
            val_acc, val_loss = self.evaluate_model(model, val_loader, criterion)
            val_accuracies.append(val_acc)
            
            scheduler.step()
            
            logger.info(f"Epoch {epoch+1}/{self.config['epochs']} - "
                       f"Train Loss: {avg_train_loss:.4f}, "
                       f"Val Acc: {val_acc:.4f}, "
                       f"Val Loss: {val_loss:.4f}")
            
            # 保存最佳模型
            if val_acc > best_val_acc:
                best_val_acc = val_acc
                best_model_path = self.save_model(model, epoch, val_acc)
                patience_counter = 0  # 重置耐心计数器
            else:
                patience_counter += 1
            
            # 检查是否需要早停
            if patience_counter >= self.config['early_stopping_patience']:
                logger.info(f"早停触发: 连续 {patience_counter} 个epoch验证准确率未提升")
                break
        
        logger.info(f"训练完成! 最佳验证准确率: {best_val_acc:.4f}")
        
        # 绘制训练曲线
        self.plot_training_curves(train_losses, val_accuracies)
        
        return best_model_path, best_val_acc
    
    def evaluate_model(self, model, data_loader, criterion):
        """评估模型"""
        model.eval()
        total_loss = 0
        correct_predictions = 0
        total_samples = 0
        
        with torch.no_grad():
            for batch in data_loader:
                input_ids = batch['input_ids'].to(self.device)
                attention_mask = batch['attention_mask'].to(self.device)
                vital_signs = batch['vital_signs'].to(self.device)
                labels = batch['labels'].to(self.device)
                
                triage_logits, confidence = model(input_ids, attention_mask, vital_signs)
                loss = criterion(triage_logits, labels)
                
                total_loss += loss.item()
                predictions = torch.argmax(triage_logits, dim=1)
                correct_predictions += (predictions == labels).sum().item()
                total_samples += labels.size(0)
        
        accuracy = correct_predictions / total_samples
        avg_loss = total_loss / len(data_loader)
        
        return accuracy, avg_loss
    
    def save_model(self, model, epoch, accuracy):
        """保存模型"""
        checkpoint = {
            'epoch': epoch,
            'model_state_dict': model.state_dict(),
            'accuracy': accuracy,
            'config': self.config,
            'model_config': {
                'bert_model_name': 'bert-base-chinese',
                'vital_signs_dim': 5,
                'hidden_dim': 256,
                'num_classes': 5
            }
        }
        
        model_path = self.model_save_dir / f'best_model_epoch_{epoch}_acc_{accuracy:.4f}.pth'
        torch.save(checkpoint, model_path)
        
        logger.info(f"模型已保存: {model_path}")
        return model_path
    
    def plot_training_curves(self, train_losses, val_accuracies):
        """绘制训练曲线"""
        try:
            import matplotlib.pyplot as plt
            
            fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(12, 4))
            
            # 训练损失
            ax1.plot(train_losses)
            ax1.set_title('Training Loss')
            ax1.set_xlabel('Epoch')
            ax1.set_ylabel('Loss')
            ax1.grid(True)
            
            # 验证准确率
            ax2.plot(val_accuracies)
            ax2.set_title('Validation Accuracy')
            ax2.set_xlabel('Epoch')
            ax2.set_ylabel('Accuracy')
            ax2.grid(True)
            
            plt.tight_layout()
            plt.savefig(self.model_save_dir / 'training_curves.png', dpi=300, bbox_inches='tight')
            logger.info("训练曲线已保存")
            
        except ImportError:
            logger.warning("matplotlib未安装，跳过训练曲线绘制")
    
    def comprehensive_evaluation(self, model_path, test_loader):
        """全面评估模型"""
        logger.info("开始全面评估...")
        
        # 加载最佳模型
        checkpoint = torch.load(model_path, map_location=self.device)
        model = MultiModalTriageModel(
            bert_model_name='bert-base-chinese',
            vital_signs_dim=5,
            hidden_dim=256,
            num_classes=5
        )
        model.load_state_dict(checkpoint['model_state_dict'])
        model.to(self.device)
        model.eval()
        
        # 收集预测结果
        all_predictions = []
        all_labels = []
        all_confidences = []
        
        with torch.no_grad():
            for batch in test_loader:
                input_ids = batch['input_ids'].to(self.device)
                attention_mask = batch['attention_mask'].to(self.device)
                vital_signs = batch['vital_signs'].to(self.device)
                labels = batch['labels'].to(self.device)
                
                triage_logits, confidence = model(input_ids, attention_mask, vital_signs)
                predictions = torch.argmax(triage_logits, dim=1)
                
                all_predictions.extend(predictions.cpu().numpy())
                all_labels.extend(labels.cpu().numpy())
                all_confidences.extend(confidence.cpu().numpy())
        
        # 计算指标
        accuracy = accuracy_score(all_labels, all_predictions)
        
        # 分类报告
        class_names = ['濒危', '危急', '急症', '次急症', '非急症']
        report = classification_report(all_labels, all_predictions, 
                                     target_names=class_names, 
                                     output_dict=True)
        
        # 混淆矩阵
        cm = confusion_matrix(all_labels, all_predictions)
        
        # 保存评估结果
        evaluation_results = {
            'test_accuracy': accuracy,
            'classification_report': report,
            'confusion_matrix': cm.tolist(),
            'model_path': str(model_path),
            'test_samples': len(all_labels)
        }
        
        results_path = self.model_save_dir / 'evaluation_results.json'
        with open(results_path, 'w', encoding='utf-8') as f:
            json.dump(evaluation_results, f, ensure_ascii=False, indent=2)
        
        logger.info(f"测试准确率: {accuracy:.4f}")
        logger.info(f"评估结果已保存: {results_path}")
        
        return evaluation_results
    
    def convert_to_deployment_format(self, model_path):
        """转换模型为部署格式"""
        logger.info("开始模型格式转换...")
        
        try:
            # 加载最佳模型权重
            checkpoint = torch.load(model_path, map_location=self.device)
            
            # 创建模型实例
            model = MultiModalTriageModel(
                bert_model_name='bert-base-chinese',
                vital_signs_dim=5,
                hidden_dim=256,
                num_classes=5
            )
            model.load_state_dict(checkpoint['model_state_dict'])
            model.to(self.device)
            
            # 保存完整的部署模型
            deployment_model_path = self.model_save_dir / 'bert-tiny-triage.pth'
            deployment_checkpoint = {
                'model_state_dict': model.state_dict(),
                'scaler_path': str(self.model_save_dir / 'scaler.pkl'),
                'model_config': checkpoint.get('model_config', {
                    'bert_model_name': 'bert-base-chinese',
                    'vital_signs_dim': 5,
                    'hidden_dim': 256,
                    'num_classes': 5
                })
            }
            torch.save(deployment_checkpoint, deployment_model_path)
            
            logger.info(f"部署模型已保存: {deployment_model_path}")
            
            # 如果TensorRT可用，尝试转换
            try:
                import tensorrt as trt
                logger.info("检测到TensorRT，开始转换为TensorRT格式...")
                
                # 通过BERTTinyTriage类进行转换
                triage_model = BERTTinyTriage()
                triage_model.model = model
                triage_model.scaler = self.scaler
                triage_model._convert_to_tensorrt()
                
            except ImportError:
                logger.info("TensorRT不可用，跳过TensorRT转换")
            
        except Exception as e:
            logger.error(f"模型转换失败: {e}")
            import traceback
            logger.error(f"详细错误信息: {traceback.format_exc()}")


def main():
    """主训练流程"""
    logger.info("开始医疗分诊模型训练流程...")
    
    # 创建训练器
    trainer = MedicalTriageTrainer()
    
    # 生成训练数据 - 减少样本数量以避免内存问题
    data = trainer.generate_synthetic_data(num_samples=2000)
    
    # 准备数据
    train_data, val_data, test_data = trainer.prepare_data(data)
    
    # 创建数据加载器
    train_loader, val_loader, test_loader = trainer.create_data_loaders(train_data, val_data, test_data)
    
    # 训练模型
    best_model_path, best_accuracy = trainer.train_model(train_loader, val_loader)
    
    # 全面评估
    evaluation_results = trainer.comprehensive_evaluation(best_model_path, test_loader)
    
    # 转换为部署格式
    trainer.convert_to_deployment_format(best_model_path)
    
    logger.info("训练流程完成!")
    logger.info(f"最佳模型: {best_model_path}")
    logger.info(f"测试准确率: {evaluation_results['test_accuracy']:.4f}")
    
    # 性能基准测试
    if best_model_path:
        triage_model = BERTTinyTriage()
        benchmark_results = triage_model.benchmark(1000)
        logger.info(f"性能基准: {benchmark_results}")


if __name__ == "__main__":
    main()