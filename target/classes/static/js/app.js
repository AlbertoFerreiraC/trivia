const API_BASE = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
    ? 'http://localhost:8081/api/v1'
    : window.location.hostname.includes('railway.app')
        ? 'trivia-fersal.up.railway.app'
        : 'https://seguridad-vial-5mt2.onrender.com/api/v1';

// Estado global de juego en la tablet
let publicCategories = [];
let selectedCategoryId = null;
let currentGameId = sessionStorage.getItem('activeGameId') || null;
let currentQuestionId = null;
let countdownInterval = null;
let answerSubmitted = false;

// Control de Audio Web nativo mejorado para tablets y efectos de presión
let audioCtx = null;
let lastBeepSecond = -1;

function getAudioContext() {
    try {
        if (!audioCtx) {
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            if (AudioContext) {
                audioCtx = new AudioContext();
            }
        }
        if (audioCtx && audioCtx.state === 'suspended') {
            audioCtx.resume();
        }
    } catch (e) {
        console.error("Error al iniciar AudioContext:", e);
    }
    return audioCtx;
}

// Generador de efectos de sonido de tensión / tic-tac
function playPressureTick(isCritical) {
    try {
        const ctx = getAudioContext();
        if (!ctx) return;

        if (ctx.state === 'suspended') {
            ctx.resume();
        }

        const osc = ctx.createOscillator();
        const gain = ctx.createGain();

        osc.type = isCritical ? 'sawtooth' : 'sine';
        osc.frequency.setValueAtTime(isCritical ? 880 : 440, ctx.currentTime);

        gain.gain.setValueAtTime(0.15, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + (isCritical ? 0.15 : 0.09));

        osc.connect(gain);
        gain.connect(ctx.destination);

        osc.start();
        osc.stop(ctx.currentTime + (isCritical ? 0.15 : 0.09));
    } catch (e) {
        // Ignorar restricciones si el navegador aún no permite audio
    }
}

// Estado del Panel Administrativo
let adminQuestionsPage = 0;
let adminQuestionsList = [];
let questionModalInstance = null;

// Inicialización al cargar la página
document.addEventListener('DOMContentLoaded', async () => {
    // Recuperar configuración de número de tablet en este dispositivo
    const savedTablet = localStorage.getItem('triviaTabletCode') || 'TABLET_1';
    const tabletSelect = document.getElementById('tabletCodeSelect');
    if (tabletSelect) {
        tabletSelect.value = savedTablet;
        updateTabletBadge();
    }

    buildModalFiveOptionInputs();
    await loadPublicCategories();

    // Si la tablet sufrió un microcorte o recarga de página con partida en curso, recupera el estado
    if (currentGameId) {
        try {
            showPage('quiz');
            await fetchNextOrCurrentQuestion();
        } catch (err) {
            sessionStorage.removeItem('activeGameId');
            currentGameId = null;
            showPage('home');
        }
    }
});

function updateTabletBadge() {
    const code = document.getElementById('tabletCodeSelect').value;
    localStorage.setItem('triviaTabletCode', code);
    document.getElementById('headerTabletBadge').textContent = code;
}

// Navegación principal entre pantallas
function showPage(pageId) {
    document.querySelectorAll('main.app-main > section').forEach(sec => sec.classList.add('hidden'));
    const target = document.getElementById(pageId);
    if (target) target.classList.remove('hidden');

    document.querySelectorAll('nav.main-nav .btn-vial').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.page === pageId);
    });

    if (pageId === 'setup') loadPublicCategories();
    if (pageId === 'ranking') loadActiveRankingTab();
    if (pageId === 'admin') checkAdminSession();

    window.scrollTo({top: 0, behavior: 'smooth'});
}

