package br.davi.narutoair.compat {
 import flash.display.Sprite;
 import flash.display.Stage;
 import flash.display.Shape;
 import flash.display.DisplayObject;
 import flash.display.InteractiveObject;
 import flash.events.Event;
 import flash.events.MouseEvent;
 import flash.events.KeyboardEvent;
 import flash.events.TextEvent;
 import flash.geom.Point;
 import flash.text.TextField;
 import flash.text.TextFormat;
 import flash.utils.getTimer;
 import flash.net.SharedObject;
 import flash.desktop.Clipboard;
 import flash.desktop.ClipboardFormats;
 import flash.media.SoundMixer;
 /** Separate app UI. Pointer sends real Android input, not fake client load events. */
 public final class MobileControls extends Sprite {
  private var host:Stage,send:Function,restart:Function,account:Function,recharge:Function;
  private var fpsHUD:Sprite=new Sprite(),fpsText:TextField=new TextField(),hudButton:TextField;
  private var hudCorner:int=0,hudPrefs:SharedObject;
  private var lastHUD:String="",lastGraphicsUpdate:int=0;
  private var floating:FloatingButton;
  private var graphicsProfile:GraphicsProfile,graphicsPanel:Sprite=new Sprite(),graphicsInfo:TextField;
  private var fpsButton:TextField,scaleButton:TextField,colorButton:TextField;
  private var modButton:TextField,styleButton:TextField,modPrefs:SharedObject,modApply:Function;
  private var modEnabled:Boolean=true,modReady:Boolean=false,modStyle:int=0,modChanged:Boolean=false;
  private var toggle:TextField,panel:Sprite=new Sprite(),pad:Sprite=new Sprite(),cursor:Shape=new Shape(),mark:Shape=new Shape();
  private var mouseButton:TextField,markButton:TextField,dragButton:TextField,hint:TextField,soundButton:TextField;
  private var mouseEnabled:Boolean=false,marks:Boolean=false,dragging:Boolean=false,tracking:Boolean=false;
  private var px:Number=100,py:Number=100,lx:Number,ly:Number,total:Number=0,markTime:int=0;
  private var keyboardPanel:Sprite=new Sprite(),editor:TextField=new TextField();
  private var lastInput:TextField;
  public function MobileControls(stage:Stage,dispatch:Function,onRestart:Function,onAccount:Function=null,profile:GraphicsProfile=null,onRecharge:Function=null,onMod:Function=null,audioReport:Function=null) {
   host=stage;send=dispatch;restart=onRestart;account=onAccount;graphicsProfile=profile;recharge=onRecharge;mouseChildren=true;
   modApply=onMod;
   try{modPrefs=SharedObject.getLocal("narutoKaguya");if(modPrefs.data.enabled!==undefined)modEnabled=Boolean(modPrefs.data.enabled);if(modPrefs.data.style!==undefined)modStyle=Math.max(0,Math.min(4,int(modPrefs.data.style)));}catch(modPrefError:Error){}
   try{modReady=modApply!=null && modApply(modEnabled,modStyle)===true;}catch(modError:Error){modReady=false;}
   try{hudPrefs=SharedObject.getLocal("narutoFPSHUD");if(hudPrefs.data.corner!==undefined)hudCorner=Math.max(0,Math.min(4,int(hudPrefs.data.corner)));}catch(prefError:Error){}
   fpsHUD.mouseEnabled=false;fpsHUD.mouseChildren=false;fpsHUD.alpha=.78;fpsHUD.graphics.beginFill(0x101820,.7);fpsHUD.graphics.drawRoundRect(0,0,72,22,5);fpsHUD.graphics.endFill();
   fpsText.defaultTextFormat=new TextFormat("_sans",12,0xffffff);fpsText.width=70;fpsText.height=21;fpsText.x=3;fpsText.y=1;fpsText.selectable=false;fpsText.mouseEnabled=false;fpsHUD.addChild(fpsText);addChild(fpsHUD);
   toggle=button(this,"CONTROLES",0,0,160,function(e:MouseEvent):void{if(graphicsPanel.visible){graphicsPanel.visible=false;panel.visible=false;}else panel.visible=!panel.visible;layout();});
   mouseButton=button(panel,"MOUSE: OFF",12,12,165,toggleMouse);
   button(panel,"TECLADO",185,12,145,keyboard);
   markButton=button(panel,"CLIQUE: OFF",12,64,165,function(e:MouseEvent):void{marks=!marks;markButton.text=marks?"CLIQUE: ON":"CLIQUE: OFF";if(!marks)mark.visible=false;});
   button(panel,"REINICIAR",185,64,145,function(e:MouseEvent):void{restart();});
   button(panel,"ENTER",12,116,98,function(e:MouseEvent):void{key(66);});
   button(panel,"ESC",122,116,98,function(e:MouseEvent):void{key(111);});
   button(panel,"TAB",232,116,98,function(e:MouseEvent):void{key(61);});
   button(panel,"SERVIDORES",12,168,165,function(e:MouseEvent):void{restart();});
   button(panel,"TROCAR CONTA",185,168,145,function(e:MouseEvent):void{if(account!=null)account();});
   button(panel,"IMAGEM / FPS",12,220,318,function(e:MouseEvent):void {panel.visible=false;graphicsPanel.visible=true;layout();});
   button(panel,"RECARGA OFICIAL",12,272,318,function(e:MouseEvent):void{panel.visible=false;if(recharge!=null)recharge();});
   soundButton=button(panel,AudioSession.enabled?"SOM: ON":"SOM: OFF",12,324,153,function(e:MouseEvent):void {AudioSession.toggle();soundButton.text=AudioSession.enabled?"SOM: ON":"SOM: OFF";});
   button(panel,"TESTAR SOM",177,324,153,function(e:MouseEvent):void {try{hint.text=AudioSession.test()?"Tom curto enviado. Volume de midia controla o som.":"Ative SOM para testar.";}catch(error:Error){hint.text="Nao foi possivel iniciar o teste de som.";}});
   button(panel,"COPIAR DIAGNOSTICO DE AUDIO",12,376,318,function(e:MouseEvent):void {
    try {
     var text:String="Naruto Online Mobile 1.3.18 — audio\nSOM="+AudioSession.enabled+"; volume global="+SoundMixer.soundTransform.volume+"\n\nSound externo:\n"+BrowserResources.audioSummary()+"\n\nTransporte:\n"+(audioReport!=null?String(audioReport()):"Indisponivel");
     var copied:Boolean=Clipboard.generalClipboard.setData(ClipboardFormats.TEXT_FORMAT,text);
     hint.text=copied?"Diagnostico copiado. Cole na conversa para analisar a musica.":"Nao foi possivel copiar o diagnostico.";
    }catch(copyError:Error){hint.text="Nao foi possivel copiar o diagnostico.";}
   });
   hint=new TextField();hint.defaultTextFormat=new TextFormat("_sans",15,0xffffff);hint.width=320;hint.height=48;hint.multiline=true;hint.wordWrap=true;hint.x=12;hint.y=428;
   hint.text="Arraste CONTROLES para mover. Mouse: deslize no touchpad.";hint.selectable=false;panel.addChild(hint);
   panel.visible=false;addChild(panel);
   fpsButton=button(graphicsPanel,"FPS: 60 / PADRAO",12,12,318,function(e:MouseEvent):void{if(!graphicsProfile)return;var choices:Array=[60,90,120,0];graphicsProfile.setFPSChoice(choices[(choices.indexOf(graphicsProfile.selectedFPS)+1)%choices.length]);updateGraphics();});
   scaleButton=button(graphicsPanel,"FSR 1: TESTAR / OPCIONAL",12,64,318,function(e:MouseEvent):void{if(graphicsProfile)graphicsProfile.setFSR(!graphicsProfile.fsrEnabled);updateGraphics();});
   colorButton=button(graphicsPanel,"CORES: SEMPRE ATIVAS",12,116,318,function(e:MouseEvent):void{});colorButton.mouseEnabled=false;
   hudButton=button(graphicsPanel,"CONTADOR: SUPERIOR ESQUERDO",12,168,318,function(e:MouseEvent):void{hudCorner=(hudCorner+1)%5;try{hudPrefs.data.corner=hudCorner;hudPrefs.flush();}catch(prefError:Error){}layout();updateGraphics();});
   hudButton.defaultTextFormat=new TextFormat("_sans",15,0xffffff,true);
   modButton=button(graphicsPanel,"KAGUYA: ATIVO",12,220,153,function(e:MouseEvent):void{setMod(!modEnabled,modStyle);});
   styleButton=button(graphicsPanel,"VENTO",177,220,153,function(e:MouseEvent):void{setMod(modEnabled,(modStyle+1)%5);});
   modButton.defaultTextFormat=new TextFormat("_sans",15,0xffffff,true);
   graphicsInfo=new TextField();graphicsInfo.defaultTextFormat=new TextFormat("_sans",13,0xffffff);graphicsInfo.width=320;graphicsInfo.height=170;graphicsInfo.x=12;graphicsInfo.y=272;graphicsInfo.multiline=true;graphicsInfo.wordWrap=true;graphicsInfo.selectable=false;graphicsPanel.addChild(graphicsInfo);
   button(graphicsPanel,"VOLTAR",12,450,318,function(e:MouseEvent):void{graphicsPanel.visible=false;panel.visible=true;layout();});
   graphicsPanel.visible=false;addChild(graphicsPanel);updateGraphics();
   pad.graphics.beginFill(0x18212a,.90);pad.graphics.lineStyle(2,0xffb43b);pad.graphics.drawRoundRect(0,0,260,120,12);pad.graphics.endFill();
   var label:TextField=new TextField();label.defaultTextFormat=new TextFormat("_sans",16,0xffffff);label.text="TOUCHPAD";label.width=230;label.height=30;label.x=15;label.y=40;label.mouseEnabled=false;pad.addChild(label);
   pad.addEventListener(MouseEvent.MOUSE_DOWN,startPad);pad.visible=false;addChild(pad);
   dragButton=button(this,"SEGURAR: OFF",0,0,160,function(e:MouseEvent):void {
    dragging=!dragging;dragButton.text=dragging?"SEGURAR: ON":"SEGURAR: OFF";pointer(dragging?"down":"up");
   });dragButton.visible=false;
   cursor.graphics.lineStyle(2,0xffffff);cursor.graphics.beginFill(0xffa51d);cursor.graphics.moveTo(0,0);cursor.graphics.lineTo(0,24);cursor.graphics.lineTo(7,18);cursor.graphics.lineTo(12,30);cursor.graphics.lineTo(18,27);cursor.graphics.lineTo(12,15);cursor.graphics.lineTo(24,15);cursor.graphics.lineTo(0,0);cursor.graphics.endFill();
   cursor.visible=false;addChild(cursor);
   mark.graphics.lineStyle(3,0xffcc00);mark.graphics.drawCircle(0,0,16);mark.graphics.moveTo(-24,0);mark.graphics.lineTo(24,0);mark.graphics.moveTo(0,-24);mark.graphics.lineTo(0,24);mark.visible=false;addChild(mark);
   editor.type="input";editor.needsSoftKeyboard=true;editor.defaultTextFormat=new TextFormat("_sans",22,0xffffff);editor.background=true;editor.backgroundColor=0x1a1a1a;editor.border=true;editor.borderColor=0xffb43b;editor.width=400;editor.height=46;editor.x=12;editor.y=12;editor.maxChars=2000;
   keyboardPanel.addChild(editor);button(keyboardPanel,"ENVIAR",424,12,115,commitText);button(keyboardPanel,"FECHAR",550,12,115,function(e:MouseEvent):void{keyboardPanel.visible=false;host.focus=lastInput;});
   keyboardPanel.visible=false;addChild(keyboardPanel);
   host.addEventListener(MouseEvent.MOUSE_DOWN,rememberInput,true,100);
   host.addEventListener(MouseEvent.MOUSE_MOVE,movePad);
   host.addEventListener(MouseEvent.MOUSE_UP,endPad);
   host.addEventListener(Event.RESIZE,layout);
   addEventListener(Event.ENTER_FRAME,frame);
   px=host.stageWidth/2;py=host.stageHeight/2;toggle.x=Math.max(0,host.stageWidth-toggle.width-18);toggle.y=15;layout();
   floating=new FloatingButton(toggle,host,"controls",layout);
  }
  public function stop():void {
   if(dragging)pointer("up");dragging=false;tracking=false;dragButton.text="SEGURAR: OFF";
   if(own(host.focus))host.focus=null;lastInput=null;editor.text="";keyboardPanel.visible=false;panel.visible=false;graphicsPanel.visible=false;visible=false;
  }
  private function button(owner:Sprite,text:String,x:Number,y:Number,w:Number,callback:Function):TextField {
   var b:TextField=new TextField();b.defaultTextFormat=new TextFormat("_sans",18,0xffffff,true);b.text=text;b.selectable=false;b.background=true;b.backgroundColor=0x303943;b.width=w;b.height=42;b.x=x;b.y=y;b.addEventListener(MouseEvent.CLICK,callback);owner.addChild(b);return b;
  }
  private function layout(event:Event=null):void {
   panel.graphics.clear();panel.graphics.beginFill(0x111922,.95);panel.graphics.drawRoundRect(0,0,344,483,12);panel.graphics.endFill();
   panel.scaleX=panel.scaleY=Math.min(1,Math.max(.1,(host.stageHeight-8)/483),Math.max(.1,(host.stageWidth-8)/344));
   panel.x=Math.max(0,Math.min(host.stageWidth-344,toggle.x+toggle.width-344));
   panel.y=toggle.y+toggle.height+8;
   if(panel.y+panel.height>host.stageHeight)panel.y=Math.max(0,toggle.y-panel.height-8);
   graphicsPanel.graphics.clear();graphicsPanel.graphics.beginFill(0x111922,.95);graphicsPanel.graphics.drawRoundRect(0,0,344,504,12);graphicsPanel.graphics.endFill();
   graphicsPanel.scaleX=graphicsPanel.scaleY=Math.min(1,Math.max(.1,(host.stageHeight-8)/504),Math.max(.1,(host.stageWidth-8)/344));
   graphicsPanel.x=Math.max(0,Math.min(host.stageWidth-graphicsPanel.width,panel.x));graphicsPanel.y=toggle.y+toggle.height+8;if(graphicsPanel.y+graphicsPanel.height>host.stageHeight)graphicsPanel.y=Math.max(0,toggle.y-graphicsPanel.height-8);
   pad.x=Math.max(0,host.stageWidth-285);pad.y=Math.max(300,host.stageHeight-200);
   dragButton.x=pad.x;dragButton.y=pad.y+130;
   keyboardPanel.x=Math.max(0,(host.stageWidth-680)/2);keyboardPanel.y=Math.max(0,host.stageHeight/2-50);
   fpsHUD.visible=hudCorner<4;fpsHUD.x=(hudCorner==1||hudCorner==3)?Math.max(0,host.stageWidth-78):6;fpsHUD.y=hudCorner>=2?Math.max(0,host.stageHeight-28):6;
   px=Math.min(host.stageWidth-1,Math.max(0,px));py=Math.min(host.stageHeight-1,Math.max(0,py));cursor.x=px;cursor.y=py;
  }
  private function own(target:Object):Boolean {return target is DisplayObject && (target===this || contains(target as DisplayObject));}
  private function rememberInput(e:MouseEvent):void {
   if(!visible || own(e.target))return;
   if(e.target is TextField && TextField(e.target).type=="input")lastInput=TextField(e.target);
   if(marks){mark.x=e.stageX;mark.y=e.stageY;markTime=getTimer();mark.alpha=1;mark.visible=true;}
  }
  private function toggleMouse(e:MouseEvent):void {
   if(dragging){pointer("up");dragging=false;dragButton.text="SEGURAR: OFF";}
   mouseEnabled=!mouseEnabled;mouseButton.text=mouseEnabled?"MOUSE: ON":"MOUSE: OFF";
   pad.visible=mouseEnabled;cursor.visible=mouseEnabled;dragButton.visible=mouseEnabled;panel.visible=false;
  }
  private function startPad(e:MouseEvent):void {tracking=true;lx=e.stageX;ly=e.stageY;total=0;e.stopPropagation();}
  private function movePad(e:MouseEvent):void {
   if(!visible || !tracking || !(e.target===pad || (e.target is DisplayObject && pad.contains(e.target as DisplayObject))))return;
   var dx:Number=e.stageX-lx,dy:Number=e.stageY-ly;lx=e.stageX;ly=e.stageY;total+=Math.abs(dx)+Math.abs(dy);
   px=Math.min(host.stageWidth-1,Math.max(0,px+dx*1.4));py=Math.min(host.stageHeight-1,Math.max(0,py+dy*1.4));if(px>=pad.x-24 && px<=pad.x+pad.width && py>=pad.y-24 && py<=pad.y+pad.height)px=Math.max(0,pad.x-28);
   cursor.x=px;cursor.y=py;
   pointer(dragging?"drag":"hover");
  }
  private function endPad(e:MouseEvent):void {if(!tracking)return;tracking=false;if(total<12 && !dragging)pointer("click");}
  private function pointer(action:String):void {
   var ok:Boolean=send(action,px/Math.max(1,host.stageWidth-1),py/Math.max(1,host.stageHeight-1),0);
   if(!ok){hint.text="Mouse indisponivel. Use o toque direto.";return;}
   if(marks && (action=="click"||action=="down")){mark.x=px;mark.y=py;markTime=getTimer();mark.alpha=1;mark.visible=true;}
  }
  private function keyboard(e:MouseEvent):void {
   var field:TextField=host.focus as TextField;
   if(field && !own(field) && field.type=="input")lastInput=field;
   panel.visible=false;
   if(lastInput && lastInput.stage){lastInput.needsSoftKeyboard=true;host.focus=lastInput;lastInput.requestSoftKeyboard();return;}
   keyboardPanel.visible=true;host.focus=editor;editor.requestSoftKeyboard();
  }
  private function commitText(e:MouseEvent):void {
   if(lastInput && lastInput.stage){host.focus=lastInput;lastInput.replaceSelectedText(editor.text);lastInput.dispatchEvent(new Event(Event.CHANGE,true));editor.text="";keyboardPanel.visible=false;}
   else {editor.text="";keyboardPanel.visible=false;panel.visible=true;hint.text="Toque primeiro no campo de texto do jogo; depois ative TECLADO.";}
  }
  private function key(code:int):void {if(lastInput && lastInput.stage)host.focus=lastInput;send("key",0,0,code);}
  private function setMod(enabled:Boolean,style:int):void {
   var applied:Boolean=false;try{applied=modApply!=null && modApply(enabled,style)===true;}catch(modError:Error){}
   modReady=applied;
   if(applied){modEnabled=enabled;modStyle=style;modChanged=true;try{modPrefs.data.enabled=enabled;modPrefs.data.style=style;modPrefs.flush();}catch(modPrefError:Error){}}
   updateGraphics();
  }
  private function updateGraphics():void {
   if(!graphicsProfile)return;
   hudButton.text="CONTADOR: "+["SUPERIOR ESQUERDO","SUPERIOR DIREITO","INFERIOR ESQUERDO","INFERIOR DIREITO","OCULTO"][hudCorner];
   fpsButton.text=graphicsProfile.selectedFPS==0?"FPS: MAX. DA TELA":("FPS: "+graphicsProfile.selectedFPS+(graphicsProfile.selectedFPS==60?" / PADRAO":" / OPCIONAL"));
   scaleButton.text=graphicsProfile.fsrEnabled?"FSR 1: DESLIGAR TESTE":"FSR 1: TESTAR / OPCIONAL";
   colorButton.text="CORES MELHORADAS: ATIVAS";
   modButton.text=modReady?(modEnabled?"KAGUYA: ON":"KAGUYA: OFF"):"INDISPONIVEL";
   styleButton.text=["VENTO","FOGO","RAIO","AGUA","TERRA"][modStyle];
   graphicsInfo.text="FPS AIR: "+(isNaN(graphicsProfile.fps)?"medindo...":graphicsProfile.fps.toFixed(1))+"\n"+graphicsProfile.status+"\n"+graphicsProfile.timingStatus+"\n"+graphicsProfile.fsrInfo+"\nSem geracao de quadros.\n"+(modChanged?"REINICIAR aplica a troca por completo.":"Kaguya: retrato principal + sons.");
  }
  private function frame(e:Event):void {var hud:String="FPS "+(graphicsProfile && !isNaN(graphicsProfile.fps)?Math.round(graphicsProfile.fps):"--");if(hud!=lastHUD){fpsText.text=hud;lastHUD=hud;}if(graphicsPanel.visible && getTimer()-lastGraphicsUpdate>=1000){lastGraphicsUpdate=getTimer();updateGraphics();}if(mark.visible){mark.alpha=Math.max(0,1-(getTimer()-markTime)/700);if(mark.alpha==0)mark.visible=false;}if(visible && parent && parent.getChildIndex(this)!=parent.numChildren-1)parent.setChildIndex(this,parent.numChildren-1);}
 }
}
