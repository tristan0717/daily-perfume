from fastapi import FastAPI
from sentence_transformers import SentenceTransformer
import faiss
import pickle

app = FastAPI()

# 서버가 켜질 때, Step 2에서 만든 5000개짜리 진짜 FAISS 지도와 데이터를 메모리에 올림
print("🚀 FastAPI 모델 로딩 중...")
model = SentenceTransformer('snunlp/KR-SBERT-V40K-klueNLI-augSTS')
index = faiss.read_index("perfumes.index")

with open("perfumes_meta.pkl", "rb") as f:
    perfume_meta = pickle.load(f)
print("✅ 로딩 완료! 5000개 향수 검색 준비 끝!")

# 🎯 자바(Spring Boot)가 호출할 벡터 검색 API
@app.get("/api/vector-search")
def search_perfumes(q: str, top_k: int = 20):
    # 1. 사용자의 질문("비오는 날 어울리는 우디")을 벡터로 변환
    query_vector = model.encode([q])
    
    # 2. FAISS 공간에서 가장 의미가 가까운 20개 찾기
    distances, indices = index.search(query_vector, top_k)
    
    results = []
    for i in indices[0]:
        results.append(perfume_meta[i])
        
    return {"results": results}