// ============================================================================
// 1. FLUJO DE JUEGO PARA LAS 3 TABLETS
// ============================================================================
async function loadPublicCategories() {
    try {
        const res = await fetch(`${API_BASE}/categories`);
        publicCategories = await res.json();
        const container = document.getElementById('categoriesContainer');
        if (!publicCategories.length) {
            container.innerHTML = '<p class="muted">No hay categorías activas disponibles.</p>';
            return;
        }
        if (!selectedCategoryId) {
            selectedCategoryId = publicCategories[0].id;
        }
        container.innerHTML = publicCategories.map(cat => `
            <div class="category-option ${cat.id === selectedCategoryId ? 'selected' : ''}"
                 onclick="selectCategoryCard(${cat.id})">
                <div class="fs-2">${cat.icon || '🚦'}</div>
                <h5 class="fw-bold mt-2 mb-1">${escapeHtml(cat.name)}</h5>
                <small class="muted d-block">${escapeHtml(cat.description || '')}</small>
                <span class="pill mt-2">${cat.activeQuestionCount} preguntas</span>
            </div>
        `).join('');
    } catch (e) {
        console.error('Error cargando categorías:', e);
    }
}

function selectCategoryCard(catId) {
    selectedCategoryId = catId;
    loadPublicCategories();
}

