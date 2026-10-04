package br.davi.narutoair.portal;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.*;

/** Cosmetic overrides only. Unmapped resources always use the official transport. */
public final class KaguyaResources {
    public interface Assets { InputStream open(String path) throws IOException; }
    private static final String[] STYLES = {"feng", "huo", "lei", "shui", "tu"};
    private static final Set<String> SOUNDS = new HashSet<String>(Arrays.asList(
        "s1754", "s1790", "s1791", "s1792", "s17661", "s17662", "s17663", "s17664",
        "s17665", "s17666", "s17667", "s17668", "s17669", "s17670", "s17676"));
    private static final Pattern PORTRAIT = Pattern.compile(
        "^/PT_NarutoAlpha[0-9.]+Build[0-9]+/assets/battle/role/head_(176_68|45_45)/10000[1-5]01/1\\.png$");
    private static final Pattern SOUND = Pattern.compile(
        "^/PT_NarutoAlpha[0-9.]+Build[0-9]+/assets/sound/swf/(s[0-9]+)\\.swf$");
    private static final class Config {
        final boolean enabled; final int style; final Assets assets;
        Config(boolean enabled, int style, Assets assets) { this.enabled=enabled; this.style=style; this.assets=assets; }
    }
    private static volatile Config config = new Config(false, 0, null);
    public static void configure(boolean enabled, int style, Assets assets) {
        if (style < 0 || style >= STYLES.length || assets == null) throw new IllegalArgumentException("mod config");
        config = new Config(enabled, style, assets);
    }
    static boolean official(URL url) {
        return url.getUserInfo() == null && (url.getProtocol().equals("https") || url.getProtocol().equals("http")) &&
            (url.getPort() < 0 || url.getPort() == url.getDefaultPort()) &&
            url.getHost().equalsIgnoreCase("cdnnaruto-pt.oasgames.com");
    }
    static boolean fullRequest(String method, Map<String,String> headers, long bodyLength) {
        if (!(method.equals("GET") || method.equals("HEAD")) || bodyLength != 0) return false;
        for (String key : new String[]{"range", "if-range", "if-none-match", "if-modified-since", "if-match", "if-unmodified-since"})
            if (headers.containsKey(key)) return false;
        return true;
    }
    /** Snapshot configuration once per request; do not mix a style during a load. */
    public static byte[] read(URL url) {
        Config c = config;
        if (!c.enabled || c.assets == null || !official(url)) return null;
        String resource = null;
        Matcher portrait = PORTRAIT.matcher(url.getPath()), sound = SOUND.matcher(url.getPath());
        boolean png = portrait.matches();
        if (png) resource = STYLES[c.style] + "/" + (portrait.group(1).equals("176_68") ? "hengtiao.png" : "jineng.png");
        else if (sound.matches() && SOUNDS.contains(sound.group(1))) resource = "sounds/" + sound.group(1) + ".swf";
        if (resource == null) return null;
        try (InputStream input = c.assets.open("mods/kaguya/" + resource)) {
            byte[] data = EntryCompatibility.readBounded(input, 256 * 1024);
            if (png) {
                if (data.length < 24 || !Arrays.equals(Arrays.copyOf(data,8), new byte[]{(byte)137,80,78,71,13,10,26,10})) return null;
                DataInputStream header = new DataInputStream(new ByteArrayInputStream(data,16,8));
                if (header.readInt() != (portrait.group(1).equals("176_68") ? 176 : 45) ||
                    header.readInt() != (portrait.group(1).equals("176_68") ? 68 : 45)) return null;
            } else if (data.length < 8 || !((data[0]=='F' || data[0]=='C') && data[1]=='W' && data[2]=='S')) return null;
            return data;
        } catch (Exception missing) { return null; }
    }
}
