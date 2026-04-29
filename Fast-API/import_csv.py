import pandas as pd
import os # 추가
from dotenv import load_dotenv # 추가
from sqlalchemy import create_engine

load_dotenv()
db_user = os.getenv("DB_USER")
db_password = os.getenv("DB_PASSWORD")
db_host = os.getenv("DB_HOST")
db_name = os.getenv("DB_NAME")

print("📦 CSV 파일을 읽는 중입니다...")
df = pd.read_csv('per_data.csv')

df = df[['Brand', 'Name', 'Categorys', 'Note', 'Picture']]
df.columns = ['brand', 'name', 'category', 'notes', 'image_url']

df = df.fillna('')

db_url = f'mysql+pymysql://{db_user}:{db_password}@{db_host}:3306/{db_name}'
engine = create_engine(db_url)

print("🚀 MySQL 데이터베이스로 데이터를 전송합니다... (약 10~30초 소요)")
df.to_sql(name='perfumes', con=engine, if_exists='append', index=False)
print("✅ 전송 완료! DB 저장이 끝났습니다.")
