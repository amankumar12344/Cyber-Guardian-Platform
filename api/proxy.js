module.exports = async (req, res) => {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Credentials', true);
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS,PATCH,DELETE,POST,PUT');
    res.setHeader(
        'Access-Control-Allow-Headers',
        'X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version, X-API-Key, Role'
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
            'Content-Type': req.headers['content-type'] || 'application/x-www-form-urlencoded',
        };
        if (req.headers['x-api-key']) headers['X-API-Key'] = req.headers['x-api-key'];
        if (req.headers['role']) headers['Role'] = req.headers['role'];

        let body = undefined;
        if (req.method === 'POST') {
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

        const response = await fetch(targetUrl, {
            method: req.method,
            headers: headers,
            body: body
        });

        const contentType = response.headers.get('content-type');
        const data = await response.text();

        if (contentType) {
            res.setHeader('Content-Type', contentType);
        }
        res.status(response.status).send(data);
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
};
