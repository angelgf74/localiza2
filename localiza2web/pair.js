'use strict';

const API_BASE = 'https://localiza2-api.angelgf.com.es';

const params      = new URLSearchParams(window.location.search);
const pairToken   = params.get('token');
const resetToken  = params.get('reset');
const loadingEl   = document.getElementById('loading');
const contentEl   = document.getElementById('content');

function show(html) {
  loadingEl.classList.add('hidden');
  contentEl.innerHTML = html;
  contentEl.classList.remove('hidden');
}

function showMsg(text, type = 'info') {
  show(`<div class="status-msg ${type}">${text}</div>`);
}

async function apiGet(path) {
  const res = await fetch(`${API_BASE}${path}`);
  if (!res.ok) { const j = await res.json().catch(() => ({})); throw new Error(j.message || `Error ${res.status}`); }
  return res.json();
}

async function apiPost(path, body, authToken) {
  const headers = { 'Content-Type': 'application/json' };
  if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
  const res = await fetch(`${API_BASE}${path}`, { method: 'POST', headers, body: JSON.stringify(body) });
  if (!res.ok) { const j = await res.json().catch(() => ({})); throw new Error(j.message || `Error ${res.status}`); }
  return res.json();
}

function getSession() {
  return {
    token:        sessionStorage.getItem('lz2_token'),
    refreshToken: sessionStorage.getItem('lz2_refresh'),
    name:         sessionStorage.getItem('lz2_name')
  };
}

function saveSession(token, refreshToken, name) {
  sessionStorage.setItem('lz2_token', token);
  sessionStorage.setItem('lz2_refresh', refreshToken);
  sessionStorage.setItem('lz2_name', name);
}

function escHtml(s) {
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

// ─── Links de navegación ──────────────────────────────────────────────────────
function authLinks(activeMode) {
  const links = [
    { mode: 'login',    label: 'Iniciar sesión' },
    { mode: 'register', label: 'Registrarse' },
    { mode: 'forgot',   label: '¿Olvidaste la contraseña?' },
  ];
  return `<div class="auth-links">
    ${links.map(l =>
      l.mode === activeMode
        ? `<span class="auth-link active">${l.label}</span>`
        : `<a href="#" class="auth-link" data-mode="${l.mode}">${l.label}</a>`
    ).join(' · ')}
  </div>`;
}

function bindAuthLinks(inviterName) {
  document.querySelectorAll('.auth-link[data-mode]').forEach(a => {
    a.addEventListener('click', e => {
      e.preventDefault();
      const mode = a.dataset.mode;
      if (mode === 'login')    renderLogin(inviterName);
      if (mode === 'register') renderRegister(inviterName);
      if (mode === 'forgot')   renderForgot();
    });
  });
}

// ─── Login ────────────────────────────────────────────────────────────────────
function renderLogin(inviterName) {
  show(`
    ${inviterName ? `<p class="pair-inviter">${escHtml(inviterName)}</p>
    <p class="pair-subtitle">quiere conectar contigo en <strong>localiza2</strong></p>` : ''}
    <p style="color:var(--text-muted);font-size:13px;margin-bottom:18px">Inicia sesión para continuar.</p>
    <form id="auth-form">
      <div class="field"><label>Email</label><input type="email" id="f-email" required autocomplete="email" /></div>
      <div class="field"><label>Contraseña</label><input type="password" id="f-password" required autocomplete="current-password" /></div>
      <button type="submit" class="btn-accept" id="auth-btn">Entrar${inviterName ? ' y aceptar' : ''}</button>
    </form>
    <div id="auth-err" class="status-msg err hidden"></div>
    ${authLinks('login')}
  `);
  bindAuthLinks(inviterName);

  document.getElementById('auth-form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn = document.getElementById('auth-btn');
    const errEl = document.getElementById('auth-err');
    errEl.classList.add('hidden');
    btn.disabled = true;
    const origText = btn.textContent;
    btn.textContent = '⟳ Entrando…';
    try {
      const email = document.getElementById('f-email').value.trim();
      const pass  = document.getElementById('f-password').value;
      const data = await apiPost('/api/auth/login', { email, password: pass });
      saveSession(data.token, data.refreshToken, data.name || data.email);
      if (inviterName) {
        await tryAccept(data.token, inviterName);
      } else {
        show(`<div class="status-msg ok">✓ Sesión iniciada. <a href="index.html" style="color:#86efac">Abrir localiza2</a></div>`);
      }
    } catch (err) {
      errEl.textContent = err.message;
      errEl.classList.remove('hidden');
      btn.disabled = false;
      btn.textContent = origText;
    }
  });
}

