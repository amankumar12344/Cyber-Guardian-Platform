const fs = require('fs');
const path = require('path');

const frontendDir = path.join(__dirname, 'frontend');

const NEW_API_BASE = `const API_BASE = (() => {
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        return 'http://localhost:8081';
    }
    // For all external/static hosting - always use Render backend directly
    // CORS is enabled with * on the backend
    return 'https://cyber-guardian-platform.onrender.com';
})();`;

function processFile(fullPath, file) {
    let content = fs.readFileSync(fullPath, 'utf8');
    
    // Match any existing API_BASE block
    const apiBaseRegex = /const API_BASE = \(\(\) => \{[\s\S]*?\}\)\(\);/g;
    if (apiBaseRegex.test(content)) {
        apiBaseRegex.lastIndex = 0;
        const newContent = content.replace(apiBaseRegex, NEW_API_BASE);
        if (newContent !== content) {
            fs.writeFileSync(fullPath, newContent, 'utf8');
            console.log(`Updated API_BASE: ${file}`);
        }
    }
}

function processDir(dir) {
    fs.readdirSync(dir).forEach(file => {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            processDir(fullPath);
        } else if ((file.endsWith('.html') || file.endsWith('.js')) && file !== 'proxy.js' && file !== 'scratch_fix_final.js') {
            processFile(fullPath, file);
        }
    });
}

processDir(frontendDir);
console.log('Done!');
