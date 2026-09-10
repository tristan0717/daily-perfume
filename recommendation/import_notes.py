"""Validate first, then upsert translations in one transaction."""
import argparse
import csv
import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read_notes(path):
    with Path(path).open(encoding='utf-8-sig', newline='') as stream:
        reader = csv.reader(stream)
        header = next(reader, None)
        if not header or len(header) != 2:
            raise ValueError('Expected a two-column translation CSV')
        result = {}
        for line, row in enumerate(reader, 2):
            if not row or not any(cell.strip() for cell in row):
                continue
            if len(row) != 2 or not all(cell.strip() for cell in row):
                raise ValueError(f'Invalid translation at line {line}')
            eng, kor = (cell.strip() for cell in row)
            if max(len(eng), len(kor)) > 255:
                raise ValueError(f'Translation too long at line {line}')
            key = eng.casefold()
            if key in result and result[key][1] != kor:
                raise ValueError(f'Conflicting translation at line {line}: {eng}')
            result[key] = (eng, kor)
        if not result:
            raise ValueError('Translation CSV is empty')
        return list(result.values())

def main():
    import pymysql
    from dotenv import load_dotenv
    parser = argparse.ArgumentParser()
    parser.add_argument('--csv', default=str(ROOT / 'Note.csv'))
    args = parser.parse_args()
    rows = read_notes(args.csv)
    load_dotenv(ROOT / 'Fast-API' / '.env')
    conn = pymysql.connect(host=os.getenv('DB_HOST', 'localhost'),
        port=int(os.getenv('DB_PORT', '3306')), user=os.environ['DB_USER'],
        password=os.environ['DB_PASSWORD'], database=os.environ['DB_NAME'], charset='utf8mb4')
    try:
        with conn.cursor() as cursor:
            cursor.execute('CREATE TABLE IF NOT EXISTS note_mapping (note_eng VARCHAR(255) PRIMARY KEY, note_kor VARCHAR(255) NOT NULL) ENGINE=InnoDB')
            conn.commit()
            conn.begin()
            cursor.executemany('INSERT INTO note_mapping (note_eng,note_kor) VALUES (%s,%s) ON DUPLICATE KEY UPDATE note_kor=VALUES(note_kor)', rows)
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()
    print(f'Imported {len(rows)} translations; existing data preserved.')

if __name__ == '__main__':
    main()
