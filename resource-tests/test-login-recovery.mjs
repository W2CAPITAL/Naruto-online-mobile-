import fs from 'node:fs';import vm from 'node:vm';import assert from 'node:assert/strict';
const script=fs.readFileSync(new URL('../build-tools/login-recovery.js',import.meta.url),'utf8');
const java=fs.readFileSync(new URL('../bridge-src/br/davi/narutoair/portal/PortalLoginRecovery.java',import.meta.url),'utf8');
assert.equal(JSON.parse(java.match(/SCRIPT=("(?:\\.|[^"\\])*");/)[1]),script,'native fallback and early HTML payload must match');
function fixture(store=new Map(),url='https://naruto-pt.oasgames.com/main.html?server_id=877'){
 let now=0,tick;const listeners={};
 function node(){return {children:[],style:{},setAttribute(){},appendChild(n){n.parentNode=this;this.children.push(n);},removeChild(n){this.children=this.children.filter(c=>c!==n);n.parentNode=null;}};}
 const document={body:node(),hidden:false,readyState:'complete',phase:'Login',getElementById:()=>({textContent:document.phase}),createElement:node,addEventListener:(n,f)=>listeners[n]=f};
 const location={href:url,reloads:0,reload(){this.reloads++;}};
 const window={top:{location:{}},sessionStorage:{getItem:k=>store.get(k)||null,setItem:(k,v)=>store.set(k,v)},setInterval:f=>{tick=f;return 1;},clearInterval:()=>window.cleared=true,addEventListener:(n,f)=>listeners[n]=f};
 const context={window,document,location,URL,Date:{now:()=>now}};
 const f={window,document,location,store,listeners,install(){vm.runInNewContext(script,context);},advance(ms){now+=ms;tick?.();},panel(){return document.body.children[0];}};f.install();return f;
}
const store=new Map(),first=fixture(store);first.advance(1000);first.advance(44999);assert.equal(first.location.reloads,0);first.advance(1);assert.equal(first.location.reloads,1);first.advance(100000);assert.equal(first.location.reloads,1,'old document retry loop');assert.ok(first.window.cleared);
const second=fixture(store);second.advance(1000);second.advance(45000);assert.equal(second.location.reloads,0,'second automatic reload forbidden');assert.equal(second.panel().id,'na-login-recovery');
second.panel().children[1].onclick();assert.equal(second.location.reloads,1,'manual retry missing');assert.equal(second.panel(),undefined);
const back=fixture(store);back.advance(1000);back.advance(45000);back.panel().children[2].onclick();assert.equal(back.window.top.location.href,'https://naruto.narutowebgame.com/pt/serverlist/');
const progresses=fixture();progresses.advance(1000);progresses.advance(44000);progresses.document.phase='Get server info...';progresses.advance(1000);progresses.advance(44000);assert.equal(progresses.location.reloads,0,'progress must reset timeout');
const background=fixture();background.advance(1000);background.document.hidden=true;background.advance(100000);background.document.hidden=false;background.listeners.visibilitychange();background.advance(44000);assert.equal(background.location.reloads,0,'return from background must not retry immediately');
const game=fixture();game.window.__naSafeCaptured={'https://cdnnaruto-pt.oasgames.com/PT_NarutoAlpha9.35Build301/entry.swf':true};game.advance(60000);assert.ok(game.window.cleared);assert.equal(game.location.reloads,0,'AIR capture must disable HTML recovery');
const placeholder=fixture();placeholder.window.__naSafeCaptured={'http://naruto-pt.oasgames.com/entry.swf':true};placeholder.advance(1000);placeholder.advance(45000);assert.equal(placeholder.location.reloads,1,'generic placeholder is not AIR launch');
const noStore=fixture();noStore.window.sessionStorage.getItem=()=>{throw new Error('denied');};delete noStore.window.__naLoginRecovery;noStore.install();noStore.advance(1000);noStore.advance(45000);assert.equal(noStore.location.reloads,0);assert.ok(noStore.panel(),'storage failure must allow manual recovery without auto loop');
for(const u of ['https://evil.test/main.html','https://naruto-pt.oasgames.com.evil.test/main.html','https://naruto-pt.oasgames.com/other.html','https://naruto.narutowebgame.com/pt/serverlist/','file:///main.html']){const f=fixture(new Map(),u);assert.equal(f.window.__naLoginRecovery,undefined);f.advance(100000);assert.equal(f.location.reloads,0);}
const anotherServer=fixture(store,'https://naruto-pt.oasgames.com/main.html?server_id=878');anotherServer.advance(1000);anotherServer.advance(45000);assert.equal(anotherServer.location.reloads,1,'server retry budget must be independent');
const once=fixture();const controller=once.window.__naLoginRecovery;once.install();assert.equal(once.window.__naLoginRecovery,controller,'duplicate timer installed');once.listeners.pagehide();once.advance(60000);assert.equal(once.location.reloads,0);
console.log('PASS: production official HTML login recovery: progress/background-aware 45s timeout, one persisted retry/server, manual buttons, missing storage, exact origin/path, AIR handoff and timer cleanup.');
