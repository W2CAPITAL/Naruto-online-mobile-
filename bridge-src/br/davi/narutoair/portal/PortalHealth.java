package br.davi.narutoair.portal;
import com.adobe.fre.*;
import java.lang.reflect.*;
/** Detect only an official launcher document with ALL linked styles missing. */
public final class PortalHealth {
 public static final String SCRIPT="(function(){if(location.hostname!=='gamebox3.narutowebgame.com'||!/^\\/gamebox\\/2\\.4\\.1\\/template\\/login\\.php$/.test(location.pathname)||document.readyState!=='complete')return;var links=document.querySelectorAll('link[rel=stylesheet]');var loaded=0;for(var i=0;i<links.length;i++)if(links[i].sheet)loaded++;if(links.length<2||loaded){window.__naMissingStyles=0;return;}window.__naMissingStyles=(window.__naMissingStyles||0)+1;if(window.__naMissingStyles>=2)location.replace('https://naruto.narutowebgame.com/pt/serverlist/');})();";
 // Initialize before increment; undefined must not become NaN.
 public static String script(){return SCRIPT+"\n"+PortalLoginRecovery.SCRIPT;}
 public static FREFunction function(){return new FREFunction(){public FREObject call(final FREContext c,FREObject[] a){
  try {
   if(MobilePortalContext.isPortalHidden())return FREObject.newObject(false);
   Field f=PortalContext.class.getDeclaredField("handler");f.setAccessible(true);Object handler=f.get(c);
   boolean accepted=(Boolean)handler.getClass().getMethod("post",Runnable.class).invoke(handler,new Runnable(){public void run(){try {
    if(MobilePortalContext.isPortalHidden())return;
    PortalBranding.install(c);
    Field field=PortalContext.class.getDeclaredField("webView");field.setAccessible(true);Object view=field.get(c);if(view==null)return;
    PortalAccess.inspect(c,view);
    Class<?> callback=Class.forName("android.webkit.ValueCallback");
    view.getClass().getMethod("evaluateJavascript",String.class,callback).invoke(view,script(),null);
   }catch(Exception e){c.dispatchStatusEventAsync("log","PORTAL HEALTH: verificacao indisponivel");}}});
   return FREObject.newObject(accepted);
  }catch(Exception e){return null;}
 }};}
}
