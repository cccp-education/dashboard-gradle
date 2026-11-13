#!/usr/bin/env node
/* ALGER Phase 0 prototype — minimal static server (no dependencies).
 * Usage: node serve.js [port]  → http://127.0.0.1:8822/
 * Prototype only, never published. */
const http = require('http');
const fs = require('fs');
const path = require('path');

const root = __dirname;
const port = Number(process.argv[2] || 8822);
const mime = {
    '.html': 'text/html; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.svg': 'image/svg+xml'
};

const server = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://127.0.0.1');
    const relative = decodeURIComponent(url.pathname === '/' ? '/index.html' : url.pathname);
    const file = path.normalize(path.join(root, relative));
    if (!file.startsWith(root + path.sep)) {
        res.writeHead(403);
        res.end('forbidden');
        return;
    }
    fs.readFile(file, (error, buffer) => {
        if (error) {
            res.writeHead(404);
            res.end('not found');
            return;
        }
        res.writeHead(200, { 'content-type': mime[path.extname(file)] || 'application/octet-stream' });
        res.end(buffer);
    });
});

server.listen(port, '127.0.0.1', () => {
    console.log('ALGER prototype: http://127.0.0.1:' + port + '/ (Ctrl+C to stop)');
});
