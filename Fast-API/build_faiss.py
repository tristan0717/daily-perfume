"""Publish a versioned index and metadata together, then atomically switch CURRENT."""
import json
import os
from pathlib import Path
from uuid import uuid4
import pandas as pd
from db_config import ROOT, database_engine
from data_utils import note_text

MODEL = 'snunlp/KR-SBERT-V40K-klueNLI-augSTS'

def main():
    import faiss
    from sentence_transformers import SentenceTransformer
    engine = database_engine()
    try:
        frame = pd.read_sql('SELECT id, brand, name, category, notes FROM perfumes ORDER BY id', engine)
    finally:
        engine.dispose()
    if frame.empty or frame['id'].isna().any() or frame['id'].duplicated().any():
        raise ValueError('A non-empty database with unique IDs is required')
    frame['notes'] = frame['notes'].fillna('').map(note_text)
    texts = frame[['category', 'notes', 'name', 'brand']].fillna('').astype(str).agg(' '.join, axis=1)
    model = SentenceTransformer(MODEL)
    embeddings = model.encode(texts.tolist(), normalize_embeddings=True, convert_to_numpy=True).astype('float32')
    index = faiss.IndexFlatIP(embeddings.shape[1])
    index.add(embeddings)
    folder = Path(os.getenv('INDEX_DIR', str(ROOT / 'artifacts')))
    folder.mkdir(parents=True, exist_ok=True)
    version = uuid4().hex
    faiss.write_index(index, str(folder / f'{version}.index'))
    (folder / f'{version}.json').write_text(json.dumps({'model': MODEL, 'ids': frame['id'].astype(int).tolist()}), encoding='utf-8')
    marker = folder / f'.CURRENT-{version}'
    marker.write_text(version, encoding='ascii')
    marker.replace(folder / 'CURRENT')
    print(f'Published {len(frame)} products. Restart FastAPI to load this version.')

if __name__ == '__main__':
    main()
