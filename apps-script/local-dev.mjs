import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const directory = path.dirname(fileURLToPath(import.meta.url));
const clientId = process.env.GOOGLE_OAUTH_CLIENT_ID || '866971493231-c6k2bc9kvutjsdasqbt45iqvvvd7v0b1.apps.googleusercontent.com';
const apiUrl = process.env.SHOP_SCRIPT_URL || 'https://script.google.com/macros/s/AKfycbyTtpGZiCXfvAN2Yg_p4sMlt8mNqdreRaVh_UMZDsNurFufY_KxIEw8wIuXh_4ueSPFbw/exec';
const port = Number(process.env.PORT || 5173);
const maxBodyBytes = 300_000;

const server = createServer(async (request, response) => {
  try {
    if (request.method === 'GET' && request.url === '/health') {
      response.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
      response.end(JSON.stringify({ ok: true, apiConfigured: Boolean(apiUrl), clientConfigured: Boolean(clientId) }));
      return;
    }

    if (request.method === 'POST' && request.url === '/api') {
      const chunks = [];
      let size = 0;
      for await (const chunk of request) {
        size += chunk.length;
        if (size > maxBodyBytes) throw new Error('Request is too large.');
        chunks.push(chunk);
      }
      const upstream = await fetch(apiUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: Buffer.concat(chunks),
        redirect: 'follow',
      });
      const body = await upstream.text();
      response.writeHead(upstream.ok ? 200 : upstream.status, {
        'Content-Type': upstream.headers.get('content-type') || 'application/json; charset=utf-8',
      });
      response.end(body);
      return;
    }

    if (request.method !== 'GET' || !['/', '/index.html'].includes(request.url)) {
      response.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      response.end('Not found');
      return;
    }

    const template = await readFile(path.join(directory, 'Index.html'), 'utf8');
    const html = template.replace('<?= googleClientId ?>', clientId);
    response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' });
    response.end(html);
  } catch (error) {
    response.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
    response.end(JSON.stringify({ ok: false, error: error.message || 'Could not reach Apps Script.' }));
  }
});

server.listen(port, '127.0.0.1', () => {
  console.log(`Avani Suto local preview: http://localhost:${port}/`);
  console.log(`Apps Script API: ${apiUrl}`);
});
