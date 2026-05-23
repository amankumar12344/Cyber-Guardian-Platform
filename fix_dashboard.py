import re

with open('d:/Cyber-Guardian-Java/frontend/dashboard.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Remove thymeleaf namespace
content = content.replace('<html xmlns:th="http://www.thymeleaf.org">', '<html>')

# Replace API Key display
content = re.sub(r'<span th:text="\$\{user\.apiKey\}".*?>.*?</span>', '<span id="navApiKey" style="color: var(--primary);"></span>', content)

# Replace Email display
content = re.sub(r'<span th:text="\$\{user\.email\}".*?>.*?</span>', '<span id="navUserEmail" style="color: #fff; font-weight: 600;"></span>', content)

# Replace target dropdown
target_dropdown_old = r'<form id="targetForm".*?</form>'
target_dropdown_new = '''
<div style="display: flex; align-items: center; gap: 10px;">
    <select id="targetSelector" onchange="changeTarget(this.value)" style="padding: 10px 18px; border-radius: 10px; background: rgba(0, 0, 0, 0.4); color: var(--primary); border: 1px solid var(--border); outline: none; font-weight: 600; font-size: 13px; cursor: pointer;">
        <option value="ALL">🔍 ALL TARGETS</option>
    </select>
</div>
'''
content = re.sub(target_dropdown_old, target_dropdown_new, content, flags=re.DOTALL)

# Replace role badge
role_badge_old = r'<div th:if="\$\{role == \'ADMIN\'\}".*?</div>\s*<div th:if="\$\{role == \'POLICE\'\}".*?</div>'
role_badge_new = '''<div id="roleBadgeContainer"></div>'''
content = re.sub(role_badge_old, role_badge_new, content, flags=re.DOTALL)

# Stats
content = re.sub(r'<h4[^>]*th:text="\$\{totalBreaches\}"[^>]*>.*?</h4>', '<h4 id="statTotalBreaches" style="font-size: 32px; font-weight: 800; color: var(--primary);">0</h4>', content)
content = re.sub(r'<h4[^>]*th:text="\$\{activeUsers\}"[^>]*>.*?</h4>', '<h4 id="statActiveUsers" style="font-size: 32px; font-weight: 800; color: #a855f7;">0</h4>', content)
content = re.sub(r'<h4[^>]*th:text="\$\{criticalAlerts\}"[^>]*>.*?</h4>', '<h4 id="statCriticalAlerts" style="font-size: 32px; font-weight: 800; color: var(--danger);">0</h4>', content)

content = re.sub(r'<h3[^>]*th:text="\$\{totalBreaches\}"[^>]*>.*?</h3>', '<h3 id="statTotalBreaches2" style="font-size: 28px; font-weight: 800; margin-top: 8px; color: var(--police-blue);">0</h3>', content)
content = re.sub(r'<h3[^>]*th:text="\$\{activeUsers\}"[^>]*>.*?</h3>', '<h3 id="statActiveUsers2" style="font-size: 28px; font-weight: 800; margin-top: 8px; color: #a855f7;">0</h3>', content)
content = re.sub(r'<h3[^>]*th:text="\$\{criticalAlerts\}"[^>]*>.*?</h3>', '<h3 id="statCriticalAlerts2" style="font-size: 28px; font-weight: 800; margin-top: 8px; color: var(--danger);">0</h3>', content)

# Live monitor src
content = re.sub(r'<img id="liveMonitorSrc" th:src=".*?"', '<img id="liveMonitorSrc" src=""', content)

# Evidence Grid 1
evidence_grid1_old = r'<div class="evidence-grid">\s*<div th:each="file : \$\{screenshots\}".*?No screen evidence records captured.*?</div>\s*</div>'
evidence_grid1_new = '''<div class="evidence-grid" id="evidenceGrid1"></div>'''
content = re.sub(evidence_grid1_old, evidence_grid1_new, content, flags=re.DOTALL)

# Evidence Grid 2 (Gallery)
evidence_grid2_old = r'<div class="evidence-grid" id="galleryGrid">\s*<div th:each="file : \$\{screenshots\}".*?No screen evidence records captured.*?</div>\s*</div>'
evidence_grid2_new = '''<div class="evidence-grid" id="galleryGrid"></div>'''
content = re.sub(evidence_grid2_old, evidence_grid2_new, content, flags=re.DOTALL)

# Config form
content = re.sub(r'<form action="/save-telegram" method="POST"[^>]*>', '<form id="telegramForm" style="display: flex; flex-direction: column; gap: 15px;">', content)
content = re.sub(r'<input type="hidden" name="apiKey" th:value="\$\{user\.apiKey\}">', '', content)
content = re.sub(r'<input type="hidden" name="role" th:value="\$\{role\}">', '', content)
content = re.sub(r'th:value="\$\{user\.telegramBotToken\}"', 'id="telBotToken"', content)
content = re.sub(r'th:value="\$\{user\.telegramChatId\}"', 'id="telChatId"', content)

# Users table
users_table_old = r'<tbody>\s*<tr th:each="u : \$\{users\}">.*?</tr>\s*</tbody>'
users_table_new = '''<tbody id="usersTableBody"></tbody>'''
content = re.sub(users_table_old, users_table_new, content, flags=re.DOTALL)

# Logs table
logs_table_old = r'<tbody>\s*<tr th:each="log : \$\{logs\}">.*?</tbody>'
logs_table_new = '''<tbody id="logsTableBody"></tbody>'''
content = re.sub(logs_table_old, logs_table_new, content, flags=re.DOTALL)

# Logout link
content = content.replace('href="/logout"', 'href="#" onclick="logoutUser()"')

# Download link
content = content.replace('href="/3rd-AI-Agent.exe"', 'href="http://localhost:8081/3rd-AI-Agent.exe"')

# JS Block Replacement
js_old = r'<script th:inline="javascript">.*?</script>'
js_new = '''<script src="dashboard.js"></script>'''
content = re.sub(js_old, js_new, content, flags=re.DOTALL)

with open('d:/Cyber-Guardian-Java/frontend/dashboard.html', 'w', encoding='utf-8') as f:
    f.write(content)

print("Dashboard HTML fixed.")
