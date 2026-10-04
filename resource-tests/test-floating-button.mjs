import {readFileSync} from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const src=readFileSync(new URL('../src/br/davi/narutoair/compat/FloatingButton.as',import.meta.url),'utf8');
let body=src.slice(src.indexOf('  private function down('),src.indexOf('  private function resize('));
body=body.replace(/private function (\w+)\(e:(MouseEvent|Event)\):void/g,'function $1(e)').replace(/:Number/g,'');
const c={target:{x:700,y:40,width:160,height:42},host:{stageWidth:1000,stageHeight:600},active:false,moved:false,Math,saves:0,changes:0,save(){this.saves++;},changed(){c.changes++;}};
// Bind the persistence observer directly; event logic is extracted from production.
c.save=()=>c.saves++;vm.createContext(c);vm.runInContext(body,c);
function event(x,y){return {stageX:x,stageY:y,stopped:false,stopPropagation(){},stopImmediatePropagation(){this.stopped=true;}};}
c.down(event(720,50));c.move(event(724,52));let e=event(724,52);c.up(e);c.click(e);assert.equal(e.stopped,false);assert.equal(c.saves,0);
c.down(event(720,50));c.move(event(1200,900));assert.equal(c.target.x,840);assert.equal(c.target.y,558);
e=event(1200,900);c.up(e);assert.equal(c.saves,1);assert.equal(e.stopped,true);let click=event(1200,900);c.click(click);assert.equal(click.stopped,true);
c.down(event(850,560));c.move(event(-500,-500));assert.equal(c.target.x,0);assert.equal(c.target.y,0);c.cancel({});assert.equal(c.active,false);assert.equal(c.saves,2);
console.log('PASS: production AIR floating-button handlers preserve taps, suppress drag clicks, clamp to screen and finish a drag on deactivate. Node model, not Android.');
