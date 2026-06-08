'use strict';

// ─── Configuración ───────────────────────────────────────────────────────────
const API_BASE       = 'https://localiza2-api.angelgf.com.es';
const REFRESH_MS     = 30_000;
const DEFAULT_CENTER = [40.4168, -3.7038];
const DEFAULT_ZOOM   = 6;
const USER_ZOOM      = 15;

const CONTACT_COLORS = [
  '#ef4444', '#f97316', '#eab308', '#22c55e',
  '#14b8a6', '#3b82f6', '#8b5cf6', '#ec4899',
];
const USER_COLOR = '#3b82f6';

// ─── Estado ──────────────────────────────────────────────────────────────────
const state = {
  token:      sessionStorage.getItem('lz2_token'),
  userName:   sessionStorage.getItem('lz2_name'),
  contacts:   [],     // ContactDto[]
  locations:  {},     // contactId → ContactLocationDto
  userPos:    null,   // { lat, lng, accuracy, timestamp }
  selectedId: null,   // número (contactId) | 'me' | null
};

// ─── Leaflet ─────────────────────────────────────────────────────────────────
let map          = null;
let userMarker   = null;
let userCircle   = null;
let historyLayer = null;
const contactMarkers = {};   // contactId → L.Marker

// ─── Timers ──────────────────────────────────────────────────────────────────
let refreshTimer = null;
let toastTimer   = null;

// ════════════════════════════════════════════════════════════════════════════
// API
// ════════════════════════════════════════════════════════════════════════════

async function apiFetch(path, options = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (state.token) headers['Authorization'] = `Bearer ${state.token}`;
  Object.assign(headers, options.headers || {});

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (res.status === 401) { doLogout(); throw new Error('Sesión expirada'); }
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(text || `Error ${res.status}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

const apiLogin          = (email, password) =>
  apiFetch('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
const apiRegister       = (name, email, password) =>
  apiFetch('/api/auth/register', { method: 'POST', body: JSON.stringify({ name, email, password }) });
const apiForgotPassword = (email) =>
  apiFetch('/api/auth/forgot-password', { method: 'POST', body: JSON.stringify({ email }) });

const apiGetContacts         = () => apiFetch('/api/contacts');
const apiGetContactLocations = () => apiFetch('/api/location/contacts');

const apiGetMyLocationHistory      = (limit = 50) => apiFetch(`/api/location/me/history?limit=${limit}`);
const apiGetContactLocationHistory = (id, limit = 50) => apiFetch(`/api/location/contacts/${id}/history?limit=${limit}`);

// ════════════════════════════════════════════════════════════════════════════
// Auth
// ════════════════════════════════════════════════════════════════════════════

// ── Navegación entre paneles de login ────────────────────────────────────────
function showLoginPanel(panel) {
  ['panel-login', 'panel-register', 'panel-forgot'].forEach(id => {
    document.getElementById(id)?.classList.toggle('hidden', id !== panel);
  });
  // Focus al primer input del panel activo
  document.querySelector(`#${panel} input`)?.focus();
}

async function doLogin(email, password) {
  const btn   = document.getElementById('login-btn');
  const errEl = document.getElementById('login-error');
  errEl.classList.remove('ok');
  errEl.classList.add('hidden');
  btn.disabled = true;
  btn.querySelector('.btn-text').classList.add('hidden');
  btn.querySelector('.btn-spinner').classList.remove('hidden');

  try {
    const data = await apiLogin(email, password);
    state.token    = data.token;
    state.userName = data.name || data.email;
    sessionStorage.setItem('lz2_token', data.token);
    sessionStorage.setItem('lz2_name',  state.userName);
    showApp();
  } catch (err) {
    errEl.textContent = (err.message.includes('401') || err.message.includes('400'))
      ? 'Email o contraseña incorrectos.'
      : `Error al conectar: ${err.message}`;
    errEl.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.querySelector('.btn-text').classList.remove('hidden');
    btn.querySelector('.btn-spinner').classList.add('hidden');
  }
}

async function doRegister(name, email, password) {
  const btn   = document.getElementById('register-btn');
  const msgEl = document.getElementById('register-msg');
  msgEl.classList.add('hidden');
  btn.disabled = true;
  btn.querySelector('.btn-text').classList.add('hidden');
  btn.querySelector('.btn-spinner').classList.remove('hidden');

  try {
    const data = await apiRegister(name, email, password);
    msgEl.textContent = data.message;
    msgEl.className = 'error-msg ok';
  } catch (err) {
    msgEl.textContent = err.message;
    msgEl.className = 'error-msg';
    msgEl.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.querySelector('.btn-text').classList.remove('hidden');
    btn.querySelector('.btn-spinner').classList.add('hidden');
  }
}

async function doForgotPassword(email) {
  const btn   = document.getElementById('forgot-btn');
  const msgEl = document.getElementById('forgot-msg');
  msgEl.classList.add('hidden');
  btn.disabled = true;
  btn.querySelector('.btn-text').classList.add('hidden');
  btn.querySelector('.btn-spinner').classList.remove('hidden');

  try {
    const data = await apiForgotPassword(email);
    msgEl.textContent = data.message;
    msgEl.className = 'error-msg ok';
  } catch (err) {
    msgEl.textContent = err.message;
    msgEl.className = 'error-msg';
    msgEl.classList.remove('hidden');
  } finally {
    btn.disabled = false;
    btn.querySelector('.btn-text').classList.remove('hidden');
    btn.querySelector('.btn-spinner').classList.add('hidden');
  }
}

function doLogout() {
  Object.assign(state, { token: null, userName: null, contacts: [], locations: {},
                          userPos: null, selectedId: null });
  sessionStorage.removeItem('lz2_token');
  sessionStorage.removeItem('lz2_name');
  stopAutoRefresh();
  destroyMap();
  showLogin();
}

// ════════════════════════════════════════════════════════════════════════════
// Mapa — vista estándar
// ════════════════════════════════════════════════════════════════════════════

function initMap() {
  if (map) return;
  map = L.map('map', { center: DEFAULT_CENTER, zoom: DEFAULT_ZOOM });
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© <a href="https://openstreetmap.org/copyright">OpenStreetMap</a>',
    maxZoom: 19,
  }).addTo(map);
}

