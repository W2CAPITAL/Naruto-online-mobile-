package br.davi.narutoair.portal;
import com.adobe.fre.*;
import java.lang.reflect.*;
import java.net.URI;
/** Preserve the official web session; only explicit account switching clears it. */
public final class PortalSession {
 public static final String SERVER_LIST="https://naruto.narutowebgame.com/pt/serverlist/";
 public static String openingUrl(String url) {
  try {URI u=new URI(url);if("gamebox3.narutowebgame.com".equalsIgnoreCase(u.getHost()) && u.getPath().endsWith("/template/login.php"))return SERVER_LIST;}catch(Exception ignored){}
  return url;
 }
 public static boolean serverList(String url) {
  try {URI u=new URI(url);String p=u.getPath();return ("https".equals(u.getScheme()) || "http".equals(u.getScheme())) &&
   "naruto.narutowebgame.com".equalsIgnoreCase(u.getHost()) && ("/pt/serverlist".equals(p) || "/pt/serverlist/".equals(p));}catch(Exception e){return false;}
 }
 public static boolean navigation(FREContext c,String url) {
  if(!MobilePortalContext.isPortalHidden() || !serverList(url))return false;
  c.dispatchStatusEventAsync("select-server","");return true;
 }
 private static Object field(FREContext c,String name)throws Exception {Field f=PortalContext.class.getDeclaredField(name);f.setAccessible(true);return f.get(c);}
 private static boolean queue(FREContext c,Runnable r) {
  try {Object h=field(c,"handler");return (Boolean)h.getClass().getMethod("post",Runnable.class).invoke(h,r);}catch(Exception e){return false;}
 }
 public static void persist(final FREContext c) {queue(c,new Runnable(){public void run(){try {
  Class<?> cm=Class.forName("android.webkit.CookieManager");Object cookies=cm.getMethod("getInstance").invoke(null);cm.getMethod("flush").invoke(cookies);
 }catch(Exception ignored){}}});}
 public static FREFunction resetAccount(){return new FREFunction(){public FREObject call(final FREContext c,FREObject[] a){
  if(a.length!=0)return null;
  boolean ok=queue(c,new Runnable(){public void run(){try {
   // Stop the old document before clearing; it must not recreate its cookies.
   Object view=field(c,"webView");if(view!=null){Class<?> v=Class.forName("android.webkit.WebView");v.getMethod("stopLoading").invoke(view);v.getMethod("loadUrl",String.class).invoke(view,"about:blank");}
   Class<?> ws=Class.forName("android.webkit.WebStorage");ws.getMethod("deleteAllData").invoke(ws.getMethod("getInstance").invoke(null));
   final Class<?> cm=Class.forName("android.webkit.CookieManager");final Object cookies=cm.getMethod("getInstance").invoke(null);
   Class<?> callback=Class.forName("android.webkit.ValueCallback");Object reply=Proxy.newProxyInstance(callback.getClassLoader(),new Class<?>[]{callback},new InvocationHandler(){
    public Object invoke(Object proxy,Method m,Object[] args)throws Throwable {if(m.getName().equals("onReceiveValue")){cm.getMethod("flush").invoke(cookies);c.dispatchStatusEventAsync("account-cleared","");}return null;}
   });
   cm.getMethod("removeAllCookies",callback).invoke(cookies,reply);
  }catch(Exception e){c.dispatchStatusEventAsync("account-error","Nao foi possivel sair da conta. Tente novamente.");}}});
  try{return FREObject.newObject(ok);}catch(Exception e){return null;}
 } };}
}
