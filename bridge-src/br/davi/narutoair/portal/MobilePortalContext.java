package br.davi.narutoair.portal;
import com.adobe.fre.FREFunction;
import java.util.*;
/** Inherits the existing portal; adds only in-memory client adaptation. */
public final class MobilePortalContext extends PortalContext {
    private static volatile boolean portalHidden;
    public static boolean isPortalHidden(){return portalHidden;}
    private static FREFunction visibility(final FREFunction original,final boolean hidden,final boolean focus) {
        return new FREFunction() {
            public com.adobe.fre.FREObject call(com.adobe.fre.FREContext context,com.adobe.fre.FREObject[] args) {
                portalHidden=hidden;
                if(!hidden)FsrBridge.stop(context);
                com.adobe.fre.FREObject result=original.call(context,args);
                PortalBranding.visibility(context,!hidden);
                if(hidden)PortalSession.persist(context);
                if(focus)MobileInputBridge.focus(context);
                return result;
            }
        };
    }
    @Override public Map<String,FREFunction> getFunctions() {
        Map<String,FREFunction> functions=new HashMap<String,FREFunction>(super.getFunctions());
        functions.put("mobileInput",MobileInputBridge.function());
        functions.put("portalHealth",PortalHealth.function());
        functions.put("resetAccount",PortalSession.resetAccount());
        functions.put("graphicsScale",GraphicsBridge.function());
        functions.put("graphicsFPS",GraphicsBridge.fpsFunction());
        functions.put("graphicsFSR",FsrBridge.function());
        functions.put("kaguyaMode",KaguyaBridge.function());
        functions.put("audioDiagnostic",AudioDiagnostic.function());
        final FREFunction recharge=PortalBranding.recharge();
        functions.put("recharge",new FREFunction(){public com.adobe.fre.FREObject call(com.adobe.fre.FREContext c,com.adobe.fre.FREObject[] a){FsrBridge.stop(c);return recharge.call(c,a);}});
        functions.put("adaptSwfBytes",new AdaptSwfBytes());
        functions.put("browserConfigure",BrowserPageBridge.configure());
        functions.put("browserEval",BrowserPageBridge.evaluate());
        for(String name:new String[]{"hide","close","show","open"}) {
            FREFunction original=functions.get(name);
            if(original!=null)functions.put(name,visibility(original,name.equals("hide") || name.equals("close"),name.equals("hide")));
        }
        return functions;
    }
    @Override public void dispose(){PortalAccess.release(this);PortalBranding.release(this);GraphicsBridge.release(this);PortalSession.persist(this);super.dispose();}
}
