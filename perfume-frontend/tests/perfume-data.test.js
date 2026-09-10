import test from 'node:test';
import assert from 'node:assert/strict';
import { readLikes, togglePerfume, perfumeKey, perfumeNotes } from '../src/perfume-data.js';

test('damaged and non-array storage never prevents startup', () => {
  for (const text of ['{', 'null', '{}', '12', '[null,{},"bad"]']) {
    assert.deepEqual(readLikes({ getItem: () => text }), []);
  }
  assert.deepEqual(readLikes({ getItem: () => { throw Error('denied'); } }), []);
});
test('same name with different database IDs stays independent', () => {
  const a = { id: 1, name: 'Rose', brand: 'A' }, b = { id: 2, name: 'Rose', brand: 'B' };
  assert.deepEqual(togglePerfume([a], b), [a, b]);
  assert.deepEqual(togglePerfume([a, b], a), [b]);
  assert.notEqual(perfumeKey(a), perfumeKey(b));
});
test('legacy likes survive safely and are deduplicated', () => {
  const item = { name: 'Rose', brand: 'A' };
  assert.deepEqual(readLikes({ getItem: () => JSON.stringify([item, item]) }), [item]);
});
test('canonical note images and ordinary DB notes are supported', () => {
  assert.deepEqual(perfumeNotes({ notes: 'Rose, Jasmine' }).map(n => n.note), ['Rose', 'Jasmine']);
  assert.deepEqual(perfumeNotes({ noteImages: [null, { note: 'Rose', imageUrl: '/rose.webp' }] }), [{ note: 'Rose', imageUrl: '/rose.webp' }]);
});
