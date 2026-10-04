package br.davi.narutoair.portal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Guards only entry.addBuildVersion's desktop menu. No manifest/game methods change. */
final class MobileMenuCompatibility {
    static final class Result {
        final byte[] bytes; final int guards;
        Result(byte[] bytes,int guards){this.bytes=bytes;this.guards=guards;}
    }
    private static final class Name {int kind,ns,string;}
    private static final class Reader {
        final byte[] bytes;int p;
        Reader(byte[] b){bytes=b;}
        void need(int n)throws IOException{if(n<0 || p>bytes.length-n)throw new IOException("Menu ABC truncated");}
        int b()throws IOException{need(1);return bytes[p++]&255;}
        void skip(int n)throws IOException{need(n);p+=n;}
        int u()throws IOException {
            int v=0;for(int i=0;i<5;i++){int b=b();if(i==4 && (b&252)!=0)throw new IOException("Menu ABC u30");v|=(b&127)<<(7*i);if((b&128)==0)return v;}
            throw new IOException("Menu ABC u30");
        }
        int count()throws IOException{int n=u();if(n>65536 || n>bytes.length+1)throw new IOException("Menu ABC count");return n;}
    }
    private static void u(OutputStream out,int n)throws IOException{do{int b=n&127;n>>>=7;out.write(n==0?b:b|128);}while(n!=0);}
    private static void string(OutputStream out,String s)throws IOException{byte[] b=s.getBytes(StandardCharsets.UTF_8);u(out,b.length);out.write(b);}
    private static String text(List<String> strings,int i)throws IOException{if(i<0 || i>=strings.size())throw new IOException("Menu string index");return strings.get(i);}
    private static Name name(List<Name> names,int i)throws IOException{if(i<0 || i>=names.size())throw new IOException("Menu name index");return names.get(i);}
    private static int traits(Reader r,List<Name> names,List<String> strings,boolean entry)throws IOException {
        int target=-1;
        for(int n=r.count();n>0;n--) {
            int ni=r.u(),type=r.b(),kind=type&15;
            String property=text(strings,name(names,ni).string);
            if(kind==0 || kind==6){r.u();r.u();if(r.u()!=0)r.b();}
            else if(kind>=1 && kind<=3){r.u();int method=r.u();if(entry && kind==1 && property.equals("addBuildVersion")){if(target>=0)throw new IOException("Duplicate menu method");target=method;}}
            else if(kind==4 || kind==5){r.u();r.u();}
            else throw new IOException("Menu ABC trait");
            if((type&64)!=0)for(int m=r.count();m>0;m--)r.u();
        }
        return target;
    }
    static byte[] guard(int menu,int supported)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        out.write(0x60);u(out,menu); // getlex flash.ui.ContextMenu
        out.write(0x66);u(out,supported); // getproperty public::isSupported
        out.write(new byte[]{0x11,1,0,0,0x47}); // iftrue over returnvoid
        return out.toByteArray();
    }
    static Result adapt(byte[] abc)throws IOException {
        Reader r=new Reader(abc);
        int minor=r.b()|(r.b()<<8),major=r.b()|(r.b()<<8);
        for(int i=0;i<2;i++)for(int n=r.count()-1;n>0;n--){for(int j=0;j<5;j++){int b=r.b();if((b&128)==0)break;if(j==4)throw new IOException("Menu numeric varint");}}
        r.skip(Math.max(0,r.count()-1)*8);
        if(minor==17)r.skip(Math.max(0,r.count()-1)*16);
        if(major==47)r.skip(Math.max(0,r.count()-1)*4);
        int ss=r.p,sc=r.count(),sp=r.p;List<String> strings=new ArrayList<String>();strings.add("");
        for(int n=1;n<sc;n++){int len=r.u();r.need(len);strings.add(new String(abc,r.p,len,StandardCharsets.UTF_8));r.skip(len);}int se=r.p;
        int nc=r.count();int[] nk=new int[Math.max(1,nc)],ns=new int[Math.max(1,nc)];int publicNS=0;
        for(int n=1;n<nc;n++){nk[n]=r.b();ns[n]=r.u();if(nk[n]==0x16 && text(strings,ns[n]).equals(""))publicNS=n;}
        for(int n=r.count()-1;n>0;n--)for(int j=r.count();j>0;j--)r.u();
        int ms=r.p,mc=r.count(),mp=r.p;List<Name> names=new ArrayList<Name>();names.add(new Name());
        int menu=0,supported=0,gameVersion=0,contextMenu=0;
        for(int n=1;n<mc;n++) {
            Name q=new Name();q.kind=r.b();
            switch(q.kind) {
                case 7:case 13:q.ns=r.u();q.string=r.u();break;
                case 9:case 14:q.string=r.u();r.u();break;
                case 15:case 16:q.string=r.u();break;
                case 17:case 18:break;
                case 27:case 28:r.u();break;
                case 29:r.u();for(int j=r.count();j>0;j--)r.u();break;
                default:throw new IOException("Menu ABC multiname");
            }
            names.add(q);String t=text(strings,q.string);
            if(q.kind==7 && q.ns>0 && q.ns<nc) {
                String space=text(strings,ns[q.ns]);
                if(t.equals("ContextMenu") && space.equals("flash.ui") && nk[q.ns]==0x16)menu=n;
                if(t.equals("isSupported") && q.ns==publicNS)supported=n;
                if(t.equals("gameVersion") && nk[q.ns]==5)gameVersion=n;
                if(t.equals("contextMenu") && space.equals(""))contextMenu=n;
            }
        }
        int me=r.p;
        if(menu==0)return new Result(abc,0);
        int methodCount=r.count();
        int[] params=new int[methodCount],returns=new int[methodCount];
        for(int m=0;m<methodCount;m++) {
            int n=r.count();params[m]=n;returns[m]=r.u();for(int j=0;j<n;j++)r.u();r.u();int flags=r.b();
            if((flags&8)!=0)for(int j=r.count();j>0;j--){r.u();r.b();}
            if((flags&128)!=0)for(int j=0;j<n;j++)r.u();
        }
        for(int n=r.count();n>0;n--){r.u();int items=r.count();for(int j=0;j<items*2;j++)r.u();}
        int classes=r.count(),target=-1;
        for(int n=0;n<classes;n++) {
            Name cls=name(names,r.u());boolean entry=text(strings,cls.string).equals("entry") && cls.ns>0 && cls.ns<nc && text(strings,ns[cls.ns]).equals("");
            r.u();if((r.b()&8)!=0)r.u();for(int j=r.count();j>0;j--)r.u();r.u();
            int found=traits(r,names,strings,entry);if(found>=0){if(target>=0)throw new IOException("Duplicate entry");target=found;}
        }
        for(int n=0;n<classes;n++){r.u();traits(r,names,strings,false);}
        for(int n=r.count();n>0;n--){r.u();traits(r,names,strings,false);}
        if(target<0)return new Result(abc,0);
        if(target>=methodCount || params[target]!=1 || !text(strings,name(names,returns[target]).string).equals("void") || publicNS==0 || gameVersion==0 || contextMenu==0)
            throw new IOException("Unsupported entry menu signature");
        int lengthStart=-1,codeStart=-1,codeEnd=-1;byte[] code=null;
        for(int n=r.count();n>0;n--) {
            int method=r.u();r.u();r.u();r.u();r.u();int ls=r.p,len=r.u(),cs=r.p;r.skip(len);
            int exceptions=r.count();for(int j=0;j<exceptions*5;j++)r.u();traits(r,names,strings,false);
            if(method==target){if(code!=null || exceptions!=0)throw new IOException("Unsupported menu body");lengthStart=ls;codeStart=cs;codeEnd=cs+len;code=Arrays.copyOfRange(abc,cs,cs+len);}
        }
        if(r.p!=abc.length || code==null)throw new IOException("Menu ABC trailing/body");
        // Verified compiler prefix: scope setup, local initializers, then gameVersion=arg.
        // Insert before the first branch; all original relative branch offsets stay valid.
        Reader c=new Reader(code);
        if(c.b()!=0xd0 || c.b()!=0x30 || c.b()!=0x20 || c.b()!=0x80)throw new IOException("Unsupported menu prefix");
        c.u();if(c.b()!=0xd6 || c.b()!=0x24 || c.b()!=0 || c.b()!=0xd7 || c.b()!=0xd0 || c.b()!=0xd1 || c.b()!=0x68 || c.u()!=gameVersion)
            throw new IOException("Unsupported menu version assignment");
        int insertion=c.p,newSupported=supported!=0?supported:mc;
        byte[] g=guard(menu,newSupported);
        if(code.length>=insertion+g.length && Arrays.equals(Arrays.copyOfRange(code,insertion,insertion+g.length),g))return new Result(abc,0);
        if(c.b()!=0x60 || c.u()!=contextMenu)throw new IOException("Unsupported menu first use");
        ByteArrayOutputStream body=new ByteArrayOutputStream();body.write(code,0,insertion);body.write(g);body.write(code,insertion,code.length-insertion);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        if(supported==0) {
            out.write(abc,0,ss);u(out,Math.max(1,sc)+1);out.write(abc,sp,se-sp);string(out,"isSupported");
            out.write(abc,se,ms-se);u(out,Math.max(1,mc)+1);out.write(abc,mp,me-mp);out.write(7);u(out,publicNS);u(out,Math.max(1,sc));
            out.write(abc,me,lengthStart-me);
        }else out.write(abc,0,lengthStart);
        u(out,body.size());body.writeTo(out);out.write(abc,codeEnd,abc.length-codeEnd);
        return new Result(out.toByteArray(),1);
    }
}
