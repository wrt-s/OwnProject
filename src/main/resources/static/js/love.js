/* Love H5 — redesigned + couple auth (ORDERER / CHEF) */
/** Canonical statuses: pending / cooking / ready / completed / cancelled */
const STATUS_MAP = {
  pending: '待接单',
  cooking: '制作中',
  ready: '待取餐',
  completed: '已完成',
  cancelled: '已取消',
  canceled: '已取消'
};

/** Map legacy DB statuses → canonical (display, filters, chef actions) */
const STATUS_ALIAS = {
  confirmed: 'pending',
  received: 'pending',
  preparing: 'cooking',
  delivered: 'ready'
};
function normalizeStatus(s) {
  if (!s) return s;
  const key = String(s).trim().toLowerCase();
  return STATUS_ALIAS[key] || key;
}
function statusLabel(s) {
  const n = normalizeStatus(s);
  return STATUS_MAP[n] || s;
}

const TAB_META = {
  order:  { title: 'Love',   sub: '点一份爱意给 TA' },
  orders: { title: '订单',   sub: '追踪每一份心意' },
  play:   { title: '玩法',   sub: '赚币 · 互动 · 惊喜' },
  mine:   { title: '我的',   sub: '账户与生活入口' }
};

const CHEF_NEXT = {
  pending: [{ status: 'cooking', label: '接单制作' }],
  cooking: [{ status: 'ready', label: '做好了' }],
  ready:   [{ status: 'completed', label: '完成' }]
};

let currentUser = null; // { id, nickname, role, roleLabel, phone }
let heartbeatTimer = null;
const HEARTBEAT_MS = 30000;
let cart = [];
const CART_LS_KEY = 'love_cart';
function normalizeCartItem(it) {
  if (!it || typeof it !== 'object') return null;
  const rawId = (it.dishId != null ? it.dishId : it.id);
  const dishId = Number(rawId);
  if (!Number.isFinite(dishId) || dishId <= 0) return null;
  const qty = Math.max(1, Math.min(99, Number(it.quantity != null ? it.quantity : it.qty) || 1));
  const priceNum = Number(it.price);
  return {
    dishId,
    dishName: String(it.dishName || it.name || ('#' + dishId)),
    price: Number.isFinite(priceNum) ? priceNum : (it.price != null ? it.price : 0),
    priceType: it.priceType || '爱心币',
    quantity: qty,
    remark: it.remark || ''
  };
}
function readCartRawFromStorage() {
  try {
    const saved = JSON.parse(localStorage.getItem(CART_LS_KEY) || '[]');
    return Array.isArray(saved) ? saved : [];
  } catch (e) { return []; }
}
function reloadCartFromStorage() {
  cart = readCartRawFromStorage().map(normalizeCartItem).filter(Boolean);
  return cart;
}
reloadCartFromStorage();
function persistCart() {
  try {
    localStorage.setItem(CART_LS_KEY, JSON.stringify(cart));
    localStorage.setItem('love_cart_updated_at', String(Date.now()));
  } catch (e) {}
  // mirror to session so 小布/其他页写入后首页可恢复
  try {
    fetch('/api/cart/sync', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ items: cart })
    }).catch(function(){});
  } catch (e2) {}
}
/** Update badge/label only — never write localStorage (avoids wiping 小布 writes). */
function paintCartBadge() {
  const n = cartCount();
  document.querySelectorAll('.cart-badge-num').forEach(el => {
    el.textContent = n > 99 ? '99+' : String(n);
    el.style.display = n > 0 ? 'flex' : 'none';
  });
  const label = document.getElementById('cartFabLabel');
  if (label) {
    if (n > 0) {
      label.textContent = formatCartTotals();
    } else {
      label.textContent = '购物车';
    }
  }
}
/** Apply server/session cart items into memory + localStorage (recovery after wipe). */
function applyServerCartItems(items) {
  if (!Array.isArray(items) || !items.length) return false;
  const mapped = items.map(normalizeCartItem).filter(Boolean);
  if (!mapped.length) return false;
  // merge by dishId (server wins quantity if higher)
  const byId = new Map();
  cart.forEach(it => byId.set(Number(it.dishId), it));
  mapped.forEach(it => {
    const id = Number(it.dishId);
    const prev = byId.get(id);
    if (!prev) byId.set(id, it);
    else {
      prev.quantity = Math.max(Number(prev.quantity) || 1, Number(it.quantity) || 1);
      if (it.dishName) prev.dishName = it.dishName;
      if (it.price != null) prev.price = it.price;
      if (it.priceType) prev.priceType = it.priceType;
    }
  });
  cart = Array.from(byId.values());
  persistCart();
  return true;
}
async function pullServerCart() {
  try {
    const r = await api('/api/cart');
    const items = r && r.code === 200 && r.data ? (r.data.items || []) : [];
    if (applyServerCartItems(items)) {
      paintCartBadge();
      syncCartFab();
      const modal = document.getElementById('cartModal');
      if (modal && modal.classList.contains('show')) renderCart();
      return true;
    }
  } catch (e) {}
  return false;
}
/** Re-read love_cart after bfcache / history.back. Never persist empty over storage. Also pulls session cart. */
function refreshCartFromStorage() {
  reloadCartFromStorage();
  paintCartBadge();
  syncCartFab();
  const modal = document.getElementById('cartModal');
  if (modal && modal.classList.contains('show')) renderCart();
  // Always try session recovery (covers wiped localStorage from stale cached love.js)
  pullServerCart();
}
function bindCartHomeRefresh() {
  if (window.__loveCartRefreshBound) return;
  window.__loveCartRefreshBound = true;
  const kick = () => { try { refreshCartFromStorage(); } catch (e) {} };
  window.addEventListener('pageshow', kick);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') kick();
  });
  window.addEventListener('focus', kick);
  window.addEventListener('storage', (e) => {
    if (!e.key || e.key === CART_LS_KEY || e.key === 'love_cart_updated_at') kick();
  });
}
let allDishes = [];
let categories = [];
let currentCategoryId = null;
let currentKeyword = '';
let currentTab = 'order';
let confirmResolver = null;
let kitchenFilter = 'all';
let cachedOrders = [];

document.addEventListener('DOMContentLoaded', async () => {
  bindTabs();
  bindConfirm();
  bindMoodHomeRefresh();
  bindCartHomeRefresh();
  handleHomeQueryParams();
  const ok = await ensureAuth();
  if (!ok) return;
  applyRoleUI();
  loadCoin();
  loadCategories();
  loadDishes();
  loadNearestAnniversary();
  loadPartnerMoodToday();
  refreshCartFromStorage();
  updateOrdersTabBadge();
  syncCartFab();
  applyHomeTabFromQuery();
});

