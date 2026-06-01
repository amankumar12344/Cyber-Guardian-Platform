const fs = require('fs');
const path = require('path');

const frontendDir = path.join(__dirname, 'frontend');

const NEW_API_BASE = `const API_BASE = (() => {
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        return 'http://localhost:8081';
    }
    if (window.location.hostname === 'cyber-guardian-platform.onrender.com') {
        return window.location.origin;
    }
    // kavach.3rdai.co (Vercel) - use same-origin /api/* route
    // This goes through Vercel serverless proxy to Render (bypasses ISP blocks)
    return window.location.origin;
})();`;

function processFile(fullPath, file) {
    let content = fs.readFileSync(fullPath, 'utf8');
    const apiBaseRegex = /const API_BASE = \(\(\) => \{[\s\S]*?\}\)\(\);/g;
    if (apiBaseRegex.test(content)) {
        apiBaseRegex.lastIndex = 0;
        const newContent = content.replace(apiBaseRegex, NEW_API_BASE);
        if (newContent !== content) {
            fs.writeFileSync(fullPath, newContent, 'utf8');
            console.log(`Updated: ${file}`);
        }
    }
}

function processDir(dir) {
    fs.readdirSync(dir).forEach(file => {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            processDir(fullPath);
        } else if ((file.endsWith('.html') || file.endsWith('.js')) && 
                   !['proxy.js', 'scratch_fix_final.js', 'fix_api_base.js', 'fix_dashboard.js'].includes(file)) {
            processFile(fullPath, file);
        }
    });
}

processDir(frontendDir);
console.log('All frontend files updated!');
