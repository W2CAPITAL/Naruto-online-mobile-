package br.davi.narutoair.portal;
import com.adobe.fre.*;
import java.util.*;
public class PortalContext extends FREContext {
    private android.os.Handler handler=new android.os.Handler();
    private android.webkit.WebView webView=new android.webkit.WebView();
    private android.view.View nativeLogButton;
    public void dispose(){}
    public android.view.View button(){return nativeLogButton;}
    public void button(android.view.View value){nativeLogButton=value;}
    public Object getActivity(){return new Object();}
    public android.os.Handler handler(){return handler;}
    public android.webkit.WebView view(){return webView;}
    public static android.webkit.WebView access$2402(PortalContext context,android.webkit.WebView value){context.webView=value;return value;}
    public class PortalJsBridge {public PortalJsBridge(){}}
    public Map<String,FREFunction> getFunctions(){
        Map<String,FREFunction> functions=new HashMap<String,FREFunction>();
        functions.put("ping",(context,args)->FREObject.newObject("original-ping"));
        for(final String name:new String[]{"hide","show","open","close"})functions.put(name,(context,args)->FREObject.newObject("original-"+name));
        return Collections.unmodifiableMap(functions);
    }
}
