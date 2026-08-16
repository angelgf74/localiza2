'use strict';

// ─── Configuración ───────────────────────────────────────────────────────────
const isDev = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
const API_BASE       = isDev ? 'http://localhost:5135' : 'https://localiza2-api.angelgf.com.es';
const DEFAULT_CENTER = [40.4168, -3.7038];
const DEFAULT_ZOOM   = 6;

const CONTACT_COLORS = [
  '#ef4444', '#f97316', '#eab308', '#22c55e',
  '#14b8a6', '#3b82f6', '#8b5cf6', '#ec4899',
];

// ─── Estado ──────────────────────────────────────────────────────────────────
const state = {
  token:          sessionStorage.getItem('lz2_token'),
  refreshToken:   sessionStorage.getItem('lz2_refresh'),
  role:           sessionStorage.getItem('lz2_role'),
  userName:       sessionStorage.getItem('lz2_name'),
  users:          [],     // AdminUserDto[]
  selectedUserId: null,
  historyPoints:  [],     // AdminLocationPointDto[]
  historyHasMore: false,
  currentView:    'table', // 'table' | 'map'
};

// ─── Guard de acceso ─────────────────────────────────────────────────────────
if (!state.token || state.role !== 'SuperAdmin') {
  window.location.href = 'index.html';
}

// ─── Leaflet ─────────────────────────────────────────────────────────────────
let map          = null;
let historyLayer = null;

// ════════════════════════════════════════════════════════════════════════════
// API
// ════════════════════════════════════════════════════════════════════════════

// Refresco en curso compartido entre llamadas concurrentes: el refresh token rota de un
// solo uso, así que dos 401 simultáneos no deben disparar dos refrescos.
let refreshInFlight = null;

async function tryRefreshToken() {
  if (!state.refreshToken) return false;
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      try {
        const res = await fetch(`${API_BASE}/api/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken: state.refreshToken })
        });
        if (!res.ok) return false;
        const data = await res.json();
        state.token        = data.token;
        state.refreshToken = data.refreshToken;
        sessionStorage.setItem('lz2_token',   data.token);
        sessionStorage.setItem('lz2_refresh', data.refreshToken);
        return true;
      } catch {
        return false;
      } finally {
        refreshInFlight = null;
      }
    })();
  }
  return refreshInFlight;
}

async function apiFetch(path, options = {}, _retried = false) {
  const headers = { 'Content-Type': 'application/json' };
  if (state.token) headers['Authorization'] = `Bearer ${state.token}`;
  Object.assign(headers, options.headers || {});

  const res = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (res.status === 401) {
    // 403 (rol insuficiente) no se reintenta: refrescar no cambia el rol del token.
    if (!_retried && await tryRefreshToken()) return apiFetch(path, options, true);
    doLogout();
    throw new Error('Sesión expirada');
  }
  if (res.status === 403) { doLogout(); throw new Error('Sesión expirada'); }
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(text || `Error ${res.status}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

const apiGetUsers = () => apiFetch('/api/admin/users');
const apiGetUserHistory = (userId, limit = 100, before = null) => {
  const p = new URLSearchParams({ limit });
  if (before) p.set('before', before);
  return apiFetch(`/api/admin/users/${userId}/history?${p}`);
};

function doLogout() {
  if (state.refreshToken) {
    fetch(`${API_BASE}/api/auth/logout`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: state.refreshToken })
    }).catch(() => {});
  }
  sessionStorage.removeItem('lz2_token');
  sessionStorage.removeItem('lz2_refresh');
  sessionStorage.removeItem('lz2_name');
  sessionStorage.removeItem('lz2_role');
  window.location.href = 'index.html';
}

// ════════════════════════════════════════════════════════════════════════════
// Mapa
// ════════════════════════════════════════════════════════════════════════════

function initMap() {
  if (map) return;
  map = L.map('admin-map', { center: DEFAULT_CENTER, zoom: DEFAULT_ZOOM });
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© <a href="https://openstreetmap.org/copyright">OpenStreetMap</a>',
    maxZoom: 19,
  }).addTo(map);
}

