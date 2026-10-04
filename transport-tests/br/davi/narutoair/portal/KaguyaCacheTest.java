package br.davi.narutoair.portal;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Byte-for-byte packaged assets through the actual loopback transport; no live account. */
public final class KaguyaCacheTest {
    static final String ORIGIN="https://cdnnaruto-pt.oasgames.com";
    static final String PREFIX="/PT_NarutoAlpha9.35Build301/";
    static final Map<String,String> EMPTY=Collections.emptyMap();
    static int requests;static byte[] upstreamBytes;static String upstreamControl="public, max-age=600",upstreamVary,upstreamCookie,upstreamAge;
    static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static URL url(String path)throws Exception{return new URL(ORIGIN+PREFIX+path);}
    static byte[] fetch(String method,String path)throws Exception {
        try(ServerSocket server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))) {
            Thread worker=new Thread(()->{try{RuntimeTransport.handle(new PortalContext(),server.accept(),ORIGIN,ORIGIN,"Test");}catch(Exception e){throw new RuntimeException(e);}});
            worker.start();byte[] bytes;
            try(Socket client=new Socket("127.0.0.1",server.getLocalPort())) {
                client.setSoTimeout(5000);
                client.getOutputStream().write((method+" "+PREFIX+path+" HTTP/1.1\r\nHost: localhost\r\n\r\n").getBytes("ISO-8859-1"));
                bytes=EntryCompatibility.readBounded(client.getInputStream(),1024*1024);
            }
            worker.join(5000);check(!worker.isAlive(),"transport worker stuck");return bytes;
        }
    }
    static byte[] body(byte[] response)throws Exception {
        String text=new String(response,"ISO-8859-1");int split=text.indexOf("\r\n\r\n");check(split>0,"HTTP headers missing");
        check(text.startsWith("HTTP/1.1 200"),"response failed");return Arrays.copyOfRange(response,split+4,response.length);
    }
    static void cache(URL url,byte[] bytes,String control)throws Exception {
        StaticAssetCache.Writer writer=StaticAssetCache.begin(url,bytes.length,control,null,false);check(writer!=null,"cache writer missing");
        writer.write(bytes,0,bytes.length);writer.finish();
    }
    public static void main(String[] args)throws Exception {
        // Official-origin URL with a deterministic upstream; exercises the production cache-write branch.
        URL.setURLStreamHandlerFactory(protocol->!protocol.equals("https")?null:new URLStreamHandler(){
            protected int getDefaultPort(){return 443;}
            protected URLConnection openConnection(URL url){requests++;return new HttpURLConnection(url){
                public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}
                public int getResponseCode(){return 200;}public String getContentType(){return "image/png";}
                public long getContentLengthLong(){return upstreamBytes.length;}
                public Map<String,List<String>> getHeaderFields(){return Collections.emptyMap();}
                public String getHeaderField(String name){if(name.equalsIgnoreCase("Content-Length"))return String.valueOf(upstreamBytes.length);if(name.equalsIgnoreCase("Cache-Control"))return upstreamControl;if(name.equalsIgnoreCase("Vary"))return upstreamVary;if(name.equalsIgnoreCase("Set-Cookie"))return upstreamCookie;if(name.equalsIgnoreCase("Age"))return upstreamAge;return null;}
                public InputStream getInputStream(){return new ByteArrayInputStream(upstreamBytes);}
            };}
        });
        final Path project=Paths.get(args[0]);Path cache=Files.createTempDirectory("naruto-cache-test");
        KaguyaResources.Assets packaged=path->Files.newInputStream(project.resolve(path));
        StaticAssetCache.configure(cache.toFile());
        String portrait="assets/battle/role/head_176_68/10000201/1.png";
        byte[] vento=Files.readAllBytes(project.resolve("mods/kaguya/feng/hengtiao.png"));
        byte[] fogo=Files.readAllBytes(project.resolve("mods/kaguya/huo/hengtiao.png"));
        byte[] original="official cache fixture".getBytes("UTF-8");cache(url(portrait),original,"public, max-age=600");
        upstreamBytes=vento;
        byte[] first=fetch("GET","assets/cache-integration.png");check(Arrays.equals(body(first),vento) && requests==1,"upstream cache miss");
        byte[] second=fetch("GET","assets/cache-integration.png");check(Arrays.equals(body(second),vento) && requests==1 && new String(second,"ISO-8859-1").contains("X-Naruto-Resource: Disk"),"second fetch not cached");
        for(String path:new String[]{"assets/vary.png","assets/cookie.png","assets/stale.png","assets/private.png"}) {
            upstreamVary=path.contains("vary")?"Cookie":null;upstreamCookie=path.contains("cookie")?"session=test":null;upstreamAge=path.contains("stale")?"601":null;upstreamControl=path.contains("private")?"private, max-age=600":"public, max-age=600";
            int before=requests;fetch("GET",path);fetch("GET",path);check(requests==before+2,"uncacheable response cached "+path);
        }
        upstreamVary=null;upstreamCookie=null;upstreamAge=null;upstreamControl="public, max-age=600";
        KaguyaResources.configure(true,0,packaged);
        check(Arrays.equals(body(fetch("GET",portrait)),vento),"original cache hid mod");
        check(body(fetch("HEAD",portrait)).length==0,"HEAD sent body");
        check(new String(fetch("HEAD",portrait),"ISO-8859-1").contains("Content-Length: "+vento.length),"HEAD length");
        check(Arrays.equals(body(fetch("GET",portrait+"?version=1")),vento),"query routing");
        KaguyaResources.configure(true,1,packaged);check(Arrays.equals(body(fetch("GET",portrait)),fogo),"style not switched");
        KaguyaResources.configure(false,1,packaged);check(Arrays.equals(body(fetch("GET",portrait)),original),"OFF failed to restore original cache");
        KaguyaResources.configure(true,0,path->{throw new IOException("missing");});check(Arrays.equals(body(fetch("GET",portrait)),original),"missing mod did not fall back");
        KaguyaResources.configure(true,0,path->new ByteArrayInputStream(new byte[]{1,2}));check(Arrays.equals(body(fetch("GET",portrait)),original),"invalid PNG did not fall back");
        KaguyaResources.configure(true,0,packaged);
        for(String style:new String[]{"feng","huo","lei","shui","tu"}) {
            int index=Arrays.asList("feng","huo","lei","shui","tu").indexOf(style);KaguyaResources.configure(true,index,packaged);
            for(String size:new String[]{"176_68","45_45"}) {
                byte[] expected=Files.readAllBytes(project.resolve("mods/kaguya/"+style+"/"+(size.equals("176_68")?"hengtiao":"jineng")+".png"));
                check(Arrays.equals(KaguyaResources.read(url("assets/battle/role/head_"+size+"/10000201/1.png")),expected),"portrait bytes");
            }
        }
        for(Path sound:Files.newDirectoryStream(project.resolve("mods/kaguya/sounds"))) {
            check(Arrays.equals(body(fetch("GET","assets/sound/swf/"+sound.getFileName())),Files.readAllBytes(sound)),"sound transport bytes");
        }
        for(String path:new String[]{"entry.swf","resource.cfg","assets/battle/role/head_176_68/10000201/2.png","assets/battle/role/head_176_68/11000111.png","assets/sound/swf/s219.swf","assets/battle/role/head_176_68/10000201/%31.png","assets/../assets/sound/swf/s1790.swf"})
            check(KaguyaResources.read(url(path))==null,"unmapped mod path "+path);
        check(KaguyaResources.read(new URL("https://evil.test"+PREFIX+portrait))==null,"host restriction");
        check(KaguyaResources.read(new URL("https://cdnnaruto-pt.oasgames.com:444"+PREFIX+portrait))==null,"port restriction");
        for(String header:new String[]{"range","if-none-match","if-modified-since","if-match","if-range","if-unmodified-since"}) {
            Map<String,String> request=new HashMap<String,String>();request.put(header,"test");check(!KaguyaResources.fullRequest("GET",request,0),"conditional mod");check(!StaticAssetCache.eligible(url(portrait),"GET",request,0),"conditional cache");
        }
        for(String path:new String[]{"entry.swf","resource.cfg","flash/core/LoginConfig.xml","config/test.cfg","assets/a.png?token=private","assets/../flash/a.swf"})check(!StaticAssetCache.eligible(url(path),"GET",EMPTY,0),"cache exclusion "+path);
        check(!StaticAssetCache.eligible(url(portrait),"POST",EMPTY,0),"POST cache");check(!StaticAssetCache.eligible(url(portrait),"GET",Collections.singletonMap("cache-control","max-age=0"),0),"revalidate cache");
        URL art=url("assets/test.png");
        for(String control:new String[]{"private, max-age=600","no-store, max-age=600","no-cache, max-age=600","max-age=0","","max-age=999999999999999999999"})check(StaticAssetCache.begin(art,10,control,null,false)==null,"cache-control ignored "+control);
        check(StaticAssetCache.begin(art,10,"max-age=600","gzip",false)==null,"encoded cache");check(StaticAssetCache.begin(art,10,"max-age=600",null,true)==null,"session response cache");check(StaticAssetCache.begin(art,8*1024*1024+1,"max-age=600",null,false)==null,"oversize cache");
        check(StaticAssetCache.begin(art,10,"max-age=600",null,false,601)==null,"upstream Age ignored");
        StaticAssetCache.Writer incomplete=StaticAssetCache.begin(art,10,"max-age=600",null,false);incomplete.write(new byte[2],0,2);incomplete.finish();check(StaticAssetCache.read(art)==null,"partial published");
        StaticAssetCache.Writer aborted=StaticAssetCache.begin(art,10,"max-age=600",null,false);aborted.write(new byte[10],0,10);aborted.abort();check(StaticAssetCache.read(art)==null,"aborted published");
        cache(art,new byte[10],"max-age=600");
        for(File file:cache.toFile().listFiles())try(RandomAccessFile io=new RandomAccessFile(file,"rw")){io.seek(4);io.writeLong(1);}
        check(StaticAssetCache.read(art)==null,"expired cache served");
        byte[] megabyte=new byte[1024*1024];for(int i=0;i<70;i++)cache(url("assets/bound"+i+".png"),megabyte,"max-age=600");
        long total=0;int count=0;for(File file:cache.toFile().listFiles()){check(!file.getName().endsWith(".part"),"partial leaked");total+=file.length();count++;}
        check(total<=64L*1024*1024 && count<=512,"disk budget exceeded");
        for(File file:cache.toFile().listFiles())file.delete();Files.delete(cache);
        System.out.println("PASS: 25 packaged Kaguya assets, HTTP GET/HEAD, style/OFF/original fallback, strict routing; bounded atomic cache, exclusions, expiry and 64 MiB disk budget.");
    }
}
