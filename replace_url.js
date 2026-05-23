const fs = require('fs');
const path = require('path');

const frontendDir = path.join(__dirname, 'frontend');
const oldUrl = 'http://localhost:8081';
const newUrl = 'https://cyber-guardian-platform.onrender.com';

function replaceInDir(dir) {
    fs.readdirSync(dir).forEach(file => {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            replaceInDir(fullPath);
        } else if (file.endsWith('.html') || file.endsWith('.js')) {
            let content = fs.readFileSync(fullPath, 'utf8');
            if (content.includes(oldUrl)) {
                content = content.split(oldUrl).join(newUrl);
                fs.writeFileSync(fullPath, content, 'utf8');
                console.log(`Updated: ${file}`);
            }
        }
    });
}

replaceInDir(frontendDir);
console.log('All URLs replaced successfully!');
