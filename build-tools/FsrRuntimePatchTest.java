import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import jdk.internal.org.objectweb.asm.*;
public class FsrRuntimePatchTest {
 public static void main(String[] args)throws Exception {
  byte[] source;try(ZipFile jar=new ZipFile(args[0])){source=jar.getInputStream(jar.getEntry("com/adobe/air/FlashEGL14.class")).readAllBytes();}
  byte[] patched=FsrRuntimePatch.patch(source);int[] create={0},before={0},swap={0};
  new ClassReader(patched).accept(new ClassVisitor(Opcodes.ASM7){public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){return new MethodVisitor(Opcodes.ASM7){
   public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){
    if(owner.equals("br/davi/narutoair/fsr/FsrRenderer") && name.equals("register"))create[0]++;
    if(owner.equals("br/davi/narutoair/fsr/FsrRenderer") && name.equals("beforeSwap"))before[0]++;
    if(owner.equals("android/opengl/EGL14") && name.equals("eglSwapBuffers")){if(before[0]!=1)throw new AssertionError("Wrong hook order");swap[0]++;}
   }
  };}},0);
  if(create[0]!=1 || before[0]!=1 || swap[0]!=1)throw new AssertionError("Wrong hook count");
  try{FsrRuntimePatch.patch(patched);throw new AssertionError("Double patch accepted");}catch(IllegalArgumentException expected){}
  try(ZipFile jar=new ZipFile(args[0]);ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(Paths.get(args[1])))){
   Enumeration<? extends ZipEntry> entries=jar.entries();while(entries.hasMoreElements()){ZipEntry e=entries.nextElement();out.putNextEntry(new ZipEntry(e.getName()));out.write(e.getName().equals("com/adobe/air/FlashEGL14.class")?patched:jar.getInputStream(e).readAllBytes());out.closeEntry();}
  }
  System.out.println("PASS: AIR EGL14 exact hook placement, original swap retained, duplicate patch rejected");
 }
}
