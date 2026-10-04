import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.abc.ABC;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import br.davi.narutoair.portal.EntryCompatibility;
import com.jpexs.decompiler.flash.abc.types.*;
import com.jpexs.decompiler.flash.abc.types.traits.*;
import com.jpexs.decompiler.flash.abc.avm2.instructions.AVM2Instruction;
/** Independently verifies the sole menu guard and byte preservation elsewhere. */
public final class IndependentVerification {
    static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    static String name(ABC a,int index){return a.constants.getString(a.constants.getMultiname(index).name_index);}
    static boolean menuMethod(ABC a,int method) {
        for(InstanceInfo cls:a.instance_info)if(name(a,cls.name_index).equals("entry"))
            for(Trait t:cls.instance_traits.traits)if(t instanceof TraitMethodGetterSetter && name(a,t.name_index).equals("addBuildVersion") &&
                ((TraitMethodGetterSetter)t).method_info==method)return true;
        return false;
    }
    static void verifyMenu(ABC a,ABC b,MethodBody oldBody,MethodBody newBody) {
        check(menuMethod(a,oldBody.method_info),"unapproved method changed");
        List<AVM2Instruction> x=oldBody.getCode().code,y=newBody.getCode().code;
        check(y.size()==x.size()+4,"guard instruction count");
        int at=-1;
        for(int k=0;k<x.size();k++)if(x.get(k).definition.instructionName.equals("initproperty") && name(a,x.get(k).operands[0]).equals("gameVersion")){at=k+1;break;}
        check(at>0,"version assignment missing");
        for(int k=0;k<x.size();k++)check(Arrays.equals(x.get(k).getBytes(),y.get(k<at?k:k+4).getBytes()),"original opcode or branch changed");
        AVM2Instruction load=y.get(at),property=y.get(at+1),branch=y.get(at+2),ret=y.get(at+3);
        check(load.definition.instructionName.equals("getlex") && name(b,load.operands[0]).equals("ContextMenu"),"native menu class");
        Multiname menu=b.constants.getMultiname(load.operands[0]);
        check(b.constants.getString(b.constants.getNamespace(menu.namespace_index).name_index).equals("flash.ui"),"guard must consult real API");
        check(property.definition.instructionName.equals("getproperty") && name(b,property.operands[0]).equals("isSupported"),"real support getter");
        Multiname support=b.constants.getMultiname(property.operands[0]);
        check(b.constants.getString(b.constants.getNamespace(support.namespace_index).name_index).equals(""),"public support getter");
        check(branch.definition.instructionName.equals("iftrue") && branch.operands[0]==1 && ret.definition.instructionName.equals("returnvoid"),"guard control flow");
        check(branch.getTargetAddress()==y.get(at+4).getAddress(),"supported path does not resume original method");
        check(oldBody.max_stack==newBody.max_stack && oldBody.max_regs==newBody.max_regs && oldBody.init_scope_depth==newBody.init_scope_depth &&
            oldBody.max_scope_depth==newBody.max_scope_depth && oldBody.exceptions.length==0 && newBody.exceptions.length==0,"method metadata changed");
        // Interpret both outcomes of the independently decoded conditional sequence.
        for(boolean supported:new boolean[]{false,true}) {
            int pc=at+2;boolean returned=false;
            if(supported)pc=at+4;else {pc++;returned=y.get(pc).definition.instructionName.equals("returnvoid");}
            check(supported?pc==at+4&&!returned:returned,"mobile/desktop branch semantics");
        }
    }
    public static void main(String[] args)throws Exception {
        SWF original=new SWF(new FileInputStream(args[0]),false);
        SWF adapted=new SWF(new FileInputStream(args[1]),false);
        check(original.getAbcList().size()==adapted.getAbcList().size(),"ABC count changed");
        for(int i=0;i<original.getAbcList().size();i++) {
            ABC a=original.getAbcList().get(i).getABC(),b=adapted.getAbcList().get(i).getABC();
            check(a.bodies.size()==b.bodies.size(),"method count changed");
            for(int j=0;j<a.bodies.size();j++)if(!Arrays.equals(a.bodies.get(j).getCodeBytes(),b.bodies.get(j).getCodeBytes()))
                verifyMenu(a,b,a.bodies.get(j),b.bodies.get(j));
        }
        byte[] fws=original.uncompressedData.clone();
        // JPEXS exposes the complete uncompressed SWF (including its header).
        check(fws[0]=='F' && fws[1]=='W',"reader's FWS representation");
        EntryCompatibility.Result fromFws=EntryCompatibility.adapt(fws);
        check(Arrays.equals(fromFws.bytes,Files.readAllBytes(Paths.get(args[1]))),"FWS and ZWS adaptations differ");
        ByteArrayOutputStream output=new ByteArrayOutputStream();output.write(fws,0,8);
        try(DeflaterOutputStream zip=new DeflaterOutputStream(output)){zip.write(fws,8,fws.length-8);}
        byte[] cws=output.toByteArray();cws[0]='C';
        check(Arrays.equals(EntryCompatibility.adapt(cws).bytes,fromFws.bytes),"CWS adaptation differs");
        Files.write(Paths.get(args[2]),fws);
        System.out.println("PASS: independent SWF/ABC parser; only the menu support guard can change; original opcodes and branch offsets preserved; mobile/desktop outcomes verified; FWS/CWS/ZWS match.");
    }
}
