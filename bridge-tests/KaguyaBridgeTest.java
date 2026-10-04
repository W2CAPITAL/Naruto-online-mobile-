import br.davi.narutoair.portal.*;
import com.adobe.fre.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
public final class KaguyaBridgeTest {
 public static class Assets {public InputStream open(String name)throws IOException{return Files.newInputStream(Paths.get(project,name));}}
 public static class Activity {public Assets getAssets(){return new Assets();}public File getCacheDir(){return cache;}}
 public static class Context extends PortalContext {public Activity getActivity(){return new Activity();}}
 static String project;static File cache;
 static void check(boolean v,String reason){if(!v)throw new AssertionError(reason);}
 static FREObject[] args(String enabled,String style){return new FREObject[]{new FREObject(enabled),new FREObject(style)};}
 public static void main(String[] a)throws Exception {
  project=a[0];cache=Files.createTempDirectory("native-kaguya").toFile();Context context=new Context();FREFunction fn=KaguyaBridge.function();
  check(new MobilePortalContext().getFunctions().containsKey("kaguyaMode"),"native registration");
  URL portrait=new URL("https://cdnnaruto-pt.oasgames.com/PT_NarutoAlpha9.35Build301/assets/battle/role/head_176_68/10000201/1.png");
  check(fn.call(context,args("true","0")).value.equals(true),"configure");check(KaguyaResources.read(portrait)!=null,"APK asset provider");
  check(fn.call(context,args("false","0")).value.equals(true),"disable");check(KaguyaResources.read(portrait)==null,"disable not applied");
  for(String[] pair:new String[][]{{"true","5"},{"true","-1"},{"TRUE","0"},{"true","bad"}})check(fn.call(context,args(pair[0],pair[1])).value.equals(false),"invalid native arguments");
  check(fn.call(new PortalContext(),args("true","0")).value.equals(false),"unavailable Activity reported as success");
  cache.delete();System.out.println("PASS: native Kaguya registration, APK AssetManager reflection, ON/OFF, invalid arguments and missing Activity.");
 }
}
