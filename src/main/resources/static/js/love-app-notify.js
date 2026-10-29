/* Love App notifications DISABLED — system status-bar / poll path off. WeChat templates remain server-side. */
(function (global) {
  const LoveAppNotify = {
    _timer: null,
    isNativeApp: function () { return false; },
    start: function () { /* no-op: phone system notifications disabled */ },
    stop: function () {
      if (this._timer) { clearInterval(this._timer); this._timer = null; }
    },
    register: async function () {},
    poll: async function () {},
    showNative: function () { return false; },
    requestNativePermission: function () {}
  };
  global.LoveAppNotify = LoveAppNotify;
  global.openAppNotifications = function () {};
})(window);