/** Handle /?denied=chef toast and /?tab=orders deep-link */
function handleHomeQueryParams() {
  try {
    const qs = new URLSearchParams(location.search || '');
    const denied = qs.get('denied');
    if (denied === 'chef' || denied === 'dashboard' || denied === 'location') {
      const deniedText = {
        dashboard: '仅做餐方可查看数据看板',
        location: '仅做餐方可查看家人位置',
        chef: '仅做餐方可访问该页面'
      };
      toast(deniedText[denied] || '仅做餐方可访问该页面');
      qs.delete('denied');
      const next = qs.toString();
      const url = location.pathname + (next ? ('?' + next) : '') + (location.hash || '');
      try { history.replaceState(null, '', url); } catch (e) {}
    }
  } catch (e) {}
}
function applyHomeTabFromQuery() {
  try {
    const qs = new URLSearchParams(location.search || '');
    const tab = qs.get('tab');
    if (tab && document.querySelector('.tab-item[data-tab="' + tab + '"]')) {
      switchTab(tab);
      qs.delete('tab');
      const next = qs.toString();
      const url = location.pathname + (next ? ('?' + next) : '') + (location.hash || '');
      try { history.replaceState(null, '', url); } catch (e) {}
    }
  } catch (e) {}
}

/* ---------- Auth ---------- */
function getUserId() {
  return currentUser && currentUser.id ? Number(currentUser.id) : null;
}

function isChef() {
  return currentUser && (currentUser.role === 'CHEF' || currentUser.role === 'ADMIN');
}

async function ensureAuth() {
  try {
    const r = await api('/api/auth/me');
    if (r.code === 200 && r.data) {
      currentUser = normalizeUser(r.data);
      persistUser(currentUser);
      return true;
    }
  } catch (e) {}
  // soft gate
  location.replace('/login');
  return false;
}

function normalizeUser(u) {
  let role = (u.role || 'ORDERER').toUpperCase();
  if (role === 'ADMIN') role = 'CHEF';
  if (role === 'USER') role = 'ORDERER';
  return {
    id: u.id || u.userId,
    nickname: u.nickname || 'Love 用户',
    phone: u.phone || '',
    role,
    roleLabel: u.roleLabel || (role === 'CHEF' ? '做餐方' : '点单方'),
    avatarUrl: u.avatarUrl
  };
}

function persistUser(u) {
  localStorage.setItem('love_user', JSON.stringify(u));
  localStorage.setItem('love_user_id', String(u.id));
}

function applyRoleUI() {
  document.body.classList.toggle('is-chef', isChef());
  startHeartbeat();
  try { bindLocationReport(); reportMyLocation({ force: false }); } catch (e) {}
  /* LoveAppNotify disabled (no phone system notifications) */
  const nameEl = document.getElementById('mineNickname');
  if (nameEl) nameEl.textContent = currentUser.nickname;
  const badge = document.getElementById('mineRoleBadge');
  if (badge) {
    badge.className = 'role-badge ' + (isChef() ? 'chef' : 'orderer');
    badge.textContent = (isChef() ? '👨‍🍳 ' : '🍜 ') + currentUser.roleLabel;
  }
  const title = document.getElementById('ordersTitle');
  if (title) title.textContent = isChef() ? '厨房订单' : '我的订单';
  if (isChef()) {
    TAB_META.orders.sub = '接单 · 更新状态';
  }
}

async function doLogout() {
  const ok = await askConfirm('退出登录？', '下次需要重新输入账号');
  if (!ok) return;
  stopHeartbeat();
  try {
    await api('/api/auth/logout', { method: 'POST' });
  } catch (e) {}
  localStorage.removeItem('love_user');
  localStorage.removeItem('love_user_id');
  try { localStorage.setItem('love_remember_flag', '0'); } catch (e) {}
  location.replace('/login');
}

/* ---------- Tabs ---------- */
function bindTabs() {
  document.querySelectorAll('.tab-item').forEach(btn => {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
  });
}

function switchTab(name) {
  if (!TAB_META[name]) return;
  currentTab = name;
  document.querySelectorAll('.tab-item').forEach(b => {
    b.classList.toggle('active', b.dataset.tab === name);
  });
  document.querySelectorAll('.page').forEach(p => {
    p.classList.toggle('active', p.id === 'page-' + name);
  });
  const meta = TAB_META[name];
  document.getElementById('headerTitle').textContent = meta.title;
  document.getElementById('headerSub').textContent = meta.sub;
  if (name === 'order') loadPartnerMoodToday();
  if (name === 'orders') {
    loadOrders();
    updateOrdersTabBadge();
  }
  if (name === 'mine') {
    applyRoleUI();
    loadCoin();
    if (isChef()) {
      loadPartnerOnline();
      const ubal = document.querySelector('#page-mine .ubal');
      if (ubal && !ubal.dataset.partnerLabeled) {
        ubal.dataset.partnerLabeled = '1';
        ubal.innerHTML = '💖 点单方爱心币 <span data-coin>0</span>';
      }
    }
  }
  syncCartFab();
}

function syncCartFab() {
  const fab = document.getElementById('cartFab');
  if (!fab) return;
  fab.classList.toggle('visible', currentTab === 'order');
}

/* ---------- API / UI helpers ---------- */
async function api(url, opts) {
  const options = Object.assign({ credentials: 'same-origin' }, opts || {});
  const res = await fetch(url, options);
  const data = await res.json().catch(() => ({ code: res.status, message: '请求失败' }));
  if (data.code === 401 && !String(url).includes('/api/auth/')) {
    location.replace('/login');
    throw new Error('unauthorized');
  }
  return data;
}

function toast(msg, ms) {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(el._t);
  el._t = setTimeout(() => el.classList.remove('show'), ms || 2200);
}

function bindConfirm() {
  const mask = document.getElementById('confirmMask');
  document.getElementById('confirmCancel').onclick = () => finishConfirm(false);
  document.getElementById('confirmOk').onclick = () => finishConfirm(true);
  mask.addEventListener('click', (e) => {
    if (e.target === mask) finishConfirm(false);
  });
}

function askConfirm(msg, sub) {
  return new Promise((resolve) => {
    confirmResolver = resolve;
    document.getElementById('confirmMsg').textContent = msg;
    document.getElementById('confirmSub').textContent = sub || '';
    document.getElementById('confirmMask').classList.add('show');
  });
}

function finishConfirm(ok) {
  document.getElementById('confirmMask').classList.remove('show');
  if (confirmResolver) {
    const r = confirmResolver;
    confirmResolver = null;
    r(ok);
  }
}

