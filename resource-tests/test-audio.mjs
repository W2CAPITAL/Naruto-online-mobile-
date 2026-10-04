import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const root=new URL('../src/br/davi/narutoair/compat/',import.meta.url);
let source=fs.readFileSync(new URL('BrowserSound.as',root),'utf8');
// Execute the authored AS3 control flow. Native decoding/output are modeled.
source=source.replace(/^\s*import .*;$/gm,'').replace(/^package[^\{]+\{/,'').replace(/\}\s*$/,'')
 .replace('public class BrowserSound','class BrowserSound')
 .replace(/private static var/g,'static').replace(/private var/g,'')
 .replace('public function BrowserSound','constructor')
 .replace(/(?:private static|public static) function/g,'static').replace(/(?:override public|private) function/g,'')
 .replace(/for each\(var (\w+) in (\w+)\)/g,'for(var $1 of $2)')
 .replace(/for\(var (\w+) in (loading|channels)\)/g,'for(var $1 of $2.keys())')
 .replace(/delete (loading|channels)\[([^\]]+)\]/g,'$1.delete($2)')
 .replace(/(loading|channels)\[([^\]]+)\]=true/g,'$1.set($2,true)')
 .replace(/event.currentTarget as SoundChannel/g,'event.currentTarget')
 .replace(/:(?:URLRequest|SoundLoaderContext|SoundTransform|SoundChannel|Dictionary|String|Boolean|Object|Array|Event|Error|int|Number|BrowserSound|void)\b/g,'')
 .replace(/\b(var|static) (\w+)(?=\s*[=;])/g,'$1 $2');
source=source.replace(/for each\(var (\w+) in (\w+)\)/g,'for(var $1 of $2)')
 .replace(/for\(var (\w+) in (loading|channels)\)/g,'for(var $1 of $2.keys())');
// AS3 static fields are lexically visible without a class qualifier.
for(const name of ['loading','channels','channelComplete'])source=source.replace(new RegExp('(?<![.\\w])'+name+'(?![\\w]|\\s*(?:=|\\())','g'),name);
source=source.replace(/(?<![.\w])(loading|channels|channelComplete)\b/g,'BrowserSound.$1')
 .replace(/static BrowserSound\./g,'static ')
 .replace('static function disable','static disable');
