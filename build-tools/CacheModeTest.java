import jdk.internal.org.objectweb.asm.*;
import java.util.*;
public class CacheModeTest {
 static byte[] fixture() {
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,"CacheFixture",null,"java/lang/Object",null);
  w.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC|Opcodes.ACC_FINAL,"script","Ljava/lang/String;",null,"(function(){try{if(window.__naSafeFlashInstalled) var payload={swf:abs,flashvars:fv,params:params,page:location.href,source:'safe-adapter'};").visitEnd();
  MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"configure","(Landroid/webkit/WebSettings;)V",null,null);
  m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitInsn(Opcodes.ICONST_M1);m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"android/webkit/WebSettings","setCacheMode","(I)V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();w.visitEnd();return w.toByteArray();
 }
 static class Loader extends ClassLoader {Class<?> load(byte[] b){return defineClass("CacheFixture",b,0,b.length);}}
 public static void main(String[] a)throws Exception {
  byte[] patched=BrowserBridgePatch.patchPortal(fixture(),"// Installed in the official game frame before SWF capture. No credentials logged.\nvar x=1;\n");
  android.webkit.WebSettings settings=new android.webkit.WebSettings();new Loader().load(patched).getMethod("configure",android.webkit.WebSettings.class).invoke(null,settings);
  if(settings.cacheMode!=2)throw new AssertionError("Cache mode not LOAD_NO_CACHE");
  if(!Arrays.equals(patched,BrowserBridgePatch.patchPortal(patched,"// Installed in the official game frame before SWF capture. No credentials logged.\nvar x=1;\n")))throw new AssertionError("Repeated patch changed bytes");
  System.out.println("PASS: verified JVM executes patched WebSettings LOAD_NO_CACHE and repeated patch is byte-idempotent.");
 }
}
