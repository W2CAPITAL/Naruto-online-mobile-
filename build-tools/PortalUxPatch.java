import java.nio.file.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
/** Hooks our own portal startup, navigation and existing LOG overlay. */
public final class PortalUxPatch {
 public static byte[] patch(byte[] bytes) {
  final String session="br/davi/narutoair/portal/PortalSession",drag="br/davi/narutoair/portal/NativeFloatingButton";
  final Set<String> done=new HashSet<>();
  new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM7){public MethodVisitor visitMethod(int a,final String n,String d,String s,String[] ex){
   return new MethodVisitor(Opcodes.ASM7){public void visitMethodInsn(int op,String o,String name,String desc,boolean i){if(o.equals(session)||o.equals(drag))done.add(n);}};
  }},0);
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM7,w){public MethodVisitor visitMethod(int a,final String n,final String d,String s,String[] ex){
   MethodVisitor target=super.visitMethod(a,n,d,s,ex);if(done.contains(n))return target;
   return new MethodVisitor(Opcodes.ASM7,target){
    public void visitCode(){super.visitCode();
     if(n.equals("createAndOpen")&&d.equals("(Ljava/lang/String;)V")){super.visitVarInsn(Opcodes.ALOAD,1);super.visitMethodInsn(Opcodes.INVOKESTATIC,session,"openingUrl","(Ljava/lang/String;)Ljava/lang/String;",false);super.visitVarInsn(Opcodes.ASTORE,1);}
     if((n.equals("handleNavigation")||n.equals("popupNavigation"))&&d.equals("(Landroid/webkit/WebView;Ljava/lang/String;)Z")){
      super.visitVarInsn(Opcodes.ALOAD,0);super.visitVarInsn(Opcodes.ALOAD,2);super.visitMethodInsn(Opcodes.INVOKESTATIC,session,"navigation","(Lcom/adobe/fre/FREContext;Ljava/lang/String;)Z",false);
      Label normal=new Label();super.visitJumpInsn(Opcodes.IFEQ,normal);super.visitInsn(Opcodes.ICONST_1);super.visitInsn(Opcodes.IRETURN);super.visitLabel(normal);super.visitFrame(Opcodes.F_SAME,0,null,0,null);
     }
    }
    public void visitInsn(int op){
     if(n.equals("attachNativeInspector")&&d.equals("(Landroid/app/Activity;)V")&&op==Opcodes.RETURN){super.visitVarInsn(Opcodes.ALOAD,0);super.visitMethodInsn(Opcodes.INVOKESTATIC,drag,"install","(Lcom/adobe/fre/FREContext;)V",false);}
     super.visitInsn(op);
    }
   };
  }},0);return w.toByteArray();
 }
 public static void main(String[] a)throws Exception{byte[] bytes=Files.readAllBytes(Paths.get(a[0]));byte[] result=patch(bytes);Files.write(Paths.get(a[1]),result);if(!Arrays.equals(result,patch(result)))throw new AssertionError("UX patch not idempotent");System.out.println("Portal startup, server selection and movable LOG hooks installed; idempotent.");}
}
