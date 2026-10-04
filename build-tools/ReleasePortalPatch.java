import java.nio.file.*;
import jdk.internal.org.objectweb.asm.*;
/** Release portal: preserve branding, remove native/HTML inspectors and handle access state. */
public final class ReleasePortalPatch {
 public static byte[] patch(byte[] input){
  final int[] replaced={0};
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM7,w){
   public MethodVisitor visitMethod(int a,String n,String d,String sig,String[] ex){
    if((n.equals("attachNativeInspector")&&d.equals("(Landroid/app/Activity;)V")) || (n.equals("trace")&&d.equals("(Ljava/lang/String;)V")) || (n.equals("injectInspector")&&d.equals("(Landroid/webkit/WebView;)V")) || (n.equals("detectCloudflareBlock")&&d.equals("(Landroid/webkit/WebView;)V"))){
     replaced[0]++;MethodVisitor m=super.visitMethod(a,n,d,sig,ex);m.visitCode();
     if(n.equals("attachNativeInspector")){m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESTATIC,"br/davi/narutoair/portal/PortalBranding","install","(Lcom/adobe/fre/FREContext;)V",false);}
     if(n.equals("detectCloudflareBlock")){m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ALOAD,1);m.visitMethodInsn(Opcodes.INVOKESTATIC,"br/davi/narutoair/portal/PortalAccess","inspect","(Lcom/adobe/fre/FREContext;Ljava/lang/Object;)V",false);}
     m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();return null;
    }
    return super.visitMethod(a,n,d,sig,ex);
   }
  },0);if(replaced[0]!=4)throw new IllegalArgumentException("Expected four release portal methods, found "+replaced[0]);return w.toByteArray();
 }
 public static void main(String[] a)throws Exception {byte[] b=patch(Files.readAllBytes(Paths.get(a[0])));Files.write(Paths.get(a[1]),b);if(!java.util.Arrays.equals(b,patch(b)))throw new AssertionError("Release patch is not idempotent");System.out.println("Release branding, silent trace, HTML inspector removal and portal access hook installed.");}
}
