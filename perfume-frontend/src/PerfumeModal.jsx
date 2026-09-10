import { useEffect, useRef } from 'react';
import NoteList from './NoteList';

export default function PerfumeModal({ perfume, recommendations, loading, error, onClose, onSelect }) {
  const dialog = useRef(null);
  useEffect(() => {
    if (!perfume) return;
    const element = dialog.current;
    const previous = document.activeElement;
    element.showModal();
    return () => { element.close(); previous?.focus?.(); };
  }, [perfume]);
  if (!perfume) return null;
  return <dialog ref={dialog} className="perfume-dialog" aria-labelledby="perfume-title"
    onCancel={e => { e.preventDefault(); onClose(); }}
    onClick={e => { if (e.target === dialog.current) onClose(); }}>
    <div className="modal-content">
      <button type="button" className="modal-close" aria-label="상세 창 닫기" onClick={onClose}>×</button>
      <span>{perfume.brand}</span>
      <h2 id="perfume-title">{perfume.name}</h2>
      <p>{perfume.description || '등록된 상세 설명이 없습니다.'}</p>
      <h3>등록된 향 노트</h3>
      <NoteList perfume={perfume} />
      <h4>비슷한 향수</h4>
      {loading && <p role="status">상세 정보를 불러오고 있습니다.</p>}
      {error && <p role="alert">{error}</p>}
      {!loading && !error && recommendations.length === 0 && <p>비슷한 향수가 없습니다.</p>}
      <div className="chat-results">
        {recommendations.map(p => <button type="button" key={p.id} className="chat-perf-card" onClick={() => onSelect(p)}>
          <small>{p.brand}</small><br />{p.name}
        </button>)}
      </div>
    </div>
  </dialog>;
}