function escapeHtml(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;')
    .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
function escapeAttr(s) { return escapeHtml(s).replace(/'/g, '&#39;'); }

function loadingHtml(text) {
  return '<div class="loading"><div class="spinner"></div><span>' + (text || '加载中…') + '</span></div>';
}

function emptyHtml(ico, title, desc, actionHtml) {
  return '<div class="empty"><div class="ico">' + ico + '</div><h3>' + title + '</h3>' +
    (desc ? '<p>' + desc + '</p>' : '') + (actionHtml || '') + '</div>';
}


/* ---------- Homepage twin mood slots (LEFT=ORDERER, RIGHT=CHEF; never copy) ---------- */
const MOOD_EMOJI = {
  '开心':'😄','平静':'😌','想你':'🥰','累了':'😮‍💨','低落':'😢',
  '生气':'😠','想吃':'😋','无聊':'😑','焦虑':'😰','睡意':'😴'
};
function moodEmojiOf(code) {
  return MOOD_EMOJI[code] || '💭';
}
/** Fill one role slot: recorded => emoji only; empty => light · . Never copies other slot. */
function renderMoodTwin(icoEl, moodCode) {
  if (!icoEl) return;
  icoEl.hidden = false;
  icoEl.style.display = '';
  icoEl.style.visibility = 'visible';
  icoEl.removeAttribute('hidden');
  if (moodCode) {
    icoEl.textContent = moodEmojiOf(moodCode);
    icoEl.classList.remove('empty');
    icoEl.classList.add('recorded');
    icoEl.setAttribute('data-empty', '0');
  } else {
    icoEl.textContent = '·';
    icoEl.classList.add('empty');
    icoEl.classList.remove('recorded');
    icoEl.setAttribute('data-empty', '1');
  }
  const twin = icoEl.closest ? icoEl.closest('.mood-twin') : null;
  if (twin) {
    twin.hidden = false;
    twin.style.display = '';
    twin.style.visibility = 'visible';
  }
}
/**
 * Map self/partner API moods into fixed role slots.
 * LEFT = 点单方(ORDERER), RIGHT = 做餐方(CHEF) — not 我/对方.
 * Viewer ORDERER => left=self, right=partner; viewer CHEF => left=partner, right=self.
 */
function renderMoodTwinsHome(selfMood, partnerMood, partnerName) {
  const ordererIco = document.getElementById('moodTwinOrdererIco')
    || document.getElementById('moodTwinSelfIco');
  const chefIco = document.getElementById('moodTwinChefIco')
    || document.getElementById('moodTwinPartnerIco');
  const hint = document.getElementById('moodTwinsHint');
  const card = document.getElementById('moodHomeCard');
  if (!ordererIco && !document.getElementById('moodHomeTitle')) return;

  // Legacy single-chip fallback
  const legacyTitle = document.getElementById('moodHomeTitle');
  if (legacyTitle && !ordererIco) {
    const metaEl = document.getElementById('moodHomeMeta');
    const icoEl = document.getElementById('moodHomeIco');
    const hasAny = !!(selfMood || partnerMood);
    if (!hasAny) {
      if (icoEl) icoEl.textContent = '💭';
      legacyTitle.textContent = '今日心情';
      if (metaEl) metaEl.textContent = '';
    } else {
      const emo = moodEmojiOf(selfMood || partnerMood);
      if (icoEl) icoEl.textContent = emo;
      legacyTitle.textContent = emo;
      if (metaEl) metaEl.textContent = '';
    }
    return;
  }

  const viewerIsChef = (typeof isChef === 'function') ? isChef() : !!(currentUser && (currentUser.role === 'CHEF' || currentUser.role === 'ADMIN'));
  // Fixed by family role, independent of who is logged in:
  const ordererMood = viewerIsChef ? (partnerMood || null) : (selfMood || null);
  const chefMood = viewerIsChef ? (selfMood || null) : (partnerMood || null);

  renderMoodTwin(ordererIco, ordererMood);
  renderMoodTwin(chefIco, chefMood);

  // Hide optional hint / labs if still in DOM
  if (hint) { hint.textContent = ''; hint.hidden = true; }
  ['moodTwinSelfLab','moodTwinPartnerLab','moodTwinOrdererLab','moodTwinChefLab'].forEach(id => {
    const el = document.getElementById(id);
    if (el) { el.textContent = ''; el.hidden = true; }
  });

  const bothEmpty = !ordererMood && !chefMood;
  if (card) {
    const parts = [];
    parts.push(ordererMood ? ('点单方 ' + moodEmojiOf(ordererMood)) : '点单方未记录');
    parts.push(chefMood ? ('做餐方 ' + moodEmojiOf(chefMood)) : '做餐方未记录');
    card.setAttribute('aria-label', parts.join(' · '));
    card.classList.toggle('both-empty', bothEmpty);
    card.classList.toggle('has-any', !bothEmpty);
  }
}
function bindMoodHomeRefresh() {
  if (window.__loveMoodRefreshBound) return;
  window.__loveMoodRefreshBound = true;
  window.addEventListener('pageshow', () => { loadPartnerMoodToday(); });
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') loadPartnerMoodToday();
  });
}
async function loadPartnerMoodToday() {
  const hasTwins = !!(document.getElementById('moodTwinOrdererIco') || document.getElementById('moodTwinSelfIco'));
  const hasLegacy = !!document.getElementById('moodHomeTitle');
  if (!hasTwins && !hasLegacy) return;
  try {
    const uid = getUserId();
    if (!uid) {
      renderMoodTwinsHome(null, null, '对方');
      return;
    }
    const r = await Promise.race([
      api('/api/mood/today'),
      new Promise((_, reject) => setTimeout(() => reject(new Error('mood-timeout')), 8000))
    ]);
    const d = (r && r.code === 200 && r.data) ? r.data : null;
    const partnerName = (d && d.partnerNickname) ? d.partnerNickname : '对方';
    // CRITICAL: self and partner are independent. Never copy one into the other.
    // Mapping to LEFT/RIGHT happens inside renderMoodTwinsHome via viewer role.
    const selfMood = (d && d.self && d.self.mood) ? d.self.mood : null;
    const partnerMood = (d && d.partner && d.partner.mood) ? d.partner.mood : null;
    renderMoodTwinsHome(selfMood, partnerMood, partnerName);
  } catch (e) {
    console.error('loadPartnerMoodToday', e);
    renderMoodTwinsHome(null, null, '对方');
  }
}

/* ---------- Coin ---------- */
async function resolveOrdererUserId() {
  try {
    const r = await api('/api/online/partner');
    const d = r && r.code === 200 ? r.data : null;
    if (!d) return null;
    if (d.userId) return Number(d.userId);
    const p = Array.isArray(d.partners) ? d.partners.find(x => x && x.userId) : null;
    return p ? Number(p.userId) : null;
  } catch (e) { return null; }
}

/* ---------- Homepage nearest anniversary ---------- */
function anniTypeIcon(t) {
  const k = String(t || 'custom').toLowerCase();
  return ({ first_date:'💕', love:'💕', birthday:'🎂', wedding:'💍', custom:'📌' })[k] || '💍';
}
/** Normalize anniversary date from string / LocalDate array / object → YYYY-MM-DD */
function normalizeAnniDate(d) {
  if (d == null || d === '') return '';
  if (Array.isArray(d) && d.length >= 3) {
    const y = d[0], m = String(d[1]).padStart(2, '0'), day = String(d[2]).padStart(2, '0');
    return y + '-' + m + '-' + day;
  }
  if (typeof d === 'object') {
    const y = d.year;
    const m = d.monthValue != null ? d.monthValue : d.month;
    const day = d.dayOfMonth != null ? d.dayOfMonth : d.day;
    if (y != null && m != null && day != null) {
      return y + '-' + String(m).padStart(2, '0') + '-' + String(day).padStart(2, '0');
    }
  }
  const s0 = String(d).trim();
  // already YYYY-MM-DD or ISO datetime
  const m = s0.match(/(\d{4})-(\d{1,2})-(\d{1,2})/);
  if (m) return m[1] + '-' + m[2].padStart(2, '0') + '-' + m[3].padStart(2, '0');
  return s0.slice(0, 10);
}
function pickNearestAnniversary(list) {
  /* Homepage chip: ONLY the next upcoming anniversary (incl. today).
     Never prefer a past occurrence. If this year's date has passed, roll to next year. */
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  let best = null;
  for (const a of list) {
    const dateStr = normalizeAnniDate(a.date || a.anniversaryDate);
    if (!dateStr) continue;
    const parts = dateStr.split('-').map(Number);
    if (parts.length < 3 || !parts[1] || !parts[2]) continue;
    const month = parts[1], day = parts[2];
    let next = new Date(now.getFullYear(), month - 1, day);
    next.setHours(0, 0, 0, 0);
    if (next < now) {
      next = new Date(now.getFullYear() + 1, month - 1, day);
      next.setHours(0, 0, 0, 0);
    }
    const daysUntil = Math.round((next - now) / 86400000);
    if (!best || daysUntil < best.daysUntil) {
      best = {
        title: a.title || a.name || '纪念日',
        type: a.type || 'custom',
        dateStr,
        upcoming: true,
        daysUntil,
        daysAgo: 0,
        delta: daysUntil
      };
    }
  }
  return best;
}
function renderAnniHomeEmpty(titleEl, metaEl, ctaEl, icoEl) {
  if (icoEl) icoEl.textContent = '📅';
  titleEl.textContent = '纪念日';
  if (metaEl) metaEl.textContent = '去添加';
  if (ctaEl) ctaEl.textContent = '';
}
async function loadNearestAnniversary() {
  const titleEl = document.getElementById('anniHomeTitle');
  const metaEl = document.getElementById('anniHomeMeta');
  const icoEl = document.getElementById('anniHomeIco');
  const ctaEl = document.getElementById('anniHomeCta');
  if (!titleEl) return;
  const stillLoading = () => /加载中/.test(titleEl.textContent || '');
  try {
    const uid = getUserId();
    if (!uid) {
      titleEl.textContent = '纪念日';
      if (metaEl) metaEl.textContent = '登录后查看';
      return;
    }
    const r = await Promise.race([
      api('/api/anniversary/list/' + uid),
      new Promise((_, reject) => setTimeout(() => reject(new Error('anni-timeout')), 8000))
    ]);
    const list = (r && r.code === 200 && (r.data && r.data.list || r.data)) || [];
    const arr = Array.isArray(list) ? list : [];
    if (!arr.length) {
      renderAnniHomeEmpty(titleEl, metaEl, ctaEl, icoEl);
      return;
    }
    const best = pickNearestAnniversary(arr);
    if (!best) {
      renderAnniHomeEmpty(titleEl, metaEl, ctaEl, icoEl);
      return;
    }
    if (icoEl) icoEl.textContent = anniTypeIcon(best.type);
    titleEl.textContent = best.title;
    let dayText;
    // Chip must never show past events (N 天前); only upcoming / today
    dayText = best.daysUntil === 0 ? '就是今天 ✨' : ('还有 ' + best.daysUntil + ' 天');
    if (metaEl) metaEl.textContent = dayText;
    if (ctaEl) ctaEl.textContent = '';
  } catch (e) {
    console.error('loadNearestAnniversary', e);
    titleEl.textContent = '纪念日';
    if (metaEl) metaEl.textContent = '点此查看';
    if (ctaEl) ctaEl.textContent = '';
  } finally {
    if (stillLoading()) renderAnniHomeEmpty(titleEl, metaEl, ctaEl, icoEl);
  }
}

async function loadCoin() {
  let uid = getUserId();
  if (!uid) return;
  try {
    if (isChef()) {
      const oid = await resolveOrdererUserId();
      if (oid) uid = oid;
    }
    const r = await api('/api/coin/balance/' + uid);
    if (r.code === 200) {
      document.querySelectorAll('[data-coin]').forEach(el => { el.textContent = r.data; });
    }
  } catch (e) { console.error(e); }
}

/* ---------- Categories & Dishes ---------- */
async function loadCategories() {
  try {
    const r = await api('/api/category/list');
    if (r.code === 200) {
      categories = r.data || [];
      renderCategories();
    }
  } catch (e) { console.error(e); }
}

function renderCategories() {
  const box = document.getElementById('catScroll');
  const items = [{ id: null, name: '全部', icon: '💕' }].concat(categories);
  box.innerHTML = items.map(c => {
    const active = (c.id === null && currentCategoryId === null) || currentCategoryId === c.id;
    return '<button type="button" class="cat-chip' + (active ? ' active' : '') + '"' +
      ' onclick="selectCategory(' + (c.id === null ? 'null' : c.id) + ')">' +
      (c.icon ? escapeHtml(c.icon) + ' ' : '') + escapeHtml(c.name) +
      '</button>';
  }).join('');
}

function selectCategory(id) {
  currentCategoryId = id;
  // Category chips (incl. 全部) reset search keyword so the list is not stuck filtered
  currentKeyword = '';
  const searchInput = document.getElementById('dishSearchInput');
  if (searchInput) searchInput.value = '';
  renderCategories();
  filterAndRender();
}

async function loadDishes() {
  const list = document.getElementById('dishList');
  list.innerHTML = loadingHtml('加载菜品中…');
  try {
    const r = await api('/api/dish/list');
    if (r.code === 200) {
      allDishes = r.data || [];
      await loadFavoriteIds();
      filterAndRender();
    } else {
      list.innerHTML = emptyHtml('😢', '加载失败', r.message || '请稍后重试');
    }
  } catch (e) {
    list.innerHTML = emptyHtml('😢', '网络错误', '请检查服务是否启动');
  }
}

function doSearch(keyword) {
  currentKeyword = (keyword == null ? '' : String(keyword)).trim();
  filterAndRender();
}

function applyDishSearch(keyword) {
  currentKeyword = (keyword || '').trim();
  // clear category so search is global unless user re-picks
  currentCategoryId = null;
  if (typeof renderCategories === 'function') renderCategories();
  filterAndRender();
  switchTab('order');
}

function filterAndRender() {
  let list = allDishes.slice();
  if (currentCategoryId != null) {
    list = list.filter(d => d.categoryId === currentCategoryId);
  }
  if (currentKeyword) {
    const kw = currentKeyword.toLowerCase();
    list = list.filter(d =>
      (d.name && d.name.toLowerCase().includes(kw)) ||
      (d.description && d.description.toLowerCase().includes(kw)) ||
      (d.tags && d.tags.toLowerCase().includes(kw))
    );
  }
  renderDishes(list);
}

function renderDishes(dishes) {
  const box = document.getElementById('dishList');
  if (!dishes.length) {
    box.innerHTML = emptyHtml('🍽️', '暂无菜品', currentKeyword ? '换个关键词试试' : '去后台加一点心意吧');
    return;
  }
  box.innerHTML = dishes.map(d => {
    const tags = [];
    if (d.isSignature) tags.push('<span class="tag sig">拿手</span>');
    if (d.isNew) tags.push('<span class="tag new">新品</span>');
    if (d.isRecommended) tags.push('<span class="tag rec">推荐</span>');
    if (d.tags) {
      String(d.tags).split(/[,，]/).filter(Boolean).slice(0, 3).forEach(t => {
        tags.push('<span class="tag">' + escapeHtml(t.trim()) + '</span>');
      });
    }
    const img = d.imageUrl
      ? '<img class="dish-img" src="' + escapeAttr(d.imageUrl) + '" alt="" loading="lazy" ' +
        'onerror="this.outerHTML=\'<div class=\\\'dish-img placeholder\\\'>🍜</div>\'">'
      : '<div class="dish-img placeholder">🍜</div>';
    const sold = d.isSoldOut ? '<div class="sold-out">已售罄</div>' : '';
    const pt = escapeHtml(d.priceType || '爱心币');
    return (
      '<article class="dish-card">' +
        img +
        '<div class="dish-body">' +
          '<div class="dish-name">' + escapeHtml(d.name) + '</div>' +
          '<div class="dish-desc">' + escapeHtml(d.description || '暂无描述') + '</div>' +
          (tags.length ? '<div class="dish-tags">' + tags.join('') + '</div>' : '') +
          '<div class="dish-foot">' +
            '<div class="dish-price">' + d.price + '<small>' + pt + '</small></div>' +
            '<div class="dish-actions">' +
            '<button type="button" class="dish-fav' + (favoriteIds.has(d.id) ? ' on' : '') +
              '" onclick="toggleFavorite(' + d.id + ')" aria-label="收藏">' + (favoriteIds.has(d.id) ? '♥' : '♡') + '</button>' +
            '<button type="button" class="btn-add" ' + (d.isSoldOut ? 'disabled' : '') +
              ' onclick="addToCart(' + d.id + ')" aria-label="加入购物车">+</button>' +
          '</div>' +
          '</div>' +
        '</div>' +
        sold +
      '</article>'
    );
  }).join('');
}


/* ---------- Favorites ---------- */
let favoriteIds = new Set();
async function loadFavoriteIds() {
  const uid = getUserId();
  if (!uid) { favoriteIds = new Set(); return; }
  try {
    const r = await api('/api/favorite/list/' + uid);
    const list = (r.code === 200 && (r.data?.list || r.data)) || [];
    favoriteIds = new Set((Array.isArray(list) ? list : []).map(d => Number(d.id || d.dishId)).filter(Boolean));
  } catch (e) { favoriteIds = new Set(); }
}
async function toggleFavorite(dishId) {
  dishId = Number(dishId);
  const uid = getUserId();
  if (!uid) { toast('请先登录'); return; }
  const on = favoriteIds.has(dishId);
  try {
    const r = await api(on ? '/api/favorite/remove' : '/api/favorite/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: uid, dishId })
    });
    if (r.code === 200) {
      if (on) favoriteIds.delete(dishId); else favoriteIds.add(dishId);
      toast(on ? '已取消收藏' : '收藏成功');
      filterAndRender();
    } else toast(r.message || '操作失败');
  } catch (e) { toast('网络错误'); }
}
function filteredDishes() {
  // reuse current rendered filter if helpers exist
  if (typeof getFilteredDishes === 'function') return getFilteredDishes();
  return allDishes || [];
}

