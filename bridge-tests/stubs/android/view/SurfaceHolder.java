package android.view;
public interface SurfaceHolder {
 interface Callback {void surfaceCreated(SurfaceHolder h);void surfaceChanged(SurfaceHolder h,int format,int width,int height);void surfaceDestroyed(SurfaceHolder h);}
 Surface getSurface();
 void addCallback(Callback cb);void removeCallback(Callback cb);void setFixedSize(int w,int h);void setSizeFromLayout();android.graphics.Rect getSurfaceFrame();
}
