package android.os;
import java.util.*;
public class Handler {
 public List<Runnable> pending=new ArrayList<Runnable>();
 public boolean accept=true;
 public boolean post(Runnable task){if(accept)pending.add(task);return accept;}
 public void drain(){for(Runnable task:new ArrayList<Runnable>(pending))task.run();pending.clear();}
}