/* ---------- Cart ---------- */
function addToCart(dishId, qty) {
  const dish = allDishes.find(d => d.id === dishId);
  if (!dish || dish.isSoldOut) return;
  const n = Math.max(1, Math.min(9, Number(qty) || 1));
  const exist = cart.find(i => i.dishId === dishId);
  if (exist) {
    exist.quantity += n;
    toast(dish.name + ' ×' + exist.quantity);
  } else {
    cart.push({
      dishId: dish.id,
      dishName: dish.name,
      price: dish.price,
      priceType: dish.priceType || '爱心币',
      quantity: n,
      remark: ''
    });
    toast('已加入：' + dish.name + (n > 1 ? (' ×' + n) : ''));
  }
  updateCartBadge();
}

function cartCount() {
  return cart.reduce((s, i) => s + i.quantity, 0);
}

function cartTotalsMap() {
  const map = new Map();
  cart.forEach(i => {
    const pt = i.priceType || '爱心币';
    const add = Number(i.price) * Number(i.quantity || 0);
    map.set(pt, (map.get(pt) || 0) + (Number.isFinite(add) ? add : 0));
  });
  return map;
}
function formatTotalsMap(map) {
  if (!map || !map.size) return '0 爱心币';
  const order = ['爱心币', '亲亲', '抱抱'];
  const keys = Array.from(map.keys()).sort((a, b) => {
    const ia = order.indexOf(a);
    const ib = order.indexOf(b);
    return (ia < 0 ? 99 : ia) - (ib < 0 ? 99 : ib);
  });
  const parts = keys.filter(k => map.get(k) > 0).map(k => map.get(k) + ' ' + k);
  return parts.length ? parts.join(' + ') : '0 爱心币';
}
function formatCartTotals() {
  return formatTotalsMap(cartTotalsMap());
}
function formatOrderPrice(o) {
  if (!o) return '0 爱心币';
  if (o.priceSummary) return o.priceSummary;
  const pt = String(o.priceType || '').trim();
  if (/\d/.test(pt) && (pt.includes('+') || pt.includes('币') || pt.includes('亲') || pt.includes('抱'))) {
    return pt.replace(/\+/g, ' + ');
  }
  const total = o.totalPrice ?? o.totalAmount ?? o.amount ?? 0;
  if (pt === '多币种') return '多币种';
  return total + ' ' + (pt || '爱心币');
}
function itemPriceText(it) {
  const remark = String((it && it.remark) || '');
  const matched = remark.match(/\[币种:([^\]]+)\]/);
  const pt = matched ? matched[1] : (it && it.priceType ? it.priceType : '');
  const price = it && it.price != null ? it.price : '';
  return price === '' ? '' : (price + (pt ? (' ' + pt) : ''));
}