function destroyMap() {
  if (map) { map.remove(); map = null; }
  userMarker = userCircle = historyLayer = null;
  Object.keys(contactMarkers).forEach(k => delete contactMarkers[k]);
}

// Actualiza el marcador del usuario (omite si está en modo historial propio)
function setUserPosition(lat, lng, accuracy, timestamp) {
  state.userPos = { lat, lng, accuracy, timestamp };
  if (!map || state.selectedId === 'me') return;

  if (!userMarker) {
    const icon = L.divIcon({
      className: '',
      html: '<div class="user-marker-wrap"><div class="user-pulse"></div><div class="user-dot"></div></div>',
      iconSize: [20, 20], iconAnchor: [10, 10], popupAnchor: [0, -14],
    });
    userMarker = L.marker([lat, lng], { icon, zIndexOffset: 1000 })
      .addTo(map)
      .bindPopup(`<div class="popup-name">Yo · ${state.userName || ''}</div>`)
      .on('click', () => selectItem('me'));
  } else {
    userMarker.setLatLng([lat, lng]);
  }

  if (accuracy && accuracy < 5000) {
    if (!userCircle) {
      userCircle = L.circle([lat, lng], {
        radius: accuracy, color: USER_COLOR, fillColor: USER_COLOR,
        fillOpacity: .08, weight: 1,
      }).addTo(map);
    } else {
      userCircle.setLatLng([lat, lng]).setRadius(accuracy);
    }
  }
}

// Actualiza los marcadores de contactos (omite el seleccionado — está en modo historial)
function updateContactMarkers() {
  if (!map) return;
  state.contacts.forEach((contact, idx) => {
    if (contact.id === state.selectedId) return;

    const loc     = state.locations[contact.id];
    const color   = CONTACT_COLORS[idx % CONTACT_COLORS.length];
    const alias   = contact.alias || contact.email || '?';

    if (!loc) {
      contactMarkers[contact.id]?.remove();
      delete contactMarkers[contact.id];
      return;
    }

    const popup = buildStandardPopup(alias, color, loc.timestamp, loc.latitude, loc.longitude);

    if (contactMarkers[contact.id]) {
      contactMarkers[contact.id].setLatLng([loc.latitude, loc.longitude]);
      contactMarkers[contact.id].getPopup().setContent(popup);
    } else {
      const icon = L.divIcon({
        className: '',
        html: `<div class="contact-marker-wrap" style="background:${color}">${alias[0].toUpperCase()}</div>`,
        iconSize: [32, 32], iconAnchor: [16, 16], popupAnchor: [0, -18],
      });
      contactMarkers[contact.id] = L.marker([loc.latitude, loc.longitude], { icon })
        .addTo(map).bindPopup(popup)
        .on('click', () => selectItem(contact.id));
    }
  });
}