async function startNewGame() {
    // Desbloquear y activar el audio con el clic del botón de inicio de partida
    getAudioContext();

    const nameInput = document.getElementById('participantNameInput');
    const participantName = nameInput.value.trim();
    const tabletCode = document.getElementById('tabletCodeSelect').value;
    const alertBox = document.getElementById('setupAlert');
    alertBox.classList.add('hidden');

    if (!participantName) {
        alertBox.textContent = 'Por favor, ingresá tu nombre antes de comenzar.';
        alertBox.classList.remove('hidden');
        nameInput.focus();
        return;
    }
    if (!selectedCategoryId) {
        alertBox.textContent = 'Seleccioná una categoría para jugar.';
        alertBox.classList.remove('hidden');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/games`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                participantName,
                categoryId: selectedCategoryId,
                tabletCode
            })
        });
        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.message || 'No se pudo iniciar la partida.');
        }

        currentGameId = data.gameId;
        sessionStorage.setItem('activeGameId', currentGameId);
        sessionStorage.setItem('activeParticipantName', data.participantName);

        showPage('quiz');
        await fetchNextOrCurrentQuestion();
    } catch (err) {
        alertBox.textContent = err.message;
        alertBox.classList.remove('hidden');
    }
}

async function fetchNextOrCurrentQuestion() {
    answerSubmitted = false;
    clearInterval(countdownInterval);
    lastBeepSecond = -1;
    document.getElementById('feedbackBox').classList.add('hidden');
    document.getElementById('timerBadge').classList.remove('critical');

    const res = await fetch(`${API_BASE}/games/${currentGameId}/current-question`);
    const data = await res.json();

    if (!res.ok) {
        if (data.errorCode === 'GAME_ALREADY_FINISHED' || data.errorCode === 'NO_MORE_QUESTIONS') {
            await showGameSummaryScreen();
            return;
        }
        throw new Error(data.message || 'Error al obtener pregunta.');
    }

    currentQuestionId = data.gameQuestionId;
    const pName = sessionStorage.getItem('activeParticipantName') || 'JUGADOR';
    document.getElementById('quizPlayerInfo').textContent = `${pName.toUpperCase()} · PREGUNTA ${data.questionOrder}/${data.totalQuestions}`;
    document.getElementById('currentScore').textContent = data.currentTotalScore;
    document.getElementById('progressBar').style.width = `${((data.questionOrder - 1) / data.totalQuestions) * 100}%`;
    document.getElementById('questionText').textContent = data.statement;

    // Mostrar imagen si la pregunta tiene una asignada; de lo contrario, mostrar el ícono
    const iconSpan = document.getElementById('questionIcon');
    const imgEl = document.getElementById('questionImage');
    if (data.imageUrl) {
        iconSpan.classList.add('hidden');
        imgEl.src = data.imageUrl;
        imgEl.classList.remove('hidden');
    } else {
        imgEl.classList.add('hidden');
        iconSpan.textContent = data.icon || '🚦';
        iconSpan.classList.remove('hidden');
    }

    // Renderizar las 5 opciones mezcladas aleatoriamente por el servidor
    const answersContainer = document.getElementById('answersContainer');
    const letters = ['A', 'B', 'C', 'D', 'E'];
    answersContainer.innerHTML = '';

    data.options.forEach((opt, idx) => {
        const btn = document.createElement('button');
        btn.className = 'answer-btn';
        btn.dataset.optionId = opt.optionId;
        btn.textContent = `${letters[idx]}. ${opt.text}`;
        btn.onclick = () => submitPlayerAnswer(opt.optionId, btn);
        answersContainer.appendChild(btn);
    });

    startVisualCountdown(data.remainingTimeMs);
}

function startVisualCountdown(remainingMs) {
    const timeSpan = document.getElementById('timeLeft');
    const badge = document.getElementById('timerBadge');
    const startWallClock = performance.now();

    const updateTick = () => {
        const elapsed = performance.now() - startWallClock;
        const leftMs = Math.max(0, remainingMs - elapsed);

        // Segundos enteros sin milisegundos
        const leftSec = Math.ceil(leftMs / 1000);
        timeSpan.textContent = leftSec;

        // Reproducir sonido de tensión en cada segundo exacto
        if (leftSec !== lastBeepSecond && leftSec > 0) {
            lastBeepSecond = leftSec;
            playPressureTick(leftSec <= 2);
        }

        if (leftMs <= 10000) {
            badge.classList.add('critical');
        } else {
            badge.classList.remove('critical');
        }

        if (leftMs <= 0) {
            clearInterval(countdownInterval);
            if (!answerSubmitted) {
                submitPlayerAnswer(null, null); // Cierre automático al agotarse el tiempo
            }
        }
    };

    updateTick();
    countdownInterval = setInterval(updateTick, 80);
}

async function submitPlayerAnswer(selectedOptionId, clickedBtn) {
    if (answerSubmitted) return;
    answerSubmitted = true;
    clearInterval(countdownInterval);

    const allButtons = document.querySelectorAll('#answersContainer .answer-btn');
    allButtons.forEach(b => b.disabled = true);

    try {
        const res = await fetch(`${API_BASE}/games/${currentGameId}/answers`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                gameQuestionId: currentQuestionId,
                selectedOptionId: selectedOptionId
            })
        });
        const evalData = await res.json();
        if (!res.ok) throw new Error(evalData.message || 'Error validando respuesta');

        // Marcar visualmente la opción correcta y si hubo error
        allButtons.forEach(btn => {
            if (Number(btn.dataset.optionId) === evalData.correctOptionId) {
                btn.classList.add('correct');
            }
        });
        if (clickedBtn && !evalData.correct) {
            clickedBtn.classList.add('wrong');
        }

        document.getElementById('currentScore').textContent = evalData.updatedTotalScore;
        document.getElementById('progressBar').style.width = `${(evalData.questionOrder / 5) * 100}%`;

        const fb = document.getElementById('feedbackBox');
        if (evalData.timedOut) {
            fb.innerHTML = `⏰ <strong>¡Tiempo agotado! (+0 pts)</strong> · Respuesta correcta: <em>${escapeHtml(evalData.correctOptionText)}</em>.<br>${escapeHtml(evalData.explanation)}`;
        } else if (evalData.correct) {
            fb.innerHTML = `✅ <strong>¡Correcto en ${(evalData.serverElapsedMs / 1000).toFixed(2)}s! (+${evalData.awardedPoints} pts)</strong><br>${escapeHtml(evalData.explanation)}`;
        } else {
            fb.innerHTML = `❌ <strong>Respuesta incorrecta (+0 pts)</strong> · Era: <em>${escapeHtml(evalData.correctOptionText)}</em>.<br>${escapeHtml(evalData.explanation)}`;
        }
        fb.classList.remove('hidden');

        setTimeout(async () => {
            if (evalData.gameFinished) {
                await showGameSummaryScreen();
            } else {
                await fetchNextOrCurrentQuestion();
            }
        }, 2800);
    } catch (e) {
        console.error(e);
    }
}

async function showGameSummaryScreen() {
    if (!currentGameId) return;
    try {
        const res = await fetch(`${API_BASE}/games/${currentGameId}`);
        const summary = await res.json();

        sessionStorage.removeItem('activeGameId');
        currentGameId = null;

        document.getElementById('resultTitle').textContent = `¡Muy bien, ${summary.participantName}!`;
        document.getElementById('resultSubtitle').textContent = `Categoría: ${summary.categoryName} · Dispositivo: ${summary.tabletCode}`;
        document.getElementById('resTotalScore').textContent = `${summary.totalScore} pts`;
        document.getElementById('resCorrectCount').textContent = `${summary.correctCount}/${summary.totalQuestions}`;
        document.getElementById('resTotalTime').textContent = `${(summary.totalResponseTimeMs / 1000).toFixed(2)}s`;

        showPage('results');
    } catch (e) {
        showPage('home');
    }
}

// ============================================================================
// 2. RANKING POR COMPETENCIA Y RANKING GENERAL
// ============================================================================
let activeRankingTab = 'competition';

function switchRankingTab(tab) {
    activeRankingTab = tab;
    document.getElementById('tabBtnComp').classList.toggle('active', tab === 'competition');
    document.getElementById('tabBtnGen').classList.toggle('active', tab === 'general');
    document.getElementById('rankingCompetitionView').classList.toggle('hidden', tab !== 'competition');
    document.getElementById('rankingGeneralView').classList.toggle('hidden', tab !== 'general');
    loadActiveRankingTab();
}

async function loadActiveRankingTab() {
    if (activeRankingTab === 'competition') {
        const res = await fetch(`${API_BASE}/rankings/competition?page=0&size=50`);
        const page = await res.json();
        const rows = page.content || [];
        const tbody = document.getElementById('compRankingRows');
        document.getElementById('emptyCompRanking').classList.toggle('hidden', rows.length > 0);

        tbody.innerHTML = rows.map((r, i) => {
            const medal = i === 0 ? '🥇 1' : i === 1 ? '🥈 2' : i === 2 ? '🥉 3' : `#${i + 1}`;
            const dateStr = r.finishedAt ? new Date(r.finishedAt).toLocaleTimeString('es-PY', {
                hour: '2-digit',
                minute: '2-digit'
            }) : '-';
            return `
                <tr>
                    <td><strong>${medal}</strong></td>
                    <td><strong>${escapeHtml(r.participantName)}</strong></td>
                    <td>${escapeHtml(r.categoryName)}</td>
                    <td><span class="pill">${escapeHtml(r.tabletCode)}</span></td>
                    <td>${r.correctCount}/5</td>
                    <td>${(r.totalResponseTimeMs / 1000).toFixed(2)}s</td>
                    <td><strong style="color:var(--teal);">${r.score} pts</strong></td>
                    <td>${dateStr}</td>
                </tr>
            `;
        }).join('');
    } else {
        const res = await fetch(`${API_BASE}/rankings/general?page=0&size=50`);
        const page = await res.json();
        const rows = page.content || [];
        const tbody = document.getElementById('genRankingRows');
        document.getElementById('emptyGenRanking').classList.toggle('hidden', rows.length > 0);

        tbody.innerHTML = rows.map((r, i) => {
            const medal = i === 0 ? '🥇 1' : i === 1 ? '🥈 2' : i === 2 ? '🥉 3' : `#${i + 1}`;
            return `
                <tr>
                    <td><strong>${medal}</strong></td>
                    <td><strong>${escapeHtml(r.participantName)}</strong></td>
                    <td><strong style="color:var(--teal);">${r.accumulatedScore} pts</strong></td>
                    <td>${r.finishedGamesCount} partidas</td>
                    <td>${r.totalCorrectAnswers} aciertos</td>
                    <td>${(r.bestTimeMs / 1000).toFixed(2)}s</td>
                </tr>
            `;
        }).join('');
    }
}

