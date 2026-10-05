package br.davi.narutoair.portal;
public final class AudioResourceLogTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  String path="/PT_NarutoAlpha9.35Build301/assets/sound/music.mp3";
  AudioResourceLog.clear();
  check(AudioResourceLog.snapshot().contains("Nenhuma"),"empty report");
  check(AudioResourceLog.audioPath(path),"MP3 transfer");
  check(AudioResourceLog.audioPath(path.replace("music.mp3","swf/s1791.swf")),"embedded sound transfer");
  for(String bad:new String[]{path+"?token=PRIVATE",path.replace("music.mp3","images/hero.png"),"https://user:PRIVATE@host"+path,"/account/PRIVATE.mp3",path.replace("music.mp3","%70rivate.mp3")})
   check(!AudioResourceLog.audioPath(bad),"private/foreign path admitted");
  for(int i=0;i<100;i++)AudioResourceLog.record(path,"HTTP",i,1234);
  String text=AudioResourceLog.snapshot();
  check(text.split("\\n").length==64,"unbounded diagnostics");
  check(!text.contains("HTTP=0;") && text.contains("HTTP=99; bytes=1234"),"rolling metadata");
  AudioResourceLog.clear();check(AudioResourceLog.snapshot().contains("Nenhuma"),"session clear");
  System.out.println("PASS: audio transfer metadata bounds, exact static asset paths, sensitive URL rejection and session reset.");
 }
}
