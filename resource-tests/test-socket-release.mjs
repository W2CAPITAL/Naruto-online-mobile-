import {readFileSync} from 'node:fs';import assert from 'node:assert/strict';
const src=readFileSync(new URL('../src/br/davi/narutoair/compat/BrowserSocket.as',import.meta.url),'utf8');
// Execute each actual release fast path with poisoned diagnostic helpers.
const helpers=new Proxy({}, {get(){throw new Error('diagnostic evaluated in release');}});
for(const match of src.matchAll(/override public function (\w+)\(([^\n]*?)\):[^\{]*\{(if\(report==null\)(?:\{[^}]*\}|return [^;]*;))/g)){
 const [,name,args,body]=match;const names=args.split(',').filter(Boolean).map(x=>x.split(':')[0]);
 const actualArgs=names.map((x,i)=>x==='bytes'?{unchanged:true}:i+1);let forwarded;
 const native={[name]:(...a)=>{forwarded=a;return 123;}};
 const js=body.replaceAll('super.','native.');
 const fn=new Function('report','native','helpers',...names,js);
 const result=fn(null,native,helpers,...actualArgs);assert.deepEqual(forwarded,actualArgs,name+' preserves arguments');
 if(name.startsWith('read') && name!=='readBytes')assert.equal(result,123,name+' preserves native return');
}
for(const name of ['writeBytes','writeUTF','writeUTFBytes','writeMultiByte','readObject','readBytes'])assert.ok(src.includes('if(report==null)'+(name.startsWith('read')&&name!=='readBytes'?'return ':'{')+'super.'+name),'release bypass missing '+name);
console.log('PASS: production socket release fast paths preserve every native argument and read result without diagnostic signatures, string copies or counter work.');
