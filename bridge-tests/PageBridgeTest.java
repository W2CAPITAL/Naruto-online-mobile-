import br.davi.narutoair.portal.*;
import com.adobe.fre.*;
public final class PageBridgeTest {
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);}
 public static void main(String[] args) {
  MobilePortalContext context=new MobilePortalContext();
  FREFunction configure=context.getFunctions().get("browserConfigure"),eval=context.getFunctions().get("browserEval");
  check(eval.call(context,new FREObject[]{new FREObject("script")})==null,"disabled transport accepted");
  check(configure.call(context,new FREObject[]{new FREObject("test-nonce")}).value.equals(true),"configure");
  check(eval.call(context,new FREObject[]{new FREObject("script")}).value.equals(true),"queue refused");
  check(context.view().script==null,"UI work ran synchronously on AIR thread");
  context.handler().drain();check(context.view().script.equals("script"),"queued browser work missing");
  BrowserPageBridge.callback(context,"wrong-nonce","payload");check(context.eventCode==null,"foreign callback accepted");
  BrowserPageBridge.callback(context,"test-nonce","payload");check(context.eventCode.equals("browser-callback") && context.eventLevel.equals("payload"),"callback transport");
  context.eventCode=null;BrowserPageBridge.callback(context,"test-nonce",new String(new char[65537]));check(context.eventCode==null,"callback limit");
  context.handler().accept=false;check(eval.call(context,new FREObject[]{new FREObject("script")}).value.equals(false),"queue failure hidden");
  check(eval.call(context,new FREObject[]{new FREObject(new String(new char[65537]))})==null,"script limit");
  configure.call(context,new FREObject[]{new FREObject("")});context.eventCode=null;
  BrowserPageBridge.callback(context,"test-nonce","payload");check(context.eventCode==null,"closed callback accepted");
  StringBuilder trace=new StringBuilder();for(int i=0;i<2000;i++)trace.append("line ").append(i).append('\n');
  String tail=BrowserPageBridge.latestTrace(trace.toString());check(tail.length()<12100 && tail.endsWith("line 1999\n") && !tail.contains("line 0\n"),"latest trace lost");
  System.out.println("PASS: page bridge model: nonblocking UI queue, nonce isolation, callbacks, limits, disabled/failing transport and final log lines.");
 }
}
