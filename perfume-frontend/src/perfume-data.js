export const LIKES_KEY = 'daily-perfume-likes';
export const perfumeKey = (p) => Number.isSafeInteger(p?.id) && p.id > 0
  ? `id:${p.id}` : `legacy:${p?.brand || ''}:${p?.name || ''}`;

export function validPerfume(p) {
  return !!p && typeof p === 'object' && typeof p.name === 'string' && p.name.trim()
    && typeof p.brand === 'string' && p.brand.trim();
}
export function readLikes(storage) {
  try {
    const saved = JSON.parse(storage.getItem(LIKES_KEY) || '[]');
    if (!Array.isArray(saved)) return [];
    return [...new Map(saved.filter(validPerfume).map(p => [perfumeKey(p), p])).values()];
  } catch { return []; }
}
export function togglePerfume(items, perfume) {
  const key = perfumeKey(perfume);
  return items.some(p => perfumeKey(p) === key)
    ? items.filter(p => perfumeKey(p) !== key) : [...items, perfume];
}
export function perfumeNotes(p) {
  if (Array.isArray(p?.noteImages)) return p.noteImages.filter(n => n && typeof n.note === 'string');
  return typeof p?.notes === 'string'
    ? p.notes.split(',').map(note => ({ note: note.trim(), kor: note.trim(), imageUrl: '/note-images/default.svg' })).filter(n => n.note)
    : [];
}
