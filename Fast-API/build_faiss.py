import pandas as pd
import os 
from dotenv import load_dotenv 
from sqlalchemy import create_engine
from sentence_transformers import SentenceTransformer
import faiss
import pickle

load_dotenv()
db_user = os.getenv("DB_USER")
db_password = os.getenv("DB_PASSWORD")
db_host = os.getenv("DB_HOST")
db_name = os.getenv("DB_NAME")

print("📦 DB에서 향수 데이터를 불러옵니다...")
engine = create_engine(f'mysql+pymysql://{db_user}:{db_password}@{db_host}:3306/{db_name}')
df = pd.read_sql("SELECT * FROM perfumes", engine)

df['combined_text'] = df['category'] + " " + df['notes'] + " " + df['name'] + " " + df['brand']

print("🧠 한국어 AI 임베딩 모델을 다운로드합니다... (최초 1회 약 1~2분 소요)")
model = SentenceTransformer('snunlp/KR-SBERT-V40K-klueNLI-augSTS')

print("✨ 5,000개의 향수를 벡터로 변환 중입니다... (약 1분 소요)")
embeddings = model.encode(df['combined_text'].tolist())

print("🔍 FAISS 검색 인덱스를 구축하고 저장합니다...")
dimension = embeddings.shape[1]
index = faiss.IndexFlatL2(dimension)
index.add(embeddings)

# 완성된 벡터 지도(index)와 원본 데이터(pkl)를 파일로 저장
faiss.write_index(index, "perfumes.index")
with open("perfumes_meta.pkl", "wb") as f:
    pickle.dump(df.to_dict('records'), f)

print("🎉 완벽합니다! FAISS 인덱스 구축이 완료되었습니다.")