function clearHistoryLayer() {
  if (historyLayer) { historyLayer.remove(); historyLayer = null; }
}

function buildHistoryPopup(name, color, loc, isLatest = false) {
  const badge = isLatest
    ? '<span style="font-size:10px;background:#1e293b;padding:1px 5px;border-radius:4px;margin-left:6px">último</span>'
    : '';
  return `<div class="popup-name" style="border-left:3px solid ${color};padding-left:7px">${escapeHtml(name)}${badge}</div>
          <div class="popup-time">${formatTimeAgo(loc.timestamp)}</div>
          <div class="popup-coords">${loc.latitude.toFixed(5)}, ${loc.longitude.toFixed(5)}</div>`;
}

function showHistory(locations, label, color) {
  clearHistoryLayer();
  if (!map || !locations.length) return;

  historyLayer = L.layerGroup().addTo(map);
  const latlngs = locations.map(l => [l.latitude, l.longitude]);

  L.polyline(latlngs, { color, weight: 2.5, opacity: .65, dashArray: '5 5' })
    .addTo(historyLayer);

  locations.slice(0, -1).forEach(loc => {
    L.circleMarker([loc.latitude, loc.longitude], {
      radius: 5, color, fillColor: color, fillOpacity: .55, weight: 1.5,
    })
    .addTo(historyLayer)
    .bindPopup(buildHistoryPopup(label, color, loc));
  });

  const latest = locations[locations.length - 1];
  L.circleMarker([latest.latitude, latest.longitude], {
    radius: 9, color: '#fff', fillColor: color, fillOpacity: 1, weight: 2.5,
  })
  .addTo(historyLayer)
  .bindPopup(buildHistoryPopup(label, color, latest, true))
  .openPopup();

  if (latlngs.length > 1) {
    map.fitBounds(L.latLngBounds(latlngs), { padding: [50, 50], maxZoom: 16 });
  } else {
    map.flyTo(latlngs[0], 15, { duration: 1 });
  }
}

// ════════════════════════════════════════════════════════════════════════════
// Lista de usuarios
// ════════════════════════════════════════════════════════════════════════════

async function loadUsers() {
  const list = document.getElementById('admin-user-list');
  try {
    state.users = await apiGetUsers();
  } catch (err) {
    list.innerHTML = `<li class="contact-empty">Error al cargar usuarios: ${escapeHtml(err.message)}</li>`;
    return;
  }
  document.getElementById('user-count').textContent = state.users.length;
  renderUserList();
}

function renderUserList() {
  const list = document.getElementById('admin-user-list');
  list.innerHTML = '';

  if (state.users.length === 0) {
    list.innerHTML = '<li class="contact-empty">Sin usuarios registrados</li>';
    return;
  }

  state.users.forEach((user, idx) => {
    const color   = CONTACT_COLORS[idx % CONTACT_COLORS.length];
    const initial = (user.name || user.email || '?')[0].toUpperCase();
    const meta    = user.lastLocationAt ? formatTimeAgo(user.lastLocationAt) : 'Sin ubicaciones';

    const li = document.createElement('li');
    li.className = `contact-item${user.id === state.selectedUserId ? ' selected' : ''}`;
    li.innerHTML = `
      <div class="contact-avatar" style="background:${color}">${escapeHtml(initial)}</div>
      <div class="contact-info">
        <div class="contact-name">${escapeHtml(user.name || user.email)}</div>
        <div class="contact-meta">${escapeHtml(user.email)} · ${escapeHtml(meta)}</div>
      </div>
      <div class="contact-dot ${user.sharingEnabled ? 'online' : 'offline'}"></div>
    `;
    li.addEventListener('click', () => selectUser(user.id));
    list.appendChild(li);
  });
}

// ════════════════════════════════════════════════════════════════════════════
// Selección de usuario e historial
// ════════════════════════════════════════════════════════════════════════════

