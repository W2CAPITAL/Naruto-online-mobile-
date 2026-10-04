package com.adobe.air;
public class AIRWindowSurfaceView extends android.view.SurfaceView {
 private int mHt=720;public int logicalWidth=1604,logicalHeight=720,panning;
 public void surfaceChanged(android.view.SurfaceHolder h,int format,int w,int height){if(height<mHt){panning++;return;}mHt=height;logicalWidth=w;logicalHeight=height;}
}