// ─── Registro ─────────────────────────────────────────────────────────────────
function renderRegister(inviterName) {
  show(`
    ${inviterName ? `<p class="pair-inviter">${escHtml(inviterName)}</p>
    <p class="pair-subtitle">te invita a unirte a <strong>localiza2</strong></p>` : ''}
    <p style="color:var(--text-muted);font-size:13px;margin-bottom:18px">Crea tu cuenta para continuar.</p>
    <form id="auth-form">
      <div class="field"><label>Nombre</label><input type="text" id="f-name" required autocomplete="name" /></div>
      <div class="field"><label>Email</label><input type="email" id="f-email" required autocomplete="email" /></div>
      <div class="field"><label>Contraseña (mín. 8 caracteres)</label><input type="password" id="f-password" required autocomplete="new-password" minlength="8" /></div>
      <button type="submit" class="btn-accept" id="auth-btn">Crear cuenta</button>
    </form>
    <div id="auth-err" class="status-msg err hidden"></div>
    ${authLinks('register')}
  `);
  bindAuthLinks(inviterName);

  document.getElementById('auth-form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn   = document.getElementById('auth-btn');
    const errEl = document.getElementById('auth-err');
    errEl.classList.add('hidden');
    btn.disabled = true;
    btn.textContent = '⟳ Registrando…';
    try {
      const name  = document.getElementById('f-name').value.trim();
      const email = document.getElementById('f-email').value.trim();
      const pass  = document.getElementById('f-password').value;
      const data = await apiPost('/api/auth/register', { name, email, password: pass });
      show(`
        <div class="status-msg ok">✓ ${escHtml(data.message)}</div>
        <p style="color:var(--text-muted);font-size:13px;margin-top:16px;text-align:center">
          Después de confirmar tu correo, abre de nuevo el enlace de invitación para aceptar.
        </p>
      `);
    } catch (err) {
      errEl.textContent = err.message;
      errEl.classList.remove('hidden');
      btn.disabled = false;
      btn.textContent = 'Crear cuenta';
    }
  });
}

// ─── Recuperar contraseña ─────────────────────────────────────────────────────
function renderForgot() {
  show(`
    <p style="font-size:15px;font-weight:700;margin-bottom:6px">Recuperar contraseña</p>
    <p style="color:var(--text-muted);font-size:13px;margin-bottom:18px">Introduce tu email y te enviaremos un enlace para restablecer tu contraseña.</p>
    <form id="auth-form">
      <div class="field"><label>Email</label><input type="email" id="f-email" required autocomplete="email" /></div>
      <button type="submit" class="btn-accept" id="auth-btn">Enviar enlace</button>
    </form>
    <div id="auth-err" class="status-msg err hidden"></div>
    ${authLinks('forgot')}
  `);
  bindAuthLinks(null);

  document.getElementById('auth-form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn   = document.getElementById('auth-btn');
    const errEl = document.getElementById('auth-err');
    errEl.classList.add('hidden');
    btn.disabled = true;
    btn.textContent = '⟳ Enviando…';
    try {
      const email = document.getElementById('f-email').value.trim();
      const data = await apiPost('/api/auth/forgot-password', { email });
      show(`<div class="status-msg ok">✓ ${escHtml(data.message)}</div>`);
    } catch (err) {
      errEl.textContent = err.message;
      errEl.classList.remove('hidden');
      btn.disabled = false;
      btn.textContent = 'Enviar enlace';
    }
  });
}

