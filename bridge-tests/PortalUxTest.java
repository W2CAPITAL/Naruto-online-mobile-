import br.davi.narutoair.portal.*;
import com.adobe.fre.*;
import android.view.*;
import java.util.*;
import jdk.internal.org.objectweb.asm.*;
public final class PortalUxTest {
 static void check(boolean x,String why){if(!x)throw new AssertionError(why);}
 static byte[] fixture(){
  ClassWriter w=new ClassWriter(ClassWriter.COMPUTE_MAXS);w.visit(Opcodes.V1_8,Opcodes.ACC_PUBLIC,"PortalUxFixture",null,"br/davi/narutoair/portal/PortalContext",null);
  MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitMethodInsn(Opcodes.INVOKESPECIAL,"br/davi/narutoair/portal/PortalContext","<init>","()V",false);m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
  w.visitField(Opcodes.ACC_PUBLIC,"opened","Ljava/lang/String;",null,null).visitEnd();
  m=w.visitMethod(Opcodes.ACC_PUBLIC,"createAndOpen","(Ljava/lang/String;)V",null,null);m.visitCode();m.visitVarInsn(Opcodes.ALOAD,0);m.visitVarInsn(Opcodes.ALOAD,1);m.visitFieldInsn(Opcodes.PUTFIELD,"PortalUxFixture","opened","Ljava/lang/String;");m.visitInsn(Opcodes.RETURN);m.visitMaxs(0,0);m.visitEnd();
  for(String n:new String[]{"handleNavigation","popupNavigation"}){m=w.visitMethod(Opcodes.ACC_PUBLIC,n,"(Landroid/webkit/WebView;Ljava/lang/String;)Z",null,null);m.visitCode();m.visitInsn(Opcodes.ICONST_0);m.visitInsn(Opcodes.IRETURN);m.visitMaxs(0,0);m.visitEnd();}
  w.visitEnd();return w.toByteArray();
 }
 static class Loader extends ClassLoader{Class<?> load(byte[] b){return defineClass("PortalUxFixture",b,0,b.length);}}
 static void touch(View v,int type,float x,float y){check(v.touch.onTouch(v,MotionEvent.obtain(0,0,type,x,y,0)),"overlay did not consume touch");}
 public static void main(String[] a)throws Exception{
  byte[] b=PortalUxPatch.patch(fixture());check(Arrays.equals(b,PortalUxPatch.patch(b)),"patch is not idempotent");
  Class<?> clazz=new Loader().load(b);PortalContext c=(PortalContext)clazz.getConstructor().newInstance();
  String list=PortalSession.SERVER_LIST;
  clazz.getMethod("createAndOpen",String.class).invoke(c,"https://gamebox3.narutowebgame.com/gamebox/2.4.1/template/login.php?x=y");check(list.equals(clazz.getField("opened").get(c)),"launcher still used");
  clazz.getMethod("createAndOpen",String.class).invoke(c,list);check(list.equals(clazz.getField("opened").get(c)),"public list changed");
  MobilePortalContext mode=new MobilePortalContext();mode.getFunctions().get("hide").call(mode,new FREObject[0]);
  for(String n:new String[]{"handleNavigation","popupNavigation"}){
   check((Boolean)clazz.getMethod(n,android.webkit.WebView.class,String.class).invoke(c,c.view(),list),"server selector did not end old client");check("select-server".equals(c.eventCode),"server event missing");
   check(!(Boolean)clazz.getMethod(n,android.webkit.WebView.class,String.class).invoke(c,c.view(),list+"s877"),"server launch route incorrectly intercepted");
   check(!PortalSession.navigation(c,"https://naruto.narutowebgame.com.evil.test/pt/serverlist/"),"foreign selector allowed");
  }
  mode.getFunctions().get("show").call(mode,new FREObject[0]);check(!PortalSession.navigation(c,list),"visible selection intercepted");
  android.webkit.CookieManager cookies=android.webkit.CookieManager.getInstance();cookies.setCookie(list,"oas_user=fixture; Path=/");
  check((Boolean)PortalSession.resetAccount().call(c,new FREObject[0]).value,"account reset not queued");
  check(!"about:blank".equals(c.view().url),"account reset not on UI queue");c.handler().drain();
  check(c.view().stopped && "about:blank".equals(c.view().url),"old page still active");check(android.webkit.WebStorage.getInstance().cleared,"portal storage retained");
  check(cookies.getCookie(list).isEmpty() && "account-cleared".equals(c.eventCode),"account clearing callback failed");
  View parent=new View(),button=new View();button.parent=parent;button.width=150;button.height=92;button.x=1200;button.y=550;c.button(button);
  NativeFloatingButton.install(c);check(button.touch!=null,"LOG drag listener not attached");
  touch(button,0,1250,580);touch(button,2,1450,700);touch(button,1,1450,700);check(button.clicks==0,"drag fired LOG click");check(button.x==1400 && button.y==628,"drag bounds wrong");
  touch(button,0,1450,670);touch(button,1,1450,670);check(button.clicks==1,"tap did not preserve LOG action");
  touch(button,0,1450,670);touch(button,3,1450,670);check(button.clicks==1,"cancel fired LOG click");
  parent.width=800;parent.height=400;button.layout.onLayoutChange(button,0,0,150,92,0,0,150,92);check(button.x>=0 && button.x<=650 && button.y<=308,"resize lost LOG button");
  System.out.println("PASS: patched portal startup/navigation executes under JVM verifier; official session reset completes on UI callback; native LOG drag suppresses click, preserves taps and stays visible after resize. JVM model, not Android device.");
 }
}
