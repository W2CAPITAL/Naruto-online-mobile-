package br.davi.narutoair.portal;
import java.util.*;
import java.util.regex.Pattern;
/** Bounded audio transfer metadata. No query, headers, credentials or media bytes. */
public final class AudioResourceLog {
    private static final ArrayDeque<String> rows=new ArrayDeque<String>();
    private static final Pattern PATH=Pattern.compile("/PT_NarutoAlpha[0-9.]+Build[0-9]+/[A-Za-z0-9_./-]+");
    public static boolean audioPath(String path) {
        if(path==null || path.length()>220)return false;
        String lower=path.toLowerCase(Locale.US);
        boolean media=lower.endsWith(".mp3") || lower.endsWith(".aac") || lower.endsWith(".m4a") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".flv");
        boolean soundSwf=lower.endsWith(".swf") && (lower.contains("/sound/") || lower.contains("/music/") || lower.contains("/bgm/"));
        return (media || soundSwf) && PATH.matcher(path).matches();
    }
    public static synchronized void record(String path,String phase,int status,long bytes) {
        if(!audioPath(path))return;
        if(rows.size()>=64)rows.removeFirst();
        rows.addLast(phase+" "+path+"; HTTP="+status+"; bytes="+Math.max(0,bytes));
    }
    public static synchronized void clear(){rows.clear();}
    public static synchronized String snapshot(){
        if(rows.isEmpty())return "Nenhuma transferencia de audio capturada no transporte.";
        StringBuilder text=new StringBuilder();for(String row:rows){if(text.length()>0)text.append('\n');text.append(row);}return text.toString();
    }
}
