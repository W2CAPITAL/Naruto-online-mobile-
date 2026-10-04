package android.opengl;
import java.util.*;
/** Runtime state recorder for executing the production GL save/restore class. Never packaged. */
public class GLES30 {
 public static final Map<Integer,int[]> values=new HashMap<Integer,int[]>();
 public static final Map<Integer,Integer> textures=new HashMap<Integer,Integer>();
 public static final Set<Integer> enabled=new HashSet<Integer>();
 public static int active=0x84C0,sampler;
 public static void glGetIntegerv(int name,int[] out,int offset){
  int[] v=name==0x84E0?new int[]{active}:name==0x8069?new int[]{textures.getOrDefault(active,0)}:name==0x8919?new int[]{sampler}:values.getOrDefault(name,new int[]{0});System.arraycopy(v,0,out,offset,v.length);
 }
 public static boolean glIsEnabled(int cap){return enabled.contains(cap);}
 public static void glActiveTexture(int unit){active=unit;}
 public static void glBindTexture(int target,int texture){textures.put(active,texture);}
 public static void glBindSampler(int unit,int value){if(unit==0)sampler=value;}
 public static void glUseProgram(int value){values.put(0x8B8D,new int[]{value});}
 public static void glBindVertexArray(int value){values.put(0x85B5,new int[]{value});}
 public static void glBindFramebuffer(int target,int value){values.put(target==0x8CA9?0x8CA6:0x8CAA,new int[]{value});}
 public static void glViewport(int a,int b,int c,int d){values.put(0x0BA2,new int[]{a,b,c,d});}
 public static void glScissor(int a,int b,int c,int d){values.put(0x0C10,new int[]{a,b,c,d});}
 public static void glColorMask(boolean a,boolean b,boolean c,boolean d){values.put(0x0C23,new int[]{a?1:0,b?1:0,c?1:0,d?1:0});}
 public static void glEnable(int cap){enabled.add(cap);}
 public static void glDisable(int cap){enabled.remove(cap);}
}
