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
    static String fullName(ABC a,int i){Multiname m=a.constants.getMultiname(i);if(m.namespace_index==0)return name(a,i);return a.constants.getString(a.constants.getNamespace(m.namespace_index).name_index)+"::"+name(a,i);}
    static Map<Long,Integer> positions(List<AVM2Instruction> code){Map<Long,Integer> p=new HashMap<Long,Integer>();for(int i=0;i<code.size();i++)p.put(code.get(i).getAddress(),i);if(!code.isEmpty()){AVM2Instruction last=code.get(code.size()-1);p.put(last.getAddress()+last.getBytesLength(),code.size());}return p;}
    static void verifyAudio(ABC a,ABC b,MethodBody oldBody,MethodBody newBody) {
        List<AVM2Instruction> x=oldBody.getCode().code,y=newBody.getCode().code;
        check(x.size()==y.size(),"audio instruction count changed");
        Map<Long,Integer> xp=positions(x),yp=positions(y);int redirects=0;
        for(int k=0;k<x.size();k++){
            AVM2Instruction old=x.get(k),now=y.get(k);int op=old.definition.instructionCode;
            int[] oo=old.operands==null?new int[0]:old.operands,nn=now.operands==null?new int[0]:now.operands;
            check(op==now.definition.instructionCode,"audio opcode changed");
            check(oo.length==nn.length,"audio operand count changed");
            List<Long> ot=old.getOffsets(),nt=now.getOffsets();
            if(!ot.isEmpty()){
                check(ot.size()==nt.size(),"audio branch/switch count");
                for(int j=0;j<ot.size();j++)check(xp.get(ot.get(j))!=null && xp.get(ot.get(j)).equals(yp.get(nt.get(j))),"audio branch/switch target changed");
            }else if(!Arrays.equals(oo,nn)){
                check(op==0x5d||op==0x5e||op==0x4a,"non-constructor instruction changed");
                check(fullName(a,old.operands[0]).equals("flash.media::Sound") && fullName(b,now.operands[0]).equals("br.davi.narutoair.compat::BrowserSound"),"only new native Sound may redirect");
                for(int j=1;j<old.operands.length;j++)check(old.operands[j]==now.operands[j],"audio argument count changed");
                redirects++;
            }
        }
        check(redirects>0 && redirects%2==0,"audio constructor pair");
        check(oldBody.max_stack==newBody.max_stack && oldBody.max_regs==newBody.max_regs && oldBody.init_scope_depth==newBody.init_scope_depth && oldBody.max_scope_depth==newBody.max_scope_depth,"audio method stack metadata changed");
        check(oldBody.exceptions.length==newBody.exceptions.length,"audio handlers count");
        for(int j=0;j<oldBody.exceptions.length;j++){
            ABCException old=oldBody.exceptions[j],now=newBody.exceptions[j];
            check(xp.get((long)old.start).equals(yp.get((long)now.start)) && xp.get((long)old.end).equals(yp.get((long)now.end)) && xp.get((long)old.target).equals(yp.get((long)now.target)),"audio handler boundary changed");
            check(old.type_index==now.type_index && old.name_index==now.name_index,"audio handler type changed");
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
                {if(menuMethod(a,a.bodies.get(j).method_info))verifyMenu(a,b,a.bodies.get(j),b.bodies.get(j));else verifyAudio(a,b,a.bodies.get(j),b.bodies.get(j));}
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
        System.out.println("PASS: independent SWF/ABC parser; menu guard plus paired external Sound constructors only; native audio types, branch/switch targets and handlers preserved; mobile/desktop outcomes verified; FWS/CWS/ZWS match.");
    }
}