// ============================================================================
// 3. PANEL ADMINISTRATIVO (AUTH, DASHBOARD, CRUD 5 OPCIONES, IMÁGENES)
// ============================================================================
async function checkAdminSession() {
    try {
        const res = await fetch(`${API_BASE}/auth/me`, {credentials: 'include'});
        if (res.ok) {
            const profile = await res.json();
            showAdminDashboard(profile);
        } else {
            document.getElementById('adminLoginBox').classList.remove('hidden');
            document.getElementById('adminDashboardBox').classList.add('hidden');
        }
    } catch (e) {
        document.getElementById('adminLoginBox').classList.remove('hidden');
        document.getElementById('adminDashboardBox').classList.add('hidden');
    }
}

async function adminLogin() {
    const username = document.getElementById('adminUsername').value.trim();
    const password = document.getElementById('adminPassword').value;
    const errBox = document.getElementById('adminLoginError');
    errBox.classList.add('hidden');

    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            credentials: 'include',
            body: JSON.stringify({username, password})
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Credenciales inválidas');
        showAdminDashboard(data);
    } catch (e) {
        errBox.textContent = e.message;
        errBox.classList.remove('hidden');
    }
}

async function adminLogout() {
    await fetch(`${API_BASE}/auth/logout`, {method: 'POST', credentials: 'include'});
    document.getElementById('adminDashboardBox').classList.add('hidden');
    document.getElementById('adminLoginBox').classList.remove('hidden');
}

