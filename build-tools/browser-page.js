// Installed in the official game frame before SWF capture. No credentials logged.
(function () {
  if (window.__naBrowserCapture) return;
  var nonce = String(Date.now()) + ':' + Math.random().toString(36).slice(2);
  var object = null;
  function safeName(name) { return /^[A-Za-z_][A-Za-z0-9_]{0,63}$/.test(name) && !/^(?:constructor|prototype|__proto__)$/.test(name); }
  function receive(d) {
    if (!d || d.type !== 'naruto-air-browser' || d.nonce !== nonce) return false;
    if (!safeName(d.name)) return false;
    if (d.action === 'callback' && object) {
      if (d.remove) { try { delete object[d.name]; } catch (_) {} return true; }
      object[d.name] = function () {
        var json = JSON.stringify({ name: d.name, args: Array.prototype.slice.call(arguments) });
        if (json.length <= 65536 && window.NarutoAirNative && window.NarutoAirNative.browserCallback)
          window.NarutoAirNative.browserCallback(nonce, json);
        return Object.prototype.hasOwnProperty.call(d, 'returnValue') ? d.returnValue : null;
      };
      return true;
    } else if (d.action === 'call' && typeof window[d.name] === 'function') {
      window[d.name].apply(window, d.args || []);
      return true;
    }
    return false;
  }
  window.__naBrowserDispatch = receive;
  window.addEventListener('message', function (e) {
    if (e.source !== parent || !/^https:\/\/naruto\.narutowebgame\.com$/.test(e.origin)) return;
    receive(e.data);
  });
  window.__naBrowserCapture = function (owner) {
    object = owner;
    var uin = null, skey = null, rechargeUid = "";
    try { rechargeUid = new URL(location.href).searchParams.get("user_name") || ""; } catch (_) {}
    try { if (typeof window.getUin === 'function') uin = window.getUin(); } catch (_) {}
    try { if (typeof window.getCookie === 'function') skey = window.getCookie('skey'); } catch (_) {}
    // Enable browser mode only with the real identity returned by the logged-in page.
    var ready = /^\d{1,10}$/.test(String(uin)) && Number(uin) > 0 &&
      Number(uin) <= 4294967295 && typeof skey === 'string' && skey.length > 0 && skey.length <= 16384 &&
      !!(window.NarutoAirNative && window.NarutoAirNative.browserCallback);
    return { rechargeUid: ready ? rechargeUid : "", ready: ready, uin: ready ? String(uin) : '', nonce: nonce,
      sessionCookies: ready ? { uin: uin, skey: skey } : null,
      origin: location.origin, userAgent: navigator.userAgent, objectID: owner && owner.id || '' };
  };
})();
