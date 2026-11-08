import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer, request } from 'node:http';

function send(url, options = {}) {
  return new Promise((resolve, reject) => {
    const req = request(url, { method: options.method ?? 'GET', headers: options.headers }, res => {
      res.resume(); res.on('end', () => resolve({ status: res.statusCode }));
    });
    req.on('error', reject);
    req.end(options.body);
  });
}
import { createAdminHandler } from './admin.mjs';

test('local API rejects cross-origin, invalid token and invalid updates; allows scoped requests', async () => {
  const calls = [];
  const handler = createAdminHandler({ token: 'test-token', publicUrl: 'https://assets.example', db: {
    list: async table => { calls.push(table); return []; },
    update: async (...args) => { calls.push(args); },
    insert: async row => { calls.push(row); },
  }, upload: async (...args) => { calls.push(args); } });
  const server = createServer((req, res) => handler(req, res, () => res.writeHead(404).end()));
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  const headers = { host: '127.0.0.1:5173', origin: 'http://127.0.0.1:5173', 'sec-fetch-site': 'same-origin', 'x-admin-token': 'test-token' };
  try {
    assert.equal((await send(base + '/api/session', { headers: { host: 'evil.example' } })).status, 403);
    assert.equal((await send(base + '/api/session', { headers: { ...headers, origin: 'https://evil.example' } })).status, 403);
    assert.equal((await send(base + '/api/remote', { headers: { ...headers, 'x-admin-token': 'wrong' } })).status, 403);
    assert.equal((await send(base + '/api/remote', { headers })).status, 200);
    assert.deepEqual(calls, ['soundscape_tracks']);
    assert.equal((await send(base + '/api/remote/100', { method: 'PATCH', headers, body: JSON.stringify({ name: 'not allowed' }) })).status, 400);
    assert.equal((await send(base + '/api/remote/100', { method: 'PATCH', headers, body: JSON.stringify({ is_active: false }) })).status, 200);
    assert.equal((await send(base + '/api/upload?path=lofi%2F..%2Fbad.mp3', { method: 'POST', headers, body: 'x' })).status, 400);
    assert.equal((await send(base + '/api/upload?path=lofi%2Fgood.mp3', { method: 'POST', headers, body: 'x' })).status, 200);
  } finally { await new Promise(resolve => server.close(resolve)); }
});
