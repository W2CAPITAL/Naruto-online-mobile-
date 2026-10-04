import br.davi.narutoair.portal.*;
import com.adobe.fre.*;
import android.view.*;
public class GraphicsTest {
 static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
 static FREObject[] scale(String value){return new FREObject[]{new FREObject(value)};}
 static MotionEvent pointer(float x,float y){MotionEvent e=MotionEvent.obtain(1,2,0,x,y,0);e.x2=x/2;e.y2=y/2;return e;}
 public static void main(String[] args)throws Exception {
  InputTest.Context c=new InputTest.Context();c.activity=new InputTest.Activity();c.activity.window=new InputTest.Window();c.activity.window.decor=new InputTest.Group();
  com.adobe.air.AIRWindowSurfaceView v=new com.adobe.air.AIRWindowSurfaceView();c.activity.window.decor.child=v;
  FREFunction configure=GraphicsBridge.function(),rate=GraphicsBridge.fpsFunction();
  check(new MobilePortalContext().getFunctions().containsKey("graphicsFPS"),"FPS bridge registration");
  check(configure.call(c,scale("80")).value.equals(true),"request not queued");check(!v.holder.fixed,"surface touched off UI thread");c.activity.drain();
  check(v.holder.bw==1283 && v.holder.bh==576,"80% buffer");check(v.logicalWidth==1283 && v.logicalHeight==576 && v.panning==0,"AIR misclassified buffer height as keyboard panning");check(v.holder.surface.requestedFPS==60f,"native default must be 60");check(v.holder.callbacks.size()==1,"missing native callback");
  check(v.holder.bw*v.holder.bh<1604*720*.65,"render pixel reduction absent");
  check(c.activity.window.attrs.preferredDisplayModeId==1 && c.activity.window.attrs.preferredRefreshRate==60,"native default must not select highest display mode");
  int resizes=v.holder.fixedCalls,hints=v.holder.surface.frameRateCalls;
  rate.call(c,scale("90"));c.activity.drain();check(v.holder.surface.requestedFPS==90 && c.activity.window.attrs.preferredDisplayModeId==2,"90 opt-in");
  rate.call(c,scale("120"));c.activity.drain();check(v.holder.surface.requestedFPS==120 && c.activity.window.attrs.preferredDisplayModeId==3,"120 opt-in");
  rate.call(c,scale("0"));c.activity.drain();check(v.holder.surface.requestedFPS==120,"MAX opt-in");
  rate.call(c,scale("60"));c.activity.drain();check(v.holder.surface.requestedFPS==60 && c.activity.window.attrs.preferredDisplayModeId==1,"return to default");
  check(v.holder.fixedCalls==resizes,"FPS choice resized game buffer");
  rate.call(c,scale("60"));c.activity.drain();check(v.holder.surface.frameRateCalls==hints+3,"redundant FPS hint repeated");
  rate.call(c,scale("0"));c.activity.drain();
  int changes=c.activity.window.updates;configure.call(c,scale("80"));c.activity.drain();check(c.activity.window.updates==changes,"same preference reapplied repeatedly");check(v.holder.fixedCalls==resizes,"unchanged buffer resized repeatedly");
  MotionEvent e=pointer(802,360);v.dispatchTouchEvent(e);MotionEvent mapped=v.motion.get(0);
  check(Math.abs(mapped.x-641.5)<.01 && mapped.y==288,"touch not aligned with smaller Stage");
  check(Math.abs(mapped.x2-320.75)<.01 && mapped.y2==144,"second pointer not transformed");
  check(e.x==802 && e.y==360 && !e.recycled,"original input mutated");
  FREFunction input=MobileInputBridge.function();input.call(c,InputTest.args("hover","0.5","0.5","0"));c.activity.drain();
  mapped=v.motion.get(1);check(mapped.action==7 && Math.abs(mapped.x-801.5f*1283/1604)<.01,"virtual mouse scaled twice or not mapped");
  configure.call(c,scale("90"));c.activity.drain();check(v.holder.bw==1444 && v.holder.bh==648,"90% buffer");
  check(v.holder.callbacks.size()==1,"duplicate listeners on setting change");
  v.width=720;v.height=1604;v.layout.onLayoutChange(v,0,0,720,1604,0,0,1604,720);check(v.holder.bw==648 && v.holder.bh==1444,"rotation aspect ratio lost");
  configure.call(c,scale("100"));c.activity.drain();check(!v.holder.fixed && v.holder.bw==720 && v.holder.bh==1604,"native restoration");
  e=pointer(360,802);v.dispatchTouchEvent(e);mapped=v.motion.get(v.motion.size()-1);check(mapped.x==360 && mapped.y==802,"native coordinates still scaled");
  // Surface acknowledgement is asynchronous on Android: use the old actual buffer until then.
  v.holder.defer=true;configure.call(c,scale("80"));c.activity.drain();e=pointer(360,802);v.dispatchTouchEvent(e);
  check(v.motion.get(v.motion.size()-1).x==360,"requested size used before acknowledgement");v.holder.ack();v.dispatchTouchEvent(e);
  check(v.motion.get(v.motion.size()-1).x==288,"acknowledged surface not used");
  configure.call(c,scale("100"));c.activity.drain();v.dispatchTouchEvent(e);check(v.motion.get(v.motion.size()-1).x==288,"native request lost old buffer mapping before ack");v.holder.ack();v.dispatchTouchEvent(e);check(v.motion.get(v.motion.size()-1).x==360,"native ack not applied");
  configure.call(c,scale("80"));c.activity.drain();int pendingCalls=v.holder.fixedCalls;
  configure.call(c,scale("80"));c.activity.drain();check(v.holder.fixedCalls==pendingCalls,"pending identical resize requested twice");
  configure.call(c,scale("100"));c.activity.drain();check(!v.holder.fixed,"pending fixed resize not canceled on portal return");v.holder.ack();check(v.holder.bw==v.width && v.holder.bh==v.height,"late resize shrank restored portal");
  v.holder.defer=false;v.holder.fail=true;configure.call(c,scale("80"));c.activity.drain();check(c.eventLevel.equals("error") && !v.holder.fixed,"failure did not restore native");check(v.touch==null && v.generic==null && v.holder.callbacks.isEmpty(),"failed graphics listeners retained");
  v.holder.fail=false;configure.call(c,scale("80"));c.activity.drain();check(v.holder.fixed && v.touch!=null,"cannot retry after failure");
  v.holder.surface.unsupported=true;rate.call(c,scale("90"));c.activity.drain();configure.call(c,scale("90"));c.activity.drain();check(v.holder.fixed,"optional FPS hint failure broke scaling");v.holder.surface.unsupported=false;
  GraphicsBridge.release(c);check(v.holder.fixed,"release ran off UI thread");c.activity.drain();check(v.holder.surface.requestedFPS==0f,"display hint retained on release");check(!v.holder.fixed && v.touch==null && v.generic==null && v.layout==null && v.holder.callbacks.isEmpty(),"restart leaked scaler/listeners");
  check(c.activity.window.attrs.preferredDisplayModeId==0 && c.activity.window.attrs.preferredRefreshRate==0,"window refresh preference not restored");
  v.display.modes=new Display.Mode[]{new Display.Mode(1,1604,720,60),new Display.Mode(3,1604,720,120),new Display.Mode(4,2000,1000,90)};
  rate.call(c,scale("0"));c.activity.drain();configure.call(c,scale("80"));c.activity.drain();check(c.activity.window.attrs.preferredDisplayModeId==3 && c.activity.window.attrs.preferredRefreshRate==120,"120Hz-only high-rate display or resolution filter failed");GraphicsBridge.release(c);c.activity.drain();
  v.display.modes=new Display.Mode[]{new Display.Mode(9,2000,1000,240),new Display.Mode(1,1604,720,60),new Display.Mode(2,1604,720,90),new Display.Mode(3,1604,720,Float.NaN),new Display.Mode(4,1604,720,Float.POSITIVE_INFINITY),new Display.Mode(5,1604,720,-1),new Display.Mode(6,1604,720,1001)};
  rate.call(c,scale("0"));c.activity.drain();configure.call(c,scale("80"));c.activity.drain();check(c.activity.window.attrs.preferredRefreshRate==90 && v.holder.surface.requestedFPS==90,"invalid modes or different resolution selected");GraphicsBridge.release(c);c.activity.drain();
  v.display.modes=new Display.Mode[]{new Display.Mode(1,1604,720,59.94f)};
  configure.call(c,scale("80"));c.activity.drain();check(c.activity.window.attrs.preferredRefreshRate==59.94f && v.holder.surface.requestedFPS==59.94f,"fractional native refresh must reach surface without invented mode");GraphicsBridge.release(c);c.activity.drain();
  v.display.modes=new Display.Mode[]{new Display.Mode(2,1604,720,90)};
  configure.call(c,scale("80"));c.activity.drain();check(v.holder.surface.requestedFPS==60 && c.activity.window.attrs.preferredRefreshRate==90,"only-90 panel must still limit AIR/surface hint to 60");GraphicsBridge.release(c);c.activity.drain();
  v.display.unavailable=true;configure.call(c,scale("80"));c.activity.drain();check(v.holder.fixed && v.holder.surface.requestedFPS==60f,"optional mode failure must retain default 60 and scaler");GraphicsBridge.release(c);c.activity.drain();
  for(String invalid:new String[]{"30","144","NaN","Infinity","-1"})check(rate.call(c,scale(invalid))==null,"invalid FPS choice accepted");
  check(configure.call(c,scale("120"))==null && configure.call(c,scale("NaN"))==null,"invalid scale accepted");
  c.activity.window.decor.child=new Object();configure.call(c,scale("80"));c.activity.drain();check(c.eventLevel.equals("error"),"missing AIR surface hidden");
  System.out.println("PASS: default 60 and explicit 90/120/MAX native preferences; rate-only changes never resize buffer, repeated hints/sizes deduplicated, fractional/only-90/invalid modes, UI queue, input, optional APIs and release restoration.");
 }
}
