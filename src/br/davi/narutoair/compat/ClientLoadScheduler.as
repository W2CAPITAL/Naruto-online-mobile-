package br.davi.narutoair.compat {
 import flash.display.Stage;
 import flash.events.Event;
 import flash.utils.Dictionary;
 import flash.utils.getTimer;
 /** Pace downloaded code imports; no client protocol or fabricated loader events. */
 public final class ClientLoadScheduler {
  private static var host:Stage;
  private static var downloads:Array=[],imports:Array=[];
  private static var active:Dictionary=new Dictionary();
  private static var count:int=0,queuedBytes:Number=0;
  private static var enabled:Boolean=false,draining:Boolean=false;
  private static var foreground:Boolean=true,lastFrameTime:int=0,deferredFrames:int=0;
  private static const MAX_DOWNLOADS:int=2;
  private static const IMPORT_WATERMARK:Number=8*1024*1024;
  public static function configure(stage:Stage):void {
   disable();host=stage;enabled=true;foreground=true;
   if(host){host.addEventListener(Event.ACTIVATE,activate);host.addEventListener(Event.DEACTIVATE,deactivate);}
  }
  public static function disable():void {
   enabled=false;
   if(host){host.removeEventListener(Event.ENTER_FRAME,frame);host.removeEventListener(Event.ACTIVATE,activate);host.removeEventListener(Event.DEACTIVATE,deactivate);}
   host=null;downloads=[];imports=[];active=new Dictionary();count=0;queuedBytes=0;
   lastFrameTime=deferredFrames=0;foreground=true;
  }
  public static function download(owner:Object,run:Function):void {
   downloads.push({owner:owner,run:run});drainDownloads();
  }
  public static function downloaded(owner:Object,size:uint,run:Function):void {
   // Record backpressure before releasing a download slot.
   imports.push({owner:owner,size:size,run:run});queuedBytes+=size;
   if(host)host.addEventListener(Event.ENTER_FRAME,frame,false,-20000);
   release(owner);drainDownloads();
  }
  private static function release(owner:Object):void {
   if(active[owner]){delete active[owner];count--;}
  }
  public static function cancel(owner:Object):void {
   for(var i:int=downloads.length-1;i>=0;i--)if(downloads[i].owner===owner)downloads.splice(i,1);
   for(i=imports.length-1;i>=0;i--)if(imports[i].owner===owner){queuedBytes-=imports[i].size;imports.splice(i,1);}
   release(owner);drainDownloads();
   if(imports.length==0)detachFrame();
  }
  private static function drainDownloads():void {
   if(draining || !enabled)return;draining=true;
   try {
    while(enabled && count<MAX_DOWNLOADS && queuedBytes<IMPORT_WATERMARK && downloads.length>0){
     var task:Object=downloads.shift();active[task.owner]=true;count++;
     task.run();
    }
   } finally {draining=false;}
  }
  private static function activate(event:Event):void {foreground=true;lastFrameTime=deferredFrames=0;}
  private static function deactivate(event:Event):void {foreground=false;lastFrameTime=deferredFrames=0;}
  private static function detachFrame():void {
   if(host)host.removeEventListener(Event.ENTER_FRAME,frame);lastFrameTime=deferredFrames=0;
  }
  private static function frame(event:Event):void {
   if(!enabled || !foreground)return;
   if(imports.length>0){
    var now:int=getTimer(),delta:int=lastFrameTime>0?now-lastFrameTime:0;
    lastFrameTime=now;
    var budget:Number=host?1000/Math.max(1,host.frameRate):1000/60;
    // Let gameplay recover after a long frame. Never indefinitely stall bootstrap.
    if(delta>budget*1.5 && deferredFrames<2){deferredFrames++;return;}
    deferredFrames=0;
    var task:Object=imports.shift();queuedBytes-=task.size;
    try {task.run();} finally {drainDownloads();}
   }
   if(imports.length==0)detachFrame();
  }
 }
}
