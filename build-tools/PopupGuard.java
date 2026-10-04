import java.nio.file.*;
import jdk.internal.org.objectweb.asm.*;
/** Build-only transformation of our portal callback; no AIR runtime changes. */
public final class PopupGuard {
 public static byte[] patch(byte[] bytes) {
  final String owner="br/davi/narutoair/portal/MobilePortalContext";
  final String descriptor="(Landroid/webkit/WebView;ZZLandroid/os/Message;)Z";
  boolean[] guarded={false},switched={false},surfaceGuarded={false};int[] found={0};
  new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM7) {
   public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
    if(!name.equals("onCreateWindow") || !desc.equals(descriptor))return null;
    found[0]++;
    return new MethodVisitor(Opcodes.ASM7) {
     public void visitMethodInsn(int op,String o,String n,String d,boolean itf) {
      if(op==Opcodes.INVOKESTATIC && o.equals(owner) && n.equals("isPortalHidden") && d.equals("()Z"))guarded[0]=true;
      if(op==Opcodes.INVOKESTATIC && n.equals("access$2402") && o.equals("br/davi/narutoair/portal/PortalContext"))switched[0]=true;
      if(op==Opcodes.INVOKESTATIC && n.equals("beforeSwitch") && o.equals("br/davi/narutoair/portal/BrowserPageBridge"))surfaceGuarded[0]=true;
     }
    };
   }
  },0);
  if(found[0]!=1)throw new IllegalArgumentException("Expected one portal popup callback");
  if(guarded[0] && (!switched[0] || surfaceGuarded[0]))return bytes;
  ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM7,writer) {
   public MethodVisitor visitMethod(int access,String name,String desc,String sig,String[] ex) {
    MethodVisitor target=super.visitMethod(access,name,desc,sig,ex);
    if(!name.equals("onCreateWindow") || !desc.equals(descriptor))return target;
    return new MethodVisitor(Opcodes.ASM7,target) {
     public void visitCode() {
      super.visitCode();
      if(guarded[0])return;
      visitMethodInsn(Opcodes.INVOKESTATIC,owner,"isPortalHidden","()Z",false);
      Label visible=new Label();visitJumpInsn(Opcodes.IFEQ,visible);
      visitInsn(Opcodes.ICONST_0);visitInsn(Opcodes.IRETURN);
      visitLabel(visible);visitFrame(Opcodes.F_SAME,0,null,0,null);
     }
     public void visitMethodInsn(int op,String o,String n,String d,boolean itf) {
      if(!surfaceGuarded[0] && op==Opcodes.INVOKESTATIC && o.equals("br/davi/narutoair/portal/PortalContext") &&
         n.equals("access$2402") && d.equals("(Lbr/davi/narutoair/portal/PortalContext;Landroid/webkit/WebView;)Landroid/webkit/WebView;")) {
       super.visitInsn(Opcodes.DUP2);
       super.visitMethodInsn(Opcodes.INVOKESTATIC,"br/davi/narutoair/portal/BrowserPageBridge","beforeSwitch","(Lcom/adobe/fre/FREContext;Ljava/lang/Object;)V",false);
      }
      super.visitMethodInsn(op,o,n,d,itf);
     }
    };
   }
  },0);
  return writer.toByteArray();
 }
 public static void main(String[] args)throws Exception {
  byte[] bytes=Files.readAllBytes(Paths.get(args[0]));
  byte[] result=patch(bytes);Files.write(Paths.get(args[1]),result);
  System.out.println("Portal popup visibility guard installed; idempotent="+java.util.Arrays.equals(result,patch(result)));
 }
}
