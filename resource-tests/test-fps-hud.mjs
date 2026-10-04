import {readFileSync} from 'node:fs';import assert from 'node:assert/strict';import vm from 'node:vm';
const src=readFileSync(new URL('../src/br/davi/narutoair/compat/MobileControls.as',import.meta.url),'utf8');
assert.ok(src.includes('fpsHUD.mouseEnabled=false;fpsHUD.mouseChildren=false;'));
assert.ok(src.includes('fpsText.mouseEnabled=false;'));
const position=src.match(/fpsHUD.visible=hudCorner<4;fpsHUD.x=.*?fpsHUD.y=.*?;/)[0];
const c={fpsHUD:{},hudCorner:0,host:{stageWidth:1604,stageHeight:720},Math};vm.createContext(c);
for(const [corner,x,y,visible] of [[0,6,6,true],[1,1526,6,true],[2,6,692,true],[3,1526,692,true],[4,6,692,false]]){c.hudCorner=corner;vm.runInContext(position,c);assert.deepEqual({...c.fpsHUD},{visible,x,y});}
c.host={stageWidth:1283,stageHeight:576};c.hudCorner=3;vm.runInContext(position,c);assert.equal(c.fpsHUD.x,1205);assert.equal(c.fpsHUD.y,548);
const update=src.match(/var hud:String="FPS ".*?if\(hud!=lastHUD\)\{fpsText.text=hud;lastHUD=hud;\}/)[0].replace('var hud:String','var hud');c.fpsText={};c.lastHUD='';c.graphicsProfile={fps:29.8};vm.runInContext(update,c);assert.equal(c.fpsText.text,'FPS 30','HUD must show measured FPS');c.graphicsProfile.fps=NaN;vm.runInContext(update,c);assert.equal(c.fpsText.text,'FPS --');
assert.ok(src.includes('getTimer()-lastGraphicsUpdate>=1000'),'settings text should not reflow every frame');
console.log('PASS: FPS HUD shows observed rate, offers four corners/off, stays within resized Stage, and both HUD/children disable mouse hit testing. Source/Node model, not compositor verification.');
