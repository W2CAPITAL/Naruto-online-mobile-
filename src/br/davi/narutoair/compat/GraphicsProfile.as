package br.davi.narutoair.compat {
 import flash.display.Stage;
 import flash.display.DisplayObject;
 import flash.display.DisplayObjectContainer;
 import flash.events.Event;
 import flash.geom.ColorTransform;
 import flash.utils.Dictionary;
 import flash.utils.getTimer;
 import flash.net.SharedObject;
 /** Display-paced release profile. Observed AIR callbacks are not presented GPU frames. */
 public final class GraphicsProfile {
  private static var active:GraphicsProfile;
  private var host:Stage,owner:DisplayObjectContainer,baseline:Array,nativeScale:Function,nativeRate:Function;
  private var fpsPrefs:SharedObject,fpsChoice:Number=60;
  private var nativeFsr:Function,fsrWanted:Boolean=false,fsrValue:String="FSR 1: opcional / desligado",fsrEpoch:int=0;
  private var originalColors:Dictionary=new Dictionary(true);
  private var running:Boolean=false,foreground:Boolean=true,frames:int=0,sampleStart:int=0;
  private var scaleValue:int=80,contrastValue:Boolean=true;
  private var targetValue:Number=60;
  private var measured:Number=NaN,statusValue:String="Aguardando jogo";
  private var confirmedWidth:int=0,confirmedHeight:int=0,verifyAt:int=0,retryAt:int=0;
  private var failures:int=0,slowSamples:int=0,fastSamples:int=0;
  private var capRepairs:uint=0;
  private var displayStatus:String="Taxa da tela indisponivel";
  public function GraphicsProfile(stage:Stage,app:DisplayObjectContainer,owned:Array,scale:Function,logger:Function,rate:Function=null,fsr:Function=null) {
   host=stage;owner=app;baseline=owned;nativeScale=scale;nativeRate=rate;nativeFsr=fsr;active=this;
   // New opt-in preference: old maximum-refresh behavior never migrates as consent.
   try{fpsPrefs=SharedObject.getLocal("narutoFrameLimitV1");if(fpsPrefs.data.choice is Number && validChoice(Number(fpsPrefs.data.choice)))fpsChoice=Number(fpsPrefs.data.choice);}catch(prefError:Error){}
   targetValue=fpsChoice>0?fpsChoice:60;
   host.frameRate=targetValue;
   // Priority orders listeners on this dispatcher, not other MovieClips.
   host.addEventListener(Event.ENTER_FRAME,frame,false,-10000);
   host.addEventListener(Event.EXIT_FRAME,finishFrame,false,-10000);
   host.addEventListener(Event.ACTIVATE,activate);
   host.addEventListener(Event.DEACTIVATE,deactivate);
  }
  public static function enforceTarget():void {if(active && active.running && active.foreground && active.host.frameRate!=active.targetValue)active.host.frameRate=active.targetValue;}
  public function get scalePercent():int{return scaleValue;}
  public function get targetFPS():Number{return targetValue;}
  public function get selectedFPS():Number{return fpsChoice;}
  public function get fsrEnabled():Boolean{return fsrWanted;}
  public function get fsrInfo():String{return fsrValue;}
  public function setFSR(enabled:Boolean):Boolean {
   if(enabled && (!running || !foreground || nativeFsr==null))return false;
   if(enabled)for(var i:int=0;i<host.stage3Ds.length;i++)if(host.stage3Ds[i].context3D!=null){fsrValue="FSR indisponivel com Stage3D separado";return false;}
   // Set before dispatch so synchronous failures are not overwritten by this call.
   fsrEpoch=fsrEpoch==2147483647?1:fsrEpoch+1;fsrWanted=enabled;fsrValue=enabled?"FSR 1: iniciando teste GPU...":"FSR 1: opcional / desligado";
   try{if(nativeFsr!=null && nativeFsr(enabled,fsrEpoch)!==true){fsrWanted=false;fsrValue="FSR: ponte indisponivel";return false;}}catch(e:Error){fsrWanted=false;fsrValue="FSR: ponte indisponivel";return false;}
   return true;
  }
  public function fsrStatus(value:String):void {
   var separator:int=value.indexOf(":");if(separator<1 || value.substr(0,separator)!=String(fsrEpoch))return;value=value.substr(separator+1);
   if(value=="off"){fsrWanted=false;fsrValue="FSR 1: opcional / desligado";return;}
   if(!running || !fsrWanted)return;
   if(value.indexOf("fallback:")==0){fsrWanted=false;fsrValue=value.substr(9);return;}
   var parts:Array=value.split(":");
   if(parts[0]=="active" && parts.length==4)fsrValue="FSR 1 EASU + RCAS: "+parts[1]+" → "+parts[2]+"; saida "+parts[3]+" FPS";
  }
  private static function validChoice(choice:Number):Boolean{return choice==0 || choice==60 || choice==90 || choice==120;}
  public function setFPSChoice(choice:Number):Boolean {
   if(!validChoice(choice))return false;
   try{if(running && nativeRate!=null && nativeRate(choice)!==true)return false;}catch(rateError:Error){return false;}
   fpsChoice=choice;targetValue=choice>0?choice:60;displayStatus="Consultando taxa escolhida";
   try{fpsPrefs.data.choice=choice;fpsPrefs.flush();}catch(prefError:Error){}
   resetSample();if(running && foreground)host.frameRate=targetValue;return true;
  }
  public function get contrast():Boolean{return contrastValue;}
  public function get fps():Number{return measured;}
  public function get status():String{return statusValue;}
  public function get timingStatus():String{return "Alvo AIR: "+host.frameRate+"; ajustes: "+capRepairs+"\n"+displayStatus;}
  public function start():void {running=true;capRepairs=0;failures=retryAt=slowSamples=fastSamples=0;scaleValue=80;targetValue=fpsChoice>0?fpsChoice:60;displayStatus="Consultando taxa escolhida";resetSample();host.frameRate=targetValue;host.quality="medium";try{if(nativeRate!=null)nativeRate(fpsChoice);}catch(rateError:Error){}applyScale();updateColors();}
  public function stop():void {
   setFSR(false);
   if(running){running=false;nativeScale(100);}retryAt=verifyAt=0;restoreColors();resetSample();statusValue="Aguardando jogo";
   host.frameRate=60;
  }
  private function applyScale():void {
   confirmedWidth=confirmedHeight=verifyAt=retryAt=0;
   statusValue="Upscaling automatico: aplicando...";
   if(nativeScale(80)!==true)fallback("Ponte de imagem temporariamente indisponivel");
  }
  public function nativeStatus(value:String):void {
   if(!running)return;
   var parts:Array=value.split(":");
   if(parts[0]=="display" && parts.length==4){
    var actual:Number=Number(parts[1]),requested:Number=Number(parts[2]),choice:Number=Number(parts[3]);
    if(!isFinite(actual) || actual<=0 || actual>1000 || !isFinite(requested) || requested<=0 || requested>1000 || choice!=fpsChoice || !validChoice(choice) || (choice>0 && requested>choice))return;
    targetValue=requested;
    if(foreground)host.frameRate=targetValue;
    displayStatus="Tela consultada: "+actual.toFixed(1)+" Hz; alvo: "+requested.toFixed(1)+" FPS";return;
   }
   if(value=="error")fallback("Reaplicando perfil de imagem");
   else if(parts[0]=="active" && parts.length==6 && int(parts[1])==80) {
    confirmedWidth=int(parts[2]);confirmedHeight=int(parts[3]);verifyAt=getTimer()+3000;
    statusValue=parts[2]+"x"+parts[3]+" → "+parts[4]+"x"+parts[5];
   }
  }
  private function resetSample():void {frames=0;sampleStart=getTimer();measured=NaN;}
  private function activate(e:Event):void {foreground=true;resetSample();if(running)host.frameRate=targetValue;}
  private function deactivate(e:Event):void {foreground=false;setFSR(false);resetSample();}
  private function finishFrame(e:Event):void {
   if(!running || !foreground)return;
   // Frame scripts may lower the cap AFTER the Stage ENTER_FRAME listener.
   if(host.frameRate!=targetValue){capRepairs++;host.frameRate=targetValue;}
   if(host.quality!="medium")host.quality="medium";
  }
  private function frame(e:Event):void {
   if(!running || !foreground)return;
   if(host.frameRate!=targetValue)host.frameRate=targetValue;
   if(host.quality!="medium")host.quality="medium";
   frames++;
   var now:int=getTimer(),elapsed:int=now-sampleStart;
   if(elapsed<1000)return;
   measured=frames*1000/elapsed;frames=0;sampleStart=now;
   verifyRenderer(now);updateQuality();updateColors();
  }
  private function verifyRenderer(now:int):void {
   // A separate Stage3D surface must not be resized with the display-list surface.
   for(var i:int=0;i<host.stage3Ds.length;i++)if(host.stage3Ds[i].context3D!=null){
    if(fsrWanted)setFSR(false);
    if(confirmedWidth>0 || verifyAt>0){nativeScale(100);confirmedWidth=confirmedHeight=verifyAt=0;statusValue="Perfil ativo; superficie Stage3D independente";}
    return;
   }
   if(retryAt>0 && now>=retryAt){applyScale();return;}
   if(verifyAt>0 && now>=verifyAt && confirmedWidth>0 && confirmedHeight>0) {
    verifyAt=0;
    if(Math.abs(host.stageWidth-confirmedWidth)>2 || Math.abs(host.stageHeight-confirmedHeight)>2)
     fallback("Reaplicando tamanho do buffer do AIR");
    else failures=0;
   }
  }
  private function fallback(reason:String):void {
   confirmedWidth=confirmedHeight=verifyAt=0;nativeScale(100);failures++;
   // Keep upscaling requested, with bounded retries instead of saving OFF forever.
   retryAt=failures<=2?getTimer()+5000:0;statusValue=reason;
  }
  private function updateQuality():void {
   // Stable cost: do not promote antialiasing when a temporarily light scene runs faster.
   if(host.quality!="medium")host.quality="medium";
  }
  private function updateColors():void {
   for(var i:int=0;i<host.numChildren;i++){var child:DisplayObject=host.getChildAt(i);if(baseline.indexOf(child)<0)color(child);}
   for(i=0;i<owner.numChildren;i++){child=owner.getChildAt(i);if(child is flash.display.Loader)color(child);}
  }
  private function color(child:DisplayObject):void {
   if(originalColors[child]!=null)return;
   var original:ColorTransform=child.transform.colorTransform;originalColors[child]=original;
   var adjusted:ColorTransform=new ColorTransform(original.redMultiplier,original.greenMultiplier,original.blueMultiplier,original.alphaMultiplier,
    original.redOffset,original.greenOffset,original.blueOffset,original.alphaOffset);
   adjusted.concat(new ColorTransform(1.10,1.08,1.06,1,-9,-8,-7,0));child.transform.colorTransform=adjusted;
  }
  private function restoreColors():void {for(var target:Object in originalColors){try{DisplayObject(target).transform.colorTransform=originalColors[target];}catch(e:Error){}}originalColors=new Dictionary(true);}
  public function summary():String {return "fpsAlvo="+targetValue+"; fpsAIR="+(isNaN(measured)?"aguardando":measured.toFixed(1))+"; imagem="+statusValue+"; cores=ativas";}
 }
}
