import {readFileSync} from 'node:fs';import vm from 'node:vm';import assert from 'node:assert/strict';
const src=readFileSync(new URL('../src/br/davi/narutoair/compat/ClientLoadScheduler.as',import.meta.url),'utf8');
let code=src.slice(src.indexOf('  public static function configure'),src.lastIndexOf('\n }'));
code=code.replace(/(?:public|private) static function (\w+)\(([^)]*)\):void/g,(_,n,args)=>`function ${n}(${args.replace(/:\w+/g,'')})`).replace(/:(?:int|Object|Number)/g,'').replace(/new Dictionary\(\)/g,'Object.create(null)');
let now=1;const listeners=new Set();const stage={frameRate:60,addEventListener:(e,f)=>listeners.add(f),removeEventListener:(e,f)=>listeners.delete(f)};
const c={host:null,downloads:[],imports:[],active:Object.create(null),count:0,queuedBytes:0,enabled:false,draining:false,MAX_DOWNLOADS:2,IMPORT_WATERMARK:8*1024*1024,foreground:true,lastFrameTime:0,deferredFrames:0,getTimer:()=>now,Math,Event:{ENTER_FRAME:'enterFrame',ACTIVATE:'activate',DEACTIVATE:'deactivate'}};
vm.createContext(c);vm.runInContext(code,c);c.configure(stage);
const starts=[],decoded=[];for(const id of ['a','b','c','d','e'])c.download(id,()=>starts.push(id));
assert.deepEqual(starts,['a','b']);assert.equal(c.count,2);
c.downloaded('a',8*1024*1024,()=>decoded.push('a'));
assert.deepEqual(starts,['a','b'],'8 MiB import backlog must pause new downloads');assert.equal(c.count,1);assert.equal(listeners.size,3);
c.frame({});assert.deepEqual(decoded,['a']);assert.deepEqual(starts,['a','b','c']);assert.equal(c.count,2);assert.equal(listeners.size,2);
c.downloaded('b',100,()=>decoded.push('b'));c.downloaded('c',200,()=>decoded.push('c'));
assert.equal(c.imports.length,2);c.frame({});assert.deepEqual(decoded,['a','b'],'at most one code import per frame');assert.equal(c.queuedBytes,200);
c.cancel('c');assert.equal(c.queuedBytes,0);assert.equal(listeners.size,2);c.frame({});assert.deepEqual(decoded,['a','b'],'unloaded module must never import later');
c.downloaded('d',100,()=>decoded.push('d'));c.disable();assert.equal(c.count,0);assert.equal(c.queuedBytes,0);assert.equal(c.imports.length,0);assert.equal(c.downloads.length,0);assert.equal(listeners.size,0);c.frame({});assert.deepEqual(decoded,['a','b']);
c.configure(stage);c.download('new',()=>starts.push('new'));assert.equal(c.count,1);c.cancel('new');assert.equal(c.count,0);
// Failed starts may cancel synchronously without recursively starting the same queue.
c.download('bad',()=>c.cancel('bad'));assert.equal(c.count,0);assert.equal(c.draining,false);
c.download('large',()=>{});c.downloaded('large',32*1024*1024,()=>decoded.push('large'));
c.download('waiting',()=>starts.push('waiting'));assert.ok(!starts.includes('waiting'));c.frame({});assert.ok(starts.includes('waiting'));
const loader=readFileSync(new URL('../src/br/davi/narutoair/compat/BrowserLoader.as',import.meta.url),'utf8');
assert.ok(loader.includes('releaseStream(false);'),'download slot must remain occupied until backpressure is registered');
assert.ok(loader.includes('ClientLoadScheduler.disable();'));assert.ok(loader.includes('ClientLoadScheduler.cancel(this);'));
console.log('PASS: production load scheduler: two concurrent code downloads, one import/frame, 8 MiB backpressure, cancellation, no stale import after restart, synchronous failure and listener teardown. Node model, not AIR device.');

// Production slow-frame budget: bounded postponement and background retention.
c.disable();c.configure(stage);decoded.length=0;
for(let i=0;i<5;i++){c.download('budget'+i,()=>{});c.downloaded('budget'+i,100,()=>decoded.push(i));}
now=100;c.frame({});assert.deepEqual(decoded,[0]);
now=140;c.frame({});assert.deepEqual(decoded,[0]);assert.equal(c.queuedBytes,400,'defer must retain queued bytes');
now=180;c.frame({});assert.deepEqual(decoded,[0]);
now=220;c.frame({});assert.deepEqual(decoded,[0,1],'bounded defer must allow progress despite sustained long frames');
now=237;c.frame({});assert.deepEqual(decoded,[0,1,2],'normal frame should resume promptly');
c.deactivate({});now=10000;c.frame({});assert.deepEqual(decoded,[0,1,2],'background decoded code');assert.equal(c.queuedBytes,200);
c.activate({});now=10001;c.frame({});assert.deepEqual(decoded,[0,1,2,3],'resume should reset stale elapsed time');
c.cancel('budget4');assert.equal(c.queuedBytes,0);assert.equal(c.lastFrameTime,0);assert.equal(listeners.size,2);
c.disable();assert.equal(listeners.size,0);
console.log('PASS: production import budget defers after long frames, bounds delay to two frames, resumes immediately at normal pacing, retains byte backpressure/background tasks and releases every listener.');
