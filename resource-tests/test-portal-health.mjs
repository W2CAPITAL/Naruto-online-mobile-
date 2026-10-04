import fs from 'node:fs';import vm from 'node:vm';import assert from 'node:assert/strict';
const src=fs.readFileSync(new URL('../bridge-src/br/davi/narutoair/portal/PortalHealth.java',import.meta.url),'utf8');
const script=JSON.parse('"'+src.match(/SCRIPT="((?:\\.|[^"\\])*)";/)[1]+'"');
function fixture(host='gamebox3.narutowebgame.com',path='/gamebox/2.4.1/template/login.php',ready='complete',sheets=[null,null]) {
 const f={window:{},location:{hostname:host,pathname:path,replace:u=>f.redirect=u},document:{readyState:ready,querySelectorAll:()=>sheets.map(sheet=>({sheet}))}};vm.createContext(f);return f;
}
const broken=fixture();vm.runInContext(script,broken);assert.equal(broken.redirect,undefined);vm.runInContext(script,broken);assert.equal(broken.redirect,'https://naruto.narutowebgame.com/pt/serverlist/');
for(const f of [fixture('evil.test'),fixture('gamebox3.narutowebgame.com','/gamebox/2.4.1/template/game.php'),fixture(undefined,undefined,'loading'),fixture(undefined,undefined,undefined,[{},null]),fixture(undefined,undefined,undefined,[])]) {vm.runInContext(script,f);vm.runInContext(script,f);assert.equal(f.redirect,undefined);}
const recovers=fixture();vm.runInContext(script,recovers);recovers.document.querySelectorAll=()=>[{sheet:{}},{sheet:null}];vm.runInContext(script,recovers);assert.equal(recovers.window.__naMissingStyles,0);
console.log('PASS: production portal health JS: two completed unstyled login checks, exact origin/path, healthy/loading/partial styles preserved, recovery resets counter.');
