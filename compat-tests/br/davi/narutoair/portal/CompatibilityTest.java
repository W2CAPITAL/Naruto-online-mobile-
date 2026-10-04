package br.davi.narutoair.portal;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
public final class CompatibilityTest {
    private static void check(boolean value,String text) { if(!value)throw new AssertionError(text); }
    private static void rejected(byte[] swf) throws Exception {
        try {EntryCompatibility.adapt(swf);throw new AssertionError("invalid SWF accepted");}catch(IOException expected){}
    }
    public static void main(String[] args)throws Exception {
        byte[] browser=Files.readAllBytes(Paths.get(args[0]));
        byte[] plain=Files.readAllBytes(Paths.get(args[1]));
        EntryCompatibility.Result adapted=EntryCompatibility.adapt(browser);
        check(adapted.references>0,"browser reference absent");
        check(adapted.securityReferences==1 && adapted.loaderReferences==1,"Security/Loader retarget counts");
        check(adapted.resourceReferences==2,"URLLoader/URLStream retarget counts");
        check(adapted.soundReferences==1,"Sound retarget count");
        check(adapted.socketReferences==1,"Socket retarget count");
        check(adapted.pageReferences==1,"ExternalInterface retarget count");
        check(adapted.bytes[0]=='F',"adapted SWF must be uncompressed");
        Files.write(Paths.get(args[2]),adapted.bytes);
        check(EntryCompatibility.adapt(adapted.bytes).references==0,"adaptation must be idempotent");
        check(Arrays.equals(EntryCompatibility.adapt(adapted.bytes).bytes,adapted.bytes),"second adaptation changed bytes");
        check(Arrays.equals(EntryCompatibility.adapt(plain).bytes,plain),"unrelated SWF changed");
        ByteArrayOutputStream compressed=new ByteArrayOutputStream();
        compressed.write(adapted.bytes,0,8);
        try(DeflaterOutputStream deflate=new DeflaterOutputStream(compressed)){deflate.write(adapted.bytes,8,adapted.bytes.length-8);}
        byte[] cws=compressed.toByteArray();cws[0]='C';
        check(Arrays.equals(EntryCompatibility.adapt(cws).bytes,cws),"unrelated CWS must be byte-for-byte preserved");
        for(int size:new int[]{0,3,7,8,9,browser.length-1})rejected(Arrays.copyOf(browser,size));
        byte[] malformed=browser.clone();malformed[4]=0;malformed[5]=0;malformed[6]=0;malformed[7]=127;rejected(malformed);
        byte[] broken=browser.clone();broken[0]='H';rejected(broken);
        try {EntryCompatibility.readBounded(new ByteArrayInputStream(new byte[1025]),1024);throw new AssertionError("buffer unbounded");}catch(IOException expected){}
        URL origin=new URL("https://cdnnaruto-pt.oasgames.com/PT_NarutoAlpha9.35Build301/entry.swf");
        Map<String,String> headers=new HashMap<String,String>();headers.put("x-naruto-air-compat","security-1");
        check(EntryCompatibility.eligible("GET",origin,origin,headers,200),"official route rejected");
        check(!EntryCompatibility.eligible("GET",origin,origin,headers,403),"403 adapted");
        check(!EntryCompatibility.eligible("GET",origin,origin,new HashMap<String,String>(),200),"missing opt-in accepted");
        check(!EntryCompatibility.eligible("HEAD",origin,origin,headers,200),"HEAD adapted");
        headers.put("range","bytes=0-20");check(!EntryCompatibility.eligible("GET",origin,origin,headers,200),"range adapted");headers.remove("range");
        check(!EntryCompatibility.eligible("GET",origin,new URL(origin,"other.swf"),headers,200),"secondary SWF adapted");
        check(!EntryCompatibility.eligible("GET",new URL("http://cdnnaruto-pt.oasgames.com/"),origin,headers,200),"insecure origin accepted");
        check(!EntryCompatibility.eligible("GET",origin,new URL("https://oasgames.com.evil.test/PT_NarutoAlpha9.35Build301/entry.swf"),headers,200),"foreign origin adapted");
        System.out.println("PASS: compiled browser fixture, FWS/CWS, idempotence, unchanged plain client, bounds, truncation and route opt-in; references="+adapted.references);
    }
}