function buildStandardPopup(name, color, timestamp, lat, lng) {
  return `<div class="popup-name" style="border-left:3px solid ${color};padding-left:7px">${escapeHtml(name)}</div>
          <div class="popup-time">${formatTimeAgo(timestamp)}</div>
          <div class="popup-coords">${lat.toFixed(5)}, ${lng.toFixed(5)}</div>`;
}

// ════════════════════════════════════════════════════════════════════════════
// Mapa — vista historial
// ════════════════════════════════════════════════════════════════════════════

function showHistory(locations, label, color, fitMap = true) {
  clearHistoryLayer();
  if (!map || !locations.length) return;

  historyLayer = L.layerGroup().addTo(map);
  const latlngs = locations.map(l => [l.latitude, l.longitude]);

  // Traza de puntos (de más antiguo a más reciente)
  L.polyline(latlngs, { color, weight: 2.5, opacity: .65, dashArray: '5 5' })
    .addTo(historyLayer);

  // Marcadores intermedios
  locations.slice(0, -1).forEach(loc => {
    L.circleMarker([loc.latitude, loc.longitude], {
      radius: 5, color, fillColor: color, fillOpacity: .55, weight: 1.5,
    })
    .addTo(historyLayer)
    .bindPopup(buildHistoryPopup(label, color, loc));
  });

  // Marcador del punto más reciente (resaltado)
  const latest = locations[locations.length - 1];
  L.circleMarker([latest.latitude, latest.longitude], {
    radius: 9, color: '#fff', fillColor: color, fillOpacity: 1, weight: 2.5,
  })
  .addTo(historyLayer)
  .bindPopup(buildHistoryPopup(label, color, latest, true))
  .openPopup();

  if (fitMap) {
    if (latlngs.length > 1) {
      map.fitBounds(L.latLngBounds(latlngs), { padding: [50, 50], maxZoom: 16 });
    } else {
      map.flyTo(latlngs[0], USER_ZOOM, { duration: 1 });
    }
  }
}

function buildHistoryPopup(name, color, loc, isLatest = false) {
  const badge = isLatest
    ? '<span style="font-size:10px;background:#1e293b;padding:1px 5px;border-radius:4px;margin-left:6px">último</span>'
    : '';
  return `<div class="popup-name" style="border-left:3px solid ${color};padding-left:7px">${escapeHtml(name)}${badge}</div>
          <div class="popup-time">${formatTimeAgo(loc.timestamp)}</div>
          <div class="popup-coords">${loc.latitude.toFixed(5)}, ${loc.longitude.toFixed(5)}</div>`;
}

function clearHistoryLayer() {
  if (historyLayer) { historyLayer.remove(); historyLayer = null; }
}

// Elimina el marcador estándar del elemento seleccionado (se sustituye por el historial)
function hideStandardMarker(id) {
  if (id === 'me') {
    if (userMarker) { userMarker.remove(); userMarker = null; }
    if (userCircle) { userCircle.remove(); userCircle = null; }
  } else {
    if (contactMarkers[id]) { contactMarkers[id].remove(); delete contactMarkers[id]; }
  }
}

// Restaura el marcador estándar al salir del modo historial
function restoreStandardMarker(id) {
  if (id === 'me') {
    if (state.userPos) {
      const { lat, lng, accuracy, timestamp } = state.userPos;
      setUserPosition(lat, lng, accuracy, timestamp);
    }
  } else {
    updateContactMarkers();
  }
}

// ════════════════════════════════════════════════════════════════════════════
// Selección de elemento (contacto o usuario propio)
// ════════════════════════════════════════════════════════════════════════════

async function selectItem(id) {
  // Deseleccionar si ya estaba seleccionado
  if (state.selectedId === id) {
    const prev = state.selectedId;
    state.selectedId = null;
    clearHistoryLayer();
    restoreStandardMarker(prev);
    renderContactList();
    return;
  }

  // Limpiar selección previa sin restaurar (se reemplaza por la nueva)
  if (state.selectedId !== null) {
    clearHistoryLayer();
    restoreStandardMarker(state.selectedId);
  }

  state.selectedId = id;
  renderContactList();
  hideStandardMarker(id);

  showRefreshIndicator(true);
  try {
    const { label, color, history } = await fetchHistoryForId(id);
    showHistory(history, label, color, true);
  } catch (err) {
    showToast(`Error al cargar historial: ${err.message}`, true);
    // Revertir selección si falla
    state.selectedId = null;
    restoreStandardMarker(id);
    renderContactList();
  } finally {
    showRefreshIndicator(false);
  }
}