async function showAdminDashboard(profile) {
    document.getElementById('adminLoginBox').classList.add('hidden');
    document.getElementById('adminDashboardBox').classList.remove('hidden');
    document.getElementById('adminWelcomePill').textContent = `ADMIN: ${profile.fullName.toUpperCase()}`;

    await loadAdminStats();
    await loadAdminCategories();
    await loadAdminQuestions(0);
}

async function loadAdminStats() {
    const res = await fetch(`${API_BASE}/admin/stats`, {credentials: 'include'});
    if (!res.ok) return;
    const st = await res.json();
    document.getElementById('statSessionCode').textContent = st.activeSessionCode;
    document.getElementById('statTotalQuestions').textContent = st.totalQuestions;
    document.getElementById('statTotalGames').textContent = st.totalGames;
    document.getElementById('statLockedQuestions').textContent = `${st.lockedQuestionsCount} retenidas`;
}

async function startNewCompetitionRound() {
    const customName = prompt('Nombre de la nueva competencia (esto también libera las preguntas retenidas en las 3 tablets):', 'Competencia Seguridad Vial');
    if (customName === null) return;

    const res = await fetch(`${API_BASE}/admin/sessions/new`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        credentials: 'include',
        body: JSON.stringify({name: customName})
    });
    if (res.ok) {
        await loadAdminStats();
        alert('Nueva sesión de competencia iniciada y bloqueos de tablets liberados.');
    }
}

function switchAdminTab(tabName) {
    document.querySelectorAll('#adminDashboardBox [data-admintab]').forEach(b => {
        b.classList.toggle('active', b.dataset.admintab === tabName);
    });
    ['questions', 'categories', 'games', 'participants', 'errors'].forEach(t => {
        document.getElementById(`adminTab_${t}`).classList.toggle('hidden', t !== tabName);
    });

    if (tabName === 'questions') loadAdminQuestions(0);
    if (tabName === 'categories') loadAdminCategories();
    if (tabName === 'games') loadAdminGamesHistory();
    if (tabName === 'participants') loadAdminParticipants();
    if (tabName === 'errors') loadAdminErrors();
}

// --- CRUD de Categorías ---
let adminCategoriesCache = [];

