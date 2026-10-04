import java.util.*;
import jdk.internal.org.objectweb.asm.*;
/** Executes a callback with the real portal gate; observes original side effects. */
public final class PopupGuardTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static byte[] fixture() {
  ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,"GuardFixture",null,"java/lang/Object",null);
  w.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"calls","I",null,null).visitEnd();
  w.visitField(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"context","Lbr/davi/narutoair/portal/PortalContext;",null,null).visitEnd();
  MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"onCreateWindow","(Landroid/webkit/WebView;ZZLandroid/os/Message;)Z",null,null);
  m.visitCode();
  m.visitFieldInsn(Opcodes.GETSTATIC,"GuardFixture","context","Lbr/davi/narutoair/portal/PortalContext;");m.visitVarInsn(Opcodes.ALOAD,0);
  m.visitMethodInsn(Opcodes.INVOKESTATIC,"br/davi/narutoair/portal/PortalContext","access$2402","(Lbr/davi/narutoair/portal/PortalContext;Landroid/webkit/WebView;)Landroid/webkit/WebView;",false);m.visitInsn(Opcodes.POP);
  m.visitFieldInsn(Opcodes.GETSTATIC,"GuardFixture","calls","I");m.visitInsn(Opcodes.ICONST_1);m.visitInsn(Opcodes.IADD);m.visitFieldInsn(Opcodes.PUTSTATIC,"GuardFixture","calls","I");m.visitInsn(Opcodes.ICONST_1);m.visitInsn(Opcodes.IRETURN);m.visitMaxs(2,4);m.visitEnd();w.visitEnd();return w.toByteArray();
 }
 public static void main(String[] args)throws Exception {
  byte[] patched=PopupGuard.patch(fixture());check(Arrays.equals(patched,PopupGuard.patch(patched)),"not idempotent");
  Class<?> f=new ClassLoader(PopupGuardTest.class.getClassLoader()){Class<?> loadBytes(){return defineClass("GuardFixture",patched,0,patched.length);}}.loadBytes();
  java.lang.reflect.Method callback=f.getMethod("onCreateWindow",android.webkit.WebView.class,boolean.class,boolean.class,android.os.Message.class);
  com.adobe.fre.FREContext context=new br.davi.narutoair.portal.MobilePortalContext();
  f.getField("context").set(null,context);
  android.webkit.WebView previous=((br.davi.narutoair.portal.PortalContext)context).view(),replacement=new android.webkit.WebView();
  Map<String,com.adobe.fre.FREFunction> functions=context.getFunctions();
  for(boolean gesture:new boolean[]{false,true}) {
   functions.get("hide").call(context,new com.adobe.fre.FREObject[0]);
   check(callback.invoke(null,null,false,gesture,null).equals(false),"hidden popup accepted");
   check(f.getField("calls").getInt(null)==0,"hidden callback ran original popup creation");
  }
  functions.get("show").call(context,new com.adobe.fre.FREObject[0]);
  check(callback.invoke(null,replacement,false,true,null).equals(true),"visible popup rejected");
  check(f.getField("calls").getInt(null)==1,"visible popup not delegated");
  check(previous.visibility==8 && replacement.visibility==0,"previous portal covers the AIR surface");
  check(((br.davi.narutoair.portal.PortalContext)context).view()==replacement,"active popup changed");
  System.out.println("PASS popup callback: hidden popup blocked; explicit show delegates; previous view hidden before switch; replacement retained; idempotent. JVM model, not Android.");
 }
}
