package android.view;
import java.util.*;
public class SurfaceView extends View {
 public final Holder holder=new Holder();public SurfaceHolder getHolder(){return holder;}
 public class Holder implements SurfaceHolder {
  public List<Callback> callbacks=new ArrayList<Callback>();public int bw=1604,bh=720;public boolean fixed,fail,defer;public int requestedW,requestedH,fixedCalls,nativeCalls;
  public void addCallback(Callback cb){callbacks.add(cb);}public void removeCallback(Callback cb){callbacks.remove(cb);}
  public void setFixedSize(int w,int h){fixedCalls++;if(fail)throw new IllegalStateException("injected surface failure");fixed=true;requestedW=w;requestedH=h;if(!defer)ack();}
  public void setSizeFromLayout(){nativeCalls++;fixed=false;requestedW=width;requestedH=height;if(!defer)ack();}
  public Surface surface=new Surface();public Surface getSurface(){return surface;}
  public android.graphics.Rect getSurfaceFrame(){return new android.graphics.Rect(bw,bh);}
  public void ack(){bw=requestedW;bh=requestedH;if(SurfaceView.this instanceof com.adobe.air.AIRWindowSurfaceView)((com.adobe.air.AIRWindowSurfaceView)SurfaceView.this).surfaceChanged(this,1,bw,bh);for(Callback cb:new ArrayList<Callback>(callbacks))cb.surfaceChanged(this,1,bw,bh);}
 }
}
