// Recovery for the official HTML login only; never retries AIR or modifies credentials.
(function () {
  if (typeof document !== 'object' || typeof location !== 'object') return;
  var page;
  try { page = new URL(location.href); } catch (_) { return; }
  if (!/^https?:$/.test(page.protocol) || page.hostname !== 'naruto-pt.oasgames.com' || page.pathname !== '/main.html') return;
  if (window.__naLoginRecovery) return;
  var key = 'na-login-retry-v1:' + (page.searchParams.get('server_id') || page.searchParams.get('zoneid') || page.searchParams.get('zone_id') || 'game');
  var lastPhase = '', changedAt = Date.now(), timer = null, panel = null, finished = false;
  var saved = null;
  try { saved = window.sessionStorage; saved.getItem(key); } catch (_) { saved = null; }
  function stop() {
    if (finished) return;
    finished = true;
    if (timer !== null) window.clearInterval(timer);
    timer = null;
    if (panel && panel.parentNode) panel.parentNode.removeChild(panel);
    panel = null;
  }
  function handedOff() {
    var captured = window.__naSafeCaptured || {};
    return Object.keys(captured).some(function (url) {
      try { var u = new URL(url); return u.hostname === 'cdnnaruto-pt.oasgames.com' && /^\/PT_Naruto[^/]+\/entry\.swf$/i.test(u.pathname); }
      catch (_) { return false; }
    });
  }
  function reload() { stop(); location.reload(); }
  function show() {
    if (panel || !document.body) return;
    panel = document.createElement('div');
    panel.id = 'na-login-recovery';
    panel.setAttribute('role', 'alert');
    panel.style.cssText = 'position:fixed;left:12px;right:12px;bottom:12px;z-index:2147483647;padding:12px;background:#15202d;color:white;border-radius:8px;font:16px sans-serif;box-sizing:border-box;';
    var label = document.createElement('div');
    label.textContent = 'A entrada no jogo ficou sem resposta. Sua conta foi mantida.';
    panel.appendChild(label);
    function button(text, action) {
      var b = document.createElement('button');
      b.type = 'button'; b.textContent = text;
      b.style.cssText = 'margin:10px 10px 0 0;padding:10px 14px;font:16px sans-serif;';
      b.onclick = action; panel.appendChild(b);
    }
    button('TENTAR NOVAMENTE', reload);
    button('VOLTAR AOS SERVIDORES', function () {
      stop(); window.top.location.href = 'https://naruto.narutowebgame.com/pt/serverlist/';
    });
    document.body.appendChild(panel);
  }
  function tick() {
    if (finished) return;
    if (handedOff()) { stop(); return; }
    if (document.hidden || document.readyState !== 'complete') { changedAt = Date.now(); return; }
    var status = document.getElementById('httpStatus');
    var phase = status ? String(status.textContent || '').trim() : '';
    if (phase !== lastPhase) {
      lastPhase = phase; changedAt = Date.now();
      if (panel && panel.parentNode) panel.parentNode.removeChild(panel);
      panel = null; return;
    }
    if (Date.now() - changedAt < 45000) return;
    // Persist before reloading. Storage failure disables automatic retry, avoiding loops.
    var retry = false;
    if (saved) try { if (saved.getItem(key) !== '1') { saved.setItem(key, '1'); retry = saved.getItem(key) === '1'; } } catch (_) {}
    if (retry) reload(); else show();
  }
  window.__naLoginRecovery = { stop: stop };
  document.addEventListener('visibilitychange', function () { changedAt = Date.now(); });
  window.addEventListener('pagehide', stop);
  timer = window.setInterval(tick, 2000);
})();
