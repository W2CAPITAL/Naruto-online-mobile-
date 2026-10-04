package br.davi.narutoair.portal;
import com.adobe.fre.*;
/** Optional renderer loaded only after explicit user choice. All failures leave normal AIR active. */
public final class FsrBridge {
 public static FREFunction function(){return new FREFunction(){public FREObject call(final FREContext c,FREObject[] args){
  try{if(args.length!=2)return null;String choice=args[0].getAsString();if(!choice.equals("true") && !choice.equals("false"))return null;
   final int epoch=Integer.parseInt(args[1].getAsString());if(epoch<=0)return null;
   final boolean enabled=choice.equals("true");Object activity=c.getClass().getMethod("getActivity").invoke(c);
   activity.getClass().getMethod("runOnUiThread",Runnable.class).invoke(activity,new Runnable(){public void run(){configure(c,enabled,epoch);}});
   return FREObject.newObject(true);
  }catch(Throwable unavailable){return null;}
 }};}
 static void configure(FREContext c,boolean enabled,int epoch){try{
  Object view=GraphicsBridge.viewFor(c);if(enabled && (!MobilePortalContext.isPortalHidden() || view==null))throw new IllegalStateException();
  if(view==null)return;
  Object ok=Class.forName("br.davi.narutoair.fsr.FsrRenderer").getMethod("configure",FREContext.class,Object.class,boolean.class,int.class).invoke(null,c,view,enabled,epoch);
  if(enabled && !Boolean.TRUE.equals(ok))throw new IllegalStateException();
 }catch(Throwable unavailable){if(enabled)try{c.dispatchStatusEventAsync("fsr",epoch+":fallback:Compositor indisponivel; imagem original ativa");}catch(Throwable ignored){}}
 }
 public static void stop(final FREContext c){try{Object a=c.getClass().getMethod("getActivity").invoke(c);a.getClass().getMethod("runOnUiThread",Runnable.class).invoke(a,new Runnable(){public void run(){configure(c,false,0);}});}catch(Throwable ignored){}}
}