source=source.replace(/static var /g,'static ').replace('static channelComplete','static channelComplete');
// Methods without "function" are instance methods; retain explicit static declarations.
source=source.replace(/\n\s*channelComplete\(/g,'\n static channelComplete(').replace(/\n\s*disable\(/g,'\n static disable(');
for(const name of ['requestUrl','active','onComplete','onError','finish','load','addEventListener'])
 source=source.replace(new RegExp('(?<![.\\w])'+name+'\\b','g'),'this.'+name);
source=source.replace('this.requestUrl=""','requestUrl=""').replace('this.active=false','active=false')
 .replace(/^(\s*)this\.(load|onComplete|onError|finish)\((.*?)\)\s*\{/gm,'$1$2($3) {');
class Events {
 listeners=new Map();
 addEventListener(name,fn){const list=this.listeners.get(name)||[];list.push(fn);this.listeners.set(name,list);}
 removeEventListener(name,fn){this.listeners.set(name,(this.listeners.get(name)||[]).filter(x=>x!==fn));}
 dispatch(name){for(const fn of this.listeners.get(name)||[])fn.call(this,{currentTarget:this});}
}
class NativeChannel extends Events {stopped=false;stop(){this.stopped=true;}}
function SoundChannel(v){return v;}
class NativeSound extends Events {
 constructor(){super();this.calls=[];}
 load(request,context){if(this.loaded)throw Error('load once');this.loaded=true;this.request=request;this.context=context;}
 close(){if(this.failClose)throw Error('already closed');this.closed=true;}
 play(...args){this.calls.push(args);return this.nullChannel?null:new NativeChannel();}
}
const notes=[];let copies=0;
const c=vm.createContext({Sound:NativeSound,SoundChannel,Dictionary:class extends Map{constructor(){super();}},Event:{COMPLETE:'complete',SOUND_COMPLETE:'soundComplete'},IOErrorEvent:{IO_ERROR:'io'},SecurityErrorEvent:{SECURITY_ERROR:'security'},BrowserResources:{request:r=>{copies++;return {...r,url:'http://127.0.0.1:32123/PT_NarutoAlpha9.35Build301/'+r.url}},note:(...a)=>notes.push(a)}});
try{vm.runInContext(source+'\nglobalThis.BrowserSound=BrowserSound;',c);}catch(e){fs.writeFileSync('/tmp/naruto-audio-transformed.js',source);throw e;}
const context={bufferTime:8000,checkPolicyFile:true},request={url:'assets/sound/music.mp3',headers:['x']};
const s=new c.BrowserSound(request,context);
assert(s instanceof NativeSound);assert.equal(copies,1);assert.notEqual(s.request,request);assert.equal(request.url,'assets/sound/music.mp3');assert.equal(s.context,context);
const transform={volume:.6,pan:-.2},channel=s.play(25,4,transform);assert.deepEqual(s.calls[0],[25,4,transform]);assert.equal(c.BrowserSound.channels.size,1);
assert.throws(()=>s.load(request),/load once/);assert.equal(c.BrowserSound.loading.size,1);
s.dispatch('complete');assert.equal(c.BrowserSound.loading.size,0);assert.equal(channel.stopped,false);
channel.dispatch('soundComplete');assert.equal(c.BrowserSound.channels.size,0);
const embedded=new c.BrowserSound();assert.equal(embedded.loaded,undefined);assert.equal(copies,2); // repeat load resolved but natively rejected
embedded.nullChannel=true;assert.equal(embedded.play(),null);
for(const phase of ['io','security']){const x=new c.BrowserSound(request);x.dispatch(phase);assert.equal(c.BrowserSound.loading.size,0);}
const playing=embedded.nullChannel=false;const old=embedded.play();const pending=new c.BrowserSound(request);pending.failClose=true;
c.BrowserSound.disable();assert(old.stopped);assert.equal(c.BrowserSound.loading.size,0);assert.equal(c.BrowserSound.channels.size,0);assert(notes.some(x=>x[1]==='CANCEL'));
assert(notes.some(x=>x[1]==='COMPLETE'));assert(notes.some(x=>x[1]==='ERROR'));
console.log('PASS: production BrowserSound control flow: constructor/embedded native inheritance, URL request copy, loader context, native play/loops/pan, null channel, real complete/error, repeat-load preservation and session cleanup. No physical audio output tested.');
let session=fs.readFileSync(new URL('AudioSession.as',root),'utf8')
 .replace(/^\s*import .*;$/gm,'').replace(/^package[^\{]+\{/,'').replace(/\}\s*$/,'')
 .replace('public final class AudioSession','class AudioSession').replace(/(?:private|public) static var/g,'static')
 .replace(/public static function/g,'static')
 .replace(/:(?:SharedObject|Boolean|void|ByteArray|int|Number|Sound|Error)\b/g,'');
for(const name of ['prefs','enabled','start'])session=session.replace(new RegExp('(?<![.\\w])'+name+'\\b','g'),'AudioSession.'+name);
session=session.replace(/static AudioSession\./g,'static ');
let flushed=0,stopAll=0,disabled=0,pcmData;
const saved={data:{enabled:false},flush:()=>flushed++},mixer={stopAll:()=>stopAll++};
class PCM {data=[];writeFloat(v){this.data.push(v);}}
class PCMPlayer {loadPCMFromByteArray(pcm,...args){pcmData={data:pcm.data,args};}play(){return {};}}
const a=vm.createContext({Sound:PCMPlayer,SoundMixer:mixer,SoundTransform:class{constructor(volume){this.volume=volume;}},AudioPlaybackMode:{MEDIA:'media'},SharedObject:{getLocal:()=>saved},ByteArray:PCM,BrowserSound:{disable:()=>disabled++}});
vm.runInContext(session+'\nglobalThis.AudioSession=AudioSession;',a);
a.AudioSession.initialize();assert.equal(a.AudioSession.enabled,false);assert.equal(mixer.soundTransform.volume,0);assert.equal(mixer.audioPlaybackMode,'media');
assert.equal(a.AudioSession.test(),false);assert.equal(pcmData,undefined);
a.AudioSession.toggle();assert.equal(saved.data.enabled,true);assert.equal(flushed,1);assert.equal(mixer.soundTransform.volume,1);
assert(a.AudioSession.test());assert.deepEqual(pcmData.args,[11025,'float',true,44100]);assert.equal(pcmData.data.length,22050);
assert.equal(pcmData.data[0],0);assert(pcmData.data.at(-1)===0);assert(pcmData.data.every(v=>Number.isFinite(v)&&Math.abs(v)<=.120000001));assert(pcmData.data.some(v=>Math.abs(v)>.1));
a.AudioSession.stop();assert.equal(disabled,1);assert.equal(stopAll,1);
console.log('PASS: production AudioSession logic: saved mute respected, media mode, user toggle persistence, cleanup and short stereo PCM tone; native decode/device output not exercised.');
