import {readFileSync} from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const source=readFileSync(new URL('../src/br/davi/narutoair/compat/BrowserResources.as',import.meta.url),'utf8');
const names=['safePath','note','audioSummary'];
const methods=names.map(name=>{
 const start=source.search(new RegExp('(?:public|private) static function '+name+'\\('));
 assert(start>=0);
 const tail=source.slice(start+1);
 const next=tail.search(/(?:public|private) static function /);
 return source.slice(start,next<0?source.length:start+1+next).trim();
}).join('\n').replace(/(?:public|private) static function/g,'function')
 .replace(/:(?:String|int|void)\b/g,'');
const c=vm.createContext({});
vm.runInContext('var audioLines=[],reporter=null;\n'+methods,c);
c.note('Sound','ERROR','https://user:PRIVATE@cdn.test/assets/sound/bgm.mp3?token=SECRET#auth',2032);
assert.equal(c.audioSummary(),'ERROR /assets/sound/bgm.mp3; code=2032');
c.note('Sound','PLAY','//user:PRIVATE@cdn.test/assets/sound/bgm.mp3?token=SECRET');
assert(!/PRIVATE|SECRET|user|cdn.test/.test(c.audioSummary()));
for(let i=0;i<100;i++)c.note('Sound','PLAY_NULL','app:/assets/sound/track'+i+'.mp3');
assert.equal(c.audioLines.length,64);
assert.equal(c.audioLines[0],'PLAY_NULL app:/assets/sound/track36.mp3');
assert(c.audioSummary().includes('track99'));
c.note('URLLoader','ERROR','https://user:PRIVATE@cdn.test/other');
assert.equal(c.audioLines.length,64);
console.log('PASS: production audio diagnostics retain only 64 Sound events, preserve error IDs, record null channels, redact URL authority/query/fragment and do no general release reporting.');