async function loadAdminCategories() {
    const res = await fetch(`${API_BASE}/admin/categories`, {credentials: 'include'});
    if (!res.ok) return;
    adminCategoriesCache = await res.json();

    const filterSel = document.getElementById('filterQuestionCategory');
    filterSel.innerHTML = '<option value="">Todas las categorías</option>' +
        adminCategoriesCache.map(c => `<option value="${c.id}">${c.icon} ${escapeHtml(c.name)}</option>`).join('');

    const modalSel = document.getElementById('qModalCategory');
    modalSel.innerHTML = adminCategoriesCache.map(c => `<option value="${c.id}">${c.icon} ${escapeHtml(c.name)}</option>`).join('');

    const tbody = document.getElementById('adminCategoriesRows');
    tbody.innerHTML = adminCategoriesCache.map(c => `
        <tr>
            <td>${c.id}</td>
            <td><strong>${c.icon} ${escapeHtml(c.name)}</strong><br><small class="muted">${escapeHtml(c.slug)}</small></td>
            <td>${c.activeQuestionCount}</td>
            <td>${c.active ? '<span class="pill">Activa</span>' : '<span class="badge bg-secondary">Inactiva</span>'}</td>
            <td><button class="btn-secondary-vial py-1 px-2" onclick="editCategory(${c.id})">Editar</button></td>
        </tr>
    `).join('');
}

function editCategory(id) {
    const c = adminCategoriesCache.find(x => x.id === id);
    if (!c) return;
    document.getElementById('editCategoryId').value = c.id;
    document.getElementById('catSlug').value = c.slug;
    document.getElementById('catSlug').disabled = true;
    document.getElementById('catName').value = c.name;
    document.getElementById('catIcon').value = c.icon;
    document.getElementById('catDesc').value = c.description || '';
    document.getElementById('catActive').checked = c.active;
}

function resetCategoryForm() {
    document.getElementById('editCategoryId').value = '';
    document.getElementById('catSlug').value = '';
    document.getElementById('catSlug').disabled = false;
    document.getElementById('catName').value = '';
    document.getElementById('catIcon').value = '🚦';
    document.getElementById('catDesc').value = '';
    document.getElementById('catActive').checked = true;
}

