package br.davi.narutoair.portal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.InflaterInputStream;
import SevenZip.Compression.LZMA.Decoder;

/** Retargets the official client's Security and Loader class references.
 * Class references are retargeted; only entry.addBuildVersion receives a mobile menu guard.
 * Other method bodies and all original branch offsets are preserved.
 * The adapter does not grant cross-sandbox privileges or change the AIR runtime.
 */
public final class EntryCompatibility {
    private static final int MAX_EXPANDED = 32 * 1024 * 1024;
    private static final String[] TARGETS={"BrowserSecurity","BrowserLoader","BrowserURLLoader","BrowserURLStream","BrowserExternalInterface","BrowserSocket","BrowserSound"};
    public static final class Result {
        public final byte[] bytes;
        public final int references;
        public final int securityReferences,loaderReferences,resourceReferences,pageReferences,menuGuards,socketReferences,soundReferences;
        Result(byte[] bytes,int security,int loaders,int resources) {
            this(bytes,security,loaders,resources,0);
        }
        Result(byte[] bytes,int security,int loaders,int resources,int page) {
            this(bytes,security,loaders,resources,page,0);
        }
        Result(byte[] bytes,int security,int loaders,int resources,int page,int menus) {
            this(bytes,security,loaders,resources,page,menus,0);
        }
        Result(byte[] bytes,int security,int loaders,int resources,int page,int menus,int sockets) {
            this(bytes,security,loaders,resources,page,menus,sockets,0);
        }
        Result(byte[] bytes,int security,int loaders,int resources,int page,int menus,int sockets,int sounds) {
            this.soundReferences=sounds;this.socketReferences=sockets;this.bytes=bytes;this.securityReferences=security;this.loaderReferences=loaders;
            this.resourceReferences=resources;this.pageReferences=page;this.menuGuards=menus;this.references=security+loaders+resources+page+menus+sockets+sounds;
        }
    }
    public static boolean eligible(String method, java.net.URL origin, java.net.URL url,
                                   Map<String,String> headers, int status) {
        return method.equals("GET") && status == 200 &&
            "security-1".equals(headers.get("x-naruto-air-compat")) && !headers.containsKey("range") &&
            origin.getProtocol().equals("https") && origin.getHost().equals("cdnnaruto-pt.oasgames.com") &&
            (origin.getPort() == -1 || origin.getPort() == 443) && RuntimeTransport.sameOrigin(origin,url) &&
            url.getPath().matches("/PT_NarutoAlpha[0-9.]+Build[0-9]+/entry\\.swf");
    }
    public static byte[] readBounded(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buffer=new byte[16384];
        for(int n;(n=in.read(buffer))!=-1;) {
            if(n > limit-out.size()) throw new IOException("SWF size limit");
            out.write(buffer,0,n);
        }
        return out.toByteArray();
    }
    public static Result adapt(byte[] input) throws IOException {
        if(input.length<9 || input[1]!='W' || input[2]!='S' || input[3]==0) throw new IOException("SWF header");
        long declared=u32(input,4);
        if(declared<9 || declared>MAX_EXPANDED) throw new IOException("SWF expanded size");
        byte[] body;
        if(input[0]=='F') {
            if(declared!=input.length) throw new IOException("SWF truncated");
            body=Arrays.copyOfRange(input,8,input.length);
        } else if(input[0]=='C') {
            try(InputStream zip=new InflaterInputStream(new ByteArrayInputStream(input,8,input.length-8))) {
                body=readBounded(zip,(int)declared-8);
            }
            if(body.length!=declared-8) throw new IOException("SWF decompressed length");
        } else if(input[0]=='Z') {
            if(input.length<17 || u32(input,8)!=input.length-17) throw new IOException("SWF LZMA length");
            long dictionary=u32(input,13);
            if(dictionary>MAX_EXPANDED) throw new IOException("SWF LZMA dictionary limit");
            Decoder decoder=new Decoder();
            if(!decoder.SetDecoderProperties(Arrays.copyOfRange(input,12,17))) throw new IOException("SWF LZMA properties");
            final int limit=(int)declared-8;
            ByteArrayOutputStream expanded=new ByteArrayOutputStream();
            OutputStream bounded=new FilterOutputStream(expanded) {
                int size;
                public void write(int b)throws IOException {if(size==limit)throw new IOException("SWF LZMA expanded limit");out.write(b);size++;}
                public void write(byte[] b,int off,int len)throws IOException {if(len>limit-size)throw new IOException("SWF LZMA expanded limit");out.write(b,off,len);size+=len;}
            };
            if(!decoder.Code(new ByteArrayInputStream(input,17,input.length-17),bounded,limit)) throw new IOException("SWF LZMA decode");
            body=expanded.toByteArray();
            if(body.length!=limit)throw new IOException("SWF LZMA decompressed length");
        } else throw new IOException("SWF compression signature");
        int rect=(5+4*((body[0]&255)>>>3)+7)/8;
        int pos=rect+4;
        if(pos>body.length) throw new IOException("SWF frame header");
        ByteArrayOutputStream out=new ByteArrayOutputStream(); out.write(body,0,pos);
        int security=0,loaders=0,resources=0,page=0,menus=0,sockets=0,sounds=0; boolean ended=false;
        while(pos<body.length) {
            int start=pos;
            if(pos>body.length-2) throw new IOException("SWF tag header");
            int header=(body[pos]&255)|((body[pos+1]&255)<<8); pos+=2;
            int type=header>>>6; long length=header&63;
            if(length==63) { length=u32(body,pos);pos+=4; }
            if(length>body.length-pos) throw new IOException("SWF tag length");
            int end=pos+(int)length;
            Result tag=null;
            if(type==82) {
                int abc=pos+4;
                if(abc>=end) throw new IOException("DoABC flags/name");
                while(abc<end && body[abc]!=0) abc++;
                if(abc==end) throw new IOException("DoABC name");
                abc++;
                tag=adaptAbc(Arrays.copyOfRange(body,abc,end));
                if(tag.references>0) {
                    int size=abc-pos+tag.bytes.length;
                    u16(out,(82<<6)|63);put32(out,size);
                    out.write(body,pos,abc-pos);out.write(tag.bytes);
                    security+=tag.securityReferences;loaders+=tag.loaderReferences;resources+=tag.resourceReferences;
                    page+=tag.pageReferences;menus+=tag.menuGuards;sockets+=tag.socketReferences;sounds+=tag.soundReferences;
                }
            }
            if(tag==null || tag.references==0) out.write(body,start,end-start);
            pos=end;
            if(type==0) {
                if(length!=0) throw new IOException("SWF end tag");
                out.write(body,pos,body.length-pos);ended=true;break;
            }
        }
        if(!ended) throw new IOException("SWF missing end tag");
        if(security+loaders+resources+page+menus+sockets+sounds==0) return new Result(input,0,0,0);
        if(out.size()>MAX_EXPANDED-8) throw new IOException("Adapted SWF size");
        ByteArrayOutputStream swf=new ByteArrayOutputStream();
        swf.write('F');swf.write('W');swf.write('S');swf.write(input[3]);put32(swf,out.size()+8);out.writeTo(swf);
        return new Result(swf.toByteArray(),security,loaders,resources,page,menus,sockets,sounds);
    }
    private static Result adaptAbc(byte[] abc) throws IOException {
        MobileMenuCompatibility.Result menu=MobileMenuCompatibility.adapt(abc);
        abc=menu.bytes;
        Cursor c=new Cursor(abc);
        int minor=c.u16(),major=c.u16();
        if((major!=46 && major!=47) || (minor!=16 && minor!=17)) throw new IOException("ABC version unsupported");
        for(int pool=0;pool<2;pool++) {
            int count=c.count();for(int i=1;i<count;i++) c.var32();
        }
        int doubles=c.count(); c.skip(Math.max(0,doubles-1)*8);
        if(minor==17) {int decimals=c.count();c.skip(Math.max(0,decimals-1)*16);}
        if(major==47) {int floats=c.count();c.skip(Math.max(0,floats-1)*4);}
        int stringsStart=c.pos, stringCount=c.count();
        List<String> strings=new ArrayList<String>(); strings.add("");
        for(int i=1;i<stringCount;i++) {
            int size=c.u30();c.need(size);
            strings.add(new String(abc,c.pos,size,StandardCharsets.UTF_8));c.skip(size);
        }
        int stringsEnd=c.pos, nsStart=c.pos, nsCount=c.count();
        int[] nsKind=new int[Math.max(1,nsCount)],nsName=new int[Math.max(1,nsCount)];
        for(int i=1;i<nsCount;i++) {nsKind[i]=c.byteValue();nsName[i]=c.index(strings.size());}
        int nsEnd=c.pos, setsStart=c.pos, setsCount=c.count();
        List<int[]> sets=new ArrayList<int[]>();sets.add(new int[0]);
        for(int i=1;i<setsCount;i++) {
            int[] s=new int[c.count()];for(int j=0;j<s.length;j++)s[j]=c.index(nsKind.length);sets.add(s);
        }
        int setsEnd=c.pos, namesStart=c.pos, nameCount=c.count();
        int newNs=Math.max(1,nsCount),firstName=Math.max(1,stringCount)+1;
        ByteArrayOutputStream names=new ByteArrayOutputStream();put30(names,nameCount);
        List<int[]> extraSets=new ArrayList<int[]>();int[] counts=new int[TARGETS.length];
        for(int i=1;i<nameCount;i++) {
            int start=c.pos,kind=c.byteValue();
            if(kind==7 || kind==13) {
                int ns=c.index(nsKind.length),name=c.index(strings.size());
                int target=adapter(ns,name,nsKind,nsName,strings);
                if(kind==7 && target>=0) {
                    names.write(kind);put30(names,newNs);put30(names,firstName+target);
                    counts[target]++;continue;
                }
            } else if(kind==9 || kind==14) {
                int name=c.index(strings.size()),set=c.index(sets.size());
                if(kind==9) {
                    int[] replacement=sets.get(set).clone();int target=-1;
                    for(int j=0;j<replacement.length;j++) {
                        int match=adapter(replacement[j],name,nsKind,nsName,strings);
                        if(match>=0){replacement[j]=newNs;target=match;}
                    }
                    if(target>=0) {
                        int idx=Math.max(1,setsCount)+extraSets.size();extraSets.add(replacement);
                        names.write(kind);put30(names,firstName+target);put30(names,idx);
                        counts[target]++;continue;
                    }
                }
            } else if(kind==15 || kind==16) c.index(strings.size());
            else if(kind==17 || kind==18) { }
            else if(kind==27 || kind==28) c.index(sets.size());
            else if(kind==29) {c.index(Math.max(1,nameCount));int count=c.count();for(int j=0;j<count;j++)c.index(Math.max(1,nameCount));}
            else throw new IOException("ABC multiname kind");
            names.write(abc,start,c.pos-start);
        }
        int references=0;for(int count:counts)references+=count;
        if(references==0) return new Result(abc,0,0,0,0,menu.guards);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        out.write(abc,0,stringsStart);put30(out,Math.max(1,stringCount)+1+TARGETS.length);
        copyPoolPayload(out,abc,stringsStart,stringsEnd);
        string(out,"br.davi.narutoair.compat");for(String target:TARGETS)string(out,target);
        put30(out,Math.max(1,nsCount)+1);copyPoolPayload(out,abc,nsStart,nsEnd);
        out.write(0x16);put30(out,Math.max(1,stringCount));
        put30(out,extraSets.isEmpty()?setsCount:Math.max(1,setsCount)+extraSets.size());
        copyPoolPayload(out,abc,setsStart,setsEnd);
        for(int[] s:extraSets){put30(out,s.length);for(int ns:s)put30(out,ns);}
        names.writeTo(out);out.write(abc,c.pos,abc.length-c.pos);
        return new Result(out.toByteArray(),counts[0],counts[1],counts[2]+counts[3],counts[4],menu.guards,counts[5],counts[6]);
    }
    private static int adapter(int ns,int className,int[] kind,int[] name,List<String> strings) {
        if(ns==0 || (kind[ns]!=0x16 && kind[ns]!=8))return -1;
        String namespace=strings.get(name[ns]),type=strings.get(className);
        if(namespace.equals("flash.system") && type.equals("Security"))return 0;
        if(namespace.equals("flash.display") && type.equals("Loader"))return 1;
        if(namespace.equals("flash.net") && type.equals("URLLoader"))return 2;
        if(namespace.equals("flash.net") && type.equals("URLStream"))return 3;
        if(namespace.equals("flash.external") && type.equals("ExternalInterface"))return 4;
        if(namespace.equals("flash.net") && type.equals("Socket"))return 5;
        if(namespace.equals("flash.media") && type.equals("Sound"))return 6;
        return -1;
    }
    private static void copyPoolPayload(OutputStream out,byte[] abc,int start,int end) throws IOException {
        Cursor cursor=new Cursor(abc);cursor.pos=start;cursor.u30();out.write(abc,cursor.pos,end-cursor.pos);
    }
    private static void string(OutputStream out,String text) throws IOException {
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);put30(out,bytes.length);out.write(bytes);
    }
    private static long u32(byte[] b,int p) throws IOException {
        if(p<0 || p>b.length-4)throw new IOException("Truncated u32");
        return (b[p]&255L)|((b[p+1]&255L)<<8)|((b[p+2]&255L)<<16)|((b[p+3]&255L)<<24);
    }
    private static void put32(OutputStream out,int value) throws IOException {
        for(int i=0;i<4;i++)out.write((value>>>(8*i))&255);
    }
    private static void u16(OutputStream out,int value) throws IOException {out.write(value&255);out.write((value>>>8)&255);}
    private static void put30(OutputStream out,int value) throws IOException {
        do {int b=value&127;value>>>=7;out.write(value==0?b:b|128);}while(value!=0);
    }
    private static final class Cursor {
        final byte[] bytes;int pos;
        Cursor(byte[] bytes){this.bytes=bytes;}
        void need(int n)throws IOException{if(n<0 || pos>bytes.length-n)throw new IOException("Truncated ABC");}
        void skip(int n)throws IOException{need(n);pos+=n;}
        int byteValue()throws IOException{need(1);return bytes[pos++]&255;}
        int u16()throws IOException{return byteValue()|(byteValue()<<8);}
        long var32()throws IOException{
            long value=0;for(int i=0;i<5;i++){int b=byteValue();if(i==4 && (b&240)!=0)throw new IOException("ABC varint overflow");value|=(long)(b&127)<<(i*7);if((b&128)==0)return value;}throw new IOException("ABC varint");
        }
        int u30()throws IOException{long n=var32();if(n>0x3fffffffL)throw new IOException("ABC u30");return(int)n;}
        int count()throws IOException{int n=u30();if(n>65536 || n>bytes.length+1)throw new IOException("ABC pool limit");return n;}
        int index(int length)throws IOException{int n=u30();if(n>=length)throw new IOException("ABC pool index");return n;}
    }
}
