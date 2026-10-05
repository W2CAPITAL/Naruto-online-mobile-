import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.abc.ABC;
import com.jpexs.decompiler.flash.abc.types.*;
import br.davi.narutoair.portal.EntryCompatibility;
import java.nio.file.*;
import java.io.*;
import java.util.*;
public final class AudioConstructorVerification {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  byte[] input=Files.readAllBytes(Paths.get(args[0]));
  EntryCompatibility.Result r=EntryCompatibility.adapt(input);
  check(r.soundReferences==6,"expected six external Sound constructors, got "+r.soundReferences);
  check(r.references==6,"unexpected global class retarget");
  check(EntryCompatibility.adapt(r.bytes).references==0,"audio constructor idempotence");
  Files.write(Paths.get(args[1]),r.bytes);
  SWF original=new SWF(new ByteArrayInputStream(input),false),adapted=new SWF(new ByteArrayInputStream(r.bytes),false);
  ABC a=original.getAbcList().get(0).getABC(),b=adapted.getAbcList().get(0).getABC();
  check(a.constants.getMultinameCount()>128,"fixture must widen u30 operands");
  for(int i=1;i<a.constants.getMultinameCount();i++){
   Multiname m=a.constants.getMultiname(i);if(m.namespace_index==0)continue;
   if(IndependentVerification.fullName(a,i).equals("flash.media::Sound"))check(IndependentVerification.fullName(b,i).equals("flash.media::Sound"),"native audio type changed");
  }
  check(a.instance_info.get(0).super_index==b.instance_info.get(0).super_index,"superclass index changed");
  int grown=0;for(int i=0;i<a.bodies.size();i++)if(a.bodies.get(i).getCodeBytes().length<b.bodies.get(i).getCodeBytes().length)grown++;
  check(grown>0,"fixture failed to exercise code relocation");
  IndependentVerification.main(new String[]{args[0],args[1],args[2]});
  System.out.println("PASS: six new Sound calls; native Sound cast/is/coerce/class references; widened u30 operands, conditional/loop/switch/exception relocation and idempotence independently decoded.");
 }
}