async function saveCategory() {
    const id = document.getElementById('editCategoryId').value;
    const payload = {
        slug: document.getElementById('catSlug').value.trim(),
        name: document.getElementById('catName').value.trim(),
        icon: document.getElementById('catIcon').value.trim() || '🚦',
        description: document.getElementById('catDesc').value.trim(),
        active: document.getElementById('catActive').checked
    };

    const url = id ? `${API_BASE}/admin/categories/${id}` : `${API_BASE}/admin/categories`;
    const method = id ? 'PUT' : 'POST';

    const res = await fetch(url, {
        method,
        headers: {'Content-Type': 'application/json'},
        credentials: 'include',
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) {
        alert(data.message || 'Error guardando categoría');
        return;
    }
    resetCategoryForm();
    await loadAdminCategories();
    await loadPublicCategories();
}

// --- CRUD de Preguntas (5 Opciones + Imagen) ---
function buildModalFiveOptionInputs() {
    const container = document.getElementById('qModalOptionsContainer');
    const letters = ['A', 'B', 'C', 'D', 'E'];
    container.innerHTML = letters.map((L, idx) => `
        <div class="input-group mb-2">
            <div class="input-group-text">
                <input class="form-check-input mt-0" type="radio" name="qModalCorrectRadio" value="${idx}" ${idx === 0 ? 'checked' : ''}>
                <strong class="ms-2">${L}</strong>
            </div>
            <input type="text" class="form-control q-modal-opt-text" placeholder="Texto de la opción ${L}">
        </div>
    `).join('');
}

async function loadAdminQuestions(page) {
    adminQuestionsPage = Math.max(0, page);
    const catId = document.getElementById('filterQuestionCategory').value;
    const active = document.getElementById('filterQuestionActive').value;

    const params = new URLSearchParams({page: adminQuestionsPage, size: 15});
    if (catId) params.append('categoryId', catId);
    if (active !== '') params.append('active', active);

    const res = await fetch(`${API_BASE}/admin/questions?${params}`, {credentials: 'include'});
    if (!res.ok) return;
    const data = await res.json();
    adminQuestionsList = data.content || [];

    document.getElementById('questionsPageInfo').textContent = `Página ${data.number + 1} de ${Math.max(1, data.totalPages)} (${data.totalElements} preguntas)`;

    const tbody = document.getElementById('adminQuestionsRows');
    tbody.innerHTML = adminQuestionsList.map(q => `
        <tr>
            <td>#${q.id}</td>
            <td>${escapeHtml(q.categoryName)}</td>
            <td>${q.imageUrl ? `<img src="\${q.imageUrl}" style="height:34px;border-radius:6px;">` : `<span class="fs-4">\${q.icon}</span>`}</td>
            <td><strong>${escapeHtml(q.statement)}</strong></td>
            <td>${q.active ? '<span class="pill">Activa</span>' : '<span class="badge bg-secondary">Inactiva</span>'}</td>
            <td>
                <button class="btn-secondary-vial py-1 px-2" onclick="openQuestionModal(${q.id})">Editar / Imagen</button>
            </td>
        </tr>
    `).join('');
}

function changeQuestionsPage(delta) {
    loadAdminQuestions(adminQuestionsPage + delta);
}

function openQuestionModal(questionId = null) {
    if (!questionModalInstance) {
        questionModalInstance = new bootstrap.Modal(document.getElementById('questionModal'));
    }
    document.getElementById('qModalError').classList.add('hidden');
    document.getElementById('qModalImageInput').value = '';

    const textInputs = document.querySelectorAll('.q-modal-opt-text');
    const radios = document.querySelectorAll('input[name="qModalCorrectRadio"]');

    if (!questionId) {
        document.getElementById('questionModalTitle').textContent = 'Nueva Pregunta (5 Opciones)';
        document.getElementById('qModalId').value = '';
        document.getElementById('qModalStatement').value = '';
        document.getElementById('qModalExplanation').value = '';
        document.getElementById('qModalIcon').value = '🚦';
        document.getElementById('qModalActive').checked = true;
        document.getElementById('qModalCurrentImageBox').classList.add('hidden');
        textInputs.forEach(inp => inp.value = '');
        radios[0].checked = true;
    } else {
        const q = adminQuestionsList.find(x => x.id === questionId);
        if (!q) return;
        document.getElementById('questionModalTitle').textContent = `Editar Pregunta #${q.id}`;
        document.getElementById('qModalId').value = q.id;
        document.getElementById('qModalCategory').value = q.categoryId;
        document.getElementById('qModalStatement').value = q.statement;
        document.getElementById('qModalExplanation').value = q.explanation;
        document.getElementById('qModalIcon').value = q.icon || '🚦';
        document.getElementById('qModalActive').checked = q.active;

        q.options.forEach((opt, idx) => {
            if (textInputs[idx]) textInputs[idx].value = opt.text;
            if (opt.correct && radios[idx]) radios[idx].checked = true;
        });

        const imgBox = document.getElementById('qModalCurrentImageBox');
        if (q.imageUrl) {
            document.getElementById('qModalPreviewImg').src = q.imageUrl;
            imgBox.classList.remove('hidden');
        } else {
            imgBox.classList.add('hidden');
        }
    }

    questionModalInstance.show();
}

async function saveQuestionFromModal() {
    const id = document.getElementById('qModalId').value;
    const errBox = document.getElementById('qModalError');
    errBox.classList.add('hidden');

    const textInputs = Array.from(document.querySelectorAll('.q-modal-opt-text'));
    const selectedRadio = document.querySelector('input[name="qModalCorrectRadio"]:checked');
    const correctIdx = selectedRadio ? Number(selectedRadio.value) : 0;

    const options = textInputs.map((inp, idx) => ({
        text: inp.value.trim(),
        correct: idx === correctIdx
    }));

    const payload = {
        categoryId: Number(document.getElementById('qModalCategory').value),
        statement: document.getElementById('qModalStatement').value.trim(),
        explanation: document.getElementById('qModalExplanation').value.trim(),
        icon: document.getElementById('qModalIcon').value.trim() || '🚦',
        active: document.getElementById('qModalActive').checked,
        options
    };

    try {
        const url = id ? `${API_BASE}/admin/questions/${id}` : `${API_BASE}/admin/questions`;
        const method = id ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method,
            headers: {'Content-Type': 'application/json'},
            credentials: 'include',
            body: JSON.stringify(payload)
        });
        const savedQuestion = await res.json();
        if (!res.ok) throw new Error(savedQuestion.message || 'Error validando la pregunta');

        const fileInput = document.getElementById('qModalImageInput');
        if (fileInput.files && fileInput.files.length > 0) {
            const formData = new FormData();
            formData.append('file', fileInput.files[0]);
            const imgRes = await fetch(`${API_BASE}/admin/questions/${savedQuestion.id}/image`, {
                method: 'POST',
                credentials: 'include',
                body: formData
            });
            if (!imgRes.ok) {
                const imgErr = await imgRes.json();
                throw new Error(imgErr.message || 'La pregunta se guardó, pero la imagen fue rechazada.');
            }
        }

        questionModalInstance.hide();
        await loadAdminQuestions(adminQuestionsPage);
        await loadAdminStats();
    } catch (e) {
        errBox.textContent = e.message;
        errBox.classList.remove('hidden');
    }
}

