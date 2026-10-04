package android.webkit;
import java.net.*; import java.util.*;
public class CookieManager {
 private static final CookieManager INSTANCE = new CookieManager();
 private final java.net.CookieManager store = new java.net.CookieManager(null, CookiePolicy.ACCEPT_ALL);
 public static CookieManager getInstance(){return INSTANCE;}
 public String getCookie(String url){try{return String.join("; ",store.get(new URI(url),Collections.emptyMap()).getOrDefault("Cookie",Collections.emptyList()));}catch(Exception e){throw new RuntimeException(e);}}
 public void setCookie(String url,String value){try{store.put(new URI(url),Collections.singletonMap("Set-Cookie",Collections.singletonList(value)));}catch(Exception e){throw new RuntimeException(e);}}
 public void setAcceptThirdPartyCookies(WebView w,boolean enabled){}
 public void flush(){}
 public void removeAllCookies(android.webkit.ValueCallback<Boolean> reply){store.getCookieStore().removeAll();reply.onReceiveValue(true);}
}
