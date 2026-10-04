import {readFileSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import vm from 'node:vm';
import assert from 'node:assert/strict';
// Execute the production resolver body with only AS3 type annotations erased.
// This covers its URL/string logic, not Flash URLRequest or AIR execution.
const source=readFileSync(fileURLToPath(new URL('../src/br/davi/narutoair/compat/BrowserResources.as',import.meta.url)),'utf8');
const names=['configure','disable','resolve','isClientSwf'];
const methods=names.map(name=>{
    const start=source.indexOf('public static function '+name+'(');
    const end=source.indexOf('public static function ',start+1);
    assert(start>=0,'production method missing');
    return source.slice(start,end<0?source.length:end).trim();
}).join('\n').replace(/public static function/g,'function')
  .replace(/\b(var\s+\w+|\w+(?=:\w+(?:[,)]))):(?:String|Array|Boolean|int|void)/g,'$1')
  .replace(/\)\s*:(?:String|Boolean|void)\s*\{/g,') {')
  .replace(/for each\(var (\w+) in (\w+)\)/g,'for(var $1 of $2)');
const context=vm.createContext({});
vm.runInContext('var officialRoot="",runtimeRoot="",folder="";\n'+methods,context);
const remote='https://cdnnaruto-pt.oasgames.com/PT_NarutoAlpha9.35Build301/entry.swf';
const local='http://127.0.0.1:35001/PT_NarutoAlpha9.35Build301/entry.swf';
const folder='http://127.0.0.1:35001/PT_NarutoAlpha9.35Build301/';
const origin='http://127.0.0.1:35001';
context.configure(remote,local);
for(const [input,expected] of [
 ['client.swf',folder+'client.swf'],
 ['assets/sound/bgm.mp3',folder+'assets/sound/bgm.mp3'],
 ['app:/assets/sound/bgm.mp3',folder+'assets/sound/bgm.mp3'],
 ['PT_NarutoAlpha6.00Build300/assets/sound/swf/s1791.swf',origin+'/PT_NarutoAlpha6.00Build300/assets/sound/swf/s1791.swf'],
 ['https://cdnnaruto-pt.oasgames.com/PT_NarutoAlpha6.00Build300/assets/sound/bgm.mp3?v=7',origin+'/PT_NarutoAlpha6.00Build300/assets/sound/bgm.mp3?v=7'],
 ['https://other.invalid/bgm.mp3','https://other.invalid/bgm.mp3'],
 ['./assets/../client.swf?v=42&token=abc',folder+'client.swf?v=42&token=abc'],
 [remote+'?version=1',folder+'entry.swf?version=1'],
 [remote.replace('https:','http:')+'?version=1',folder+'entry.swf?version=1'],
 ['/images/loading.png','http://127.0.0.1:35001/images/loading.png'],
 ['../shared/data.bin','http://127.0.0.1:35001/shared/data.bin'],
 ['https://naruto-pt-login.oasgames.com/LoginManager-1.0.php?token=abc','https://naruto-pt-login.oasgames.com/LoginManager-1.0.php?token=abc'],
 ['https://cdnnaruto-pt.oasgames.com.evil.test/client.swf','https://cdnnaruto-pt.oasgames.com.evil.test/client.swf'],
 ['//evil.test/client.swf','//evil.test/client.swf'],
 // Regression: Original entry initLoadingView derives resource.cfg from loaderInfo.loaderURL; AIR reports app:/NarutoAir.swf.
 ['app:/resource.cfg',folder+'resource.cfg'],
 ['app:/version.txt',folder+'version.txt'],
 ['app:/version.txt?v=11&token=abc#fragment',folder+'version.txt?v=11&token=abc#fragment'],
 ['app:/icons/logo.png',folder+'icons/logo.png'],
 ['resource.cfg',folder+'resource.cfg'],
 ['version.txt',folder+'version.txt'],
 ['./version.txt',folder+'version.txt'],
 [folder+'./flash/core/LoginConfig.xml',folder+'flash/core/LoginConfig.xml'],
 ['flash/core/LoginConfig.xml',folder+'flash/core/LoginConfig.xml'],
 [folder+'flash/core/LoginConfig.xml',folder+'flash/core/LoginConfig.xml'],
 [remote.replace('entry.swf','flash/core/LoginConfig.xml')+'?v=42',folder+'flash/core/LoginConfig.xml?v=42'],
 ['PT_NarutoAlpha6.24Build301/flash/core/LoginConfig.xml',origin+'/PT_NarutoAlpha6.24Build301/flash/core/LoginConfig.xml'],
 ['PT_NarutoAlpha6.24Build301/flash/core/naruto.core.swf',origin+'/PT_NarutoAlpha6.24Build301/flash/core/naruto.core.swf'],
 [folder+'flash/core/naruto.core.swf',folder+'flash/core/naruto.core.swf'],
 ['app://evil.test/client.swf','app://evil.test/client.swf'],
 ['app-storage:/version.txt','app-storage:/version.txt'],
 ['file:/version.txt','file:/version.txt'],
 ['app:/../../version.txt','app:/../../version.txt'],
 ['../../../../client.swf','../../../../client.swf'],
 ['../bad\\client.swf','../bad\\client.swf']
]) assert.equal(context.resolve(input),expected,input);
for(const url of [folder+'client.swf',folder+'modules/loader.swf?version=2',origin+'/PT_NarutoAlpha6.24Build301/flash/core/naruto.core.swf',origin+'/flash/hud/activityDashboard.swf'])assert.equal(context.isClientSwf(url),true,url);
for(const url of [folder+'client.png',folder+'%2e%2e/out.swf',folder+'../out.swf',origin+'/shared/client.swf',remote,folder.replace(':35001',':35002')+'client.swf',origin+'.evil.test/flash/core/out.swf'])assert.equal(context.isClientSwf(url),false,url);
assert.throws(()=>context.configure(remote,local.replace('Build301','Build302')));
context.configure(remote,local);context.disable();
assert.equal(context.resolve('client.swf'),'client.swf');
assert.equal(context.isClientSwf(folder+'client.swf'),false);
console.log('PASS: production AS3 resolver body (type-erased): device app:/ config regression, manifest beside entry, intact build tags for LoginConfig, mixed-version SWFs, query preservation, origin boundaries, traversal and disable. AIR runtime not exercised.');
