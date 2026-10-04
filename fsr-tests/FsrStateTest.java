import android.opengl.GLES30;
import java.lang.reflect.*;
import java.util.*;
public class FsrStateTest {
 public static void main(String[] args)throws Exception {
  GLES30.glUseProgram(123);GLES30.glBindVertexArray(45);GLES30.glBindFramebuffer(0x8CA9,67);GLES30.glBindFramebuffer(0x8CA8,89);
  GLES30.glViewport(8,6,1283,576);GLES30.glScissor(10,20,100,200);GLES30.glColorMask(true,false,true,false);
  GLES30.glActiveTexture(0x84C0);GLES30.glBindTexture(0x0DE1,101);GLES30.glBindSampler(0,55);GLES30.glActiveTexture(0x84C7);GLES30.glBindTexture(0x0DE1,202);
  for(int cap:new int[]{0x0C11,0x0BE2,0x0B71,0x8C89})GLES30.glEnable(cap);
  Map<Integer,int[]> saved=new HashMap<Integer,int[]>();for(Map.Entry<Integer,int[]> e:GLES30.values.entrySet())saved.put(e.getKey(),e.getValue().clone());
  Set<Integer> caps=new HashSet<Integer>(GLES30.enabled);
  Class<?> type=Class.forName("br.davi.narutoair.fsr.FsrRenderer$State");Constructor<?> constructor=type.getDeclaredConstructor();constructor.setAccessible(true);Object state=constructor.newInstance();
  GLES30.glUseProgram(999);GLES30.glBindVertexArray(0);GLES30.glBindFramebuffer(0x8CA9,0);GLES30.glBindFramebuffer(0x8CA8,0);
  GLES30.glViewport(0,0,1604,720);GLES30.glScissor(0,0,1604,720);GLES30.glColorMask(true,true,true,true);GLES30.enabled.clear();GLES30.enabled.add(0x0B90);
  GLES30.glActiveTexture(0x84C0);GLES30.glBindTexture(0x0DE1,333);GLES30.glBindSampler(0,0);
  Method restore=type.getDeclaredMethod("restore");restore.setAccessible(true);restore.invoke(state);
  for(Map.Entry<Integer,int[]> e:saved.entrySet())if(!Arrays.equals(e.getValue(),GLES30.values.get(e.getKey())))throw new AssertionError("GL state corrupted: "+e.getKey());
  if(!caps.equals(GLES30.enabled) || GLES30.active!=0x84C7 || GLES30.textures.get(0x84C0)!=101 || GLES30.textures.get(0x84C7)!=202 || GLES30.sampler!=55)throw new AssertionError("Texture or enable state corrupted");
  // Disabled/unregistered hook executes zero GL calls and does not need an Android surface.
  br.davi.narutoair.fsr.FsrRenderer.beforeSwap(new Object());br.davi.narutoair.fsr.FsrRenderer.register(new Object(),null);
  System.out.println("PASS: production GL state restoration (program, VAO, FBOs, viewport, scissor, color mask, caps, texture units and sampler); inactive hook is no-op");
 }
}
