const API = CONFIG.API_BASE_URL;
let faranyEnvole = "";
let scrapingInterval = null;

function naviguer(page) {
  document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
  document.getElementById('page-' + page).classList.add('active');
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('nav-' + page).classList.add('active');

  if (page === 'jeux') {
    // Appel natif Android — affiche la WebView bet261
    if (window.NativeApp) {
      window.NativeApp.showGame();
    }
  } else {
    if (window.NativeApp) {
      window.NativeApp.showDashboard();
    }
  }
}

async function verifierServeur() {
  const dot = document.getElementById('serverDot');
  const txt = document.getElementById('serverStatusText');
  try {
    const r = await fetch(API + '/api/etat', { signal: AbortSignal.timeout(5000) });
    await r.json();
    dot.className = 'server-dot online';
    txt.textContent = 'En ligne ✓';
    txt.className = 'server-status-text online';
  } catch {
    dot.className = 'server-dot offline';
    txt.textContent = 'Hors ligne ✗';
    txt.className = 'server-status-text offline';
  }
}

function demarrerAntiSleep() {
  setInterval(async () => { try { await fetch(API + '/api/etat'); } catch {} }, CONFIG.PING_INTERVAL_MS);
}

async function chargerEtatInitial() {
  try {
    const r = await fetch(API + '/api/etat');
    const d = await r.json();
    if (d.success && d.multiplicateurs.length > 0) {
      mettreAJourHistorique(d.multiplicateurs);
      mettreAJourEtat(d.etat);
      mettreAJourReco(d.recommandation);
    }
  } catch {}
}

document.getElementById('imageUpload').addEventListener('change', async (e) => {
  const file = e.target.files[0];
  if (!file) return;
  afficherStatus('⏳ Analyse OCR en cours...', 'loading');
  const formData = new FormData();
  formData.append('image', file);
  try {
    const r = await fetch(API + '/api/analyser_capture', { method: 'POST', body: formData });
    const d = await r.json();
    if (d.success) {
      afficherStatus('✅ Initialisation réussie !', 'success');
      mettreAJourHistorique(d.multiplicateurs);
      mettreAJourEtat(d.etat);
      mettreAJourReco(d.recommandation);
    } else {
      afficherStatus('❌ ' + d.error, 'error');
    }
  } catch { afficherStatus('🔌 Serveur inaccessible.', 'error'); }
  e.target.value = '';
});

function mettreAJourHistorique(mults) {
  const grid = document.getElementById('historyGrid');
  grid.innerHTML = '';
  if (!mults.length) {
    grid.innerHTML = '<div class="empty-state">En attente de données...</div>';
    return;
  }
  mults.slice(-40).forEach(mult => {
    const pill = document.createElement('div');
    pill.classList.add('pill');
    pill.innerText = mult.toFixed(2) + 'x';
    if (mult < 2) pill.classList.add('bleu');
    else if (mult < 5) pill.classList.add('violet');
    else if (mult < 50) pill.classList.add('rose');
    else pill.classList.add('vert');
    grid.appendChild(pill);
  });
}

function mettreAJourEtat(etat) {
  const badge = document.getElementById('etatBadge');
  if (!badge || !etat) return;
  badge.textContent = etat;
  badge.className = 'etat-badge';
  const map = { 'Froide': 'froide', 'Équilibrée': 'equilibree', 'Chaude': 'chaude', 'Surchauffée': 'surchauffee' };
  if (map[etat]) badge.classList.add(map[etat]);
}

function mettreAJourReco(texte) {
  const el = document.getElementById('recoText');
  if (!el) return;
  el.textContent = texte;
  el.className = 'reco-text';
  if (texte.includes('🔥 ENTRÉE')) el.classList.add('entree');
  else if (texte.includes('✅ VICTOIRE')) el.classList.add('victoire');
  else if (texte.includes('❌ ÉCHEC')) el.classList.add('echec');
  else if (texte.includes('⚠️')) el.classList.add('alerte');
}

function vibrer(texte) {
  if (!navigator.vibrate) return;
  if (texte.includes('🔥 ENTRÉE')) navigator.vibrate([600, 200, 600]);
  else if (texte.includes('✅ VICTOIRE')) navigator.vibrate([150, 100, 150, 100, 150]);
  else if (texte.includes('❌ ÉCHEC')) navigator.vibrate([1000]);
}

function afficherStatus(msg, type) {
  const el = document.getElementById('statusMsg');
  el.textContent = msg;
  el.className = 'status-msg ' + type;
  el.classList.remove('hidden');
}

async function resetDonnees() {
  try { await fetch(API + '/api/reset', { method: 'POST' }); } catch {}
  mettreAJourHistorique([]);
  mettreAJourEtat('Acquisition');
  document.getElementById('recoText').textContent = '⚪ En attente...';
  document.getElementById('recoText').className = 'reco-text';
}

(async () => {
  await verifierServeur();
  await chargerEtatInitial();
  demarrerAntiSleep();
  setInterval(verifierServeur, 30000);
})();
