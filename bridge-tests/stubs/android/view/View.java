package android.view;
import java.util.*;
public class View {
 public interface OnClickListener {void onClick(View v);}
 public OnClickListener clickListener;public int visibility;public ViewGroup.LayoutParams params;public int fronts;
 public void setOnClickListener(OnClickListener l){clickListener=l;}public void setVisibility(int v){visibility=v;}public void bringToFront(){fronts++;}public ViewGroup.LayoutParams getLayoutParams(){return params;}public void setLayoutParams(ViewGroup.LayoutParams p){params=p;}public void setBackgroundColor(int c){}
 public interface OnTouchListener {boolean onTouch(View v,MotionEvent e);}
 public interface OnGenericMotionListener {boolean onGenericMotion(View v,MotionEvent e);}
 public interface OnLayoutChangeListener {void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob);}
 public OnTouchListener touch;public OnLayoutChangeListener layout;
 public OnGenericMotionListener generic;
 public float x,y;public int width=1604,height=720,clicks;public View parent;public boolean pressed;
 public float getX(){return x;}public float getY(){return y;}public void setX(float value){x=value;}public void setY(float value){y=value;}
 public View getParent(){return parent;}public void setPressed(boolean value){pressed=value;}public boolean performClick(){clicks++;if(clickListener!=null)clickListener.onClick(this);return true;}
 public boolean post(Runnable r){r.run();return true;}
 public void setOnTouchListener(OnTouchListener l){touch=l;}public void addOnLayoutChangeListener(OnLayoutChangeListener l){layout=l;}
 public void setOnGenericMotionListener(OnGenericMotionListener l){generic=l;}public void removeOnLayoutChangeListener(OnLayoutChangeListener l){if(layout==l)layout=null;}
 public boolean focused=false;
 public List<MotionEvent> motion=new ArrayList<MotionEvent>();
 public List<KeyEvent> keys=new ArrayList<KeyEvent>();
 public boolean requestFocus(){focused=true;return true;}
 public int getWidth(){return width;}
 public int getHeight(){return height;}
 public Display display=new Display();public Display getDisplay(){return display;}
 public boolean dispatchTouchEvent(MotionEvent event){if(touch!=null && touch.onTouch(this,event))return true;return onTouchEvent(event);}
 public boolean dispatchGenericMotionEvent(MotionEvent event){if(generic!=null && generic.onGenericMotion(this,event))return true;return onGenericMotionEvent(event);}
 public boolean onTouchEvent(MotionEvent event){motion.add(event.copy());return true;}
 public boolean onGenericMotionEvent(MotionEvent event){motion.add(event.copy());return true;}
 public boolean dispatchKeyEvent(KeyEvent event){keys.add(event);return true;}
}
