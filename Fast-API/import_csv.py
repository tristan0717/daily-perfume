"""Repeatable import. Existing IDs are retained and no table is dropped."""
import argparse
import warnings
import json
import csv
from data_utils import parse_notes, note_text
from pathlib import Path
ROOT = Path(__file__).resolve().parent

COLUMNS = {'Brand': 'brand', 'Name': 'name', 'Categorys': 'category', 'Note': 'notes', 'Picture': 'image_url'}

def read_records(path):
    records = {}
    with open(path, encoding='utf-8-sig', newline='') as stream:
        reader = csv.DictReader(stream)
        if not set(COLUMNS).issubset(reader.fieldnames or []):
            raise ValueError('CSV requires: ' + ', '.join(COLUMNS))
        for line, row in enumerate(reader, 2):
            record = {target: (row.get(source) or '').strip() for source, target in COLUMNS.items()}
            if not record['name'] or not record['brand']:
                warnings.warn(f'Skipping CSV line {line}: Brand or Name is empty', stacklevel=2)
                continue
            record['category'] = note_text(record['category'])
            record['notes'] = json.dumps(parse_notes(record['notes']), ensure_ascii=False)
            for column, limit in {'brand':255, 'name':255, 'category':100, 'notes':4000, 'image_url':500}.items():
                if len(record[column]) > limit:
                    raise ValueError(f'{column} exceeds {limit} characters at line {line}')
            records[(record['brand'].casefold(), record['name'].casefold())] = record
    return list(records.values())

def import_records(engine, records):
    from sqlalchemy import text
    # The application creates the schema first. Serialize concurrent imports.
    with engine.connect() as conn:
        if conn.execute(text("SELECT GET_LOCK('daily_perfume_import', 30)")).scalar() != 1:
            raise RuntimeError('Another perfume import is running')
        conn.commit()
        try:
            with conn.begin():
                existing = conn.execute(text('SELECT id, brand, name FROM perfumes')).mappings().all()
                identities = {}
                for row in existing:
                    key = (row['brand'].casefold(), row['name'].casefold())
                    if key in identities:
                        raise ValueError('Existing duplicate products must be reconciled before importing')
                    identities[key] = row['id']
                for record in records:
                    key = (record['brand'].casefold(), record['name'].casefold())
                    if key in identities:
                        conn.execute(text('UPDATE perfumes SET category=:category, notes=:notes, image_url=:image_url WHERE id=:id'),
                                     {**record, 'id': identities[key]})
                    else:
                        result = conn.execute(text('INSERT INTO perfumes (brand,name,category,notes,image_url) VALUES (:brand,:name,:category,:notes,:image_url)'), record)
                        identities[key] = result.lastrowid
        finally:
            conn.execute(text("SELECT RELEASE_LOCK('daily_perfume_import')"))
            conn.commit()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--csv', type=str, default=str(ROOT / 'per_data.csv'))
    args = parser.parse_args()
    records = read_records(args.csv)  # Validate before touching the database.
    from db_config import database_engine
    engine = database_engine()
    try:
        import_records(engine, records)
    finally:
        engine.dispose()
    print(f'Imported {len(records)} products. Rebuild the search index next.')

if __name__ == '__main__':
    main()
