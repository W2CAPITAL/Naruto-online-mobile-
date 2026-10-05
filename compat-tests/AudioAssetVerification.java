import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.abc.ABC;
import com.jpexs.decompiler.flash.abc.types.InstanceInfo;
import com.jpexs.decompiler.flash.abc.types.Multiname;
import com.jpexs.decompiler.flash.tags.Tag;
import com.jpexs.decompiler.flash.tags.DefineSoundTag;
import com.jpexs.decompiler.flash.tags.SymbolClassTag;
import br.davi.narutoair.portal.EntryCompatibility;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Independent parser verifies embedded mod audio survives ABC retargeting. */
public final class AudioAssetVerification {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 static String type(ABC abc,int name){Multiname m=abc.constants.getMultiname(name);return abc.constants.getString(abc.constants.getNamespace(m.namespace_index).name_index)+"::"+abc.constants.getString(m.name_index);}
 public static void main(String[] args)throws Exception {
  int files=0;
  try(java.util.stream.Stream<Path> paths=Files.list(Paths.get(args[0]))){
   for(Path p:(Iterable<Path>)paths.filter(v->v.toString().endsWith(".swf"))::iterator){
    byte[] original=Files.readAllBytes(p);EntryCompatibility.Result result=EntryCompatibility.adapt(original);
    check(result.soundReferences==0,"embedded sound must not be retargeted: "+p.getFileName());
    check(Arrays.equals(original,result.bytes),"embedded sound archive changed");
    check(result.references==0,"unrelated references changed in sound archive");
    check(EntryCompatibility.adapt(result.bytes).references==0,"sound retarget not idempotent");
    SWF before=new SWF(new ByteArrayInputStream(original),false),after=new SWF(new ByteArrayInputStream(result.bytes),false);
    check(before.getTags().size()==after.getTags().size(),"sound tag count");
    int sounds=0,symbols=0;
    for(int i=0;i<before.getTags().size();i++){
     Tag a=before.getTags().get(i),b=after.getTags().get(i);
     check(a.getId()==b.getId(),"tag order");
     if(a.getId()!=82)check(Arrays.equals(a.getOriginalData(),b.getOriginalData()),"non-ABC tag changed");
     if(a instanceof DefineSoundTag){DefineSoundTag sound=(DefineSoundTag)a;check(sound.soundFormat==2 && sound.soundSampleCount>0,"MP3/sample count");sounds++;}
     if(a instanceof SymbolClassTag){SymbolClassTag symbol=(SymbolClassTag)a;check(symbol.names.equals(Collections.singletonList("s1")) && symbol.tags.equals(Collections.singletonList(1)),"embedded sound linkage");symbols++;}
    }
    check(sounds==1 && symbols==1,"embedded sound tags absent");
    ABC a=before.getAbcList().get(0).getABC(),b=after.getAbcList().get(0).getABC();
    check(a.instance_info.size()==1 && b.instance_info.size()==1,"sound class count");
    InstanceInfo x=a.instance_info.get(0),y=b.instance_info.get(0);
    check(type(a,x.super_index).equals("flash.media::Sound"),"original sound superclass");
    check(type(b,y.super_index).equals("flash.media::Sound"),"native Sound wrapper superclass");
    check(a.bodies.size()==b.bodies.size(),"method count");
    for(int i=0;i<a.bodies.size();i++)check(Arrays.equals(a.bodies.get(i).getCodeBytes(),b.bodies.get(i).getCodeBytes()),"embedded sound constructor opcodes changed");
    check(Arrays.equals(original,Files.readAllBytes(p)),"packaged mod changed");files++;
   }
  }
  check(files==15,"expected all 15 Kaguya sounds");
  System.out.println("PASS: independent parser on 15 real Kaguya sound SWFs; MP3 bytes, sample counts, SymbolClass s1 linkage, constructor opcodes unchanged; native Sound superclass and entire SWF byte preservation. Does not prove AIR native playback.");
 }
}
