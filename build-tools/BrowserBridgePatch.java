import java.nio.file.*;
import jdk.internal.org.objectweb.asm.*;
/** Patches only our portal's JS adapter and adds a callback to its own JS interface. */
public final class BrowserBridgePatch {
 private static final String PREFIX="(function(){try{if(window.__naSafeFlashInstalled)";
 private static final String PAYLOAD="var payload={swf:abs,flashvars:fv,params:params,page:location.href,source:'safe-adapter'};";
 static String script(String value,String bridge) {
  if(!value.contains(PREFIX) || !value.contains(PAYLOAD))return value;
  if(value.contains("__naBrowserCapture")) {
   int start=value.indexOf("// Installed in the official game frame before SWF capture. No credentials logged.");
   int end=start<0?-1:value.indexOf(PREFIX,start);
   if(start<0 || end<0)throw new IllegalArgumentException("Unknown existing page bridge");
   return value.substring(0,start)+bridge+value.substring(end);
  }
  return value.replace(PREFIX,bridge+PREFIX).replace(PAYLOAD,PAYLOAD+"payload.browser=window.__naBrowserCapture(owner);");
 }
 public static byte[] patchPortal(byte[] bytes,final String bridge) {
  ClassWriter w=new ClassWriter(0);final int[] count={0};
  new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM7,w) {
   public FieldVisitor visitField(int a,String n,String d,String s,Object v) {
    if(v instanceof String) {String p=script((String)v,bridge);if(!p.equals(v))count[0]++;v=p;}
    return super.visitField(a,n,d,s,v);
   }
   public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex) {
    return new MethodVisitor(Opcodes.ASM7,super.visitMethod(a,n,d,s,ex)) {
     private boolean hasTraceGuard=false;
     private int previousOpcode=-1;
     public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf) {
      if(owner.equals("br/davi/narutoair/portal/BrowserPageBridge") && name.equals("latestTrace"))hasTraceGuard=true;
      if(owner.equals("android/webkit/WebSettings") && name.equals("setCacheMode") && desc.equals("(I)V") && previousOpcode!=Opcodes.ICONST_2) {super.visitInsn(Opcodes.POP);super.visitInsn(Opcodes.ICONST_2);}
      super.visitMethodInsn(op,owner,name,desc,itf);previousOpcode=-1;
     }
     // Clipboard pastes have repeatedly lost the final client error. Keep the latest complete lines.
     public void visitInsn(int opcode) {
      if(!hasTraceGuard && n.equals("traceText") && d.equals("()Ljava/lang/String;") && opcode==Opcodes.ARETURN)
       super.visitMethodInsn(Opcodes.INVOKESTATIC,"br/davi/narutoair/portal/BrowserPageBridge","latestTrace","(Ljava/lang/String;)Ljava/lang/String;",false);
      super.visitInsn(opcode);previousOpcode=opcode;
     }
     public void visitLdcInsn(Object v) {
      if(v instanceof String){String p=script((String)v,bridge);if(!p.equals(v))count[0]++;v=p;}
      super.visitLdcInsn(v);
     }
    };
   }
  },0);
  byte[] result=w.toByteArray();
  if(count[0]==0 && !new String(bytes,java.nio.charset.StandardCharsets.ISO_8859_1).contains("__naBrowserCapture"))
   throw new IllegalArgumentException("Official SWF capture adapter not found");
  return result;
 }
 public static byte[] patchInterface(byte[] bytes) {
  final boolean[] exists={false};ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);
  new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM7,w) {
   public MethodVisitor visitMethod(int a,String n,String d,String s,String[] ex) {
    if(n.equals("browserCallback") && d.equals("(Ljava/lang/String;Ljava/lang/String;)V"))exists[0]=true;
    return super.visitMethod(a,n,d,s,ex);
   }
   public void visitEnd() {
    if(!exists[0]) {
     MethodVisitor m=super.visitMethod(Opcodes.ACC_PUBLIC,"browserCallback","(Ljava/lang/String;Ljava/lang/String;)V",null,null);
     m.visitAnnotation("Landroid/webkit/JavascriptInterface;",true).visitEnd();
     m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);
     m.visitFieldInsn(Opcodes.GETFIELD,"br/davi/narutoair/portal/PortalContext$PortalJsBridge","this$0","Lbr/davi/narutoair/portal/PortalContext;");
     m.visitVarInsn(Opcodes.ALOAD,1);m.visitVarInsn(Opcodes.ALOAD,2);
     m.visitMethodInsn(Opcodes.INVOKESTATIC,"br/davi/narutoair/portal/BrowserPageBridge","callback","(Lcom/adobe/fre/FREContext;Ljava/lang/String;Ljava/lang/String;)V",false);
     m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
    }
    super.visitEnd();
   }
  },0);return w.toByteArray();
 }
 public static void main(String[] a)throws Exception {
  byte[] bytes=Files.readAllBytes(Paths.get(a[1]));
  byte[] result=a[0].equals("portal")?patchPortal(bytes,new String(Files.readAllBytes(Paths.get(a[3])),java.nio.charset.StandardCharsets.UTF_8)):patchInterface(bytes);
  Files.write(Paths.get(a[2]),result);System.out.println("Browser page bridge installed: "+a[0]);
 }
}
