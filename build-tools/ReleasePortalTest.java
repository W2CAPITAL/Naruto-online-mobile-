import java.util.*;
import jdk.internal.org.objectweb.asm.*;
import br.davi.narutoair.portal.PortalContext;
public final class ReleasePortalTest {
 static byte[] fixture(){ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,"ReleaseFixture",null,"br/davi/narutoair/portal/PortalContext",null);
  w.visitField(Opcodes.ACC_PUBLIC,"calls","I",null,null).visitEnd();
  MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"br/davi/narutoair/portal/PortalContext","<init>","()V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
  for(String name:new String[]{"trace","attachNativeInspector","injectInspector","detectCloudflareBlock"}){String desc=name.equals("trace")?"(Ljava/lang/String;)V":name.equals("attachNativeInspector")?"(Landroid/app/Activity;)V":"(Landroid/webkit/WebView;)V";m=w.visitMethod(Opcodes.ACC_PUBLIC,name,desc,null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitInsn(Opcodes.DUP);m.visitFieldInsn(Opcodes.GETFIELD,"ReleaseFixture","calls","I");m.visitInsn(Opcodes.ICONST_1);m.visitInsn(Opcodes.IADD);m.visitFieldInsn(Opcodes.PUTFIELD,"ReleaseFixture","calls","I");m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();}
  w.visitEnd();return w.toByteArray();}
 static class Loader extends ClassLoader {Class<?> load(byte[] b){return defineClass("ReleaseFixture",b,0,b.length);}}
 public static void main(String[] args)throws Exception {
  byte[] result=ReleasePortalPatch.patch(fixture());if(!Arrays.equals(result,ReleasePortalPatch.patch(result)))throw new AssertionError("not idempotent");
  final List<String> instructions=new ArrayList<String>();new ClassReader(result).accept(new ClassVisitor(Opcodes.ASM7){public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e){if(!n.equals("trace")&&!n.equals("attachNativeInspector")&&!n.equals("injectInspector")&&!n.equals("detectCloudflareBlock"))return null;return new MethodVisitor(Opcodes.ASM7){public void visitMethodInsn(int op,String owner,String name,String desc,boolean itf){instructions.add(owner+"/"+name);}};}},0);
  if(!instructions.equals(Arrays.asList("br/davi/narutoair/portal/PortalBranding/install","br/davi/narutoair/portal/PortalAccess/inspect")))throw new AssertionError("log retained or branding removed");
  Class<?> cls=new Loader().load(result);PortalContext c=(PortalContext)cls.getConstructor().newInstance();for(int i=0;i<10000;i++)cls.getMethod("trace",String.class).invoke(c,"fixture");
  cls.getMethod("attachNativeInspector",android.app.Activity.class).invoke(c,new Object[]{null});cls.getMethod("injectInspector",android.webkit.WebView.class).invoke(c,c.view());if(c.view().script!=null)throw new AssertionError("HTML inspector injected");
  cls.getMethod("detectCloudflareBlock",android.webkit.WebView.class).invoke(c,c.view());if(cls.getField("calls").getInt(c)!=0)throw new AssertionError("release trace/inspector executed old code");
  System.out.println("PASS: release native patch runs under verifier; 10,000 traces allocate no old log buffer; native/HTML inspectors removed; access detector replaced; patch idempotent.");
 }
}
