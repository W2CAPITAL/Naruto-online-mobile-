package br.davi.narutoair.compat {
 import flash.display.InteractiveObject;
 import flash.display.Stage;
 import flash.events.Event;
 import flash.events.MouseEvent;
 import flash.net.SharedObject;
 /** Move app overlays without forwarding their drag as a click. */
 public final class FloatingButton {
  private var target:InteractiveObject,host:Stage,changed:Function,store:SharedObject,name:String;
  private var active:Boolean=false,moved:Boolean=false,sx:Number,sy:Number,bx:Number,by:Number;
  private var nx:Number,ny:Number;
  public function FloatingButton(button:InteractiveObject,stage:Stage,key:String,onMove:Function=null) {
   target=button;host=stage;name=key;changed=onMove;
   nx=target.x/Math.max(1,host.stageWidth-target.width);ny=target.y/Math.max(1,host.stageHeight-target.height);
   try {store=SharedObject.getLocal("narutoFloatingButtons");var saved:Object=store.data[name];
    if(saved && isFinite(Number(saved.x)) && isFinite(Number(saved.y))){nx=Number(saved.x);ny=Number(saved.y);}
   }catch(e:Error){}
   target.addEventListener(MouseEvent.MOUSE_DOWN,down,false,200);
   target.addEventListener(MouseEvent.CLICK,click,false,200);
   host.addEventListener(MouseEvent.MOUSE_MOVE,move,false,200);
   host.addEventListener(MouseEvent.MOUSE_UP,up,false,200);
   host.addEventListener(Event.RESIZE,resize);host.addEventListener(Event.DEACTIVATE,cancel);
   reposition();
  }
  public function reposition():void {
   nx=Math.min(1,Math.max(0,nx));ny=Math.min(1,Math.max(0,ny));
   target.x=nx*Math.max(0,host.stageWidth-target.width);target.y=ny*Math.max(0,host.stageHeight-target.height);
   if(changed!=null)changed();
  }
  private function down(e:MouseEvent):void {active=true;moved=false;sx=e.stageX;sy=e.stageY;bx=target.x;by=target.y;e.stopPropagation();}
  private function move(e:MouseEvent):void {
   if(!active)return;
   var dx:Number=e.stageX-sx,dy:Number=e.stageY-sy;
   if(!moved && dx*dx+dy*dy<64)return;
   moved=true;target.x=Math.min(Math.max(0,host.stageWidth-target.width),Math.max(0,bx+dx));
   target.y=Math.min(Math.max(0,host.stageHeight-target.height),Math.max(0,by+dy));
   nx=target.x/Math.max(1,host.stageWidth-target.width);ny=target.y/Math.max(1,host.stageHeight-target.height);
   if(changed!=null)changed();e.stopImmediatePropagation();
  }
  private function up(e:MouseEvent):void {if(!active)return;active=false;if(moved){save();e.stopImmediatePropagation();}}
  private function click(e:MouseEvent):void {if(moved){e.stopImmediatePropagation();moved=false;}}
  private function cancel(e:Event):void {if(active && moved)save();active=false;}
  private function resize(e:Event):void {reposition();}
  private function save():void {try{if(store){store.data[name]={x:nx,y:ny};store.flush();}}catch(e:Error){}}
 }
}
