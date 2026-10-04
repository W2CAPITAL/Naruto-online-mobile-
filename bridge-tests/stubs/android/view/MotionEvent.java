package android.view;
public class MotionEvent {
 public int action,source;public float x,y,x2,y2;public long downTime;public boolean recycled;
 public static MotionEvent obtain(long down,long now,int action,float x,float y,int meta){MotionEvent m=new MotionEvent();m.action=action;m.x=x;m.y=y;m.downTime=down;return m;}
 public void setSource(int value){source=value;}
 public int getActionMasked(){return action;}public float getRawX(){return x;}public float getRawY(){return y;}
 public void recycle(){recycled=true;}
 public static MotionEvent obtain(MotionEvent value){return value.copy();}
 public void transform(android.graphics.Matrix m){x*=m.sx;y*=m.sy;x2*=m.sx;y2*=m.sy;}
 public MotionEvent copy(){MotionEvent e=obtain(downTime,downTime,action,x,y,0);e.source=source;e.x2=x2;e.y2=y2;return e;}
}
