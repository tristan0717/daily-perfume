import { perfumeNotes } from './perfume-data';

export default function NoteList({ perfume, onSearch, onHover, onMove }) {
  const notes = perfumeNotes(perfume);
  if (!notes.length) return <span>등록된 노트 정보가 없습니다.</span>;
  const groups = [['TOP', perfume.topNotes], ['MID', perfume.middleNotes], ['BASE', perfume.baseNotes]];
  const hasGroups = groups.some(([, values]) => Array.isArray(values) && values.some(n => n?.note));
  const chip = n => onSearch ? <button type="button" key={n.note} className="note-chip"
    onMouseEnter={() => onHover?.(n)} onMouseLeave={() => onHover?.(null)} onMouseMove={onMove}
    onClick={e => { e.stopPropagation(); onSearch(`"${n.kor || n.note}" 노트가 들어간 다른 향수 찾아줘`); }}>
    {n.kor || n.note}
  </button> : <span className="note-chip" key={n.note}>{n.kor || n.note}</span>;
  return <div className="note-list">
    {hasGroups ? groups.map(([label, values]) => Array.isArray(values) && values.length > 0 &&
      <div key={label}><small>{label}: </small>{values.filter(n => n?.note).map(chip)}</div>) : notes.map(chip)}
  </div>;
}
