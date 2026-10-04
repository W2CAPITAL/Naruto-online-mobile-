package br.davi.narutoair.portal;
import com.adobe.fre.*;
import java.io.*;
import java.lang.reflect.*;

/** Native APK assets, independent of login and game protocol. No asset IO on the UI thread. */
public final class KaguyaBridge {
    private static FREObject flag(boolean value) { try { return FREObject.newObject(value); } catch(Exception unavailable) { return null; } }
    public static FREFunction function() { return new FREFunction() {
        public FREObject call(FREContext context,FREObject[] args) {
            try {
                if(args.length != 2) return flag(false);
                String enabled=args[0].getAsString();int style=Integer.parseInt(args[1].getAsString());
                if(!(enabled.equals("true") || enabled.equals("false")) || style<0 || style>4) return flag(false);
                Object activity=context.getClass().getMethod("getActivity").invoke(context);
                final Object assets=activity.getClass().getMethod("getAssets").invoke(activity);
                final Method open=assets.getClass().getMethod("open",String.class);
                KaguyaResources.configure(Boolean.parseBoolean(enabled),style,new KaguyaResources.Assets() {
                    public InputStream open(String path) throws IOException {
                        try { return (InputStream)open.invoke(assets,path); }
                        catch(Exception failed){throw new IOException("APK asset unavailable",failed);}
                    }
                });
                try {
                    File cache=(File)activity.getClass().getMethod("getCacheDir").invoke(activity);
                    if(cache!=null)StaticAssetCache.configure(new File(cache,"naruto-static-v1"));
                } catch(Exception unavailable) { /* Mod does not depend on disk cache. */ }
                return flag(true);
            } catch(Exception unavailable) { return flag(false); }
        }
    }; }
}
