package br.davi.narutoair.portal;
import com.adobe.fre.*;
import java.lang.reflect.*;
import java.util.*;
/** Queue JS on Android's UI thread; never block AIR waiting for WebView. */
public final class BrowserPageBridge {
 private static final Map<FREContext,String> nonces=Collections.synchronizedMap(new WeakHashMap<FREContext,String>());
 public static String latestTrace(String text) {
  if(text.length()<=12000)return text;
  int cut=text.indexOf('\n',text.length()-12000);
  return "TRECHO FINAL DO LOG (linhas anteriores omitidas)\n"+text.substring(cut<0?text.length()-12000:cut+1);
 }
 public static void callback(FREContext context,String nonce,String json) {
  if(nonce==null || json==null || json.length()>65536 || !nonce.equals(nonces.get(context)))return;
  context.dispatchStatusEventAsync("browser-callback",json);
 }
 private static Object field(FREContext context,String name)throws Exception {
  Field f=PortalContext.class.getDeclaredField(name);f.setAccessible(true);return f.get(context);
 }
 /** Called on the UI thread before our popup callback replaces the active view. */
 public static void beforeSwitch(FREContext context,Object replacement) {
  try {
   Object previous=field(context,"webView");
   if(previous!=null && previous!=replacement)
    previous.getClass().getMethod("setVisibility",int.class).invoke(previous,8);
  }catch(Exception e){throw new IllegalStateException("Nao foi possivel ocultar a janela anterior do portal.",e);}
 }
 public static FREFunction configure() {
  return new FREFunction(){public FREObject call(FREContext c,FREObject[] a) {
   try {
    String nonce=a.length==1?a[0].getAsString():"";
    if(nonce.length()>128)throw new IllegalArgumentException();
    if(nonce.isEmpty())nonces.remove(c);else nonces.put(c,nonce);
    return FREObject.newObject(true);
   }catch(Exception e){return null;}
  }};
 }
 public static FREFunction evaluate() {
  return new FREFunction(){public FREObject call(final FREContext c,FREObject[] a) {
   try {
    if(!nonces.containsKey(c) || a.length!=1)return null;
    final String script=a[0].getAsString();if(script.length()>65536)return null;
    Object handler=field(c,"handler");if(handler==null || field(c,"webView")==null)return null;
    Boolean accepted=(Boolean)handler.getClass().getMethod("post",Runnable.class).invoke(handler,new Runnable(){public void run(){
     try {
      Object view=field(c,"webView");
      Class<?> callback=Class.forName("android.webkit.ValueCallback");
      view.getClass().getMethod("evaluateJavascript",String.class,callback).invoke(view,script,null);
     }catch(Exception e){c.dispatchStatusEventAsync("browser-error","Falha ao executar chamada na pagina oficial.");}
    }});
    return FREObject.newObject(accepted.booleanValue());
   }catch(Exception e){return null;}
  }};
 }
}
