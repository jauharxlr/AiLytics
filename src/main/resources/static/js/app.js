let authToken = localStorage.getItem('token');
let currentUser = JSON.parse(localStorage.getItem('user'));
let pollInterval = null;

document.addEventListener('DOMContentLoaded', () => {
    initApp();

    // --- Authentication ---
    const loginForm = document.getElementById('loginForm');
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            username: document.getElementById('loginUsername').value,
            password: document.getElementById('loginPassword').value
        };

        try {
            const res = await fetch('/api/v1/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error('Invalid credentials');
            const data = await res.json();
            
            authToken = data.token;
            currentUser = { username: data.username, role: data.role };
            localStorage.setItem('token', authToken);
            localStorage.setItem('user', JSON.stringify(currentUser));
            
            initApp();
        } catch (err) {
            alert(err.message);
        }
    });

    // --- Drag & Drop ---
    const dropzone = document.getElementById('dropzone');
    const fileInput = document.getElementById('fileInput');
    const selectedFileName = document.getElementById('selectedFileName');
    
    dropzone.addEventListener('click', () => fileInput.click());
    dropzone.addEventListener('dragover', (e) => { e.preventDefault(); dropzone.classList.add('border-indigo-500', 'bg-indigo-50'); });
    dropzone.addEventListener('dragleave', () => { dropzone.classList.remove('border-indigo-500', 'bg-indigo-50'); });
    dropzone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropzone.classList.remove('border-indigo-500', 'bg-indigo-50');
        if (e.dataTransfer.files.length) {
            fileInput.files = e.dataTransfer.files;
            handleFileSelect();
        }
    });
    fileInput.addEventListener('change', handleFileSelect);

    function handleFileSelect() {
        if (fileInput.files.length) {
            selectedFileName.textContent = fileInput.files[0].name;
            selectedFileName.classList.remove('hidden');
        }
    }

    // --- Workflow Submission ---
    const uploadForm = document.getElementById('uploadForm');
    uploadForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const file = fileInput.files[0];
        if (!file) return alert('Select document first');

        const formData = new FormData();
        formData.append('file', file);
        formData.append('action', document.getElementById('actionSelect').value);
        formData.append('username', document.getElementById('portalUsername').value);
        formData.append('password', document.getElementById('portalPassword').value);

        try {
            const res = await fetch('/api/v1/process', {
                method: 'POST',
                headers: { 'Authorization': `Bearer ${authToken}` },
                body: formData
            });
            const data = await res.json();
            startMonitoring(data.jobId);
            uploadForm.reset();
            selectedFileName.classList.add('hidden');
        } catch (err) {
            alert('Mission failed: ' + err.message);
        }
    });

    // --- User Management (Master Only) ---
    const createUserForm = document.getElementById('createUserForm');
    createUserForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            username: document.getElementById('newUsername').value,
            password: document.getElementById('newPassword').value
        };

        try {
            const res = await fetch('/api/v1/admin/users', {
                method: 'POST',
                headers: { 
                    'Authorization': `Bearer ${authToken}`,
                    'Content-Type': 'application/json' 
                },
                body: JSON.stringify(payload)
            });
            if (!res.ok) throw new Error('Action restricted or failure');
            closeModals();
            refreshUserList();
        } catch (err) {
            alert(err.message);
        }
    });

    document.getElementById('refreshBtn').onclick = refreshHistory;
});

// --- App Control ---
function initApp() {
    if (!authToken) {
        document.getElementById('loginScreen').classList.remove('hidden');
        document.getElementById('appLayout').classList.add('hidden');
        return;
    }

    document.getElementById('loginScreen').classList.add('hidden');
    document.getElementById('appLayout').classList.remove('hidden');
    document.getElementById('navUsername').textContent = currentUser.username;
    document.getElementById('navRole').textContent = currentUser.role === 'MASTER' ? 'Master Authority' : 'Staff Member';

    if (currentUser.role === 'MASTER') {
        document.getElementById('masterNav').classList.remove('hidden');
    } else {
        document.getElementById('masterNav').classList.add('hidden');
        switchTab('dashboard');
    }

    refreshHistory();
    if (currentUser.role === 'MASTER') refreshUserList();
}

