document.addEventListener('DOMContentLoaded', () => {
    const dropzone = document.getElementById('dropzone');
    const fileInput = document.getElementById('fileInput');
    const selectedFileName = document.getElementById('selectedFileName');
    const uploadForm = document.getElementById('uploadForm');
    const historyTable = document.getElementById('historyTable');
    const emptyHistory = document.getElementById('emptyHistory');
    const activeJobCard = document.getElementById('activeJobCard');
    const jobStatusBadge = document.getElementById('jobStatusBadge');
    const approvalModal = document.getElementById('approvalModal');
    const extractedDataForm = document.getElementById('extractedDataForm');
    
    let currentJobId = null;
    let pollInterval = null;

    // --- Drag & Drop ---
    dropzone.addEventListener('click', () => fileInput.click());
    dropzone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropzone.classList.add('border-indigo-500', 'bg-indigo-50');
    });
    dropzone.addEventListener('dragleave', () => {
        dropzone.classList.remove('border-indigo-500', 'bg-indigo-50');
    });
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

    // --- Form Submission ---
    uploadForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const file = fileInput.files[0];
        if (!file) return alert('Please select a file');

        const formData = new FormData();
        formData.append('file', file);
        formData.append('action', document.getElementById('actionSelect').value);
        formData.append('username', document.getElementById('username').value);
        formData.append('password', document.getElementById('password').value);

        try {
            const res = await fetch('/api/v1/process', {
                method: 'POST',
                body: formData
            });
            const data = await res.json();
            if (data.jobId) {
                startMonitoring(data.jobId);
                uploadForm.reset();
                selectedFileName.classList.add('hidden');
            }
        } catch (err) {
            alert('Error starting job: ' + err.message);
        }
    });

    // --- Monitoring ---
    function startMonitoring(jobId) {
        currentJobId = jobId;
        activeJobCard.classList.remove('hidden');
        resetStepper();
        
        if (pollInterval) clearInterval(pollInterval);
        pollInterval = setInterval(() => pollJobStatus(jobId), 2000);
    }

    async function pollJobStatus(jobId) {
        try {
            const res = await fetch(`/api/v1/status/${jobId}`);
            if (!res.ok) return;
            const job = await res.json();

            updateUI(job);

            if (job.status === 'COMPLETED' || job.status === 'FAILED') {
                clearInterval(pollInterval);
                currentJobId = null;
                setTimeout(() => activeJobCard.classList.add('hidden'), 5000);
                refreshHistory();
            } else if (job.status === 'AWAITING_APPROVAL') {
                clearInterval(pollInterval);
                showApprovalUI(job);
            }
        } catch (err) {
            console.error('Polling error', err);
        }
    }

    function updateUI(job) {
        jobStatusBadge.textContent = job.status;
        
        // Simple stepper logic
        if (job.status === 'PROCESSING') {
            setStepActive(1);
        } else if (job.status === 'COMPLETED') {
            setStepComplete(1);
            setStepComplete(2);
            setStepComplete(3);
        }
    }

    function setStepActive(num) {
        const el = document.getElementById(`step${num}`);
        el.className = "w-6 h-6 rounded-full bg-indigo-100 flex items-center justify-center mr-3 text-indigo-600 animate-pulse";
    }

    function setStepComplete(num) {
        const el = document.getElementById(`step${num}`);
        el.className = "w-6 h-6 rounded-full bg-green-100 flex items-center justify-center mr-3 text-green-600";
        el.innerHTML = '<i class="fas fa-check text-xs"></i>';
    }

    function resetStepper() {
        for (let i = 1; i <= 3; i++) {
            const el = document.getElementById(`step${i}`);
            el.className = "w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center mr-3 text-slate-400";
            el.textContent = i;
        }
    }

    // --- HITL Verification ---
    function showApprovalUI(job) {
        extractedDataForm.innerHTML = '';
        Object.entries(job.extractedData).forEach(([key, value]) => {
            const div = document.createElement('div');
            div.innerHTML = `
                <label class="block text-xs font-bold text-slate-400 uppercase mb-1">${key}</label>
                <input type="text" data-key="${key}" value="${value}" class="w-full rounded-lg border-slate-200 text-sm focus:ring-indigo-500 focus:border-indigo-500">
            `;
            extractedDataForm.appendChild(div);
        });
        
        document.getElementById('confirmBtn').onclick = () => confirmApproval(job.jobId);
        approvalModal.classList.remove('hidden');
    }

    async function confirmApproval(jobId) {
        const correctedData = {};
        extractedDataForm.querySelectorAll('input').forEach(input => {
            correctedData[input.dataset.key] = input.value;
        });

        try {
            await fetch(`/api/v1/jobs/${jobId}/verify`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(correctedData)
            });
            closeModal();
            startMonitoring(jobId); // Resume polling
        } catch (err) {
            alert('Failed to submit: ' + err.message);
        }
    }

    window.closeModal = () => {
        approvalModal.classList.add('hidden');
    };

    // --- History ---
    async function refreshHistory() {
        try {
            const res = await fetch('/api/v1/jobs');
            const jobs = await res.json();
            
            if (jobs.length > 0) {
                emptyHistory.classList.add('hidden');
                historyTable.innerHTML = '';
                jobs.forEach(job => {
                    const tr = document.createElement('tr');
                    tr.className = "hover:bg-slate-50 transition-colors";
                    const statusClass = getStatusClass(job.status);
                    const confidence = job.confidenceScore ? (job.confidenceScore * 100).toFixed(0) + '%' : '-';
                    
                    tr.innerHTML = `
                        <td class="px-6 py-4 text-sm font-medium text-slate-700">${job.actionName}</td>
                        <td class="px-6 py-4">
                            <span class="px-2 py-1 rounded text-[10px] font-bold uppercase tracking-wider ${statusClass}">${job.status}</span>
                        </td>
                        <td class="px-6 py-4 text-sm text-slate-500">${confidence}</td>
                        <td class="px-6 py-4 text-sm font-mono text-slate-400">${job.resultId || '-'}</td>
                        <td class="px-6 py-4 text-right space-x-2">
                            ${job.status === 'AWAITING_APPROVAL' ? `<button onclick="showApprovalUIById('${job.jobId}')" class="text-indigo-600 hover:text-indigo-900 text-sm font-semibold">Verify</button>` : ''}
                            ${job.status === 'FAILED' ? `<button onclick="retryJob('${job.jobId}')" class="text-amber-600 hover:text-amber-900 text-sm font-semibold"><i class="fas fa-redo-alt mr-1"></i>Retry</button>` : ''}
                        </td>
                    `;
                    historyTable.appendChild(tr);
                });
            } else {
                emptyHistory.classList.remove('hidden');
            }
        } catch (err) {
            console.error('History refresh failed', err);
        }
    }

    function getStatusClass(status) {
        switch (status) {
            case 'COMPLETED': return 'bg-green-100 text-green-700';
            case 'FAILED': return 'bg-red-100 text-red-700';
            case 'AWAITING_APPROVAL': return 'bg-amber-100 text-amber-700';
            case 'PROCESSING': return 'bg-blue-100 text-blue-700';
            default: return 'bg-slate-100 text-slate-700';
        }
    }

    window.showApprovalUIById = async (jobId) => {
        const res = await fetch(`/api/v1/status/${jobId}`);
        const job = await res.json();
        showApprovalUI(job);
    };

    window.retryJob = async (jobId) => {
        try {
            const res = await fetch(`/api/v1/jobs/${jobId}/retry`, { method: 'POST' });
            if (res.ok) {
                startMonitoring(jobId);
            } else {
                const err = await res.json();
                alert(err.error || 'Failed to retry job');
            }
        } catch (err) {
            alert('Retry request failed: ' + err.message);
        }
    };

    document.getElementById('refreshBtn').onclick = refreshHistory;
    refreshHistory(); // Initial load
});
