import {readFileSync} from 'node:fs';import vm from 'node:vm';import assert from 'node:assert/strict';
const script=readFileSync(new URL('../build-tools/portal-access.js',import.meta.url),'utf8');
const java=readFileSync(new URL('../bridge-src/br/davi/narutoair/portal/PortalAccess.java',import.meta.url),'utf8');
assert.equal(JSON.parse(java.match(/public static final String SCRIPT=("(?:\\.|[^"\\])*");/)[1]),script,'embedded production classifier drift');
function classify({url='https://naruto.narutowebgame.com/pt/serverlist/',text='',ids=[],title='',ready='complete',frame=false}={}){
 const page=new URL(url);const context={location:page,window:{},document:{body:{textContent:text},title,readyState:ready,getElementById:id=>ids.includes(id)?{}:null}};
 context.window.top=frame?{}:context.window;return vm.runInNewContext(script,context);
}
assert.equal(classify({text:'Sorry, you have been blocked. Cloudflare Ray ID: fixture',ids:['cf-error-details']}),'blocked');
assert.equal(classify({text:'Você foi bloqueado. Cloudflare'}),'blocked');
assert.equal(classify({url:'https://gamebox3.narutowebgame.com/gamebox/2.4.1/template/login.php',text:'You have been blocked',ids:['cf-error-details']}),'blocked');
assert.equal(classify({title:'Just a moment...'}),'challenge');assert.equal(classify({ids:['challenge-running']}),'challenge');
assert.equal(classify({text:'You have been blocked by another player'}),'clear');
assert.equal(classify({text:'Cloudflare Error 502',ids:['cf-error-details']}),'unknown');
assert.equal(classify({url:'https://naruto.narutowebgame.com.evil.test/',text:'Cloudflare You have been blocked'}),'unknown');
assert.equal(classify({url:'https://www.facebook.com/',text:'Cloudflare You have been blocked'}),'unknown');
assert.equal(classify({url:'http://naruto.narutowebgame.com/',text:'Cloudflare You have been blocked'}),'unknown');
assert.equal(classify({frame:true,text:'Cloudflare You have been blocked'}),'unknown');
assert.equal(classify({ready:'loading',text:'Cloudflare You have been blocked'}),'unknown');
assert.equal(classify(), 'clear');
assert.ok(!/fetch\(|location\.reload|location\.replace|sessionStorage|cookie\s*=|setInterval|userAgent\s*=/.test(script));
console.log('PASS: actual portal access script recognizes terminal Cloudflare block vs challenge, rejects foreign/frame/loading pages, leaves login/cookies/navigation untouched.');
