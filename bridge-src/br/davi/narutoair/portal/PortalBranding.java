package br.davi.narutoair.portal;
import com.adobe.fre.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.InputStream;
/** Native portal footer and an independent payment tab. Neither changes the game WebView. */
public final class PortalBranding {
 private static final Map<FREContext,Object> footers=Collections.synchronizedMap(new WeakHashMap<FREContext,Object>());
 private static final Map<FREContext,Object> payments=Collections.synchronizedMap(new WeakHashMap<FREContext,Object>());
 private static final Map<FREContext,Object> titles=Collections.synchronizedMap(new WeakHashMap<FREContext,Object>()),retries=Collections.synchronizedMap(new WeakHashMap<FREContext,Object>()),serverButtons=Collections.synchronizedMap(new WeakHashMap<FREContext,Object>());
 private static final String TITLE="Naruto Online Mobile\n\u00a9 2026 W1/W2 Solu\u00e7\u00f5es Capitais";
 public static final String W1="https://github.com/W1CAPITAL",W2="https://github.com/W2CAPITAL",SUPPORT="https://wa.me/5513991199349";
 public static String rechargeURL(String uid,String server) {
  String url="https://pay.narutowebgame.com/pay/web/index.html?gid=narutopt&lang=pt&windows_store=0";
  if(uid!=null && uid.matches("[A-Za-z0-9@._:+-]{1,254}"))try {url+="&uid="+java.net.URLEncoder.encode(uid,"UTF-8");}catch(java.io.UnsupportedEncodingException impossible){throw new AssertionError(impossible);}
  if(server!=null && server.matches("[0-9]{1,6}"))url+="&serverId="+server;
  return url;
 }
 private static Class<?> type(String n)throws Exception{return Class.forName(n);}
 private static Object call(Object o,String n,Class<?>[] t,Object... a)throws Exception{return o.getClass().getMethod(n,t).invoke(o,a);}
 private static Object create(String n,Object activity)throws Exception{return type(n).getConstructor(type("android.content.Context")).newInstance(activity);}
 private static Object activity(FREContext c)throws Exception{return call(c,"getActivity",new Class<?>[0]);}
 private static void post(FREContext c,Runnable r){try {Object a=activity(c);call(a,"runOnUiThread",new Class<?>[]{Runnable.class},r);}catch(Exception e){c.dispatchStatusEventAsync("log","Interface do portal indisponivel.");}}
 private static int dp(Object a,int value)throws Exception {Object res=call(a,"getResources",new Class<?>[0]),metrics=call(res,"getDisplayMetrics",new Class<?>[0]);return Math.round(value*type("android.util.DisplayMetrics").getField("density").getFloat(metrics));}
 private static void add(Object parent,Object child,Object params)throws Exception {call(parent,"addView",new Class<?>[]{type("android.view.View"),type("android.view.ViewGroup$LayoutParams")},child,params);}
 private static Object params(String n,int w,int h)throws Exception{return type(n).getConstructor(int.class,int.class).newInstance(w,h);}
 private static void click(Object view,final Runnable action)throws Exception {
  Class<?> listener=type("android.view.View$OnClickListener");
  call(view,"setOnClickListener",new Class<?>[]{listener},Proxy.newProxyInstance(listener.getClassLoader(),new Class<?>[]{listener},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){if(m.getName().equals("onClick"))action.run();return null;}}));
 }
 private static Object text(Object a,String value,int size)throws Exception {Object t=create("android.widget.TextView",a);call(t,"setText",new Class<?>[]{CharSequence.class},value);call(t,"setTextSize",new Class<?>[]{float.class},(float)size);call(t,"setTextColor",new Class<?>[]{int.class},0xfffafafa);call(t,"setGravity",new Class<?>[]{int.class},17);return t;}
 private static void external(FREContext c,String url){try {Object a=activity(c),uri=type("android.net.Uri").getMethod("parse",String.class).invoke(null,url);Object intent=type("android.content.Intent").getConstructor(String.class,type("android.net.Uri")).newInstance("android.intent.action.VIEW",uri);call(a,"startActivity",new Class<?>[]{type("android.content.Intent")},intent);}catch(Exception e){c.dispatchStatusEventAsync("log","Nao foi possivel abrir o link externo.");}}
 public static void install(final FREContext c) {
  try {
   Object a=activity(c),footer=footers.get(c);
   if(footer==null){
    footer=create("android.widget.LinearLayout",a);call(footer,"setOrientation",new Class<?>[]{int.class},0);call(footer,"setGravity",new Class<?>[]{int.class},16);call(footer,"setBackgroundColor",new Class<?>[]{int.class},0xff15202d);
    Object logo=create("android.widget.ImageView",a);
    try {Object assets=call(a,"getAssets",new Class<?>[0]);InputStream stream=(InputStream)call(assets,"open",new Class<?>[]{String.class},"brand/mobile-logo.png");try {Object drawable=type("android.graphics.drawable.Drawable").getMethod("createFromStream",InputStream.class,String.class).invoke(null,stream,"mobile-logo");call(logo,"setImageDrawable",new Class<?>[]{type("android.graphics.drawable.Drawable")},drawable);}finally{stream.close();}}catch(Exception ignored){}
    add(footer,logo,params("android.widget.LinearLayout$LayoutParams",dp(a,58),-1));
    Object title=text(a,TITLE,12);Object weighted=type("android.widget.LinearLayout$LayoutParams").getConstructor(int.class,int.class,float.class).newInstance(0,-1,1f);add(footer,title,weighted);
    Object github1=text(a,"GitHub\nW1",12),github2=text(a,"GitHub\nW2",12),support=text(a,"Suporte\n(13) 99119-9349",12);
    add(footer,github1,params("android.widget.LinearLayout$LayoutParams",dp(a,56),-1));add(footer,github2,params("android.widget.LinearLayout$LayoutParams",dp(a,56),-1));add(footer,support,params("android.widget.LinearLayout$LayoutParams",dp(a,118),-1));
    click(github1,new Runnable(){public void run(){external(c,W1);}});click(github2,new Runnable(){public void run(){external(c,W2);}});click(support,new Runnable(){public void run(){external(c,SUPPORT);}});
    Object retry=text(a,"RECARREGAR",11),servers=text(a,"SERVIDORES",11);
    add(footer,retry,params("android.widget.LinearLayout$LayoutParams",dp(a,86),-1));add(footer,servers,params("android.widget.LinearLayout$LayoutParams",dp(a,82),-1));
    titles.put(c,title);retries.put(c,retry);serverButtons.put(c,servers);
    click(retry,new Runnable(){public void run(){PortalAccess.reload(c);}});
    click(servers,new Runnable(){public void run(){if(MobilePortalContext.isPortalHidden())return;if(PortalAccess.restricted(c))external(c,PortalSession.SERVER_LIST);else c.dispatchStatusEventAsync("select-server","");}});
    Object layout=params("android.widget.FrameLayout$LayoutParams",-1,dp(a,58));type("android.widget.FrameLayout$LayoutParams").getField("gravity").setInt(layout,80);
    call(a,"addContentView",new Class<?>[]{type("android.view.View"),type("android.view.ViewGroup$LayoutParams")},footer,layout);footers.put(c,footer);
   }
   apply(c,!MobilePortalContext.isPortalHidden());
  }catch(Exception e){c.dispatchStatusEventAsync("log","Creditos do portal: inicializacao indisponivel.");}
 }
 public static void accessStatus(FREContext c,String status,int seconds) {
  try{if(MobilePortalContext.isPortalHidden())return;Object title=titles.get(c),retry=retries.get(c),servers=serverButtons.get(c);if(title==null)return;
   boolean limited=status.equals("blocked") || status.equals("challenge");String message=status.equals("blocked")?"Site bloqueou esta conexao":status.equals("challenge")?"Aguarde a verificacao do site":"";
   call(title,"setText",new Class<?>[]{CharSequence.class},TITLE+(limited?"\n"+message:""));call(title,"setTextSize",new Class<?>[]{float.class},limited?10f:12f);
   call(retry,"setText",new Class<?>[]{CharSequence.class},limited && seconds>0?"AGUARDE "+seconds+"s":"RECARREGAR");
   call(servers,"setText",new Class<?>[]{CharSequence.class},limited?"ABRIR SITE":"SERVIDORES");
  }catch(Exception ignored){}
 }
 private static void apply(FREContext c,boolean shown)throws Exception {
  Object footer=footers.get(c);if(footer==null)return;
  call(footer,"setVisibility",new Class<?>[]{int.class},shown?0:8);
  if(shown)call(footer,"bringToFront",new Class<?>[0]);
  Field field=PortalContext.class.getDeclaredField("webView");field.setAccessible(true);Object web=field.get(c);
  if(web!=null){Object layout=call(web,"getLayoutParams",new Class<?>[0]);if(layout!=null && type("android.view.ViewGroup$MarginLayoutParams").isInstance(layout)){type("android.view.ViewGroup$MarginLayoutParams").getField("bottomMargin").setInt(layout,shown?dp(activity(c),58):0);call(web,"setLayoutParams",new Class<?>[]{type("android.view.ViewGroup$LayoutParams")},layout);}}
 }
 public static void visibility(final FREContext c,final boolean shown){post(c,new Runnable(){public void run(){try{apply(c,shown);}catch(Exception ignored){}}});}
 public static FREFunction recharge(){return new FREFunction(){public FREObject call(final FREContext c,FREObject[] args){try {
  if(args==null || args.length!=2)return FREObject.newObject(false);
  final String url=rechargeURL(args[0].getAsString(),args[1].getAsString());
  post(c,new Runnable(){public void run(){openPayment(c,url);}});return FREObject.newObject(true);
 }catch(Exception e){return null;}}};}
 private static void openPayment(final FREContext c,String url) {
  Object overlay=null;
  try {
   closePayment(c);Object a=activity(c);overlay=create("android.widget.LinearLayout",a);call(overlay,"setOrientation",new Class<?>[]{int.class},1);call(overlay,"setBackgroundColor",new Class<?>[]{int.class},0xffffffff);
   Object close=text(a,"\u2190 VOLTAR AO JOGO  |  RECARGA OFICIAL",16);call(close,"setBackgroundColor",new Class<?>[]{int.class},0xff15202d);click(close,new Runnable(){public void run(){closePayment(c);MobileInputBridge.focus(c);}});add(overlay,close,params("android.widget.LinearLayout$LayoutParams",-1,dp(a,48)));
   Object web=create("android.webkit.WebView",a),settings=call(web,"getSettings",new Class<?>[0]);call(settings,"setJavaScriptEnabled",new Class<?>[]{boolean.class},true);call(settings,"setDomStorageEnabled",new Class<?>[]{boolean.class},true);
   call(web,"setWebViewClient",new Class<?>[]{type("android.webkit.WebViewClient")},type("android.webkit.WebViewClient").newInstance());
   Object cookies=type("android.webkit.CookieManager").getMethod("getInstance").invoke(null);call(cookies,"setAcceptThirdPartyCookies",new Class<?>[]{type("android.webkit.WebView"),boolean.class},web,true);
   add(overlay,web,params("android.widget.LinearLayout$LayoutParams",-1,-1));Object layout=params("android.widget.FrameLayout$LayoutParams",-1,-1);call(a,"addContentView",new Class<?>[]{type("android.view.View"),type("android.view.ViewGroup$LayoutParams")},overlay,layout);
   payments.put(c,overlay);call(web,"loadUrl",new Class<?>[]{String.class},url);
   c.dispatchStatusEventAsync("log","Recarga oficial aberta em aba separada; jogo preservado.");
  }catch(Exception e){if(overlay!=null)remove(overlay);payments.remove(c);c.dispatchStatusEventAsync("log","Aba de recarga indisponivel; abrindo pagina oficial no navegador.");external(c,url);}
 }
 private static void remove(Object view){try {Object p=call(view,"getParent",new Class<?>[0]);if(p!=null)call(p,"removeView",new Class<?>[]{type("android.view.View")},view);}catch(Exception ignored){}}
 private static void closePayment(FREContext c){Object overlay=payments.remove(c);if(overlay==null)return;try {Object web=call(overlay,"getChildAt",new Class<?>[]{int.class},1);call(web,"stopLoading",new Class<?>[0]);call(web,"loadUrl",new Class<?>[]{String.class},"about:blank");call(web,"destroy",new Class<?>[0]);}catch(Exception ignored){}remove(overlay);}
 public static void release(final FREContext c){post(c,new Runnable(){public void run(){closePayment(c);titles.remove(c);retries.remove(c);serverButtons.remove(c);Object footer=footers.remove(c);if(footer!=null)remove(footer);}});}
}
