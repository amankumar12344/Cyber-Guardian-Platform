document.addEventListener('DOMContentLoaded', function() {
    const loginView = document.getElementById('login-view');
    const signupView = document.getElementById('signup-view');
    const controlView = document.getElementById('control-view');
    const logsView = document.getElementById('logs-view');
    const logsContainer = document.getElementById('logs-container');
    const statusValue = document.getElementById('status-value');
    
    const lockBtn = document.getElementById('lockBtn');
    const unlockBtn = document.getElementById('unlockBtn');
    const loginBtn = document.getElementById('loginBtn');
    const signupBtn = document.getElementById('signupBtn');
    const logoutBtn = document.getElementById('logoutBtn');
    const viewLogsBtn = document.getElementById('viewLogsBtn');
    const backToControlBtn = document.getElementById('backToControlBtn');
    const showSignupBtn = document.getElementById('showSignupBtn');
    const showLoginBtn = document.getElementById('showLoginBtn');

    // Check if already logged in
    chrome.storage.local.get(['isLoggedIn'], function(result) {
        if (result.isLoggedIn) {
            showControlView();
        }
    });

    function showControlView() {
        loginView.style.display = 'none';
        signupView.style.display = 'none';
        controlView.style.display = 'block';
        logsView.style.display = 'none';
        updateStatus();
    }

    function showLoginView() {
        loginView.style.display = 'block';
        signupView.style.display = 'none';
        controlView.style.display = 'none';
        logsView.style.display = 'none';
        chrome.storage.local.set({isLoggedIn: false});
    }

    function showLogsView() {
        controlView.style.display = 'none';
        logsView.style.display = 'block';
        fetchLogs();
    }

    showSignupBtn.addEventListener('click', () => {
        loginView.style.display = 'none';
        signupView.style.display = 'block';
    });

    showLoginBtn.addEventListener('click', showLoginView);
    viewLogsBtn.addEventListener('click', showLogsView);
    backToControlBtn.addEventListener('click', showControlView);

    loginBtn.addEventListener('click', () => {
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        if (email && password) {
            const formData = new FormData();
            formData.append('email', email);
            formData.append('password', password);

            fetch('http://localhost:8081/api/login', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    chrome.storage.local.set({isLoggedIn: true, userEmail: email, apiKey: data.apiKey});
                    showControlView();
                } else {
                    alert(data.message);
                }
            })
            .catch(() => alert("Could not connect to Agent."));
        }
    });

    signupBtn.addEventListener('click', () => {
        const email = document.getElementById('regEmail').value;
        const password = document.getElementById('regPassword').value;

        if (email && password) {
            const formData = new FormData();
            formData.append('email', email);
            formData.append('password', password);

            fetch('http://localhost:8081/api/register', {
                method: 'POST',
                body: formData
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert("Account Created! Please Login.");
                    showLoginView();
                } else {
                    alert(data.message);
                }
            })
        }
    });

    logoutBtn.addEventListener('click', showLoginView);

    function updateStatus() {
        fetch('http://localhost:8081/api/status')
            .then(response => response.json())
            .then(data => {
                statusValue.textContent = data.locked ? "LOCKED" : "UNLOCKED";
                statusValue.className = data.locked ? "locked" : "unlocked";
            })
            .catch(() => {
                statusValue.textContent = "OFFLINE";
                statusValue.className = "locked";
            });
    }

    function fetchLogs() {
        logsContainer.innerHTML = '<p style="color: #64748b; text-align: center;">Loading Activity...</p>';
        fetch('http://localhost:8081/api/logs')
            .then(response => response.json())
            .then(logs => {
                logsContainer.innerHTML = '';
                if (logs.length === 0) {
                    logsContainer.innerHTML = '<p style="color: #64748b; text-align: center;">No activity recorded yet.</p>';
                    return;
                }
                logs.reverse().forEach(log => {
                    const logEl = document.createElement('div');
                    logEl.style.background = '#1e293b';
                    logEl.style.padding = '10px';
                    logEl.style.borderRadius = '8px';
                    logEl.style.marginBottom = '10px';
                    logEl.style.border = '1px solid #334155';

                    const time = new Date(log.timestamp).toLocaleString();
                    let imgHtml = '';
                    if (log.screenshotPath) {
                        imgHtml = `<img src="http://localhost:8081/api/screenshots/${log.screenshotPath}" style="width: 100%; border-radius: 5px; margin-top: 10px; cursor: pointer;" onclick="window.open(this.src)">`;
                    }

                    logEl.innerHTML = `
                        <div style="font-weight: bold; color: #ef4444;">🚫 ${log.appName} BLOCKED</div>
                        <div style="font-size: 11px; color: #94a3b8;">${time}</div>
                        <div style="font-size: 12px; margin-top: 5px;">${log.details}</div>
                        ${imgHtml}
                    `;
                    logsContainer.appendChild(logEl);
                });
            })
            .catch(() => logsContainer.innerHTML = '<p style="color: #ef4444; text-align: center;">Failed to load logs.</p>');
    }

    lockBtn.addEventListener('click', () => {
        fetch('http://localhost:8081/api/control?action=lock', { method: 'POST' }).then(() => updateStatus());
    });

    unlockBtn.addEventListener('click', () => {
        fetch('http://localhost:8081/api/control?action=unlock', { method: 'POST' }).then(() => updateStatus());
    });

    const linkTelegramBtn = document.getElementById('linkTelegramBtn');
    linkTelegramBtn.addEventListener('click', () => {
        chrome.storage.local.get(['userEmail'], function(result) {
            if (result.userEmail) {
                // Link to official bot with start parameter (email)
                const botUsername = "Ak47_kumar_bot"; 
                const url = `https://t.me/${botUsername}?start=${result.userEmail}`;
                window.open(url, '_blank');
            } else {
                alert("Please log in first.");
            }
        });
    });

    setInterval(() => {
        if (controlView.style.display === 'block') updateStatus();
    }, 5000);
});