async function fetchHistoryForId(id) {
  if (id === 'me') {
    return {
      label:   state.userName || 'Yo',
      color:   USER_COLOR,
      history: await apiGetMyLocationHistory(),
    };
  }
  const idx     = state.contacts.findIndex(c => c.id === id);
  const contact = state.contacts[idx];
  return {
    label:   contact?.alias || contact?.email || 'Contacto',
    color:   CONTACT_COLORS[idx % CONTACT_COLORS.length],
    history: await apiGetContactLocationHistory(id),
  };
}

// ════════════════════════════════════════════════════════════════════════════
// Carga y refresco de datos
// ════════════════════════════════════════════════════════════════════════════

async function loadAll() {
  showRefreshIndicator(true);
  try {
    const [contacts, locs] = await Promise.all([
      apiGetContacts(),
      apiGetContactLocations(),
    ]);

    state.contacts = contacts || [];
    state.locations = {};

    let myLoc = null;
    for (const l of (locs || [])) {
      if (l.contactId === 0) {
        myLoc = l;            // entrada especial: el propio usuario
      } else {
        state.locations[l.contactId] = l;
      }
    }

    if (myLoc) {
      setUserPosition(myLoc.latitude, myLoc.longitude, myLoc.accuracy, myLoc.timestamp);
      setGeoBadge('ok', formatTimeAgo(myLoc.timestamp));
    } else {
      setGeoBadge('warn', 'Sin ubicación');
    }

    renderContactList();

    if (state.selectedId !== null) {
      // Refrescar historial del seleccionado sin mover el mapa
      try {
        const { label, color, history } = await fetchHistoryForId(state.selectedId);
        showHistory(history, label, color, false);
      } catch { /* fallo silencioso en refresco de fondo */ }
    } else {
      updateContactMarkers();
    }
  } catch (err) {
    showToast(`Error al actualizar: ${err.message}`, true);
  } finally {
    showRefreshIndicator(false);
  }
}

function startAutoRefresh() {
  if (refreshTimer) return;
  refreshTimer = setInterval(loadAll, REFRESH_MS);
}

function stopAutoRefresh() {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null; }
}

// ════════════════════════════════════════════════════════════════════════════
// UI
// ════════════════════════════════════════════════════════════════════════════

function showLogin() {
  document.getElementById('login-screen').classList.remove('hidden');
  document.getElementById('app-screen').classList.add('hidden');
  showLoginPanel('panel-login');
}

function showApp() {
  document.getElementById('login-screen').classList.add('hidden');
  document.getElementById('app-screen').classList.remove('hidden');
  document.getElementById('nav-username').textContent = state.userName || '';
  initMap();
  renderContactList();   // muestra "YO" inmediatamente, antes de que lleguen los datos
  loadAll().then(startAutoRefresh);
}