function updateCartBadge() {
  persistCart();
  paintCartBadge();
}

function showCart() {
  refreshCartFromStorage();
  renderCart();
  document.getElementById('cartModal').classList.add('show');
}
function hideCart() {
  document.getElementById('cartModal').classList.remove('show');
}
function closeCart(e) {
  if (!e || e.target === e.currentTarget) hideCart();
}

function renderCart() {
  const bd = document.getElementById('cartBody');
  const btn = document.getElementById('submitBtn');
  if (!cart.length) {
    bd.innerHTML = emptyHtml('🛒', '购物车是空的', '去点单页挑几道菜吧');
    btn.disabled = true;
    document.getElementById('cartTotal').textContent = '0 爱心币';
    return;
  }
  btn.disabled = false;
  bd.innerHTML = cart.map((item, idx) => (
    '<div class="cart-row">' +
      '<div class="info">' +
        '<div class="name">' + escapeHtml(item.dishName) + '</div>' +
        '<div class="price">' + item.price + ' ' + escapeHtml(item.priceType) + ' / 份</div>' +
        '<div class="qty-ctrl">' +
          '<button type="button" class="qty-btn" onclick="chgQty(' + idx + ',-1)" aria-label="减少">−</button>' +
          '<span class="qty-num">' + item.quantity + '</span>' +
          '<button type="button" class="qty-btn" onclick="chgQty(' + idx + ',1)" aria-label="增加">+</button>' +
          '<button type="button" class="btn btn-danger" style="margin-left:4px" onclick="delCart(' + idx + ')">删除</button>' +
        '</div>' +
        '<textarea class="cart-remark" rows="1" placeholder="单品备注（少辣 / 多饭…）" ' +
          'onchange="cart[' + idx + '].remark=this.value">' + escapeHtml(item.remark || '') + '</textarea>' +
      '</div>' +
    '</div>'
  )).join('');
  document.getElementById('cartTotal').textContent = formatCartTotals();
}

function chgQty(idx, d) {
  cart[idx].quantity += d;
  if (cart[idx].quantity <= 0) cart.splice(idx, 1);
  updateCartBadge();
  renderCart();
}
function delCart(idx) {
  cart.splice(idx, 1);
  updateCartBadge();
  renderCart();
}

