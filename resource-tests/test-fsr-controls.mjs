import {readFileSync} from 'node:fs';import vm from 'node:vm';import assert from 'node:assert/strict';
const src=readFileSync(new URL('../src/br/davi/narutoair/compat/GraphicsProfile.as',import.meta.url),'utf8');
let body=src.slice(src.indexOf('  public function setFSR('),src.indexOf('  private static function validChoice('));
body=body.replace(/public function (\w+)\(([^)]*)\):(Boolean|void)/g,(_,n,a)=>`function ${n}(${a.replace(/:(Boolean|String)/g,'')})`)
 .replace(/:(int|Array)/g,'').replace(/catch\((\w+):Error\)/g,'catch($1)');
let calls=[];const c={running:false,foreground:true,host:{stage3Ds:[],frameRate:60},nativeFsr:(enabled,epoch)=>{calls.push([enabled,epoch]);return true;},fsrWanted:false,fsrValue:'OFF',fsrEpoch:0};
vm.createContext(c);vm.runInContext(body,c);
assert.equal(c.setFSR(true),false);assert.equal(c.fsrWanted,false);assert.equal(calls.length,0,'portal must never enable compositor');
c.running=true;c.foreground=false;assert.equal(c.setFSR(true),false);assert.equal(calls.length,0);
c.foreground=true;c.host.stage3Ds=[{context3D:{}}];assert.equal(c.setFSR(true),false);assert.equal(calls.length,0);
c.host.stage3Ds=[];assert.equal(c.setFSR(true),true);assert.equal(c.fsrWanted,true);assert.deepEqual(calls,[[true,1]]);assert.equal(c.host.frameRate,60,'FSR must not speed game simulation');
c.fsrStatus('1:active:1283x576:1604x720:59.9');assert.ok(c.fsrValue.includes('EASU + RCAS'));assert.ok(c.fsrValue.includes('59.9'));
c.setFSR(false);const off=c.fsrValue;c.fsrStatus('1:active:1283x576:1604x720:120');assert.equal(c.fsrValue,off,'stale active after OFF');
c.setFSR(true);c.fsrStatus('1:fallback:Old failure');assert.equal(c.fsrWanted,true,'old failure must not cancel new choice');
c.fsrStatus('3:fallback:Unsupported GPU');assert.equal(c.fsrWanted,false);assert.equal(c.fsrValue,'Unsupported GPU');
c.nativeFsr=()=>false;assert.equal(c.setFSR(true),false);assert.equal(c.fsrWanted,false);
c.nativeFsr=()=>{throw Error('missing');};assert.equal(c.setFSR(true),false);assert.equal(c.fsrWanted,false);
c.nativeFsr=()=>true;c.setFSR(true);c.fsrStatus(`${c.fsrEpoch}:off`);assert.equal(c.fsrWanted,false,'portal or recharge teardown must update control');
assert.equal(c.host.frameRate,60);console.log('PASS: explicit FSR opt-in, game/foreground/Stage3D guards, real status, native failures, teardown and epoch rejection; default 60 unchanged');