async function deleteCurrentQuestionImage() {
    const id = document.getElementById('qModalId').value;
    if (!id) return;
    const res = await fetch(`${API_BASE}/admin/questions/${id}/image`, {
        method: 'DELETE',
        credentials: 'include'
    });
    if (res.ok) {
        document.getElementById('qModalCurrentImageBox').classList.add('hidden');
        await loadAdminQuestions(adminQuestionsPage);
    }
}

async function loadAdminGamesHistory() {
    const res = await fetch(`${API_BASE}/admin/games?page=0&size=40`, {credentials: 'include'});
    if (!res.ok) return;
    const data = await res.json();
    document.getElementById('adminGamesRows').innerHTML = (data.content || []).map(g => `
        <tr>
            <td><small>${escapeHtml(g.sessionCode)}</small></td>
            <td><strong>${escapeHtml(g.participantName)}</strong></td>
            <td>${escapeHtml(g.categoryName)}</td>
            <td><span class="pill">${escapeHtml(g.tabletCode)}</span></td>
            <td>${escapeHtml(g.status)}</td>
            <td><strong>${g.totalScore} pts</strong></td>
            <td>${g.correctCount}/5</td>
            <td>${(g.totalResponseTimeMs / 1000).toFixed(2)}s</td>
            <td><small>${g.startedAt ? new Date(g.startedAt).toLocaleString('es-PY') : '-'}</small></td>
        </tr>
    `).join('');
}

async function loadAdminParticipants() {
    const res = await fetch(`${API_BASE}/admin/participants?page=0&size=40`, {credentials: 'include'});
    if (!res.ok) return;
    const data = await res.json();
    document.getElementById('adminParticipantsRows').innerHTML = (data.content || []).map(p => `
        <tr>
            <td>#${p.id}</td>
            <td><strong>${escapeHtml(p.displayName)}</strong></td>
            <td><code>${escapeHtml(p.normalizedName)}</code></td>
            <td>${p.lastPlayedAt ? new Date(p.lastPlayedAt).toLocaleString('es-PY') : '-'}</td>
        </tr>
    `).join('');
}

async function loadAdminErrors() {
    const res = await fetch(`${API_BASE}/admin/errors?page=0&size=40`, {credentials: 'include'});
    if (!res.ok) return;
    const data = await res.json();
    document.getElementById('adminErrorsRows').innerHTML = (data.content || []).map(err => `
        <tr>
            <td><small>${err.createdAt ? new Date(err.createdAt).toLocaleString('es-PY') : '-'}</small></td>
            <td><code>${escapeHtml(err.errorCode)}</code></td>
            <td>${err.httpStatus}</td>
            <td><small>${escapeHtml(err.endpoint)}</small></td>
            <td>${escapeHtml(err.message)}</td>
        </tr>
    `).join('');
}

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}