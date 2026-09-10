import json
import logging
import os
import re
from contextlib import asynccontextmanager
from pathlib import Path
from vector_core import SearchEngine, SearchBusyError
from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel, Field

log = logging.getLogger(__name__)
ROOT = Path(__file__).resolve().parent

class SearchRequest(BaseModel):
    q: str = Field(min_length=1, max_length=1000)
    top_k: int = Field(default=20, ge=1, le=100)

@asynccontextmanager
async def lifespan(app):
    import faiss
    from sentence_transformers import SentenceTransformer
    folder = Path(os.getenv('INDEX_DIR', str(ROOT / 'artifacts')))
    version = (folder / 'CURRENT').read_text(encoding='ascii').strip()
    if not re.fullmatch('[a-f0-9]{32}', version):
        raise ValueError('Invalid index version')
    meta = json.loads((folder / f'{version}.json').read_text(encoding='utf-8'))
    app.state.engine = SearchEngine(SentenceTransformer(meta['model']),
        faiss.read_index(str(folder / f'{version}.index')), meta['ids'])
    yield

app = FastAPI(lifespan=lifespan)

def run_search(query, top_k):
    try:
        return {'results': app.state.engine.search(query, top_k)}
    except SearchBusyError:
        raise HTTPException(429, 'Search is busy; retry shortly', headers={'Retry-After': '2'}) from None
    except HTTPException:
        raise
    except ValueError:
        raise HTTPException(422, 'Invalid search input') from None
    except Exception:
        log.error('Vector search failed')
        raise HTTPException(503, 'Search is temporarily unavailable') from None

@app.post('/api/vector-search')
def search_perfumes(request: SearchRequest):
    return run_search(request.q, request.top_k)

@app.get('/api/vector-search')
def search_legacy(q: str = Query(min_length=1, max_length=1000), top_k: int = Query(default=20, ge=1, le=100)):
    return run_search(q, top_k)

@app.get('/health')
def health():
    return {'status': 'ok', 'products': len(app.state.engine.ids)}
