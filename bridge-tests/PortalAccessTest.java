import br.davi.narutoair.portal.*;
import android.webkit.*;
import android.widget.*;
import java.lang.reflect.*;
import java.util.*;
public class PortalAccessTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception {
  BrandingTest.Context c=new BrandingTest.Context();c.view().url=PortalSession.SERVER_LIST;PortalBranding.install(c);
  LinearLayout footer=(LinearLayout)c.a.root.getChildAt(0);TextView title=(TextView)footer.getChildAt(1),retry=(TextView)footer.getChildAt(5),servers=(TextView)footer.getChildAt(6);
  PortalAccess.inspect(c,c.view());check(c.view().script.equals(PortalAccess.SCRIPT),"wrong classifier");c.view().reply.onReceiveValue("\"blocked\"");
  check(title.text.contains("Site bloqueou")&&title.text.contains("W1/W2"),"blocked warning removed credits");check(retry.text.startsWith("AGUARDE")&&servers.text.equals("ABRIR SITE"),"blocked controls missing");
  retry.performClick();retry.performClick();check(c.view().reloads==0,"blocked retries were not limited");
  servers.performClick();check(c.a.external.size()==1&&c.a.external.get(0).uri.value.equals(PortalSession.SERVER_LIST),"browser gets nonpublic URL or missing user action");
  Field states=PortalAccess.class.getDeclaredField("states");states.setAccessible(true);Object state=((Map<?,?>)states.get(null)).get(c);Field next=state.getClass().getDeclaredField("nextRetry");next.setAccessible(true);long initial=next.getLong(state);
  PortalAccess.inspect(c,c.view());c.view().reply.onReceiveValue("\"blocked\"");check(next.getLong(state)==initial,"poll extended cooldown indefinitely");
  next.setLong(state,System.nanoTime()-1);retry.performClick();check(c.view().reloads==1,"expired cooldown cannot reload");retry.performClick();check(c.view().reloads==1,"multiple immediate retries allowed");
  PortalAccess.inspect(c,c.view());ValueCallback<String> stale=c.view().reply;PortalAccess.inspect(c,c.view());c.view().reply.onReceiveValue("\"clear\"");stale.onReceiveValue("\"blocked\"");
  check(!PortalAccess.restricted(c)&&servers.text.equals("SERVIDORES")&&title.text.equals("Naruto Online Mobile\n\u00a9 2026 W1/W2 Solu\u00e7\u00f5es Capitais"),"stale callback replaced healthy portal");
  retry.performClick();check(c.view().reloads==2,"ordinary explicit reload changed");servers.performClick();check(c.eventCode.equals("select-server"),"healthy server selection changed");
  PortalAccess.inspect(c,c.view());stale=c.view().reply;c.view().url=PortalSession.SERVER_LIST+"s877";stale.onReceiveValue("\"blocked\"");check(!PortalAccess.restricted(c),"old URL status applied");
  PortalAccess.inspect(c,c.view());stale=c.view().reply;Field view=PortalContext.class.getDeclaredField("webView");view.setAccessible(true);WebView replacement=new WebView();replacement.url=PortalSession.SERVER_LIST;view.set(c,replacement);stale.onReceiveValue("\"blocked\"");check(!PortalAccess.restricted(c),"old popup status applied");
  PortalAccess.inspect(c,replacement);replacement.reply.onReceiveValue("\"challenge\"");check(PortalAccess.restricted(c)&&title.text.contains("verificacao"),"challenge not distinguished");check(replacement.reloads==0,"challenge auto reloaded");
  PortalAccess.inspect(c,replacement);stale=replacement.reply;Field hidden=MobilePortalContext.class.getDeclaredField("portalHidden");hidden.setAccessible(true);hidden.setBoolean(null,true);stale.onReceiveValue("\"clear\"");check(!PortalAccess.reload(c),"hidden game reload allowed");hidden.setBoolean(null,false);
  PortalAccess.inspect(c,replacement);stale=replacement.reply;PortalAccess.release(c);stale.onReceiveValue("\"blocked\"");check(!((Map<?,?>)states.get(null)).containsKey(c),"dispose callback retained state");
  c.view().url="https://unrelated.example/";String script=c.view().script;PortalAccess.inspect(c,c.view());check(Objects.equals(script,c.view().script),"foreign page inspected");
  PortalBranding.release(c);c.a.drain();check(c.a.root.children.isEmpty(),"footer leaked on disposal");
  System.out.println("PASS: portal block/challenge status, credits, manual cooldown/expiry, no automatic retries, public browser action, stale checks/URL/popup/hidden/dispose guards; JVM model, not device.");
 }
}