function clearCartNow() {
  cart = [];
  persistCart();
  try { fetch('/api/cart/clear', { method:'POST', credentials:'same-origin' }).catch(function(){}); } catch (e) {}
  paintCartBadge();
  renderCart();
}

async function clearCart() {
  if (!cart.length) return;
  const ok = await askConfirm('确定清空购物车？', '已选菜品将被移除');
  if (!ok) return;
  clearCartNow();
}

async function submitOrder() {
  if (!cart.length) return;
  const uid = getUserId();
  if (!uid) { location.replace('/login'); return; }
  const btn = document.getElementById('submitBtn');
  btn.disabled = true;
  btn.textContent = '提交中…';
  try {
    const orderData = {
      userId: uid,
      items: cart.map(i => ({
        dishId: i.dishId,
        quantity: i.quantity,
        remark: i.remark || ''
      })),
      remark: (document.getElementById('orderRemark') && document.getElementById('orderRemark').value) || ''
    };
    const r = await api('/api/order/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(orderData)
    });
    if (r.code === 200) {
      const summary = r.data && r.data.priceSummary;
      toast(summary ? ('下单成功 ' + summary) : (r.message || '下单成功 💕'));
      cart = [];
      updateCartBadge();
      hideCart();
      const remarkEl = document.getElementById('orderRemark');
      if (remarkEl) remarkEl.value = '';
      if (r.balance !== undefined) {
        document.querySelectorAll('[data-coin]').forEach(el => { el.textContent = r.balance; });
      } else {
        loadCoin();
      }
      setTimeout(() => switchTab('orders'), 400);
    } else {
      toast('下单失败: ' + (r.message || ''));
    }
  } catch (e) {
    if (e.message !== 'unauthorized') toast('下单失败，请重试');
  } finally {
    btn.disabled = cart.length === 0;
    btn.textContent = '提交订单';
  }
}

/* ---------- Surprise ---------- */
async function getSurprise() {
  try {
    toast('正在抽取惊喜…', 1200);
    const r = await api('/api/dish/surprise');
    if (r.code === 200 && r.data) {
      const d = r.data;
      const tip = r.message_tip ? '\n' + r.message_tip : '';
      const ok = await askConfirm(
        '🎁 ' + d.name,
        (d.description || '神秘推荐') + tip
      );
      if (ok) {
        if (!allDishes.find(x => x.id === d.id)) allDishes.push(d);
        addToCart(d.id);
        switchTab('order');
        showCart();
      }
    } else {
      toast(r.message || '获取失败');
    }
  } catch (e) {
    if (e.message !== 'unauthorized') toast('获取惊喜失败');
  }
}

/* ---------- AI one-sentence order (cart only, no checkout) ---------- */
async function aiOneSentenceOrder() {
  const input = document.getElementById('aiOrderInput');
  const btn = document.getElementById('aiOrderBtn');
  const text = (input && input.value || '').trim();
  if (!text) {
    toast('可以说：来份蛋羹 / 搜索清淡 / 清空购物车 / 记一下买花');
    if (input) input.focus();
    return;
  }
  if (btn) btn.disabled = true;
  const candBox = document.getElementById('aiCandidateBox');
  if (candBox) candBox.remove();
  try {
    toast('正在理解你的话…', 1600);
    const r = await api('/api/ai/cart', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text })
    });
    if (r.code !== 200) {
      toast(r.message || 'AI 点单失败');
      return;
    }
    const data = r.data || {};
    const action = data.action || 'add_dishes';

    if (action === 'clear_cart') {
      clearCartNow();
      toast(r.message || '购物车已清空 🧺', 2200);
      if (input) input.value = '';
      return;
    }
    if (action === 'create_note') {
      const note = data.note || {};
      toast(r.message || ('已记入备忘：' + (note.title || '备忘')), 2800);
      if (input) input.value = '';
      return;
    }
    if (action === 'search_dishes') {
      const kw = (data.keyword || data.query || text || '').trim();
      if (!kw) {
        toast('请告诉我要搜索的菜品或标签');
        return;
      }
      applyDishSearch(kw);
      const filtered = (allDishes || []).filter(d => {
        const k = kw.toLowerCase();
        return (d.name && d.name.toLowerCase().includes(k)) ||
          (d.description && d.description.toLowerCase().includes(k)) ||
          (d.tags && d.tags.toLowerCase().includes(k));
      });
      toast(r.message || ('已筛选「' + kw + '」· ' + filtered.length + ' 道'), 2600);
      if (input) input.value = '';
      return;
    }

    const added = Array.isArray(data.added) ? data.added : [];
    const candidates = Array.isArray(data.candidates) ? data.candidates : [];

    function ensureDish(item) {
      if (!allDishes.find(x => x.id === item.dishId)) {
        allDishes.push({
          id: item.dishId,
          name: item.dishName,
          price: item.price,
          priceType: item.priceType || '爱心币',
          isSoldOut: 0
        });
      }
    }

    if (added.length) {
      added.forEach(item => {
        ensureDish(item);
        addToCart(item.dishId, item.quantity || 1);
      });
      toast(r.message || ('已加入：' + added.map(a => a.dishName).join('、')), 2800);
      updateCartBadge();
      syncCartFab();
      if (input) input.value = '';
    } else if (candidates.length) {
      toast(r.message || '说法有点模糊，请点选候选', 2800);
      showAiCandidates(candidates, ensureDish);
    } else {
      toast(r.message || '没有匹配到菜品');
    }
  } catch (e) {
    if (e.message !== 'unauthorized') toast('AI 点单失败，请稍后重试');
  } finally {
    if (btn) btn.disabled = false;
  }
}

function showAiCandidates(candidates, ensureDish) {
  let box = document.getElementById('aiCandidateBox');
  if (box) box.remove();
  box = document.createElement('div');
  box.id = 'aiCandidateBox';
  box.className = 'ai-candidate-list';
  candidates.forEach(c => {
    const b = document.createElement('button');
    b.type = 'button';
    b.textContent = (c.dishName || ('#' + c.dishId)) + (c.quantity > 1 ? (' ×' + c.quantity) : '');
    b.onclick = () => {
      ensureDish(c);
      addToCart(c.dishId, c.quantity || 1);
      updateCartBadge();
    };
    box.appendChild(b);
  });
  const bar = document.getElementById('aiOrderBar');
  if (bar && bar.parentNode) {
    bar.parentNode.insertBefore(box, bar.nextSibling);
  }
}


/* ---------- Orders tab badge (chef unfinished only) ---------- */
const UNFINISHED_STATUSES = ['pending', 'cooking', 'ready'];
function countUnfinishedOrders(orders) {
  if (!Array.isArray(orders)) return 0;
  return orders.filter(o => UNFINISHED_STATUSES.includes(normalizeStatus(o.status))).length;
}
function updateOrdersTabBadge(orders) {
  const el = document.getElementById('ordersTabBadge');
  if (!el) return;
  if (!isChef()) {
    el.hidden = true;
    el.textContent = '';
    return;
  }
  const apply = (list) => {
    const n = countUnfinishedOrders(list);
    if (n > 0) {
      el.hidden = false;
      el.textContent = n > 99 ? '99+' : String(n);
    } else {
      el.hidden = true;
      el.textContent = '';
    }
  };
  if (Array.isArray(orders)) {
    apply(orders);
    return;
  }
  if (Array.isArray(cachedOrders) && cachedOrders.length) {
    apply(cachedOrders);
  }
  // Always refresh from API for chef so badge stays correct on load
  (async () => {
    try {
      const r = await api('/api/order/list');
      if (r.code === 200) {
        cachedOrders = r.data || [];
        apply(cachedOrders);
      } else apply([]);
    } catch (e) { /* ignore */ }
  })();
}

