module.exports = async (req, res) => {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Credentials', true);
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS,PATCH,DELETE,POST,PUT');
    res.setHeader(
        'Access-Control-Allow-Headers',
        'X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version, X-API-Key, Role, Authorization'
    );

    if (req.method === 'OPTIONS') {
        res.status(200).end();
        return;
    }

    // Extract path to append to Render backend URL
    const path = req.url.replace('/api/proxy', '');
    const targetUrl = `https://cyber-guardian-platform.onrender.com${path}`;

    try {
        const headers = {
            'Content-Type': req.headers['content-type'] || 'application/json',
        };
        if (req.headers['x-api-key']) headers['X-API-Key'] = req.headers['x-api-key'];
        if (req.headers['role']) headers['Role'] = req.headers['role'];
        if (req.headers['authorization']) headers['Authorization'] = req.headers['authorization'];
        if (req.headers['accept']) headers['Accept'] = req.headers['accept'];

        let body = undefined;
        const methodsWithBody = ['POST', 'PUT', 'PATCH', 'DELETE'];
        if (methodsWithBody.includes(req.method)) {
            if (req.body !== undefined && req.body !== null) {
                if (typeof req.body === 'object') {
                    if (req.headers['content-type']?.includes('json')) {
                        body = JSON.stringify(req.body);
                    } else {
                        body = new URLSearchParams(req.body).toString();
                    }
                } else {
                    body = req.body;
                }
            }
        }

        const response = await fetch(targetUrl, {
            method: req.method,
            headers: headers,
            body: body
        });

        const contentType = response.headers.get('content-type');
        if (contentType) {
            res.setHeader('Content-Type', contentType);
        }

        // Use arrayBuffer to prevent binary data corruption and size inflation
        const arrayBuffer = await response.arrayBuffer();
        const buffer = Buffer.from(arrayBuffer);

        res.status(response.status).send(buffer);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};
