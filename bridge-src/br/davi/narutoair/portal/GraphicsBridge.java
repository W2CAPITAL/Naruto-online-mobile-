package br.davi.narutoair.portal;
import com.adobe.fre.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
/** Android hardware scaler on the AIR surface. No frame copying or fake FSR. */
public final class GraphicsBridge {
 static Object viewFor(FREContext c){synchronized(states){State s=states.get(c);return s==null?null:s.view;}}
 private static final Map<FREContext,State> states=new WeakHashMap<FREContext,State>();
 private static final Map<FREContext,Float> limits=new WeakHashMap<FREContext,Float>();
 private static Object call(Object o,String name,Class<?>[] types,Object... args)throws Exception {return o.getClass().getMethod(name,types).invoke(o,args);}
 private static Object activity(FREContext c)throws Exception{return call(c,"getActivity",new Class<?>[0]);}
 private static Object find(Object node,int depth)throws Exception {
  if(node==null || depth>32)return null;
  if(Class.forName("com.adobe.air.AIRWindowSurfaceView").isInstance(node))return node;
  try {int n=((Number)call(node,"getChildCount",new Class<?>[0])).intValue();for(int i=0;i<n;i++){Object v=find(call(node,"getChildAt",new Class<?>[]{int.class},i),depth+1);if(v!=null)return v;}}
  catch(NoSuchMethodException ignored){}return null;
 }
 private static void signal(FREContext c,String value){if(c!=null)try{c.dispatchStatusEventAsync("graphics",value);}catch(Exception ignored){}}
 private static boolean queue(FREContext c,Runnable r){try{call(activity(c),"runOnUiThread",new Class<?>[]{Runnable.class},r);return true;}catch(Exception e){return false;}}
 public static FREFunction function(){return new FREFunction(){public FREObject call(final FREContext c,FREObject[] args){
  try {
   if(args.length!=1)return null;final int percent=Integer.parseInt(args[0].getAsString());
   if(percent!=80 && percent!=90 && percent!=100)return null;
   return FREObject.newObject(queue(c,new Runnable(){public void run(){
    State s=null;try {
     synchronized(states){s=states.get(c);}
     if(s==null){Object w=GraphicsBridge.call(activity(c),"getWindow",new Class<?>[0]);Object view=find(GraphicsBridge.call(w,"getDecorView",new Class<?>[0]),0);if(view==null)throw new IllegalStateException();s=new State(c,view);s.install();synchronized(states){states.put(c,s);}}
     s.percent=percent;s.apply();
    }catch(Exception e){if(s!=null)s.restore();signal(c,"error");}
   }}));
  }catch(Exception e){return null;}
 }};}
 public static FREFunction fpsFunction(){return new FREFunction(){public FREObject call(final FREContext c,FREObject[] args){
  try {
   if(args.length!=1)return null;final float limit=Float.parseFloat(args[0].getAsString());
   if(limit!=0f && limit!=60f && limit!=90f && limit!=120f)return null;
   return FREObject.newObject(queue(c,new Runnable(){public void run(){
    State s;synchronized(states){limits.put(c,limit);s=states.get(c);}
    if(s!=null && s.percent<100)s.applyRefresh();
   }}));
  }catch(Exception unavailable){return null;}
 }};}
 public static void release(final FREContext c){FsrBridge.stop(c);queue(c,new Runnable(){public void run(){State s;synchronized(states){s=states.remove(c);limits.remove(c);}if(s!=null)s.restore();}});}
 private static final class State {
  final WeakReference<FREContext> context;final Object view,holder;final WindowRefresh display;int percent=100,bufferWidth,bufferHeight,requestedWidth,requestedHeight;boolean fixedRequested;Object callback,layout;float lastHint=Float.NaN;
  State(FREContext c,Object v)throws Exception{context=new WeakReference<FREContext>(c);view=v;display=new WindowRefresh(c,v);holder=Class.forName("android.view.SurfaceView").getMethod("getHolder").invoke(v);}
  int dimension(String n)throws Exception{return ((Number)Class.forName("android.view.View").getMethod(n).invoke(view)).intValue();}
  void install()throws Exception {
   final Class<?> v=Class.forName("android.view.View"),motion=Class.forName("android.view.MotionEvent"),matrix=Class.forName("android.graphics.Matrix");
   // Validate input remapping before enabling a smaller buffer.
   motion.getMethod("obtain",motion);motion.getMethod("transform",matrix);view.getClass().getMethod("onTouchEvent",motion);view.getClass().getMethod("onGenericMotionEvent",motion);
   for(final String kind:new String[]{"OnTouchListener","OnGenericMotionListener"}){
    Class<?> type=Class.forName("android.view.View$"+kind);
    Object listener=Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a)throws Throwable {
     if(!m.getName().equals("onTouch") && !m.getName().equals("onGenericMotion"))return objectMethod(p,m,a);
     if(bufferWidth<=0 || bufferHeight<=0)return false;
     Object event=null;try {
      int w=dimension("getWidth"),h=dimension("getHeight");if(w<=0 || h<=0 || (bufferWidth==w && bufferHeight==h))return false;
      event=motion.getMethod("obtain",motion).invoke(null,a[1]);Object transform=matrix.getConstructor().newInstance();
      matrix.getMethod("setScale",float.class,float.class).invoke(transform,(float)bufferWidth/w,(float)bufferHeight/h);
      motion.getMethod("transform",matrix).invoke(event,transform);
      // Call AIR's handler directly; dispatchTouchEvent would enter this listener again.
      view.getClass().getMethod(kind.equals("OnTouchListener")?"onTouchEvent":"onGenericMotionEvent",motion).invoke(view,event);
      return true;
     }catch(Exception e){restore();signal(context.get(),"error");return false;}
     finally{if(event!=null)motion.getMethod("recycle").invoke(event);}
    }});
    v.getMethod("set"+kind,type).invoke(view,listener);
   }
   Class<?> cb=Class.forName("android.view.SurfaceHolder$Callback");
   callback=Proxy.newProxyInstance(cb.getClassLoader(),new Class<?>[]{cb},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){
    try {
     if(m.getName().equals("surfaceChanged")){bufferWidth=(Integer)a[2];bufferHeight=(Integer)a[3];if(fixedRequested && (bufferWidth!=requestedWidth || bufferHeight!=requestedHeight))fixedRequested=false;notifySize();}
     else if(m.getName().equals("surfaceDestroyed")){bufferWidth=bufferHeight=0;fixedRequested=false;lastHint=Float.NaN;}
     else if(m.getName().equals("surfaceCreated")){fixedRequested=false;lastHint=Float.NaN;apply();}
     else return objectMethod(p,m,a);
    }catch(Exception e){restore();signal(context.get(),"error");}return null;
   }});
   Class.forName("android.view.SurfaceHolder").getMethod("addCallback",cb).invoke(holder,callback);
   Class<?> lc=Class.forName("android.view.View$OnLayoutChangeListener");
   layout=Proxy.newProxyInstance(lc.getClassLoader(),new Class<?>[]{lc},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){
    if(m.getName().equals("onLayoutChange")){try{apply();}catch(Exception e){restore();signal(context.get(),"error");}return null;}return objectMethod(p,m,a);
   }});v.getMethod("addOnLayoutChangeListener",lc).invoke(view,layout);
  }
  void apply()throws Exception {
   int w=dimension("getWidth"),h=dimension("getHeight");if(w<=0 || h<=0)throw new IllegalStateException();
   Class<?> sh=Class.forName("android.view.SurfaceHolder");
   Object rect=sh.getMethod("getSurfaceFrame").invoke(holder);
   bufferWidth=((Number)call(rect,"width",new Class<?>[0])).intValue();bufferHeight=((Number)call(rect,"height",new Class<?>[0])).intValue();
   if(percent==100){if(fixedRequested || bufferWidth!=w || bufferHeight!=h){fixedRequested=false;sh.getMethod("setSizeFromLayout").invoke(holder);}}
   else {int bw=Math.max(1,Math.round(w*percent/100f)),bh=Math.max(1,Math.round(h*percent/100f));
    // AIR 51.1.4.1 classifies a reduced height as keyboard panning and returns
    // before emitting WindowEventData(size). Prepare only our fixed-buffer change.
    // Reflection targets our packaged AIR class, not a restricted Android API.
    if(!fixedRequested || requestedWidth!=bw || requestedHeight!=bh){
     Field oldHeight=Class.forName("com.adobe.air.AIRWindowSurfaceView").getDeclaredField("mHt");
     oldHeight.setAccessible(true);oldHeight.setInt(view,bh);
    // AIR's surfaceChanged callback informs its Stage of these buffer dimensions.
    // Track the actual surface rectangle until Android acknowledges the new size.
     fixedRequested=true;requestedWidth=bw;requestedHeight=bh;
     sh.getMethod("setFixedSize",int.class,int.class).invoke(holder,bw,bh);
    }
   }
   if(percent<100)applyRefresh();else {refreshRate(0f);display.restore();}
   signal(context.get(),"requested:"+percent);
  }
  void applyRefresh(){
   Float choice; synchronized(states){choice=limits.get(context.get());}
   refreshRate(display.apply(choice==null?60f:choice));
  }
  void refreshRate(float fps){
   if(Float.compare(lastHint,fps)==0)return;
   try {Object surface=Class.forName("android.view.SurfaceHolder").getMethod("getSurface").invoke(holder);Class<?> type=Class.forName("android.view.Surface");
    try {type.getMethod("setFrameRate",float.class,int.class,int.class).invoke(surface,fps,0,0);}catch(NoSuchMethodException old){type.getMethod("setFrameRate",float.class,int.class).invoke(surface,fps,0);}lastHint=fps;
   }catch(Exception unsupported){} // Optional API 30+, not a guarantee of presented FPS.
  }
  void notifySize()throws Exception {signal(context.get(),"active:"+percent+":"+bufferWidth+":"+bufferHeight+":"+dimension("getWidth")+":"+dimension("getHeight"));}
  void restore(){refreshRate(0f);display.restore();FREContext c=context.get();synchronized(states){if(states.get(c)==this)states.remove(c);}percent=100;bufferWidth=bufferHeight=0;try{Class.forName("android.view.SurfaceHolder").getMethod("setSizeFromLayout").invoke(holder);}catch(Exception ignored){}
   try {Class<?> v=Class.forName("android.view.View");for(String n:new String[]{"OnTouchListener","OnGenericMotionListener"})v.getMethod("set"+n,Class.forName("android.view.View$"+n)).invoke(view,new Object[]{null});
    if(layout!=null)v.getMethod("removeOnLayoutChangeListener",Class.forName("android.view.View$OnLayoutChangeListener")).invoke(view,layout);
    if(callback!=null)Class.forName("android.view.SurfaceHolder").getMethod("removeCallback",Class.forName("android.view.SurfaceHolder$Callback")).invoke(holder,callback);
   }catch(Exception ignored){}
   layout=callback=null;
  }
 }
 private static Object objectMethod(Object proxy,Method m,Object[] a){if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);if(m.getName().equals("equals"))return proxy==a[0];if(m.getName().equals("toString"))return "NarutoGraphicsListener";return null;}
}