// ─── Nueva contraseña (desde enlace de email) ─────────────────────────────────
function renderResetPassword() {
  show(`
    <p style="font-size:15px;font-weight:700;margin-bottom:6px">Nueva contraseña</p>
    <p style="color:var(--text-muted);font-size:13px;margin-bottom:18px">Elige una contraseña nueva para tu cuenta.</p>
    <form id="auth-form">
      <div class="field"><label>Nueva contraseña</label><input type="password" id="f-password" required autocomplete="new-password" minlength="8" /></div>
      <div class="field"><label>Repite la contraseña</label><input type="password" id="f-password2" required autocomplete="new-password" minlength="8" /></div>
      <button type="submit" class="btn-accept" id="auth-btn">Guardar contraseña</button>
    </form>
    <div id="auth-err" class="status-msg err hidden"></div>
  `);

  document.getElementById('auth-form').addEventListener('submit', async e => {
    e.preventDefault();
    const btn    = document.getElementById('auth-btn');
    const errEl  = document.getElementById('auth-err');
    const pass   = document.getElementById('f-password').value;
    const pass2  = document.getElementById('f-password2').value;
    errEl.classList.add('hidden');
    if (pass !== pass2) {
      errEl.textContent = 'Las contraseñas no coinciden.';
      errEl.classList.remove('hidden');
      return;
    }
    btn.disabled = true;
    btn.textContent = '⟳ Guardando…';
    try {
      const data = await apiPost('/api/auth/reset-password', { token: resetToken, newPassword: pass });
      show(`
        <div class="status-msg ok">✓ ${escHtml(data.message)}</div>
        <p style="text-align:center;margin-top:16px">
          <a href="index.html" style="color:var(--accent);font-size:13px">Ir a localiza2</a>
        </p>
      `);
    } catch (err) {
      errEl.textContent = err.message;
      errEl.classList.remove('hidden');
      btn.disabled = false;
      btn.textContent = 'Guardar contraseña';
    }
  });
}

// ─── Vista de aceptar / rechazar ──────────────────────────────────────────────
function renderAccept(inviterName) {
  show(`
    <p class="pair-inviter">${escHtml(inviterName)}</p>
    <p class="pair-subtitle">quiere conectar contigo en <strong>localiza2</strong></p>
    <div class="pair-actions">
      <button class="btn-accept" id="accept-btn">Aceptar</button>
      <button class="btn-decline" id="decline-btn">Rechazar</button>
    </div>
    <div id="result" class="hidden"></div>
  `);

  document.getElementById('accept-btn').addEventListener('click', async () => {
    const btn = document.getElementById('accept-btn');
    btn.disabled = true;
    btn.textContent = '⟳ Aceptando…';
    await tryAccept(getSession().token, inviterName);
  });

  document.getElementById('decline-btn').addEventListener('click', () => {
    show(`<div class="status-msg info">Invitación rechazada.</div>`);
  });
}

async function tryAccept(authToken, inviterName) {
  try {
    const data = await apiPost('/api/contacts/pair/accept', { token: pairToken }, authToken);
    show(`
      <div class="status-msg ok">✓ ${escHtml(data.message)}</div>
      <hr class="divider" />
      <p style="text-align:center">
        <a href="index.html" style="color:var(--accent);font-size:13px">Abrir localiza2</a>
      </p>
    `);
  } catch (err) {
    show(`
      <p class="pair-inviter">${escHtml(inviterName)}</p>
      <p class="pair-subtitle">quiere conectar contigo en <strong>localiza2</strong></p>
      <div class="status-msg err">${escHtml(err.message)}</div>
    `);
  }
}

// ─── Arranque ─────────────────────────────────────────────────────────────────
(async () => {
  // Flujo de reset de contraseña (viene del email de recuperación)
  if (resetToken) {
    renderResetPassword();
    return;
  }

  if (!pairToken) { showMsg('No se proporcionó un código de emparejamiento.', 'err'); return; }

  let info;
  try {
    info = await apiGet(`/api/contacts/pair/info/${pairToken}`);
  } catch (err) {
    showMsg(err.message, 'err');
    return;
  }

  const { inviterName } = info;
  const session = getSession();

  if (session.token) {
    renderAccept(inviterName);
  } else {
    renderLogin(inviterName);
  }
})();
