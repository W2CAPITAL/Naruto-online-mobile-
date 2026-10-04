package br.davi.narutoair.portal;
import com.adobe.fre.FREContext;
import java.lang.reflect.*;
import java.net.URI;
import java.util.*;
/** Visible portal access state. Does not override Cloudflare, clear auth or retry automatically. */
public final class PortalAccess {
 private static final Map<FREContext,State> states=Collections.synchronizedMap(new WeakHashMap<FREContext,State>());
 public static final String SCRIPT="// Classifies only the visible official portal. No credentials, network requests or page changes.\n(function () {\n  try {\n    if (window.top !== window || location.protocol !== 'https:' ||\n        !/^(?:naruto|gamebox3)\\.narutowebgame\\.com$/.test(location.hostname) || document.readyState !== 'complete') return 'unknown';\n    var text = String(document.body && document.body.textContent || '').slice(0, 16000).toLowerCase().replace(/\\s+/g, ' ');\n    var cloudflare = !!document.getElementById('cf-error-details') || text.indexOf('cloudflare') >= 0;\n    if (cloudflare && /you have been blocked|voc\u00ea foi bloqueado|voce foi bloqueado/.test(text)) return 'blocked';\n    if (document.getElementById('challenge-running') || document.getElementById('cf-chl-widget') ||\n        document.getElementById('challenge-stage') || /^just a moment/i.test(document.title || '')) return 'challenge';\n    if (cloudflare && document.getElementById('cf-error-details')) return 'unknown';\n    return 'clear';\n  } catch (_) { return 'unknown'; }\n})();\n";
 private static final long WAIT_NS=60_000_000_000L;
 private static final class State {Object view;String url,status="clear";long nextRetry,check;}
 private static Object call(Object o,String n,Class<?>[] t,Object... a)throws Exception{return o.getClass().getMethod(n,t).invoke(o,a);}
 private static Object current(FREContext c)throws Exception {Field f=PortalContext.class.getDeclaredField("webView");f.setAccessible(true);return f.get(c);}
 private static String url(Object v)throws Exception{return (String)call(v,"getUrl",new Class<?>[0]);}
 private static boolean allowed(String text){try {URI u=new URI(text);return "https".equals(u.getScheme()) && u.getUserInfo()==null && (u.getPort()==-1 || u.getPort()==443) && ("naruto.narutowebgame.com".equals(u.getHost()) || "gamebox3.narutowebgame.com".equals(u.getHost()));}catch(Exception ignored){return false;}}
 private static State state(FREContext c,Object v,String u){State s=states.get(c);if(s==null){s=new State();states.put(c,s);}if(s.view!=v || !Objects.equals(s.url,u)){s.view=v;s.url=u;s.status="clear";s.nextRetry=0;s.check++;}return s;}
 /** Called by the portal's existing on-page-finished hook and periodic visible health check on UI thread. */
 public static void inspect(final FREContext c,final Object view){try{
  if(MobilePortalContext.isPortalHidden() || view==null || current(c)!=view)return;
  final String page=url(view);final State s=state(c,view,page);
  if(!allowed(page)){PortalBranding.accessStatus(c,"clear",0);return;}
  final long ticket=++s.check;
  Class<?> callback=Class.forName("android.webkit.ValueCallback");Object reply=Proxy.newProxyInstance(callback.getClassLoader(),new Class<?>[]{callback},new InvocationHandler(){
   public Object invoke(Object p,Method method,Object[] args)throws Throwable {
    if(!method.getName().equals("onReceiveValue"))return null;
    try{
     if(MobilePortalContext.isPortalHidden() || states.get(c)!=s || s.check!=ticket || current(c)!=view || !Objects.equals(url(view),page))return null;
     String value=args!=null && args.length==1?String.valueOf(args[0]):"";
     String next=value.equals("\"blocked\"")?"blocked":value.equals("\"challenge\"")?"challenge":value.equals("\"clear\"")?"clear":null;
     if(next==null)return null;
     if(!next.equals(s.status))s.nextRetry=next.equals("clear")?0:System.nanoTime()+WAIT_NS;
     s.status=next;PortalBranding.accessStatus(c,next,seconds(s));
    }catch(Exception ignored){}return null;
   }
  });
  call(view,"evaluateJavascript",new Class<?>[]{String.class,callback},SCRIPT,reply);
 }catch(Exception ignored){} }
 private static int seconds(State s){return (int)Math.max(0,(s.nextRetry-System.nanoTime()+999_999_999L)/1_000_000_000L);}
 /** Explicit user action only. Preserve cookies/UA and bound reloads when access is blocked/challenged. */
 public static boolean reload(FREContext c){try{
  if(MobilePortalContext.isPortalHidden())return false;Object view=current(c);if(view==null)return false;
  State s=state(c,view,url(view));if(!s.status.equals("clear")){
   int wait=seconds(s);if(wait>0){PortalBranding.accessStatus(c,s.status,wait);return false;}
   s.nextRetry=System.nanoTime()+WAIT_NS;PortalBranding.accessStatus(c,s.status,60);
  }
  // In-flight callbacks from before this explicit reload cannot mark the new document.
  s.check++;call(view,"reload",new Class<?>[0]);return true;
 }catch(Exception ignored){return false;} }
 public static boolean restricted(FREContext c){try{if(MobilePortalContext.isPortalHidden())return false;Object v=current(c);State s=states.get(c);return s!=null && s.view==v && Objects.equals(s.url,url(v)) && !s.status.equals("clear");}catch(Exception ignored){return false;}}
 public static void release(FREContext c){states.remove(c);}
}
