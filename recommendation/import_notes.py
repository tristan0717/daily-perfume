import csv
import pymysql
import os                     # 운영체제 기능 사용
from dotenv import load_dotenv # .env 읽어오는 기능

load_dotenv()

db_password = os.getenv("DB_PASSWORD")

conn = pymysql.connect(host='localhost', user='root', password=db_password, db='perfume_db', charset='utf8mb4')

# 1. 테이블 새로 만들기 (기존에 꼬인게 있으면 덮어씁니다)
cursor.execute("DROP TABLE IF EXISTS note_mapping")
cursor.execute("""
    CREATE TABLE note_mapping (
        note_eng VARCHAR(255) PRIMARY KEY,
        note_kor VARCHAR(255)
    )
""")

# 2. 방금 만든 CSV 파일 읽어서 넣기
with open('/Users/hongjeong-u/Downloads/first project/Note.csv', 'r', encoding='utf-8') as f:
    reader = csv.reader(f)
    next(reader) # 첫 번째 줄(Note, note_kor)은 스킵
    
    for row in reader:
        if len(row) == 2:
            # 영어 단어, 한글 단어 양옆 공백 제거 후 DB에 저장
            cursor.execute(
                "INSERT IGNORE INTO note_mapping (note_eng, note_kor) VALUES (%s, %s)",
                (row[0].strip(), row[1].strip())
            )

conn.commit()
cursor.close()
conn.close()

print("🎉 노트 매핑 데이터 DB 저장 완벽하게 성공!")
