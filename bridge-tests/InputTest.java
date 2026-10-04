import br.davi.narutoair.portal.*;
import com.adobe.fre.*;
import java.util.*;
public class InputTest {
 public static class Group {public Object child;public int getChildCount(){return 1;}public Object getChildAt(int i){return child;}}
 public static class Window {public Group decor;public Group getDecorView(){return decor;}public android.view.WindowManager.LayoutParams attrs=new android.view.WindowManager.LayoutParams();public int updates;public android.view.WindowManager.LayoutParams getAttributes(){return attrs;}public void setAttributes(android.view.WindowManager.LayoutParams a){attrs=a;updates++;}}
 public static class Activity {public Window window;public List<Runnable> queue=new ArrayList<Runnable>();public Window getWindow(){return window;}public void runOnUiThread(Runnable r){queue.add(r);}public void drain(){List<Runnable> old=new ArrayList<Runnable>(queue);queue.clear();for(Runnable r:old)r.run();}}
 public static class Context extends PortalContext {public Activity activity;public Activity getActivity(){return activity;}}
 static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
 static FREObject[] args(String action,String x,String y,String code){return new FREObject[]{new FREObject(action),new FREObject(x),new FREObject(y),new FREObject(code)};}
 public static void main(String[] a)throws Exception {
  Context c=new Context();c.activity=new Activity();c.activity.window=new Window();c.activity.window.decor=new Group();
  com.adobe.air.AIRWindowSurfaceView surface=new com.adobe.air.AIRWindowSurfaceView();c.activity.window.decor.child=surface;
  FREFunction input=MobileInputBridge.function();
  check(input.call(c,args("click","0.5","0.5","0")).value.equals(true),"queue click");
  check(surface.motion.isEmpty(),"input executed off UI thread");c.activity.drain();
  check(surface.motion.size()==2 && surface.motion.get(0).action==0 && surface.motion.get(1).action==1,"click down/up");
  check(surface.motion.get(0).x==801.5f && surface.motion.get(0).y==359.5f,"coordinate mapping");
  check(surface.focused,"AIR focus");
  input.call(c,args("down","0.25","0.25","0"));c.activity.drain();
  input.call(c,args("drag","0.75","0.75","0"));c.activity.drain();
  input.call(c,args("up","0.75","0.75","0"));c.activity.drain();
  check(surface.motion.get(2).action==0 && surface.motion.get(3).action==2 && surface.motion.get(4).action==1,"drag sequence");
  check(surface.motion.get(2).downTime==surface.motion.get(3).downTime && surface.motion.get(3).downTime==surface.motion.get(4).downTime,"drag downTime changed");
  input.call(c,args("hover","1","0","0"));c.activity.drain();
  check(surface.motion.get(5).action==7 && surface.motion.get(5).source==0x2002,"real mouse hover");
  input.call(c,args("key","0","0","66"));c.activity.drain();
  check(surface.keys.size()==2 && surface.keys.get(0).code==66 && surface.keys.get(1).action==1,"real key down/up");
  check(input.call(c,args("click","NaN","0","0"))==null,"NaN coordinate accepted");
  check(input.call(c,args("click","2","0","0"))==null,"outside coordinate accepted");
  check(input.call(c,args("key","0","0","3"))==null,"system key accepted");
  surface.focused=false;MobileInputBridge.focus(c);check(!surface.focused,"focus not queued");c.activity.drain();check(surface.focused,"focus missing");
  c.activity.window.decor.child=new Object();input.call(c,args("click","0","0","0"));c.activity.drain();check(c.eventLevel.contains("indisponivel"),"missing surface hidden");
  System.out.println("PASS: Android input reflection model: UI queue, AIR focus, pointer coordinates, down/up, hover, real key events, range/system-key rejection and missing surface.");
 }
}
