import java.nio.file.*;
import jdk.internal.org.objectweb.asm.*;
/** Build-only, optional postprocessing hook on the tested AIR 51.1.4.1 EGL14 path. */
public final class FsrRuntimePatch {
 public static byte[] patch(byte[] source) {
  final String hook="br/davi/narutoair/fsr/FsrRenderer";
  final int[] create={0},swap={0},old={0};
  new ClassReader(source).accept(new ClassVisitor(Opcodes.ASM7){
   public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){return new MethodVisitor(Opcodes.ASM7){
    public void visitMethodInsn(int op,String o,String n,String d,boolean i){if(o.equals(hook))old[0]++;}
   };}
  },0);
  if(old[0]!=0)throw new IllegalArgumentException("Start from the unmodified SDK runtimeClasses.jar");
  ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  new ClassReader(source).accept(new ClassVisitor(Opcodes.ASM7,writer){
   public MethodVisitor visitMethod(int a,final String name,String desc,String sig,String[] ex){
    MethodVisitor target=super.visitMethod(a,name,desc,sig,ex);
    return new MethodVisitor(Opcodes.ASM7,target){
     public void visitCode(){super.visitCode();if(name.equals("CreateWindowSurface") && desc.equals("(Landroid/view/SurfaceView;I)I")){
      create[0]++;super.visitVarInsn(Opcodes.ALOAD,0);super.visitVarInsn(Opcodes.ALOAD,1);
      super.visitMethodInsn(Opcodes.INVOKESTATIC,hook,"register","(Ljava/lang/Object;Landroid/view/SurfaceView;)V",false);
     }}
     public void visitMethodInsn(int op,String o,String n,String d,boolean itf){
      if(name.equals("SwapEGLBuffers") && op==Opcodes.INVOKESTATIC && o.equals("android/opengl/EGL14") && n.equals("eglSwapBuffers")){
       // The original display/surface stay on the operand stack; hook restores the current EGL and GL state.
       swap[0]++;super.visitVarInsn(Opcodes.ALOAD,0);super.visitMethodInsn(Opcodes.INVOKESTATIC,hook,"beforeSwap","(Ljava/lang/Object;)V",false);
      }
      super.visitMethodInsn(op,o,n,d,itf);
     }
    };
   }
  },0);
  if(create[0]!=1 || swap[0]!=1)throw new IllegalArgumentException("Unexpected AIR EGL14 implementation: "+create[0]+"/"+swap[0]);
  return writer.toByteArray();
 }
 public static void main(String[] args)throws Exception{Files.write(Paths.get(args[1]),patch(Files.readAllBytes(Paths.get(args[0]))));System.out.println("Optional FSR EGL14 pre-present hook installed");}
}
