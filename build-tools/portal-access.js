// Classifies only the visible official portal. No credentials, network requests or page changes.
(function () {
  try {
    if (window.top !== window || location.protocol !== 'https:' ||
        !/^(?:naruto|gamebox3)\.narutowebgame\.com$/.test(location.hostname) || document.readyState !== 'complete') return 'unknown';
    var text = String(document.body && document.body.textContent || '').slice(0, 16000).toLowerCase().replace(/\s+/g, ' ');
    var cloudflare = !!document.getElementById('cf-error-details') || text.indexOf('cloudflare') >= 0;
    if (cloudflare && /you have been blocked|você foi bloqueado|voce foi bloqueado/.test(text)) return 'blocked';
    if (document.getElementById('challenge-running') || document.getElementById('cf-chl-widget') ||
        document.getElementById('challenge-stage') || /^just a moment/i.test(document.title || '')) return 'challenge';
    if (cloudflare && document.getElementById('cf-error-details')) return 'unknown';
    return 'clear';
  } catch (_) { return 'unknown'; }
})();
