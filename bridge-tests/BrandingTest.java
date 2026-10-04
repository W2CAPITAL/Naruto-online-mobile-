import br.davi.narutoair.portal.*;
import com.adobe.fre.*;
import android.view.*;
import android.widget.*;
import java.util.*;
public final class BrandingTest {
 public static class Activity extends android.content.Context {
  public final FrameLayout root=new FrameLayout(this);public final List<Runnable> queue=new ArrayList<Runnable>();public final List<android.content.Intent> external=new ArrayList<android.content.Intent>();
  public void addContentView(View v,ViewGroup.LayoutParams p){root.addView(v,p);}public void runOnUiThread(Runnable r){queue.add(r);}public void startActivity(android.content.Intent i){external.add(i);}public void drain(){for(Runnable r:new ArrayList<Runnable>(queue))r.run();queue.clear();}
 }
 public static class Context extends MobileBase {public final Activity a=new Activity();@Override public Object getActivity(){return a;}}
 public static class MobileBase extends PortalContext {}
 static void check(boolean v,String s){if(!v)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception {
  Context c=new Context();c.view().setLayoutParams(new FrameLayout.LayoutParams(-1,-1));
  PortalBranding.install(c);check(c.a.root.children.size()==1,"footer missing");LinearLayout footer=(LinearLayout)c.a.root.getChildAt(0);
  check(((ViewGroup.MarginLayoutParams)c.view().getLayoutParams()).bottomMargin==58,"footer covers portal instead of reserving space");
  check(((ImageView)footer.getChildAt(0)).drawable!=null,"logo not loaded");check(((TextView)footer.getChildAt(1)).text.contains("W1/W2 Solu"),"copyright missing");
  PortalBranding.install(c);check(c.a.root.children.size()==1,"duplicate footer on popup");
  footer.getChildAt(2).performClick();footer.getChildAt(3).performClick();footer.getChildAt(4).performClick();
  check(c.a.external.size()==3 && PortalBranding.SUPPORT.equals(c.a.external.get(2).uri.value),"support opens wrong contact");
  footer.getChildAt(5).performClick();check(c.view().reloads==1,"portal recovery does not reload active view");
  footer.getChildAt(6).performClick();check("select-server".equals(c.eventCode),"return to server selection missing");
  PortalBranding.visibility(c,false);c.a.drain();check(footer.visibility==8 && ((ViewGroup.MarginLayoutParams)c.view().getLayoutParams()).bottomMargin==0,"credits still cover game");
  String real=PortalBranding.rechargeURL("10163819","877");check(real.contains("uid=10163819")&&real.contains("serverId=877"),"wrong payment targeting");
  check(!PortalBranding.rechargeURL("1&uid=evil","javascript:bad").contains("uid="),"untrusted target parameters accepted");
  check(PortalBranding.rechargeURL("fixture@example.test","877").contains("uid=fixture%40example.test"),"platform account not encoded");
  String old=c.view().url;
  PortalBranding.recharge().call(c,new FREObject[]{FREObject.newObject("10163819"),FREObject.newObject("877")});
  check(c.a.root.children.size()==1,"payment created outside UI thread");c.a.drain();check(c.a.root.children.size()==2,"payment tab missing");
  LinearLayout payment=(LinearLayout)c.a.root.getChildAt(1);android.webkit.WebView web=(android.webkit.WebView)payment.getChildAt(1);
  check(real.equals(web.url)&&web.settings.js&&web.settings.dom&&web.client!=null,"official payment tab configuration missing");
  check(Objects.equals(c.view().url,old) && !c.view().stopped,"game portal replaced");
  payment.getChildAt(0).performClick();check(web.destroyed && c.a.root.children.size()==1,"return does not release payment tab");
  PortalBranding.visibility(c,true);c.a.drain();check(footer.visibility==0,"credits missing after return to portal");
  PortalBranding.release(c);c.a.drain();check(c.a.root.children.isEmpty(),"native UI leaked on dispose");
  System.out.println("PASS: branding footer reserves portal space, loads logo, opens support links, hides for game, and does not duplicate. Official payment tab targets current UID/server, rejects parameter injection, preserves portal and releases on close/dispose. JVM model, not device.");
 }
}
