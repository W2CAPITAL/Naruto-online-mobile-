import {readFileSync} from 'node:fs';import vm from 'node:vm';import assert from 'node:assert/strict';
const src=readFileSync(new URL('../src/br/davi/narutoair/compat/GraphicsProfile.as',import.meta.url),'utf8');
assert.ok(src.includes('targetValue:Number=60'), 'default and safe fallback must be 60');
assert.ok(src.includes('fpsChoice:Number=60'));
assert.ok(src.includes('getLocal("narutoFrameLimitV1")'),'higher rates require a new explicit opt-in preference');
assert.ok(src.includes('active.host.frameRate=active.targetValue'), 'module guard must respect display target');
assert.ok(!src.includes('narutoGraphics'),'old maximum/OFF settings must not migrate');
assert.ok(src.includes('Event.EXIT_FRAME,finishFrame,false,-10000'),'restore cap after frame scripts');
let body=src.slice(src.indexOf('  private function resetSample('),src.indexOf('  private function verifyRenderer('));
body=body.replace(/private function (\w+)\([^)]*\):void/g,(s,n)=>`function ${n}(${n==='resetSample'?'':'e'})`).replace(/:int/g,'');
let now=0,colors=0;
const c={host:{frameRate:30},setFSR:()=>{},capRepairs:0,running:true,foreground:true,frames:0,sampleStart:0,measured:NaN,targetValue:90,getTimer:()=>now,updateColors:()=>colors++,verifyRenderer:()=>{},updateQuality:()=>{}};
vm.createContext(c);vm.runInContext(body,c);now=1;c.frame({});assert.equal(c.host.frameRate,90,'do not wait one second to undo client cap');c.resetSample();
for(let i=1;i<=90;i++){now=1+i*1000/90;c.frame({});c.host.frameRate=30;c.host.quality='best';c.finishFrame({});assert.equal(c.host.frameRate,90);assert.equal(c.host.quality,'medium');}
assert.equal(c.capRepairs,90,'repair child changes after Stage ENTER_FRAME');assert.ok(Math.abs(c.measured-90)<.01);assert.equal(colors,1);
for(let i=1;i<=30;i++){now=1001+i*1000/30;c.frame({});}
assert.equal(c.measured,30,'target 90 must not falsify measured rate');
c.deactivate({});now=60000;c.frame({});assert.equal(c.frames,0);c.activate({});assert.equal(c.sampleStart,60000);assert.ok(Number.isNaN(c.measured));
c.running=false;now+=10000;c.frame({});assert.equal(c.frames,0);
c.host.frameRate=30;c.finishFrame({});assert.equal(c.host.frameRate,30,'stopped game must not change portal cap');
console.log('PASS: EXIT_FRAME repairs late caps without double-counting FPS; honest observed rate and pause/resume preserved.');
// Execute production native status and selection handlers, including stale async responses.
let statusBody=src.slice(src.indexOf('  public function nativeStatus('),src.indexOf('  private function resetSample('));
statusBody=statusBody.replace('public function nativeStatus(value:String):void','function nativeStatus(value)')
 .replace(/:(Array|Number|int)/g,'').replace(/\bint\(/g,'Number(');
let choiceBody=src.slice(src.indexOf('  private static function validChoice('),src.indexOf('  public function get contrast('));
choiceBody=choiceBody.replace(/(?:private static|public) function (\w+)\(([^)]*)\):(Boolean|void)/g,(_,name,args)=>`function ${name}(${args.replace(/:Number/g,'')})`)
 .replace(/catch\((\w+):Error\)/g,'catch($1)');
