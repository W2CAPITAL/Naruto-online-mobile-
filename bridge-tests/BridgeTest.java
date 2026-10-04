import com.adobe.fre.*;
import br.davi.narutoair.portal.*;
import java.nio.file.*;
import java.util.*;
public final class BridgeTest {
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        FREContext context=new PortalExtension().createContext(null);
        Map<String,FREFunction> functions=context.getFunctions();
        check(functions.containsKey("adaptSwfBytes"),"adapter not registered");
        check(functions.get("ping").call(context,new FREObject[0]).value.equals("original-ping"),"portal function changed");
        for(String name:new String[]{"hide","show","close","open"}) {
            check(functions.get(name).call(context,new FREObject[0]).value.equals("original-"+name),"original visibility function not delegated");
            check(MobilePortalContext.isPortalHidden()==(name.equals("hide") || name.equals("close")),"popup gate state: "+name);
        }
        FREFunction adapt=functions.get("adaptSwfBytes");
        byte[] input=Files.readAllBytes(Paths.get(args[0]));
        FREByteArray source=new FREByteArray(input);
        FREObject result=adapt.call(context,new FREObject[]{source});
        check(result.getProperty("error")==null,"adapter failed");
        check(result.getProperty("security").value.equals(1) && result.getProperty("loaders").value.equals(1),"metadata mismatch");
        check(result.getProperty("resources").value.equals(2),"resource metadata mismatch");
        check(result.getProperty("sounds").value.equals(1),"audio metadata mismatch");
        check(result.getProperty("page").value.equals(1),"page bridge metadata mismatch");
        check(Arrays.equals(input,source.snapshot()),"input mutated");
        FREByteArray output=(FREByteArray)result.getProperty("bytes");
        check(Arrays.equals(output.snapshot(),Files.readAllBytes(Paths.get(args[1]))),"native bridge output differs from parser");
        FREObject again=adapt.call(context,new FREObject[]{output});
        check(again.getProperty("bytes")==output,"unchanged SWF copied unnecessarily");
        check(again.getProperty("security").value.equals(0) && again.getProperty("loaders").value.equals(0),"second adaptation");
        check(again.getProperty("sounds").value.equals(0),"second sound adaptation");
        check(again.getProperty("resources").value.equals(0),"second resource adaptation");
        FREObject invalid=adapt.call(context,new FREObject[]{new FREByteArray(new byte[12])});
        check(invalid.getProperty("error")!=null && FREObject.acquired==0,"malformed SWF lock leaked");
        FREByteArray huge=new FREByteArray(new byte[12]) {public long getLength(){return 33L*1024*1024;}};
        check(adapt.call(context,new FREObject[]{huge}).getProperty("error")!=null && FREObject.acquired==0,"size guard or lifetime failed");
        check(adapt.call(context,new FREObject[]{FREObject.newObject("bad")}).getProperty("error")!=null,"wrong type accepted");
        check(FREObject.acquired==0,"native array still acquired");
        System.out.println("PASS: native bridge model; registration, original portal functions, input preservation, output, idempotence, limits and acquire/release on success/error.");
    }
}
