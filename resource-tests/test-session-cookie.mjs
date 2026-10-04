import {readFileSync} from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const src=readFileSync(new URL('../src/br/davi/narutoair/compat/BrowserExternalInterface.as',import.meta.url),'utf8');
const start=src.indexOf('public static function call(');
const end=src.indexOf('private static function sessionCookie(',start);
let method=src.slice(start,end).trim();
method=method.replace(/public static function call\(name:String,\.\.\.args\):\*/,'function call(name,...args)');
method=method.replace(/var (\w+):(String|Array)/g,'var $1');
let queued=[];
const context={state:{uin:'1234567',userAgent:'testUA',sessionCookies:{uin:1234567,skey:'fixture-session'}},available:true,sessionCookie:key=>context.state.sessionCookies[key],send:(...a)=>queued.push(a)};
vm.createContext(context);vm.runInContext(method,context);
for(const key of ['uin','skey']) {
 assert.equal(context.call('getCookie',key),context.state.sessionCookies[key]);
 for(const accessor of [`getCookie('${key}')`,`window.getCookie("${key}")`,`function(){ return getCookie('${key}'); }`]) {
  assert.equal(context.call('eval',accessor),context.state.sessionCookies[key]);
  if(accessor.startsWith('function'))assert.equal(context.call(accessor),context.state.sessionCookies[key]);
 }
}
assert.equal(queued.length,0);
assert.equal(context.call('oss_report',40,'start'),null);assert.equal(queued.length,1);
assert.equal(context.call('getCookie','other'),null);assert.equal(queued.length,2);
assert.equal(context.call('eval',"getCookie('skey'); unrelated()"),null);assert.equal(queued.length,3);
context.state.sessionCookies.skey='updated';assert.equal(context.call('getCookie','skey'),'updated');
assert.ok(!src.includes('String(state.sessionCookies.skey)'));
console.log('PASS: production session accessor returns real captured key synchronously, preserves page side effects, and rejects mixed expressions from the pure-accessor path.');
