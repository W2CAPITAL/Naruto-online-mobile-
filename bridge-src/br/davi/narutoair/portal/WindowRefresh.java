package br.davi.narutoair.portal;
import com.adobe.fre.FREContext;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
/** Optional supported-mode preference. No system settings or fixed FPS claim. */
final class WindowRefresh {
 private final WeakReference<FREContext> context;private final Object view;
 private Object window;private int oldMode,selectedMode;private float oldRate,selectedRate;
 WindowRefresh(FREContext c,Object v){context=new WeakReference<FREContext>(c);view=v;}
 private static Object call(Object o,String name,Class<?>[] types,Object... args)throws Exception{return o.getClass().getMethod(name,types).invoke(o,args);}
 private static Object get(Object o,String n)throws Exception{return call(o,n,new Class<?>[0]);}
 float apply(float limit){
  try {
   Object display=get(view,"getDisplay"),current=get(display,"getMode"),modes=get(display,"getSupportedModes");
   int w=((Number)get(current,"getPhysicalWidth")).intValue(),h=((Number)get(current,"getPhysicalHeight")).intValue();
   Object chosen=null,lowest=null;float chosenRate=0,lowestRate=Float.POSITIVE_INFINITY;
   for(int i=0;i<Array.getLength(modes);i++){
    Object mode=Array.get(modes,i);float rate=((Number)get(mode,"getRefreshRate")).floatValue();
    if(((Number)get(mode,"getPhysicalWidth")).intValue()!=w || ((Number)get(mode,"getPhysicalHeight")).intValue()!=h || !Float.isFinite(rate) || rate<=0 || rate>1000)continue;
    if(rate<lowestRate){lowest=mode;lowestRate=rate;}
    if((limit==0 || rate<=limit) && (chosen==null || rate>chosenRate)){chosen=mode;chosenRate=rate;}
   }
   if(chosen==null){chosen=lowest;chosenRate=lowestRate;}
   if(chosen==null)return limit>0?limit:60f;
   float cadence=limit>0?Math.min(limit,chosenRate):chosenRate;
   FREContext c=context.get();if(c==null)return cadence;
   Object activeWindow=get(get(c,"getActivity"),"getWindow"),attrs=get(activeWindow,"getAttributes");
   Field modeField=attrs.getClass().getField("preferredDisplayModeId"),rateField=attrs.getClass().getField("preferredRefreshRate");
   if(window==null){window=activeWindow;oldMode=modeField.getInt(attrs);oldRate=rateField.getFloat(attrs);}
   selectedMode=((Number)get(chosen,"getModeId")).intValue();selectedRate=chosenRate;
   if(modeField.getInt(attrs)!=selectedMode || rateField.getFloat(attrs)!=selectedRate){
    modeField.setInt(attrs,selectedMode);rateField.setFloat(attrs,selectedRate);
    call(activeWindow,"setAttributes",new Class<?>[]{Class.forName("android.view.WindowManager$LayoutParams")},attrs);
   }
   float actual=((Number)get(current,"getRefreshRate")).floatValue();
   c.dispatchStatusEventAsync("graphics","display:"+actual+":"+cadence+":"+limit);
   return cadence;
  }catch(Exception unavailable){} // Preferences are advisory and may be ignored by Android.
  return limit>0?limit:60f;
 }
 void restore(){
  if(window==null)return;
  try {
   Object attrs=get(window,"getAttributes");Field mode=attrs.getClass().getField("preferredDisplayModeId"),rate=attrs.getClass().getField("preferredRefreshRate");
   // Respect a later preference set by another component of this window.
   if(mode.getInt(attrs)==selectedMode && rate.getFloat(attrs)==selectedRate){mode.setInt(attrs,oldMode);rate.setFloat(attrs,oldRate);call(window,"setAttributes",new Class<?>[]{Class.forName("android.view.WindowManager$LayoutParams")},attrs);}
  }catch(Exception unavailable){}window=null;
 }
}
