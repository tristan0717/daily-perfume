import csv
import pymysql # 아까 파이썬 서버에서 쓰셨던 라이브러리

# DB 연결 (본인 비밀번호로 꼭 수정하세요!)
conn = pymysql.connect(host='localhost', user='root', password='wjddn0717', db='perfume_db', charset='utf8mb4')
cursor = conn.cursor()

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
