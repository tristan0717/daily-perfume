import { chromium } from 'playwright';
import { spawn } from 'node:child_process';
import assert from 'node:assert/strict';
import { mkdir } from 'node:fs/promises';

const server = spawn('npm', ['run', 'dev', '--', '--host', '127.0.0.1'], { stdio: 'inherit' });
let browser;
const product = (id, brand = 'Test') => ({ id, brand, name: 'Rose', notes: 'Rose, Musk', description: 'DB description',
  noteImages: [{ note: 'Rose', kor: '장미', imageUrl: '/note-images/default.svg' }], topNotes: [], middleNotes: [], baseNotes: [] });
try {
  let ready = false;
  for (let attempt = 0; attempt < 120; attempt++) {
    try { if ((await fetch('http://127.0.0.1:5173')).ok) { ready = true; break; } } catch { /* Starting */ }
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  assert(ready, 'Vite did not start');
  browser = await chromium.launch();
  const page = await browser.newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  await page.addInitScript(() => localStorage.setItem('daily-perfume-likes', '{bad json'));
  await page.route('**/api/**', async route => {
    const path = new URL(route.request().url()).pathname;
    let body;
    if (path.endsWith('/search')) body = { recommendations: [product(1, 'A'), product(2, 'B')], custom_perfume: null, fallback: false };
    else if (path.endsWith('/recommendations')) body = [product(3, 'Related')];
    else body = product(Number(path.split('/').pop()), path.endsWith('/1') ? 'A' : 'B');
    await route.fulfill({ json: body });
  });
  await page.goto('http://127.0.0.1:5173');
  await page.getByRole('button', { name: '💬 바로 AI와 대화하기' }).click();
  await page.getByLabel('향수 검색 내용').fill('장미 향수');
  await page.getByLabel('검색 보내기').click();
  await page.locator('.chat-perf-card').first().waitFor();
  await page.locator('.chat-perf-card').first().getByText('🤍').click();
  await page.locator('.chat-perf-card').nth(1).getByText('🤍').click();
  await page.getByText('❤️ 찜한 향수 (2)', { exact: true }).click();
  assert.equal(await page.locator('.chat-perf-card').count(), 2);
  await page.locator('.chat-perf-card').first().click();
  await page.getByRole('dialog').waitFor();
  await page.getByRole('dialog').getByText('장미', { exact: true }).waitFor();
  await page.getByRole('dialog').getByText('Related', { exact: true }).waitFor();
  await page.getByLabel('상세 창 닫기').click();
  assert.equal(await page.getByRole('dialog').count(), 0);
  await page.getByText('Back to Chat', { exact: true }).click();
  await page.setViewportSize({ width: 390, height: 844 });
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), 'Mobile viewport overflows');
  await mkdir('test-results', { recursive: true });
  await page.screenshot({ path: 'test-results/mobile-chat.png', fullPage: true });
  assert.deepEqual(errors, []);
  console.log('Browser smoke tests passed: damaged storage, distinct likes, likes modal, API notes, mobile layout');
} finally {
  await browser?.close();
  server.kill('SIGTERM');
}
