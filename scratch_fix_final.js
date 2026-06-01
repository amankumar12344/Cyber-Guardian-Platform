const fs = require('fs');
const path = require('path');

const frontendDir = path.join(__dirname, 'frontend');

function processDir(dir) {
    fs.readdirSync(dir).forEach(file => {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            processDir(fullPath);
        } else if ((file.endsWith('.html') || file.endsWith('.js')) && file !== 'proxy.js') {
            let content = fs.readFileSync(fullPath, 'utf8');
            let updated = false;

            // 1. Fix API_BASE - replace entire API_BASE block with clean version
            const apiBaseRegex = /const API_BASE = \(\(\) => \{[\s\S]*?\}\)\(\);/g;
            if (apiBaseRegex.test(content)) {
                apiBaseRegex.lastIndex = 0;
                content = content.replace(apiBaseRegex, `const API_BASE = (() => {
    const saved = localStorage.getItem('custom_api_base');
    if (saved) return saved;
    if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
        return 'http://localhost:8081';
    }
    if (window.location.protocol === 'file:') {
        return 'https://cyber-guardian-platform.onrender.com';
    }
    if (window.location.hostname === 'cyber-guardian-platform.onrender.com') {
        return window.location.origin;
    }
    // Vercel / Custom domain - use built-in proxy rewrite to bypass ISP blocks
    return window.location.origin + '/api/proxy';
})();`);
                updated = true;
            }

            // 2. Remove confirm/prompt popup from catch block - replace with simple alert
            const popupRegex = /const changeBase = confirm\([\s\S]*?}\s*}\s*}/g;
            if (popupRegex.test(content)) {
                popupRegex.lastIndex = 0;
                content = content.replace(popupRegex, `alert('Server connection error. Reload karke retry karein.');`);
                updated = true;
            }

            if (updated) {
                fs.writeFileSync(fullPath, content, 'utf8');
                console.log(`Updated: ${file}`);
            }
        }
    });
}

processDir(frontendDir);
console.log('Clean rewrite complete!');