function renderContactList() {
  const list  = document.getElementById('contact-list');
  const count = document.getElementById('contact-count');
  if (!list) return;

  count.textContent = state.contacts.length;
  list.innerHTML = '';

  // ── Fila del usuario propio ──────────────────────────────────────────────
  const meSelected = state.selectedId === 'me';
  const meMeta     = state.userPos
    ? formatTimeAgo(state.userPos.timestamp)
    : 'Sin ubicación';
  const meDot      = state.userPos ? onlineDotClass(state.userPos.timestamp) : 'offline';
  const meInitial  = (state.userName || 'Y')[0].toUpperCase();

  const meLi = document.createElement('li');
  meLi.className = `contact-item${meSelected ? ' selected' : ''}`;
  meLi.innerHTML = `
    <div class="contact-avatar" style="background:${USER_COLOR}">${meInitial}</div>
    <div class="contact-info">
      <div class="contact-name">YO</div>
      <div class="contact-meta">${escapeHtml(meMeta)}</div>
    </div>
    <div class="contact-dot ${meDot}"></div>
  `;
  meLi.addEventListener('click', () => selectItem('me'));
  list.appendChild(meLi);

  // ── Separador ────────────────────────────────────────────────────────────
  if (state.contacts.length > 0) {
    const sep = document.createElement('li');
    sep.className = 'contact-separator';
    sep.textContent = 'Contactos';
    list.appendChild(sep);
  } else {
    const empty = document.createElement('li');
    empty.className = 'contact-empty';
    empty.textContent = 'Sin contactos aceptados';
    list.appendChild(empty);
  }

  // ── Filas de contactos ───────────────────────────────────────────────────
  state.contacts.forEach((contact, idx) => {
    const loc     = state.locations[contact.id];
    const alias   = contact.alias || contact.email || 'Contacto';
    const color   = CONTACT_COLORS[idx % CONTACT_COLORS.length];
    const dotCls  = loc ? onlineDotClass(loc.timestamp) : 'offline';
    const meta    = loc
      ? `${formatTimeAgo(loc.timestamp)}${
          state.userPos
            ? '  ·  ' + formatDist(haversine(state.userPos.lat, state.userPos.lng, loc.latitude, loc.longitude))
            : ''}`
      : 'Sin ubicación';

    const li = document.createElement('li');
    li.className = `contact-item${state.selectedId === contact.id ? ' selected' : ''}`;
    li.innerHTML = `
      <div class="contact-avatar" style="background:${color}">
        ${contact.photoUrl
          ? `<img src="${escapeHtml(contact.photoUrl)}" alt="${escapeHtml(alias)}" />`
          : alias[0].toUpperCase()}
      </div>
      <div class="contact-info">
        <div class="contact-name">${escapeHtml(alias)}</div>
        <div class="contact-meta">${escapeHtml(meta)}</div>
      </div>
      <div class="contact-dot ${dotCls}"></div>
    `;
    li.addEventListener('click', () => selectItem(contact.id));
    list.appendChild(li);
  });
}

function showRefreshIndicator(visible) {
  document.getElementById('refresh-indicator')?.classList.toggle('hidden', !visible);
  document.getElementById('refresh-btn')?.classList.toggle('spinning', visible);
}

function setGeoBadge(type, label) {
  const el = document.getElementById('geo-status');
  if (!el) return;
  el.className   = `geo-badge ${type}`;
  el.textContent = label;
}

function flyToUser() {
  if (!map) return;
  if (state.userPos) {
    map.flyTo([state.userPos.lat, state.userPos.lng], USER_ZOOM, { duration: 1 });
  } else {
    showToast('Sin posición propia almacenada', true);
  }
}

// ════════════════════════════════════════════════════════════════════════════
// Toast
// ════════════════════════════════════════════════════════════════════════════

function showToast(msg, isError = false, ms = 3500) {
  const el = document.getElementById('toast');
  if (!el) return;
  el.textContent = msg;
  el.className   = `toast${isError ? ' error' : ''}`;
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.add('hidden'), ms);
}

// ════════════════════════════════════════════════════════════════════════════
// Utilidades
// ════════════════════════════════════════════════════════════════════════════