function switchTab(tab) {
    document.getElementById('dashboardTab').classList.toggle('hidden', tab !== 'dashboard');
    document.getElementById('usersTab').classList.toggle('hidden', tab !== 'users');
    
    // Highlight active nav
    document.querySelectorAll('#masterNav button').forEach(btn => {
        btn.classList.toggle('text-indigo-600', btn.textContent.toLowerCase().includes(tab));
        btn.classList.toggle('font-bold', btn.textContent.toLowerCase().includes(tab));
    });
}

async function refreshHistory() {
    try {
        const res = await fetch('/api/v1/jobs', {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        const jobs = await res.json();
        const table = document.getElementById('historyTable');
        const empty = document.getElementById('emptyHistory');

        if (jobs.length > 0) {
            empty.classList.add('hidden');
            table.innerHTML = '';
                jobs.forEach(job => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td class="px-6 py-4 text-sm font-bold text-slate-700">${job.actionName}</td>
                        <td class="px-6 py-4">
                            <span class="px-2 py-1 rounded text-[10px] font-bold uppercase ${getStatusClass(job.status)}">${job.status}</span>
                        </td>
                        <td class="px-6 py-4 text-sm text-slate-500 font-medium">${job.confidenceScore ? (job.confidenceScore * 100).toFixed(0) + '%' : '-'}</td>
                        <td class="px-6 py-4 text-sm font-mono text-slate-400">${job.resultId || '-'}</td>
                        <td class="px-6 py-4 text-right space-x-3">
                            <button onclick="watchVideo('${job.jobId}')" class="text-slate-400 hover:text-indigo-600 transition-all" title="Watch Playback">
                                <i class="fas fa-play-circle text-lg"></i>
                            </button>
                            ${job.status === 'AWAITING_APPROVAL' ? `<button onclick="verifyJob('${job.jobId}')" class="text-indigo-600 hover:text-indigo-900 text-xs font-bold uppercase tracking-wider">Verify</button>` : ''}
                            ${job.status === 'FAILED' ? `<button onclick="retryJob('${job.jobId}')" class="text-amber-600 hover:text-amber-900 text-xs font-bold uppercase tracking-wider">Retry</button>` : ''}
                        </td>
                    `;
                    table.appendChild(tr);
                });
        } else {
            empty.classList.remove('hidden');
        }
    } catch (err) {}
}

async function refreshUserList() {
    try {
        const res = await fetch('/api/v1/admin/users', {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });
        const users = await res.json();
        const table = document.getElementById('userTable');
        table.innerHTML = '';
        users.forEach(u => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="px-6 py-4 text-sm font-medium">${u.username}</td>
                <td class="px-6 py-4 text-xs font-bold text-slate-400">${u.role}</td>
                <td class="px-6 py-4 text-right">
                    <button onclick="deleteUser(${u.id})" class="text-red-400 hover:text-red-600"><i class="fas fa-trash-alt"></i></button>
                </td>
            `;
            table.appendChild(tr);
        });
    } catch (err) {}
}

// --- Status Helpers ---
function getStatusClass(status) {
    switch (status) {
        case 'COMPLETED': return 'bg-green-50 text-green-600';
        case 'FAILED': return 'bg-red-50 text-red-600';
        case 'AWAITING_APPROVAL': return 'bg-amber-50 text-amber-600';
        case 'PROCESSING': return 'bg-blue-50 text-blue-600';
        default: return 'bg-slate-50 text-slate-400';
    }
}

// --- UI Controls ---
window.logout = () => {
    localStorage.clear();
    location.reload();
};

window.showCreateUserModal = () => {
    document.getElementById('modalOverlay').classList.remove('hidden');
    document.getElementById('createUserModal').classList.remove('hidden');
};

window.closeModals = () => {
    document.getElementById('modalOverlay').classList.add('hidden');
    document.getElementById('createUserModal').classList.add('hidden');
    document.getElementById('verificationModal').classList.add('hidden');
    document.getElementById('videoModal').classList.add('hidden');
    document.getElementById('debugVideoPlayer').pause();
    document.getElementById('debugVideoPlayer').src = "";
};

window.watchVideo = (jobId) => {
    const videoPlayer = document.getElementById('debugVideoPlayer');
    videoPlayer.src = `/api/v1/jobs/${jobId}/debug/video`;
    
    document.getElementById('modalOverlay').classList.remove('hidden');
    document.getElementById('videoModal').classList.remove('hidden');
    videoPlayer.play().catch(err => {
        console.warn("Auto-play blocked or video missing", err);
        alert("Video playback unavailable for this mission.");
        closeModals();
    });
};

window.verifyJob = async (jobId) => {
    const res = await fetch(`/api/v1/status/${jobId}`, { headers: { 'Authorization': `Bearer ${authToken}` } });
    const job = await res.json();
    
    const container = document.getElementById('extractedDataFields');
    container.innerHTML = '';
    Object.entries(job.extractedData).forEach(([k, v]) => {
        const div = document.createElement('div');
        div.innerHTML = `
            <label class="block text-[10px] font-bold text-slate-400 uppercase mb-1">${k}</label>
            <input type="text" data-key="${k}" value="${v}" class="w-full rounded-xl border-slate-200 text-sm">
        `;
        container.appendChild(div);
    });

    document.getElementById('modalOverlay').classList.remove('hidden');
    document.getElementById('verificationModal').classList.remove('hidden');
    document.getElementById('resumeBtn').onclick = () => resumeJob(jobId);
};

async function resumeJob(jobId) {
    const data = {};
    document.querySelectorAll('#extractedDataFields input').forEach(i => data[i.dataset.key] = i.value);
    await fetch(`/api/v1/jobs/${jobId}/verify`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${authToken}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    closeModals();
    startMonitoring(jobId);
}

window.retryJob = async (jobId) => {
    await fetch(`/api/v1/jobs/${jobId}/retry`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${authToken}` }
    });
    startMonitoring(jobId);
};

function startMonitoring(jobId) {
    document.getElementById('activeJobCard').classList.remove('hidden');
    if (pollInterval) clearInterval(pollInterval);
    pollInterval = setInterval(async () => {
        const res = await fetch(`/api/v1/status/${jobId}`, { headers: { 'Authorization': `Bearer ${authToken}` } });
        const job = await res.json();
        updateStepper(job);
        if (job.status === 'COMPLETED' || job.status === 'FAILED') {
            clearInterval(pollInterval);
            setTimeout(() => document.getElementById('activeJobCard').classList.add('hidden'), 5000);
            refreshHistory();
        }
    }, 2000);
}

function updateStepper(job) {
    document.getElementById('jobStatusBadge').textContent = job.status;
    const s1I = document.getElementById('step1Icon'), s1R = document.getElementById('step1Row');
    const s2I = document.getElementById('step2Icon'), s2R = document.getElementById('step2Row');
    const s3I = document.getElementById('step3Icon'), s3R = document.getElementById('step3Row');

    if (job.status === 'PROCESSING') {
        s1I.className = "w-8 h-8 rounded-full bg-indigo-100 text-indigo-600 border-indigo-600 flex items-center justify-center mr-4 transition-all step-pulse";
        s1R.classList.add('text-indigo-600');
    } else if (job.status === 'COMPLETED') {
        [s1I, s2I, s3I].forEach(i => i.innerHTML = '<i class="fas fa-check"></i>');
        [s1I, s2I, s3I].forEach(i => i.className = "w-8 h-8 rounded-full bg-green-100 text-green-600 border-green-600 flex items-center justify-center mr-4");
    }
}
