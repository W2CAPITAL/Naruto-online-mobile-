import {readFileSync} from 'node:fs';import assert from 'node:assert/strict';import vm from 'node:vm';
const src=readFileSync(new URL('../src/br/davi/narutoair/compat/MobileControls.as',import.meta.url),'utf8');
const body=src.match(/private function setMod\(enabled:Boolean,style:int\):void \{([\s\S]*?)\n  \}/)[1].replace('var applied:Boolean','var applied').replace(/catch\((\w+):Error\)/g,'catch($1)');
let persisted=0,updates=0;
const c={modApply:(enabled,style)=>enabled===false && style===2,modReady:true,modEnabled:true,modStyle:0,modChanged:false,modPrefs:{data:{},flush(){persisted++;}},updateGraphics(){updates++;}};vm.createContext(c);
function apply(enabled,style){c.enabled=enabled;c.style=style;vm.runInContext(body,c);}
apply(false,2);assert.equal(c.modReady,true);assert.equal(c.modEnabled,false);assert.equal(c.modStyle,2);assert.equal(c.modChanged,true);assert.equal(persisted,1);assert.deepEqual({...c.modPrefs.data},{enabled:false,style:2});
apply(true,3);assert.equal(c.modReady,false);assert.equal(c.modEnabled,false);assert.equal(c.modStyle,2);assert.equal(persisted,1,'failed native configuration persisted');
c.modApply=()=>{throw Error('unavailable');};apply(true,4);assert.equal(c.modReady,false);assert.equal(persisted,1);assert.equal(updates,3);
assert.ok(src.includes('getLocal("narutoKaguya")'));
assert.ok(src.includes('modApply(modEnabled,modStyle)===true'));
assert.ok(src.includes('REINICIAR aplica a troca por completo.'));
assert.ok(!body.includes('restart()'),'changing cosmetics must not interrupt session');
const main=readFileSync(new URL('../src/NarutoAir.as',import.meta.url),'utf8');const wrapper=readFileSync(new URL('../wrapper-src/br/davi/narutoair/portal/PortalMarker.as',import.meta.url),'utf8');
assert.ok(main.includes('bridge.kaguyaMode(enabled,style)===true'));
assert.ok(wrapper.includes('context.call("kaguyaMode",String(enabled),String(style))'));
console.log('PASS: production Kaguya controls execute ON/OFF/style changes, persist only native success, handle unavailable provider and retain session.');