function haversine(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const φ1 = lat1 * Math.PI / 180, φ2 = lat2 * Math.PI / 180;
  const Δφ = (lat2 - lat1) * Math.PI / 180;
  const Δλ = (lon2 - lon1) * Math.PI / 180;
  const a  = Math.sin(Δφ/2)**2 + Math.cos(φ1)*Math.cos(φ2)*Math.sin(Δλ/2)**2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function formatDist(m) {
  return m < 1000 ? `${Math.round(m)} met.` : `${(m / 1000).toFixed(1)} km`;
}

function formatTimeAgo(iso) {
  const s = Math.floor((Date.now() - new Date(iso)) / 1000);
  if (s < 60)    return 'ahora';
  if (s < 3600)  return `hace ${Math.floor(s / 60)} min`;
  if (s < 86400) return `hace ${Math.floor(s / 3600)} h`;
  return `hace ${Math.floor(s / 86400)} días`;
}

function onlineDotClass(iso) {
  const m = (Date.now() - new Date(iso)) / 60_000;
  return m < 5 ? 'online' : m < 60 ? 'recent' : 'offline';
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ════════════════════════════════════════════════════════════════════════════
// Emparejamiento
// ════════════════════════════════════════════════════════════════════════════

async function apiGenerateQr() {
  return apiFetch('/api/contacts/pair/qr', { method: 'POST' });
}

async function openPairModal() {
  document.getElementById('pair-modal').classList.remove('hidden');
  loadQrCode();
}

function closePairModal() {
  document.getElementById('pair-modal').classList.add('hidden');
}

async function loadQrCode() {
  const box = document.getElementById('qr-container');
  const expEl = document.getElementById('qr-expiry');
  box.innerHTML = '<span style="font-size:13px;color:var(--text-muted)">Generando…</span>';
  expEl.textContent = '';
  try {
    const data = await apiGenerateQr();
    const pairUrl = buildPairUrl(data.token);

    box.innerHTML = '';
    // eslint-disable-next-line no-undef
    new QRCode(box, { text: pairUrl, width: 200, height: 200,
      colorDark: '#f1f5f9', colorLight: '#1e293b' });

    const expDate = new Date(data.expiresAt);
    expEl.textContent = `Válido hasta ${expDate.toLocaleTimeString([], { hour:'2-digit', minute:'2-digit' })}`;
  } catch (err) {
    box.innerHTML = `<span style="color:var(--danger);font-size:13px">${escapeHtml(err.message)}</span>`;
  }
}

function buildPairUrl(token) {
  const base = window.location.hostname === 'localhost'
    ? `${window.location.origin}`
    : `${window.location.origin}/localiza2`;
  return `${base}/pair.html?token=${token}`;
}

async function sendEmailInvite(email, alias) {
  return apiFetch('/api/contacts/invite', {
    method: 'POST',
    body: JSON.stringify({ email, alias }),
  });
}

// ════════════════════════════════════════════════════════════════════════════
// Eventos
// ════════════════════════════════════════════════════════════════════════════

document.getElementById('login-form').addEventListener('submit', e => {
  e.preventDefault();
  const email    = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;
  if (email && password) doLogin(email, password);
});

document.getElementById('register-form').addEventListener('submit', e => {
  e.preventDefault();
  const name     = document.getElementById('reg-name').value.trim();
  const email    = document.getElementById('reg-email').value.trim();
  const password = document.getElementById('reg-password').value;
  if (name && email && password) doRegister(name, email, password);
});

document.getElementById('forgot-form').addEventListener('submit', e => {
  e.preventDefault();
  const email = document.getElementById('forgot-email').value.trim();
  if (email) doForgotPassword(email);
});

document.getElementById('go-register').addEventListener('click', e => { e.preventDefault(); showLoginPanel('panel-register'); });
document.getElementById('go-forgot').addEventListener('click', e => { e.preventDefault(); showLoginPanel('panel-forgot'); });
document.getElementById('go-login-from-reg').addEventListener('click', e => { e.preventDefault(); showLoginPanel('panel-login'); });
document.getElementById('go-login-from-forgot').addEventListener('click', e => { e.preventDefault(); showLoginPanel('panel-login'); });

document.getElementById('logout-btn').addEventListener('click', doLogout);
document.getElementById('refresh-btn').addEventListener('click', () =>
  loadAll().then(() => showToast('Datos actualizados')));
document.getElementById('locate-btn').addEventListener('click', flyToUser);

document.getElementById('pair-btn').addEventListener('click', openPairModal);
document.getElementById('pair-modal-close').addEventListener('click', closePairModal);
document.getElementById('pair-modal').addEventListener('click', e => {
  if (e.target === document.getElementById('pair-modal')) closePairModal();
});

document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(t => t.classList.add('hidden'));
    btn.classList.add('active');
    document.getElementById(`tab-${btn.dataset.tab}`).classList.remove('hidden');
    if (btn.dataset.tab === 'qr') loadQrCode();
  });
});

document.getElementById('qr-refresh-btn').addEventListener('click', loadQrCode);

document.getElementById('invite-form').addEventListener('submit', async e => {
  e.preventDefault();
  const btn    = document.getElementById('invite-btn');
  const result = document.getElementById('invite-result');
  const email  = document.getElementById('invite-email').value.trim();
  const alias  = document.getElementById('invite-alias').value.trim();
  result.className = 'hidden';
  btn.disabled = true;
  btn.querySelector('.btn-text').classList.add('hidden');
  btn.querySelector('.btn-spinner').classList.remove('hidden');
  try {
    await sendEmailInvite(email, alias);
    result.textContent = `Invitación enviada a ${email}.`;
    result.className = 'status-msg ok';
    document.getElementById('invite-email').value = '';
    document.getElementById('invite-alias').value = '';
  } catch (err) {
    result.textContent = err.message;
    result.className = 'status-msg err';
  } finally {
    btn.disabled = false;
    btn.querySelector('.btn-text').classList.remove('hidden');
    btn.querySelector('.btn-spinner').classList.add('hidden');
  }
});

// ════════════════════════════════════════════════════════════════════════════
// Arranque
// ════════════════════════════════════════════════════════════════════════════

if (state.token) showApp(); else showLogin();
