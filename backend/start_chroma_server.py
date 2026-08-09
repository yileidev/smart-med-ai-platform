"""
启动Chroma HTTP Server
使用本地持久化数据库提供HTTP API服务
"""

import chromadb
from chromadb.config import Settings
import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# 创建FastAPI应用
app = FastAPI()

# 配置CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 初始化Chroma客户端（使用本地数据库）
client = chromadb.PersistentClient(path="./chroma_data")

@app.get("/api/v1/heartbeat")
async def heartbeat():
    """健康检查"""
    return {"status": "ok"}

@app.post("/api/v1/collections/{collection_name}/query")
async def query_collection(collection_name: str, request: dict):
    """查询collection"""
    try:
        collection = client.get_collection(name=collection_name)
        
        query_texts = request.get("query_texts", [])
        n_results = request.get("n_results", 10)
        
        results = collection.query(
            query_texts=query_texts,
            n_results=n_results
        )
        
        return results
    except Exception as e:
        return {"error": str(e)}

@app.get("/api/v1/collections/{collection_name}")
async def get_collection(collection_name: str):
    """获取collection信息"""
    try:
        collection = client.get_collection(name=collection_name)
        return {
            "name": collection.name,
            "metadata": collection.metadata
        }
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    print("🚀 启动Chroma HTTP Server...")
    print("📁 数据库路径: ./chroma_data")
    print("🌐 HTTP API: http://localhost:8000")
    print("✅ 服务就绪！")
    
    uvicorn.run(app, host="0.0.0.0", port=8000, log_level="info")
