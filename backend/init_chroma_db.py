"""
Chroma DB 医疗知识库初始化脚本
用于创建 medical_knowledge collection 并导入基础医疗知识
"""

import chromadb

# 使用PersistentClient（本地数据库模式）
client = chromadb.PersistentClient(path="./chroma_data")

# 删除旧的collection（如果存在）
try:
    client.delete_collection("medical_knowledge")
    print("已删除旧的 medical_knowledge collection")
except:
    print("不存在旧的 collection，创建新的")

# 创建新的collection
collection = client.create_collection(
    name="medical_knowledge",
    metadata={"description": "医疗急诊分诊知识库"}
)

# 准备医疗知识数据（症状-疾病-科室-设备关联）
medical_knowledge = [
    # 心血管系统 (10条)
    {
        "id": "mk001",
        "text": "胸痛胸闷伴心率异常：可能心肌梗死或心绞痛，需立即心电图检查，优先分配心电监护仪和除颇仪，转诊心内科或急诊抢救室",
        "metadata": {"category": "cardiovascular", "department": "心内科", "urgency": "critical", "equipment": "心电监护仪,除颇仪"}
    },
    {
        "id": "mk002",
        "text": "持续性胸痛伴大汗呼吸困难：高度疑似急性心梗，分诊等级Ⅰ级（红色），立即启用急救绿色通道，需心电图、肌钙蛋白检测、冠脉造影设备",
        "metadata": {"category": "cardiovascular", "department": "心内科", "urgency": "critical", "equipment": "心电图机,冠脉造影设备"}
    },
    {
        "id": "mk003",
        "text": "高血压急症伴头痛视物模糊：收缩压>180mmHg，需监护血压，给予降压药物，转诊神经内科或心内科",
        "metadata": {"category": "cardiovascular", "department": "心内科", "urgency": "urgent", "equipment": "血压计,监护仪"}
    },
    {
        "id": "mk004",
        "text": "心力衰竭急性加重：冒险后呼吸困难、下肢浮肿、端坐呼吸，需利尿、强心、扩血管治疗，监测水电解质",
        "metadata": {"category": "cardiovascular", "department": "心内科", "urgency": "urgent", "equipment": "监护仪,输液泵"}
    },
    
    # 呼吸系统
    {
        "id": "mk004",
        "text": "呼吸困难血氧饱和度低于90%：可能肺栓塞、肺炎或哮喘急性发作，需吸氧设备、胸部X光或CT，转呼吸内科",
        "metadata": {"category": "respiratory", "department": "呼吸内科", "urgency": "urgent", "equipment": "氧气瓶,胸部X光机"}
    },
    {
        "id": "mk005",
        "text": "高热伴咳嗽咳痰呼吸急促：疑似重症肺炎，需胸部影像学检查、血常规、CRP检测，优先分配呼吸机和雾化设备",
        "metadata": {"category": "respiratory", "department": "呼吸内科", "urgency": "urgent", "equipment": "呼吸机,雾化器,X光机"}
    },
    {
        "id": "mk006",
        "text": "哮喘急性发作呼吸困难：需立即雾化吸入支气管扩张剂，监测血氧饱和度，准备呼吸机备用",
        "metadata": {"category": "respiratory", "department": "呼吸内科", "urgency": "urgent", "equipment": "雾化器,监护仪,呼吸机"}
    },
    
    # 神经系统
    {
        "id": "mk007",
        "text": "突发意识障碍伴肢体偏瘫：疑似脑卒中，立即头颅CT检查，分诊等级Ⅰ级，转神经内科或神经外科，需CT设备",
        "metadata": {"category": "neurological", "department": "神经内科", "urgency": "critical", "equipment": "CT机,监护仪"}
    },
    {
        "id": "mk008",
        "text": "剧烈头痛伴呕吐意识模糊：可能脑出血或脑膜炎，紧急头颅CT或MRI检查，转神经外科",
        "metadata": {"category": "neurological", "department": "神经外科", "urgency": "critical", "equipment": "CT机,MRI"}
    },
    {
        "id": "mk009",
        "text": "癫痫持续状态：需立即抗癫痫药物静脉注射，保持气道通畅，监测生命体征，准备气管插管设备",
        "metadata": {"category": "neurological", "department": "神经内科", "urgency": "critical", "equipment": "监护仪,急救车,气管插管设备"}
    },
    
    # 消化系统
    {
        "id": "mk010",
        "text": "急性腹痛伴呕吐发热：可能急性阑尾炎或胆囊炎，需腹部超声或CT检查，转普外科或消化内科",
        "metadata": {"category": "digestive", "department": "普外科", "urgency": "urgent", "equipment": "B超机,CT机"}
    },
    {
        "id": "mk011",
        "text": "呕血黑便伴休克症状：上消化道出血，分诊等级Ⅰ级，立即建立静脉通路、补液、胃镜检查，转消化内科",
        "metadata": {"category": "digestive", "department": "消化内科", "urgency": "critical", "equipment": "胃镜,监护仪,输液泵"}
    },
    {
        "id": "mk012",
        "text": "急性胰腺炎：剧烈上腹痛放射至背部，需血淀粉酶检测、腹部CT，禁食补液，转消化内科或ICU",
        "metadata": {"category": "digestive", "department": "消化内科", "urgency": "urgent", "equipment": "CT机,监护仪"}
    },
    
    # 外伤系统
    {
        "id": "mk013",
        "text": "多发性外伤伴出血休克：分诊等级Ⅰ级，立即止血、补液、X光或CT评估骨折和内脏损伤，转骨科或外科",
        "metadata": {"category": "trauma", "department": "骨科", "urgency": "critical", "equipment": "X光机,CT机,监护仪"}
    },
    {
        "id": "mk014",
        "text": "头部外伤意识障碍：需立即头颅CT排除颅内出血，密切观察瞳孔变化，转神经外科",
        "metadata": {"category": "trauma", "department": "神经外科", "urgency": "critical", "equipment": "CT机,监护仪"}
    },
    {
        "id": "mk015",
        "text": "骨折开放性伤口：需清创缝合、X光检查、抗生素预防感染，转骨科",
        "metadata": {"category": "trauma", "department": "骨科", "urgency": "urgent", "equipment": "X光机,清创包"}
    },
    
    # 内分泌系统
    {
        "id": "mk016",
        "text": "糖尿病酮症酸中毒：高血糖伴恶心呕吐呼吸深快，需血糖监测、胰岛素泵、电解质纠正，转内分泌科或ICU",
        "metadata": {"category": "endocrine", "department": "内分泌科", "urgency": "critical", "equipment": "血糖仪,胰岛素泵,监护仪"}
    },
    {
        "id": "mk017",
        "text": "低血糖昏迷：血糖<3.0mmol/L伴意识障碍，立即静脉注射葡萄糖，监测血糖变化",
        "metadata": {"category": "endocrine", "department": "内分泌科", "urgency": "urgent", "equipment": "血糖仪,监护仪"}
    },
    
    # 儿科急症
    {
        "id": "mk018",
        "text": "高热惊厥：儿童高热>39℃伴抽搐，立即物理降温、止惊药物，监测生命体征，转儿科",
        "metadata": {"category": "pediatric", "department": "儿科", "urgency": "urgent", "equipment": "监护仪,体温计"}
    },
    {
        "id": "mk019",
        "text": "急性喉炎呼吸困难：儿童犬吠样咳嗽伴吸气性呼吸困难，需雾化治疗、吸氧，准备气管插管设备",
        "metadata": {"category": "pediatric", "department": "儿科", "urgency": "urgent", "equipment": "雾化器,氧气,喉镜"}
    },
    
    # 过敏反应
    {
        "id": "mk020",
        "text": "过敏性休克：接触过敏原后皮疹、呼吸困难、血压下降，立即肾上腺素注射、补液、抗组胺药物",
        "metadata": {"category": "allergic", "department": "急诊科", "urgency": "critical", "equipment": "监护仪,急救车"}
    },
    
    # 感染性疾病
    {
        "id": "mk021",
        "text": "脓毒症休克：高热或低体温、心动过速、血压下降，需血培养、抗生素治疗、液体复苏，转ICU",
        "metadata": {"category": "infectious", "department": "ICU", "urgency": "critical", "equipment": "监护仪,输液泵"}
    },
    {
        "id": "mk022",
        "text": "急性传染病疑似：发热皮疹淋巴结肿大，需隔离观察、血常规、传染病筛查，转感染科",
        "metadata": {"category": "infectious", "department": "感染科", "urgency": "urgent", "equipment": "隔离病房,监护仪"}
    },
    
    # 妇产科急症
    {
        "id": "mk023",
        "text": "异位妊娠破裂：急性腹痛伴阴道出血、休克，需腹腔穿刺或超声检查，紧急手术，转妇产科",
        "metadata": {"category": "obstetric", "department": "妇产科", "urgency": "critical", "equipment": "B超机,监护仪,手术室"}
    },
    {
        "id": "mk024",
        "text": "先兆子痫：孕妇高血压伴蛋白尿、头痛、视力模糊，需降压、硫酸镁预防抽搐，转产科",
        "metadata": {"category": "obstetric", "department": "产科", "urgency": "urgent", "equipment": "监护仪,血压计"}
    },
    
    # 中毒急症
    {
        "id": "mk025",
        "text": "急性药物中毒：误服或过量药物，需催吐或洗胃、活性炭吸附、血液净化，转急诊科或ICU",
        "metadata": {"category": "poisoning", "department": "急诊科", "urgency": "critical", "equipment": "洗胃机,监护仪"}
    },
    
    # 精神科急症
    {
        "id": "mk026",
        "text": "急性精神障碍伴自伤或伤人：需安全约束、镇静药物、精神科会诊",
        "metadata": {"category": "psychiatric", "department": "精神科", "urgency": "urgent", "equipment": "约束带,监护仪"}
    },
    
    # 眼科急症
    {
        "id": "mk027",
        "text": "急性青光眼：眼痛、视力下降、眼压升高，需降眼压治疗，转眼科",
        "metadata": {"category": "ophthalmology", "department": "眼科", "urgency": "urgent", "equipment": "眼压计,裂隙灯"}
    },
    
    # 耳鼻喉急症
    {
        "id": "mk028",
        "text": "异物阻塞气道：呼吸困难、窒息征象，立即海姆立克急救或喉镜取出异物，准备气管切开",
        "metadata": {"category": "ent", "department": "耳鼻喉科", "urgency": "critical", "equipment": "喉镜,气管切开包"}
    },
    {
        "id": "mk029",
        "text": "鼻出血不止：持续出血>30分钟，需鼻腔填塞止血、监测血压，排除凝血功能障碍",
        "metadata": {"category": "ent", "department": "耳鼻喉科", "urgency": "urgent", "equipment": "鼻镜,止血材料"}
    },
    
    # 泌尿系统
    {
        "id": "mk030",
        "text": "急性肾绞痛：腰痛放射至下腹、血尿，疑似泌尿系结石，需泌尿系超声或CT，转泌尿外科",
        "metadata": {"category": "urological", "department": "泌尿外科", "urgency": "urgent", "equipment": "B超机,CT机"}
    }
]

# 批量添加数据到collection
ids = [item["id"] for item in medical_knowledge]
documents = [item["text"] for item in medical_knowledge]
metadatas = [item["metadata"] for item in medical_knowledge]

collection.add(
    ids=ids,
    documents=documents,
    metadatas=metadatas
)

print(f"✅ 成功创建 medical_knowledge collection")
print(f"✅ 已导入 {len(medical_knowledge)} 条医疗知识数据")
print(f"✅ 涵盖类别：心血管、呼吸、神经、消化、外伤、内分泌、儿科、过敏、感染、妇产、中毒、精神、眼科、耳鼻喉、泌尿系统")

# 测试查询
print("\n📊 测试语义检索：")
results = collection.query(
    query_texts=["胸痛心率快"],
    n_results=3
)
print(f"查询'胸痛心率快'的结果：")
for i, doc in enumerate(results['documents'][0]):
    print(f"{i+1}. {doc[:80]}...")
    print(f"   科室: {results['metadatas'][0][i]['department']}, 紧急度: {results['metadatas'][0][i]['urgency']}")

print("\n✅ Chroma DB 初始化完成！后端现在可以使用RAG增强诊断功能。")
