/* Love H5 — shared subpage chrome + auth */
(function (global) {
  const LovePage = {
    currentUser: null,
    _ready: null,

    api: async function (url, opts) {
      const options = Object.assign({ credentials: 'same-origin' }, opts || {});
      const res = await fetch(url, options);
      const data = await res.json().catch(() => ({ code: res.status, message: '请求失败' }));
      if (data.code === 401 && !String(url).includes('/api/auth/')) {
        location.replace('/login');
        throw new Error('unauthorized');
      }
      return data;
    },

    toast: function (msg, ms) {
      let el = document.getElementById('toast');
      if (!el) {
        el = document.createElement('div');
        el.id = 'toast';
        el.className = 'toast';
        el.setAttribute('role', 'status');
        document.body.appendChild(el);
      }
      el.textContent = msg;
      el.classList.add('show');
      clearTimeout(el._t);
      el._t = setTimeout(() => el.classList.remove('show'), ms || 2200);
    },

    normalizeUser: function (u) {
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
    },

    persistUser: function (u) {
      localStorage.setItem('love_user', JSON.stringify(u));
      localStorage.setItem('love_user_id', String(u.id));
    },

    getCurrentUser: function () {
      return this.currentUser;
    },

    getUserId: function () {
      if (this.currentUser && this.currentUser.id != null) return Number(this.currentUser.id);
      const cached = localStorage.getItem('love_user_id');
      return cached ? Number(cached) : null;
    },

    isChef: function () {
      const u = this.currentUser;
      return !!(u && (u.role === 'CHEF' || u.role === 'ADMIN'));
    },

    formatOrderPrice: function (o) {
      if (!o) return '0 爱心币';
      if (o.priceSummary) return o.priceSummary;
      const pt = String(o.priceType || '').trim();
      if (/\d/.test(pt) && (pt.includes('+') || pt.includes('币') || pt.includes('亲') || pt.includes('抱'))) {
        return pt.replace(/\+/g, ' + ');
      }
      const total = o.totalPrice ?? o.totalAmount ?? o.amount ?? 0;
      if (pt === '多币种') return '多币种';
      return total + ' ' + (pt || '爱心币');
    },

    back: function (fallback) {
      // Use window.history so page scripts cannot shadow History API
      // (ai-chat previously used let history = [] for chat messages).
      const fb = fallback || '/';
      try {
        if (window.history.length > 1) {
          window.history.back();
          return;
        }
      } catch (e) {}
      location.href = fb;
    },

    requireAuth: async function () {
      try {
        const r = await this.api('/api/auth/me');
        if (r.code === 200 && r.data) {
          this.currentUser = this.normalizeUser(r.data);
          this.persistUser(this.currentUser);
          document.body.classList.toggle('is-chef', this.isChef());
          try { LovePage.startHeartbeat(); } catch (e) {}
          try { LovePage.bindLocationReport(); LovePage.reportLocation({ force: false }); } catch (e) {}
          return this.currentUser;
        }
      } catch (e) {}
      location.replace('/login');
      return null;
    },

    requireChef: async function () {
      const u = await this.requireAuth();
      if (!u) return null;
      if (!this.isChef()) {
        this.toast('仅做餐方可访问');
        setTimeout(() => { location.replace('/?denied=chef'); }, 600);
        return null;
      }
      return u;
    },

    /** Boot helper: await LovePage.ready({ chefOnly: true }) */
    markOfflineBeacon: function () {
      try {
        if (navigator.sendBeacon) navigator.sendBeacon('/api/online/offline');
        else fetch('/api/online/offline', { method: 'POST', credentials: 'same-origin', keepalive: true }).catch(() => {});
      } catch (e) {}
    },
    bindOfflineBeacon: function () {
      if (window.__loveOfflineBound) return;
      window.__loveOfflineBound = true;
      window.addEventListener('pagehide', () => LovePage.markOfflineBeacon());
    },
    startHeartbeat: function () {
      if (this._hbTimer) clearInterval(this._hbTimer);
      this.bindOfflineBeacon();
      const ping = () => {
        fetch('/api/online/heartbeat', { method: 'POST', credentials: 'same-origin' }).catch(() => {
          fetch('/api/online/record', { method: 'POST', credentials: 'same-origin' }).catch(() => {});
        });
      };
      ping();
      this._hbTimer = setInterval(ping, 30000);
    },
    stopHeartbeat: function () {
      if (this._hbTimer) { clearInterval(this._hbTimer); this._hbTimer = null; }
    },
    /** CHEF：解析同家庭点单方 userId（家庭 partner 优先） */
    resolveOrdererUserId: async function () {
      try {
        const r = await this.api('/api/online/partner');
        const d = r && r.code === 200 ? r.data : null;
        if (d && d.userId) return Number(d.userId);
        const p = d && Array.isArray(d.partners) ? d.partners.find(x => x && x.userId) : null;
        if (p) return Number(p.userId);
      } catch (e) {}
      try {
        const r = await this.api('/api/online/family');
        const list = (r && r.data && r.data.orderers) || [];
        if (list[0] && list[0].userId) return Number(list[0].userId);
      } catch (e) {}
      return null;
    },
  loadPartnerOnline: async function () {
    const nameEl = document.getElementById('partnerOnlineName');
    const metaEl = document.getElementById('partnerOnlineMeta');
    const dotEl = document.getElementById('partnerOnlineDot');
    if (!nameEl) return;
    try {
      const r = await this.api('/api/online/partner');
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
      if (dotEl) dotEl.style.background = online ? 'var(--ok, #22c55e)' : '#bbb';
    } catch (e) {
      if (metaEl) metaEl.textContent = '状态获取失败';
    }
  },
  ready: function (opts) {
      opts = opts || {};
      document.body.classList.add('love-sub');
      if (this._ready) return this._ready;
      this._ready = (async () => {
        const u = opts.chefOnly ? await this.requireChef() : await this.requireAuth();
        return u;
      })();
      return this._ready;
    },


    /** Geolocation silent report (no background tracking). Prefer native LoveNative bridge in App. */
    _locReporting: false,
    _locLastAt: 0,

    hasNativeGeo: function () {
      try {
        return !!(window.LoveNative && typeof window.LoveNative.getLocation === 'function');
      } catch (e) { return false; }
    },

    /** Resolve {lat,lng,accuracy,status?} via native bridge or browser geolocation. */
    acquireCoords: function (opts) {
      opts = opts || {};
      const force = !!opts.force;
      // Fast UX: ~8–12s, never wait ~28s by default
      const timeout = opts.timeout != null ? opts.timeout : (force ? 10000 : 8000);
      const self = this;
      const onRefine = typeof opts.onRefine === 'function' ? opts.onRefine : null;
      if (self.hasNativeGeo()) {
        return new Promise((resolve) => {
          const id = 'n' + Date.now() + '_' + Math.floor(Math.random() * 1e6);
          if (!window.__loveNativeLocCallbacks) {
            window.__loveNativeLocCallbacks = {};
            window.__loveNativeLocRefineCbs = window.__loveNativeLocRefineCbs || {};
            window.__loveNativeLocResult = function (reqId, jsonStr) {
              try {
                const cb = window.__loveNativeLocCallbacks[reqId];
                delete window.__loveNativeLocCallbacks[reqId];
                if (!cb) return;
                let data = null;
                try { data = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr; } catch (e) {}
                cb(data);
              } catch (e) {}
            };
            window.__loveNativeLocRefine = function (reqId, jsonStr) {
              try {
                const cb = window.__loveNativeLocRefineCbs && window.__loveNativeLocRefineCbs[reqId];
                if (!cb) return;
                let data = null;
                try { data = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr; } catch (e) {}
                cb(data);
              } catch (e) {}
            };
          }
          const timer = setTimeout(() => {
            if (!window.__loveNativeLocCallbacks[id]) return;
            delete window.__loveNativeLocCallbacks[id];
            try { if (window.__loveNativeLocRefineCbs) delete window.__loveNativeLocRefineCbs[id]; } catch (e) {}
            resolve({ ok: false, reason: 'timeout', source: 'native' });
          }, timeout + 1500);
          window.__loveNativeLocCallbacks[id] = function (data) {
            clearTimeout(timer);
            if (data && data.ok) {
              resolve({
                ok: true,
                lat: Number(data.lat),
                lng: Number(data.lng),
                accuracy: data.accuracy != null ? Number(data.accuracy) : null,
                source: 'native',
                provider: data.provider || null,
                status: data.status || null
              });
            } else {
              resolve({ ok: false, reason: (data && data.reason) || 'error', source: 'native', status: data && data.status });
            }
          };
          if (onRefine) {
            window.__loveNativeLocRefineCbs = window.__loveNativeLocRefineCbs || {};
            window.__loveNativeLocRefineCbs[id] = function (data) {
              try {
                if (!data || !data.ok) return;
                onRefine({
                  ok: true,
                  lat: Number(data.lat),
                  lng: Number(data.lng),
                  accuracy: data.accuracy != null ? Number(data.accuracy) : null,
                  source: 'native',
                  provider: data.provider || null,
                  status: data.status || 'refined'
                });
              } catch (e) {}
            };
            // cleanup refine cb after max refine window
            setTimeout(() => {
              try { if (window.__loveNativeLocRefineCbs) delete window.__loveNativeLocRefineCbs[id]; } catch (e) {}
            }, timeout + 8000);
          }
          try {
            window.LoveNative.getLocation(id, true, timeout);
          } catch (e) {
            clearTimeout(timer);
            delete window.__loveNativeLocCallbacks[id];
            resolve({ ok: false, reason: 'error', source: 'native' });
          }
        });
      }
      // Browser / WebView geolocation fallback
      return new Promise((resolve) => {
        if (!navigator.geolocation) {
          resolve({ ok: false, reason: 'unsupported' });
          return;
        }
        navigator.geolocation.getCurrentPosition(
          (pos) => resolve({
            ok: true,
            lat: pos.coords.latitude,
            lng: pos.coords.longitude,
            accuracy: pos.coords.accuracy,
            source: 'browser'
          }),
          (err) => {
            let reason = 'error';
            const code = err && err.code != null ? Number(err.code) : 0;
            if (code === 1) reason = 'denied';
            else if (code === 2) reason = 'unavailable';
            else if (code === 3) reason = 'timeout';
            const insecure = (location.protocol === 'http:' && location.hostname !== 'localhost' && location.hostname !== '127.0.0.1');
            resolve({ ok: false, reason, code, insecure: !!insecure, message: err && err.message, source: 'browser' });
          },
          { enableHighAccuracy: true, timeout: timeout, maximumAge: force ? 0 : 30000 }
        );
      });
    },

    reportLocation: function (opts) {
      opts = opts || {};
      const self = this;
      const force = !!opts.force;
      const minInterval = opts.minIntervalMs != null ? opts.minIntervalMs : 60000;
      const now = Date.now();
      // Return shape: { ok, reason?, lat?, lng?, accuracy?, changed? }
      if (!force && self._locLastAt && (now - self._locLastAt) < minInterval) {
        return Promise.resolve({ ok: false, reason: 'skipped' });
      }
      if (self._locReporting && !force) return Promise.resolve({ ok: false, reason: 'busy' });
      self._locReporting = true;
      return (async () => {
        try {
          // Skip browser geolocation unsupported check when native bridge exists
          if (!self.hasNativeGeo() && !navigator.geolocation) {
            if (opts.toastOnError) self.toast('当前环境不支持定位');
            return { ok: false, reason: 'unsupported' };
          }
          const timeout = opts.timeoutMs != null ? opts.timeoutMs : (force ? 10000 : 8000);
          const reportOnce = async (coords) => {
            const r = await self.api('/api/family-location/report', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                lat: coords.lat,
                lng: coords.lng,
                accuracy: coords.accuracy,
                status: coords.status || null,
                provider: coords.provider || null
              })
            });
            return r;
          };
          const pos = await self.acquireCoords({
            force: force,
            timeout: timeout,
            onRefine: async (refined) => {
              try {
                const rr = await reportOnce(refined);
                if (rr && rr.code === 200) self._locLastAt = Date.now();
              } catch (e) {}
            }
          });
          if (!pos || !pos.ok) {
            if (opts.toastOnError) {
              const r = pos && pos.reason;
              let msg = '定位失败';
              if (r === 'denied') msg = '定位被拒绝（请开系统位置权限；App 请装含原生定位的新版）';
              else if (r === 'timeout') msg = '定位超时（请到开阔处重试）';
              else if (r === 'unavailable') msg = '定位不可用（请打开手机定位服务）';
              self.toast(msg);
            }
            return Object.assign({ ok: false }, pos || { reason: 'error' });
          }
          const lat = pos.lat;
          const lng = pos.lng;
          const accuracy = pos.accuracy;
          const status = pos.status || null;
          const r = await reportOnce(pos);
          if (r && r.code === 200) {
            self._locLastAt = Date.now();
            const changed = r.changed !== false;
            const locStatus = r.locStatus || status || null;
            // Prefer server-kept coords when coarse was rejected
            const outLat = (r.data && r.data.lat != null) ? Number(r.data.lat) : lat;
            const outLng = (r.data && r.data.lng != null) ? Number(r.data.lng) : lng;
            const outAcc = (r.data && r.data.accuracy != null) ? Number(r.data.accuracy) : accuracy;
            return {
              ok: true,
              lat: outLat,
              lng: outLng,
              accuracy: outAcc,
              changed: !!changed,
              source: pos.source || 'browser',
              status: locStatus,
              provider: pos.provider || null
            };
          }
          if (opts.toastOnError) self.toast((r && r.message) || '位置上报失败');
          return { ok: false, reason: 'http' };
        } catch (e) {
          if (opts.toastOnError) self.toast('位置上报失败');
          return { ok: false, reason: 'error' };
        } finally {
          self._locReporting = false;
        }
      })();
    },

    bindLocationReport: function () {
      if (window.__loveLocBound) return;
      window.__loveLocBound = true;
      const kick = () => {
        try { LovePage.reportLocation({ force: false }); } catch (e) {}
      };
      document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') kick();
      });
      window.addEventListener('pageshow', (ev) => { kick(); });
    },
    escapeHtml: function (s) {
      return String(s == null ? '' : s)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }
  };

  global.LovePage = LovePage;
  // aliases used by older page scripts
  global.showNotification = function (msg) { LovePage.toast(msg); };
  global.toast = function (msg, ms) { LovePage.toast(msg, ms); };
})(window);
