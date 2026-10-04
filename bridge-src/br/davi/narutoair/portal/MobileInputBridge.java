package br.davi.narutoair.portal;
import com.adobe.fre.*;
import java.lang.reflect.*;
/** Real Android input to the app's own AIR view, on the UI thread. */
public final class MobileInputBridge {
 private static final java.util.Map<FREContext,Long> downTimes=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<FREContext,Long>());
 private static Object invoke(Object o,String name,Class<?>[] types,Object... args)throws Exception {
  return o.getClass().getMethod(name,types).invoke(o,args);
 }
 private static Object activity(FREContext c)throws Exception {return invoke(c,"getActivity",new Class<?>[0]);}
 private static Object surface(Object node,int depth)throws Exception {
  if(node==null || depth>32)return null;
  if(Class.forName("com.adobe.air.AIRWindowSurfaceView").isInstance(node))return node;
  try {
   int n=((Number)invoke(node,"getChildCount",new Class<?>[0])).intValue();
   for(int i=0;i<n;i++){Object found=surface(invoke(node,"getChildAt",new Class<?>[]{int.class},i),depth+1);if(found!=null)return found;}
  }catch(NoSuchMethodException ignored){}
  return null;
 }
 private static Object find(FREContext c)throws Exception {
  Object a=activity(c),window=invoke(a,"getWindow",new Class<?>[0]);
  return surface(invoke(window,"getDecorView",new Class<?>[0]),0);
 }
 private static void error(FREContext c){try{c.dispatchStatusEventAsync("log","INPUT 1.3.5: superficie AIR indisponivel");}catch(Exception ignored){}}
 private static boolean queue(final FREContext c,final Runnable task) {
  try {invoke(activity(c),"runOnUiThread",new Class<?>[]{Runnable.class},task);return true;}
  catch(Exception e){return false;}
 }
 public static void focus(final FREContext c) {
  queue(c,new Runnable(){public void run(){try{Object view=find(c);if(view!=null){Class.forName("android.view.View").getMethod("requestFocus").invoke(view);}}catch(Exception e){error(c);}}});
 }
 public static FREFunction function() {
  return new FREFunction(){public FREObject call(final FREContext c,FREObject[] args) {
   try {
    if(args.length!=4)return null;
    final String action=args[0].getAsString();final double x=Double.parseDouble(args[1].getAsString()),y=Double.parseDouble(args[2].getAsString());
    final int key=Integer.parseInt(args[3].getAsString());
    if(!Double.isFinite(x)||!Double.isFinite(y)||x<0||x>1||y<0||y>1)return null;
    if(!action.matches("hover|click|down|drag|up|key|focus") || (action.equals("key") && key!=66 && key!=67 && key!=111 && key!=61))return null;
    boolean ok=queue(c,new Runnable(){public void run(){
     try {
      Object view=find(c);if(view==null){error(c);return;}
      Class<?> v=Class.forName("android.view.View");v.getMethod("requestFocus").invoke(view);
      if(action.equals("focus"))return;
      if(action.equals("key")) {
       Class<?> k=Class.forName("android.view.KeyEvent");
       for(int state=0;state<2;state++){Object event=k.getConstructor(int.class,int.class).newInstance(state,key);v.getMethod("dispatchKeyEvent",k).invoke(view,event);}return;
      }
      int w=((Number)v.getMethod("getWidth").invoke(view)).intValue(),h=((Number)v.getMethod("getHeight").invoke(view)).intValue();
      float px=(float)(x*Math.max(0,w-1)),py=(float)(y*Math.max(0,h-1));
      Class<?> m=Class.forName("android.view.MotionEvent");
      long now=((Number)Class.forName("android.os.SystemClock").getMethod("uptimeMillis").invoke(null)).longValue();
      int first=action.equals("hover")?7:action.equals("up")?1:action.equals("drag")?2:0;
      if(first==0)downTimes.put(c,now);
      long down=downTimes.containsKey(c)?downTimes.get(c):now;
      emit(v,view,m,down,now,first,px,py,action.equals("hover"));
      if(action.equals("click"))emit(v,view,m,down,now,1,px,py,false);
      if(action.equals("click") || action.equals("up"))downTimes.remove(c);
     }catch(Exception e){error(c);}
    }});
    return FREObject.newObject(ok);
   }catch(Exception e){return null;}
  }};
 }
 private static void emit(Class<?> v,Object view,Class<?> m,long down,long now,int action,float x,float y,boolean hover)throws Exception {
  Object event=m.getMethod("obtain",long.class,long.class,int.class,float.class,float.class,int.class).invoke(null,down,now,action,x,y,0);
  try {
   m.getMethod("setSource",int.class).invoke(event,hover?0x2002:0x1002);
   v.getMethod(hover?"dispatchGenericMotionEvent":"dispatchTouchEvent",m).invoke(view,event);
  }finally{m.getMethod("recycle").invoke(event);}
 }
}
