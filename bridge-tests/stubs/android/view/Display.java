package android.view;
public class Display {
 public boolean unavailable;public Mode current=new Mode(1,1604,720,60);
 public Mode[] modes={current,new Mode(2,1604,720,90),new Mode(3,1604,720,120),new Mode(4,2000,1000,90)};
 public Mode getMode(){if(unavailable)throw new IllegalStateException();return current;}public Mode[] getSupportedModes(){return modes;}
 public static class Mode {final int id,w,h;final float hz;public Mode(int i,int x,int y,float rate){id=i;w=x;h=y;hz=rate;}public int getModeId(){return id;}public int getPhysicalWidth(){return w;}public int getPhysicalHeight(){return h;}public float getRefreshRate(){return hz;}}
}