/* ---------- Orders ---------- */
function setKitchenFilter(f) {
  kitchenFilter = f;
  document.querySelectorAll('#kitchenBar .kitchen-chip').forEach(c => {
    c.classList.toggle('active', c.dataset.kfilter === f);
  });
  renderOrders(cachedOrders);
}

async function loadOrders() {
  const box = document.getElementById('orderList');
  const hint = document.getElementById('orderCountHint');
  box.innerHTML = loadingHtml();
  try {
    let r;
    if (isChef()) {
      r = await api('/api/order/list');
    } else {
      r = await api('/api/order/user/' + getUserId());
    }
    if (r.code === 200) {
      cachedOrders = r.data || [];
      if (hint) hint.textContent = cachedOrders.length ? '共 ' + cachedOrders.length + ' 单' : '';
      renderOrders(cachedOrders);
      updateOrdersTabBadge(cachedOrders);
    } else {
      if (hint) hint.textContent = '';
      box.innerHTML = emptyHtml('😢', '加载失败', r.message || '');
    }
  } catch (e) {
    if (hint) hint.textContent = '';
    if (e.message !== 'unauthorized') {
      box.innerHTML = emptyHtml('😢', '网络错误', '请稍后重试');
    }
  }
}

function filteredOrders(orders) {
  if (!isChef() || kitchenFilter === 'all') return orders;
  if (kitchenFilter === 'active') {
    return orders.filter(o => ['pending', 'cooking', 'ready'].includes(normalizeStatus(o.status)));
  }
  return orders.filter(o => normalizeStatus(o.status) === kitchenFilter);
}

function renderOrders(orders) {
  const box = document.getElementById('orderList');
  const list = filteredOrders(orders);
  if (!list.length) {
    box.innerHTML = emptyHtml(
      '📝',
      isChef() ? '暂无订单' : '暂无订单',
      isChef() ? '等点单方下单吧~' : '快去点一份爱意吧~',
      isChef() ? '' : '<button type="button" class="btn btn-primary" style="margin-top:16px" onclick="switchTab(\'order\')">去点单</button>'
    );
    return;
  }
  box.innerHTML = list.map(o => {
    const ns = normalizeStatus(o.status);
    const st = statusLabel(o.status);
    const time = o.createdAt ? new Date(o.createdAt).toLocaleString('zh-CN') : '-';
    let actions = '<button type="button" class="btn btn-ghost" onclick="viewOrder(' + o.id + ')">详情</button>';
    if (!isChef() && ns === 'pending') {
      actions += '<button type="button" class="btn btn-danger" onclick="cancelOrder(\'' + escapeAttr(o.orderNo) + '\')">取消</button>';
    }
    if (isChef() && CHEF_NEXT[ns]) {
      CHEF_NEXT[ns].forEach(n => {
        actions += '<button type="button" class="btn btn-primary" onclick="updateOrderStatus(\'' +
          escapeAttr(o.orderNo) + '\',\'' + n.status + '\')">' + n.label + '</button>';
      });
    }
    const orderNo = o.orderNo || ('#' + o.id);
    return (
      '<div class="order-card">' +
        '<div class="order-top">' +
          '<div>' +
            '<div class="order-no">' + escapeHtml(orderNo) + '</div>' +
            '<div class="order-time">' + time +
              (isChef() && o.userId ? (' · 用户 #' + o.userId) : '') +
            '</div>' +
          '</div>' +
          '<span class="status-pill status-' + escapeAttr(ns) + '">' + st + '</span>' +
        '</div>' +
        '<div class="order-sum">合计 <strong>' + escapeHtml(formatOrderPrice(o)) + '</strong>' +
          (o.remark ? '<div class="order-remark">备注：' + escapeHtml(o.remark) + '</div>' : '') +
        '</div>' +
        '<div class="order-actions">' + actions + '</div>' +
      '</div>'
    );
  }).join('');
}

async function updateOrderStatus(orderNo, status) {
  const label = statusLabel(status);
  const ok = await askConfirm('更新订单状态？', '#' + orderNo + ' → ' + label);
  if (!ok) return;
  try {
    const r = await api('/api/order/status?orderNo=' + encodeURIComponent(orderNo) +
      '&status=' + encodeURIComponent(status), { method: 'PUT' });
    if (r.code === 200) {
      toast(r.message || '已更新为' + label);
      loadOrders();
      updateOrdersTabBadge();
    } else {
      toast(r.message || '更新失败');
    }
  } catch (e) {
    if (e.message !== 'unauthorized') toast('更新失败');
  }
}

async function viewOrder(id) {
  const body = document.getElementById('detailBody');
  const foot = document.getElementById('detailFooter');
  body.innerHTML = loadingHtml();
  foot.innerHTML = '';
  document.getElementById('detailModal').classList.add('show');
  try {
    const r = await api('/api/order/detail/' + id);
    if (r.code === 200 && r.data) {
      // detail API returns { order, items } — not a flat Order
      const payload = r.data;
      const o = payload.order || payload;
      const ns = normalizeStatus(o.status);
      const st = statusLabel(o.status) || '未知';
      const time = o.createdAt ? new Date(o.createdAt).toLocaleString('zh-CN') : '-';
      const list = payload.items || o.items || o.orderItems || [];
      let itemsHtml = '';
      if (list.length) {
        itemsHtml = '<div class="detail-items">' +
          list.map(it =>
            '<div class="detail-item">' +
              '<span class="di-name">' + escapeHtml(it.dishName || it.name || '菜品') +
                (it.remark ? ' <small style="color:var(--muted)">(' + escapeHtml(String(it.remark).replace(/\s*\[币种:[^\]]+\]/, '')) + ')</small>' : '') +
              '</span>' +
              '<span class="di-qty">×' + (it.quantity || 1) +
                (itemPriceText(it) ? (' · ' + escapeHtml(itemPriceText(it))) : '') +
              '</span>' +
            '</div>'
          ).join('') +
        '</div>';
      }
      const finishRaw = o.completedAt || o.finishTime || o.finishedAt ||
        (ns === 'completed' ? o.updatedAt : null);
      const finishTime = finishRaw ? new Date(finishRaw).toLocaleString('zh-CN') : '';
      body.innerHTML =
        '<div class="detail-row"><span class="k">订单号</span><span class="v">' + escapeHtml(o.orderNo || ('#' + (o.id || ''))) + '</span></div>' +
        '<div class="detail-row"><span class="k">状态</span><span class="v"><span class="status-pill status-' +
          escapeAttr(ns) + '">' + st + '</span></span></div>' +
        '<div class="detail-row"><span class="k">下单时间</span><span class="v">' + time + '</span></div>' +
        (finishTime ? '<div class="detail-row"><span class="k">完成时间</span><span class="v">' + finishTime + '</span></div>' : '') +
        '<div class="detail-row"><span class="k">合计</span><span class="v" style="color:var(--rose);font-weight:750">' +
          escapeHtml(formatOrderPrice(o)) + '</span></div>' +
        (o.remark ? '<div class="detail-row"><span class="k">备注</span><span class="v">' + escapeHtml(o.remark) + '</span></div>' : '') +
        itemsHtml;
      let footBtns = '';
      if (!isChef() && ns === 'pending') {
        footBtns += '<button type="button" class="btn btn-danger" onclick="cancelOrder(\'' +
          escapeAttr(o.orderNo) + '\');hideDetail()">取消订单</button>';
      }
      if (isChef() && CHEF_NEXT[ns]) {
        CHEF_NEXT[ns].forEach(n => {
          footBtns += '<button type="button" class="btn btn-primary" onclick="updateOrderStatus(\'' +
            escapeAttr(o.orderNo) + '\',\'' + n.status + '\');hideDetail()">' + n.label + '</button>';
        });
      }
      foot.innerHTML = footBtns;
    } else {
      body.innerHTML = emptyHtml('😢', '获取失败', r.message || '');
    }
  } catch (e) {
    if (e.message !== 'unauthorized') body.innerHTML = emptyHtml('😢', '获取详情失败', '');
  }
}

