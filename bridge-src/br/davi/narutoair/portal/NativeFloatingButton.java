package br.davi.narutoair.portal;
import com.adobe.fre.*;
import java.lang.reflect.*;
/** Drag the existing Android LOG button; a stationary tap keeps its click action. */
public final class NativeFloatingButton {
 private static float savedX=Float.NaN,savedY=Float.NaN;
 private static Object call(Object o,String name,Class<?>[] types,Object... args)throws Exception {return o.getClass().getMethod(name,types).invoke(o,args);}
 private static float number(Object o,String name)throws Exception{return ((Number)call(o,name,new Class<?>[0])).floatValue();}
 public static void install(final FREContext context) {
  PortalBranding.install(context);
  try {
   Field f=PortalContext.class.getDeclaredField("nativeLogButton");f.setAccessible(true);final Object button=f.get(context);if(button==null)return;
   final Class<?> view=Class.forName("android.view.View");final Object activity=call(context,"getActivity",new Class<?>[0]);
   read(activity);
   final Runnable restore=new Runnable(){public void run(){try{if(Float.isFinite(savedX)&&Float.isFinite(savedY))position(button,view,savedX,savedY);}catch(Exception ignored){}}};
   call(button,"post",new Class<?>[]{Runnable.class},restore);
   Class<?> layout=Class.forName("android.view.View$OnLayoutChangeListener");
   call(button,"addOnLayoutChangeListener",new Class<?>[]{layout},Proxy.newProxyInstance(layout.getClassLoader(),new Class<?>[]{layout},new InvocationHandler(){
    public Object invoke(Object p,Method m,Object[] a){if(m.getName().equals("onLayoutChange"))restore.run();return null;}
   }));
   Class<?> touch=Class.forName("android.view.View$OnTouchListener");
   Object listener=Proxy.newProxyInstance(touch.getClassLoader(),new Class<?>[]{touch},new InvocationHandler(){
    float sx,sy,bx,by;boolean active,moved;
    public Object invoke(Object p,Method m,Object[] a)throws Throwable {
     if(!m.getName().equals("onTouch"))return null;
     Object event=a[1];int action=((Number)call(event,"getActionMasked",new Class<?>[0])).intValue();
     float x=number(event,"getRawX"),y=number(event,"getRawY");
     if(action==0){active=true;moved=false;sx=x;sy=y;bx=number(button,"getX");by=number(button,"getY");view.getMethod("setPressed",boolean.class).invoke(button,true);}
     else if(action==2 && active){float dx=x-sx,dy=y-sy;if(dx*dx+dy*dy>=64)moved=true;
      if(moved){Object parent=call(button,"getParent",new Class<?>[0]);float w=Math.max(0,number(parent,"getWidth")-number(button,"getWidth")),h=Math.max(0,number(parent,"getHeight")-number(button,"getHeight"));
       float px=Math.max(0,Math.min(w,bx+dx)),py=Math.max(0,Math.min(h,by+dy));view.getMethod("setX",float.class).invoke(button,px);view.getMethod("setY",float.class).invoke(button,py);
       savedX=px/Math.max(1,w);savedY=py/Math.max(1,h);view.getMethod("setPressed",boolean.class).invoke(button,false);
      }
     }else if(action==1 || action==3){boolean click=active&&!moved&&action==1;active=false;view.getMethod("setPressed",boolean.class).invoke(button,false);
      if(moved)write(activity);if(click)view.getMethod("performClick").invoke(button);
     }
     return true;
    }
   });
   view.getMethod("setOnTouchListener",touch).invoke(button,listener);
  }catch(Exception e){context.dispatchStatusEventAsync("log","LOG movel: inicializacao indisponivel.");}
 }
 private static void position(Object b,Class<?> v,float x,float y)throws Exception {
  Object p=call(b,"getParent",new Class<?>[0]);float w=Math.max(0,number(p,"getWidth")-number(b,"getWidth")),h=Math.max(0,number(p,"getHeight")-number(b,"getHeight"));
  v.getMethod("setX",float.class).invoke(b,Math.max(0,Math.min(1,x))*w);v.getMethod("setY",float.class).invoke(b,Math.max(0,Math.min(1,y))*h);
 }
 private static Object preferences(Object activity)throws Exception{return Class.forName("android.content.Context").getMethod("getSharedPreferences",String.class,int.class).invoke(activity,"narutoOverlayPositions",0);}
 private static void read(Object activity){try{Object p=preferences(activity);Class<?> sp=Class.forName("android.content.SharedPreferences");savedX=(Float)sp.getMethod("getFloat",String.class,float.class).invoke(p,"logX",savedX);savedY=(Float)sp.getMethod("getFloat",String.class,float.class).invoke(p,"logY",savedY);}catch(Exception ignored){}}
 private static void write(Object activity){try{Object p=preferences(activity),editor=Class.forName("android.content.SharedPreferences").getMethod("edit").invoke(p);Class<?> ed=Class.forName("android.content.SharedPreferences$Editor");ed.getMethod("putFloat",String.class,float.class).invoke(editor,"logX",savedX);ed.getMethod("putFloat",String.class,float.class).invoke(editor,"logY",savedY);ed.getMethod("apply").invoke(editor);}catch(Exception ignored){}}
}
