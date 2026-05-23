const fs = require('fs');
let content = fs.readFileSync('frontend/dashboard.html', 'utf8');

// Remove thymeleaf namespace
content = content.replace('<html xmlns:th="http://www.thymeleaf.org">', '<html>');

// Replace API Key display
content = content.replace(/<span th:text="\$\{user\.apiKey\}".*?>.*?<\/span>/g, '<span id="navApiKey" style="color: var(--primary);"></span>');

// Replace Email display
content = content.replace(/<span th:text="\$\{user\.email\}".*?>.*?<\/span>/g, '<span id="navUserEmail" style="color: #fff; font-weight: 600;"></span>');

// Replace target dropdown
const target_dropdown_old = /<form id="targetForm".*?<\/form>/s;
const target_dropdown_new = `
<div style="display: flex; align-items: center; gap: 10px;">
    <select id="targetSelector" onchange="changeTarget(this.value)" style="padding: 10px 18px; border-radius: 10px; background: rgba(0, 0, 0, 0.4); color: var(--primary); border: 1px solid var(--border); outline: none; font-weight: 600; font-size: 13px; cursor: pointer;">
        <option value="ALL">🔍 ALL TARGETS</option>
    </select>
</div>
`;
content = content.replace(target_dropdown_old, target_dropdown_new);

// Replace role badge
const role_badge_old = /<div th:if="\$\{role == 'ADMIN'\}"[\s\S]*?<\/div>\s*<div th:if="\$\{role == 'POLICE'\}"[\s\S]*?<\/div>/s;
const role_badge_new = '<div id="roleBadgeContainer"></div>';
content = content.replace(role_badge_old, role_badge_new);

// Stats
content = content.replace(/<h4[^>]*th:text="\$\{totalBreaches\}"[^>]*>.*?<\/h4>/s, '<h4 id="statTotalBreaches" style="font-size: 32px; font-weight: 800; color: var(--primary);">0</h4>');
content = content.replace(/<h4[^>]*th:text="\$\{activeUsers\}"[^>]*>.*?<\/h4>/s, '<h4 id="statActiveUsers" style="font-size: 32px; font-weight: 800; color: #a855f7;">0</h4>');
content = content.replace(/<h4[^>]*th:text="\$\{criticalAlerts\}"[^>]*>.*?<\/h4>/s, '<h4 id="statCriticalAlerts" style="font-size: 32px; font-weight: 800; color: var(--danger);">0</h4>');

content = content.replace(/<h3[^>]*th:text="\$\{totalBreaches\}"[^>]*>.*?<\/h3>/s, '<h3 id="statTotalBreaches2" style="font-size: 28px; font-weight: 800; margin-top: 8px; color: var(--police-blue);">0</h3>');
content = content.replace(/<h3[^>]*th:text="\$\{activeUsers\}"[^>]*>.*?<\/h3>/s, '<h3 id="statActiveUsers2" style="font-size: 28px; font-weight: 800; margin-top: 8px; color: #a855f7;">0</h3>');
content = content.replace(/<h3[^>]*th:text="\$\{criticalAlerts\}"[^>]*>.*?<\/h3>/s, '<h3 id="statCriticalAlerts2" style="font-size: 28px; font-weight: 800; margin-top: 8px; color: var(--danger);">0</h3>');

// Live monitor src
content = content.replace(/<img id="liveMonitorSrc" th:src=".*?"/, '<img id="liveMonitorSrc" src=""');

// Evidence Grid 1
const evidence_grid1_old = /<div class="evidence-grid">\s*<div th:each="file : \$\{screenshots\}"[\s\S]*?No screen evidence records captured.*?<\/div>\s*<\/div>/s;
const evidence_grid1_new = '<div class="evidence-grid" id="evidenceGrid1"></div>';
content = content.replace(evidence_grid1_old, evidence_grid1_new);

// Evidence Grid 2 (Gallery)
const evidence_grid2_old = /<div class="evidence-grid" id="galleryGrid">\s*<div th:each="file : \$\{screenshots\}"[\s\S]*?No screen evidence records captured.*?<\/div>\s*<\/div>/s;
const evidence_grid2_new = '<div class="evidence-grid" id="galleryGrid"></div>';
content = content.replace(evidence_grid2_old, evidence_grid2_new);

// Config form
content = content.replace(/<form action="\/save-telegram" method="POST"[^>]*>/s, '<form id="telegramForm" style="display: flex; flex-direction: column; gap: 15px;">');
content = content.replace(/<input type="hidden" name="apiKey" th:value="\$\{user\.apiKey\}">/s, '');
content = content.replace(/<input type="hidden" name="role" th:value="\$\{role\}">/s, '');
content = content.replace(/th:value="\$\{user\.telegramBotToken\}"/s, 'id="telBotToken"');
content = content.replace(/th:value="\$\{user\.telegramChatId\}"/s, 'id="telChatId"');

// Users table
const users_table_old = /<tbody>\s*<tr th:each="u : \$\{users\}">.*?<\/tr>\s*<\/body>/s;
const users_table_new = '<tbody id="usersTableBody"></tbody>';
content = content.replace(users_table_old, users_table_new);

// Logs table
const logs_table_old = /<tbody>\s*<tr th:each="log : \$\{logs\}">.*?<\/tbody>/s;
const logs_table_new = '<tbody id="logsTableBody"></tbody>';
content = content.replace(logs_table_old, logs_table_new);

// Logout link
content = content.replace('href="/logout"', 'href="#" onclick="logoutUser()"');

// Download link
content = content.replace('href="/3rd-AI-Agent.exe"', 'href="http://localhost:8081/3rd-AI-Agent.exe"');

// JS Block Replacement
const js_old = /<script th:inline="javascript">.*?<\/script>/s;
const js_new = '<script src="dashboard.js"></script>';
content = content.replace(js_old, js_new);

fs.writeFileSync('frontend/dashboard.html', content, 'utf8');
console.log("Dashboard HTML fixed.");