let saved=0;
const d={running:true,foreground:true,host:{frameRate:60},targetValue:60,fpsChoice:60,displayStatus:'',fallback:()=>{},getTimer:()=>0,resetSample:()=>{},nativeRate:()=>true,fpsPrefs:{data:{},flush(){saved++;}}};
vm.createContext(d);vm.runInContext(statusBody,d);vm.runInContext(choiceBody,d);
d.nativeStatus('display:120:60:60');assert.equal(d.targetValue,60);
for(const event of ['display:60:90:90','display:60:120:0','display:60:90:60','display:60:NaN:60','display:60:Infinity:60','display:60:0:60','display:60:-1:60','display:60:1001:60','display:NaN:60:60','display:0:60:60','display:60:60:extra','display:60:120']){d.nativeStatus(event);assert.equal(d.targetValue,60,'default or malformed status must not enable high FPS');}
for(const choice of [90,120,0,60]){assert.equal(d.setFPSChoice(choice),true);assert.equal(d.fpsChoice,choice);assert.equal(d.host.frameRate,choice||60);assert.equal(d.fpsPrefs.data.choice,choice);}
assert.equal(saved,4);
for(const invalid of [30,144,-1,NaN,Infinity])assert.equal(d.setFPSChoice(invalid),false);
d.nativeRate=()=>false;assert.equal(d.setFPSChoice(90),false);assert.equal(d.fpsChoice,60);assert.equal(saved,4);
d.nativeRate=()=>{throw Error('unavailable');};assert.equal(d.setFPSChoice(90),false);assert.equal(d.fpsChoice,60);
d.nativeRate=()=>true;d.setFPSChoice(0);
for(const hz of [120,144,240,59.94,90]){d.nativeStatus('display:60:'+hz+':0');assert.equal(d.targetValue,hz);assert.equal(d.host.frameRate,hz);}
d.setFPSChoice(60);d.nativeStatus('display:60:144:0');assert.equal(d.host.frameRate,60,'old MAX acknowledgement must not override newly chosen 60');
d.nativeStatus('display:59.94:59.94:60');assert.equal(d.targetValue,59.94);
d.setFPSChoice(120);d.foreground=false;d.nativeStatus('display:60:120:120');assert.equal(d.targetValue,120);assert.equal(d.host.frameRate,120);d.host.frameRate=30;d.nativeStatus('display:60:120:120');assert.equal(d.host.frameRate,30,'background status reactivated cadence');
c.targetValue=d.targetValue;c.running=true;c.activate({});assert.equal(c.host.frameRate,120);
c.host.frameRate=30;c.finishFrame({});assert.equal(c.host.frameRate,120,'late cap guard must follow selected target');
c.resetSample();now=c.sampleStart+1000;c.frame({});assert.equal(c.measured,1,'display hint must not create counted frames');
d.running=false;d.nativeStatus('display:60:90:120');assert.equal(d.targetValue,120,'late display event after portal return must be ignored');
// Run production preference restoration: corrupt/old-looking values never opt into MAX.
let restore=src.slice(src.indexOf('   try{fpsPrefs=SharedObject.getLocal'),src.indexOf('   // Priority orders'));
restore=restore.replace('fpsPrefs.data.choice is Number',"typeof fpsPrefs.data.choice==='number'").replace(/catch\((\w+):Error\)/g,'catch($1)');
for(const stored of [undefined,null,'90',30,NaN,60,90,120,0,144]){
 const savedContext={fpsChoice:60,targetValue:60,host:{},validChoice:d.validChoice,SharedObject:{getLocal(name){assert.equal(name,'narutoFrameLimitV1');return {data:{choice:stored}};}}};
 vm.createContext(savedContext);vm.runInContext(restore,savedContext);
 const expected=typeof stored==='number' && [0,60,90,120].includes(stored)?stored:60;
 assert.equal(savedContext.fpsChoice,expected);assert.equal(savedContext.host.frameRate,expected||60);
}
let stopBody=src.slice(src.indexOf('  public function stop('),src.indexOf('  private function applyScale(')).replace('public function stop():void','function stop()');
const stopped={running:true,setFSR:()=>{},nativeScale:()=>true,restoreColors:()=>{},resetSample:()=>{},host:{frameRate:120},fpsChoice:120};
vm.createContext(stopped);vm.runInContext(stopBody,stopped);stopped.stop();assert.equal(stopped.host.frameRate,60);assert.equal(stopped.running,false);assert.equal(stopped.fpsChoice,120,'portal stop must retain user preference');
console.log('PASS: default 60, explicit saved 90/120/MAX choices, stale/invalid async rejection, fractional refresh, native failure, background isolation and actual callback counting.');
function convert(s){return s.replace(/private function (\w+)\(([^)]*)\):void/g,(all,n,args)=>`function ${n}(${args.replace(/:(int|String)/g,'')})`).replace(/:int/g,'');}
const gate=convert(src.slice(src.indexOf('  private function verifyRenderer('),src.indexOf('  private function updateQuality(')));
let calls=[];const g={fsrWanted:false,host:{stageWidth:1283,stageHeight:576,stage3Ds:[]},scaleValue:80,confirmedWidth:1283,confirmedHeight:576,verifyAt:3000,retryAt:0,failures:0,nativeScale:n=>calls.push(n),applyScale:()=>calls.push(80),getTimer:()=>now,Math};
vm.createContext(g);vm.runInContext(gate,g);g.verifyRenderer(3000);assert.equal(calls.length,0);
now=4000;g.host.stageWidth=1604;g.verifyAt=4000;g.verifyRenderer(4000);assert.equal(g.scaleValue,80,'do not persist OFF after temporary mismatch');assert.deepEqual(calls,[100]);assert.equal(g.retryAt,9000);
g.verifyRenderer(8000);assert.equal(calls.length,1);g.verifyRenderer(9000);assert.deepEqual(calls,[100,80]);
g.retryAt=0;now=10000;g.fallback('retry');assert.equal(g.retryAt,15000);g.fallback('retry limit');assert.equal(g.retryAt,0,'bound retry to avoid resize loop');
g.confirmedWidth=1283;g.host.stage3Ds=[{context3D:{}}];g.verifyRenderer(11000);assert.equal(g.confirmedWidth,0,'separate GPU surface must retain native safety');assert.equal(g.scaleValue,80);
console.log('PASS: upscaling remains requested, retries temporary mismatches at bounded intervals, and preserves separate Stage3D surface safety.');
const quality=convert(src.slice(src.indexOf('  private function updateQuality('),src.indexOf('  private function updateColors(')));
const q={host:{quality:'high'},measured:30,slowSamples:0,fastSamples:0};vm.createContext(q);vm.runInContext(quality,q);
q.updateQuality();assert.equal(q.host.quality,'medium');
q.measured=90;for(let i=0;i<20;i++)q.updateQuality();assert.equal(q.host.quality,'medium','fast scene must not promote expensive antialiasing');
q.host.quality='best';q.updateQuality();assert.equal(q.host.quality,'medium');
console.log('PASS: stable medium antialiasing remains active across slow and fast scenes, retaining display-paced target and colors.');
