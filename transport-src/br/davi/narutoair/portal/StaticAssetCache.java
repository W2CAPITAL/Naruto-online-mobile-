package br.davi.narutoair.portal;

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;

/** Optional app-private disk cache. Only explicitly cacheable, versioned CDN art/SWFs. */
public final class StaticAssetCache {
    private static final long MAX_FILE = 8L * 1024 * 1024, MAX_TOTAL = 64L * 1024 * 1024;
    private static final int MAX_FILES = 512, MAGIC = 0x4e414331;
    private static File directory;
    private static final Pattern PATH = Pattern.compile(
        "^/PT_NarutoAlpha[0-9.]+Build[0-9]+/(assets|flash)/[A-Za-z0-9_./-]+\\.(png|jpg|jpeg|swf)$");
    public static synchronized void configure(File root) { directory = root; }
    static boolean eligible(URL url, String method, Map<String,String> headers, long bodyLength) {
        return method.equals("GET") && KaguyaResources.fullRequest(method,headers,bodyLength) &&
            KaguyaResources.official(url) && url.getQuery() == null && PATH.matcher(url.getPath()).matches() &&
            !url.getPath().contains("..") && !url.getPath().contains("//") &&
            !bypass(headers.get("cache-control")) && !"no-cache".equalsIgnoreCase(headers.get("pragma"));
    }
    private static boolean bypass(String control) {
        if(control==null)return false;
        String c=control.toLowerCase(Locale.US);
        return c.contains("no-cache") || c.contains("no-store") || c.matches(".*(?:^|,)\\s*max-age\\s*=\\s*0\\s*(?:,|$).*");
    }
    private static String key(URL url) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(url.toExternalForm().getBytes("UTF-8"));
        StringBuilder hex = new StringBuilder(64);
        final char[] alphabet="0123456789abcdef".toCharArray();
        for (byte b : digest) hex.append(alphabet[(b & 255) >>> 4]).append(alphabet[b & 15]);
        return hex.toString() + ".bin";
    }
    public static final class Hit implements Closeable {
        public final InputStream stream; public final long length; public final String type;
        Hit(InputStream stream,long length,String type) { this.stream=stream;this.length=length;this.type=type; }
        public void close() throws IOException { stream.close(); }
    }
    public static synchronized Hit read(URL url) {
        if (directory == null) return null;
        DataInputStream input = null;
        try {
            File file = new File(directory,key(url));
            input = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            if (input.readInt() != MAGIC || input.readLong() <= System.currentTimeMillis()) throw new IOException("expired");
            long length = input.readLong(); String type = input.readUTF();
            if (length <= 0 || length > MAX_FILE || file.length() != 4+8+8+2+type.length()+length) throw new IOException("cache length");
            file.setLastModified(System.currentTimeMillis()); // LRU only; embedded expiry never changes.
            return new Hit(input,length,type);
        } catch (Exception miss) {
            if (input != null) try { input.close(); } catch (IOException ignored) { }
            try { new File(directory,key(url)).delete(); } catch (Exception ignored) { }
            return null;
        }
    }
    public static Writer begin(URL url, long length, String cacheControl, String encoding, boolean hasCookie) {
        return begin(url,length,cacheControl,encoding,hasCookie,0);
    }
    public static Writer begin(URL url, long length, String cacheControl, String encoding, boolean hasCookie,long ageSeconds) {
        if (!eligible(url,"GET",Collections.<String,String>emptyMap(),0)) return null;
        if (hasCookie || length <= 0 || length > MAX_FILE || encoding != null && !encoding.equalsIgnoreCase("identity")) return null;
        String control = cacheControl == null ? "" : cacheControl.toLowerCase(Locale.US);
        if (control.contains("no-store") || control.contains("no-cache") || control.contains("private")) return null;
        Matcher age = Pattern.compile("(?:^|,)\\s*max-age\\s*=\\s*([0-9]+)\\s*(?:,|$)").matcher(control);
        if (!age.find()) return null; // Never invent permission to cache responses.
        try {
            long seconds = Math.min(3600,Long.parseLong(age.group(1))-Math.max(0,ageSeconds));
            if (seconds <= 0) return null;
            File root;
            synchronized (StaticAssetCache.class) { root = directory; }
            if (root == null || !root.isDirectory() && !root.mkdirs()) return null;
            return new Writer(root,key(url),length,System.currentTimeMillis()+seconds*1000,type(url));
        } catch (Exception optional) { return null; }
    }
    private static String type(URL url) {
        String path = url.getPath().toLowerCase(Locale.US);
        return path.endsWith(".swf") ? "application/x-shockwave-flash" : path.endsWith(".png") ? "image/png" : "image/jpeg";
    }
    public static final class Writer {
        private final File temporary, target; private final long expected;
        private OutputStream stream; private long written;
        Writer(File root,String name,long length,long expiry,String type) throws IOException {
            temporary=File.createTempFile("asset-",".part",root);target=new File(root,name);expected=length;
            try {
                DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(temporary)));
                stream=output;output.writeInt(MAGIC);output.writeLong(expiry);output.writeLong(length);output.writeUTF(type);
            } catch (IOException failure) { abort();throw failure; }
        }
        public void write(byte[] bytes,int offset,int length) {
            if (stream == null) return;
            try { if (written+length > expected) throw new IOException("oversize");stream.write(bytes,offset,length);written+=length; }
            catch (IOException optional) { abort(); }
        }
        public void finish() {
            if (stream == null) return;
            try {
                stream.close();stream=null;
                if (written != expected) { temporary.delete();return; }
                synchronized (StaticAssetCache.class) {
                    // An old session may have been writing during reconfiguration.
                    if (!target.getParentFile().equals(directory)) { temporary.delete();return; }
                    if (!temporary.renameTo(target)) { temporary.delete();return; }
                    trim(directory);
                }
            } catch (Exception optional) { abort(); }
        }
        public void abort() {
            if (stream != null) try { stream.close(); } catch (IOException ignored) { }
            stream=null;temporary.delete();
        }
    }
    private static void trim(File root) {
        File[] files=root.listFiles(new FilenameFilter(){public boolean accept(File d,String n){return n.endsWith(".bin");}});
        if (files == null) return;
        Arrays.sort(files,new Comparator<File>(){public int compare(File a,File b){return Long.compare(a.lastModified(),b.lastModified());}});
        long bytes=0;for(File file:files) bytes+=file.length();
        int count=files.length;
        for(File file:files) { if(bytes<=MAX_TOTAL && count<=MAX_FILES)break;long size=file.length();if(file.delete()){bytes-=size;count--;} }
        // Remove partial files left by process death, never concurrent active writes.
        File[] parts=root.listFiles(new FilenameFilter(){public boolean accept(File d,String n){return n.endsWith(".part");}});
        if(parts!=null)for(File part:parts)if(System.currentTimeMillis()-part.lastModified()>3600000)part.delete();
    }
}
