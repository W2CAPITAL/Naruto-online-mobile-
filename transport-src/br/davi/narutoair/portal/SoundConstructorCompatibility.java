package br.davi.narutoair.portal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Redirects only statically resolved new Sound(...) expressions.
 * Sound types, casts, superclass indices and embedded SymbolClass audio stay native.
 * Pool growth can widen u30 operands: branches, switches and handlers are relocated.
 * Unsupported or ambiguous constructor expressions remain native.
 */
final class SoundConstructorCompatibility {
    static final class Result {final byte[] bytes;final int constructors;Result(byte[] b,int n){bytes=b;constructors=n;}}
    private static final class Reader {
        final byte[] bytes;int p;
        Reader(byte[] b){bytes=b;}
        void need(int n)throws IOException{if(n<0 || p>bytes.length-n)throw new IOException("Audio ABC truncated");}
        int b()throws IOException{need(1);return bytes[p++]&255;}
        void skip(int n)throws IOException{need(n);p+=n;}
        int u()throws IOException{int v=0;for(int i=0;i<5;i++){int b=b();if(i==4 && (b&252)!=0)throw new IOException("Audio u30 overflow");v|=(b&127)<<(7*i);if((b&128)==0)return v;}throw new IOException("Audio u30");}
        int count()throws IOException{int n=u();if(n>65536 || n>bytes.length+1)throw new IOException("Audio count");return n;}
        int s24()throws IOException{int v=b()|(b()<<8)|(b()<<16);return (v<<8)>>8;}
    }
    private static void u(OutputStream o,int n)throws IOException{do{int b=n&127;n>>>=7;o.write(n==0?b:b|128);}while(n!=0);}
    private static void s24(OutputStream o,int n)throws IOException{if(n<-8388608 || n>8388607)throw new IOException("Audio branch range");o.write(n&255);o.write((n>>>8)&255);o.write((n>>>16)&255);}
    private static void string(OutputStream o,String s)throws IOException{byte[] b=s.getBytes(StandardCharsets.UTF_8);u(o,b.length);o.write(b);}
    private static void traits(Reader r)throws IOException{for(int n=r.count();n>0;n--){r.u();int t=r.b(),k=t&15;if(k==0||k==6){r.u();r.u();if(r.u()!=0)r.b();}else if(k>=1&&k<=5){r.u();r.u();}else throw new IOException("Audio trait");if((t&64)!=0)for(int j=r.count();j>0;j--)r.u();}}
    private static final class Ins {int start,end,op,name=-1,replacement=-1;int[] targets;byte[] raw;}
    private static final int[] FORMATS=new int[256];
    static {
        Arrays.fill(FORMATS,-1);
        // AVM2 operand encodings: 0=none, 1=u30, 2=two u30, 3=s24,
        // 4=lookupswitch, 5=u8, 6=debug(u8,u30,u8,u30).
        for(int op:new int[]{0x01,0x02,0x03,0x07,0x09,0x0a,0x0b,0x1c,0x1d,0x1e,0x1f,0x20,0x21,0x23,0x26,0x27,0x28,0x29,0x2a,0x2b,0x30,0x34,0x35,0x36,0x37,0x38,0x39,0x3a,0x3b,0x3c,0x3d,0x3e,0x47,0x48,0x4b,0x50,0x51,0x52,0x57,0x64,0x69,0x6b,0x70,0x71,0x72,0x73,0x74,0x75,0x76,0x77,0x78,0x79,0x7a,0x7b,0x81,0x82,0x83,0x84,0x85,0x87,0x88,0x89,0x90,0x91,0x93,0x95,0x96,0x97,0x9a,0x9b,0xa0,0xa1,0xa2,0xa3,0xa4,0xa5,0xa6,0xa7,0xa8,0xa9,0xaa,0xab,0xac,0xad,0xae,0xaf,0xb0,0xb1,0xb3,0xb4,0xc0,0xc1,0xc4,0xc5,0xc6,0xc7,0xd0,0xd1,0xd2,0xd3,0xd4,0xd5,0xd6,0xd7,0xf3})FORMATS[op]=0;
        for(int op:new int[]{0x04,0x05,0x06,0x08,0x22,0x25,0x2c,0x2d,0x2e,0x2f,0x31,0x33,0x40,0x41,0x42,0x49,0x4d,0x53,0x54,0x55,0x56,0x58,0x59,0x5a,0x5b,0x5d,0x5e,0x5f,0x60,0x61,0x62,0x63,0x66,0x67,0x68,0x6a,0x6c,0x6d,0x6e,0x6f,0x80,0x86,0x8f,0x92,0x94,0x9c,0x9e,0xb2,0xb5,0xb6,0xb7,0xb8,0xb9,0xc2,0xc3,0xf0,0xf1,0xf2})FORMATS[op]=1;
        for(int op:new int[]{0x32,0x43,0x44,0x45,0x46,0x4a,0x4c,0x4e,0x4f,0x9d,0x9f})FORMATS[op]=2;
        for(int op:new int[]{0x0c,0x0d,0x0e,0x0f,0x10,0x11,0x12,0x13,0x14,0x15,0x16,0x17,0x18,0x19,0x1a})FORMATS[op]=3;
        for(int op:new int[]{0x1b})FORMATS[op]=4;
        for(int op:new int[]{0x24,0x65})FORMATS[op]=5;
        for(int op:new int[]{0xef})FORMATS[op]=6;
    }
    private static final class Code {
        byte[] bytes;int changes;Map<Integer,Integer> offsets;
        int at(int old)throws IOException{Integer n=offsets.get(old);if(n==null)throw new IOException("Audio control-flow boundary");return n;}
    }
    private static Code rewrite(byte[] code,Set<Integer> sound,int replacement)throws IOException {
        Code result=new Code();result.bytes=code;result.offsets=new HashMap<Integer,Integer>();
        Reader r=new Reader(code);List<Ins> list=new ArrayList<Ins>();Set<Integer> boundaries=new HashSet<Integer>();
        while(r.p<code.length){Ins i=new Ins();i.start=r.p;i.op=r.b();int f=FORMATS[i.op];if(f<0)return result;
            if(f==1||f==2){i.name=r.u();if(f==2)r.u();}
            else if(f==3){int jump=r.s24();i.targets=new int[]{r.p+jump};boundaries.add(i.targets[0]);}
            else if(f==4){int def=r.s24(),n=r.count();i.targets=new int[n+2];i.targets[0]=i.start+def;for(int j=1;j<i.targets.length;j++)i.targets[j]=i.start+r.s24();for(int t:i.targets)boundaries.add(t);}
            else if(f==5)r.b();else if(f==6){r.b();r.u();r.b();r.u();}
            i.end=r.p;i.raw=Arrays.copyOfRange(code,i.start,i.end);list.add(i);
        }
        Map<Integer,Deque<Ins>> pending=new HashMap<Integer,Deque<Ins>>();
        for(Ins i:list){
            if(boundaries.contains(i.start))pending.clear();
            if((i.op==0x5d||i.op==0x5e) && sound.contains(i.name)){
                Deque<Ins> q=pending.get(i.name);if(q==null){q=new ArrayDeque<Ins>();pending.put(i.name,q);}q.push(i);
            }else if(i.op==0x4a && sound.contains(i.name)){
                Deque<Ins> q=pending.get(i.name);if(q!=null&&!q.isEmpty()){Ins find=q.pop();find.replacement=replacement;i.replacement=replacement;result.changes++;}
            }else if((i.op==0x46||i.op==0x4c||i.op==0x4f) && sound.contains(i.name))pending.remove(i.name);
            // These operations can consume/store the constructor's scope object.
            if(i.targets!=null || i.op==0x03 || i.op==0x47 || i.op==0x48 || i.op==0x29 || i.op==0x2a || i.op==0x2b || i.op==0x63 || (i.op>=0xd4&&i.op<=0xd7))pending.clear();
        }
        if(result.changes==0)return result;
        int size=0;
        for(Ins i:list){result.offsets.put(i.start,size);if(i.replacement>=0){Reader old=new Reader(i.raw);old.b();old.u();ByteArrayOutputStream b=new ByteArrayOutputStream();b.write(i.op);u(b,i.replacement);b.write(i.raw,old.p,i.raw.length-old.p);i.raw=b.toByteArray();}size+=i.raw.length;}
        result.offsets.put(code.length,size);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(Ins i:list){if(i.targets==null)out.write(i.raw);else if(i.op==0x1b){out.write(i.op);s24(out,result.at(i.targets[0])-result.at(i.start));u(out,i.targets.length-2);for(int j=1;j<i.targets.length;j++)s24(out,result.at(i.targets[j])-result.at(i.start));}else{out.write(i.op);s24(out,result.at(i.targets[0])-(result.at(i.start)+i.raw.length));}}
        result.bytes=out.toByteArray();return result;
    }
    static Result adapt(byte[] abc)throws IOException {
        Reader r=new Reader(abc);int minor=r.b()|(r.b()<<8),major=r.b()|(r.b()<<8);
        for(int j=0;j<2;j++)for(int n=r.count()-1;n>0;n--){for(int k=0;k<5;k++){if((r.b()&128)==0)break;if(k==4)throw new IOException("Audio numeric varint");}}
        r.skip(Math.max(0,r.count()-1)*8);if(minor==17)r.skip(Math.max(0,r.count()-1)*16);if(major==47)r.skip(Math.max(0,r.count()-1)*4);
        int ss=r.p,sc=r.count(),sp=r.p;List<String> strings=new ArrayList<String>();strings.add("");
        for(int n=1;n<sc;n++){int len=r.u();r.need(len);strings.add(new String(abc,r.p,len,StandardCharsets.UTF_8));r.skip(len);}int se=r.p;
        int nsStart=r.p,nc=r.count(),np=r.p;int[] nk=new int[Math.max(1,nc)],ns=new int[Math.max(1,nc)];
        for(int n=1;n<nc;n++){nk[n]=r.b();ns[n]=r.u();}int ne=r.p;
        int setCount=r.count();List<int[]> sets=new ArrayList<int[]>();sets.add(new int[0]);for(int n=1;n<setCount;n++){int[] set=new int[r.count()];for(int j=0;j<set.length;j++)set[j]=r.u();sets.add(set);}
        int ms=r.p,mc=r.count(),mp=r.p;Set<Integer> sounds=new HashSet<Integer>();
        for(int n=1;n<mc;n++){
            int k=r.b(),namespace=0,name=0,set=0;
            if(k==7||k==13){namespace=r.u();name=r.u();}else if(k==9||k==14){name=r.u();set=r.u();}else if(k==15||k==16)r.u();else if(k==17||k==18){}else if(k==27||k==28)r.u();else if(k==29){r.u();for(int j=r.count();j>0;j--)r.u();}else throw new IOException("Audio multiname");
            if(name>0 && strings.get(name).equals("Sound")){
                if(k==7 && namespace>0 && (nk[namespace]==0x16||nk[namespace]==8) && strings.get(ns[namespace]).equals("flash.media"))sounds.add(n);
                if(k==9 && set>0){int[] candidates=sets.get(set);if(candidates.length==1){int s=candidates[0];if((nk[s]==0x16||nk[s]==8)&&strings.get(ns[s]).equals("flash.media"))sounds.add(n);}}
            }
        }
        int me=r.p;if(sounds.isEmpty())return new Result(abc,0);
        int methodCount=r.count();for(int n=0;n<methodCount;n++){int count=r.count();r.u();for(int j=0;j<count;j++)r.u();r.u();int flags=r.b();if((flags&8)!=0)for(int j=r.count();j>0;j--){r.u();r.b();}if((flags&128)!=0)for(int j=0;j<count;j++)r.u();}
        for(int n=r.count();n>0;n--){r.u();for(int j=r.count()*2;j>0;j--)r.u();}
        int classes=r.count();for(int n=0;n<classes;n++){r.u();r.u();if((r.b()&8)!=0)r.u();for(int j=r.count();j>0;j--)r.u();r.u();traits(r);}
        for(int n=0;n<classes;n++){r.u();traits(r);}for(int n=r.count();n>0;n--){r.u();traits(r);}
        int bodies=r.count(),bodyStart=r.p,changes=0;ByteArrayOutputStream body=new ByteArrayOutputStream();
        for(int n=0;n<bodies;n++){
            int start=r.p;for(int j=0;j<5;j++)r.u();int ls=r.p,len=r.u(),cs=r.p;r.skip(len);
            Code c=rewrite(Arrays.copyOfRange(abc,cs,cs+len),sounds,Math.max(1,mc));changes+=c.changes;
            body.write(abc,start,ls-start);u(body,c.bytes.length);body.write(c.bytes);
            int ec=r.count();u(body,ec);for(int j=0;j<ec;j++){int from=r.u(),to=r.u(),target=r.u();u(body,c.changes>0?c.at(from):from);u(body,c.changes>0?c.at(to):to);u(body,c.changes>0?c.at(target):target);u(body,r.u());u(body,r.u());}
            int ts=r.p;traits(r);body.write(abc,ts,r.p-ts);
        }
        if(r.p!=abc.length)throw new IOException("Audio ABC trailing bytes");if(changes==0)return new Result(abc,0);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        out.write(abc,0,ss);u(out,Math.max(1,sc)+2);out.write(abc,sp,se-sp);string(out,"br.davi.narutoair.compat");string(out,"BrowserSound");
        u(out,Math.max(1,nc)+1);out.write(abc,np,ne-np);out.write(0x16);u(out,Math.max(1,sc));
        out.write(abc,ne,ms-ne);u(out,Math.max(1,mc)+1);out.write(abc,mp,me-mp);out.write(7);u(out,Math.max(1,nc));u(out,Math.max(1,sc)+1);
        out.write(abc,me,bodyStart-me);body.writeTo(out);return new Result(out.toByteArray(),changes);
    }
}
