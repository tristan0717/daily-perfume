from threading import BoundedSemaphore

class SearchBusyError(Exception):
    pass

class SearchEngine:
    def __init__(self, model, index, ids):
        if index.ntotal != len(ids) or len(ids) != len(set(ids)):
            raise ValueError('Index and metadata do not match')
        self.model, self.index, self.ids = model, index, ids
        self.gate = BoundedSemaphore(2)

    def search(self, query, top_k):
        if not query.strip() or len(query) > 1000 or not 1 <= top_k <= 100:
            raise ValueError('Invalid search input')
        if not self.ids:
            return []
        if not self.gate.acquire(blocking=False):
            raise SearchBusyError('Search is busy')
        try:
            vector = self.model.encode([query], normalize_embeddings=True, convert_to_numpy=True).astype('float32')
            _, indices = self.index.search(vector, min(top_k, len(self.ids)))
            return [{'id': self.ids[int(i)]} for i in indices[0] if 0 <= i < len(self.ids)]
        finally:
            self.gate.release()