function hideDetail() {
  document.getElementById('detailModal').classList.remove('show');
}
function closeDetail(e) {
  if (!e || e.target === e.currentTarget) hideDetail();
}

async function cancelOrder(orderNo) {
  const ok = await askConfirm('取消订单？', '订单 ' + orderNo + ' 将被取消');
  if (!ok) return;
  try {
    const r = await api('/api/order/cancel', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ orderNo: orderNo, userId: getUserId() })
    });
    if (r.code === 200) {
      toast(r.message || '已取消');
      if (r.balance !== undefined) {
        document.querySelectorAll('[data-coin]').forEach(el => { el.textContent = r.balance; });
      } else {
        loadCoin();
      }
      loadOrders();
      updateOrdersTabBadge();
    } else {
      toast(r.message || '取消失败');
    }
  } catch (e) {
    if (e.message !== 'unauthorized') toast('取消失败');
  }
}


/* ---------- Online heartbeat / partner status ---------- */
function markOfflineBeacon() {
  try {
    if (navigator.sendBeacon) {
      navigator.sendBeacon('/api/online/offline');
    } else {
      fetch('/api/online/offline', { method: 'POST', credentials: 'same-origin', keepalive: true }).catch(() => {});
    }
  } catch (e) {}
}
function bindOfflineBeacon() {
  if (window.__loveOfflineBound) return;
  window.__loveOfflineBound = true;
  window.addEventListener('pagehide', markOfflineBeacon);
}

/* ---------- Family location silent report (foreground only) ---------- */
let __locReporting = false;
let __locLastAt = 0;
function reportMyLocation(opts) {
  opts = opts || {};
  const force = !!opts.force;
  const minInterval = opts.minIntervalMs != null ? opts.minIntervalMs : 60000;
  const now = Date.now();
  if (!force && __locLastAt && (now - __locLastAt) < minInterval) return Promise.resolve(null);
  if (__locReporting && !force) return Promise.resolve(null);
  __locReporting = true;
  return (async () => {
    try {
      let coords = null;
      if (window.LovePage && typeof LovePage.acquireCoords === 'function') {
        const p = await LovePage.acquireCoords({ force: force, timeout: force ? 10000 : 8000 });
        if (p && p.ok) coords = p;
      } else if (window.LoveNative && typeof LoveNative.getLocation === 'function') {
        coords = await new Promise((resolve) => {
          const id = 'h' + Date.now();
          if (!window.__loveNativeLocCallbacks) {
            window.__loveNativeLocCallbacks = {};
            window.__loveNativeLocResult = function (reqId, jsonStr) {
              const cb = window.__loveNativeLocCallbacks[reqId];
              delete window.__loveNativeLocCallbacks[reqId];
              if (!cb) return;
              try { cb(typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr); } catch (e) { cb(null); }
            };
          }
          const timer = setTimeout(() => { delete window.__loveNativeLocCallbacks[id]; resolve(null); }, 12000);
          window.__loveNativeLocCallbacks[id] = function (data) {
            clearTimeout(timer);
            if (data && data.ok) resolve({ ok: true, lat: Number(data.lat), lng: Number(data.lng), accuracy: data.accuracy != null ? Number(data.accuracy) : null });
            else resolve(null);
          };
          try { window.LoveNative.getLocation(id, true, 10000); } catch (e) { clearTimeout(timer); resolve(null); }
        });
      } else if (navigator.geolocation) {
        coords = await new Promise((resolve) => {
          navigator.geolocation.getCurrentPosition(
            (pos) => resolve({ ok: true, lat: pos.coords.latitude, lng: pos.coords.longitude, accuracy: pos.coords.accuracy }),
            () => resolve(null),
            { enableHighAccuracy: true, timeout: force ? 10000 : 8000, maximumAge: force ? 0 : 30000 }
          );
        });
      }
      if (!coords || !coords.ok) return null;
      const r = await api('/api/family-location/report', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ lat: coords.lat, lng: coords.lng, accuracy: coords.accuracy })
      });
      if (r && r.code === 200) {
        __locLastAt = Date.now();
        return { lat: coords.lat, lng: coords.lng, accuracy: coords.accuracy };
      }
      return null;
    } catch (e) {
      return null;
    } finally {
      __locReporting = false;
    }
  })();
}
function bindLocationReport() {
  if (window.__loveLocBoundHome) return;
  window.__loveLocBoundHome = true;
  const kick = () => { try { reportMyLocation({ force: false }); } catch (e) {} };
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') kick();
  });
  window.addEventListener('pageshow', () => kick());
}
function startHeartbeat() {
  stopHeartbeat();
  bindOfflineBeacon();
  pingHeartbeat();
  heartbeatTimer = setInterval(pingHeartbeat, HEARTBEAT_MS);
}
function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
}
async function pingHeartbeat() {
  try {
    await fetch('/api/online/heartbeat', { method: 'POST', credentials: 'same-origin' });
  } catch (e) {
    try { await fetch('/api/online/record', { method: 'POST', credentials: 'same-origin' }); } catch (e2) {}
  }
}
async function loadPartnerOnline() {
  const nameEl = document.getElementById('partnerOnlineName');
  const metaEl = document.getElementById('partnerOnlineMeta');
  const dotEl = document.getElementById('partnerOnlineDot');
  if (!nameEl || !isChef()) return;
  try {
    const r = await api('/api/online/partner');
    const d = (r && r.code === 200 && r.data) ? r.data : null;
    if (!d) {
      nameEl.textContent = '点单方';
      if (metaEl) metaEl.textContent = '暂无状态';
      if (dotEl) dotEl.style.background = '#bbb';
      return;
    }
    const online = !!d.online;
    nameEl.textContent = d.nickname || '点单方';
    if (metaEl) metaEl.textContent = online ? '在线' : ('离线 · ' + (d.lastSeenText || d.lastSeen || '暂无记录'));
    if (dotEl) {
      dotEl.style.background = online ? 'var(--ok, #22c55e)' : '#bbb';
      dotEl.title = online ? '在线' : '离线';
    }
  } catch (e) {
    if (metaEl) metaEl.textContent = '状态获取失败';
  }
}
