"""
扩展医疗知识库到100+条
"""

import chromadb

# 使用本地数据库
client = chromadb.PersistentClient(path="./chroma_data")
collection = client.get_collection("medical_knowledge")

# 当前数量
current_count = collection.count()
print(f"当前知识库数量: {current_count}")

# 新增70条专业医疗知识
new_knowledge = [
    # 心血管系统扩展 (6条)
    {"id": "mk031", "text": "心律失常房颤：心悸不规则、脉搏短绌，需心电图确诊、抗凝治疗、节律控制或频率控制", "metadata": {"category": "cardiovascular", "department": "心内科", "urgency": "urgent", "equipment": "心电图机,除颤仪"}},
    {"id": "mk032", "text": "主动脉夹层：撕裂样胸背痛、血压差异、脉搏不对称，需紧急CT血管造影、降压控心率、外科会诊", "metadata": {"category": "cardiovascular", "department": "心外科", "urgency": "critical", "equipment": "CT机,监护仪"}},
    {"id": "mk033", "text": "深静脉血栓形成：下肢肿胀疼痛、温度升高、浅静脉曲张，需彩超检查、抗凝治疗", "metadata": {"category": "cardiovascular", "department": "血管外科", "urgency": "urgent", "equipment": "彩超,监护仪"}},
    {"id": "mk034", "text": "心包填塞：颈静脉怒张、心音遥远、脉压缩小，需超声心动图、心包穿刺引流", "metadata": {"category": "cardiovascular", "department": "心内科", "urgency": "critical", "equipment": "超声仪,穿刺包"}},
    {"id": "mk035", "text": "肺栓塞：突发呼吸困难、胸痛、晕厥，需CTA检查、溶栓或抗凝治疗", "metadata": {"category": "cardiovascular", "department": "呼吸内科", "urgency": "critical", "equipment": "CT机,呼吸机"}},
    {"id": "mk036", "text": "心肌炎：乏力、胸闷、心律失常，需心肌酶、心电图、卧床休息、对症治疗", "metadata": {"category": "cardiovascular", "department": "心内科", "urgency": "urgent", "equipment": "心电图机,监护仪"}},
    
    # 呼吸系统扩展 (6条)
    {"id": "mk037", "text": "肺炎重症：高热寒战、咳脓痰、呼吸急促、氧饱和度低，需胸片、血培养、广谱抗生素、氧疗", "metadata": {"category": "respiratory", "department": "呼吸内科", "urgency": "urgent", "equipment": "X光机,呼吸机,雾化器"}},
    {"id": "mk038", "text": "气胸：突发胸痛、呼吸困难、患侧呼吸音减弱，需胸片确诊、胸腔闭式引流", "metadata": {"category": "respiratory", "department": "胸外科", "urgency": "urgent", "equipment": "X光机,引流管"}},
    {"id": "mk039", "text": "慢阻肺急性加重：咳嗽咳痰加重、呼吸困难、发绀，需支气管扩张剂、激素、抗生素、呼吸支持", "metadata": {"category": "respiratory", "department": "呼吸内科", "urgency": "urgent", "equipment": "雾化器,呼吸机,血气分析仪"}},
    {"id": "mk040", "text": "支气管哮喘发作：喘息、呼气性呼吸困难、干咳，需雾化吸入、激素、平喘药", "metadata": {"category": "respiratory", "department": "呼吸内科", "urgency": "urgent", "equipment": "雾化器,监护仪"}},
    {"id": "mk041", "text": "肺结核咯血：咳嗽咳痰伴咯血、午后低热、盗汗，需胸片、痰菌检查、隔离治疗", "metadata": {"category": "respiratory", "department": "感染科", "urgency": "urgent", "equipment": "X光机,隔离病房"}},
    {"id": "mk042", "text": "呼吸衰竭：严重呼吸困难、发绀、意识障碍，需血气分析、机械通气、病因治疗", "metadata": {"category": "respiratory", "department": "ICU", "urgency": "critical", "equipment": "呼吸机,血气分析仪,监护仪"}},
    
    # 神经系统扩展 (8条)
    {"id": "mk043", "text": "脑梗死急性期：偏瘫、失语、面瘫，发病4.5小时内可溶栓，需头颅CT/MRI、神经功能评估", "metadata": {"category": "neurological", "department": "神经内科", "urgency": "critical", "equipment": "CT机,MRI"}},
    {"id": "mk044", "text": "蛛网膜下腔出血：突发剧烈头痛、颈项强直、意识障碍，需头颅CT、腰穿、脑血管造影", "metadata": {"category": "neurological", "department": "神经外科", "urgency": "critical", "equipment": "CT机,腰穿包"}},
    {"id": "mk045", "text": "癫痫持续状态：抽搐超过5分钟或反复发作，需苯二氮䓬类、抗癫痫药、气道管理", "metadata": {"category": "neurological", "department": "神经内科", "urgency": "critical", "equipment": "监护仪,气管插管设备"}},
    {"id": "mk046", "text": "急性脊髓炎：双下肢无力、感觉障碍、尿潴留，需MRI检查、激素冲击治疗", "metadata": {"category": "neurological", "department": "神经内科", "urgency": "urgent", "equipment": "MRI,导尿管"}},
    {"id": "mk047", "text": "格林-巴利综合征：四肢对称性无力、腱反射消失、呼吸肌麻痹，需肺功能监测、免疫治疗", "metadata": {"category": "neurological", "department": "神经内科", "urgency": "urgent", "equipment": "呼吸机,监护仪"}},
    {"id": "mk048", "text": "重症肌无力危象：吞咽困难、呼吸肌无力、眼睑下垂，需新斯的明试验、胆碱酯酶抑制剂", "metadata": {"category": "neurological", "department": "神经内科", "urgency": "urgent", "equipment": "呼吸机,监护仪"}},
    {"id": "mk049", "text": "颅内压增高：头痛呕吐、意识障碍、瞳孔改变，需甘露醇脱水、病因治疗", "metadata": {"category": "neurological", "department": "神经外科", "urgency": "critical", "equipment": "监护仪,呼吸机"}},
    {"id": "mk050", "text": "脑膜炎：发热头痛、颈强、意识障碍，需腰穿检查、抗生素或抗病毒治疗", "metadata": {"category": "neurological", "department": "神经内科", "urgency": "critical", "equipment": "腰穿包,监护仪"}},
    
    # 消化系统扩展 (8条)
    {"id": "mk051", "text": "消化道穿孔：突发剧烈腹痛、腹肌紧张、肠鸣音消失，需立位腹部X线、急诊手术", "metadata": {"category": "digestive", "department": "普外科", "urgency": "critical", "equipment": "X光机,手术室"}},
    {"id": "mk052", "text": "急性肠梗阻：腹痛呕吐、腹胀停止排便排气，需腹部X线、胃肠减压、手术或保守治疗", "metadata": {"category": "digestive", "department": "普外科", "urgency": "urgent", "equipment": "X光机,胃管"}},
    {"id": "mk053", "text": "肝性脑病：意识障碍、扑翼样震颤、氨味，需降氨治疗、纠正诱因、支持治疗", "metadata": {"category": "digestive", "department": "消化内科", "urgency": "critical", "equipment": "监护仪,输液泵"}},
    {"id": "mk054", "text": "急性重症胰腺炎：上腹剧痛、发热、休克，需禁食、胃肠减压、抑酶、抗感染、液体复苏", "metadata": {"category": "digestive", "department": "消化内科", "urgency": "critical", "equipment": "监护仪,胃管,输液泵"}},
    {"id": "mk055", "text": "食管静脉曲张破裂出血：呕血黑便、失血性休克，需三腔管压迫、止血药、内镜治疗", "metadata": {"category": "digestive", "department": "消化内科", "urgency": "critical", "equipment": "胃镜,三腔管,监护仪"}},
    {"id": "mk056", "text": "急性胆囊炎：右上腹痛、发热、Murphy征阳性，需腹部B超、抗感染、必要时手术", "metadata": {"category": "digestive", "department": "普外科", "urgency": "urgent", "equipment": "B超机,监护仪"}},
    {"id": "mk057", "text": "肠道缺血：突发剧烈腹痛、便血、腹膜刺激征，需CTA检查、急诊血管再通或手术", "metadata": {"category": "digestive", "department": "血管外科", "urgency": "critical", "equipment": "CT机,手术室"}},
    {"id": "mk058", "text": "炎症性肠病急性发作：腹痛腹泻、黏液脓血便、里急后重，需结肠镜、激素或免疫抑制剂", "metadata": {"category": "digestive", "department": "消化内科", "urgency": "urgent", "equipment": "肠镜,监护仪"}},
    
    # 泌尿系统扩展 (5条)
    {"id": "mk059", "text": "急性肾损伤：尿量减少、肌酐升高、水电解质紊乱，需病因治疗、液体管理、必要时透析", "metadata": {"category": "urological", "department": "肾内科", "urgency": "urgent", "equipment": "透析机,监护仪"}},
    {"id": "mk060", "text": "急性尿潴留：下腹胀痛、无法排尿、膀胱充盈，需导尿、解除梗阻", "metadata": {"category": "urological", "department": "泌尿外科", "urgency": "urgent", "equipment": "导尿管,B超机"}},
    {"id": "mk061", "text": "肾绞痛：腰腹部剧痛、血尿、恶心呕吐，疑似泌尿系结石，需泌尿系CT、解痉止痛", "metadata": {"category": "urological", "department": "泌尿外科", "urgency": "urgent", "equipment": "CT机,B超机"}},
    {"id": "mk062", "text": "急性肾盂肾炎：高热寒战、腰痛、尿频尿急，需尿培养、广谱抗生素治疗", "metadata": {"category": "urological", "department": "泌尿外科", "urgency": "urgent", "equipment": "监护仪"}},
    {"id": "mk063", "text": "膀胱破裂：下腹外伤、血尿、排尿困难，需膀胱造影、急诊手术修补", "metadata": {"category": "urological", "department": "泌尿外科", "urgency": "critical", "equipment": "X光机,手术室"}},
    
    # 内分泌系统扩展 (4条)
    {"id": "mk064", "text": "甲状腺危象：高热、心动过速、烦躁、呕吐腹泻，需抗甲状腺药物、β受体阻滞剂、激素", "metadata": {"category": "endocrine", "department": "内分泌科", "urgency": "critical", "equipment": "监护仪,输液泵"}},
    {"id": "mk065", "text": "肾上腺危象：低血压、低血糖、电解质紊乱、虚脱，需糖皮质激素替代、补液", "metadata": {"category": "endocrine", "department": "内分泌科", "urgency": "critical", "equipment": "监护仪,输液泵"}},
    {"id": "mk066", "text": "高渗性高血糖状态：血糖极高>33mmol/L、渗透压升高、意识障碍，需大量补液、小剂量胰岛素", "metadata": {"category": "endocrine", "department": "内分泌科", "urgency": "critical", "equipment": "血糖仪,胰岛素泵,监护仪"}},
    {"id": "mk067", "text": "乳酸酸中毒：二甲双胍使用者、肾功能不全、呼吸深快、意识障碍，需碳酸氢钠、血液透析", "metadata": {"category": "endocrine", "department": "内分泌科", "urgency": "critical", "equipment": "透析机,监护仪"}},
    
    # 血液系统 (5条)
    {"id": "mk068", "text": "DIC弥散性血管内凝血：多部位出血、血栓形成、休克，需补充凝血因子、抗凝治疗、病因治疗", "metadata": {"category": "hematology", "department": "血液科", "urgency": "critical", "equipment": "监护仪,输液泵"}},
    {"id": "mk069", "text": "急性白血病高白细胞症：白细胞>100×10^9/L、呼吸困难、意识障碍，需白细胞单采、化疗", "metadata": {"category": "hematology", "department": "血液科", "urgency": "critical", "equipment": "血细胞分离机,监护仪"}},
    {"id": "mk070", "text": "严重贫血：血红蛋白<60g/L、心悸气短、头晕乏力，需输血、病因治疗", "metadata": {"category": "hematology", "department": "血液科", "urgency": "urgent", "equipment": "输血设备,监护仪"}},
    {"id": "mk071", "text": "血小板减少性紫癜：皮肤瘀点瘀斑、黏膜出血、血小板<20×10^9/L，需激素、免疫球蛋白", "metadata": {"category": "hematology", "department": "血液科", "urgency": "urgent", "equipment": "监护仪"}},
    {"id": "mk072", "text": "溶血危象：黄疸、血红蛋白尿、腰背痛、贫血，需输血、保护肾功能、病因治疗", "metadata": {"category": "hematology", "department": "血液科", "urgency": "urgent", "equipment": "输血设备,监护仪"}},
    
    # 风湿免疫 (4条)
    {"id": "mk073", "text": "系统性红斑狼疮危象：高热、多器官受累、血细胞减少，需大剂量激素冲击治疗", "metadata": {"category": "rheumatology", "department": "风湿免疫科", "urgency": "critical", "equipment": "监护仪"}},
    {"id": "mk074", "text": "类风湿关节炎急性发作：多关节肿痛、晨僵、活动受限，需NSAIDs、激素、抗风湿药", "metadata": {"category": "rheumatology", "department": "风湿免疫科", "urgency": "urgent", "equipment": ""}},
    {"id": "mk075", "text": "痛风性关节炎急性发作：第一跖趾关节剧痛、红肿热痛、尿酸升高，需秋水仙碱、NSAIDs", "metadata": {"category": "rheumatology", "department": "风湿免疫科", "urgency": "urgent", "equipment": ""}},
    {"id": "mk076", "text": "血管炎危象：多器官缺血、肾功能衰竭、肺出血，需激素联合免疫抑制剂、血浆置换", "metadata": {"category": "rheumatology", "department": "风湿免疫科", "urgency": "critical", "equipment": "血浆置换机,监护仪"}},
    
    # 产科急症 (6条)
    {"id": "mk077", "text": "产后大出血：阴道流血>500ml、血压下降、脉搏加快，需宫缩剂、输血、纱布填塞或手术", "metadata": {"category": "obstetric", "department": "产科", "urgency": "critical", "equipment": "监护仪,输血设备"}},
    {"id": "mk078", "text": "子痫：孕妇抽搐、昏迷、高血压，需硫酸镁、降压、必要时剖宫产终止妊娠", "metadata": {"category": "obstetric", "department": "产科", "urgency": "critical", "equipment": "监护仪,急救车"}},
    {"id": "mk079", "text": "胎盘早剥：腹痛、阴道流血、胎心异常，需急诊剖宫产、输血、DIC防治", "metadata": {"category": "obstetric", "department": "产科", "urgency": "critical", "equipment": "胎心监护仪,手术室"}},
    {"id": "mk080", "text": "羊水栓塞：分娩时突发呼吸困难、循环衰竭、DIC，需气管插管、抗休克、抗凝", "metadata": {"category": "obstetric", "department": "产科", "urgency": "critical", "equipment": "呼吸机,监护仪,输液泵"}},
    {"id": "mk081", "text": "前置胎盘出血：妊娠晚期无痛性阴道流血，需B超确诊、期待治疗或剖宫产", "metadata": {"category": "obstetric", "department": "产科", "urgency": "urgent", "equipment": "B超机,胎心监护仪"}},
    {"id": "mk082", "text": "宫外孕破裂：停经后腹痛、阴道流血、休克，需急诊手术、输血", "metadata": {"category": "obstetric", "department": "妇产科", "urgency": "critical", "equipment": "B超机,手术室,监护仪"}},
    
    # 儿科扩展 (6条)
    {"id": "mk083", "text": "新生儿窒息：Apgar评分低、呼吸抑制、心率慢，需复苏、气管插管、正压通气", "metadata": {"category": "pediatric", "department": "新生儿科", "urgency": "critical", "equipment": "新生儿复苏台,呼吸机"}},
    {"id": "mk084", "text": "婴幼儿腹泻脱水：频繁水样便、前囟凹陷、皮肤弹性差，需补液纠正脱水、病因治疗", "metadata": {"category": "pediatric", "department": "儿科", "urgency": "urgent", "equipment": "输液泵,监护仪"}},
    {"id": "mk085", "text": "川崎病：发热5天以上、皮疹、球结膜充血、草莓舌，需大剂量免疫球蛋白、阿司匹林", "metadata": {"category": "pediatric", "department": "儿科", "urgency": "urgent", "equipment": "输液泵,超声心动图"}},
    {"id": "mk086", "text": "小儿肠套叠：阵发性哭闹、呕吐、果酱样血便，需空气或钡剂灌肠复位或手术", "metadata": {"category": "pediatric", "department": "儿外科", "urgency": "urgent", "equipment": "X光机,B超机"}},
    {"id": "mk087", "text": "急性喉炎Ⅲ度：犬吠样咳嗽、吸气性呼吸困难、三凹征，需雾化激素、吸氧、气管切开准备", "metadata": {"category": "pediatric", "department": "儿科", "urgency": "critical", "equipment": "雾化器,喉镜,气管切开包"}},
    {"id": "mk088", "text": "病毒性心肌炎：乏力、胸闷、心律失常，可致心源性休克，需卧床、营养心肌、抗病毒", "metadata": {"category": "pediatric", "department": "儿科", "urgency": "urgent", "equipment": "心电图机,监护仪"}},
    
    # 感染性疾病 (5条)
    {"id": "mk089", "text": "感染性休克：发热或低体温、心动过速、血压下降、意识障碍，需液体复苏、血管活性药、抗生素", "metadata": {"category": "infectious", "department": "ICU", "urgency": "critical", "equipment": "监护仪,输液泵,呼吸机"}},
    {"id": "mk090", "text": "破伤风：张口困难、苦笑面容、角弓反张，需抗毒素、青霉素、镇静、呼吸支持", "metadata": {"category": "infectious", "department": "感染科", "urgency": "critical", "equipment": "呼吸机,监护仪"}},
    {"id": "mk091", "text": "狂犬病：恐水恐风、兴奋躁动、吞咽困难，无特效治疗，重在预防接种", "metadata": {"category": "infectious", "department": "感染科", "urgency": "critical", "equipment": "隔离病房"}},
    {"id": "mk092", "text": "流行性脑脊髓膜炎：高热、剧烈头痛、皮肤瘀点瘀斑、脑膜刺激征，需大剂量青霉素、隔离", "metadata": {"category": "infectious", "department": "感染科", "urgency": "critical", "equipment": "隔离病房,腰穿包,监护仪"}},
    {"id": "mk093", "text": "疟疾：寒战高热、大汗、贫血、脾大，需血涂片找疟原虫、抗疟治疗", "metadata": {"category": "infectious", "department": "感染科", "urgency": "urgent", "equipment": "显微镜,监护仪"}},
    
    # 急性中毒 (5条)
    {"id": "mk094", "text": "有机磷中毒：瞳孔缩小、流涎流泪、呼吸困难、肌颤，需阿托品化、解磷定、洗胃", "metadata": {"category": "poisoning", "department": "急诊科", "urgency": "critical", "equipment": "洗胃机,呼吸机,监护仪"}},
    {"id": "mk095", "text": "一氧化碳中毒：头痛头晕、恶心呕吐、意识障碍、樱桃红色，需高浓度吸氧、高压氧舱", "metadata": {"category": "poisoning", "department": "急诊科", "urgency": "critical", "equipment": "高压氧舱,监护仪"}},
    {"id": "mk096", "text": "镇静催眠药中毒：昏迷、呼吸抑制、血压下降，需洗胃、呼吸支持、血液净化", "metadata": {"category": "poisoning", "department": "急诊科", "urgency": "critical", "equipment": "洗胃机,呼吸机,透析机"}},
    {"id": "mk097", "text": "酒精中毒：兴奋、共济失调、昏迷、呼吸抑制，需催吐、补液、纳洛酮", "metadata": {"category": "poisoning", "department": "急诊科", "urgency": "urgent", "equipment": "监护仪,胃管"}},
    {"id": "mk098", "text": "食物中毒：恶心呕吐、腹泻腹痛、发热，需洗胃、补液、抗感染", "metadata": {"category": "poisoning", "department": "急诊科", "urgency": "urgent", "equipment": "洗胃机,输液泵"}},
    
    # 其他急症 (3条)
    {"id": "mk099", "text": "热射病：高热>40℃、意识障碍、无汗、多器官功能障碍，需快速降温、液体复苏", "metadata": {"category": "other", "department": "急诊科", "urgency": "critical", "equipment": "冰毯,监护仪,输液泵"}},
    {"id": "mk100", "text": "低体温症：核心体温<35℃、寒战、意识障碍，需复温、呼吸循环支持", "metadata": {"category": "other", "department": "急诊科", "urgency": "critical", "equipment": "加温毯,监护仪"}},
]

# 批量添加
ids = [item["id"] for item in new_knowledge]
documents = [item["text"] for item in new_knowledge]
metadatas = [item["metadata"] for item in new_knowledge]

collection.add(
    ids=ids,
    documents=documents,
    metadatas=metadatas
)

final_count = collection.count()
print(f"✅ 成功添加 {len(new_knowledge)} 条医疗知识")
print(f"✅ 知识库总数: {final_count} 条")
print(f"✅ 涵盖类别更新：心血管、呼吸、神经、消化、泌尿、内分泌、血液、风湿免疫、产科、儿科、感染、中毒等")
