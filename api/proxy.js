const https = require('https');
const http = require('http');

module.exports = async (req, res) => {
    // Enable CORS for all origins
    res.setHeader('Access-Control-Allow-Credentials', true);
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS,PATCH,DELETE,POST,PUT');
    res.setHeader('Access-Control-Allow-Headers', 'X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version, X-API-Key, Role, Authorization');

    if (req.method === 'OPTIONS') {
        res.status(200).end();
        return;
    }

    // Extract the path after /api/ and forward to Render
    // req.url for route /api/(.*) will be something like /api/login
    // We need to forward to https://cyber-guardian-platform.onrender.com/api/login
    const urlPath = req.url || '/';
    // The route catches /api/(.*), so the full path is already in req.url
    const targetUrl = `https://cyber-guardian-platform.onrender.com${urlPath}`;

    console.log(`[PROXY] ${req.method} ${urlPath} -> ${targetUrl}`);

    try {
        const fetchOptions = {
            method: req.method,
            headers: {
                'Content-Type': req.headers['content-type'] || 'application/x-www-form-urlencoded',
                'Accept': req.headers['accept'] || 'application/json',
            },
        };

        if (req.headers['x-api-key']) fetchOptions.headers['X-API-Key'] = req.headers['x-api-key'];
        if (req.headers['role']) fetchOptions.headers['Role'] = req.headers['role'];
        if (req.headers['authorization']) fetchOptions.headers['Authorization'] = req.headers['authorization'];

        // Read body for POST/PUT requests
        if (req.method === 'POST' || req.method === 'PUT' || req.method === 'PATCH') {
            const body = await new Promise((resolve, reject) => {
                let data = '';
                req.on('data', chunk => data += chunk);
                req.on('end', () => resolve(data));
                req.on('error', reject);
            });
            if (body) {
                fetchOptions.body = body;
                fetchOptions.headers['Content-Length'] = Buffer.byteLength(body).toString();
            }
        }

        const response = await fetch(targetUrl, fetchOptions);
        const contentType = response.headers.get('content-type') || 'application/json';
        const data = await response.text();

        res.setHeader('Content-Type', contentType);
        res.status(response.status).send(data);

    } catch (error) {
        console.error('[PROXY ERROR]', error.message);
        res.status(502).json({ 
            error: 'Backend unreachable', 
            message: error.message,
            success: false 
        });
    }
};