async function selectUser(userId) {
  state.selectedUserId = userId;
  state.historyPoints  = [];
  state.historyHasMore = false;
  renderUserList();

  const user = state.users.find(u => u.id === userId);
  document.getElementById('admin-empty-state').classList.add('hidden');
  document.getElementById('admin-user-panel').classList.remove('hidden');
  document.getElementById('admin-detail-name').textContent = user?.name || user?.email || 'Usuario';

  try {
    await loadHistory();
  } catch (err) {
    showToast(`Error al cargar historial: ${err.message}`, true);
  }
  renderCurrentView();
}

async function loadHistory(before = null) {
  const page = await apiGetUserHistory(state.selectedUserId, 100, before);
  state.historyHasMore = page.length === 100;
  state.historyPoints  = before ? [...page, ...state.historyPoints] : page;
}

async function loadMoreHistory() {
  if (!state.selectedUserId || !state.historyHasMore || !state.historyPoints.length) return;
  const before = state.historyPoints[0].timestamp;
  const btn = document.getElementById('admin-load-more-btn');
  btn.disabled = true;
  btn.textContent = 'Cargando…';
  try {
    await loadHistory(before);
    renderCurrentView();
  } catch (err) {
    showToast(`Error al cargar más: ${err.message}`, true);
  } finally {
    btn.disabled = false;
    btn.textContent = 'Cargar más antiguos';
  }
}

// ════════════════════════════════════════════════════════════════════════════
// Vistas: tabla / mapa
// ════════════════════════════════════════════════════════════════════════════

function renderTable() {
  const tbody = document.getElementById('admin-history-tbody');
  const empty = document.getElementById('admin-history-empty');
  const rows  = [...state.historyPoints].reverse();

  tbody.innerHTML = rows.map(l => `
    <tr>
      <td>${new Date(l.timestamp).toLocaleString('es-ES')}</td>
      <td>${l.latitude.toFixed(5)}</td>
      <td>${l.longitude.toFixed(5)}</td>
      <td>${l.accuracy != null ? Math.round(l.accuracy) + ' m' : '—'}</td>
      <td>${l.batteryLevel != null ? l.batteryLevel + '%' : '—'}</td>
    </tr>`).join('');

  empty.classList.toggle('hidden', rows.length > 0);
  document.getElementById('admin-load-more-btn').classList.toggle('hidden', !state.historyHasMore);
}

function renderMap() {
  initMap();
  setTimeout(() => map.invalidateSize(), 0);
  const idx   = state.users.findIndex(u => u.id === state.selectedUserId);
  const color = CONTACT_COLORS[idx % CONTACT_COLORS.length];
  const label = state.users[idx]?.name || state.users[idx]?.email || 'Usuario';
  showHistory(state.historyPoints, label, color);
}

function renderCurrentView() {
  if (state.currentView === 'table') renderTable();
  else renderMap();
}

// ════════════════════════════════════════════════════════════════════════════
// Toast
// ════════════════════════════════════════════════════════════════════════════

let toastTimer = null;
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

function formatTimeAgo(iso) {
  const s = Math.floor((Date.now() - new Date(iso)) / 1000);
  if (s < 60)    return 'ahora';
  if (s < 3600)  return `hace ${Math.floor(s / 60)} min`;
  if (s < 86400) return `hace ${Math.floor(s / 3600)} h`;
  return `hace ${Math.floor(s / 86400)} días`;
}

function escapeHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// ════════════════════════════════════════════════════════════════════════════
// Eventos
// ════════════════════════════════════════════════════════════════════════════

document.getElementById('nav-username').textContent = state.userName || '';
document.getElementById('logout-btn').addEventListener('click', doLogout);
document.getElementById('admin-load-more-btn').addEventListener('click', loadMoreHistory);

document.querySelectorAll('#admin-tab-table, #admin-tab-map').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('#admin-tab-table, #admin-tab-map').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    state.currentView = btn.dataset.view;
    document.getElementById('admin-view-table').classList.toggle('hidden', state.currentView !== 'table');
    document.getElementById('admin-view-map').classList.toggle('hidden', state.currentView !== 'map');
    renderCurrentView();
  });
});

// ════════════════════════════════════════════════════════════════════════════
// Arranque
// ════════════════════════════════════════════════════════════════════════════

loadUsers();
